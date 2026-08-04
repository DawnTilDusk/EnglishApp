"""Enrich FLTRP junior words JSON with structured senses.

Reads docs/words/fltrp_junior_words.json (fresh extract without senses preferred),
writes senses onto each word, backfills part_of_speech/translation, prints a report.
"""
from __future__ import annotations

import json
import re
import sys
from datetime import datetime, timezone
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JSON_PATH = ROOT / "docs" / "words" / "fltrp_junior_words.json"
REPORT_PATH = ROOT / "docs" / "words" / "fltrp_senses_enrich_report.txt"

POS_ALIASES = {
    "n": "n.",
    "v": "v.",
    "vt": "v.",
    "vi": "v.",
    "adj": "adj.",
    "adv": "adv.",
    "prep": "prep.",
    "pron": "pron.",
    "conj": "conj.",
    "interj": "interj.",
    "num": "num.",
    "art": "art.",
    "aux": "aux.",
    "det": "det.",
    "modal": "aux.",
    "phr": "phr.",
    "phrase": "phr.",
    "t": "v.",  # show t. → transitive typo residue
}

POS_TOKEN_RE = re.compile(
    r"^\s*(?:&+\s*)?(?P<pos>v\.\s*aux\.|n|v|vt|vi|adj|adv|prep|pron|conj|interj|num|art|aux|det|modal|phr|phrase|t)\.?\s*",
    re.I,
)

# Split points for mid-string POS: "; adj." / "；n." / " adj." / "&adv."
MID_POS_SPLIT_RE = re.compile(
    r"(?:^|[\s;；,，/]|&)(?=(?:&+\s*)?(?:v\.\s*aux\.|n|v|vt|vi|adj|adv|prep|pron|conj|interj|num|art|aux|det|modal|phr|t)\.?\s*)",
    re.I,
)

POS_IN_ZH_RE = re.compile(
    r"(^|[\s;；])(?:n|v|adj|adv|prep|pron|conj|aux|art|num|interj|phr)\.",
    re.I,
)

PRONOUNS = {
    "i", "me", "my", "mine", "myself", "we", "us", "our", "ours", "ourselves",
    "you", "your", "yours", "yourself", "yourselves", "he", "him", "his", "himself",
    "she", "her", "hers", "herself", "it", "its", "itself", "they", "them", "their",
    "theirs", "themselves", "this", "that", "these", "those", "who", "whom", "whose",
    "which", "what", "someone", "somebody", "something", "anyone", "anybody",
    "anything", "everyone", "everybody", "everything", "nobody", "nothing",
}
ARTICLES = {"a", "an", "the"}
PREPS = {
    "in", "on", "at", "to", "for", "of", "from", "with", "by", "about", "into",
    "onto", "upon", "over", "under", "between", "among", "through", "during",
    "before", "after", "above", "below", "near", "beside", "without", "within",
    "across", "against", "along", "around", "behind", "beyond", "despite", "except",
    "inside", "outside", "towards", "toward", "until", "till", "via", "per", "as",
}
CONJ = {"and", "or", "but", "so", "because", "if", "when", "while", "although", "though", "than", "whether"}
AUX = {
    "am", "is", "are", "was", "were", "be", "been", "being", "do", "does", "did",
    "have", "has", "had", "will", "would", "shall", "should", "can", "could",
    "may", "might", "must",
}
INTERJ = {"hello", "hi", "hey", "bye", "goodbye", "thanks", "please", "sorry", "ok", "okay", "yes", "no"}


def norm_pos(raw: str | None) -> str | None:
    if not raw:
        return None
    s = raw.strip().lower()
    s = re.sub(r"\s+", "", s).rstrip(".")
    if "aux" in s and (s.startswith("v") or s == "aux"):
        return "aux."
    base = s.split(".")[0]
    return POS_ALIASES.get(base) or POS_ALIASES.get(s)


def clean_zh(text: str) -> str:
    t = text.strip()
    t = re.sub(r"\s+", " ", t)
    t = t.replace(";", "；")
    t = re.sub(r"\s*；\s*", "；", t)
    t = re.sub(r"[；]{2,}", "；", t)
    t = t.strip(" \t;；,，/&。.")
    return t


def guess_pos(english: str) -> tuple[str, bool]:
    eng = english.strip()
    low = eng.lower()
    tokens = re.findall(r"[A-Za-z']+", eng)
    if not tokens:
        return "phr.", True
    w = tokens[0].lower()
    if w in INTERJ and len(tokens) == 1:
        return "interj.", False
    if " " in eng or "=" in eng or any(c in low for c in ("'m", "'re", "'ve", "'ll", "'d")):
        return "phr.", False
    if len(tokens) >= 2:
        return "phr.", False
    if w in ARTICLES:
        return "art.", False
    if w in PRONOUNS:
        return "pron.", False
    if w in PREPS:
        return "prep.", True
    if w in CONJ:
        return "conj.", True
    if w in AUX:
        return "aux.", True
    if re.fullmatch(
        r"\d+|first|second|third|fourth|fifth|sixth|seventh|eighth|ninth|tenth|"
        r"eleven|twelve|thirteen|fourteen|fifteen|sixteen|seventeen|eighteen|"
        r"nineteen|twenty|thirty|forty|fifty|hundred|thousand",
        w,
    ):
        return "num.", False
    return "n.", True


def strip_ame_pos_prefix(zh: str) -> str:
    """Handle '(Am E meter) n. 米' → keep note + 米, drop n."""
    m = re.match(
        r"^(?P<note>\([^)]*\))\s*(?:n|v|adj|adv)\.?\s*(?P<rest>.+)$",
        zh,
        re.I,
    )
    if m:
        note = m.group("note").strip()
        rest = clean_zh(m.group("rest"))
        return clean_zh(f"{note} {rest}") if rest else note
    return zh


def parse_segment(seg: str, default_pos: str | None) -> tuple[str, str] | None:
    seg = seg.strip(" &")
    if not seg:
        return None
    poses: list[str] = []
    rest = seg
    while True:
        m = POS_TOKEN_RE.match(rest)
        if not m:
            break
        p = norm_pos(m.group("pos"))
        if p:
            poses.append(p)
        rest = rest[m.end() :]
    zh = clean_zh(strip_ame_pos_prefix(rest))
    if not zh and not poses:
        return None
    pos = poses[0] if poses else default_pos
    if not pos:
        return None
    if not zh:
        return None
    return pos, zh


def split_multi_pos_text(text: str) -> list[str]:
    """Split '北，北方；adj. 北方的' into chunks at POS boundaries."""
    text = text.strip()
    if not text:
        return []
    # Find all mid POS starts
    parts: list[str] = []
    indices = [m.start() for m in MID_POS_SPLIT_RE.finditer(text)]
    # Always include 0
    cuts = sorted({0, *[i for i in indices if i > 0], len(text)})
    for i in range(len(cuts) - 1):
        chunk = text[cuts[i] : cuts[i + 1]].strip(" ;；,&")
        if chunk:
            parts.append(chunk)
    return parts or [text]


def preprocess_translation(tr: str) -> str:
    """Normalize notes like '(Am E meter) n. 米' before POS splitting."""
    t = tr.strip()
    t = re.sub(
        r"(\([^)]*\))\s*(?:n|v|adj|adv)\.?\s*",
        r"\1 ",
        t,
        flags=re.I,
    )
    return t


def build_senses(
    english: str, part_of_speech: str | None, translation: str
) -> tuple[list[dict], bool]:
    low_conf = False
    primary = norm_pos(part_of_speech)
    tr = preprocess_translation(translation or "")

    # Pattern: primary POS in field + translation starts with & other POS
    if primary and tr.lstrip().startswith("&"):
        extras, rest = [], tr
        poses: list[str] = [primary]
        while True:
            m = POS_TOKEN_RE.match(rest)
            if not m:
                break
            p = norm_pos(m.group("pos"))
            if p and p not in poses:
                poses.append(p)
            rest = rest[m.end() :]
        zh = clean_zh(rest) or english
        senses = [{"part_of_speech": p, "translation": zh} for p in poses]
        return senses, False

    chunks = split_multi_pos_text(tr)
    senses: list[dict] = []

    for chunk in chunks:
        chunk = chunk.strip(" ;；,&")
        if not chunk:
            continue
        if POS_TOKEN_RE.match(chunk):
            parsed = parse_segment(chunk, default_pos=None)
            if parsed:
                senses.append({"part_of_speech": parsed[0], "translation": parsed[1]})
            continue
        zh = clean_zh(chunk)
        if not zh:
            continue
        pos = primary if not senses else (primary or guess_pos(english)[0])
        if not pos:
            pos, lc = guess_pos(english)
            low_conf = low_conf or lc
        # If we already have senses, this zh-only chunk belongs to primary before mid-POS
        if senses and not POS_TOKEN_RE.match(chunk):
            # prepend situation already handled by order: zh-only chunks come first
            pass
        senses.append({"part_of_speech": pos, "translation": zh})

    # Merge consecutive same-POS? Keep as-is.
    # If multiple zh-only then POS chunks: first group uses primary
    if not senses:
        zh = clean_zh(tr) or english
        pos = primary
        if not pos:
            pos, lc = guess_pos(english)
            low_conf = True
        senses = [{"part_of_speech": pos, "translation": zh}]

    # Deduplicate identical sense pairs
    dedup: list[dict] = []
    seen: set[tuple[str, str]] = set()
    for s in senses:
        zh = clean_zh(s["translation"])
        while True:
            m = POS_TOKEN_RE.match(zh)
            if not m:
                break
            zh = zh[m.end() :].strip()
        zh = clean_zh(zh) or english
        key = (s["part_of_speech"], zh)
        if key in seen:
            continue
        seen.add(key)
        dedup.append({"part_of_speech": s["part_of_speech"], "translation": zh})
    senses = dedup

    if any(POS_IN_ZH_RE.search(s["translation"]) for s in senses):
        # Last-chance: re-split any sense whose zh still embeds POS
        repaired: list[dict] = []
        for s in senses:
            if not POS_IN_ZH_RE.search(s["translation"]):
                repaired.append(s)
                continue
            for chunk in split_multi_pos_text(s["translation"]):
                chunk = chunk.strip(" ;；,&")
                if POS_TOKEN_RE.match(chunk):
                    parsed = parse_segment(chunk, None)
                    if parsed:
                        repaired.append(
                            {"part_of_speech": parsed[0], "translation": parsed[1]}
                        )
                else:
                    zh = clean_zh(chunk)
                    if zh:
                        repaired.append(
                            {"part_of_speech": s["part_of_speech"], "translation": zh}
                        )
        senses = repaired or senses
        if any(POS_IN_ZH_RE.search(s["translation"]) for s in senses):
            low_conf = True

    return senses, low_conf


def main() -> None:
    if not JSON_PATH.exists():
        print(f"Missing {JSON_PATH}", file=sys.stderr)
        sys.exit(1)

    payload = json.loads(JSON_PATH.read_text(encoding="utf-8"))
    # Prefer raw fields: if senses already present, still rebuild from part_of_speech + translation
    # after extract (translation may already be corrupted). Caller should re-extract first.

    low_rows: list[str] = []
    multi = 0
    total = 0

    for book in payload["books"]:
        for word in book["words"]:
            total += 1
            # Drop old senses before rebuild
            word.pop("senses", None)
            senses, low = build_senses(
                english=word["english"],
                part_of_speech=word.get("part_of_speech"),
                translation=word.get("translation") or "",
            )
            word["senses"] = senses
            word["part_of_speech"] = senses[0]["part_of_speech"]
            uniq_zh: list[str] = []
            for s in senses:
                if s["translation"] not in uniq_zh:
                    uniq_zh.append(s["translation"])
            word["translation"] = "；".join(uniq_zh)
            if len(senses) > 1:
                multi += 1
            if low:
                low_rows.append(
                    f"{book['book_id']}\t{word['english']}\t{json.dumps(senses, ensure_ascii=False)}"
                )

    payload["senses_enriched_at"] = datetime.now(timezone.utc).isoformat()
    JSON_PATH.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")
    REPORT_PATH.write_text(
        "\n".join(
            [f"total={total}", f"multi_sense={multi}", f"low_confidence={len(low_rows)}", "", *low_rows]
        ),
        encoding="utf-8",
    )
    print(f"enriched {total} words; multi_sense={multi}; low_confidence={len(low_rows)}")
    print(f"wrote {JSON_PATH}")
    print(f"report {REPORT_PATH}")


if __name__ == "__main__":
    main()
