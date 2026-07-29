"""Apply 020_reading_comprehension_catalog.sql and verify row counts."""
from __future__ import annotations

import json
import os
import sys
import urllib.error
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ENV_FILE = ROOT / ".env.local"
MIGRATION = ROOT / "supabase" / "migrations" / "020_reading_comprehension_catalog.sql"
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
        print("Missing SUPABASE_ACCESS_TOKEN")
        sys.exit(2)
    ref = project_ref(env)
    print(f"Project ref: {ref}")
    print("Smoke:", run_sql(token, ref, "select 1 as ok;"))

    sql = MIGRATION.read_text(encoding="utf-8")
    print(f"Applying {MIGRATION.name} ({len(sql)} chars)...")
    try:
        result = run_sql(token, ref, sql)
        print("Apply OK", result)
    except Exception as ex:
        print(f"Full-file apply failed: {ex}")
        print("Retrying statement-by-statement...")
        # Split on semicolons at end of lines roughly — use simple split for DDL+seed
        parts: list[str] = []
        buf: list[str] = []
        in_dollar = False
        dollar_tag = ""
        for line in sql.splitlines():
            buf.append(line)
            # track dollar quotes
            i = 0
            while i < len(line):
                if not in_dollar and line[i] == "$":
                    j = line.find("$", i + 1)
                    if j != -1:
                        tag = line[i : j + 1]
                        # opening
                        in_dollar = True
                        dollar_tag = tag
                        i = j + 1
                        continue
                elif in_dollar and dollar_tag and dollar_tag in line[i:]:
                    idx = line.find(dollar_tag, i)
                    if idx == i or (idx >= 0 and line[i:idx].find("$") < 0):
                        # simplified: if tag appears, toggle
                        pass
                    if dollar_tag in line[i:]:
                        pos = line.index(dollar_tag, i)
                        in_dollar = False
                        i = pos + len(dollar_tag)
                        continue
                i += 1
            joined = "\n".join(buf)
            if not in_dollar and joined.rstrip().endswith(";"):
                parts.append(joined.strip())
                buf = []
        if buf:
            parts.append("\n".join(buf).strip())
        for idx, part in enumerate(parts, 1):
            if not part or part.lstrip().startswith("--") and ";" not in part:
                # skip pure comment blocks without statements
                if not any(
                    kw in part.upper()
                    for kw in ("CREATE", "ALTER", "DROP", "GRANT", "INSERT", "DELETE", "SELECT")
                ):
                    continue
            print(f"  stmt {idx}/{len(parts)} ({len(part)} chars)...")
            run_sql(token, ref, part)

    verify = run_sql(
        token,
        ref,
        """
SELECT
  (SELECT COUNT(*) FROM reading_sets) AS sets,
  (SELECT COUNT(*) FROM reading_questions) AS questions,
  (SELECT COUNT(*) FROM reading_options) AS options;
""",
    )
    print("Verify:", verify)
    # Expect 5/25/100
    row = verify[0] if isinstance(verify, list) and verify else verify
    if isinstance(row, dict):
        sets = int(row.get("sets", 0))
        questions = int(row.get("questions", 0))
        options = int(row.get("options", 0))
        if (sets, questions, options) != (5, 25, 100):
            print(f"Unexpected counts: {sets}/{questions}/{options}")
            sys.exit(1)
        print("Counts OK: 5/25/100")


if __name__ == "__main__":
    main()
