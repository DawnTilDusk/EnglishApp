import { requireTeacher } from "@/lib/auth";
import { ConsoleShell } from "@/components/ConsoleShell";
import { createClient } from "@/lib/supabase/server";

export default async function TeacherOrdersPage() {
  const profile = await requireTeacher();
  const supabase = await createClient();

  const { data: orders } = await supabase
    .from("shop_orders")
    .select("id, student_id, product_id, tokens_amount, status, created_at")
    .eq("agency_id", profile.agency_id!)
    .order("created_at", { ascending: false });

  const studentIds = [...new Set((orders ?? []).map((o) => o.student_id))];
  const productIds = [...new Set((orders ?? []).map((o) => o.product_id))];

  const [{ data: students }, { data: products }] = await Promise.all([
    studentIds.length
      ? supabase.from("students").select("id, name").in("id", studentIds)
      : Promise.resolve({ data: [] as { id: string; name: string }[] }),
    productIds.length
      ? supabase.from("shop_products").select("id, name").in("id", productIds)
      : Promise.resolve({ data: [] as { id: string; name: string }[] }),
  ]);

  const studentName = new Map((students ?? []).map((s) => [s.id, s.name]));
  const productName = new Map((products ?? []).map((p) => [p.id, p.name]));

  return (
    <ConsoleShell
      profile={profile}
      title="机构订单（只读）"
      tabs={[
        { href: "/teacher", label: "学生" },
        { href: "/teacher/shop", label: "商城（只读）" },
        { href: "/teacher/orders", label: "订单（只读）", active: true },
      ]}
    >
      <div className="card">
        <table>
          <thead>
            <tr>
              <th>时间</th>
              <th>学生</th>
              <th>商品</th>
              <th>代币</th>
              <th>状态</th>
            </tr>
          </thead>
          <tbody>
            {(orders ?? []).map((o) => (
              <tr key={o.id}>
                <td>{o.created_at ?? "—"}</td>
                <td>{studentName.get(o.student_id) || o.student_id}</td>
                <td>{productName.get(o.product_id) || o.product_id}</td>
                <td>{o.tokens_amount}</td>
                <td>{o.status}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </ConsoleShell>
  );
}
