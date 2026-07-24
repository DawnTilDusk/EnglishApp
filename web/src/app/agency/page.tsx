import { requireAgencyAdmin } from "@/lib/auth";
import { ConsoleShell } from "@/components/ConsoleShell";
import { createClient } from "@/lib/supabase/server";
import Link from "next/link";

export default async function AgencyHomePage() {
  const profile = await requireAgencyAdmin();
  const supabase = await createClient();

  const [{ count: teacherCount }, { count: studentCount }, { count: productCount }, { count: orderCount }] =
    await Promise.all([
      supabase
        .from("teachers")
        .select("*", { count: "exact", head: true })
        .eq("agency_id", profile.agency_id!),
      supabase
        .from("students")
        .select("*", { count: "exact", head: true })
        .eq("agency_id", profile.agency_id!),
      supabase
        .from("shop_products")
        .select("*", { count: "exact", head: true })
        .eq("agency_id", profile.agency_id!),
      supabase
        .from("shop_orders")
        .select("*", { count: "exact", head: true })
        .eq("agency_id", profile.agency_id!),
    ]);

  return (
    <ConsoleShell
      profile={profile}
      title="机构控制台"
      tabs={[
        { href: "/agency", label: "概览", active: true },
        { href: "/agency/teachers", label: "教师" },
        { href: "/agency/students", label: "学生" },
        { href: "/agency/shop", label: "商城" },
        { href: "/agency/orders", label: "订单" },
      ]}
    >
      <div className="stack">
        <div className="row">
          <div className="card" style={{ flex: 1, minWidth: 180 }}>
            <div className="muted">教师</div>
            <div style={{ fontSize: 28, fontWeight: 700 }}>{teacherCount ?? 0}</div>
          </div>
          <div className="card" style={{ flex: 1, minWidth: 180 }}>
            <div className="muted">学生</div>
            <div style={{ fontSize: 28, fontWeight: 700 }}>{studentCount ?? 0}</div>
          </div>
          <div className="card" style={{ flex: 1, minWidth: 180 }}>
            <div className="muted">商品</div>
            <div style={{ fontSize: 28, fontWeight: 700 }}>{productCount ?? 0}</div>
          </div>
          <div className="card" style={{ flex: 1, minWidth: 180 }}>
            <div className="muted">订单</div>
            <div style={{ fontSize: 28, fontWeight: 700 }}>{orderCount ?? 0}</div>
          </div>
        </div>
        <div className="card stack">
          <strong>快捷入口</strong>
          <div className="row">
            <Link className="btn secondary" href="/agency/teachers">
              管理教师
            </Link>
            <Link className="btn secondary" href="/agency/shop">
              管理机构店
            </Link>
            <Link className="btn secondary" href="/agency/orders">
              查看订单
            </Link>
          </div>
        </div>
      </div>
    </ConsoleShell>
  );
}
