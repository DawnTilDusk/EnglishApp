"""Import the reviewed 20260922 pack into Seedie; default is a read-only check.

Use --apply only after prepare_listening_pack.py --upload --sql. All new rows are
inserted in one transaction; existing rows are never deleted or overwritten.
"""
import argparse
import json
from pathlib import Path

from apply_one_migration import run_sql
from upload_pep_audio import load_env, project_ref

ROOT = Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--apply', action='store_true')
args = parser.parse_args()
env = load_env()
ref = project_ref(env)
if ref != 'xojbnrxnkgkqacrkcmqc':
    raise RuntimeError('Configured project is not the reviewed Seedie project')
token = env['SUPABASE_ACCESS_TOKEN']
columns = run_sql(token, ref, "SELECT column_name FROM information_schema.columns WHERE table_schema='public' AND table_name='listening_materials' ORDER BY ordinal_position")
print('Columns:', [c['column_name'] for c in columns], flush=True)
if args.apply:
    manifest = json.loads((ROOT / 'EngResource/listening_original_20260922/audio_manifest.json').read_text(encoding='utf-8'))
    if len(manifest) != 12 or not all(a.get('audio_url') for a in manifest.values()):
        raise RuntimeError('Upload and verify all 12 audio files before importing')
    sql = (ROOT / 'scripts/listening_seed/original_20260922.sql').read_text(encoding='utf-8')
    print('Importing pack in one transaction...', flush=True)
    run_sql(token, ref, sql)
    print('Import complete', flush=True)
verification = run_sql(token, ref, """
SELECT m.material_id, m.grade_level, m.audio_url, count(distinct q.question_id) AS questions,
       count(o.option_id) AS options,
       count(distinct q.question_id) FILTER (WHERE q.correct_option_id=o.option_id) AS valid_answers
FROM public.listening_materials m
LEFT JOIN public.listening_questions q ON q.material_id=m.material_id
LEFT JOIN public.listening_options o ON o.question_id=q.question_id
WHERE m.material_id LIKE 'original-%-20260922-%'
GROUP BY m.material_id,m.grade_level,m.audio_url ORDER BY m.material_id
"""
)
print(json.dumps(verification, ensure_ascii=False, indent=2), flush=True)
if args.apply:
    assert len(verification) == 12
    assert all(r['questions'] == 3 and r['options'] == 12 and r['valid_answers'] == 3 and r['audio_url'] for r in verification)
    output = ROOT / 'EngResource/listening_original_20260922/import_verification.json'
    output.write_text(json.dumps(verification, indent=2), encoding='utf-8')
    print('Verified 12 materials, 36 questions, 144 options, 36 valid answers')
