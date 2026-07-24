import { requireAgencyAdmin } from "@/lib/auth";
import { ConsoleShell } from "@/components/ConsoleShell";
import { createClient } from "@/lib/supabase/server";
import { upsertProductAction } from "../actions";
import { ProductForm } from "./ProductForm";

export default async function AgencyShopPage() {
  const profile = await requireAgencyAdmin();
  const supabase = await createClient();
  const { data: products } = await supabase
    .from("shop_products")
    .select("*")
    .eq("agency_id", profile.agency_id!)
    .order("created_at", { ascending: false });

  return (
    <ConsoleShell
      profile={profile}
      title="机构商城"
      tabs={[
        { href: "/agency", label: "概览" },
        { href: "/agency/teachers", label: "教师" },
        { href: "/agency/students", label: "学生" },
        { href: "/agency/shop", label: "商城", active: true },
        { href: "/agency/orders", label: "订单" },
      ]}
    >
      <div className="stack">
        <div className="card">
          <h2 style={{ marginTop: 0 }}>上架商品</h2>
          <ProductForm action={upsertProductAction} />
        </div>
        <div className="card">
          <h2 style={{ marginTop: 0 }}>商品列表</h2>
          <table>
            <thead>
              <tr>
                <th>名称</th>
                <th>价格</th>
                <th>库存</th>
                <th>状态</th>
                <th>编辑</th>
              </tr>
            </thead>
            <tbody>
              {(products ?? []).map((p) => (
                <tr key={p.id}>
                  <td>
                    <div>{p.name}</div>
                    {p.description && <div className="muted">{p.description}</div>}
                  </td>
                  <td>{p.price_tokens}</td>
                  <td>{p.stock === -1 ? "不限" : p.stock}</td>
                  <td>{p.is_active ? "上架" : "下架"}</td>
                  <td>
                    <ProductForm
                      action={upsertProductAction}
                      product={{
                        id: p.id,
                        name: p.name,
                        description: p.description,
                        price_tokens: p.price_tokens,
                        stock: p.stock,
                        is_active: p.is_active,
                      }}
                      compact
                    />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </ConsoleShell>
  );
}
