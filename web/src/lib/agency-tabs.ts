export type AgencyTabId =
  | "home"
  | "teachers"
  | "students"
  | "videos"
  | "shop"
  | "orders";

export function agencyTabs(active: AgencyTabId) {
  return [
    { href: "/agency", label: "概览", active: active === "home" },
    { href: "/agency/teachers", label: "教师", active: active === "teachers" },
    { href: "/agency/students", label: "学生", active: active === "students" },
    { href: "/agency/videos", label: "视频课", active: active === "videos" },
    { href: "/agency/shop", label: "商城", active: active === "shop" },
    { href: "/agency/orders", label: "订单", active: active === "orders" },
  ];
}
