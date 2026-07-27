import { requireAgencyAdmin } from "@/lib/auth";
import { ConsoleShell } from "@/components/ConsoleShell";
import { createClient } from "@/lib/supabase/server";
import { BindStudentForm } from "./BindStudentForm";
import { CreateStudentForm } from "./CreateStudentForm";

export default async function AgencyStudentsPage() {
  const profile = await requireAgencyAdmin();
  const supabase = await createClient();

  const [{ data: students }, { data: teachers }] = await Promise.all([
    supabase
      .from("students")
      .select("id, name, agency_id, teacher_id, student_no, class_id")
      .eq("agency_id", profile.agency_id!)
      .order("name"),
    supabase
      .from("teachers")
      .select("id, display_name")
      .eq("agency_id", profile.agency_id!)
      .order("display_name"),
  ]);

  const teacherName = new Map((teachers ?? []).map((t) => [t.id, t.display_name]));

  return (
    <ConsoleShell
      profile={profile}
      title="学生与绑定"
      tabs={[
        { href: "/agency", label: "概览" },
        { href: "/agency/teachers", label: "教师" },
        { href: "/agency/students", label: "学生", active: true },
        { href: "/agency/shop", label: "商城" },
        { href: "/agency/orders", label: "订单" },
      ]}
    >
      <div className="stack">
        <div className="card">
          <h2 style={{ marginTop: 0 }}>创建学生</h2>
          <p className="muted" style={{ marginTop: 0 }}>
            创建后学生可用该邮箱密码登录 App；须同时绑定本机构教师。
          </p>
          <CreateStudentForm teachers={teachers ?? []} />
        </div>
        <div className="card">
          <h2 style={{ marginTop: 0 }}>换绑教师</h2>
          <BindStudentForm
            students={students ?? []}
            teachers={teachers ?? []}
          />
        </div>
        <div className="card">
          <h2 style={{ marginTop: 0 }}>本机构学生</h2>
          <table>
            <thead>
              <tr>
                <th>姓名</th>
                <th>学号</th>
                <th>班级</th>
                <th>教师</th>
              </tr>
            </thead>
            <tbody>
              {(students ?? []).map((s) => (
                <tr key={s.id}>
                  <td>{s.name}</td>
                  <td>{s.student_no || "—"}</td>
                  <td>{s.class_id || "—"}</td>
                  <td>
                    {s.teacher_id
                      ? teacherName.get(s.teacher_id) || s.teacher_id
                      : "未绑定"}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {(students ?? []).length === 0 && (
            <p className="muted">暂无学生，请先创建。</p>
          )}
        </div>
      </div>
    </ConsoleShell>
  );
}
