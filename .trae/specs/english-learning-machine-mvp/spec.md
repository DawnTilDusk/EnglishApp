# 英语学习机 MVP 规格说明书 (English Learning Machine MVP Spec)

## 为什么 (Why)
用户拥有一个名为 "LenovoY11" 的 Android 平板模拟器，配置为大屏 (1920x1200)、8核 CPU、2GB RAM，并具备麦克风、摄像头和多种传感器。为了充分利用该硬件，我们需要开发一款基于 Jetpack Compose 的英语学习应用，提供基础的单词学习、发音练习和进度追踪功能。

## 变更内容 (What Changes)
我们将构建一个包含以下核心模块的 MVP (最小可行性产品)：

- **主页 (Dashboard)**: 展示学习模块入口和当前进度。
- **单词卡片模块 (Flashcard Module)**:
    - 展示单词、音标、释义和示例图片。
    - 集成 Text-to-Speech (TTS) 进行单词发音。
- **练习模块 (Practice/Quiz Module)**:
    - 简单的选择题（看图选词或听音选词）。
    - 实时反馈（正确/错误）。
- **个人中心 (Profile)**:
    - 用户名称设置。
    - 学习统计（已学单词数）。
    - 数据持久化（使用 DataStore 或 SharedPreferences）。

**技术栈**:
- UI: Jetpack Compose (Material3)
- 架构: MVVM
- 导航: Jetpack Navigation Compose
- 依赖注入: Hilt (可选，MVP 阶段可先手动注入以简化) -> 决定使用 Hilt 以利于扩展。
- 异步: Coroutines & Flow
- 本地存储: DataStore Preferences

## 影响 (Impact)
- **新增文件**:
    - `ui/screens/`: 存放各个功能页面的 Composable。
    - `ui/navigation/`: 定义导航图。
    - `data/`: 存放数据模型和存储逻辑。
    - `viewmodel/`: 存放业务逻辑。
- **修改文件**:
    - `MainActivity.kt`: 设置导航宿主。
    - `build.gradle.kts`: 添加必要的依赖 (Navigation, DataStore, Icons 等)。

## 新增需求 (ADDED Requirements)

### 需求：主页导航
系统应提供一个主界面，用户可以从中进入“单词学习”、“测验挑战”和“个人中心”。

#### 场景：启动应用
- **当** 用户打开应用时
- **则** 显示主页，包含欢迎语和功能卡片。

### 需求：单词学习 (Flashcards)
系统应提供单词卡片浏览功能。
- **当** 用户进入单词学习模块
- **则** 显示当前单词卡片。
- **当** 用户点击“发音”按钮
- **则** 系统通过 TTS 朗读该单词。
- **当** 用户点击“下一个”
- **则** 切换到下一个单词。

### 需求：测验 (Quiz)
系统应提供简单的四选一测验。
- **当** 用户选择正确答案
- **则** 显示绿色高亮并增加分数。
- **当** 用户选择错误答案
- **则** 显示红色高亮提示。

### 需求：数据持久化
系统应保存用户的学习进度。
- **当** 用户完成一个单词的学习
- **则** 更新本地存储中的“已学单词数”。
