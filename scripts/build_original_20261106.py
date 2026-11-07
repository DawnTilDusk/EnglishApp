"""Validate and export 20 original readings and 20 device-TTS listening exercises.

Default: local files only. --apply appends this exact batch through the Data API,
then compares every published field and checks public client visibility.
"""
from __future__ import annotations

import argparse
from collections import Counter
from datetime import datetime, timezone
import json
from pathlib import Path
import re
import urllib.error
import urllib.parse
import urllib.request

from content_seed.common import BATCH, choices, words
from content_seed.reading_20261106 import READING
from content_seed.listening_20261106 import LISTENING

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'scripts/content_seed' / f'original_{BATCH}.json'
REPORT = ROOT / 'docs/2026-11-06/original_pack_import.json'
STAMP = int(datetime(2026, 11, 6, tzinfo=timezone.utc).timestamp() * 1000)
KEYS = {
    'reading_sets': ('set_id',), 'reading_questions': ('question_id',),
    'reading_options': ('question_id', 'option_id'),
    'listening_materials': ('material_id',), 'listening_questions': ('question_id',),
    'listening_options': ('question_id', 'option_id'),
}


def normal(text):
    return re.sub(r'\s+', ' ', text).strip().casefold()


def validate():
    assert len(READING) == len(LISTENING) == 20
    seen = set()
    answer_counts = Counter()
    for items, size in ((READING, 5), (LISTENING, 3)):
        for item in items:
            assert item['id'] not in seen
            seen.add(item['id'])
            assert len(item['questions']) == size, item['id']
            assert words(item['text']) >= (120 if size == 5 else 60), item['id']
            if size == 5:
                assert item['band'] in {'g7a', 'g7b', 'g8a', 'g8b', 'g9a', 'g9b'}
            else:
                assert item['grade'] in {7, 8, 9} and item['transcript_zh'].strip()
            for index, question in enumerate(item['questions']):
                assert len(question['wrong']) == 3
                texts = [question['correct'], *question['wrong']]
                assert len({normal(text) for text in texts}) == 4, (item['id'], index)
                assert all(text.strip() for text in texts)
                assert question['stem'].strip() and question['explanation'].strip()
                assert normal(question['evidence']) in normal(item['text']), (item['id'], index, 'missing evidence')
                correct, options = choices(item['id'], index, question)
                assert options[ord(correct)-65]['option_text'] == question['correct']
                answer_counts[correct] += 1
    assert set(answer_counts) == set('ABCD')
    return dict(reading=20, reading_questions=100, listening=20, listening_questions=60,
                reading_bands=dict(Counter(item['band'] for item in READING)),
                listening_grades=dict(Counter(item['grade'] for item in LISTENING)),
                correct_option_distribution=dict(answer_counts),
                evidence_checks=160, editorial_review='AI self-check; human editorial review pending')


def tables():
    result = {table: [] for table in KEYS}
    for number, item in enumerate(READING):
        result['reading_sets'].append(dict(
            set_id=item['id'], title=item['title'], title_zh=item['title_zh'], grade=int(item['band'][1]),
            grade_band=item['band'], content_origin='ai_generated', difficulty={'7':'easy','8':'medium','9':'hard'}[item['band'][1]],
            topic='original_practice', passage=item['text'], word_count=words(item['text']),
            estimated_minutes=7 if item['band'][1]=='7' else 9, sort_order=1100+number,
            version=1, updated_at=STAMP))
        for index, q in enumerate(item['questions']):
            qid = f"{item['id']}-q{index+1}"
            correct, options = choices(item['id'], index, q)
            result['reading_questions'].append(dict(question_id=qid, set_id=item['id'], question_type=q['kind'],
                stem=q['stem'], sort_order=index, correct_option_id=correct,
                explanation=f"{q['explanation']} 原文依据：{q['evidence']}", highlight_word=None, reward_token=2))
            result['reading_options'].extend(option | {'question_id':qid} for option in options)
    for number, item in enumerate(LISTENING):
        result['listening_materials'].append(dict(
            material_id=item['id'], title=item['title'], title_zh=item['title_zh'],
            material_type='monologue', prompt_text='AI-original practice. Audio uses the device English text-to-speech service.',
            transcript=item['text'], transcript_zh=item['transcript_zh'], audio_url=None,
            estimated_seconds=max(35, round(words(item['text'])/135*60)), sort_order=2100+number,
            version=1, updated_at=STAMP, book_id=None, grade_level=item['grade']*2-1))
        for index, q in enumerate(item['questions']):
            qid = f"{item['id']}-q{index+1}"
            correct, options = choices(item['id'], index, q)
            result['listening_questions'].append(dict(question_id=qid, material_id=item['id'], question_type=q['kind'],
                stem=q['stem'], sort_order=index, correct_option_id=correct,
                explanation=f"{q['explanation']} 原文依据：{q['evidence']}", reward_token=2))
            result['listening_options'].extend(option | {'question_id':qid} for option in options)
    return result


def environment():
    values = {}
    for path in (ROOT/'.env.local', ROOT/'web/.env.local'):
        if path.exists():
            for line in path.read_text(encoding='utf-8-sig').splitlines():
                if '=' in line and not line.lstrip().startswith('#'):
                    key, value = line.split('=',1)
                    values.setdefault(key.strip(), value.strip().strip('"').strip("'"))
    return values


class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        return None


def publish(rows):
    env = environment()
    base = (env.get('SUPABASE_URL') or env.get('NEXT_PUBLIC_SUPABASE_URL') or '').rstrip('/')
    if base != 'https://xojbnrxnkgkqacrkcmqc.supabase.co':
        raise RuntimeError('Unexpected target project')
    service = env.get('SUPABASE_SERVICE_ROLE_KEY')
    anon = env.get('SUPABASE_ANON_KEY') or env.get('NEXT_PUBLIC_SUPABASE_ANON_KEY')
    if not service or not anon:
        raise RuntimeError('Configure service and public credentials before applying')
    opener = urllib.request.build_opener(NoRedirect)

    def request(path, method='GET', data=None, key=service):
        encoded = None if data is None else json.dumps(data, ensure_ascii=False).encode()
        req = urllib.request.Request(base+'/rest/v1/'+path, data=encoded, method=method,
            headers={'apikey':key, 'Authorization':'Bearer '+key, 'Content-Type':'application/json',
                     'Prefer':'resolution=ignore-duplicates,return=minimal'})
        with opener.open(req, timeout=40) as response:
            raw = response.read()
            return json.loads(raw) if raw else []

    # Optional field: older projects can still use the pack, retaining translations in the local source.
    translation_uploaded = True
    try:
        request('listening_materials?select=transcript_zh&limit=0')
    except urllib.error.HTTPError as error:
        if error.code != 400:
            raise
        body = json.loads(error.read())
        if body.get('code') != '42703':
            raise RuntimeError('Unexpected listening schema error') from None
        translation_uploaded = False
        rows['listening_materials'] = [{k:v for k,v in r.items() if k!='transcript_zh'} for r in rows['listening_materials']]

    existing = {}
    filters = {}
    for table, expected in rows.items():
        column = KEYS[table][0]
        ids = sorted({str(row[column]) for row in expected})
        filters[table] = column+'=in.('+','.join(ids)+')'
        actual = request(table+'?select=*&'+filters[table]+'&limit=1000')
        existing[table] = {tuple(r[k] for k in KEYS[table]):r for r in actual}
        for row in expected:
            old = existing[table].get(tuple(row[k] for k in KEYS[table]))
            if old is not None and any(old.get(k)!=value for k,value in row.items()):
                raise RuntimeError(f'Existing record differs in {table}; refusing to overwrite')

    newly_created = {table: [r[KEYS[table][0]] for r in rows[table]
                            if (r[KEYS[table][0]],) not in existing[table]]
                     for table in ('reading_sets','listening_materials')}
    try:
        for table, expected in rows.items():
            request(table, 'POST', expected)
            print('Appended/retained', table, len(expected), flush=True)
        for table, expected in rows.items():
            for credential, label in ((service, 'service'), (anon, 'client')):
                actual = request(table+'?select=*&'+filters[table]+'&limit=1000', key=credential)
                assert len(actual)==len(expected), (table, label, 'count mismatch')
                indexed = {tuple(r[k] for k in KEYS[table]):r for r in actual}
                for row in expected:
                    found = indexed[tuple(row[k] for k in KEYS[table])]
                    assert all(found.get(k)==value for k,value in row.items()), (table, label, 'field mismatch')
            print('Verified all fields and client reads:', table, flush=True)
    except Exception:
        # Roll back only parent records absent at preflight; foreign keys remove this run's children.
        # Never delete or update a pre-existing question, even when retrying this command.
        for table, ids in newly_created.items():
            if ids:
                request(table+'?'+KEYS[table][0]+'=in.('+','.join(ids)+')', 'DELETE')
        raise
    report = dict(batch=BATCH, counts={table:len(expected) for table,expected in rows.items()},
        all_fields_verified=True, public_reads_verified=True, translations_uploaded=translation_uploaded,
        listening_audio='device_tts', human_editorial_review=False,
        inserted_parent_counts={table:len(ids) for table,ids in newly_created.items()})
    REPORT.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding='utf-8')
    return report


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--apply', action='store_true')
    args = parser.parse_args()
    summary = validate()
    data = tables()
    OUT.write_text(json.dumps(dict(batch=BATCH, origin='ai_generated', audio='device_tts',
        human_editorial_review=False, validation=summary, reading=READING, listening=LISTENING, tables=data),
        ensure_ascii=False, indent=2), encoding='utf-8')
    print(json.dumps(summary, ensure_ascii=False), flush=True)
    if args.apply:
        try:
            print(json.dumps(publish(data), ensure_ascii=False), flush=True)
        except urllib.error.HTTPError as error:
            raise SystemExit(f'Import failed: HTTP {error.code}; credentials were not printed') from None
