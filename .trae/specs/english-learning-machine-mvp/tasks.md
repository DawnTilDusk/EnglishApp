# 任务列表 (Tasks)

- [x] 任务 1: 项目基础架构搭建
  - [x] 子任务 1.1: 添加依赖 (Navigation Compose, Material Icons, DataStore)。
  - [x] 子任务 1.2: 创建包结构 (`ui`, `data`, `model`, `viewmodel`)。
  - [x] 子任务 1.3: 设置 `MainActivity` 使用 Compose Navigation Host。

- [x] 任务 2: 数据层实现
  - [x] 子任务 2.1: 定义 `Word` 数据模型 (id, text, meaning, phonetic, example)。
  - [x] 子任务 2.2: 创建 `WordRepository` 提供模拟数据。
  - [x] 子任务 2.3: 实现 `UserPreferencesRepository` (使用 DataStore) 存储用户名和进度。

- [x] 任务 3: UI 组件开发 (基础组件)
  - [x] 子任务 3.1: 创建通用的 `AppTopBar`。
  - [x] 子任务 3.2: 创建通用的 `Flashcard` 组件。

- [x] 任务 4: 功能模块实现 - 主页 & 个人中心
  - [x] 子任务 4.1: 实现 `HomeScreen`，包含导航卡片。
  - [x] 子任务 4.2: 实现 `ProfileScreen`，显示进度和编辑用户名。

- [x] 任务 5: 功能模块实现 - 单词学习
  - [x] 子任务 5.1: 实现 `LearningViewModel` 处理单词加载和 TTS 逻辑。
  - [x] 子任务 5.2: 实现 `LearningScreen`，集成 `Flashcard` 组件和 TTS 播放按钮。

- [x] 任务 6: 功能模块实现 - 测验
  - [x] 子任务 6.1: 实现 `QuizViewModel` 生成题目和验证答案。
  - [x] 子任务 6.2: 实现 `QuizScreen`，包含问题和选项按钮。

# 任务依赖 (Task Dependencies)
- 任务 4, 5, 6 依赖于 任务 1, 2, 3。
