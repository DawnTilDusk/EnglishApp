"""Validate an original listening pack, generate F5-TTS audio, and prepare seed SQL.

Run with the existing f5-tts Python environment. No database writes are performed.
Audio is generated locally; --upload publishes only this pack's MP3 files to the
existing public audio-pep bucket using the server credential in local env files.
"""
from __future__ import annotations

import argparse
import hashlib
import http.client
import io
import json
import math
import os
from pathlib import Path
import shutil
import subprocess
import sys
import time
import urllib.error
import urllib.request

ROOT = Path(__file__).resolve().parents[1]
PACK = ROOT / 'scripts/listening_seed/original_20260922.json'
OUTPUT = ROOT / 'EngResource/listening_original_20260922'
REFERENCE = ROOT / 'EngResource/f5tts_reference'
BUCKET = 'audio-pep'


def transfer(request):
    """Retry interrupted transfers; uploads use immutable content-addressed names."""
    import requests
    url = request if isinstance(request, str) else request.full_url
    method = 'GET' if isinstance(request, str) else request.get_method()
    headers = {} if isinstance(request, str) else dict(request.header_items())
    data = None if isinstance(request, str) else request.data
    for attempt in range(4):
        try:
            with requests.Session() as session:
                session.trust_env = attempt < 2
                response = session.request(method, url, headers=headers, data=data, timeout=(15, 45), allow_redirects=False)
                if response.status_code >= 400:
                    raise urllib.error.HTTPError(url, response.status_code, response.reason,
                                                 response.headers, io.BytesIO(response.content))
                if response.status_code >= 300:
                    raise RuntimeError('Unexpected redirect during authenticated transfer')
                return response.content, response.headers.get('Content-Type', '').split(';')[0]
        except requests.RequestException:
            if attempt == 3:
                raise
            time.sleep(2 * (attempt + 1))


def validate(pack):
    seen = set()
    for material in pack['materials']:
        assert material['material_id'] not in seen, 'Duplicate material ID'
        seen.add(material['material_id'])
        assert material['grade'] in (7, 8, 9)
        assert all(material[k].strip() for k in ('title', 'title_zh', 'transcript', 'transcript_zh'))
        assert len(material['questions']) == 3
        for q in material['questions']:
            assert q['question_id'] not in seen, 'Duplicate question ID'
            seen.add(q['question_id'])
            assert [o['option_id'] for o in q['options']] == list('ABCD')
            assert q['correct_option_id'] in 'ABCD'
            assert len({o['option_text'].lower() for o in q['options']}) == 4
            assert q['stem'].strip() and q['explanation'].strip()
    print(f"Validated {len(pack['materials'])} materials / {sum(len(m['questions']) for m in pack['materials'])} questions")


def ffmpeg_path():
    candidate = Path(sys.prefix).parent / 'f5-ffmpeg/Library/bin/ffmpeg.exe'
    found = str(candidate) if candidate.exists() else shutil.which('ffmpeg')
    if not found:
        raise RuntimeError('ffmpeg not found; activate f5-tts and configure its PATH')
    os.environ['PATH'] = str(Path(found).parent) + os.pathsep + os.environ.get('PATH', '')
    return found


def generate(pack, manifest):
    import numpy as np
    import soundfile as sf
    import torch
    from f5_tts.api import F5TTS
    from huggingface_hub import snapshot_download

    if not torch.cuda.is_available():
        raise RuntimeError('CUDA unavailable; refusing an unexpectedly slow CPU batch')
    ffmpeg = ffmpeg_path()
    # Reuse the models already downloaded for the user's successful MVP.
    model_dir = Path(snapshot_download('SWivid/F5-TTS', local_files_only=True))
    vocoder_dir = snapshot_download('charactr/vocos-mel-24khz', local_files_only=True)
    tts = F5TTS(ckpt_file=str(model_dir / 'F5TTS_v1_Base/model_1250000.safetensors'),
                vocoder_local_path=vocoder_dir, device='cuda')
    reference_text = (REFERENCE / 'reference.txt').read_text(encoding='utf-8').strip()
    for index, m in enumerate(pack['materials']):
        mid = m['material_id']
        text_hash = hashlib.sha256(m['transcript'].encode()).hexdigest()
        mp3 = OUTPUT / f'{mid}.mp3'
        old = manifest.get(mid, {})
        if old.get('text_sha256') == text_hash and mp3.exists() and old.get('audio_sha256') == hashlib.sha256(mp3.read_bytes()).hexdigest():
            print('Reusing', mid, flush=True)
            continue
        print(f"Generating {index + 1}/{len(pack['materials'])}: {mid}", flush=True)
        wav, sr, _ = tts.infer(ref_file=str(REFERENCE / 'reference.wav'), ref_text=reference_text,
            gen_text=m['transcript'], seed=20260922 + index, nfe_step=32,
            speed={7: .85, 8: .90, 9: .95}[m['grade']])
        if not np.isfinite(wav).all() or len(wav) / sr < 10 or np.max(np.abs(wav)) < .01:
            raise RuntimeError(f'Invalid generated waveform: {mid}')
        peak = float(np.max(np.abs(wav)))
        if peak > .95:
            wav = wav * (.95 / peak)
        wave_path = OUTPUT / f'{mid}.wav'
        sf.write(wave_path, wav, sr, subtype='PCM_16')
        subprocess.run([ffmpeg, '-v', 'error', '-y', '-i', str(wave_path), '-ac', '1',
                        '-c:a', 'libmp3lame', '-b:a', '96k', str(mp3)], check=True)
        manifest[mid] = {'duration_seconds': round(len(wav) / sr, 3), 'sample_rate': sr,
                         'text_sha256': text_hash, 'audio_sha256': hashlib.sha256(mp3.read_bytes()).hexdigest(),
                         'file': mp3.name, 'voice_reference': 'EngResource/f5tts_reference/reference.wav'}
        (OUTPUT / 'audio_manifest.json').write_text(json.dumps(manifest, indent=2), encoding='utf-8')
        print('Generated', mid, manifest[mid]['duration_seconds'], 'seconds', flush=True)


def upload(pack, manifest, env):
    key = env.get('SUPABASE_SERVICE_ROLE_KEY')
    if not key:
        raise RuntimeError('A service role credential is required; management tokens cannot authorize Storage uploads')
    base = env['SUPABASE_URL'].rstrip('/')
    for m in pack['materials']:
        entry = manifest[m['material_id']]
        content = (OUTPUT / entry['file']).read_bytes()
        assert hashlib.sha256(content).hexdigest() == entry['audio_sha256']
        object_path = f"original/20260922/{m['material_id']}-{entry['audio_sha256'][:12]}.mp3"
        public_url = f'{base}/storage/v1/object/public/{BUCKET}/{object_path}'
        request = urllib.request.Request(f'{base}/storage/v1/object/{BUCKET}/{object_path}',
            data=content, method='POST', headers={'Authorization': f'Bearer {key}', 'apikey': key,
            'Content-Type': 'audio/mpeg', 'x-upsert': 'false'})
        try:
            transfer(request)
        except urllib.error.HTTPError as error:
            # A retry can reuse an identical content-addressed object, never overwrite it.
            if error.code not in (400, 409):
                raise RuntimeError(f'Upload failed: HTTP {error.code}') from None
        downloaded, content_type = transfer(public_url)
        assert content_type.startswith('audio/')
        if hashlib.sha256(downloaded).hexdigest() != entry['audio_sha256']:
            raise RuntimeError(f'Public audio verification failed: {m["material_id"]}')
        entry['audio_url'] = public_url
        print('Uploaded and verified', m['material_id'], flush=True)
        (OUTPUT / 'audio_manifest.json').write_text(json.dumps(manifest, indent=2), encoding='utf-8')


def sql_string(value):
    return 'NULL' if value is None else "'" + str(value).replace("'", "''") + "'"


def build_sql(pack, manifest):
    # Unique batch IDs, transaction and ON CONFLICT DO NOTHING make imports additive.
    sql = ['-- Original Seedie listening pack. Upload matching audio before importing.', 'BEGIN;',
           'ALTER TABLE public.listening_materials ADD COLUMN IF NOT EXISTS transcript_zh TEXT;']
    now = int(time.time() * 1000)
    for m in pack['materials']:
        a = manifest[m['material_id']]
        audio_url = a.get('audio_url') or (
            'https://xojbnrxnkgkqacrkcmqc.supabase.co/storage/v1/object/public/'
            f"{BUCKET}/original/20260922/{m['material_id']}-{a['audio_sha256'][:12]}.mp3")
        values = [m['material_id'], m['title'], m['title_zh'], m['material_type'],
            'Listen to the passage and choose the best answer for each question.',
            m['transcript'], m['transcript_zh'], audio_url]
        sql.append('INSERT INTO public.listening_materials (material_id,title,title_zh,material_type,prompt_text,transcript,transcript_zh,audio_url,estimated_seconds,sort_order,version,updated_at,book_id,grade_level) VALUES (' + ','.join(map(sql_string,values)) + f",{math.ceil(a['duration_seconds'])},{m['sort_order']},1,{now},NULL,{m['grade'] * 2 - 1}) ON CONFLICT (material_id) DO NOTHING;")
        for q in m['questions']:
            values = [q['question_id'],m['material_id'],q['question_type'],q['stem'],q['correct_option_id'],q['explanation']]
            sql.append('INSERT INTO public.listening_questions (question_id,material_id,question_type,stem,correct_option_id,explanation,sort_order,reward_token) VALUES (' + ','.join(map(sql_string,values)) + f",{q['sort_order']},{q['reward_token']}) ON CONFLICT (question_id) DO NOTHING;")
            for o in q['options']:
                sql.append('INSERT INTO public.listening_options (question_id,option_id,option_text,sort_order) VALUES (' + ','.join(map(sql_string,[q['question_id'],o['option_id'],o['option_text']])) + f",{o['sort_order']}) ON CONFLICT (question_id,option_id) DO NOTHING;")
    sql.append('COMMIT;')
    path = ROOT / 'scripts/listening_seed/original_20260922.sql'
    path.write_text('\n'.join(sql) + '\n', encoding='utf-8')
    print('Prepared', path)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--generate', action='store_true')
    parser.add_argument('--upload', action='store_true')
    parser.add_argument('--sql', action='store_true')
    args = parser.parse_args()
    pack = json.loads(PACK.read_text(encoding='utf-8'))
    validate(pack)
    OUTPUT.mkdir(parents=True, exist_ok=True)
    path = OUTPUT / 'audio_manifest.json'
    manifest = json.loads(path.read_text(encoding='utf-8')) if path.exists() else {}
    if args.generate:
        generate(pack, manifest)
    if args.upload:
        from upload_pep_audio import load_env
        env = load_env()
        if env.get('SUPABASE_URL') != 'https://xojbnrxnkgkqacrkcmqc.supabase.co':
            raise RuntimeError('Configured project differs from the reviewed Seedie project')
        upload(pack, manifest, env)
    if args.sql:
        build_sql(pack, manifest)


if __name__ == '__main__':
    main()
