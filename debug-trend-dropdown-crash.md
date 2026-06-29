# [OPEN] trend-dropdown-crash

## 症状
- 点击数据花园 `词汇量趋势` 卡片中的筛选按钮后发生闪退。

## 期望
- 点击按钮后只在按钮正下方展开局部下拉菜单，不应崩溃。

## 当前上下文
- 最近将趋势筛选菜单从自定义浮层改成了 `DropdownMenu`。
- 改动主要集中在 `app/src/main/java/com/example/seedie/ui/screens/garden/StatsPanelSection.kt`。

## 假设
- H1: `DropdownMenu` 被放在错误的组合层级，缺少正确锚点，点击展开时触发运行时异常。
- H2: 菜单尺寸依赖 `buttonWidth`，在某些首次点击时为 `0.dp` 或非法尺寸，导致 Compose 布局崩溃。
- H3: `DropdownMenu` 与当前外层 `BoxWithConstraints` / `zIndex` / `AnimatedVisibility` 组合发生不兼容，展开时触发窗口定位异常。
- H4: 闪退并不在 `DropdownMenu` 本身，而是在点击后触发的其他状态更新路径，例如 `expandedMenu` 切换导致趋势卡某处访问越界或空状态。

## 证据计划
- 在趋势筛选按钮点击前后记录：按钮标签、当前展开状态、测量宽度。
- 在菜单展开分支记录：菜单类型、宽度、高度约束、选项数量。
- 在趋势卡状态切换处记录：`expandedMenu`、`selectedRange`、`selectedMetric`。

## 状态
- 已获得运行时证据，准备修复

## 证据结论
- 运行时崩溃栈显示：`java.lang.IllegalStateException: Vertically scrollable component was measured with an infinity maximum height constraints`
- 调用链包含 `DropdownMenu` / `AndroidPopup` / `Column(Modifier.verticalScroll(...))`
- 由此可确认：
  - H1: 部分成立。问题发生在弹出式菜单路径，但根因不是“按钮点击逻辑”，而是菜单内部滚动容器与 Popup 测量约束冲突。
  - H2: 否。日志没有显示 `0.dp` 或非法宽度导致的尺寸异常。
  - H3: 成立。`DropdownMenu` 处于 Popup 测量环境下时，内部 `verticalScroll` 触发了无限高度约束崩溃。
  - H4: 否。崩溃栈没有指向 `expandedMenu`、范围切换或指标切换的数据状态路径。

## 修复策略
- 移除 `DropdownMenu` 内部的 `verticalScroll` 容器，改成普通 `Column`。
- 当前选项数量固定较少（范围 5 项、指标 2 项），无需在本次修复中保留滚动行为。
