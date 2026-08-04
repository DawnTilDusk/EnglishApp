# 词库 senses 契约

更新日期：2026-08-04

## 存储

`vocabulary_words.senses`（JSONB / Room `sensesJson`）：

```json
[
  {"part_of_speech": "v.", "translation": "改变；变化"},
  {"part_of_speech": "n.", "translation": "改变；变化"}
]
```

- 同一 `word_id` 可有多个义项；不拆行
- 兼容列：`part_of_speech` = 首义 POS；`translation` = 去重后的纯中文（`；` 连接）

## 展示

单行：`v. 改变；变化；n. 改变；变化`（由 `WordSenseFormat.displayLabel` 生成；义项间用 `；`）

## 流水线

1. `extract_fltrp_words.py`（从 doc）
2. `enrich_fltrp_senses.py`
3. `apply_fltrp_word_books.py --inplace`
