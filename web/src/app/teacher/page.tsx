import { requireTeacher } from "@/lib/auth";
import { ConsoleShell } from "@/components/ConsoleShell";
import { createClient } from "@/lib/supabase/server";
import Link from "next/link";

export default async function TeacherHomePage({
  searchParams,
}: {
  searchParams: Promise<{ q?: string }>;
}) {
  const { q } = await searchParams;
  const query = (q ?? "").trim();
  const profile = await requireTeacher();
  const supabase = await createClient();

  let studentsQuery = supabase
    .from("students")
    .select("id, name, student_no, class_id")
    .eq("teacher_id", profile.id)
    .order("name");

  if (query) {
    studentsQuery = studentsQuery.ilike("name", `%${query}%`);
  }

  const { data: students } = await studentsQuery;
  const list = students ?? [];

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
      <div className="card stack">
        <div className="row" style={{ justifyContent: "space-between" }}>
          <h2 style={{ margin: 0 }}>我的学生</h2>
          <form method="get" action="/teacher" className="row">
            <input
              type="search"
              name="q"
              defaultValue={query}
              placeholder="按姓名筛选"
              aria-label="按姓名筛选"
            />
            <button className="btn secondary" type="submit">
              筛选
            </button>
            {query && (
              <Link className="btn secondary" href="/teacher">
                清除
              </Link>
            )}
          </form>
        </div>
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
            {list.map((s) => (
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
        {list.length === 0 && (
          <p className="muted">
            {query
              ? `没有姓名匹配「${query}」的学生。`
              : "暂无绑定学生，请联系机构管理员。"}
          </p>
        )}
      </div>
    </ConsoleShell>
  );
}
