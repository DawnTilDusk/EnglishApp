# Seedie App Changelog (当前开发状态)

> 记录 Seedie App 迄今为止的架构演进与功能实现状态。

## 已完成架构与基础设施 (阶段一 & 阶段二)
* **技术栈基石**: 完成基于 Jetpack Compose, Hilt (依赖注入), Room (本地数据库), Kotlin Coroutines (Flow 响应式数据流) 的 Clean Architecture 底层架构。
* **设计系统 (Garden Theme)**: 实现了全局横屏锁定 (`landscape`)、Android 15 Target SDK 适配。确立了森林配色方案（主色 `#66BB6A`，背景 `#F1F8E9` 等）、大圆角卡片（`24.dp`）以及核心 UI 扩展方法 `Modifier.gardenShadow()` (8% 透明度绿色软阴影)。
* **数据持久化**: 成功搭建基于 Room 的本地 SQLite 存储 (`SeedieDatabase`)，包含以下实体及 DAO 接口：
  * `DailyTaskEntity` (每日任务记录)
  * `CheckInEntity` (打卡记录)
  * `GardenPlotEntity` (花园土地植物状态数据)
  * `EconomyTransactionEntity` (代币收支账单流水)
* **全局状态中心**: 
  * `UserSessionRepository`: 统管全局核心状态，提供词汇量、学习时长等关键指标的唯一事实来源 (Source of Truth)。
  * `EconomyManager`: 提供统一的代币增删逻辑，防止多页面并发修改产生数据撕裂，并自动记录账单。
  * `RewardEventBus`: 搭建了基于 `SharedFlow` 的事件总线，为跨页面的动效（如跨 Tab 触发金币掉落）提供分发机制。

## 已完成核心 UI 与页面 (阶段三 ~ 阶段七)
* **全局路由与主框架**:
  * 搭建了 `SeedieNavHost`，完成 `SplashScreen` 到 `MainScreen` 的路由跳转。
  * `MainScreen` 中实现了基于 `HorizontalPager` 的双层导航联动：支持手势左右滑动平滑切换四大业务 Tab，底部 `BottomNavigationBar` 实现状态高亮。并在中间悬浮层实现了自定义的可拉长、加深的圆点指示器 (`CustomIndicatorPanel`)。
* **Tab 1：首页概览 (Dashboard)**:
  * 实现了基于 `LazyVerticalGrid` 的 `HeatmapSection` (学习热力图)，能够展示打卡密度的深浅绿色动态映射。
  * 实现了基于 `LazyColumn` 的 `DailyMissionSection` (任务列表)，对接 `DashboardViewModel` 实现了勾选状态的更新与金币实时发放逻辑。
* **Tab 2：学习中心 (Learning Hub)**:
  * 使用 `LazyVerticalGrid` 构建了适配屏幕宽度的 2列多行 网格布局。
  * 封装了标准功能卡片 `StudyModuleCard`，完成 6 大学习模块（背单词、语法等）的静态渲染，并为未开发模块接入了 `Snackbar` ("该功能正在 Garden 中萌芽...") 的拦截提示。
* **Tab 3：数据与花园 (Data & Garden - 核心 IP)**:
  * **看板区**: 摒弃沉重的第三方图表库，纯 Compose `Canvas` 手绘实现了带动画的“学习时间分布环形图” (`DonutChart`) 和“词汇量趋势折线图” (`LineChart`)。
  * **养成区**: 构建了 `GardenPlotSection` 土地网格。
  * **核心闭环逻辑**: 实现了 `GardenEngine` 和 `GardenViewModel`，彻底打通了“消耗代币 -> 购买种子 -> 浇水升级 (Seed -> Sprout -> Flower)”的虚拟 IP 核心链路，且状态持久化至 Room。
* **Tab 4：个人中心 (Profile & Rewards)**:
  * 构建了 `IdentitySection` 身份信息区，展示头像、昵称、等级 (LV) 与动态经验值进度条。
  * 构建了 `AssetGallerySection` 资产与奖励区，通过 ViewModel 实时监听全局代币余额，并搭建了成就徽章 (`AchievementBadge`) 的网格展示墙。
