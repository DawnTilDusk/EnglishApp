"""Read-only hierarchy / shop audit against Supabase REST (service role)."""
from __future__ import annotations

import json
import urllib.error
import urllib.parse
import urllib.request

from load_supabase_env import load_supabase_env


def get_json(url: str, key: str, path: str, params: dict[str, str] | None = None) -> list | dict:
    query = f"?{urllib.parse.urlencode(params)}" if params else ""
    req = urllib.request.Request(
        f"{url.rstrip('/')}/rest/v1/{path}{query}",
        headers={
            "apikey": key,
            "Authorization": f"Bearer {key}",
            "Accept": "application/json",
        },
    )
    try:
        with urllib.request.urlopen(req, timeout=60) as resp:
            return json.loads(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        body = e.read().decode("utf-8", errors="replace")
        raise SystemExit(f"HTTP {e.code} on {path}: {body}") from e


def main() -> None:
    env = load_supabase_env()
    url, key = env["SUPABASE_URL"], env["SUPABASE_SERVICE_ROLE_KEY"]

    print("=== Hierarchy / shop audit (read-only) ===\n")

    teachers = get_json(url, key, "teachers", {"select": "*"})
    if isinstance(teachers, list):
        print(f"teachers total: {len(teachers)}")
        if teachers and "agency_id" not in teachers[0]:
            print("  WARN: teachers.agency_id column missing (run 010 migration)")
        else:
            missing_agency = [t for t in teachers if not t.get("agency_id")]
            print(f"teachers missing agency_id: {len(missing_agency)}")
            for t in missing_agency[:20]:
                print(f"  - {t.get('id')} {t.get('display_name')}")
            if len(missing_agency) > 20:
                print(f"  ... and {len(missing_agency) - 20} more")
    print()

    profiles = get_json(url, key, "profiles", {"select": "id,role,agency_id,email"})
    teachers_ids = {t["id"] for t in teachers} if isinstance(teachers, list) else set()
    students = get_json(url, key, "students", {"select": "id,agency_id,teacher_id,name"})
    student_ids = {s["id"] for s in students} if isinstance(students, list) else set()

    if isinstance(profiles, list):
        role_mismatches = []
        for p in profiles:
            pid, role = p["id"], p.get("role")
            if role == "teacher" and pid not in teachers_ids:
                role_mismatches.append((pid, role, "missing teachers row"))
            if role == "student" and pid not in student_ids:
                role_mismatches.append((pid, role, "missing students row"))
            if pid in teachers_ids and role != "teacher":
                role_mismatches.append((pid, role, "has teachers row but role != teacher"))
        print(f"role / row mismatches: {len(role_mismatches)}")
        for item in role_mismatches[:30]:
            print(f"  - {item}")
        print()

    # Legacy teacher-scoped columns may still exist before migration.
    try:
        products = get_json(url, key, "shop_products", {"select": "*"})
    except SystemExit as e:
        print(f"shop_products: {e}")
        products = []

    if isinstance(products, list) and products:
        sample = products[0]
        print(f"shop_products total: {len(products)}")
        print(f"shop_products columns (sample keys): {sorted(sample.keys())}")
        if "teacher_id" in sample and "agency_id" not in sample:
            print("  WARN: still teacher-scoped (no agency_id column yet)")
        elif "agency_id" in sample:
            missing = [p for p in products if not p.get("agency_id")]
            print(f"  products missing agency_id: {len(missing)}")
        print()

    try:
        orders = get_json(url, key, "shop_orders", {"select": "*"})
    except SystemExit as e:
        print(f"shop_orders: {e}")
        orders = []

    if isinstance(orders, list) and orders:
        sample = orders[0]
        print(f"shop_orders total: {len(orders)}")
        print(f"shop_orders columns (sample keys): {sorted(sample.keys())}")
        pending = [o for o in orders if o.get("status") == "pending"]
        print(f"  pending orders: {len(pending)}")
        if "teacher_id" in sample and "agency_id" not in sample:
            print("  WARN: still teacher-scoped (no agency_id column yet)")
        print()

    print("Done.")


if __name__ == "__main__":
    main()
