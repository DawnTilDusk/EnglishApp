"""Offline ASR cross-check of the generated listening pack; does not replace human listening."""
import difflib
import json
import re
from pathlib import Path

import numpy as np
from scipy.signal import resample_poly
import soundfile as sf
import torch
from transformers import WhisperForConditionalGeneration, WhisperProcessor

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'EngResource/listening_original_20260922'
MODEL = ROOT / 'tmp/whisper-base-en-local'
torch.set_num_threads(4)
processor = WhisperProcessor.from_pretrained(str(MODEL))
model = WhisperForConditionalGeneration.from_pretrained(str(MODEL)).to('cuda').eval()


def normalize(text):
    text = text.lower().replace("'", '')
    numbers = {'1':'one','2':'two','3':'three','4':'four','5':'five','6':'six','7':'seven','8':'eight','9':'nine','10':'ten','12':'twelve','15':'fifteen','20':'twenty','30':'thirty'}
    text = re.sub(r'\b(\d+):(\d+)\b', lambda m: numbers.get(m[1],m[1])+' '+numbers.get(m[2],m[2]), text)
    return [numbers.get(word, word) for word in re.findall(r'[a-z0-9]+', text)]


results = []
pack = json.loads((ROOT / 'scripts/listening_seed/original_20260922.json').read_text(encoding='utf-8'))
for m in pack['materials']:
    wave, rate = sf.read(OUT / (m['material_id'] + '.wav'), dtype='float32')
    wave = resample_poly(wave, 16000, rate)
    # Use Whisper's timestamp-based long-form decoding for audio longer than 30s.
    batch = processor(wave, sampling_rate=16000, return_tensors='pt', return_attention_mask=True,
                      truncation=False, padding='longest')
    with torch.inference_mode():
        ids = model.generate(batch.input_features.to('cuda'), attention_mask=batch.attention_mask.to('cuda'),
                             return_timestamps=True, num_beams=3, max_new_tokens=440)
    heard = processor.batch_decode(ids, skip_special_tokens=True)[0].strip()
    expected, actual = normalize(m['transcript']), normalize(heard)
    similarity = difflib.SequenceMatcher(None, expected, actual, autojunk=False).ratio()
    entry = {'material_id':m['material_id'], 'word_sequence_similarity':round(similarity,4),
             'expected':m['transcript'], 'recognized':heard}
    results.append(entry)
    (OUT / 'asr_review.json').write_text(json.dumps(results, ensure_ascii=False, indent=2), encoding='utf-8')
    print(json.dumps(entry, ensure_ascii=True), flush=True)
