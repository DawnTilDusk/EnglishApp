"""Append the reviewed pack through the Data API when management auth is unavailable.

Requires previously uploaded, verified audio. Stages new materials outside the
free-practice catalog until all questions and options have been read back and
compared. No schema changes; Chinese transcripts remain local if the optional
transcript_zh column has not yet been added by migration 042.
"""
import hashlib
import json
import math
from pathlib import Path
import time
import urllib.error
import urllib.request

from prepare_listening_pack import transfer, validate
from upload_pep_audio import load_env

ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / 'EngResource/listening_original_20260922'
env = load_env()
base = env['SUPABASE_URL'].rstrip('/')
if base != 'https://xojbnrxnkgkqacrkcmqc.supabase.co':
    raise RuntimeError('Wrong project')
key = env['SUPABASE_SERVICE_ROLE_KEY']
pack = json.loads((ROOT / 'scripts/listening_seed/original_20260922.json').read_text(encoding='utf-8'))
manifest = json.loads((OUTPUT / 'audio_manifest.json').read_text(encoding='utf-8'))
validate(pack)


def request(path, method='GET', data=None, credential=key):
    req = urllib.request.Request(base + '/rest/v1/' + path,
        data=None if data is None else json.dumps(data, ensure_ascii=False).encode(), method=method,
        headers={'apikey': credential, 'Authorization': 'Bearer ' + credential,
                 'Content-Type': 'application/json', 'Prefer': 'resolution=ignore-duplicates,return=representation'})
    raw, _ = transfer(req)
    return json.loads(raw) if raw else []


try:
    request('listening_materials?select=transcript_zh&limit=0')
    has_translation = True
except urllib.error.HTTPError as error:
    body = json.loads(error.read())
    if error.code != 400 or body.get('code') != '42703':
        raise
    has_translation = False
print('Cloud transcript_zh available:', has_translation, flush=True)

materials, questions, options = [], [], []
for m in pack['materials']:
    a = manifest[m['material_id']]
    if not a.get('audio_url'):
        raise RuntimeError('Upload and verify all audio first')
    assert hashlib.sha256((OUTPUT / a['file']).read_bytes()).hexdigest() == a['audio_sha256']
    row = dict(material_id=m['material_id'],title=m['title'],title_zh=m['title_zh'],
        material_type=m['material_type'],prompt_text='Listen to the passage and choose the best answer for each question.',
        transcript=m['transcript'],audio_url=a['audio_url'],estimated_seconds=math.ceil(a['duration_seconds']),
        sort_order=m['sort_order'],version=1,updated_at=int(time.time()*1000),
        book_id='seedie-staging-original-20260922',grade_level=m['grade']*2-1)
    if has_translation:
        row['transcript_zh'] = m['transcript_zh']
    materials.append(row)
    for q in m['questions']:
        questions.append({k:v for k,v in q.items() if k!='options'} | {'material_id':m['material_id']})
        options.extend(o | {'question_id':q['question_id']} for o in q['options'])

material_filter = 'material_id=in.(' + ','.join(m['material_id'] for m in materials) + ')'
question_filter = 'question_id=in.(' + ','.join(q['question_id'] for q in questions) + ')'
for table, rows in [('listening_materials',materials),('listening_questions',questions),('listening_options',options)]:
    request(table, 'POST', rows)
    print('Appended/reused',table,len(rows),flush=True)

checks = [('listening_materials',materials,material_filter,('material_id',)),
          ('listening_questions',questions,material_filter,('question_id',)),
          ('listening_options',options,question_filter,('question_id','option_id'))]
for table, expected, where, keys in checks:
    actual = request(table+'?select=*&'+where)
    assert len(actual)==len(expected), (table,'wrong row count')
    indexed = {tuple(r[k] for k in keys):r for r in actual}
    for row in expected:
        found=indexed[tuple(row[k] for k in keys)]
        for k,v in row.items():
            if k not in ('updated_at','book_id'):
                assert found[k]==v,(table,tuple(row[x] for x in keys),k)
    print('Verified every field:',table,flush=True)

# Publish only this pack's staged rows after verifying all children.
request('listening_materials?'+material_filter+'&book_id=eq.seedie-staging-original-20260922',
        'PATCH',{'book_id':None})
final=request('listening_materials?select=material_id,book_id,audio_url&'+material_filter)
assert len(final)==12 and all(r['book_id'] is None and r['audio_url'] for r in final)
anon=env.get('NEXT_PUBLIC_SUPABASE_ANON_KEY') or env.get('SUPABASE_ANON_KEY')
if anon:
    for table, expected, where, _ in checks:
        visible=request(table+'?select=*&'+where,credential=anon)
        assert len(visible)==len(expected),(table,'not readable by client')
    print('Client public reads verified',flush=True)
report={'materials':12,'questions':36,'options':144,'all_fields_verified':True,
        'client_reads_verified':bool(anon),'translations_uploaded':has_translation,'method':'Data API',
        'material_ids':[m['material_id'] for m in materials]}
(OUTPUT / 'import_verification.json').write_text(json.dumps(report,indent=2),encoding='utf-8')
print(json.dumps(report),flush=True)
