# Changelog 2026-08-04

## 阅读/听力双模式（自由刷题 + 作业）

### 做了什么

- 学习中心阅读/听力入口先选模式：「自由刷题」或「完成老师的作业」
- 自由刷题：题库列表选题（单套/单材料）；「已做过」徽章不移除题目
- 跨设备完成态：`user_practice_item_completions`；自由刷 `mark_my_practice_items_completed`；作业提交同步写入；历史 submitted 已 backfill
- 写作入口仍为作业列表（本轮未改）
- 自由刷代币 `refId = free_{module}:{itemRef}`（去重）；作业仍为 `assignment:{submissionId}`

### 关键路径

- 迁移：[`supabase/migrations/029_user_practice_item_completions.sql`](../../supabase/migrations/029_user_practice_item_completions.sql)（已远程 apply）
- App：`PracticeModeChooser*`、`PracticeCatalog*`、`FreePracticeArgs`、Reading/Listening ViewModel 自由路径、`SeedieNavHost` / `MainScreen`

### 文档

- 修订 [`docs/2026-07-24/supabase_table_map.md`](../2026-07-24/supabase_table_map.md)、[`docs/2026-06-28/project_overview.md`](../2026-06-28/project_overview.md)

---

## 外研版词库重建 + 线性词汇检测

### 做了什么

- 从 [`docs/words/外研版初中英语单词一览表(音标版).doc`](../words/外研版初中英语单词一览表(音标版).doc) 提取并提交可复现 JSON [`docs/words/fltrp_junior_words.json`](../words/fltrp_junior_words.json)
- 清空远端旧词书（含原 FLTRP 八上 demo / Seedie 12 词书）与相关 `user_vocabulary_*` 进度；导入六册外研词书（共 1792 词）
- 重做词汇测验：六档线性估测（上限 1800，每档 12 题、≥11 晋级）；开场拉齐六册题池
- `profiles.vocabulary_size` + `vocabulary_estimated_at`；RPC `set_my_vocabulary_estimate`；资料信息展示词汇量（未测＝「未检测」）
- 背单词不再通过 `vocabularyDelta` 增加词汇量

### 关键路径

- 脚本：`scripts/extract_fltrp_words.py`、`scripts/apply_fltrp_word_books.py`、`scripts/apply_one_migration.py`
- 迁移：[`supabase/migrations/027_profiles_vocabulary_size.sql`](../../supabase/migrations/027_profiles_vocabulary_size.sql)（已远程 apply）
- App：`domain/quiz/*`、`VocabularyQuizRepositoryImpl`、`VocabularyQuizViewModel`、`MainViewModel`、`IdentitySection`、`ProfileRepository*`

### 文档

- [`vocab_size_detection.md`](./vocab_size_detection.md)
- 修订 [`docs/2026-07-24/supabase_table_map.md`](../2026-07-24/supabase_table_map.md)

---

## 词库 senses 完善

### 做了什么

- 全库补全词性；汉译中剥离词性标记；多词性拆为 `senses` 列表（同一 `word_id`）
- 背单词/测验选项展示单行组合文案：`n. 改变；v. 变化`
- 远端 `vocabulary_words.senses` JSONB；Room `sensesJson`（DB v9）

### 关键路径

- `scripts/enrich_fltrp_senses.py`；报告 `docs/words/fltrp_senses_enrich_report.txt`
- 迁移 [`028_vocabulary_words_senses.sql`](../../supabase/migrations/028_vocabulary_words_senses.sql)（已远程 apply + inplace upsert）
- App：`WordSense.kt`、`VocabularyWordSenseExt.kt`、`VocabularyOptionBuilder`、Practice/Quiz UI

### 文档

- [`word_senses.md`](./word_senses.md)

---

## 词汇检测「以上都不对」

### 做了什么

- 词汇检测四选一约 40% 概率将第 4 选项替换为「以上都不对」（对错语义随被替换项保留）

### 关键路径

- [`VocabularyQuizConstants.kt`](../../app/src/main/java/com/example/seedie/domain/quiz/VocabularyQuizConstants.kt)、[`VocabularyQuizOptionExtras.kt`](../../app/src/main/java/com/example/seedie/domain/quiz/VocabularyQuizOptionExtras.kt)、[`VocabularyQuizSessionFactory.kt`](../../app/src/main/java/com/example/seedie/data/repository/VocabularyQuizSessionFactory.kt)

### 文档

- 修订 [`vocab_size_detection.md`](./vocab_size_detection.md)
