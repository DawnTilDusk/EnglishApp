"""Load env from repo root .env.local (or process env)."""
from __future__ import annotations

import os
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ENV_FILE = ROOT / ".env.local"

REQUIRED_KEYS = ("SUPABASE_URL", "SUPABASE_SERVICE_ROLE_KEY")


def load_supabase_env() -> dict[str, str]:
    values: dict[str, str] = {}
    if ENV_FILE.exists():
        for line in ENV_FILE.read_text(encoding="utf-8").splitlines():
            line = line.strip()
            if not line or line.startswith("#") or "=" not in line:
                continue
            key, _, value = line.partition("=")
            values[key.strip()] = value.strip().strip('"').strip("'")

    for key in REQUIRED_KEYS:
        values.setdefault(key, os.environ.get(key, ""))

    missing = [k for k in REQUIRED_KEYS if not values.get(k)]
    if missing:
        raise SystemExit(
            f"Missing {', '.join(missing)}. Set them in {ENV_FILE.name} or the environment."
        )

    values.setdefault("SUPABASE_ANON_KEY", os.environ.get("SUPABASE_ANON_KEY", values.get("SUPABASE_ANON_KEY", "")))
    return values
