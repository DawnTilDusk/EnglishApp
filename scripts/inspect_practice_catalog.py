"""Read the deployed practice catalog; never prints credentials or writes to Supabase."""
from __future__ import annotations

import argparse
import json
from collections import Counter
from pathlib import Path
import urllib.error
import urllib.request

ROOT = Path(__file__).resolve().parents[1]


def configuration() -> tuple[str, str]:
    values: dict[str, str] = {}
    for path in (ROOT / '.env.local', ROOT / 'web/.env.local'):
        if not path.exists():
            continue
        for line in path.read_text(encoding='utf-8-sig').splitlines():
            if '=' in line and not line.lstrip().startswith('#'):
                name, value = line.split('=', 1)
                values.setdefault(name.strip(), value.strip().strip('"').strip("'"))
    url = values.get('SUPABASE_URL') or values.get('NEXT_PUBLIC_SUPABASE_URL')
    key = values.get('SUPABASE_ANON_KEY') or values.get('NEXT_PUBLIC_SUPABASE_ANON_KEY')
    if not url or not key:
        raise RuntimeError('Configure the project URL and public anon credential first')
    return url.rstrip('/'), key


def fetch(table: str, fields: str = '*') -> list[dict]:
    base, key = configuration()
    rows = []
    while True:
        request = urllib.request.Request(
            f'{base}/rest/v1/{table}?select={fields}&offset={len(rows)}&limit=1000',
            headers={'apikey': key, 'Authorization': f'Bearer {key}'},
        )
        with urllib.request.urlopen(request, timeout=25) as response:
            page = json.load(response)
        rows.extend(page)
        if len(page) < 1000:
            return rows


def inventory() -> dict:
    tables = {name: fetch(name) for name in (
        'reading_sets', 'reading_questions', 'reading_options',
        'listening_materials', 'listening_questions', 'listening_options',
    )}
    reading_counts = Counter(q['set_id'] for q in tables['reading_questions'])
    listening_counts = Counter(q['material_id'] for q in tables['listening_questions'])
    return {
        'counts': {name: len(rows) for name, rows in tables.items()},
        'reading': [{**{key: row.get(key) for key in (
            'set_id', 'title', 'title_zh', 'grade', 'grade_band', 'content_origin', 'version')},
            'question_count': reading_counts[row['set_id']]} for row in tables['reading_sets']],
        'listening': [{**{key: row.get(key) for key in (
            'material_id', 'title', 'title_zh', 'grade_level', 'book_id', 'version')},
            'question_count': listening_counts[row['material_id']],
            'has_audio': bool(row.get('audio_url')),
            'has_transcript': bool(row.get('transcript'))} for row in tables['listening_materials']],
    }


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output', type=Path)
    args = parser.parse_args()
    try:
        result = inventory()
        if args.output:
            args.output.parent.mkdir(parents=True, exist_ok=True)
            args.output.write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding='utf-8')
        print(json.dumps(result['counts'], ensure_ascii=False))
        print('Listening exercise groups:', sum(r['question_count'] > 0 and not r['book_id'] for r in result['listening']))
    except urllib.error.HTTPError as error:
        raise SystemExit(f'Catalog read failed: HTTP {error.code}') from None
