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

## 写作作文作业（题库 + 人工批改）

- 题库：[`025_writing_prompts_catalog.sql`](../../supabase/migrations/025_writing_prompts_catalog.sql) — `writing_prompts` + 8 道中考风格作文种子；已远程 apply（验证 `prompt_count=8`）。
- 作业扩展：[`026_writing_assignments.sql`](../../supabase/migrations/026_writing_assignments.sql) — `module_id` 含 `writing`；提交状态加 `returned`；写作列 `original_path` / `annotated_path` / `score` / `max_score` / `feedback_text` / `returned_at`；RPC `submit_writing_assignment` / `return_writing_assignment`；私有 Storage bucket `writing-submissions`；已远程 apply。
- 教师 Web：布置可选写作题；详情「批改」→ 下载原件、页内圈画并上传批改图、打分评语返还（PDF 仍打开/下载 + 文件补传）。
- Android：写作模块上线；三列未完成 / 批改中 / 已完成；拍照或选文件提交；批改中看原件；已完成看批改图与分数（点击可全屏双指缩放）；返还后代币 `assignment:{submissionId}`。
- 应用脚本：[`scripts/apply_writing_025_026.py`](../../scripts/apply_writing_025_026.py)。

### 教师页内圈画批改

- 批改页增加原件/批改件下载（fetch blob）。
- 图片作业：「开始批改」进入双层 canvas（画笔/橡皮/颜色/线宽/撤销/清空），导出压缩 JPEG（最长边≤2400、质量自适应）上传 `annotated.jpg`，避免全分辨率 PNG 触发 Storage 10MB 上限；再打分返还。
- 组件：[`WritingAnnotator.tsx`](../../web/src/app/teacher/assignments/[id]/submissions/[submissionId]/WritingAnnotator.tsx)、[`GradeWritingForm.tsx`](../../web/src/app/teacher/assignments/[id]/submissions/[submissionId]/GradeWritingForm.tsx)。

### 文档

- [`docs/2026-07-24/supabase_table_map.md`](../2026-07-24/supabase_table_map.md) — 写作目录、作业三模块、Storage
- [`docs/2026-07-24/permission_model.md`](../2026-07-24/permission_model.md) — 写作提交/返还 RPC
- [`docs/2026-06-28/teacher_setup.md`](../2026-06-28/teacher_setup.md) — 批改路由
- [`docs/2026-06-28/project_overview.md`](../2026-06-28/project_overview.md) — 写作模块已实现
- 本 changelog
