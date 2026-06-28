# Seedie App Changelog (2026-06-28)

> 记录相对 develop@459ae6c（Phase 3 Step 1/3 完成点）以来，听力训练、词汇测验 Demo 落地及共享词书/音频基础设施抽取。

## 对比基准

| 项 | 内容 |
|----|------|
| **基准 commit** | `459ae6c` — Phase 3 分 9 步，已完成 Step 1（Supabase 建表）与 Step 3（Room v5→v6 迁移）；Step 2（CSV 模板）与 Step 4 之后尚未完成 |
| **当前 HEAD** | `45c0fdf`（develop，与 origin/develop 同步） |
| **区间 commits** | `df37681` → `e8e847a` → `3ddaed8` → `45c0fdf`（共 4 个） |
| **diff 规模** | 68 files changed，+3989 / -222 lines |

### 能力对比（459ae6c → HEAD）

| 维度 | 459ae6c（基准） | HEAD（当前） |
|------|-----------------|--------------|
| 词书数据源 | 内置静态 24 词包，`VocabularyPracticeRepositoryImpl` 内联 seed | 抽取为 [`WordBookSeeder`](../app/src/main/java/com/example/seedie/data/repository/WordBookSeeder.kt)，Study/听力/测验共用 |
| MCQ 选项生成 | 逻辑内嵌于练习仓库 | 抽取为 [`VocabularyOptionBuilder`](../app/src/main/java/com/example/seedie/data/repository/VocabularyOptionBuilder.kt) |
| 单词音频 | 无 bundled 音频 | 24 个 `res/raw/word_w*.mp3` + [`WordAudioPlayer`](../app/src/main/java/com/example/seedie/data/audio/WordAudioPlayer.kt) |
| 听力训练 | 学习中心占位，`isAvailable=false`，Snackbar「萌芽中」 | 完整线性 session，路由 `listening_practice` |
| 词汇测验 | 同上占位 | 完整线性 session，路由 `vocabulary_quiz`，含演示用词汇量估算 |
| `StudyResult` | 无估算字段 | 新增可选字段 `estimatedVocabulary: Int?` |
| Phase 3 Step 4-9 | 未开始 | 仍未开始 |

### 模块状态对比

| 模块 | 459ae6c | HEAD |
|------|---------|------|
| 背单词 / 单词复习 | 已实现 | 已实现（底层抽取，行为不变） |
| 听力训练 | 占位 Snackbar | 已实现 Demo |
| 词汇测验 | 占位 Snackbar | 已实现 Demo + 词汇量估算 |
| 语法 / 口语 / 教材 / 写作 | 占位 | 占位 |
| Phase 3 云端词书下载 | Step 1+3 就绪 | Step 4-9 未开始 |

---

## 本轮完成内容

### 1. 工程与构建（`df37681`）

- [`settings.gradle.kts`](../settings.gradle.kts)：插件与依赖仓库切换华为镜像源，改善国内构建可达性
- [`gradle/wrapper/gradle-wrapper.properties`](../gradle/wrapper/gradle-wrapper.properties)：Gradle 分发地址走镜像
- [`gradle/libs.versions.toml`](../gradle/libs.versions.toml)：依赖版本微调

### 2. 共享基础设施

**词书加载**

- 新建 [`WordBookSeeder.kt`](../app/src/main/java/com/example/seedie/data/repository/WordBookSeeder.kt)
  - `ensureSeeded()`：Room 无词书时插入 [`VocabularyStaticWordPack`](../app/src/main/java/com/example/seedie/data/repository/VocabularyStaticWordPack.kt)（24 词）
  - `loadActiveBook()` / `loadWordsForBook()` / `filterByDifficulty()`

**MCQ 选项**

- 新建 [`VocabularyOptionBuilder.kt`](../app/src/main/java/com/example/seedie/data/repository/VocabularyOptionBuilder.kt)
  - 同词性优先 distractor；公开 `buildEnglishOptions` / `buildTranslationOptions`
  - 供 Study、听力、词汇测验复用

**UI 组件**

- 新建 [`PracticeOptionCard.kt`](../app/src/main/java/com/example/seedie/ui/components/PracticeOptionCard.kt)：四选一选项卡片（选中/正误反馈）

**音频**

- 新建 [`WordAudioPlayer.kt`](../app/src/main/java/com/example/seedie/data/audio/WordAudioPlayer.kt)：MediaPlayer 播放 raw 资源，失败可 TTS 兜底
- 新建 [`BundledWordAudioResolver.kt`](../app/src/main/java/com/example/seedie/data/local/BundledWordAudioResolver.kt)：`w1`–`w24` → `R.raw.word_w*`
- 新增 24 个 [`app/src/main/res/raw/word_w*.mp3`](../app/src/main/res/raw/)
- 新建 [`scripts/fetch_word_audio.py`](../scripts/fetch_word_audio.py) + [`scripts/words.json`](../scripts/words.json)：音频资源生成脚本

**DI**

- [`RepositoryModule.kt`](../app/src/main/java/com/example/seedie/di/RepositoryModule.kt)：绑定 `ListeningPracticeRepository`、`VocabularyQuizRepository`

### 3. 听力训练 Demo（`e8e847a`）

**架构**：镜像「无 Room 持久化、线性 session」模式（与后续词汇测验一致）。

| 层级 | 文件 |
|------|------|
| 路由 | [`Screen.ListeningPractice`](../app/src/main/java/com/example/seedie/ui/navigation/Screen.kt)、[`SeedieNavHost`](../app/src/main/java/com/example/seedie/ui/navigation/SeedieNavHost.kt) |
| 数据 | [`ListeningPracticeRepository`](../app/src/main/java/com/example/seedie/domain/repository/ListeningPracticeRepository.kt) / [`ListeningPracticeRepositoryImpl`](../app/src/main/java/com/example/seedie/data/repository/ListeningPracticeRepositoryImpl.kt) |
| UI | [`listening/`](../app/src/main/java/com/example/seedie/ui/screens/learning/listening/)（Models / ViewModel / Screen / Route） |

**行为**：

- 默认从激活词书随机抽 10 题（mixed 难度）
- 自动播放 bundled MP3；听音四选一英文
- 答对获 `rewardToken`；完成后 `StudyResult(moduleId="listening")` 回传 Main
- [`MainViewModel`](../app/src/main/java/com/example/seedie/ui/screens/main/MainViewModel.kt) 完成含「听力」字样的每日任务

**入口**：[`MainScreen`](../app/src/main/java/com/example/seedie/ui/screens/main/MainScreen.kt) `listening` 模块 `isAvailable=true`

### 4. 词汇测验 Demo（`3ddaed8` + `45c0fdf`）

| 层级 | 文件 |
|------|------|
| 路由 | [`Screen.VocabularyQuiz`](../app/src/main/java/com/example/seedie/ui/navigation/Screen.kt) |
| 组卷 | [`VocabularyQuizSessionFactory`](../app/src/main/java/com/example/seedie/data/repository/VocabularyQuizSessionFactory.kt)、[`VocabularyQuizWordSelector`](../app/src/main/java/com/example/seedie/domain/quiz/VocabularyQuizWordSelector.kt) |
| 估算 | [`VocabularyEstimateCalculator`](../app/src/main/java/com/example/seedie/domain/quiz/VocabularyEstimateCalculator.kt)、[`VocabularyQuizConstants`](../app/src/main/java/com/example/seedie/domain/quiz/VocabularyQuizConstants.kt) |
| 数据 | [`VocabularyQuizRepository`](../app/src/main/java/com/example/seedie/domain/repository/VocabularyQuizRepository.kt) / [`VocabularyQuizRepositoryImpl`](../app/src/main/java/com/example/seedie/data/repository/VocabularyQuizRepositoryImpl.kt) |
| UI | [`quiz/`](../app/src/main/java/com/example/seedie/ui/screens/learning/quiz/)（Models / ViewModel / Screen / Route） |

**组卷规则**：

- 固定 12 题；`easy` / `medium` / `hard` **各 4 词**分层抽样（24 词静态库 8/8/8）
- 题型：**仅英→中**（显示英文 + 音标 + 词性，四选一中文）
- `Random(sessionId.hashCode())` 保证同 session 可复现

**结果与奖励**：

- 结果页展示**演示用词汇量估算**（非优秀/良好分级）；免责声明见 `ESTIMATE_DISCLAIMER`
- 答对 + `rewardToken`；完成整场 +5 代币（`COMPLETION_BONUS`）
- [`StudyResult.estimatedVocabulary`](../app/src/main/java/com/example/seedie/domain/model/StudyResult.kt) 回传 Main Snackbar
- 测验**不**自动完成「背诵单词」或「语法测验」每日任务

**入口与 UI 打磨**：

- [`MainScreen`](../app/src/main/java/com/example/seedie/ui/screens/main/MainScreen.kt)：`quiz` 模块 `isAvailable=true`，副标题保持「检验学习成果」
- `45c0fdf`：语音按钮下方反馈区增加 28dp 固定高度占位，提交前后选项区不跳动

### 5. 背单词模块适配

- [`VocabularyPracticeRepositoryImpl`](../app/src/main/java/com/example/seedie/data/repository/VocabularyPracticeRepositoryImpl.kt) 改用 `WordBookSeeder` + `VocabularyOptionBuilder`（Study/Review 行为不变，代码抽取）
- [`VocabularyPracticeScreen`](../app/src/main/java/com/example/seedie/ui/screens/learning/practice/VocabularyPracticeScreen.kt) 复用 `PracticeOptionCard`

### 6. 单元测试（新增）

| 文件 | 覆盖 |
|------|------|
| [`VocabularyOptionBuilderTest.kt`](../app/src/test/java/com/example/seedie/data/repository/VocabularyOptionBuilderTest.kt) | 英文/中文选项 4 选一、确定性 seed |
| [`VocabularyQuizSessionFactoryTest.kt`](../app/src/test/java/com/example/seedie/data/repository/VocabularyQuizSessionFactoryTest.kt) | 12 题、各档 4 词、选项结构 |
| [`VocabularyEstimateCalculatorTest.kt`](../app/src/test/java/com/example/seedie/domain/quiz/VocabularyEstimateCalculatorTest.kt) | 估算边界与单调性 |
| [`VocabularyQuizWordSelectorTest.kt`](../app/src/test/java/com/example/seedie/domain/quiz/VocabularyQuizWordSelectorTest.kt) | 分层抽样 |
| [`VocabularyQuizTestFixtures.kt`](../app/src/test/java/com/example/seedie/data/repository/VocabularyQuizTestFixtures.kt) | 24 词 test fixture |

### 7. 项目总览文档

- 本轮 diff 中新增 [`docs/project_overview.md`](../docs/project_overview.md)（约 1100 行）
- **注意**：该文件编写时仍将听力/测验标记为「未实现」，与当前 HEAD 代码**不一致**，需后续同步（见 [`todo_list.md`](todo_list.md)）

---

## 当前可验证结果

- 学习中心「听力训练」「词汇测验」可点击进入，不再显示「萌芽中」
- 听力：听音选英文，完成可触发含「听力」的每日任务
- 测验：12 题英→中，各难度 4 词；结果页显示「约 X 词」估算；返回 Main 后 Snackbar 含估算与代币
- 背单词 Study/Review 主闭环与 459ae6c 行为一致

## 当前实现边界

- 仍依赖内置 24 词静态包；Supabase 词书下载（Phase 3 Step 4-9）未接入
- 词汇量估算为演示公式，无 IRT/词频表；测验历史不持久化
- 听力/测验 session 仅存内存，进程杀死即丢失
- `docs/project_overview.md` 模块状态表过时，需单独更新

## Commit 索引

| Commit | 说明 |
|--------|------|
| `df37681` | 修改配置文件走华为镜像源 |
| `e8e847a` | 单词听力 demo 实现 |
| `3ddaed8` | 单词检测 demo 实现 |
| `45c0fdf` | 优化单词检测功能界面视觉（反馈区占位防抖动等） |
