# Changelog 2026-08-17

## 修复登录后云端代币不展示（仅显示本地 0）

### 做了什么

- 根因：展示余额依赖内存 `cloudBalanceCache`；Profile/花园只订阅 `totalTokens`，从未在登录时拉取云端。清数据/重装后本地流水为空 → 一直显示 0（`3@3.com` 云端仍约 8 万）
- 修复：`EconomyManagerImpl` 在 session 用户变化时自动 hydrate；同步上传失败时仍尽量缓存已读到的云端余额

### 关键路径

- [`EconomyManagerImpl.kt`](../../app/src/main/java/com/example/seedie/data/repository/EconomyManagerImpl.kt)

### 文档

- 修订 [economy_token_system.md](../2026-07-24/economy_token_system.md) §3

---

## 枯苗铲除 + 种树 1 分钟容错

### 做了什么

- 确认选树开练后 **60 秒内**中途退出不记账（不种枯苗）；超时退出仍种枯苗；完成且有题仍种活苗
- 点选枯苗可花 **50** 代币铲除腾格（`refId = garden_remove:{userId}:{plantId}`）；余额不足不删树；活苗不可铲
- 退出确认 Dialog：**容错内用独立练习退出文案**（不复用枯苗警告）；容错外仍用枯苗警告；选树页与铲除确认/失败提示一并写清

### 关键路径

- [`GardenForestRules.kt`](../../app/src/main/java/com/example/seedie/domain/usecase/GardenForestRules.kt)
- [`GardenEngine.kt`](../../app/src/main/java/com/example/seedie/domain/usecase/GardenEngine.kt)
- [`GardenPlantDao.kt`](../../app/src/main/java/com/example/seedie/data/local/dao/GardenPlantDao.kt)
- [`ForestPanel.kt`](../../app/src/main/java/com/example/seedie/ui/screens/garden/ForestPanel.kt) / [`GardenViewModel.kt`](../../app/src/main/java/com/example/seedie/ui/screens/garden/GardenViewModel.kt)
- 听/读/背单词/词汇测验：`sessionOpenedAtMillis` + `GardenExitConfirmDialog`

### 文档

- 修订 [garden_forest_mvp.md](../2026-08-06/garden_forest_mvp.md)
- 修订 [economy_token_system.md](../2026-07-24/economy_token_system.md) refId 表
- 修订 [project_overview.md](../2026-06-28/project_overview.md) §5.6

---

## 修复 woman 汉译（词汇检测显示音标/复数注）

### 做了什么

- `fltrp-g7-vol1` 的 `woman` 曾把 `(pl. women ['wimin])` 当成汉译；改为 **女人**
- 远端 `vocabulary_words` 已更新；源 JSON [`fltrp_junior_words.json`](../words/fltrp_junior_words.json) 同步
- 词汇检测开场优先拉云端词并 REPLACE 进 Room，避免本地脏缓存

### 关键路径

- 迁移：[`039_fix_fltrp_woman_translation.sql`](../../supabase/migrations/039_fix_fltrp_woman_translation.sql)（远端已 apply 等价 SQL）
- App：[`VocabularyQuizRepositoryImpl.kt`](../../app/src/main/java/com/example/seedie/data/repository/VocabularyQuizRepositoryImpl.kt)

---

## 修复平板启动闪退（Room word_books.gradeLevel）

### 做了什么

- 设备本地库从 v10→v11 时，`word_books` 缺少 `gradeLevel`，Room 校验失败导致 App 转圈后退出
- 加固 [`MIGRATION_10_11`](../../app/src/main/java/com/example/seedie/data/local/SeedieDatabaseMigrations.kt)：幂等补齐 `gradeLevel` / `difficultyValue` / `masterId` / `exampleTranslation` 与花园表

### 文档

- 本条 changelog

---

## 词汇检测：晋级 ≥7 + 按比例估测 + 按次真实趋势

### 做了什么

- 晋级门槛：`ADVANCE_MIN_CORRECT` 11 → **7**
- 估测：每档一律 `(答对/12)×quota`；去掉「通过档满配额」「六档全通直给 1800」
- 新表 `user_vocabulary_estimates`；`set_my_vocabulary_estimate` 更新 profile 并 append 历史；已有估测 backfill 1 行
- 花园「词汇量趋势」去掉演示数据；按每次检测画点，横轴为测试日本地日期；未测空态

### 关键路径

- 迁移：[`supabase/migrations/038_user_vocabulary_estimates.sql`](../../supabase/migrations/038_user_vocabulary_estimates.sql)（已远程 apply）
- App：`VocabularyQuizConstants`、`GradeBandVocabularyEstimator`、`ProfileRepository*`、`GardenViewModel`、`StatsPanelSection`、`DataGardenScreen`

### 文档

- 修订 [`docs/2026-08-04/vocab_size_detection.md`](../2026-08-04/vocab_size_detection.md)
- 修订 [`docs/2026-07-24/supabase_table_map.md`](../2026-07-24/supabase_table_map.md)
