"""Apply 023_practice_assignments_grants.sql and verify grants."""
from __future__ import annotations

import json
import os
import sys
import urllib.error
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ENV_FILE = ROOT / ".env.local"
MIGRATION = ROOT / "supabase" / "migrations" / "023_practice_assignments_grants.sql"
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
        with urllib.request.urlopen(req, timeout=120) as resp:
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
        select table_name, privilege_type
        from information_schema.role_table_grants
        where table_schema = 'public'
          and grantee = 'authenticated'
          and privilege_type = 'SELECT'
          and table_name in (
            'practice_assignments',
            'practice_assignment_items',
            'practice_assignment_recipients',
            'practice_assignment_submissions'
          )
        order by table_name;
        """,
    )
    print("Verify grants:", verify)
    rows = run_sql(
        token,
        ref,
        """
        select count(*)::int as assignment_count
        from public.practice_assignments;
        """,
    )
    print("Assignment rows:", rows)
    print("OK")


if __name__ == "__main__":
    main()
