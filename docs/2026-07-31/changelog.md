# 2026-07-31 changelog

## 练习作业系统（阅读 / 听力）

- 新增远端表 `practice_assignments` / `practice_assignment_items` / `practice_assignment_recipients` / `practice_assignment_submissions`，迁移 [`022_practice_assignments.sql`](../../supabase/migrations/022_practice_assignments.sql)；已远程 apply。
- RPC：`create_practice_assignment`、`start_practice_assignment`、`submit_practice_assignment`（已提交拒绝二次提交；`answer_payload` 供回顾）。
- 教师 Web：`/teacher/assignments` 列表 / 新建（按套选题、学员全选与班级筛选、截止到分钟）/ 详情完成率；顶栏增加「作业」。
- Android：阅读/听力入口改为作业列表（未完成 / 已完成）；答题整份一次提交；已完成可只读回顾；代币 `refId = assignment:{submissionId}`；取消作业路径全库完成奖。
- 应用脚本：[`scripts/apply_practice_assignments_022.py`](../../scripts/apply_practice_assignments_022.py)。

### 布置后跳转加固

- 解析 RPC `assignment_id`（支持 jsonb 字符串）并做 UUID 校验；写后 `select` 确认可读再进详情，否则回 `/teacher/assignments?created=1`。
- 详情页非法 id 回列表；库中无记录仍 `notFound()`。

### 作业列表空白修复

- 迁移 [`023_practice_assignments_grants.sql`](../../supabase/migrations/023_practice_assignments_grants.sql)：为四表 `GRANT SELECT TO authenticated`（已远程 apply）；此前仅有 RLS、无表级授权导致布置成功但列表/详情读不到。
- 列表页展示 `select` 错误；写后校验日志带上 `readError.message`。

### RLS 无限递归修复

- 迁移 [`024_practice_assignments_rls_no_recursion.sql`](../../supabase/migrations/024_practice_assignments_rls_no_recursion.sql)（已远程 apply）：子表 policy 不再回查 `practice_assignments` 触发 RLS；改用 `private.is_practice_assignment_teacher` / `is_practice_assignment_recipient`（SECURITY DEFINER）消除 `infinite recursion detected in policy`。

### 学生端提交后列表未刷新

- `AssignmentListViewModel.initialize` 在从答题页返回时因缓存提前 return，未重新拉取 `submitted` 状态；改为每次进入/ON_RESUME 都 `refresh()`。

### 文档

- [`docs/2026-07-24/supabase_table_map.md`](../2026-07-24/supabase_table_map.md) — 作业四表与阅读/听力入口说明
- [`docs/2026-07-24/permission_model.md`](../2026-07-24/permission_model.md) — 教师布置 / 学生作答边界与 RPC
- [`docs/2026-06-28/teacher_setup.md`](../2026-06-28/teacher_setup.md) — `/teacher/assignments*` 路由
- [`docs/2026-06-28/project_overview.md`](../2026-06-28/project_overview.md) — 学习模块行为
- 本 changelog
