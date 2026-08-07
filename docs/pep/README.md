# PEP 教材语料入库契约

面向"人教版初中英语"官方音频 + 单词表 + 听力原文的整理与灌入 Supabase 的约定文档。
本目录只放**数据契约 + 抽取产物**；实际入库由 `scripts/apply_pep_corpus.py` 完成。

---

## 年级基准值（difficulty_value）

公式：`difficulty_value = (grade - 1) * 2 + volume`，其中 volume=1 表示上册、2 表示下册。

| 年级/册 | difficulty_value | book_id 示例 |
|---|---|---|
| 一年级上 | 1 | pep-g1-vol1 |
| 一年级下 | 2 | pep-g1-vol2 |
| … | … | … |
| 六年级下 | 12 | pep-g6-vol2 |
| 七年级上 | 13 | pep-g7-vol1 |
| 七年级下 | 14 | pep-g7-vol2 |
| 八年级上 | 15 | pep-g8-vol1 |
| 八年级下 | 16 | pep-g8-vol2 |
| 九年级上 | 17 | pep-g9-vol1 |
| 九年级下 | 18 | pep-g9-vol2 |
| 高中及以上 | ≥19 | 不设上限 |

同一 english 在多本教材出现时，`vocabulary_master.difficulty_value` = 所有含 grade_level 教材 grade_level 的算术平均值（保留两位小数）。

---

## Book ID 命名

- 人教版：`pep-g{grade}-vol{1|2}`
- 外研版（已存量）：`fltrp-g{grade}-vol{1|2}`
- 自制/示例：任意，但若不设 `grade_level` 就不会进入 master 平均计算

## Master ID 命名

- `mv-{slug(english)}`
- slug：小写、非字母数字替换为 `-`，去尾部 `-`
- 示例：`hello` → `mv-hello`；`take off` → `mv-take-off`

---

## `pep_word_books.json` schema

```jsonc
{
  "generated_at": 2026080601,
  "books": [
    {
      "book_id": "pep-g7-vol1",
      "title": "人教版 英语 七年级上册",
      "description": "PEP 2024 版·七年级上",
      "language": "en-US",
      "grade_level": 13,
      "version": 2026080601,
      "units": [
        {
          "unit_ref": "Starter Unit 1",
          "module_title": "Starter Unit 1 Hello!",
          "words": [
            {
              "english": "hello",
              "phonetic": "/həˈloʊ/",
              "senses": [
                { "part_of_speech": "int.", "translation": "你好" }
              ],
              "example_sentence": "Hello, I'm Amy.",
              "sort_order": 0,
              "needs_review": false
            }
          ]
        }
      ]
    }
  ]
}
```

字段说明：
- `senses`：多义项数组；`part_of_speech` + `translation` 必填。展示端沿用 `docs/2026-08-04/word_senses.md` 契约。
- `needs_review`：LLM 自动补齐字段时为 true，人工审核后置 false。
- 单词的 `word_id` 由 `apply_pep_corpus.py` 生成为 `{book_id}-{sort_order:04d}`。
- 每个 unit 对应一条 `word_book_modules` 记录，`module_id = {book_id}-u{index}`。

## `pep_listening_bundles.json` schema

```jsonc
{
  "generated_at": 2026080601,
  "bundles": [
    {
      "material_id": "pep-g7-vol1-u1-secA1b",
      "book_id": "pep-g7-vol1",
      "grade_level": 13,
      "unit_ref": "Unit 1",
      "section_ref": "Section A 1b",
      "material_type": "dialogue",
      "title": "Unit 1 · Section A 1b",
      "title_zh": "第一单元 A部分 1b",
      "prompt_text": "Listen and choose the right answer.",
      "transcript": null,                 // 若从 docx 抽出则填英文原文
      "source_relpath": "7下/新版/【2】7下音频（新教材）/【2】7下听力+单词（细分版）/23-7下-Unit-3-Setion-B-1b.mp3",
      "estimated_seconds": 45,
      "sort_order": 0,
      "needs_review": true
    }
  ]
}
```

字段说明：
- `source_relpath`：相对于 `C:\Users\zhong\Desktop\Eng资料` 的路径；`apply_pep_corpus.py` 据此上传 Storage。
- `audio_url` 不放在 json 里，由脚本上传后回填到 `listening_materials`。
- 缺 transcript 时保持 `null`，客户端 fallback 逻辑不朗读文本，只播放音频。
- 目前不写选择题；后续如要出题可另行 authoring，走 `listening_questions`/`listening_options` 已有表。

## Storage 约定

- Bucket：`audio-pep`（public read）
- Object key：`{book_id}/{unit_slug}/{section_slug}.mp3`
- `unit_slug` = unit_ref 转小写去空格（`Unit 1` → `unit-1`、`Starter Unit 1` → `starter-unit-1`）
- `section_slug` = section_ref 转小写去空格（`Section A 1b` → `section-a-1b`）
- `audio_url` = `${SUPABASE_URL}/storage/v1/object/public/audio-pep/{key}`

## 流水线

```
scan_pep_corpus.py       → docs/pep/scan_report.json
extract_pep_words.py     → docs/pep/pep_word_books.json
extract_pep_listening.py → docs/pep/pep_listening_bundles.json
apply_pep_corpus.py      → 上传 mp3 + 写 Supabase
```

`apply_pep_corpus.py` 支持：
- `--dry-run`：只打印 SQL 摘要
- `--only=words|listening|master`：分步验证
- `--book-id=pep-g7-vol1`：只处理指定 book_id
