"""Apply migrations 010–013 via Supabase Management API (needs SUPABASE_ACCESS_TOKEN)."""
from __future__ import annotations

import json
import os
import sys
import urllib.error
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MIGRATIONS = ROOT / "supabase" / "migrations"
ENV_FILE = ROOT / ".env.local"
PROJECT_REF = "xojbnrxnkgkqacrkcmqc"
FILES = [
    "010_agency_teacher_hierarchy.sql",
    "011_agency_shop.sql",
    "012_account_rpc_and_triggers.sql",
    "013_rls_progress_hierarchy.sql",
]


def load_env() -> dict[str, str]:
    values: dict[str, str] = {}
    if ENV_FILE.exists():
        for line in ENV_FILE.read_text(encoding="utf-8").splitlines():
            line = line.strip()
            if not line or line.startswith("#") or "=" not in line:
                continue
            key, _, value = line.partition("=")
            values[key.strip()] = value.strip().strip('"').strip("'")
    for key in ("SUPABASE_ACCESS_TOKEN", "SUPABASE_URL"):
        values.setdefault(key, os.environ.get(key, ""))
    return values


def run_sql(token: str, ref: str, sql: str) -> object:
    body = json.dumps({"query": sql}).encode("utf-8")
    req = urllib.request.Request(
        f"https://api.supabase.com/v1/projects/{ref}/database/query",
        data=body,
        method="POST",
        headers={
            "Authorization": f"Bearer {token}",
            "Content-Type": "application/json",
            "Accept": "application/json",
            "User-Agent": "Mozilla/5.0 SeedieMigration/1.0",
        },
    )
    try:
        with urllib.request.urlopen(req, timeout=300) as resp:
            raw = resp.read().decode("utf-8")
            return json.loads(raw) if raw else None
    except urllib.error.HTTPError as e:
        err = e.read().decode("utf-8", errors="replace")
        raise RuntimeError(f"HTTP {e.code}: {err}") from e


def main() -> None:
    env = load_env()
    token = env.get("SUPABASE_ACCESS_TOKEN", "")
    if not token:
        print("Missing SUPABASE_ACCESS_TOKEN in .env.local")
        sys.exit(2)

    ref = PROJECT_REF
    url = env.get("SUPABASE_URL", "")
    if "://" in url:
        host = url.split("://", 1)[1].split("/", 1)[0]
        if host.endswith(".supabase.co"):
            ref = host.split(".")[0]

    print(f"Project ref: {ref}")
    print("Smoke test: SELECT 1")
    print(run_sql(token, ref, "select 1 as ok;"))

    for name in FILES:
        path = MIGRATIONS / name
        sql = path.read_text(encoding="utf-8")
        print(f"\nApplying {name} ({len(sql)} chars)...")
        try:
            result = run_sql(token, ref, sql)
            print(f"  OK {name}")
            if result is not None:
                preview = json.dumps(result, ensure_ascii=False)[:300]
                print(f"  result preview: {preview}")
        except Exception as ex:
            print(f"  FAIL {name}: {ex}")
            sys.exit(1)

    print("\nAll migrations applied.")


if __name__ == "__main__":
    main()
