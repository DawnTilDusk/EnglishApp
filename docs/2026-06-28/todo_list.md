# Seedie App TODO List (2026-06-28)

> 相对 develop@459ae6c 本轮（听力/测验 Demo）完成后的后续待办。Phase 3 Step 4-9 继承自 [2026-05-27/todo_list.md](../2026-05-27/todo_list.md)，文档同步项为本轮新增。

## 文档同步

- [ ] 同步更新 [`docs/project_overview.md`](../project_overview.md)：
  - 学习中心模块表：`listening`、`quiz` 改为 **已实现**
  - 补充 [`listening/`](../app/src/main/java/com/example/seedie/ui/screens/learning/listening/)、[`quiz/`](../app/src/main/java/com/example/seedie/ui/screens/learning/quiz/) 相关文件路径与行为说明
  - 移除或更正「Snackbar 萌芽中」占位描述

## Phase 3 剩余步骤（继承 2026-05-27，均未开始）

### Step 4：Remote Models + 网络层

- [ ] 在 `data/remote/Models.kt` 新增 `SupabaseWordBook`、`SupabaseWordBookModule`、`SupabaseVocabularyWord` 三个 `@Serializable` 数据类（字段用 `@SerialName` 对应 Supabase snake_case）
- [ ] 新建 `data/remote/WordBookRemoteDataSource.kt`：
  - `fetchAllBooks(): List<SupabaseWordBook>`
  - `fetchModulesByBook(bookId): List<SupabaseWordBookModule>`
  - `fetchWordsByBook(bookId, page, pageSize): List<SupabaseVocabularyWord>`（分页，每页 100 条）

### Step 5：WordBookSyncer（下载器）

- [ ] 新建 `data/sync/syncer/WordBookSyncer.kt`：
  - 拉取 Supabase word_books 列表 → 与本地 version 对比 → Upsert 本地 word_books 元数据
  - 下载单本词书：先拉 modules → 再分页拉 vocabulary_words → batch insert
  - 下载过程中更新 `downloadStatus`：`PENDING → DOWNLOADING → COMPLETED`
  - 暴露 `downloadProgress: Flow<DownloadProgress>`（已下载词数 / 总词数）
  - 下载失败时 `downloadStatus` 改为 `FAILED`，支持重试
- [ ] 在 `SyncScope` 中新增 `WORD_BOOK_LIST`（仅刷新书单，不下载词）
- [ ] 在 `SyncManagerImpl` 中注册新 Scope

### Step 6：词书选择页

- [ ] 新建 `ui/screens/profile/wordbookselection/WordBookSelectionScreen.kt`：
  - 从 Profile 页入口进入
  - 展示所有可用词书（来自本地 word_books 表，含未下载的）
  - 每本书卡片显示：书名、描述、词数、难度、模块数、下载状态
  - 下载状态对应操作按钮：`NOT_DOWNLOADED` → 下载按钮；`DOWNLOADING` → 进度条；`COMPLETED + 非激活` → "切换使用"；`COMPLETED + 激活` → "使用中"角标
- [ ] 新建 `ui/screens/profile/wordbookselection/WordBookSelectionViewModel.kt`：
  - 观察本地词书列表（`WordBookDao.getAllBooks()`）
  - 触发 Supabase 书单刷新
  - 处理下载动作（调 `WordBookSyncer`）
  - 处理切换激活词书（`deactivateAllBooks + setActiveBook`）
- [ ] 在 ProfileScreen 增加"词书管理"入口按钮
- [ ] 注册新路由

### Step 7：移除静态词包，接入动态词书

- [ ] 修改 `VocabularyPracticeRepositoryImpl.ensureSeededWordBook()`：
  - 有已下载词书时不再插入静态词包
  - 没有任何词书时 fallback 到现有静态词包（兜底，避免无词可学崩溃）
- [ ] 确认 `startOrResumeStudySession(bookId)` 使用的是激活词书的 bookId
- [ ] 听力/测验仓库随激活词书切换而加载对应词库（当前已走 `WordBookSeeder`，需验证大词书场景）

### Step 8：学习页显示 Module 标题 + 云端音频

- [ ] 在 `VocabularyPracticeViewModel` 中新增当前词的 `moduleTitle: String?`（根据 wordId 查 moduleId 再查 module title）
- [ ] 在 `VocabularyPracticeScreen` 顶部展示 Module 标题（仅当 moduleTitle 非空时显示）
- [ ] 在单词卡片上增加音频播放按钮（仅当 `audioUrl` 非空时显示）
- [ ] 集成 ExoPlayer 流式播放 Supabase Storage `audioUrl`（`SimpleCache + CacheDataSource`）

### Step 9：学习中心进度展示

- [ ] 在 `MainViewModel` 中新增：
  - `activeBookInfo: StateFlow<ActiveBookInfo?>` （书名、词数）
  - `vocabProgressStats: StateFlow<VocabProgressStats>` （已学数、已掌握数、待复习数）
- [ ] 修改 `LearningHubScreen` 背单词入口卡片：展示当前词书名 + 已掌握/已学数字
- [ ] 新增数据类 `ActiveBookInfo(bookId, title, wordCount)`
- [ ] 新增数据类 `VocabProgressStats(learnedCount, masteredCount, pendingReviewCount)`

## 本轮衍生待办（可选）

- [ ] 词汇测验：Room 持久化测验历史 / 错题本
- [ ] 词汇测验：替换演示估算为基于词频或 IRT 的算法（需词库规模支撑）
- [ ] 每日任务：新增「词汇测验」专属任务文案与完成逻辑（当前测验不自动勾选任何任务）
- [ ] CI / 本地跑通 `./gradlew testDebugUnitTest` 并纳入流水线

## 当前阻塞项

- **词书数据导入**：需管理员按 `docs/templates/` 三份 CSV 模板整理词书数据，并在 Supabase SQL Editor 导入
- 数据导入不阻塞 Step 4-9 代码编写，可先写代码，等数据就绪后联调
