import { requireTeacher } from "@/lib/auth";
import { ConsoleShell } from "@/components/ConsoleShell";
import { createClient } from "@/lib/supabase/server";
import { notFound } from "next/navigation";

export default async function TeacherStudentDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;
  const profile = await requireTeacher();
  const supabase = await createClient();

  const { data: student } = await supabase
    .from("students")
    .select("id, name, student_no, class_id, teacher_id")
    .eq("id", id)
    .eq("teacher_id", profile.id)
    .maybeSingle();

  if (!student) {
    notFound();
  }

  const { data: stats, error } = await supabase.rpc("get_teacher_student_stats", {
    p_student_id: id,
  });

  return (
    <ConsoleShell
      profile={profile}
      title={`学生 · ${student.name}`}
      tabs={[
        { href: "/teacher", label: "学生", active: true },
        { href: "/teacher/shop", label: "商城（只读）" },
        { href: "/teacher/orders", label: "订单（只读）" },
      ]}
    >
      <div className="card stack">
        {error && <p className="error">{error.message}</p>}
        <div>
          <div className="muted">学号</div>
          <div>{student.student_no || "—"}</div>
        </div>
        <div>
          <div className="muted">班级</div>
          <div>{student.class_id || "—"}</div>
        </div>
        {stats && (
          <div className="row">
            <div className="card" style={{ flex: 1 }}>
              <div className="muted">签到天数</div>
              <strong>{stats.total_check_ins ?? 0}</strong>
            </div>
            <div className="card" style={{ flex: 1 }}>
              <div className="muted">学习分钟</div>
              <strong>{stats.total_study_minutes ?? 0}</strong>
            </div>
            <div className="card" style={{ flex: 1 }}>
              <div className="muted">已学词数</div>
              <strong>{stats.learned_word_count ?? 0}</strong>
            </div>
            <div className="card" style={{ flex: 1 }}>
              <div className="muted">代币余额</div>
              <strong>{stats.token_balance ?? 0}</strong>
            </div>
          </div>
        )}
      </div>
    </ConsoleShell>
  );
}
