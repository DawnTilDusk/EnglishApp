# 2026-07-29 changelog

## 阅读训练上线（Supabase 唯一题库）

- 新增远端表 `reading_sets` / `reading_questions` / `reading_options`（公开 SELECT），迁移 [`020_reading_comprehension_catalog.sql`](../../supabase/migrations/020_reading_comprehension_catalog.sql)；已远程 apply，校验 **5 / 25 / 100**。
- 内容由临时本地 JSON 生成 seed 后上云；**已删除** `app/src/main/assets/reading/`，运行时 SSOT 仅为 Supabase。
- 生成脚本：[`scripts/generate_reading_seed.py`](../../scripts/generate_reading_seed.py)；应用脚本：[`scripts/apply_reading_020.py`](../../scripts/apply_reading_020.py)。
- Android：学习中心原「教材训练」改为「阅读训练」；`ReadingRemoteDataSource` 三次 bulk 拉取 → 左文右题、整套提交解析、逐篇刷完；答对按 `reward_token`，全库完成 +5 完成奖。
- 关键路径：`ui/screens/learning/reading/*`、`ReadingPracticeRepositoryImpl`、`ActivityModule.ReadingPractice`。

### 文档

- [`docs/2026-07-24/supabase_table_map.md`](../2026-07-24/supabase_table_map.md) — 增加阅读内容表
- [`docs/2026-06-28/project_overview.md`](../2026-06-28/project_overview.md) — 学习模块表更新
- 本 changelog

## 编译清理（`compileDebugKotlin`）

- 清掉 Kotlin 增量缓存冲突后，源码本身可成功编译；Android Studio 里看到的「5 个错误」来自 daemon/caches 锁冲突，不是阅读模块语法错误。
- 消除原先 15 条 `compileDebugKotlin` 警告：`@param:` 注解目标、Room `migrate(db)` 参数名、冗余 `else`、IdentitySection 弃用 API。
- 顺带修复 `MIGRATION_1_2` 里被损坏的索引名 `index_vocabulary_words_bookId`。
