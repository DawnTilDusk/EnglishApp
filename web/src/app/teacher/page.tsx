import { requireTeacher } from "@/lib/auth";
import { ConsoleShell } from "@/components/ConsoleShell";
import { createClient } from "@/lib/supabase/server";
import Link from "next/link";

export default async function TeacherHomePage() {
  const profile = await requireTeacher();
  const supabase = await createClient();

  const { data: students } = await supabase
    .from("students")
    .select("id, name, student_no, class_id")
    .eq("teacher_id", profile.id)
    .order("name");

  return (
    <ConsoleShell
      profile={profile}
      title="教师控制台"
      tabs={[
        { href: "/teacher", label: "学生", active: true },
        { href: "/teacher/shop", label: "商城（只读）" },
        { href: "/teacher/orders", label: "订单（只读）" },
      ]}
    >
      <div className="card">
        <h2 style={{ marginTop: 0 }}>我的学生</h2>
        <table>
          <thead>
            <tr>
              <th>姓名</th>
              <th>学号</th>
              <th>班级</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {(students ?? []).map((s) => (
              <tr key={s.id}>
                <td>{s.name}</td>
                <td>{s.student_no || "—"}</td>
                <td>{s.class_id || "—"}</td>
                <td>
                  <Link href={`/teacher/students/${s.id}`}>详情</Link>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {(students ?? []).length === 0 && (
          <p className="muted">暂无绑定学生，请联系机构管理员。</p>
        )}
      </div>
    </ConsoleShell>
  );
}
