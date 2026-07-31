"""Apply 022_practice_assignments.sql and verify."""
from __future__ import annotations

import json
import os
import sys
import urllib.error
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ENV_FILE = ROOT / ".env.local"
MIGRATION = ROOT / "supabase" / "migrations" / "022_practice_assignments.sql"
DEFAULT_REF = "xojbnrxnkgkqacrkcmqc"


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


def project_ref(env: dict[str, str]) -> str:
    ref = DEFAULT_REF
    url = env.get("SUPABASE_URL", "")
    if "://" in url:
        host = url.split("://", 1)[1].split("/", 1)[0]
        if host.endswith(".supabase.co"):
            ref = host.split(".")[0]
    return ref


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
    ref = project_ref(env)
    print(f"Project ref: {ref}")
    print("Smoke:", run_sql(token, ref, "select 1 as ok;"))
    sql = MIGRATION.read_text(encoding="utf-8")
    print(f"Applying {MIGRATION.name} ({len(sql)} chars)...")
    run_sql(token, ref, sql)
    verify = run_sql(
        token,
        ref,
        """
        select
          (select count(*)::int from information_schema.tables
             where table_schema='public' and table_name='practice_assignments') as assignments_table,
          (select count(*)::int from information_schema.routines
             where routine_schema='public' and routine_name='create_practice_assignment') as create_rpc,
          (select count(*)::int from information_schema.routines
             where routine_schema='public' and routine_name='submit_practice_assignment') as submit_rpc;
        """,
    )
    print("Verify:", verify)
    print("OK")


if __name__ == "__main__":
    main()
