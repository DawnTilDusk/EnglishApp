# 平板横屏适配重构计划 (Tablet Landscape Refactoring Plan)

## 目标
将 `QuizScreen` 重构为适应 2560*1600 (16:10) 平板横屏显示的布局，采用左右分栏设计，并完美兼容 16:9 到 16:11 的长宽比波动。

## 实施步骤

### 1. 重构 QuizScreen 为双列（左右）布局
横屏下，原本的从上到下单列 (`Column`) 会导致视线移动过长且浪费屏幕两侧空间。我们将采用 `Row` 将屏幕一分为二：
- **左侧区域 (Weight = 1f)**：主要展示题目信息。包括顶部 Score（分数）、"What is the meaning of" 文本、目标单词、底部的结果反馈以及 "Next Question" 按钮。垂直方向居中对齐。
- **右侧区域 (Weight = 1f)**：主要展示选项。包含 4 个 `OutlinedCard` 选项卡片，并允许内容在极端情况下垂直滚动。

### 2. 移除硬编码尺寸，引入弹性空间适配
为了兼容 16:9 到 16:11 的高度变化：
- 移除代码中固定的 `Spacer(height = 48.dp)` 和 `Spacer(height = 32.dp)`。
- 采用 `Arrangement.SpaceEvenly`（均匀分布）或 `Modifier.weight(1f)` 来分配组件间的空白区域，让 Compose 引擎自动计算并填补不同比例下的高度差异。

### 3. 优化大屏交互尺寸
- 增大屏幕的全局外边距，从 `24.dp` 提升至 `32.dp`，让大屏边缘不显得拥挤。
- 调整卡片 (`OutlinedCard`) 的高度和内部边距，使其更适合平板手指的点击热区。
- 左、右两栏之间添加适度的留白 (`Spacer(width = 32.dp)`) 以区分视觉层级。

### 4. 锁定横屏显示 (可选配置)
如果应用主要针对平板且仅需横屏运行，将修改 `AndroidManifest.xml` 中的 `MainActivity` 配置，添加 `android:screenOrientation="sensorLandscape"`，强制应用以横屏模式启动和运行。