# Seedie App 任务拆解 (Tasks)

## 阶段一：基础工程与核心 UI 规范
- [x] T1.1: 配置横屏锁定 (`landscape`)、Android 15 Target SDK，适配 2560*1600 分辨率。
- [x] T1.2: 按照 Clean Architecture 搭建基础目录结构 (`ui`, `domain`, `data`, `di`)。
- [x] T1.3: 引入官方核心依赖 (Compose Navigation, Room, Hilt, Coroutines) 并完成基础配置。
- [x] T1.4: 搭建 `ui/theme` 核心色盘 (主色#66BB6A等)、空间底色、24dp 圆角 `CardShape` 及自定义 `gardenShadow` 扩展。

## 阶段二：底层数据流与状态中枢
- [ ] T2.1: 构建 Room 数据库层 (实体表：任务、打卡、植物、资产)。
- [ ] T2.2: 实现 `UserSessionRepository`，作为 Source of Truth 暴露全局核心数据状态。
- [ ] T2.3: 实现 `EconomyManager`，提供统一的代币增删改查及账单记录接口。
- [ ] T2.4: 建立 `RewardEventBus` (SharedFlow)，提供全局动效事件的分发机制。

## 阶段三：主页面框架与全局路由
- [ ] T3.1: 搭建全局路由 (Splash -> Main)。
- [ ] T3.2: 开发 Splash/Check-in 页：实现全屏背景图铺底，上层叠加居中大号带✅的“签到”按钮，点击触发代币掉落并跳转。
- [ ] T3.3: 搭建主框架：实现 `HorizontalPager` (4页) 及与其绑定的底层常驻 `BottomNavigationBar`。
- [ ] T3.4: 开发中控联动指示器 (Slot API)：悬浮于主内容与底部导航间，实现随页面滑动拉长加深颜色的动态过渡。

## 阶段四：Tab 1 首页概览 (Dashboard)
- [ ] T4.1: 搭建首页基础布局：左侧分配给学习热力图，右侧分配给每日任务列表。
- [ ] T4.2: 开发左侧热力图：包含“热力图/Calendar”标题层，基于 `LazyVerticalGrid` 的矩阵层，并在外层封装 Box/Surface 为“花园剪影”预留层级空间。
- [ ] T4.3: 开发右侧任务列表：包含“每日任务”标题层，使用 `LazyColumn` 渲染任务卡片 (含状态占位与 rewardType 预留)。
- [ ] T4.4: 对接数据：热力图读取历史打卡映射颜色深浅；任务列表支持勾选响应、跨天重置，完成时触发奖励闭环。

## 阶段五：Tab 2 学习模块中心 (Learning Hub)
- [ ] T5.1: 搭建布局：使用 `LazyVerticalGrid` 实现 2 列多行的响应式网格容器。
- [ ] T5.2: 开发标准功能卡片 `StudyModuleCard`：包含“拟物图标+主标题+微描述”，右上角预留红点/进度 Badge 位置。
- [ ] T5.3: 配置模块数据 (`ModuleConfig`)：渲染背单词、语法、测验等 6 个模块，未开发模块接入“萌芽中”的 Snackbar 提示。
- [ ] T5.4: 建立子页面回调契约 (`StudyResult`)：点击通过 `StudyRouter` 分发，完成学习后更新 `TaskManager` 状态。

## 阶段六：Tab 3 数据与花园 (Data & Garden)
- [ ] T6.1: 搭建布局：左侧为双层看板，右侧为养成区。
- [ ] T6.2: 开发左侧看板：上方使用 Canvas 绘制“学习时间分布”环形比例图，下方绘制“词汇量”趋势折线图 (支持点击气泡与弹跳入场动效)。
- [ ] T6.3: 开发右侧养成区：包含“Garden”标题层，下方构建 4x4 或 5x5 的网格土地 (`Grid Plot`)，网格上方预留天气/环境悬浮层。
- [ ] T6.4: 闭环联调：实现 `GardenEngine` 监听词汇量阈值，基于 Slot 架构和策略模式渲染植物 (Seed->Sprout->Flower) 升级。

## 阶段七：Tab 4 个人中心 (Profile & Rewards)
- [ ] T7.1: 搭建布局：左侧为身份信息卡片，右侧占据较大空间作为奖励与资产面板。
- [ ] T7.2: 开发左侧卡片：上方垂直排列的卡片展示头像/昵称/年级，下方卡片展示 LV 及升级进度条。
- [ ] T7.3: 开发右侧面板：上方展示“数字代币”余额，下方构建“成就勋章”展示墙 (预留带问号的神秘成就空位)。
- [ ] T7.4: 闭环联调：余额实时响应 `EconomyManager`，成就墙监听学习数据自动解锁对应 Vector 徽章 (灰度转彩色)。

## 阶段八：系统联调与细节打磨
- [ ] T8.1: 跑通核心链路：Tab 2 学习完成 -> Tab 1 任务完成 -> Tab 4 代币增加 -> Tab 3 植物升级。
- [ ] T8.2: 完善双层导航联动：优化指示器变形与 Tab 高亮在手指滑动过程中的精准同频。
- [ ] T8.3: 植入微交互动效：补充跨页面的金币掉落动画及 UI 状态的平滑过渡。