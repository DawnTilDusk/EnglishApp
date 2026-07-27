# Changelog — 2026-07-27

## Android：Profile 新增词书下载与切换

在 `Profile -> 更多功能 -> 学习目标` 接入真实词书管理浮层，不再是占位 Toast。用户现在可以查看远端词书目录、下载词书，并把已下载词书设为当前学习词书；练习/测验/听力继续统一走 active 词书。默认内置词书仍作为兜底，不会在 seed 时覆盖已下载词书。

### 改动

- [`ProfileViewModel.kt`](../../app/src/main/java/com/example/seedie/ui/screens/profile/ProfileViewModel.kt)、[`ProfileScreen.kt`](../../app/src/main/java/com/example/seedie/ui/screens/profile/ProfileScreen.kt)、[`IdentitySection.kt`](../../app/src/main/java/com/example/seedie/ui/screens/profile/IdentitySection.kt)：新增“学习目标”浮层、词书列表、下载状态、切换当前词书和反馈文案。
- [`WordBookRepository.kt`](../../app/src/main/java/com/example/seedie/domain/repository/WordBookRepository.kt)、[`WordBookRepositoryImpl.kt`](../../app/src/main/java/com/example/seedie/data/repository/WordBookRepositoryImpl.kt)、[`WordBookRemoteDataSource.kt`](../../app/src/main/java/com/example/seedie/data/remote/WordBookRemoteDataSource.kt)：新增远端词书目录读取、本地下载入库、下载状态维护与 active 词书切换。
- [`WordBookSeeder.kt`](../../app/src/main/java/com/example/seedie/data/repository/WordBookSeeder.kt)、[`WordBookDao.kt`](../../app/src/main/java/com/example/seedie/data/local/dao/WordBookDao.kt)：补齐默认词书兜底与本地词书状态读写。
- [`016_seed_word_book_catalog.sql`](../../supabase/migrations/016_seed_word_book_catalog.sql)：补齐远端词书目录与词条数据，新增 `Seedie 成长阅读词书` 供客户端下载。

---

## Android：修复外研社词书下载时的 JSON null 报错

外研社词书远端数据中存在 `phonetic: null` 等字段，客户端此前按非空 `String` 解码，导致 `Unexpected JSON token ... at path: $[0].phonetic`，下载在解码阶段直接失败。本轮改为先兼容远端 `null`，再在入库前统一回落为空字符串或默认值。

### 改动

- [`Models.kt`](../../app/src/main/java/com/example/seedie/data/remote/Models.kt)：将 `phonetic`、`part_of_speech`、`translation`、`example_sentence`、`difficulty_level` 改为可空，兼容 Supabase 返回 `null`。
- [`WordBookRepositoryImpl.kt`](../../app/src/main/java/com/example/seedie/data/repository/WordBookRepositoryImpl.kt)：映射到 Room 实体前统一使用 `orEmpty()` / `?: "mixed"` 做回落，避免下载链路因空值中断。

---

## Android：补回 Gradle settings 并修复插件解析失败

根工程缺少 `settings.gradle.kts` 时，Gradle 只会去 Plugin Portal 查找插件，拿不到 Android 插件所在的 `google()` 仓库，导致 `Plugin [id: 'com.android.application' ...] was not found`。本轮补回标准 settings 配置，并在根构建脚本补齐序列化插件声明。

### 改动

- [`settings.gradle.kts`](../../settings.gradle.kts)：新增 `pluginManagement` / `dependencyResolutionManagement`，显式配置 `google()`、`mavenCentral()`、`gradlePluginPortal()`，并恢复 `include(":app")`。
- [`build.gradle.kts`](../../build.gradle.kts)：补上 `kotlin.serialization` 的 `apply false` 声明，和 `app` 模块插件使用保持一致。

---

## 机构表单改用 useActionState，消除 Webpack 模块错误

`__webpack_modules__[moduleId] is not a function`：`startTransition` + 客户端调用大包 `agency/actions` 在 revalidate/HMR 后易损坏模块图。改为各路由 colocated `actions.ts` + React `useActionState` / 原生 `form action`；删除 [`agency/actions.ts`](../../web/src/app/agency/actions.ts)。

### 改动

- [`students/actions.ts`](../../web/src/app/agency/students/actions.ts) + Create/Bind 表单
- [`teachers/actions.ts`](../../web/src/app/agency/teachers/actions.ts) + CreateTeacherForm
- [`shop/actions.ts`](../../web/src/app/agency/shop/actions.ts) + ProductForm / ToggleActiveButton
- [`lib/action-state.ts`](../../web/src/lib/action-state.ts)

---

## 学生/换绑/商城表单对齐教师页修复

核对：`create_student_account` 远端已含 `extensions`（冒烟创建+登录通过）；`CreateStudentForm` 先前已修。本轮补齐同页 `BindStudentForm`，以及商城 `ProductForm` / `ToggleActiveButton`：客户端直接 import server action，成功后 `router.refresh()`，避免 action-prop + HMR 的 webpack 错误。

### 改动

- [`BindStudentForm.tsx`](../../web/src/app/agency/students/BindStudentForm.tsx)、[`students/page.tsx`](../../web/src/app/agency/students/page.tsx)
- [`ProductForm.tsx`](../../web/src/app/agency/shop/ProductForm.tsx)、[`ToggleActiveButton.tsx`](../../web/src/app/agency/shop/ToggleActiveButton.tsx)、[`shop/page.tsx`](../../web/src/app/agency/shop/page.tsx)

---

## 修复创建教师页 Webpack 模块错误

热更新后 `__webpack_modules__[moduleId] is not a function`：将 `CreateTeacherForm` 挪到 [`teachers/`](../../web/src/app/agency/teachers/)，客户端直接 import server action（不再经 Server Component 传 prop）；学生表单同样调整；清 `.next` 重建。

### 改动

- [`teachers/CreateTeacherForm.tsx`](../../web/src/app/agency/teachers/CreateTeacherForm.tsx)、[`teachers/page.tsx`](../../web/src/app/agency/teachers/page.tsx)
- [`students/CreateStudentForm.tsx`](../../web/src/app/agency/students/CreateStudentForm.tsx)、[`students/page.tsx`](../../web/src/app/agency/students/page.tsx)

---

## 修复创建教师/学生表单 reset 空引用

`startTransition` 异步返回后 React 合成事件的 `currentTarget` 已为 null。改为先缓存 `HTMLFormElement` 再 `reset()`（教师/学生两个表单）。

### 改动

- [`CreateTeacherForm.tsx`](../../web/src/app/agency/CreateTeacherForm.tsx)
- [`CreateStudentForm.tsx`](../../web/src/app/agency/students/CreateStudentForm.tsx)
- [`ProductForm.tsx`](../../web/src/app/agency/shop/ProductForm.tsx)（同模式一并修）

---

## 修复创建教师/学生 RPC 的 gen_salt 错误

`pgcrypto` 在远端位于 `extensions` schema，而 `create_teacher_account` / `create_student_account` / `create_agency_admin` 的 `search_path` 未包含它，导致 `function gen_salt(unknown) does not exist`。Migration [`019_fix_pgcrypto_account_rpcs.sql`](../../supabase/migrations/019_fix_pgcrypto_account_rpcs.sql) 已应用：限定 `extensions.crypt` / `extensions.gen_salt`，并补写 `auth.identities`；service_role 冒烟创建教师 + 密码登录通过。

### 文档

- 更新 [`docs/2026-06-28/teacher_setup.md`](../2026-06-28/teacher_setup.md) §8。

---

## 修复 Web 登录后立刻退回登录页


浏览器端改用 `@supabase/ssr` 的 `createBrowserClient`，登录 session 写入 cookie，与 middleware / Server Component 一致；登录成功后 `router.refresh()`。根因是此前纯 `@supabase/supabase-js` 只写 localStorage，服务端 `getUser()` 读不到。

### 改动

- [`web/src/lib/supabase/client.ts`](../../web/src/lib/supabase/client.ts)
- [`web/src/app/login/LoginClient.tsx`](../../web/src/app/login/LoginClient.tsx)

### 文档

- 更新 [`docs/2026-06-28/teacher_setup.md`](../2026-06-28/teacher_setup.md) §6（cookie session）。

---

## 重建机构管理员账号

远端机构 `temp_agency` 此前无 `agency_admin` 用户。已用 Auth Admin API 创建 `1@1.com`，并将 `profiles` 设为 `agency_admin`（机构 UUID `1d13f462-…`）；密码登录 smoke 通过。（其后 `019` 已修好 `create_agency_admin` 的 `gen_salt`，可再走 RPC。）

### 文档

- 更新 [`docs/2026-06-28/teacher_setup.md`](../2026-06-28/teacher_setup.md) §8 与联调账号表。

---

## 机构端 Web：创建学生 + 商城目录重构

### 改动

- [`/agency/students`](../../web/src/app/agency/students/page.tsx)：新增「创建学生」表单（邮箱/密码/姓名/学号班级/**必选教师**），调用 `create_student_account`；保留换绑。
- [`createStudentAction`](../../web/src/app/agency/actions.ts) / [`CreateStudentForm`](../../web/src/app/agency/students/CreateStudentForm.tsx)。
- [`/agency/shop`](../../web/src/app/agency/shop/page.tsx) 按目录台重构：状态筛选、一键上/下架（`setProductActiveAction`）、单次编辑（`?edit=`）；去掉行内 compact 表单；新建默认上架，编辑不再改 `is_active`。

### 文档

- 更新 [`docs/2026-06-28/teacher_setup.md`](../2026-06-28/teacher_setup.md) §4 / §5 / §5.1。

---

## 教师端 Web：名下学生只读明细

### 改动

- 抽取 `requireAssignedStudent`（[`web/src/lib/teacher-student.ts`](../../web/src/lib/teacher-student.ts)）：教师鉴权 + 名下学生校验。
- [`/teacher`](../../web/src/app/teacher/page.tsx)：支持 `?q=` 按姓名筛选；区分「无绑定」与「筛选无结果」空态。
- [`/teacher/students/[id]`](../../web/src/app/teacher/students/[id]/page.tsx)：`?section=` 切换概览 / 签到 / 词书 / 流水只读明细（汇总仍走 `get_teacher_student_stats`）。
- 远端 RLS smoke：教师对 `students`、签到、词书进度/轮次、经济流水的 SELECT policy 均存在；本轮未新增 migration。

### 文档

- 更新 [`docs/2026-06-28/teacher_setup.md`](../2026-06-28/teacher_setup.md) §6.1 教师 Web 能力说明。
