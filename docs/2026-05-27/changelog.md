# Seedie App Changelog (2026-05-27)

> 记录本轮围绕 Phase 3（词书云端管理 + 用户下载 + 进度展示）的方案设计与 Room v5→v6 迁移落地。

## 本轮完成内容

### Phase 3 整体方案确定

**需求明确**：
- 管理员将英语词书上传到 Supabase，用户在 App 内选择/下载词书
- 每本词书约 500-600 词，支持 Module/Unit 分组
- 单词附带音频文件（Supabase Storage）
- 用户选书后背单词加载对应词书的单词
- 学习中心展示当前词书的学习进度
- 词书选择入口暂定放在 Profile 页

**关键架构决策**：
- 词书数据流向：Supabase（公开只读）→ 按需下载 → 本地 Room（Type A 只读）
- 音频：存 Supabase Storage（bucket: `word-audio`，公开），App 用 ExoPlayer 流式播放 + 自动本地缓存，不随词书整体预下载
- Module 分组：独立 `word_book_modules` 表，词学习时顺序按 `module.sortOrder → word.sortOrder` 跨模块连续推进，游标不分模块分别维护
- 所有词书共用同一张 `vocabulary_words` 表（标准关系型设计，用 `bookId` 区分）
- 版本管理：本地 `version` vs 云端 `version` 对比，云端高则重新全量下载该书

### Step 1：Supabase 建表 ✅

在 Supabase SQL Editor 执行完成，三张表 + RLS + Storage bucket 全部就绪：

**新建表**：
- `public.word_books`：词书元数据（book_id, title, description, language, difficulty, version, word_count, cover_url, updated_at）
- `public.word_book_modules`：模块表（module_id, book_id, title, sort_order, word_count），外键 CASCADE
- `public.vocabulary_words`：单词表（word_id, book_id, module_id, english, phonetic, part_of_speech, translation, example_sentence, difficulty_level, reward_token, estimated_duration_sec, sort_order, audio_url），外键 CASCADE

**RLS**：三张表均为公开只读（`FOR SELECT USING (true)`）

**Storage**：`word-audio` bucket，PUBLIC，已建读取策略

### Step 2：词书数据格式模板 ✅

生成三份 CSV 模板供管理员整理数据：
- `docs/templates/template_books.csv`
- `docs/templates/template_modules.csv`
- `docs/templates/template_words.csv`

**重要规则**：
- `word_id` 命名：`{book_id}-{4位序号}`，如 `oxford3000-0001`（不用单词本身作 ID，因为同词可出现在多本书）
- `sort_order` 全书连续递增，不按 Module 重置
- 音频文件命名：`{word_id}.mp3`，路径：`word-audio/{book_id}/{word_id}.mp3`

### Step 3：Room v5→v6 迁移 ✅

**编译验证通过**（`kspDebugKotlin BUILD SUCCESSFUL`）

**新建文件**：
- `data/local/entity/WordBookModuleEntity.kt`：Module 实体，外键关联 word_books
- `data/local/dao/WordBookModuleDao.kt`：getModulesByBook / getModuleById / insertModules / deleteModulesByBook

**修改文件**：
- `entity/WordBookEntity.kt`：新增 `coverUrl: String? = null`
- `entity/VocabularyWordEntity.kt`：新增 `moduleId: String? = null`、`audioUrl: String? = null`，新增 moduleId 索引
- `dao/WordBookDao.kt`：新增 `getAllBooks(): Flow`、`getAllBooksOnce()`、`insertBooks()`、`updateDownloadStatus()`、`deactivateAllBooks()`、`setActiveBook()`、`getBookById()`
- `dao/VocabularyWordDao.kt`：新增 `deleteWordsByBook()`、`getWordCountByBook()`
- `SeedieDatabaseMigrations.kt`：新增 `MIGRATION_5_6`（建 word_book_modules 表；vocabulary_words 加 moduleId/audioUrl 列；word_books 加 coverUrl 列）
- `SeedieDatabase.kt`：版本 5→6，注册 `WordBookModuleEntity`、`wordBookModuleDao()`
- `di/DatabaseModule.kt`：注册 `MIGRATION_5_6`、`provideWordBookModuleDao()`

## 当前状态

- Supabase 表结构已就绪 ✅
- 本地 Room v6 结构已就绪 ✅
- 词书数据模板已生成，**等待管理员整理词书数据并导入 Supabase** ⏳
- Android 代码层面 Step 4-9 尚未开始 ⬜
