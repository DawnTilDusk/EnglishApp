# Changelog 2026-09-05

## 创建学生撞 `users_email_partial_key`

### 做了什么

- 根因：`auth.users.email` 有唯一索引 `users_email_partial_key`；机构 Web 创建学生时若邮箱已存在（含浏览器把管理员 `1@1.com` 填进表单），RPC 把 Postgres 约束名直接抛给 UI
- 修复：`create_student_account` / `create_teacher_account` 先规范化并检查邮箱，冲突时返回「该邮箱已被使用」；表单字段改名并关闭自动填充

### 关键路径

- [`040_create_account_unique_email.sql`](../../supabase/migrations/040_create_account_unique_email.sql)
- [`web/src/app/agency/students/CreateStudentForm.tsx`](../../web/src/app/agency/students/CreateStudentForm.tsx) / [`actions.ts`](../../web/src/app/agency/students/actions.ts)
- [`web/src/app/agency/teachers/CreateTeacherForm.tsx`](../../web/src/app/agency/teachers/CreateTeacherForm.tsx) / [`actions.ts`](../../web/src/app/agency/teachers/actions.ts)

### 文档

- 修订 [teacher_setup.md](../2026-06-28/teacher_setup.md) §4

---

## 新学生 App 登录后闪退

### 做了什么

- 根因：机构新建学生（如 `5@5.com`）登录后主界面 `MainViewModel` 会拉待复习词书；没有已下载词书时 `WordBookSeeder.loadActiveBook()` 直接 `error()`，协程未捕获导致进程退出。空词汇趋势图对空 `indices` 调用 `coerceIn` 同样会崩。另：`set_my_phone` 在 017 后用 `COALESCE(status,'active')`，绑手机号无法把新建学生从 `inactive` 激活
- 修复：内置词书兜底、登录/签到/任务/趋势刷新不再把异常抛出主协程；绑手机号将 `status` 设为 `active`，并回填已绑手机仍 inactive 的学生

### 关键路径

- [`WordBookSeeder.kt`](../../app/src/main/java/com/example/seedie/data/repository/WordBookSeeder.kt)
- [`MainViewModel.kt`](../../app/src/main/java/com/example/seedie/ui/screens/main/MainViewModel.kt) / [`SplashViewModel.kt`](../../app/src/main/java/com/example/seedie/ui/screens/splash/SplashViewModel.kt)
- [`041_activate_on_phone_bind.sql`](../../supabase/migrations/041_activate_on_phone_bind.sql)

### 文档

- 修订 [teacher_setup.md](../2026-06-28/teacher_setup.md) §4
