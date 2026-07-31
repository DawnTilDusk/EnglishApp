import { requireTeacher } from "@/lib/auth";
import { ConsoleShell } from "@/components/ConsoleShell";
import { createClient } from "@/lib/supabase/server";
import Link from "next/link";
import { notFound, redirect } from "next/navigation";
import {
  formatDueAt,
  isTurnedInStatus,
  isUuid,
  moduleLabel,
  submissionStatusLabel,
  teacherTabs,
} from "@/lib/teacher-assignments";

export default async function AssignmentDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;
  if (!isUuid(id)) {
    redirect("/teacher/assignments");
  }

  const profile = await requireTeacher();
  const supabase = await createClient();

  const { data: assignment } = await supabase
    .from("practice_assignments")
    .select("id, module_id, title, due_at, allow_late, created_at, teacher_id")
    .eq("id", id)
    .eq("teacher_id", profile.id)
    .maybeSingle();

  if (!assignment) notFound();

  const isWriting = assignment.module_id === "writing";

  const [{ data: items }, { data: submissions }] = await Promise.all([
    supabase
      .from("practice_assignment_items")
      .select("item_ref, sort_order")
      .eq("assignment_id", id)
      .order("sort_order"),
    supabase
      .from("practice_assignment_submissions")
      .select(
        "id, student_id, status, submitted_at, correct_count, total_count, earned_tokens, score, max_score, returned_at"
      )
      .eq("assignment_id", id),
  ]);

  const studentIds = (submissions ?? []).map((s) => s.student_id as string);
  const nameById = new Map<string, string>();
  if (studentIds.length > 0) {
    const { data: students } = await supabase
      .from("students")
      .select("id, name")
      .in("id", studentIds);
    for (const s of students ?? []) {
      nameById.set(s.id as string, s.name as string);
    }
  }

  const itemRefs = (items ?? []).map((i) => i.item_ref as string);
  const titleByRef = new Map<string, string>();
  const promptTextByRef = new Map<string, string>();
  if (itemRefs.length > 0) {
    if (assignment.module_id === "reading") {
      const { data: sets } = await supabase
        .from("reading_sets")
        .select("set_id, title, title_zh")
        .in("set_id", itemRefs);
      for (const s of sets ?? []) {
        titleByRef.set(
          s.set_id as string,
          (s.title_zh as string) || (s.title as string)
        );
      }
    } else if (assignment.module_id === "listening") {
      const { data: mats } = await supabase
        .from("listening_materials")
        .select("material_id, title, title_zh")
        .in("material_id", itemRefs);
      for (const m of mats ?? []) {
        titleByRef.set(
          m.material_id as string,
          (m.title_zh as string) ||
            (m.title as string) ||
            (m.material_id as string)
        );
      }
    } else {
      const { data: prompts } = await supabase
        .from("writing_prompts")
        .select("prompt_id, title, title_zh, prompt_text, prompt_text_zh")
        .in("prompt_id", itemRefs);
      for (const p of prompts ?? []) {
        titleByRef.set(
          p.prompt_id as string,
          (p.title_zh as string) || (p.title as string)
        );
        promptTextByRef.set(
          p.prompt_id as string,
          (p.prompt_text_zh as string) || (p.prompt_text as string) || ""
        );
      }
    }
  }

  const turnedInCount = (submissions ?? []).filter((s) =>
    isTurnedInStatus(s.status as string)
  ).length;
  const totalCount = submissions?.length ?? 0;

  return (
    <ConsoleShell
      profile={profile}
      title="教师控制台"
      tabs={teacherTabs("assignments")}
    >
      <div className="card stack">
        <div className="row" style={{ justifyContent: "space-between" }}>
          <h2 style={{ margin: 0 }}>{assignment.title}</h2>
          <Link className="btn secondary" href="/teacher/assignments">
            返回列表
          </Link>
        </div>
        <p className="muted" style={{ margin: 0 }}>
          {moduleLabel(assignment.module_id as string)} · 截止{" "}
          {formatDueAt(assignment.due_at as string)}
          {assignment.allow_late ? " · 允许补交" : " · 不可补交"} · 已交{" "}
          {turnedInCount}/{totalCount}
        </p>

        <h3 style={{ marginBottom: 0 }}>题目</h3>
        <ol>
          {(items ?? []).map((item) => {
            const ref = item.item_ref as string;
            const body = promptTextByRef.get(ref);
            return (
              <li key={ref}>
                <div>{titleByRef.get(ref) ?? ref}</div>
                {body ? (
                  <p className="muted" style={{ whiteSpace: "pre-wrap", marginTop: 8 }}>
                    {body}
                  </p>
                ) : null}
              </li>
            );
          })}
        </ol>

        <h3 style={{ marginBottom: 0 }}>学员提交</h3>
        <table>
          <thead>
            <tr>
              <th>姓名</th>
              <th>状态</th>
              <th>提交时间</th>
              <th>{isWriting ? "分数" : "正确 / 总题"}</th>
              <th>代币</th>
              {isWriting ? <th>操作</th> : null}
            </tr>
          </thead>
          <tbody>
            {(submissions ?? []).map((s) => {
              const status = s.status as string;
              const scoreCell = isWriting
                ? status === "returned" && s.score != null
                  ? `${s.score}/${s.max_score ?? "—"}`
                  : "—"
                : (s.total_count as number) > 0
                  ? `${s.correct_count}/${s.total_count}`
                  : "—";
              return (
                <tr key={s.id as string}>
                  <td>
                    {nameById.get(s.student_id as string) ??
                      (s.student_id as string)}
                  </td>
                  <td>
                    {submissionStatusLabel(status, assignment.module_id as string)}
                  </td>
                  <td>
                    {s.submitted_at
                      ? formatDueAt(s.submitted_at as string)
                      : "—"}
                  </td>
                  <td>{scoreCell}</td>
                  <td>{(s.earned_tokens as number) ?? 0}</td>
                  {isWriting ? (
                    <td>
                      {status === "submitted" ? (
                        <Link
                          href={`/teacher/assignments/${id}/submissions/${s.id as string}`}
                        >
                          批改
                        </Link>
                      ) : status === "returned" ? (
                        <Link
                          href={`/teacher/assignments/${id}/submissions/${s.id as string}`}
                        >
                          查看
                        </Link>
                      ) : (
                        "—"
                      )}
                    </td>
                  ) : null}
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </ConsoleShell>
  );
}
