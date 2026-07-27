# Changelog — 2026-07-27

## 教师端 Web：名下学生只读明细

### 改动

- 抽取 `requireAssignedStudent`（[`web/src/lib/teacher-student.ts`](../../web/src/lib/teacher-student.ts)）：教师鉴权 + 名下学生校验。
- [`/teacher`](../../web/src/app/teacher/page.tsx)：支持 `?q=` 按姓名筛选；区分「无绑定」与「筛选无结果」空态。
- [`/teacher/students/[id]`](../../web/src/app/teacher/students/[id]/page.tsx)：`?section=` 切换概览 / 签到 / 词书 / 流水只读明细（汇总仍走 `get_teacher_student_stats`）。
- 远端 RLS smoke：教师对 `students`、签到、词书进度/轮次、经济流水的 SELECT policy 均存在；本轮未新增 migration。

### 文档

- 更新 [`docs/2026-06-28/teacher_setup.md`](../2026-06-28/teacher_setup.md) §6.1 教师 Web 能力说明。
