"""Assemble docs/pep/pep_listening_bundles.json from scan_report.json (audio
segments + listening docx) and optional transcript files.

Rules:
- Each segment mp3 → one bundle row (transcript initially null).
- If a matching *.docx exists next to segments and provides a unit-1 transcript,
  we pattern-match its lines against bundles with unit_ref='Unit 1' and copy
  the transcript over. Any unmatched transcript blocks are left in
  book.raw_docx_transcripts for the operator to place manually.

Usage:
    python scripts/extract_pep_listening.py [--only pep-g7-vol1]
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
OUT_JSON = ROOT / "docs" / "pep" / "pep_listening_bundles.json"


def slug(text: str) -> str:
    t = re.sub(r"[^A-Za-z0-9]+", "-", text.lower()).strip("-")
    return re.sub(r"-+", "-", t)


def build_material_id(book_id: str, unit_ref: str, section_ref: str) -> str:
    return f"{book_id}-{slug(unit_ref)}-{slug(section_ref)}"


def try_read_docx_text(docx_path: Path) -> str:
    try:
        from zipfile import ZipFile
        import xml.etree.ElementTree as ET
    except Exception:
        return ""
    try:
        with ZipFile(str(docx_path)) as z:
            with z.open("word/document.xml") as fp:
                tree = ET.parse(fp)
        ns = {"w": "http://schemas.openxmlformats.org/wordprocessingml/2006/main"}
        lines: list[str] = []
        for para in tree.iter("{%s}p" % ns["w"]):
            texts = [t.text or "" for t in para.iter("{%s}t" % ns["w"])]
            line = "".join(texts).strip()
            if line:
                lines.append(line)
        return "\n".join(lines)
    except Exception as e:
        print(f"[warn] failed to read {docx_path}: {e}", file=sys.stderr)
        return ""


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--scan", type=Path, default=SCAN_JSON)
    parser.add_argument("--out", type=Path, default=OUT_JSON)
    parser.add_argument("--only", type=str, default=None)
    args = parser.parse_args()

    if not args.scan.exists():
        print(f"Missing {args.scan}", file=sys.stderr)
        sys.exit(1)
    scan = json.loads(args.scan.read_text(encoding="utf-8"))
    root = Path(scan["root"])

    if args.out.exists():
        payload = json.loads(args.out.read_text(encoding="utf-8"))
    else:
        payload = {"$schema_version": 1, "generated_at": 0, "bundles": []}
    bundles: list[dict] = payload.setdefault("bundles", [])

    existing_ids = {b["material_id"] for b in bundles}

    for entry in scan.get("books", []):
        book_id = entry["book_id"]
        if args.only and args.only != book_id:
            continue
        grade_level = entry["grade_level"]

        # Optional docx transcripts (raw text saved on the book for later manual placement)
        raw_docx: list[dict] = []
        for rel in entry.get("listening_docs", []):
            docx_path = root / rel
            if not docx_path.exists():
                continue
            text = try_read_docx_text(docx_path)
            if text:
                raw_docx.append({"source_relpath": rel, "text": text})

        # Add / update segment bundles
        sort_order = 0
        for seg in entry.get("audio_segments", []):
            unit_ref = seg.get("unit_ref") or "Unit"
            section_ref = seg.get("section_ref") or seg["name"]
            material_id = build_material_id(book_id, unit_ref, section_ref)
            if material_id in existing_ids:
                continue
            bundles.append({
                "material_id": material_id,
                "book_id": book_id,
                "grade_level": grade_level,
                "unit_ref": unit_ref,
                "section_ref": section_ref,
                "material_type": "dialogue",
                "title": f"{unit_ref} · {section_ref}",
                "title_zh": None,
                "prompt_text": "Listen to the audio and follow along.",
                "transcript": None,
                "source_relpath": seg["relpath"],
                "estimated_seconds": 45,
                "sort_order": sort_order,
                "needs_review": True,
                "raw_docx_transcripts": raw_docx if sort_order == 0 else None,
            })
            existing_ids.add(material_id)
            sort_order += 1
        print(f"{book_id}: {sort_order} segment bundles registered")

    payload["generated_at"] = int(time.strftime("%Y%m%d%H"))
    args.out.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"Wrote {args.out}")


if __name__ == "__main__":
    main()
