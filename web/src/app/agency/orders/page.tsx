import { requireAgencyAdmin } from "@/lib/auth";
import { ConsoleShell } from "@/components/ConsoleShell";
import { createClient } from "@/lib/supabase/server";

export default async function AgencyOrdersPage() {
  const profile = await requireAgencyAdmin();
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
      title="订单"
      tabs={[
        { href: "/agency", label: "概览" },
        { href: "/agency/teachers", label: "教师" },
        { href: "/agency/students", label: "学生" },
        { href: "/agency/shop", label: "商城" },
        { href: "/agency/orders", label: "订单", active: true },
      ]}
    >
      <div className="card">
        <p className="muted">下单即扣款完成，无审单流程。</p>
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
