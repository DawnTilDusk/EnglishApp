# Seedie App Changelog (2026-07-01)

> 基于 2026-07-01 当日工作会话整理。本轮重点围绕 Tab4 真实资料接入、Supabase 架构梳理、Tab1 首页卡片轮播、全局活跃统计一期、以及 Tab3「学习时间分布」真实数据与局部 UI 微调展开。

## 本轮完成内容

### 1. Tab4 个人中心接入真实资料链路

- 完成 `Profile` 模块从假数据到真实业务数据的切换：
  - 资料读写改为走 Repository 链路
  - 年级字段使用固定选项
  - 头像色调与年级字段在数据库侧补齐约束与默认值
- 编辑资料弹层改为全屏 `Dialog` 方案：
  - 遮罩覆盖到底部导航栏
  - 支持点击遮罩关闭
  - 保持居中展示和更清晰的层级关系
- 资料信息行与学习卡片统一点击反馈规范：
  - 引入/统一 `gardenPressable`
  - ripple 严格跟随组件圆角
- 视觉细节优化：
  - “退出登录”按钮改为危险操作红色
  - 资料信息行去掉不自然的阴影按压效果，保留更轻的触控反馈

### 2. Supabase 架构梳理与清理方向明确

- 基于 schema 健康报告梳理远端结构现状：
  - 识别出早期 `agencies` 多租户设计已基本停滞
  - 识别出一批高疑似废弃/烂尾表，如 `points_ledger`、`rewards`
- 明确当前 App 实际运行的主链路：
  - `profiles`
  - `students`
  - 学习进度同步
  - 教师商城与经济系统
- 达成架构决策：
  - 业务逐步收敛到 `teachers`
  - `agencies` 相关旧结构先标记为 Legacy，再考虑清退

### 3. Tab1 首页左侧轮播卡片升级

- 新增 `DashboardFeatureCarousel` 容器，将首页左侧主视觉改为轮播卡片区
- 将热力图与“每日一句”卡片统一纳入轮播：
  - 保持双卡切换
  - 补齐英汉双语文案展示
- 动效目标调整为类似系统服务卡片的层叠感：
  - 支持伪无限循环
  - 通过 `scale`、`alpha` 和位移差制造前后层级
- 同步清理了轮播实现中的若干 Compose 细节问题：
  - 属性命名冲突
  - 委托导入缺失
  - 插值实现冗余

### 4. 本地全局活跃统计一期落地

- 明确并实现“本地全局活跃计时”一期方向：
  - 只做本地
  - 只做按天、按模块聚合
  - 暂不做远端同步和挂机判定
- 新增本地聚合链路：
  - `ActivityDurationEntity`
  - `ActivityDurationDao`
  - `ActivityTrackingRepository`
  - `GlobalActivityTracker`
- 页面追踪范围覆盖：
  - 主 Tab
  - 学习子路由
  - 商店页
  - 前后台切换
- 当前链路已能产出：
  - 今日模块活跃时长汇总
  - 最近多日总时长趋势

### 5. Tab3 统计 UI 边界回收与真实数据接线

- 修正过度改动问题：
  - 恢复了原有 `Tab3` 词汇量趋势、学习时间分布、下拉交互和整体视觉骨架
  - 保留后台统计链路，但不再误把整张统计面板都替换成活跃时长 UI
- `学习时间分布` 正式接入真实数据：
  - 从 `GardenViewModel` 输出统一 `UiState`
  - 基于今日学习相关模块汇总生成分布项
  - 支持超过 3 个栏目同时展示
- 保持下方 `词汇量趋势` 卡片结构与交互不变，避免继续越界

### 6. Tab3「学习时间分布」局部 UI 多轮微调

- 第一轮微调：
  - 恢复进入 Tab3 时的环形图展开动效
  - 所有分钟文案统一改为 `min`
  - 环形条带略微加粗
  - 顶部按钮文案改为 `详细分类`
  - 展开后改为“左图右例”布局
- 第二轮微调：
  - 右侧图例容器颜色与主卡片视觉统一
  - 保留左侧颜色点与滚动能力
  - 图例项改为更接近 Tab4 资料信息的单行列表
  - 行与行之间增加细分割线
  - 展开后环图继续左移，圆心更靠近栏目横向左三等分

### 7. 编译与运行时问题修复

- 修复 `ActivityTrackingModels.kt` 中 `const val` 使用枚举属性导致的编译错误
- 修复 `StatsPanelSection.kt` 中在 `Canvas` 绘制作用域直接访问 `MaterialTheme.colorScheme` 的编译错误
- 修复 `详细分类` 展开时 `FlowRow` 与当前 Compose 运行时不兼容导致的闪退：
  - 移除 `FlowRow`
  - 改为更兼容的列表式实现
- 修复 `AnimatedVisibility` 在嵌套作用域下被错误解析为 `ColumnScope.AnimatedVisibility` 的调用冲突

---

## 本轮产出物

### 新增 / 更新的规格文档

- `.trae/specs/add-local-global-activity-tracking-phase1/`
- `.trae/specs/connect-garden-donut-real-data/`
- `.trae/specs/refine-garden-learning-distribution-ui/`

### 关键代码关注点

- `app/src/main/java/com/example/seedie/ui/screens/garden/StatsPanelSection.kt`
- `app/src/main/java/com/example/seedie/ui/screens/garden/GardenViewModel.kt`
- `app/src/main/java/com/example/seedie/ui/screens/garden/DataGardenScreen.kt`
- `app/src/main/java/com/example/seedie/domain/usecase/GlobalActivityTracker.kt`
- `app/src/main/java/com/example/seedie/data/repository/ActivityTrackingRepositoryImpl.kt`
- `app/src/main/java/com/example/seedie/ui/screens/profile/IdentitySection.kt`
- `app/src/main/java/com/example/seedie/ui/screens/profile/ProfileScreen.kt`

## 当前实现边界

- 全局活跃统计已具备本地闭环，但仍有跨天窗口刷新与跨午夜持续停留的边界问题待继续处理
- `Tab3` 下方 `词汇量趋势` 仍以现有静态演示数据为主，本轮没有接入真实趋势统计
- `学习时间分布` 已接入真实数据并完成局部 UI 微调，但最终视觉手感仍需手动验收
- Supabase 架构清理目前停留在梳理与决策阶段，尚未进入大规模物理删除或迁移

## 建议后续事项

- 继续修复全局活跃统计跨天刷新问题
- 评估 `词汇量趋势` 后续是否需要接真实业务数据
- 在 Tab3 完成本轮手动验收后，再决定是否继续细调右侧图例和环图位置
