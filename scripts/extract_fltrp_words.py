"""Extract FLTRP junior word list from .doc into reproducible JSON.

Requires Microsoft Word + pywin32 on Windows for the extract step.
"""
from __future__ import annotations

import json
import re
import sys
from datetime import datetime, timezone
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DOC_PATH = ROOT / "docs" / "words" / "外研版初中英语单词一览表(音标版).doc"
OUT_PATH = ROOT / "docs" / "words" / "fltrp_junior_words.json"
ESTIMATE_CAP = 1800

BOOKS = [
    (
        "g7a",
        "fltrp-g7-vol1",
        "外研版 七年级上册",
        "easy",
        r"外研新版七年级上册英语单词表",
    ),
    (
        "g7b",
        "fltrp-g7-vol2",
        "外研版 七年级下册",
        "easy",
        r"新版外研社英语七年级\(下\)词汇表|外研社英语七年级\(下\)词汇表",
    ),
    (
        "g8a",
        "fltrp-g8-vol1",
        "外研版 八年级上册",
        "medium",
        r"外语教研版八年级英语上册单词表|八年级英语上册单词表",
    ),
    (
        "g8b",
        "fltrp-g8-vol2",
        "外研版 八年级下册",
        "medium",
        r"外研版八年级英语下册单词表",
    ),
    (
        "g9a",
        "fltrp-g9-vol1",
        "外研版 九年级上册",
        "hard",
        r"外研版九年级上册英语单词表",
    ),
    (
        "g9b",
        "fltrp-g9-vol2",
        "外研版 九年级下册",
        "hard",
        r"外研版九年级英语下册词汇表",
    ),
]

ENTRY_RE = re.compile(
    r"^(?P<eng>.+?)\s*(?:\[(?P<p1>[^\]]+)\]|/(?P<p2>[^/]+)/)\s*(?P<rest>.*)$"
)
POS_REST_RE = re.compile(
    r"^(?P<pos>n|v|adj|adv|prep|pron|conj|interj|num|art|aux|det|modal)\.?\s+(?P<tr>.+)$",
    re.I,
)
POS_GLUED_RE = re.compile(
    r"^(?P<pos>n|v|adj|adv|prep|pron|conj|interj|num|art)\.(?P<tr>\S.*)$",
    re.I,
)
HEADER_RE = re.compile(r"^(Module|Starter|外研|外语教研|新版外研)", re.I)


def extract_doc_text(path: Path) -> str:
    import win32com.client

    word = win32com.client.Dispatch("Word.Application")
    word.Visible = False
    try:
        doc = word.Documents.Open(str(path), ReadOnly=True)
        text = doc.Content.Text
        doc.Close(False)
        return text
    finally:
        word.Quit()


def parse_chunk(chunk: str) -> list[dict]:
    lines = [line.strip() for line in re.split(r"[\r\n]+", chunk) if line.strip()]
    entries: list[dict] = []
    seen: set[str] = set()
    for line in lines:
        if HEADER_RE.match(line):
            continue
        match = ENTRY_RE.match(line)
        if not match:
            continue
        eng = re.sub(r"\s+", " ", match.group("eng")).strip(" .,;:，；\t")
        if match.group("p1"):
            phonetic = f"[{match.group('p1').strip()}]"
        else:
            phonetic = f"/{match.group('p2').strip()}/"
        rest = (match.group("rest") or "").strip()
        pos = None
        pos_match = POS_REST_RE.match(rest)
        if pos_match:
            pos = pos_match.group("pos").lower() + "."
            translation = pos_match.group("tr").strip()
        else:
            glued = POS_GLUED_RE.match(rest)
            if glued:
                pos = glued.group("pos").lower() + "."
                translation = glued.group("tr").strip()
            else:
                translation = rest
        translation = re.sub(r"\s+", " ", translation).strip(" \t;；")
        if not eng or not translation:
            continue
        key = eng.lower()
        if key in seen:
            continue
        seen.add(key)
        entries.append(
            {
                "english": eng,
                "phonetic": phonetic or None,
                "part_of_speech": pos,
                "translation": translation,
                "example_sentence": None,
            }
        )
    return entries


def compute_quotas(counts: list[int], cap: int = ESTIMATE_CAP) -> list[int]:
    total = sum(counts)
    if total <= 0:
        raise ValueError("empty word counts")
    raw = [cap * count / total for count in counts]
    quotas = [int(x) for x in raw]
    remainders = sorted(
        range(len(counts)),
        key=lambda i: raw[i] - quotas[i],
        reverse=True,
    )
    diff = cap - sum(quotas)
    for i in range(diff):
        quotas[remainders[i % len(counts)]] += 1
    # Guard against overshoot from edge cases
    while sum(quotas) > cap:
        for i in sorted(range(len(quotas)), key=lambda j: quotas[j], reverse=True):
            if quotas[i] > 1:
                quotas[i] -= 1
                if sum(quotas) == cap:
                    break
    while sum(quotas) < cap:
        for i in remainders:
            quotas[i] += 1
            if sum(quotas) == cap:
                break
    return quotas


def main() -> None:
    if not DOC_PATH.exists():
        print(f"Missing source doc: {DOC_PATH}", file=sys.stderr)
        sys.exit(1)

    text = extract_doc_text(DOC_PATH)
    markers: list[tuple[int, str, str, str, str]] = []
    for band, book_id, title, diff, pat in BOOKS:
        match = re.search(pat, text)
        if not match:
            print(f"Missing section for {band}", file=sys.stderr)
            sys.exit(1)
        markers.append((match.start(), band, book_id, title, diff))
    markers.sort()

    books_out = []
    for i, (pos, band, book_id, title, diff) in enumerate(markers):
        end = markers[i + 1][0] if i + 1 < len(markers) else len(text)
        words = parse_chunk(text[pos:end])
        books_out.append(
            {
                "band": band,
                "book_id": book_id,
                "title": title,
                "description": f"{title}词汇（外研版初中）",
                "language": "en-GB",
                "difficulty": band,
                "difficulty_level_default": diff,
                "version": 2026080401,
                "module_id": f"{book_id}-m1",
                "module_title": "全册单词",
                "words": words,
                "word_count": len(words),
            }
        )
        print(f"{band} {book_id}: {len(words)} words")

    counts = [book["word_count"] for book in books_out]
    quotas = compute_quotas(counts)
    payload = {
        "source": DOC_PATH.name,
        "generated_at": datetime.now(timezone.utc).isoformat(),
        "estimate_cap": ESTIMATE_CAP,
        "quotas": {books_out[i]["band"]: quotas[i] for i in range(len(books_out))},
        "quota_list": quotas,
        "books": books_out,
    }
    OUT_PATH.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"quotas={quotas} sum={sum(quotas)}")
    print(f"wrote {OUT_PATH} ({OUT_PATH.stat().st_size} bytes)")


if __name__ == "__main__":
    main()
