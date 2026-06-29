# Seedie App Changelog (2026-06-29)

> 记录从 `e2a70b853ba574064d1b0540135b8e8717ac721c` 到 `5a2fe336c977afeeb7ebdf8660016ef48cf560e9` 的全部改动。本轮主要覆盖教师端接入、学生商城与代币同步、数据花园大规模重构、词汇练习细节优化，以及启动签到页状态管理调整。

## 对比基准

| 项 | 内容 |
|----|------|
| **起始 commit** | `e2a70b8` - 教师数据库搭建 |
| **结束 commit** | `5a2fe33` - 重构启动页与花园页的状态管理及 UI 逻辑 |
| **区间 commits** | `e2a70b8` → `eef410a` → `2a16a97` → `dbf2850` → `d4704e3` → `11efb0a` → `4b67790` → `df47d0b` → `129fa85` → `389279a` → `74e219e` → `6f789d5` → `8559785` → `220ff88` → `5a2fe33`（共 15 个） |
| **diff 规模** | 62 files changed，+5040 / -377 |

### 能力变化概览

| 维度 | 起始阶段 | 结束阶段 |
|------|----------|----------|
| 登录体系 | 以学生端为主，教师能力尚未完整打通 | 支持学生 / 教师双模式登录，按角色切换不同主导航 |
| 教师端 | 仅数据库与基础表设计起步 | 具备教师工作台、学生看板、教师商城 Demo |
| 商城与代币 | 学生端代币与商城链路不稳定 | 增加 RPC 批量同步、余额对账与刷新结果模型 |
| 数据花园 | 基础看板与地块展示 | 重构为带筛选、趋势图、交互地块、详情面板的完整 Dashboard |
| 词汇练习 | 标准四选一 | 新增“全都不对”选项并补齐对应 ViewModel 测试 |
| 启动签到 | 登录后固定先进入签到页 | 启动阶段读取当天签到状态，已签到可直接跳过 |

---

## 本轮完成内容

### 1. 教师端基础设施与角色登录

- 补全教师端域模型与仓储接口：
  - 新增 `UserRole`、`ShopModels`、`TeacherRepository`、`ShopRepository`
  - 在 `RepositoryModule` 中接入教师相关实现
- 打通教师端导航与页面骨架：
  - 新增 `TeacherNavHost`、`TeacherScreen`、`TeacherBottomNavigationBar`
  - 新增 `TeacherMainScreen`、`TeacherDashboardScreen`、`TeacherShopScreen`
- 调整全局鉴权状态：
  - `MainActivity` 根据 `session.role` 在 `TeacherNavHost` 与 `SeedieNavHost` 间切换
  - `MainViewModel` 改为显式管理 `Loading / LoggedOut / LoggedIn`
  - `AuthService` 增加业务 session 恢复、登录错误暂存与清理逻辑
- 登录页升级为双模式：
  - `LoginScreen` / `LoginViewModel` 支持“学生登录 / 教师登录”切换
  - 学生端保留手机号校验，教师端沿用邮箱密码登录

### 2. 学生商城与代币同步闭环

- 远端数据源侧新增 `EconomyRemoteDataSource`：
  - 支持查询本人余额
  - 支持 RPC 批量同步交易
  - 支持余额对账与单条交易补录
- 代币同步链路增强：
  - `EconomyTransactionDao` 增加同步辅助查询
  - `EconomyTransactionSyncer` 引入 `EconomySyncResult`
  - `EconomyManager` / `EconomyManagerImpl` 暴露余额刷新与对账能力
  - 新增 `BalanceRefreshResult` 作为刷新结果模型
- 学生商城体验补齐：
  - `StudentShopViewModel` 增加同步状态与刷新动作
  - `StudentShopScreen` 可展示同步中状态、结果反馈与更完整的商品交互
- Supabase 侧补充代币与商城相关 SQL：
  - `002_shop_and_economy.sql`
  - `003_teacher_shop_rpc.sql`
  - `006_economy_sync_rpc.sql`
  - `007_sync_economy_batch_rpc.sql`
  - `008_reconcile_token_balance.sql`

### 3. 退出登录与会话切换体验

- 新增 `LogoutConfirmDialog` 统一承载退出确认弹窗
- `ProfileScreen` / `ProfileViewModel` 接入退出登录入口
- `TeacherMainScreen` / `TeacherMainViewModel` 同步支持教师端退出登录
- 配合 `AuthService` 与 `MainViewModel` 的会话恢复逻辑，减少角色切换或异常登录后的脏状态残留

### 4. 数据花园大规模重构

**地块区**

- `GardenPlotSection` 从基础网格演进为完整交互式地块模块
- 新增选中地块状态、详情面板、悬浮操作栏与更细的文案表达
- 后续迭代中补充“选中地块自动隐藏”与若干 UI 交互收口
- 最终在 `5a2fe33` 中继续清理状态管理与 UI 逻辑，收敛地块区实现复杂度

**统计区**

- `StatsPanelSection` 成为本轮改动最集中的文件，累计完成：
  - 统计卡片与环形图卡片重排
  - 趋势图与词汇量趋势展示
  - 趋势筛选菜单重构
  - Tab 切换后的图表刷新修复
  - 下拉菜单闪退问题修复
  - 多轮 UI 清理与组件拆分
- `DataGardenScreen` 与 `MainScreen` 配合调整，确保花园页重播动画、面板刷新与主页面入口联动正常
- `debug-trend-dropdown-crash.md` 记录了趋势筛选菜单闪退问题的排查结果，便于后续追踪

### 5. 词汇练习交互补强

- `VocabularyPracticeViewModel` 新增“全都不对”选项注入逻辑
- `VocabularyPracticeModels` 增补对应状态表达，避免错误选项集下的提示不清晰
- `PracticeOptionCard` 做了轻量配套调整，适应新增选项的展示
- 新增 `VocabularyPracticeViewModelOptionInjectionTest`，对新题型分支补齐 JVM 单元测试

### 6. 启动页签到逻辑调整

- `SplashViewModel` 引入 `SplashUiState`
- 启动时先根据 `CheckInDao` 查询当天签到状态
- 若已签到则直接跳过签到按钮，未签到时才展示签到入口
- 这一轮把签到页从“每次进入都必须手动确认”改为“由持久化记录驱动的启动判断”

### 7. 数据库与部署文档同步

- 新增教师端与用户同步相关迁移：
  - `000_user_sync_tables.sql`
  - `001_add_teacher_role.sql`
  - `004_verify_teacher_setup.sql`
  - `005_fix_teacher_account.sql`
- 更新 `docs/teacher_setup.md`，补充教师账号、教师端环境与迁移执行说明
- `.gitignore` 与部分 IDE 配置文件有轻微调整，用于适配本轮开发环境

---

## 当前可验证结果

- 登录页可切换学生 / 教师两种模式，并根据角色进入不同主界面
- 教师端可进入工作台与商城 Demo，且支持退出登录
- 学生端商城具备更完整的代币同步与余额刷新链路
- 数据花园已具备更强的视觉层级、筛选能力与交互式地块体验
- 词汇练习支持“全都不对”选项，相关逻辑已补测试
- 学生端当天已签到时，再次进入应用可直接跳过签到页

## 当前实现边界

- 教师端整体仍以 Demo / 管理骨架为主，后续仍需补更多业务能力
- 代币同步虽然引入了 RPC 批处理与对账，但仍需要真实联调验证边界场景
- 数据花园本轮主要完成 UI 与状态重构，是否全部接入真实统计数据仍需继续核对
- 启动签到页已接入本地持久化判断，但跨端一致性仍依赖后续同步链路稳定性

## Commit 索引

| Commit | 说明 |
|--------|------|
| `e2a70b8` | 教师数据库搭建 |
| `eef410a` | 学生端商城 demo 实现 |
| `2a16a97` | 教师端登录 demo 实现 |
| `dbf2850` | 添加登出功能 |
| `d4704e3` | 试图修复代币同步功能 |
| `11efb0a` | 实现完整的交互式花园地块模块 |
| `4b67790` | 重构花园页面统计环形图卡片的 UI 布局 |
| `df47d0b` | 词汇练习新增“全都不对”选项并优化提示显示逻辑 |
| `129fa85` | 重构统计面板，新增筛选并修复 tab 图表刷新问题 |
| `389279a` | 重构花园统计面板筛选菜单并优化 UI 布局 |
| `74e219e` | 修复数据花园趋势筛选下拉菜单闪退问题 |
| `6f789d5` | 重构趋势筛选菜单与相关组件 |
| `8559785` | 重构花园地块界面，新增悬浮操作栏与详情面板 |
| `220ff88` | 优化花园页面 UI 文案与交互，新增选中地块自动隐藏 |
| `5a2fe33` | 重构启动页与花园页的状态管理及 UI 逻辑 |
