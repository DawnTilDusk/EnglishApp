import { requireTeacher } from "@/lib/auth";
import { ConsoleShell } from "@/components/ConsoleShell";
import { createClient } from "@/lib/supabase/server";

export default async function TeacherShopPage() {
  const profile = await requireTeacher();
  const supabase = await createClient();

  const { data: products } = await supabase
    .from("shop_products")
    .select("*")
    .eq("agency_id", profile.agency_id!)
    .order("created_at", { ascending: false });

  return (
    <ConsoleShell
      profile={profile}
      title="机构商城（只读）"
      tabs={[
        { href: "/teacher", label: "学生" },
        { href: "/teacher/shop", label: "商城（只读）", active: true },
        { href: "/teacher/orders", label: "订单（只读）" },
      ]}
    >
      <div className="card">
        <p className="muted">商品由机构管理员维护，教师仅可查看。</p>
        <table>
          <thead>
            <tr>
              <th>名称</th>
              <th>价格</th>
              <th>库存</th>
              <th>状态</th>
            </tr>
          </thead>
          <tbody>
            {(products ?? []).map((p) => (
              <tr key={p.id}>
                <td>{p.name}</td>
                <td>{p.price_tokens}</td>
                <td>{p.stock === -1 ? "不限" : p.stock}</td>
                <td>{p.is_active ? "上架" : "下架"}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </ConsoleShell>
  );
}
