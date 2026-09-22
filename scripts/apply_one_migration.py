"""Apply a single SQL migration via Management API. Usage: python scripts/apply_one_migration.py 042_agency_video_lessons.sql"""
from __future__ import annotations

import importlib.util
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
HELPER = ROOT / "scripts" / "apply_migrations.py"


def load_helper():
    spec = importlib.util.spec_from_file_location("apply_migrations", HELPER)
    if spec is None or spec.loader is None:
        raise SystemExit("Cannot load apply_migrations.py")
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod


def resolve_ref(mod, env: dict[str, str]) -> str:
    ref = mod.PROJECT_REF
    url = env.get("SUPABASE_URL", "")
    if "://" in url:
        host = url.split("://", 1)[1].split("/", 1)[0]
        if host.endswith(".supabase.co"):
            ref = host.split(".")[0]
    return ref


def main() -> None:
    if len(sys.argv) < 2:
        print("Usage: python scripts/apply_one_migration.py <filename.sql>")
        sys.exit(2)
    name = sys.argv[1]
    path = ROOT / "supabase" / "migrations" / name
    if not path.exists():
        print(f"Missing {path}")
        sys.exit(2)

    mod = load_helper()
    env = mod.load_env()
    token = env.get("SUPABASE_ACCESS_TOKEN", "")
    if not token:
        print("Missing SUPABASE_ACCESS_TOKEN in .env.local")
        sys.exit(2)

    ref = resolve_ref(mod, env)
    print(f"Project ref: {ref}")
    print("Smoke test: SELECT 1")
    print(mod.run_sql(token, ref, "select 1 as ok;"))

    sql = path.read_text(encoding="utf-8")
    print(f"Applying {name} ({len(sql)} chars)...")
    result = mod.run_sql(token, ref, sql)
    print(f"OK {name}")
    if result is not None:
        print("result preview:", json.dumps(result, ensure_ascii=False)[:400])


if __name__ == "__main__":
    main()
