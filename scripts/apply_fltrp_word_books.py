"""Apply docs/words/fltrp_junior_words.json to remote Supabase word catalog.

Clears old word books / related user progress, upserts six FLTRP books,
prints frozen grade-band quotas for Kotlin constants.
"""
from __future__ import annotations

import json
import os
import sys
import urllib.error
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ENV_FILE = ROOT / ".env.local"
JSON_PATH = ROOT / "docs" / "words" / "fltrp_junior_words.json"
PROJECT_REF = "xojbnrxnkgkqacrkcmqc"
BATCH = 80

KEEP_BOOK_IDS = {
    "fltrp-g7-vol1",
    "fltrp-g7-vol2",
    "fltrp-g8-vol1",
    "fltrp-g8-vol2",
    "fltrp-g9-vol1",
    "fltrp-g9-vol2",
}


def load_env() -> dict[str, str]:
    values: dict[str, str] = {}
    if ENV_FILE.exists():
        for line in ENV_FILE.read_text(encoding="utf-8").splitlines():
            line = line.strip()
            if not line or line.startswith("#") or "=" not in line:
                continue
            key, _, value = line.partition("=")
            values[key.strip()] = value.strip().strip('"').strip("'")
    for key in ("SUPABASE_ACCESS_TOKEN", "SUPABASE_URL"):
        values.setdefault(key, os.environ.get(key, ""))
    return values


def project_ref(env: dict[str, str]) -> str:
    ref = PROJECT_REF
    url = env.get("SUPABASE_URL", "")
    if "://" in url:
        host = url.split("://", 1)[1].split("/", 1)[0]
        if host.endswith(".supabase.co"):
            ref = host.split(".")[0]
    return ref


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
            "User-Agent": "Mozilla/5.0 SeedieFltrpImport/1.0",
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


def clear_old_catalog(token: str, ref: str) -> None:
    keep = ", ".join(sql_str(x) for x in sorted(KEEP_BOOK_IDS))
    sql = f"""
-- Clear progress for books that will be removed or replaced
DELETE FROM public.user_vocabulary_word_learning_progress
WHERE book_id NOT IN ({keep});
DELETE FROM public.user_vocabulary_study_rounds
WHERE book_id NOT IN ({keep});
DELETE FROM public.user_vocabulary_book_progress
WHERE book_id NOT IN ({keep});

-- Wipe all catalog rows then re-insert FLTRP six
DELETE FROM public.vocabulary_words;
DELETE FROM public.word_book_modules;
DELETE FROM public.word_books;

UPDATE public.profiles
SET vocabulary_size = 0,
    vocabulary_estimated_at = NULL
WHERE TRUE;
"""
    print("Clearing old catalog + progress...")
    print(run_sql(token, ref, sql))


def upsert_book(token: str, ref: str, book: dict) -> None:
    sql = f"""
INSERT INTO public.word_books (
    book_id, title, description, language, difficulty, version,
    word_count, cover_url, updated_at
) VALUES (
    {sql_str(book["book_id"])},
    {sql_str(book["title"])},
    {sql_str(book["description"])},
    {sql_str(book["language"])},
    {sql_str(book["difficulty"])},
    {int(book["version"])},
    {int(book["word_count"])},
    NULL,
    {int(book["version"])}
)
ON CONFLICT (book_id) DO UPDATE SET
    title = EXCLUDED.title,
    description = EXCLUDED.description,
    language = EXCLUDED.language,
    difficulty = EXCLUDED.difficulty,
    version = EXCLUDED.version,
    word_count = EXCLUDED.word_count,
    cover_url = EXCLUDED.cover_url,
    updated_at = EXCLUDED.updated_at;

INSERT INTO public.word_book_modules (
    module_id, book_id, title, sort_order, word_count
) VALUES (
    {sql_str(book["module_id"])},
    {sql_str(book["book_id"])},
    {sql_str(book["module_title"])},
    0,
    {int(book["word_count"])}
)
ON CONFLICT (module_id) DO UPDATE SET
    book_id = EXCLUDED.book_id,
    title = EXCLUDED.title,
    sort_order = EXCLUDED.sort_order,
    word_count = EXCLUDED.word_count;
"""
    run_sql(token, ref, sql)


def sql_jsonb(value: object) -> str:
    return f"{sql_str(json.dumps(value, ensure_ascii=False))}::jsonb"


def ensure_senses_column(token: str, ref: str) -> None:
    print(
        run_sql(
            token,
            ref,
            """
ALTER TABLE public.vocabulary_words
  ADD COLUMN IF NOT EXISTS senses JSONB NOT NULL DEFAULT '[]'::jsonb;
""",
        )
    )


def upsert_words_batch(token: str, ref: str, book: dict, start: int, end: int) -> None:
    words = book["words"][start:end]
    if not words:
        return
    level = book["difficulty_level_default"]
    book_id = book["book_id"]
    module_id = book["module_id"]
    values = []
    for offset, word in enumerate(words):
        sort_order = start + offset
        word_id = f"{book_id}-{sort_order + 1:04d}"
        reward = 3 if level == "easy" else 4 if level == "medium" else 5
        duration = 8 if level == "easy" else 10 if level == "medium" else 12
        senses = word.get("senses") or [
            {
                "part_of_speech": word.get("part_of_speech") or "n.",
                "translation": word.get("translation") or word["english"],
            }
        ]
        values.append(
            "("
            f"{sql_str(word_id)}, "
            f"{sql_str(book_id)}, "
            f"{sql_str(module_id)}, "
            f"{sql_str(word['english'])}, "
            f"{sql_str(word.get('phonetic'))}, "
            f"{sql_str(word.get('part_of_speech'))}, "
            f"{sql_str(word['translation'])}, "
            f"{sql_str(word.get('example_sentence'))}, "
            f"{sql_str(level)}, "
            f"{reward}, "
            f"{duration}, "
            f"{sort_order}, "
            "NULL, "
            f"{sql_jsonb(senses)}"
            ")"
        )
    sql = f"""
INSERT INTO public.vocabulary_words (
    word_id, book_id, module_id, english, phonetic, part_of_speech,
    translation, example_sentence, difficulty_level, reward_token,
    estimated_duration_sec, sort_order, audio_url, senses
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
    reward_token = EXCLUDED.reward_token,
    estimated_duration_sec = EXCLUDED.estimated_duration_sec,
    sort_order = EXCLUDED.sort_order,
    audio_url = EXCLUDED.audio_url,
    senses = EXCLUDED.senses;
"""
    run_sql(token, ref, sql)


def main() -> None:
    env = load_env()
    token = env.get("SUPABASE_ACCESS_TOKEN", "")
    if not token:
        print("Missing SUPABASE_ACCESS_TOKEN in .env.local")
        sys.exit(2)
    if not JSON_PATH.exists():
        print(f"Missing {JSON_PATH}; run extract_fltrp_words.py first")
        sys.exit(1)

    payload = json.loads(JSON_PATH.read_text(encoding="utf-8"))
    ref = project_ref(env)
    print(f"Project ref: {ref}")
    print(run_sql(token, ref, "select 1 as ok;"))
    ensure_senses_column(token, ref)

    inplace = "--inplace" in sys.argv
    if not inplace:
        clear_old_catalog(token, ref)
    else:
        print("In-place upsert (keep existing books/progress)...")

    for book in payload["books"]:
        # bump version so clients re-download content
        book["version"] = int(book.get("version") or 2026080401)
        if inplace:
            book["version"] = max(book["version"], 2026080402)
        print(f"Upserting {book['book_id']} ({book['word_count']} words)...")
        upsert_book(token, ref, book)
        n = len(book["words"])
        for start in range(0, n, BATCH):
            end = min(start + BATCH, n)
            upsert_words_batch(token, ref, book, start, end)
            print(f"  words {start + 1}-{end}")

    verify = run_sql(
        token,
        ref,
        """
SELECT book_id, word_count,
       (SELECT COUNT(*) FROM public.vocabulary_words w WHERE w.book_id = b.book_id) AS actual,
       (SELECT COUNT(*) FROM public.vocabulary_words w
         WHERE w.book_id = b.book_id AND jsonb_array_length(w.senses) > 0) AS with_senses
FROM public.word_books b
ORDER BY book_id;
""",
    )
    print("Verify:", verify)

    sample = run_sql(
        token,
        ref,
        """
SELECT english, part_of_speech, translation, senses
FROM public.vocabulary_words
WHERE english IN ('change', 'north', 'hello')
ORDER BY english, book_id
LIMIT 6;
""",
    )
    print("Sample:", sample)

    quotas = payload.get("quota_list", [])
    print("Frozen quotas (Kotlin):", quotas)


if __name__ == "__main__":
    main()
