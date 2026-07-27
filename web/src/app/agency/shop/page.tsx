import { requireAgencyAdmin } from "@/lib/auth";
import { ConsoleShell } from "@/components/ConsoleShell";
import { createClient } from "@/lib/supabase/server";
import Link from "next/link";
import { ProductForm } from "./ProductForm";
import { ToggleActiveButton } from "./ToggleActiveButton";

type StatusFilter = "all" | "active" | "inactive";

function parseStatus(raw: string | undefined): StatusFilter {
  if (raw === "active" || raw === "inactive") return raw;
  return "all";
}

function shopHref(status: StatusFilter, edit?: string | null) {
  const params = new URLSearchParams();
  if (status !== "all") params.set("status", status);
  if (edit) params.set("edit", edit);
  const q = params.toString();
  return q ? `/agency/shop?${q}` : "/agency/shop";
}

export default async function AgencyShopPage({
  searchParams,
}: {
  searchParams: Promise<{ status?: string; edit?: string }>;
}) {
  const { status: statusRaw, edit } = await searchParams;
  const status = parseStatus(statusRaw);
  const profile = await requireAgencyAdmin();
  const supabase = await createClient();

  let productsQuery = supabase
    .from("shop_products")
    .select("*")
    .eq("agency_id", profile.agency_id!)
    .order("created_at", { ascending: false });

  if (status === "active") {
    productsQuery = productsQuery.eq("is_active", true);
  } else if (status === "inactive") {
    productsQuery = productsQuery.eq("is_active", false);
  }

  const { data: products } = await productsQuery;
  const list = products ?? [];
  const editing = edit ? list.find((p) => p.id === edit) : undefined;
  // If filtered out, still try to load the product for edit form
  let editProduct = editing;
  if (edit && !editProduct) {
    const { data } = await supabase
      .from("shop_products")
      .select("*")
      .eq("id", edit)
      .eq("agency_id", profile.agency_id!)
      .maybeSingle();
    editProduct = data ?? undefined;
  }

  const filters: { id: StatusFilter; label: string }[] = [
    { id: "all", label: "全部" },
    { id: "active", label: "上架中" },
    { id: "inactive", label: "已下架" },
  ];

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
          <h2 style={{ marginTop: 0 }}>新增商品</h2>
          <p className="muted" style={{ marginTop: 0 }}>
            新建后默认上架，学生 App 可见；下架请在列表中一键操作。
          </p>
          <ProductForm statusFilter={status} />
        </div>

        {editProduct && (
          <div className="card">
            <h2 style={{ marginTop: 0 }}>编辑商品 · {editProduct.name}</h2>
            <ProductForm
              statusFilter={status}
              cancelHref={shopHref(status)}
              product={{
                id: editProduct.id,
                name: editProduct.name,
                description: editProduct.description,
                price_tokens: editProduct.price_tokens,
                stock: editProduct.stock,
              }}
            />
          </div>
        )}

        <div className="card stack">
          <div className="row" style={{ justifyContent: "space-between" }}>
            <h2 style={{ margin: 0 }}>商品列表</h2>
            <div className="tabs">
              {filters.map((f) => (
                <Link
                  key={f.id}
                  href={shopHref(f.id, edit)}
                  className={status === f.id ? "active" : undefined}
                >
                  {f.label}
                </Link>
              ))}
            </div>
          </div>
          <table>
            <thead>
              <tr>
                <th>名称</th>
                <th>价格</th>
                <th>库存</th>
                <th>状态</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              {list.map((p) => (
                <tr
                  key={p.id}
                  style={
                    edit === p.id
                      ? { background: "rgba(15, 118, 110, 0.08)" }
                      : undefined
                  }
                >
                  <td>
                    <div>{p.name}</div>
                    {p.description && (
                      <div className="muted">{p.description}</div>
                    )}
                  </td>
                  <td>{p.price_tokens}</td>
                  <td>{p.stock === -1 ? "不限" : p.stock}</td>
                  <td>{p.is_active ? "上架" : "下架"}</td>
                  <td>
                    <div className="row" style={{ gap: 8 }}>
                      <ToggleActiveButton
                        productId={p.id}
                        isActive={p.is_active}
                      />
                      <Link
                        className="btn secondary"
                        href={shopHref(status, p.id)}
                      >
                        编辑
                      </Link>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {list.length === 0 && (
            <p className="muted">
              {status === "all"
                ? "暂无商品，请先新增。"
                : status === "active"
                  ? "没有上架中的商品。"
                  : "没有已下架的商品。"}
            </p>
          )}
        </div>
      </div>
    </ConsoleShell>
  );
}
