export function teacherTabs(active: "students" | "assignments" | "shop" | "orders") {
  return [
    { href: "/teacher", label: "学生", active: active === "students" },
    { href: "/teacher/assignments", label: "作业", active: active === "assignments" },
    { href: "/teacher/shop", label: "商城（只读）", active: active === "shop" },
    { href: "/teacher/orders", label: "订单（只读）", active: active === "orders" },
  ];
}

const UUID_RE =
  /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;

export function isUuid(value: string): boolean {
  return UUID_RE.test(value);
}

/** Normalize create_practice_assignment jsonb return into an assignment UUID. */
export function parseAssignmentIdFromRpc(data: unknown): string | null {
  let payload: unknown = data;
  if (typeof payload === "string") {
    try {
      payload = JSON.parse(payload);
    } catch {
      return null;
    }
  }
  if (!payload || typeof payload !== "object" || Array.isArray(payload)) {
    return null;
  }
  const raw = (payload as { assignment_id?: unknown }).assignment_id;
  if (typeof raw !== "string") return null;
  const id = raw.trim();
  return isUuid(id) ? id : null;
}

export function moduleLabel(moduleId: string): string {
  switch (moduleId) {
    case "reading":
      return "阅读训练";
    case "listening":
      return "听力训练";
    default:
      return moduleId;
  }
}

export function formatDueAt(iso: string): string {
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return iso;
  return d.toLocaleString("zh-CN", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  });
}

export function submissionStatusLabel(status: string): string {
  switch (status) {
    case "pending":
      return "未开始";
    case "in_progress":
      return "进行中";
    case "submitted":
      return "已提交";
    default:
      return status;
  }
}
