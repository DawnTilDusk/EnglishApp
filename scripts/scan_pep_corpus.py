"""Scan the local PEP textbook corpus folder and emit docs/pep/scan_report.json.

Usage:
    python scripts/scan_pep_corpus.py [--root <path>]

Default root: C:\\Users\\zhong\\Desktop\\Eng资料

For every book folder we recognise, we output:
    - book_id (e.g. pep-g7-vol1)
    - grade_level (13..18)
    - source root
    - grouped file lists: word_pdfs / audio_full / audio_segments / listening_docs / textbooks
    - heuristic mapping of `细分版` mp3 → {unit_ref, section_ref}

The output is a plain JSON dictionary; downstream extract_* scripts consume it.
"""
from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path

DEFAULT_ROOT = Path(r"C:\Users\zhong\Desktop\Eng资料")

# grade folder (e.g. "7上") → (grade, volume, book_id, grade_level)
GRADE_MAP = {
    "1上": ("g1", 1, "pep-g1-vol1", 1),
    "1下": ("g1", 2, "pep-g1-vol2", 2),
    "2上": ("g2", 1, "pep-g2-vol1", 3),
    "2下": ("g2", 2, "pep-g2-vol2", 4),
    "3上": ("g3", 1, "pep-g3-vol1", 5),
    "3下": ("g3", 2, "pep-g3-vol2", 6),
    "4上": ("g4", 1, "pep-g4-vol1", 7),
    "4下": ("g4", 2, "pep-g4-vol2", 8),
    "5上": ("g5", 1, "pep-g5-vol1", 9),
    "5下": ("g5", 2, "pep-g5-vol2", 10),
    "6上": ("g6", 1, "pep-g6-vol1", 11),
    "6下": ("g6", 2, "pep-g6-vol2", 12),
    "7上": ("g7", 1, "pep-g7-vol1", 13),
    "7下": ("g7", 2, "pep-g7-vol2", 14),
    "8上": ("g8", 1, "pep-g8-vol1", 15),
    "8下": ("g8", 2, "pep-g8-vol2", 16),
    "9上": ("g9", 1, "pep-g9-vol1", 17),
    "9下": ("g9", 2, "pep-g9-vol2", 18),
}

# Regex for segment filenames like "01 P2 8上Unit1SectionA1b.mp3"
# or "23-7下-Unit-3-Setion-B-1b.mp3"
SEG_RE_A = re.compile(
    r"(?P<idx>\d+)\s+P?(?P<page>\d+)?\s*\d?[上下]?"
    r"(?P<unit_kind>SU|Unit)(?P<unit>\d+)"
    r"(?P<section>SectionA|SectionB|SecA|SecB|Section-A|Section-B|SetionB|SecionB|Pronunciation|PR)"
    r"(?P<tail>[A-Za-z0-9]*)",
    re.IGNORECASE,
)
SEG_RE_B = re.compile(
    r"(?P<idx>\d+)[-_ ]\d?[上下][-_ ](?:Unit|SU)[-_ ]?(?P<unit>\d+)"
    r"[-_ ]?(?P<section>Section[-_ ]?[AB]?|Set[io]n[-_ ]?[AB]|Pronunciation|Vocabulary|PR)"
    r"[-_ ]?(?P<tail>[A-Za-z0-9]*)",
    re.IGNORECASE,
)


def classify_segment(name: str) -> dict | None:
    stem = Path(name).stem
    match = SEG_RE_A.search(stem) or SEG_RE_B.search(stem)
    if not match:
        return None
    unit_kind = (match.groupdict().get("unit_kind") or "Unit").lower()
    unit_num = int(match.group("unit"))
    section_raw = match.group("section").lower().replace("-", "").replace("_", "").replace(" ", "")
    section_raw = section_raw.replace("setion", "section").replace("secion", "section")
    tail = (match.groupdict().get("tail") or "").strip()

    if section_raw.startswith("sectiona") or section_raw == "seca":
        section_prefix = "Section A"
    elif section_raw.startswith("sectionb") or section_raw == "secb":
        section_prefix = "Section B"
    elif section_raw == "pronunciation" or section_raw == "pr":
        section_prefix = "Pronunciation"
    elif section_raw == "vocabulary":
        section_prefix = "Vocabulary"
    else:
        section_prefix = section_raw.title() or "Section"

    if unit_kind == "su":
        unit_ref = f"Starter Unit {unit_num}"
    else:
        unit_ref = f"Unit {unit_num}"

    section_ref = section_prefix if not tail else f"{section_prefix} {tail}"
    return {
        "index_hint": int(match.group("idx")),
        "unit_ref": unit_ref,
        "section_ref": section_ref,
    }


def slug(text: str) -> str:
    text = re.sub(r"[^A-Za-z0-9]+", "-", text.lower()).strip("-")
    return re.sub(r"-+", "-", text)


def scan_book(root: Path, folder: Path, book_id: str, grade_level: int) -> dict:
    entry: dict = {
        "book_id": book_id,
        "grade_level": grade_level,
        "source_root": str(folder),
        "word_pdfs": [],
        "textbook_pdfs": [],
        "audio_full": [],
        "audio_segments": [],
        "listening_docs": [],
        "other_files": [],
    }
    for path in folder.rglob("*"):
        if not path.is_file():
            continue
        rel = str(path.relative_to(root))
        suffix = path.suffix.lower()
        name = path.name
        if suffix == ".pdf":
            lower = name.lower()
            if "单词" in name or "词汇" in name or "vocab" in lower:
                entry["word_pdfs"].append(rel)
            elif "教科书" in name or "教材" in name or "课程标准" in name:
                entry["textbook_pdfs"].append(rel)
            else:
                entry["textbook_pdfs"].append(rel)
        elif suffix == ".mp3":
            hint = classify_segment(name)
            record = {"relpath": rel, "name": name}
            if hint:
                record.update(hint)
                entry["audio_segments"].append(record)
            else:
                entry["audio_full"].append(record)
        elif suffix in {".docx", ".doc"}:
            entry["listening_docs"].append(rel)
        else:
            entry["other_files"].append(rel)
    entry["audio_segments"].sort(key=lambda x: x.get("index_hint", 0))
    entry["audio_full"].sort(key=lambda x: x["name"])
    return entry


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, default=DEFAULT_ROOT)
    parser.add_argument(
        "--out",
        type=Path,
        default=Path(__file__).resolve().parents[1] / "docs" / "pep" / "scan_report.json",
    )
    args = parser.parse_args()

    if not args.root.exists():
        print(f"Root not found: {args.root}", file=sys.stderr)
        sys.exit(1)

    books: list[dict] = []
    for grade_folder in sorted(args.root.iterdir()):
        if not grade_folder.is_dir():
            continue
        key = grade_folder.name
        if key not in GRADE_MAP:
            continue
        _, _, book_id, grade_level = GRADE_MAP[key]
        entry = scan_book(args.root, grade_folder, book_id, grade_level)
        books.append(entry)

    report = {
        "$schema_version": 1,
        "root": str(args.root),
        "book_count": len(books),
        "books": books,
    }
    args.out.parent.mkdir(parents=True, exist_ok=True)
    args.out.write_text(
        json.dumps(report, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )
    print(f"Wrote {args.out}  ({len(books)} books)")


if __name__ == "__main__":
    main()
