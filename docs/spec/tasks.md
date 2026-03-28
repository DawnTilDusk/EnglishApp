# Seedie App 任务拆解 (Tasks)

## 阶段一：基础工程与核心 UI 规范
- [ ] T1.1: 配置横屏锁定、Android 15 Target SDK，清理无用资源。
- [ ] T1.2: 引入必要依赖 (Compose Navigation, Room, Hilt, Coroutines 等)。
- [ ] T1.3: 搭建 `ui/theme` 核心色盘、24dp 圆角 Shape 及自定义 `gardenShadow` 扩展。

## 阶段二：底层数据流与状态中枢
- [ ] T2.1: 构建 Room Database 层 (实体表：任务、打卡、植物、资产)。
- [ ] T2.2: 实现 `UserSessionRepository`，暴露全局核心数据状态 (`StateFlow`)。
- [ ] T2.3: 实现 `EconomyManager`，提供统一的代币增删改查接口与账单记录。
- [ ] T2.4: 建立 `RewardEventBus`，提供全局动效事件的分发机制。

## 阶段三：主页面框架与全局路由
- [ ] T3.1: 搭建基于 Compose Navigation 的全局路由 (Splash -> Main)。
- [ ] T3.2: 开发前置路由 `Splash/Check-in` 页及初始签到动画逻辑。
- [ ] T3.3: 搭建主框架：实现 `HorizontalPager` 及与其绑定的底部导航栏 (`BottomNavigationBar`)。
- [ ] T3.4: 开发中控联动指示器 (Slot API)，实现随 Pager 滚动的形变和颜色渐变动效。

## 阶段四：Tab 2 学习模块中心 (生产端)
- [ ] T4.1: 设计模块配置结构 (`ModuleConfig`) 和路由分发器 (`StudyRouter`)。
- [ ] T4.2: 开发统一的 `StudyModuleCard`，支持图标、标题、进度红点展示。
- [ ] T4.3: 使用 `LazyVerticalGrid` 完成网格布局，接入“空实现”交互反馈 (Snackbar)。
- [ ] T4.4: 建立子页面回调契约 (`StudyResult`) 接口，以便打通任务系统。

## 阶段五：Tab 1 首页概览 (展示端)
- [ ] T5.1: 搭建首页整体 Layout，区分左侧热力图区域与右侧任务区域。
- [ ] T5.2: 开发日历热力图组件 (`HeatmapCalendar`)，对接 `StatisticsRepository` 的历史数据渲染颜色。
- [ ] T5.3: 开发每日任务列表组件，支持跨天重置与选中状态响应。
- [ ] T5.4: 在任务完成逻辑中对接 `RewardEventBus` 和 `EconomyManager`，实现奖励闭环。

## 阶段六：Tab 4 个人中心 (资产端)
- [ ] T6.1: 搭建身份信息卡片，展示用户基础属性及等级进度。
- [ ] T6.2: 搭建奖励与资产面板，实时订阅并展示当前代币数量。
- [ ] T6.3: 开发成就徽章组件 (支持 Vector 图标及灰度/彩色过滤)。
- [ ] T6.4: 实现成就触发引擎，基于历史学习数据自动解锁对应的里程碑。

## 阶段七：Tab 3 数据与花园 (消费与 IP 端)
- [ ] T7.1: 使用原生 Canvas 绘制数据看板的环形比例图及折线图，添加入场弹性动画。
- [ ] T7.2: 设计并开发 `GardenEngine`，监听词汇量增长以转换花园养料。
- [ ] T7.3: 搭建花园网格 (`Garden Plot`) 及其 Slot 架构。
- [ ] T7.4: 基于策略模式实现至少一种植物 (Seed -> Sprout -> Flower) 的不同等级渲染。

## 阶段八：系统联调与细节打磨
- [ ] T8.1: 全局状态测试：从 Tab 2 学习 -> Tab 1 任务完成 -> Tab 4 余额增加 -> Tab 3 花园生长的完整闭环。
- [ ] T8.2: 优化 Pager 滑动流畅度与状态同步的性能。
- [ ] T8.3: 添加并调试全局事件总线的金币掉落及植物升级等视觉动效。