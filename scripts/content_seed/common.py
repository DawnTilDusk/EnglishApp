"""Authoring helpers. Correct answers are supplied explicitly; option order is deterministic."""
import hashlib
import re

BATCH = '20261106'


def question(kind, stem, correct, wrong, explanation, evidence):
    return dict(kind=kind, stem=stem, correct=correct, wrong=wrong,
                explanation=explanation, evidence=evidence)


def reading(band, slug, title, title_zh, passage, questions):
    return dict(id=f'r-original-{BATCH}-{slug}', band=band, title=title,
                title_zh=f'【AI原创练习】{title_zh}', text=passage.strip(), questions=questions)


def listening(grade, slug, title, title_zh, transcript, transcript_zh, questions):
    return dict(id=f'l-original-{BATCH}-{slug}', grade=grade, title=title,
                title_zh=f'【AI原创·设备朗读】{title_zh}', text=transcript.strip(),
                transcript_zh=transcript_zh.strip(), questions=questions)


def choices(item_id, index, authored):
    # Rotating choices avoids an answer-position shortcut while preserving reproducible imports.
    position = (int(hashlib.sha256(item_id.encode()).hexdigest()[:8], 16) + index) % 4
    ordered = list(authored['wrong'])
    ordered.insert(position, authored['correct'])
    return chr(65 + position), [dict(option_id=chr(65+i), option_text=text, sort_order=i)
                                for i, text in enumerate(ordered)]


def words(text):
    return len(re.findall(r"[A-Za-z]+(?:['’-][A-Za-z]+)*", text))
