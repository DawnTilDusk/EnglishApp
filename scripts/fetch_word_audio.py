#!/usr/bin/env python3
"""Download bundled word audio MP3 files into app/src/main/res/raw/."""

from __future__ import annotations

import asyncio
import json
import sys
import time
from pathlib import Path

import edge_tts

ROOT = Path(__file__).resolve().parent.parent
WORDS_JSON = ROOT / "scripts" / "words.json"
OUTPUT_DIR = ROOT / "app" / "src" / "main" / "res" / "raw"
VOICE = "en-US-JennyNeural"
MIN_FILE_BYTES = 1024
MAX_RETRIES = 3
RETRY_DELAY_SEC = 1.0


def load_words() -> list[dict[str, str]]:
    with WORDS_JSON.open(encoding="utf-8") as handle:
        words = json.load(handle)
    if not isinstance(words, list) or not words:
        raise ValueError("words.json must contain a non-empty list")
    return words


def output_path(word_id: str) -> Path:
    return OUTPUT_DIR / f"word_{word_id}.mp3"


async def download_word(word_id: str, english: str) -> tuple[str, bool, str]:
    destination = output_path(word_id)
    if destination.exists() and destination.stat().st_size > MIN_FILE_BYTES:
        return word_id, True, "skipped"

    last_error = "unknown error"
    for attempt in range(1, MAX_RETRIES + 1):
        try:
            communicate = edge_tts.Communicate(english, VOICE)
            await communicate.save(str(destination))
            if destination.stat().st_size <= MIN_FILE_BYTES:
                raise RuntimeError("downloaded file is too small")
            return word_id, True, "downloaded"
        except Exception as exc:  # noqa: BLE001
            last_error = str(exc)
            if destination.exists():
                destination.unlink(missing_ok=True)
            if attempt < MAX_RETRIES:
                await asyncio.sleep(RETRY_DELAY_SEC)
    return word_id, False, last_error


async def main() -> int:
    words = load_words()
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)

    successes: list[str] = []
    failures: list[tuple[str, str]] = []

    for index, item in enumerate(words):
        word_id = item["wordId"]
        english = item["english"]
        word_id, ok, message = await download_word(word_id, english)
        if ok:
            successes.append(f"{word_id}: {message}")
        else:
            failures.append((word_id, message))
        if index < len(words) - 1:
            await asyncio.sleep(0.3)

    print("Download summary")
    for line in successes:
        print(f"  OK  {line}")
    for word_id, message in failures:
        print(f"  FAIL {word_id}: {message}")

    expected_files = {output_path(item["wordId"]).name for item in words}
    actual_files = {
        path.name
        for path in OUTPUT_DIR.glob("word_*.mp3")
        if path.stat().st_size > MIN_FILE_BYTES
    }
    missing = expected_files - actual_files
    if missing:
        print(f"Missing audio files: {sorted(missing)}")
        return 1
    if failures:
        return 1
    print(f"All {len(words)} word audio files are ready in {OUTPUT_DIR}")
    return 0


if __name__ == "__main__":
    raise SystemExit(asyncio.run(main()))
