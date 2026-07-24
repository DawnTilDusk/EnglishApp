# Changelog

日期：2026-06-30  
提交区间：`a1f5d9b29f2bea0fbd2d4d606c6e7b4653d3a304..2197919bc0ce8220da059aaf5b9d1176de0f373e`

## 1. 概览
- 本次区间共包含 9 个提交，重点集中在 Tab4 `Profile` 模块的真实业务接入、资料编辑交互完善，以及通用卡片/点击反馈样式收敛。
- Android 端新增了 `ProfileRepository` 读写链路，打通了个人资料查询、资料更新、手机号绑定与业务会话刷新。
- Supabase 端新增 `009_profile_real_data.sql`，为 `profiles` 表补齐 `grade`、`avatar_tone` 字段约束，并提供 `set_my_profile`、`set_my_phone` RPC。
- UI 层完成了资料页编辑弹窗、手机号绑定弹窗、年级受限下拉选择，以及 `gardenPressable` / `TabSectionSurface` 等通用交互和样式抽象。
- 收尾阶段清理了临时调试文件，并将 Supabase 架构体检结果归档到 `docs/2026-06-30`。

## 2. 重点变更

### 2.1 Profile 真实业务接入
- 新增 `ProfileRepository` 接口及 `ProfileRepositoryImpl` 实现，负责读取当前用户资料、调用 RPC 更新资料、绑定手机号，并在更新资料后刷新业务会话。
- `ProfileViewModel` 从静态展示演进为真实状态管理，新增 `ProfileIdentityUiState` / `ProfileEditorUiState`，统一承载加载、保存、手机号绑定、消息提示等状态。
- `Models.kt` 中的 `Profile` DTO 新增 `grade`、`phone`、`phone_verified`、`phone_updated_at`、`avatar_tone` 等字段映射，保证客户端能消费新的远端资料结构。
- `RepositoryModule.kt` 中注册了 `ProfileRepository` 的 Hilt 绑定，完成依赖注入闭环。

### 2.2 Supabase schema 与 RPC 补强
- `profiles` 表新增 `grade` 与 `avatar_tone` 字段，并对历史空值和非法值进行了回填。
- `grade` 被收敛为固定枚举：小学一年级到高三及 `其他`，默认值为 `一年级`，同时追加 `CHECK` 约束和 `NOT NULL` 约束。
- 新增 `set_my_profile(display_name, grade, avatar_tone)` RPC，服务端统一兜底年级合法性与头像索引校验。
- 新增 `set_my_phone(phone)` RPC，用于个人手机号绑定，并同步维护 `phone_verified`、`phone_updated_at` 等字段。

### 2.3 Profile 页面与编辑流完善
- `IdentitySection` 完成了个人资料面板的主结构：头像名片、资料信息、成长进度、更多功能、退出登录按钮。
- 新增“编辑资料”入口，支持修改用户名、切换头像样式、使用受限下拉框选择年级。
- 新增手机号绑定弹窗，带基础格式校验与错误提示，提交成功后刷新本地资料状态。
- `ProfileScreen` 引入全屏 `Dialog` 式资料编辑浮层，遮罩层负责点击关闭，内容区域单独拦截点击。
- 后续样式提交进一步收窄了资料编辑弹窗宽度，改善桌面大屏下的浮层观感。

### 2.4 通用 UI 与交互规范收敛
- 提取 `TabSectionSurface` 通用组件，将 Dashboard、Garden、Learning、Profile 等页面卡片统一为一致的渐变背景、描边与阴影风格。
- 提取并简化 `gardenPressable` 修饰符，通过 `clip(shape).clickable(...)` 统一控制点击反馈裁切范围。
- `ProfileInfoRow` 的点击反馈从原先较重的按压表现调整为更轻量的 ripple 交互，避免阴影与透明背景、分割线产生冲突。
- “退出登录”按钮改为 `MaterialTheme.colorScheme.error` 风格，强化危险操作提示。

### 2.5 文档与临时文件清理
- 新增并归档 `supabase_schema_health_report.md`，系统梳理远端 `public` schema 的在用主链路表、待确认表与 legacy 候选表。
- 删除 `todo_list.md`、截图探针文件 `seedie_probe.png` / `seedie_tab4.png` / `tmp_tab4_probe.png` 以及 `window_dump.xml` 等临时调试产物。
- `docs/2026-06-28/debug-trend-dropdown-crash.md` 被纳入版本管理归档，保留问题排查记录。

## 3. 提交清单

| Commit | 类型 | 摘要 |
| --- | --- | --- |
| `d5ae994` | feat(profile) | 完善个人资料页面，新增手机号绑定入口并优化文案 |
| `89b72b8` | feat(profile) | 实现完整的用户个人资料管理功能 |
| `e978767` | refactor(ui) | 提取通用 `TabSectionSurface` 组件统一卡片样式 |
| `48ac329` | fix(profile) | 将退出登录按钮改为错误提示色 |
| `769523f` | refactor(profile) | 重构 `ProfileInfoRow` 点击反馈 |
| `4d7659f` | refactor(ui) | 提取通用 `pressable` 交互修饰符 |
| `837a455` | refactor(ui.theme) | 简化 `gardenPressable` 并清理导入 |
| `60e5d7b` | style(profile screen) | 调整个人资料编辑弹窗宽度 |
| `2197919` | chore | 清理无用临时文件并归档调试文档 |

## 4. 影响范围

### Android 代码
- `app/src/main/java/com/example/seedie/data/remote/Models.kt`
- `app/src/main/java/com/example/seedie/data/repository/ProfileRepositoryImpl.kt`
- `app/src/main/java/com/example/seedie/di/RepositoryModule.kt`
- `app/src/main/java/com/example/seedie/domain/profile/ProfileGradeOptions.kt`
- `app/src/main/java/com/example/seedie/domain/repository/ProfileRepository.kt`
- `app/src/main/java/com/example/seedie/ui/components/TabSectionSurface.kt`
- `app/src/main/java/com/example/seedie/ui/screens/profile/IdentitySection.kt`
- `app/src/main/java/com/example/seedie/ui/screens/profile/ProfileScreen.kt`
- `app/src/main/java/com/example/seedie/ui/screens/profile/ProfileViewModel.kt`
- `app/src/main/java/com/example/seedie/ui/theme/ModifierExt.kt`

### Supabase
- `supabase/migrations/009_profile_real_data.sql`

### 文档与归档
- `docs/2026-06-30/supabase_schema_health_report.md`
- `docs/2026-06-28/debug-trend-dropdown-crash.md`

## 5. 对外可见结果
- 用户现在可以在 Tab4 查看并编辑真实个人资料，而不是停留在静态展示。
- 年级字段改为受控选项，客户端与数据库约束保持一致，降低脏数据风险。
- 手机号绑定入口和交互已具备，可将号码同步到个人资料与认证用户数据。
- Profile、Learning、Garden、Dashboard 多处卡片和点击反馈样式更加统一。
- 项目目录中的临时调试文件得到清理，文档归档更完整。

## 6. 风险与备注
- 本次区间包含数据库迁移与 RPC 变更，部署时需要确保 `009_profile_real_data.sql` 已正确应用到远端环境。
- `ProfileViewModel` 的资料读写已切换到真实远端链路，若 Supabase 侧 schema 或 RPC 未同步，Profile 页面会出现加载或保存失败提示。
- `docs/2026-06-30/supabase_schema_health_report.md` 已对远端旧表和 legacy 候选表做出初步判断，但尚未进入删除阶段。
