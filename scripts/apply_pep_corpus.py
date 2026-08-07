"""Upload PEP audio segments to Supabase Storage and upsert word_books /
vocabulary_words / vocabulary_master / listening_materials rows.

Reuses REST direct-call pattern from apply_fltrp_word_books.py.

Usage:
    python scripts/apply_pep_corpus.py [--only=words|listening|master]
                                       [--book-id pep-g7-vol1]
                                       [--dry-run]

Env vars (read from .env.local):
    SUPABASE_ACCESS_TOKEN  - service-role token for management API
    SUPABASE_URL           - project URL
    SUPABASE_SERVICE_ROLE_KEY (optional, used for Storage upload; falls back to access token)
"""
from __future__ import annotations

import argparse
import json
import os
import re
import sys
import time
import urllib.error
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ENV_FILE = ROOT / ".env.local"
WORD_JSON = ROOT / "docs" / "pep" / "pep_word_books.json"
LISTEN_JSON = ROOT / "docs" / "pep" / "pep_listening_bundles.json"
CORPUS_ROOT_DEFAULT = Path(r"C:\Users\zhong\Desktop\Eng资料")

BUCKET = "audio-pep"


def load_env() -> dict[str, str]:
    values: dict[str, str] = {}
    if ENV_FILE.exists():
        for line in ENV_FILE.read_text(encoding="utf-8").splitlines():
            line = line.strip()
            if not line or line.startswith("#") or "=" not in line:
                continue
            key, _, value = line.partition("=")
            values[key.strip()] = value.strip().strip('"').strip("'")
    for key in (
        "SUPABASE_ACCESS_TOKEN",
        "SUPABASE_URL",
        "SUPABASE_SERVICE_ROLE_KEY",
    ):
        values.setdefault(key, os.environ.get(key, ""))
    return values


def project_ref(env: dict[str, str]) -> str:
    url = env.get("SUPABASE_URL", "")
    if "://" in url:
        host = url.split("://", 1)[1].split("/", 1)[0]
        if host.endswith(".supabase.co"):
            return host.split(".")[0]
    raise RuntimeError("SUPABASE_URL missing / malformed in .env.local")


def run_sql(token: str, ref: str, sql: str) -> object:
    body = json.dumps({"query": sql}).encode("utf-8")
    req = urllib.request.Request(
        f"https://api.supabase.com/v1/projects/{ref}/database/query",
        data=body,
        method="POST",
        headers={
            "Authorization": f"Bearer {token}",
            "Content-Type": "application/json",
            "Accept": "application/json",
            "User-Agent": "SeediePepImport/1.0",
        },
    )
    try:
        with urllib.request.urlopen(req, timeout=300) as resp:
            raw = resp.read().decode("utf-8")
            return json.loads(raw) if raw else None
    except urllib.error.HTTPError as e:
        err = e.read().decode("utf-8", errors="replace")
        raise RuntimeError(f"HTTP {e.code}: {err}") from e


def sql_str(value: str | None) -> str:
    if value is None:
        return "NULL"
    escaped = value.replace("'", "''")
    return f"'{escaped}'"


def sql_int(value: int | None) -> str:
    return "NULL" if value is None else str(int(value))


def sql_jsonb(value: object) -> str:
    return f"{sql_str(json.dumps(value, ensure_ascii=False))}::jsonb"


def slug(text: str) -> str:
    t = re.sub(r"[^A-Za-z0-9]+", "-", text.lower()).strip("-")
    return re.sub(r"-+", "-", t)


def master_id_for(english: str) -> str:
    return f"mv-{slug(english)}"


# ---------------------------------------------------------------------------
# Storage
# ---------------------------------------------------------------------------


def ensure_bucket(env: dict[str, str], dry_run: bool) -> None:
    if dry_run:
        print(f"[dry-run] would ensure bucket {BUCKET}")
        return
    url = env["SUPABASE_URL"].rstrip("/") + f"/storage/v1/bucket"
    key = env.get("SUPABASE_SERVICE_ROLE_KEY") or env["SUPABASE_ACCESS_TOKEN"]
    body = json.dumps({"id": BUCKET, "name": BUCKET, "public": True}).encode("utf-8")
    req = urllib.request.Request(
        url,
        data=body,
        method="POST",
        headers={
            "Authorization": f"Bearer {key}",
            "apikey": key,
            "Content-Type": "application/json",
        },
    )
    try:
        with urllib.request.urlopen(req, timeout=60) as resp:
            resp.read()
            print(f"Bucket {BUCKET} created.")
    except urllib.error.HTTPError as e:
        if e.code in (400, 409):
            # already exists
            print(f"Bucket {BUCKET} exists.")
        else:
            err = e.read().decode("utf-8", errors="replace")
            raise RuntimeError(f"ensure_bucket HTTP {e.code}: {err}") from e


def upload_object(env: dict[str, str], key: str, file_path: Path,
                  dry_run: bool) -> str:
    """Upload a local file to Storage, return the public URL."""
    public_url = f"{env['SUPABASE_URL'].rstrip('/')}/storage/v1/object/public/{BUCKET}/{key}"
    if dry_run:
        print(f"[dry-run] would upload {file_path} -> {public_url}")
        return public_url
    auth = env.get("SUPABASE_SERVICE_ROLE_KEY") or env["SUPABASE_ACCESS_TOKEN"]
    data = file_path.read_bytes()
    req = urllib.request.Request(
        f"{env['SUPABASE_URL'].rstrip('/')}/storage/v1/object/{BUCKET}/{key}",
        data=data,
        method="POST",
        headers={
            "Authorization": f"Bearer {auth}",
            "apikey": auth,
            "Content-Type": "audio/mpeg",
            "x-upsert": "true",
        },
    )
    try:
        with urllib.request.urlopen(req, timeout=600) as resp:
            resp.read()
    except urllib.error.HTTPError as e:
        err = e.read().decode("utf-8", errors="replace")
        raise RuntimeError(f"upload {key} HTTP {e.code}: {err}") from e
    return public_url


# ---------------------------------------------------------------------------
# Words
# ---------------------------------------------------------------------------


def upsert_book(token: str, ref: str, book: dict, dry_run: bool) -> None:
    sql = f"""
INSERT INTO public.word_books (
    book_id, title, description, language, difficulty, grade_level,
    version, word_count, cover_url, updated_at
) VALUES (
    {sql_str(book['book_id'])},
    {sql_str(book['title'])},
    {sql_str(book.get('description'))},
    {sql_str(book.get('language') or 'en-US')},
    {sql_str('mixed')},
    {sql_int(book.get('grade_level'))},
    {int(book.get('version') or 1)},
    {sum(len(u.get('words') or []) for u in book.get('units') or [])},
    NULL,
    {int(book.get('version') or 1)}
)
ON CONFLICT (book_id) DO UPDATE SET
    title = EXCLUDED.title,
    description = EXCLUDED.description,
    language = EXCLUDED.language,
    difficulty = EXCLUDED.difficulty,
    grade_level = EXCLUDED.grade_level,
    version = EXCLUDED.version,
    word_count = EXCLUDED.word_count,
    updated_at = EXCLUDED.updated_at;
"""
    if dry_run:
        print(f"[dry-run] upsert word_books {book['book_id']}")
    else:
        run_sql(token, ref, sql)


def upsert_modules(token: str, ref: str, book: dict, dry_run: bool) -> list[str]:
    """Insert one module per unit; returns list of module_ids in unit order."""
    module_ids: list[str] = []
    values = []
    for idx, unit in enumerate(book.get("units") or []):
        module_id = f"{book['book_id']}-u{idx + 1:02d}"
        module_ids.append(module_id)
        values.append(
            "("
            f"{sql_str(module_id)}, "
            f"{sql_str(book['book_id'])}, "
            f"{sql_str(unit.get('module_title') or unit.get('unit_ref') or f'Unit {idx + 1}')}, "
            f"{idx}, "
            f"{len(unit.get('words') or [])}"
            ")"
        )
    if not values:
        return module_ids
    sql = f"""
DELETE FROM public.word_book_modules WHERE book_id = {sql_str(book['book_id'])};
INSERT INTO public.word_book_modules (module_id, book_id, title, sort_order, word_count)
VALUES {',\n'.join(values)}
ON CONFLICT (module_id) DO UPDATE SET
    book_id = EXCLUDED.book_id,
    title = EXCLUDED.title,
    sort_order = EXCLUDED.sort_order,
    word_count = EXCLUDED.word_count;
"""
    if dry_run:
        print(f"[dry-run] upsert {len(values)} modules for {book['book_id']}")
    else:
        run_sql(token, ref, sql)
    return module_ids


def reward_for(diff: int) -> int:
    return round(diff / 4) + 2


def duration_for(diff: int) -> int:
    return 8 + diff // 6


def upsert_words(token: str, ref: str, book: dict, module_ids: list[str],
                 dry_run: bool) -> None:
    diff = int(book.get("grade_level") or 0)
    reward = reward_for(diff) if diff else 3
    duration = duration_for(diff) if diff else 8
    book_id = book["book_id"]
    values = []
    sort_global = 0
    for unit_idx, unit in enumerate(book.get("units") or []):
        module_id = module_ids[unit_idx] if unit_idx < len(module_ids) else None
        for word in unit.get("words") or []:
            english = word["english"].strip()
            if not english:
                continue
            word_id = f"{book_id}-{sort_global + 1:04d}"
            senses = word.get("senses") or [{
                "part_of_speech": "",
                "translation": word.get("translation") or english,
            }]
            phonetic = word.get("phonetic")
            pos = ""
            trans = ""
            if senses:
                pos = senses[0].get("part_of_speech") or ""
                trans = "；".join(
                    s.get("translation") or "" for s in senses if s.get("translation")
                )
            values.append(
                "("
                f"{sql_str(word_id)}, "
                f"{sql_str(book_id)}, "
                f"{sql_str(module_id)}, "
                f"{sql_str(english)}, "
                f"{sql_str(phonetic)}, "
                f"{sql_str(pos)}, "
                f"{sql_str(trans)}, "
                f"{sql_str(word.get('example_sentence'))}, "
                f"{sql_str('mixed')}, "
                f"{sql_int(diff)}, "
                f"{reward}, "
                f"{duration}, "
                f"{sort_global}, "
                "NULL, "
                f"{sql_jsonb(senses)}, "
                f"{sql_str(master_id_for(english))}"
                ")"
            )
            sort_global += 1
    if not values:
        return
    sql = f"""
DELETE FROM public.vocabulary_words WHERE book_id = {sql_str(book_id)};
INSERT INTO public.vocabulary_words (
    word_id, book_id, module_id, english, phonetic, part_of_speech,
    translation, example_sentence, difficulty_level, difficulty_value,
    reward_token, estimated_duration_sec, sort_order, audio_url, senses,
    master_id
) VALUES
{',\n'.join(values)}
ON CONFLICT (word_id) DO UPDATE SET
    book_id = EXCLUDED.book_id,
    module_id = EXCLUDED.module_id,
    english = EXCLUDED.english,
    phonetic = EXCLUDED.phonetic,
    part_of_speech = EXCLUDED.part_of_speech,
    translation = EXCLUDED.translation,
    example_sentence = EXCLUDED.example_sentence,
    difficulty_level = EXCLUDED.difficulty_level,
    difficulty_value = EXCLUDED.difficulty_value,
    reward_token = EXCLUDED.reward_token,
    estimated_duration_sec = EXCLUDED.estimated_duration_sec,
    sort_order = EXCLUDED.sort_order,
    senses = EXCLUDED.senses,
    master_id = EXCLUDED.master_id;
"""
    if dry_run:
        print(f"[dry-run] upsert {len(values)} vocabulary_words for {book_id}")
    else:
        run_sql(token, ref, sql)


def refresh_master(token: str, ref: str, dry_run: bool) -> None:
    """Recompute vocabulary_master from all vocabulary_words + word_books."""
    sql = """
WITH per_word AS (
    SELECT
        w.master_id,
        w.english,
        w.phonetic,
        w.senses,
        w.audio_url,
        b.grade_level
    FROM public.vocabulary_words w
    JOIN public.word_books b ON b.book_id = w.book_id
    WHERE w.master_id IS NOT NULL AND w.english IS NOT NULL AND w.english <> ''
),
agg AS (
    SELECT
        master_id,
        (array_agg(english ORDER BY english))[1] AS english,
        (array_agg(phonetic) FILTER (WHERE phonetic IS NOT NULL AND phonetic <> ''))[1] AS phonetic,
        (array_agg(senses) FILTER (WHERE senses IS NOT NULL AND jsonb_array_length(senses) > 0))[1] AS senses,
        (array_agg(audio_url) FILTER (WHERE audio_url IS NOT NULL AND audio_url <> ''))[1] AS audio_url,
        AVG(grade_level) FILTER (WHERE grade_level IS NOT NULL) AS avg_difficulty,
        COUNT(*) AS occurrences,
        bool_or(grade_level IS NOT NULL) AS is_textbook
    FROM per_word
    GROUP BY master_id
)
INSERT INTO public.vocabulary_master (
    master_id, english, phonetic, senses, audio_url,
    difficulty_value, occurrence_count, is_textbook, updated_at
)
SELECT
    master_id,
    english,
    phonetic,
    COALESCE(senses, '[]'::jsonb),
    audio_url,
    ROUND(avg_difficulty::numeric, 2),
    occurrences::int,
    is_textbook,
    EXTRACT(EPOCH FROM now())::bigint
FROM agg
ON CONFLICT (master_id) DO UPDATE SET
    english = EXCLUDED.english,
    phonetic = COALESCE(EXCLUDED.phonetic, public.vocabulary_master.phonetic),
    senses = CASE
        WHEN jsonb_array_length(EXCLUDED.senses) > 0 THEN EXCLUDED.senses
        ELSE public.vocabulary_master.senses
    END,
    audio_url = COALESCE(EXCLUDED.audio_url, public.vocabulary_master.audio_url),
    difficulty_value = EXCLUDED.difficulty_value,
    occurrence_count = EXCLUDED.occurrence_count,
    is_textbook = EXCLUDED.is_textbook,
    updated_at = EXCLUDED.updated_at;
"""
    if dry_run:
        print("[dry-run] refresh vocabulary_master")
    else:
        run_sql(token, ref, sql)


# ---------------------------------------------------------------------------
# Listening bundles
# ---------------------------------------------------------------------------


def upsert_listening(token: str, ref: str, env: dict[str, str], bundles: list[dict],
                     corpus_root: Path, dry_run: bool,
                     book_filter: str | None) -> None:
    if not bundles:
        return
    values = []
    for b in bundles:
        if book_filter and b.get("book_id") != book_filter:
            continue
        book_id = b["book_id"]
        unit_slug = slug(b.get("unit_ref") or "unit")
        section_slug = slug(b.get("section_ref") or "section")
        object_key = f"{book_id}/{unit_slug}/{section_slug}.mp3"
        src = b.get("source_relpath")
        audio_url: str | None = None
        if src:
            src_path = corpus_root / src
            if src_path.exists():
                audio_url = upload_object(env, object_key, src_path, dry_run)
            else:
                print(f"[warn] source missing: {src_path}", file=sys.stderr)
        values.append(
            "("
            f"{sql_str(b['material_id'])}, "
            f"{sql_str(b.get('title'))}, "
            f"{sql_str(b.get('title_zh'))}, "
            f"{sql_str(b.get('material_type') or 'dialogue')}, "
            f"{sql_str(b.get('prompt_text'))}, "
            f"{sql_str(b.get('transcript'))}, "
            f"{sql_str(audio_url)}, "
            f"{int(b.get('estimated_seconds') or 60)}, "
            f"{int(b.get('sort_order') or 0)}, "
            f"1, "
            f"EXTRACT(EPOCH FROM now())::bigint, "
            f"{sql_str(book_id)}, "
            f"{sql_int(b.get('grade_level'))}, "
            f"{sql_str(b.get('unit_ref'))}, "
            f"{sql_str(b.get('section_ref'))}"
            ")"
        )
    if not values:
        return
    sql = f"""
INSERT INTO public.listening_materials (
    material_id, title, title_zh, material_type, prompt_text, transcript,
    audio_url, estimated_seconds, sort_order, version, updated_at,
    book_id, grade_level, unit_ref, section_ref
) VALUES
{',\n'.join(values)}
ON CONFLICT (material_id) DO UPDATE SET
    title = EXCLUDED.title,
    title_zh = EXCLUDED.title_zh,
    material_type = EXCLUDED.material_type,
    prompt_text = EXCLUDED.prompt_text,
    transcript = EXCLUDED.transcript,
    audio_url = COALESCE(EXCLUDED.audio_url, public.listening_materials.audio_url),
    estimated_seconds = EXCLUDED.estimated_seconds,
    sort_order = EXCLUDED.sort_order,
    version = EXCLUDED.version,
    updated_at = EXCLUDED.updated_at,
    book_id = EXCLUDED.book_id,
    grade_level = EXCLUDED.grade_level,
    unit_ref = EXCLUDED.unit_ref,
    section_ref = EXCLUDED.section_ref;
"""
    if dry_run:
        print(f"[dry-run] upsert {len(values)} listening_materials rows")
    else:
        run_sql(token, ref, sql)


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--dry-run", action="store_true")
    parser.add_argument(
        "--only",
        choices=("words", "listening", "master", "all"),
        default="all",
    )
    parser.add_argument("--book-id", type=str, default=None)
    parser.add_argument("--corpus-root", type=Path, default=CORPUS_ROOT_DEFAULT)
    args = parser.parse_args()

    env = load_env()
    token = env.get("SUPABASE_ACCESS_TOKEN") or ""
    if not token and not args.dry_run:
        print("Missing SUPABASE_ACCESS_TOKEN in .env.local", file=sys.stderr)
        sys.exit(2)
    ref = project_ref(env) if not args.dry_run else "dry"

    if args.only in ("listening", "all"):
        ensure_bucket(env, args.dry_run)

    if args.only in ("words", "all"):
        if not WORD_JSON.exists():
            print(f"Missing {WORD_JSON}; run extract_pep_words.py first", file=sys.stderr)
        else:
            payload = json.loads(WORD_JSON.read_text(encoding="utf-8"))
            for book in payload.get("books", []):
                if args.book_id and book["book_id"] != args.book_id:
                    continue
                book.setdefault("version", int(time.strftime("%Y%m%d") + "01"))
                print(f"Upserting {book['book_id']} …")
                upsert_book(token, ref, book, args.dry_run)
                module_ids = upsert_modules(token, ref, book, args.dry_run)
                upsert_words(token, ref, book, module_ids, args.dry_run)

    if args.only in ("listening", "all"):
        if not LISTEN_JSON.exists():
            print(f"Missing {LISTEN_JSON}; run extract_pep_listening.py first", file=sys.stderr)
        else:
            payload = json.loads(LISTEN_JSON.read_text(encoding="utf-8"))
            upsert_listening(
                token=token,
                ref=ref,
                env=env,
                bundles=payload.get("bundles", []),
                corpus_root=args.corpus_root,
                dry_run=args.dry_run,
                book_filter=args.book_id,
            )

    if args.only in ("master", "all"):
        refresh_master(token, ref, args.dry_run)

    if not args.dry_run:
        verify = run_sql(
            token,
            ref,
            """
SELECT
  (SELECT COUNT(*) FROM public.word_books WHERE book_id LIKE 'pep-%') AS pep_books,
  (SELECT COUNT(*) FROM public.vocabulary_words w
     JOIN public.word_books b ON b.book_id = w.book_id
    WHERE b.book_id LIKE 'pep-%') AS pep_words,
  (SELECT COUNT(*) FROM public.vocabulary_master) AS master_rows,
  (SELECT COUNT(*) FROM public.listening_materials
    WHERE book_id LIKE 'pep-%') AS pep_listening_rows;
""",
        )
        print("Verify:", verify)


if __name__ == "__main__":
    main()
