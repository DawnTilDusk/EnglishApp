import { requireTeacher } from "@/lib/auth";
import { ConsoleShell } from "@/components/ConsoleShell";
import { createClient } from "@/lib/supabase/server";
import Link from "next/link";
import {
  formatDueAt,
  moduleLabel,
  teacherTabs,
} from "@/lib/teacher-assignments";

type AssignmentRow = {
  id: string;
  module_id: string;
  title: string;
  due_at: string;
  created_at: string;
};

export default async function TeacherAssignmentsPage({
  searchParams,
}: {
  searchParams: Promise<{ created?: string }>;
}) {
  const { created } = await searchParams;
  const profile = await requireTeacher();
  const supabase = await createClient();

  const { data: assignments, error: listError } = await supabase
    .from("practice_assignments")
    .select("id, module_id, title, due_at, created_at")
    .eq("teacher_id", profile.id)
    .order("created_at", { ascending: false });

  const list = (assignments ?? []) as AssignmentRow[];
  const ids = list.map((a) => a.id);

  const recipientCounts = new Map<string, number>();
  const submittedCounts = new Map<string, number>();

  if (!listError && ids.length > 0) {
    const { data: recipients } = await supabase
      .from("practice_assignment_recipients")
      .select("assignment_id")
      .in("assignment_id", ids);
    for (const row of recipients ?? []) {
      const id = row.assignment_id as string;
      recipientCounts.set(id, (recipientCounts.get(id) ?? 0) + 1);
    }

    const { data: submitted } = await supabase
      .from("practice_assignment_submissions")
      .select("assignment_id")
      .in("assignment_id", ids)
      .eq("status", "submitted");
    for (const row of submitted ?? []) {
      const id = row.assignment_id as string;
      submittedCounts.set(id, (submittedCounts.get(id) ?? 0) + 1);
    }
  }

  return (
    <ConsoleShell
      profile={profile}
      title="教师控制台"
      tabs={teacherTabs("assignments")}
    >
      <div className="card stack">
        <div className="row" style={{ justifyContent: "space-between" }}>
          <h2 style={{ margin: 0 }}>作业</h2>
          <Link className="btn" href="/teacher/assignments/new">
            布置作业
          </Link>
        </div>
        {listError && (
          <p className="error">加载作业失败：{listError.message}</p>
        )}
        {created === "1" && !listError && (
          <p style={{ margin: 0, color: "var(--accent)" }}>
            作业已布置，可在下方打开详情。
          </p>
        )}
        <table>
          <thead>
            <tr>
              <th>标题</th>
              <th>模块</th>
              <th>截止</th>
              <th>已交 / 应交</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {list.map((a) => {
              const total = recipientCounts.get(a.id) ?? 0;
              const done = submittedCounts.get(a.id) ?? 0;
              return (
                <tr key={a.id}>
                  <td>{a.title}</td>
                  <td>{moduleLabel(a.module_id)}</td>
                  <td>{formatDueAt(a.due_at)}</td>
                  <td>
                    {done} / {total}
                  </td>
                  <td>
                    <Link href={`/teacher/assignments/${a.id}`}>详情</Link>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
        {!listError && list.length === 0 && (
          <p className="muted">还没有布置作业，点击右上角开始。</p>
        )}
      </div>
    </ConsoleShell>
  );
}
