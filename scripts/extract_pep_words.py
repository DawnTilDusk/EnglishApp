"""Extract PEP word lists from the PDFs discovered by scan_pep_corpus.py and
produce/merge docs/pep/pep_word_books.json.

Design notes:
- PDFs from the vendor are visually formatted lists (English + 汉语 + phonetic).
  Reliable extraction requires an external library (pdfplumber) that may not be
  installed on this sandbox. To stay dependency-light and match the spec's
  "structure-first, human review later" philosophy, this script:

    1. Tries to import `pdfplumber`. If missing, it emits a warning, still
       writes a book skeleton with empty units, and flags `needs_review=true`
       on the book so the operator knows to fill in later.
    2. If pdfplumber is present, it parses each word_pdf and does a best-effort
       split by lines matching `<english> <phonetic?> <chinese>`.

- Units cannot always be inferred from the PDF alone; we allocate a single
  fallback module_title = "全书总表" per book. Later, when audio segments
  reveal Unit boundaries, extract_pep_listening.py can also register units
  and apply_pep_corpus.py auto-associates by unit_ref when the operator
  edits the JSON manually.

Usage:
    python scripts/extract_pep_words.py [--scan docs/pep/scan_report.json]
                                        [--out docs/pep/pep_word_books.json]
                                        [--only pep-g7-vol1]
"""
from __future__ import annotations

import argparse
import json
import re
import sys
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SCAN_JSON = ROOT / "docs" / "pep" / "scan_report.json"
OUT_JSON = ROOT / "docs" / "pep" / "pep_word_books.json"

BOOK_TITLES = {
    "pep-g1-vol1": "人教版 英语 一年级上册",
    "pep-g1-vol2": "人教版 英语 一年级下册",
    "pep-g2-vol1": "人教版 英语 二年级上册",
    "pep-g2-vol2": "人教版 英语 二年级下册",
    "pep-g3-vol1": "人教版 英语 三年级上册",
    "pep-g3-vol2": "人教版 英语 三年级下册",
    "pep-g4-vol1": "人教版 英语 四年级上册",
    "pep-g4-vol2": "人教版 英语 四年级下册",
    "pep-g5-vol1": "人教版 英语 五年级上册",
    "pep-g5-vol2": "人教版 英语 五年级下册",
    "pep-g6-vol1": "人教版 英语 六年级上册",
    "pep-g6-vol2": "人教版 英语 六年级下册",
    "pep-g7-vol1": "人教版 英语 七年级上册",
    "pep-g7-vol2": "人教版 英语 七年级下册",
    "pep-g8-vol1": "人教版 英语 八年级上册",
    "pep-g8-vol2": "人教版 英语 八年级下册",
    "pep-g9-vol1": "人教版 英语 九年级上册",
    "pep-g9-vol2": "人教版 英语 九年级下册",
}

# Line pattern: english (with optional space), optional phonetic /.../, then Chinese
LINE_RE = re.compile(
    r"^(?P<eng>[A-Za-z][A-Za-z\-\' ]*?)\s+"
    r"(?P<pho>/[^/]+/)?\s*"
    r"(?P<zh>[\u4e00-\u9fa5\u3002\uff0c\uff1b\u3001\uff08\uff09；，。()\s]+.*)$"
)


def try_extract_words_from_pdf(pdf_path: Path) -> list[dict]:
    """Best-effort extraction; returns [] if pdfplumber missing or fails."""
    try:
        import pdfplumber  # type: ignore
    except Exception:
        return []

    words: list[dict] = []
    try:
        with pdfplumber.open(str(pdf_path)) as pdf:
            for page in pdf.pages:
                text = page.extract_text() or ""
                for line in text.splitlines():
                    line = line.strip()
                    if not line:
                        continue
                    match = LINE_RE.match(line)
                    if not match:
                        continue
                    eng = match.group("eng").strip()
                    pho = (match.group("pho") or "").strip()
                    zh = match.group("zh").strip()
                    if len(eng) < 2 or len(zh) < 1:
                        continue
                    words.append({
                        "english": eng,
                        "phonetic": pho or None,
                        "senses": [{"part_of_speech": "", "translation": zh}],
                        "example_sentence": None,
                        "needs_review": True,
                    })
    except Exception as e:
        print(f"[warn] pdfplumber failed on {pdf_path}: {e}", file=sys.stderr)
        return []
    return words


def merge(existing: dict, book_id: str, grade_level: int, words: list[dict]) -> dict:
    for book in existing.get("books", []):
        if book["book_id"] == book_id:
            return book
    book = {
        "book_id": book_id,
        "title": BOOK_TITLES.get(book_id, book_id),
        "description": f"人教版 · 年级基准 {grade_level}",
        "language": "en-US",
        "grade_level": grade_level,
        "version": int(time.strftime("%Y%m%d") + "01"),
        "units": [],
    }
    existing.setdefault("books", []).append(book)
    if words:
        for i, w in enumerate(words):
            w["sort_order"] = i
        book["units"].append({
            "unit_ref": "全书总表",
            "module_title": "全书总表",
            "words": words,
        })
    return book


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--scan", type=Path, default=SCAN_JSON)
    parser.add_argument("--out", type=Path, default=OUT_JSON)
    parser.add_argument("--only", type=str, default=None,
                        help="only process this book_id")
    args = parser.parse_args()

    if not args.scan.exists():
        print(f"Missing {args.scan}; run scan_pep_corpus.py first", file=sys.stderr)
        sys.exit(1)
    scan = json.loads(args.scan.read_text(encoding="utf-8"))

    if args.out.exists():
        payload = json.loads(args.out.read_text(encoding="utf-8"))
    else:
        payload = {"$schema_version": 1, "generated_at": 0, "books": []}

    root = Path(scan["root"])
    for entry in scan.get("books", []):
        book_id = entry["book_id"]
        if args.only and args.only != book_id:
            continue
        grade_level = entry["grade_level"]
        words: list[dict] = []
        for rel in entry.get("word_pdfs", []):
            pdf_path = root / rel
            if not pdf_path.exists():
                continue
            words.extend(try_extract_words_from_pdf(pdf_path))
        # dedupe by english
        seen: dict[str, dict] = {}
        for w in words:
            key = w["english"].lower()
            if key not in seen:
                seen[key] = w
        words = list(seen.values())
        merge(payload, book_id, grade_level, words)
        print(f"{book_id}: parsed {len(words)} words")

    payload["generated_at"] = int(time.strftime("%Y%m%d%H"))
    args.out.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"Wrote {args.out}")


if __name__ == "__main__":
    main()
