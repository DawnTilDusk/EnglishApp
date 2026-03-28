# Seedie App 技术实现方案 (Spec)

## 〇、约束与基础设定
* **硬件与显示**：锁定横屏运行 (`android:screenOrientation="landscape"`)，适配 2560*1600 分辨率。
* **操作系统**：目标 Android 15 (API Level 35)。
* **UI 框架**：100% Jetpack Compose（原生的 `Canvas` 和 `Animation` 替代第三方图表与动效库）。
* **架构模式**：遵循 Clean Architecture（UI层 / 领域层 / 数据层严格分离）+ MVI 状态管理。
* **核心技术栈**：Jetpack 官方标准件（ViewModel, StateFlow, Room, Hilt, Compose Navigation），兼顾极简与未来可扩展性。

## 一、 主题与 UI 规范系统 (Theme System)
依据“Garden”核心 IP 设计，在 `ui/theme` 中定义：
1. **Color Palette**:
   * Primary: `#66BB6A` (生机绿)
   * Secondary: `#8D6E63` (橡木棕)
   * Accent: `#FFB74D` (暖阳橙)
   * Background: `#F1F8E9` (淡薄荷绿)
   * Surface: `#FFFFFF` (奶油白)
2. **Shape & Shadow**:
   * `CardShape`: `RoundedCornerShape(24.dp)`
   * 自定义 `Modifier.gardenShadow()`，封装 8% 透明度的绿色软阴影（`Color(0x1466BB6A)`）。

## 二、 核心底层架构 (Global Architecture)

为了完美支撑双层联动导航和数据闭环，我们需要设计以下全局单例与机制：

### 1. 全局状态源 (UserSessionRepository)
作为 Source of Truth，管理核心数据。配合 Kotlin `StateFlow` 提供全局订阅能力：
```kotlin
data class UserSessionState(
    val tokens: Int = 0,
    val totalStudyTimeMinutes: Int = 0,
    val vocabularySize: Int = 0
)
// 统一提供 updateTokens(amount), addStudyTime(time) 等原子操作。
```

### 2. 双层联动与 Slot API
主页面使用 `HorizontalPager`，通过 Hoisting `PagerState` 统筹滑动：
```kotlin
val pagerState = rememberPagerState(pageCount = 4)
// 中控指示器插槽
CustomIndicatorPanel(pagerState = pagerState) { page ->
    GardenSeedIndicator(isActive = page == currentPage)
}
// 底部导航
BottomNavigationBar(pagerState = pagerState)
```

### 3. 全局动效事件总线 (Global Reward Event Bus)
用于跨层级/跨页面触发视觉奖励（如掉落金币）：
```kotlin
object RewardEventBus {
    private val _events = MutableSharedFlow<RewardEvent>()
    val events = _events.asSharedFlow()
    suspend fun emit(event: RewardEvent) { _events.emit(event) }
}
```

## 三、 数据持久化设计 (Room Database)
使用 Room 进行本地数据管理，划分以下核心表：
1. **DailyTaskEntity**: 包含 `id`, `date`, `title`, `isCompleted`, `rewardType`, `rewardAmount`。
2. **CheckInEntity**: 包含 `date`, `studyTime`, `isCheckedIn`（用于驱动 Tab 1 热力图）。
3. **GardenPlotEntity**: 包含 `plotIndex` (0~15), `plantType`, `level`（用于驱动 Tab 3 花园）。
4. **EconomyTransactionEntity**: 包含 `timestamp`, `amount`, `reason`（记账本，闭环核心）。

## 四、 页面级技术实现拆解

### 1. Splash & Check-in (启动与签到)
* **实现**：作为一个前置的 Compose 路由（`NavHost` 的 `startDestination`）。
* **逻辑**：检查本地今日是否已签到。未签到则显示带有全屏插画的 `CheckInScreen`，点击后触发 `RewardEventBus` 发送代币掉落动画，延迟后 `NavController.navigate("main")` 且 `popUpTo(0)` 清除栈。

### 2. Tab 1: 首页概览 (Dashboard)
* **热力图 (Heatmap)**：使用 `LazyVerticalGrid`。传入从 `StatisticsRepository` 读取的 `List<CheckInEntity>`，按日期映射深浅颜色（从 `#E8F5E9` 到 `#2E7D32`）。
* **每日任务 (Daily Mission)**：监听 `TaskManager.dailyTasks` (StateFlow)。任务卡片勾选后，回调 `onTaskCompleted`，触发 `EconomyManager.addTokens()` 和 `RewardEventBus.emit()`。

### 3. Tab 2: 学习模块中心 (Learning Hub)
* **组件**：`LazyVerticalGrid(columns = GridCells.Fixed(2))` 渲染 `StudyModuleCard`。
* **契约 (Contract)**：点击模块（如背单词）触发 `StudyRouter` 导航至对应子页，子页完成时带回 `StudyResult`。
* **空实现**：对于未开发模块，提供统一的 `Snackbar` 提示机制。

### 4. Tab 3: 数据与花园 (Data & Garden) - 闭环消费端
* **看板**：利用 Compose 的 `Canvas` 原生绘制环形图和折线图。使用 `animateFloatAsState` 实现切换到 Tab 3 时的图表弹跳动画。
* **GardenEngine (核心引擎)**：监听 `UserSessionState.vocabularySize`。当词汇量跨越阈值（如每增加50），通过 `GardenRepository` 更新特定 `GardenPlotEntity` 的 `level`。
* **植物组件**：使用策略模式（Strategy Pattern）或枚举定义植物外观。`@Composable PlantSlot(plot: GardenPlotEntity)`。

### 5. Tab 4: 个人中心 (Profile)
* **资产监听**：`EconomyManager.tokensFlow` 绑定到 UI。
* **成就触发器 (Achievement Trigger)**：监听核心数据流，达到条件时向数据库插入成就记录，并即时更新 UI 上的“成就墙”徽章（`ColorMatrix` 实现去色/点亮）。

## 五、 数据与业务闭环流程 (The Loop)
1. **生产 (Tab 2)**：用户完成学习 -> 返回 `StudyResult` -> `TaskManager` 标记任务完成。
2. **结算 (Global)**：任务完成 -> `EconomyManager` 增加代币，`StatisticsRepository` 增加词汇量和时长。
3. **反馈 (UI/Tab 1)**：`RewardEventBus` 触发全局动画 -> Tab 1 任务列表更新，热力图加深。
4. **转化 (Tab 3)**：词汇量增加 -> `GardenEngine` 触发植物升级 -> Tab 3 花园繁荣度上升。
5. **消费 (Tab 3/Tab 4)**：使用代币在花园购买高级种子/解锁成就。