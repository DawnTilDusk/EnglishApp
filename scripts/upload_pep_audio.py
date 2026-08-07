"""Upload PEP 7上 Unit 1-7 mp3 files to Supabase Storage `audio-pep` bucket
and update listening_materials.audio_url accordingly.

Usage (in project root):
    python scripts/upload_pep_audio.py
    python scripts/upload_pep_audio.py --dry-run

Env (read from web/.env.local or app/.env.local):
    NEXT_PUBLIC_SUPABASE_URL / SUPABASE_URL
    SUPABASE_SERVICE_ROLE_KEY  (preferred; needed for Storage write)
    or SUPABASE_ACCESS_TOKEN
"""
from __future__ import annotations

import argparse
import json
import os
import sys
import time
import urllib.error
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CORPUS_ROOT = ROOT / "EngResource"
BUCKET = "audio-pep"
UA = "Mozilla/5.0 SeediePepUpload/1.0"

# material_id → 相对于 EngResource 的源 mp3 路径
MAPPING: dict[str, str] = {
    # Unit 1
    "pep-g7-vol1-u1-seca-1b":  r"7上\新版\【1】7上音频（新教材）\【2】7上听力+单词（细分）\23 P20 7上U1SecA1b.mp3",
    "pep-g7-vol1-u1-seca-1c":  r"7上\新版\【1】7上音频（新教材）\【2】7上听力+单词（细分）\24 P20 7上U1SecA1c.mp3",
    "pep-g7-vol1-u1-pr-1":     r"7上\新版\【1】7上音频（新教材）\【2】7上听力+单词（细分）\25 P21 7上U1PR1.mp3",
    "pep-g7-vol1-u1-pr-2":     r"7上\新版\【1】7上音频（新教材）\【2】7上听力+单词（细分）\26 P21 7上U1PR2.mp3",
    "pep-g7-vol1-u1-seca-2a":  r"7上\新版\【1】7上音频（新教材）\【2】7上听力+单词（细分）\27 P21 7上U1SecA2a.mp3",
    "pep-g7-vol1-u1-seca-2d":  r"7上\新版\【1】7上音频（新教材）\【2】7上听力+单词（细分）\28 P22 7上U1SecA2d.mp3",
    "pep-g7-vol1-u1-secb-1b":  r"7上\新版\【1】7上音频（新教材）\【2】7上听力+单词（细分）\29 P24 7上U1SecB1b.mp3",
    # Unit 2
    "pep-g7-vol1-u2-seca-1b":  r"7上\新版\【1】7上音频（新教材）\【2】7上听力+单词（细分）\30 P28 7上U2SecA1b.mp3",
    "pep-g7-vol1-u2-seca-1c":  r"7上\新版\【1】7上音频（新教材）\【2】7上听力+单词（细分）\31 P28 7上U2SecA1c.mp3",
    "pep-g7-vol1-u2-pr-1":     r"7上\新版\【1】7上音频（新教材）\【2】7上听力+单词（细分）\32 P29 7上U2PR1.mp3",
    "pep-g7-vol1-u2-pr-2":     r"7上\新版\【1】7上音频（新教材）\【2】7上听力+单词（细分）\33 P29 7上U2PR2.mp3",
    "pep-g7-vol1-u2-pr-3":     r"7上\新版\【1】7上音频（新教材）\【2】7上听力+单词（细分）\34 P29 7上U2PR3.mp3",
    "pep-g7-vol1-u2-seca-2a":  r"7上\新版\【1】7上音频（新教材）\【2】7上听力+单词（细分）\35 P29 7上U2SecA2a.mp3",
    "pep-g7-vol1-u2-seca-2c":  r"7上\新版\【1】7上音频（新教材）\【2】7上听力+单词（细分）\36 P30 7上U2SecA2c.mp3",
    "pep-g7-vol1-u2-secb-1b":  r"7上\新版\【1】7上音频（新教材）\【2】7上听力+单词（细分）\37 P32 7上U2SecB1b.mp3",
    # Unit 3
    "pep-g7-vol1-u3-seca-1b":  r"7上\新版\【1】7上音频（新教材）\【2】7上听力+单词（细分）\38 P36 7上U3SecA1b.mp3",
    "pep-g7-vol1-u3-seca-1c":  r"7上\新版\【1】7上音频（新教材）\【2】7上听力+单词（细分）\39 P36 7上U3SecA1c.mp3",
    "pep-g7-vol1-u3-pr-1":     r"7上\新版\【1】7上音频（新教材）\【2】7上听力+单词（细分）\40 P37 7上U3PR1.mp3",
    "pep-g7-vol1-u3-pr-2":     r"7上\新版\【1】7上音频（新教材）\【2】7上听力+单词（细分）\41 P37 7上U3PR2.mp3",
    "pep-g7-vol1-u3-seca-2a":  r"7上\新版\【1】7上音频（新教材）\【2】7上听力+单词（细分）\42 P37 7上U3SecA2a.mp3",
    "pep-g7-vol1-u3-seca-2d":  r"7上\新版\【1】7上音频（新教材）\【2】7上听力+单词（细分）\43 P38 7上U3SecA2d.mp3",
    "pep-g7-vol1-u3-secb-1b":  r"7上\新版\【1】7上音频（新教材）\【2】7上听力+单词（细分）\44 P40 7上U3SecB1b.mp3",
    # Unit 4
    "pep-g7-vol1-u4-seca-1b":  r"7上\新版\【1】7上音频（新教材）\【2】7上听力+单词（细分）\45 P44 7上U4SecA1b.mp3",
    "pep-g7-vol1-u4-seca-1c":  r"7上\新版\【1】7上音频（新教材）\【2】7上听力+单词（细分）\46 P44 7上U4SecA1c.mp3",
    "pep-g7-vol1-u4-pr-1":     r"7上\新版\【1】7上音频（新教材）\【2】7上听力+单词（细分）\47 P45 7上U4PR1.mp3",
    "pep-g7-vol1-u4-pr-2":     r"7上\新版\【1】7上音频（新教材）\【2】7上听力+单词（细分）\48 P45 7上U4PR2.mp3",
    "pep-g7-vol1-u4-seca-2a":  r"7上\新版\【1】7上音频（新教材）\【2】7上听力+单词（细分）\49 P45 7上U4SecA2a.mp3",
    "pep-g7-vol1-u4-seca-2c":  r"7上\新版\【1】7上音频（新教材）\【2】7上听力+单词（细分）\50 P46 7上U4SecA2c.mp3",
    "pep-g7-vol1-u4-secb-1b":  r"7上\新版\【1】7上音频（新教材）\【2】7上听力+单词（细分）\51 P48 7上U4SecB1b.mp3",
    # Unit 5
    "pep-g7-vol1-u5-seca-1b":  r"7上\新版\【1】7上音频（新教材）\【2】7上听力+单词（细分）\52 P52 7上U5SecA1b.mp3",
    "pep-g7-vol1-u5-seca-1c":  r"7上\新版\【1】7上音频（新教材）\【2】7上听力+单词（细分）\53 P52 7上U5SecA1c.mp3",
    "pep-g7-vol1-u5-pr-1":     r"7上\新版\【1】7上音频（新教材）\【2】7上听力+单词（细分）\54 P53 7上U5PR1.mp3",
    "pep-g7-vol1-u5-pr-2":     r"7上\新版\【1】7上音频（新教材）\【2】7上听力+单词（细分）\55 P53 7上U5PR2.mp3",
    "pep-g7-vol1-u5-seca-2a":  r"7上\新版\【1】7上音频（新教材）\【2】7上听力+单词（细分）\56 P53 7上U5SecA2a.mp3",
    "pep-g7-vol1-u5-seca-2c":  r"7上\新版\【1】7上音频（新教材）\【2】7上听力+单词（细分）\57 P54 7上U5SecA2c.mp3",
    "pep-g7-vol1-u5-secb-1b":  r"7上\新版\【1】7上音频（新教材）\【2】7上听力+单词（细分）\58 P56 7上U5SecB1b.mp3",
    # Unit 6
    "pep-g7-vol1-u6-seca-1b":  r"7上\新版\【1】7上音频（新教材）\【2】7上听力+单词（细分）\59 P60 7上U6SecA1b.mp3",
    "pep-g7-vol1-u6-seca-1c":  r"7上\新版\【1】7上音频（新教材）\【2】7上听力+单词（细分）\60 P60 7上U6SecA1c.mp3",
    "pep-g7-vol1-u6-seca-1d":  r"7上\新版\【1】7上音频（新教材）\【2】7上听力+单词（细分）\61 P60 7上U6SecA1d.mp3",
    "pep-g7-vol1-u6-pr-1":     r"7上\新版\【1】7上音频（新教材）\【2】7上听力+单词（细分）\62 P61 7上U6PR1.mp3",
    "pep-g7-vol1-u6-pr-2":     r"7上\新版\【1】7上音频（新教材）\【2】7上听力+单词（细分）\63 P61 7上U6PR2.mp3",
    "pep-g7-vol1-u6-seca-2a":  r"7上\新版\【1】7上音频（新教材）\【2】7上听力+单词（细分）\64 P61 7上U6SecA2a.mp3",
    "pep-g7-vol1-u6-seca-2d":  r"7上\新版\【1】7上音频（新教材）\【2】7上听力+单词（细分）\65 P62 7上U6SecA2d.mp3",
    "pep-g7-vol1-u6-secb-1b":  r"7上\新版\【1】7上音频（新教材）\【2】7上听力+单词（细分）\66 P64 7上U6SecB1b.mp3",
    # Unit 7
    "pep-g7-vol1-u7-seca-1a":  r"7上\新版\【1】7上音频（新教材）\【2】7上听力+单词（细分）\67 P68 7上U7SecA1a.mp3",
    "pep-g7-vol1-u7-seca-1b":  r"7上\新版\【1】7上音频（新教材）\【2】7上听力+单词（细分）\68 P68 7上U7SecA1b.mp3",
    "pep-g7-vol1-u7-seca-1c":  r"7上\新版\【1】7上音频（新教材）\【2】7上听力+单词（细分）\69 P68 7上U7SecA1c.mp3",
    "pep-g7-vol1-u7-pr-1":     r"7上\新版\【1】7上音频（新教材）\【2】7上听力+单词（细分）\70 P69 7上U7PR1.mp3",
    "pep-g7-vol1-u7-pr-2":     r"7上\新版\【1】7上音频（新教材）\【2】7上听力+单词（细分）\71 P69 7上U7PR2.mp3",
    "pep-g7-vol1-u7-seca-2a":  r"7上\新版\【1】7上音频（新教材）\【2】7上听力+单词（细分）\72 P69 7上U7SecA2a.mp3",
    "pep-g7-vol1-u7-seca-2c":  r"7上\新版\【1】7上音频（新教材）\【2】7上听力+单词（细分）\73 P70 7上U7SecA2c.mp3",
    "pep-g7-vol1-u7-secb-1b":  r"7上\新版\【1】7上音频（新教材）\【2】7上听力+单词（细分）\74 P72 7上U7SecB1b.mp3",
}


def load_env() -> dict[str, str]:
    values: dict[str, str] = {}
    for env_file in (ROOT / "web" / ".env.local", ROOT / ".env.local", ROOT / "app" / ".env.local"):
        if not env_file.exists():
            continue
        for line in env_file.read_text(encoding="utf-8").splitlines():
            line = line.strip()
            if not line or line.startswith("#") or "=" not in line:
                continue
            k, _, v = line.partition("=")
            values.setdefault(k.strip(), v.strip().strip('"').strip("'"))
    for k in ("SUPABASE_URL", "NEXT_PUBLIC_SUPABASE_URL",
              "SUPABASE_SERVICE_ROLE_KEY", "SUPABASE_ACCESS_TOKEN",
              "NEXT_PUBLIC_SUPABASE_ANON_KEY"):
        values.setdefault(k, os.environ.get(k, ""))
    if not values.get("SUPABASE_URL"):
        values["SUPABASE_URL"] = values.get("NEXT_PUBLIC_SUPABASE_URL", "")
    return values


def project_ref(env: dict[str, str]) -> str:
    url = env.get("SUPABASE_URL", "")
    if "://" in url:
        host = url.split("://", 1)[1].split("/", 1)[0]
        if host.endswith(".supabase.co"):
            return host.split(".")[0]
    raise RuntimeError("SUPABASE_URL missing / malformed in .env.local")


def ensure_bucket(env: dict[str, str], dry_run: bool) -> None:
    if dry_run:
        print(f"[dry-run] ensure bucket {BUCKET}")
        return
    url = env["SUPABASE_URL"].rstrip("/") + "/storage/v1/bucket"
    key = env.get("SUPABASE_SERVICE_ROLE_KEY") or env.get("SUPABASE_ACCESS_TOKEN")
    if not key:
        raise RuntimeError("Need SUPABASE_SERVICE_ROLE_KEY or SUPABASE_ACCESS_TOKEN in .env.local")
    body = json.dumps({"id": BUCKET, "name": BUCKET, "public": True}).encode("utf-8")
    req = urllib.request.Request(url, data=body, method="POST", headers={
        "Authorization": f"Bearer {key}",
        "apikey": key,
        "Content-Type": "application/json",
        "User-Agent": UA,
    })
    try:
        with urllib.request.urlopen(req, timeout=60) as resp:
            resp.read()
            print(f"Bucket {BUCKET} created.")
    except urllib.error.HTTPError as e:
        if e.code in (400, 409):
            print(f"Bucket {BUCKET} exists.")
        else:
            raise RuntimeError(f"ensure_bucket HTTP {e.code}: {e.read().decode('utf-8', 'replace')}") from e


def upload_object(env: dict[str, str], key: str, file_path: Path, dry_run: bool) -> str:
    public_url = f"{env['SUPABASE_URL'].rstrip('/')}/storage/v1/object/public/{BUCKET}/{key}"
    if dry_run:
        print(f"[dry-run] upload {file_path.name}  ->  {key}")
        return public_url
    auth = env.get("SUPABASE_SERVICE_ROLE_KEY") or env.get("SUPABASE_ACCESS_TOKEN")
    data = file_path.read_bytes()
    last_err: Exception | None = None
    for attempt in range(3):
        req = urllib.request.Request(
            f"{env['SUPABASE_URL'].rstrip('/')}/storage/v1/object/{BUCKET}/{key}",
            data=data, method="POST", headers={
                "Authorization": f"Bearer {auth}",
                "apikey": auth,
                "Content-Type": "audio/mpeg",
                "x-upsert": "true",
                "User-Agent": UA,
            },
        )
        try:
            with urllib.request.urlopen(req, timeout=600) as resp:
                resp.read()
            return public_url
        except urllib.error.HTTPError as e:
            body = e.read().decode("utf-8", "replace")
            last_err = RuntimeError(f"upload {key} HTTP {e.code}: {body}")
            if e.code in (400, 500, 502, 503, 504) and attempt < 2:
                time.sleep(1 + attempt)
                continue
            break
        except (urllib.error.URLError, TimeoutError) as e:
            last_err = e
            if attempt < 2:
                time.sleep(1 + attempt)
                continue
            break
    raise last_err  # type: ignore[misc]


def run_sql(env: dict[str, str], ref: str, sql: str) -> None:
    token = env.get("SUPABASE_ACCESS_TOKEN")
    if not token:
        raise RuntimeError("SUPABASE_ACCESS_TOKEN missing (needed for management API SQL).")
    body = json.dumps({"query": sql}).encode("utf-8")
    req = urllib.request.Request(
        f"https://api.supabase.com/v1/projects/{ref}/database/query",
        data=body, method="POST", headers={
            "Authorization": f"Bearer {token}",
            "Content-Type": "application/json",
            "Accept": "application/json",
            "User-Agent": UA,
        },
    )
    try:
        with urllib.request.urlopen(req, timeout=120) as resp:
            resp.read()
    except urllib.error.HTTPError as e:
        raise RuntimeError(f"SQL HTTP {e.code}: {e.read().decode('utf-8', 'replace')}") from e


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--dry-run", action="store_true")
    ap.add_argument("--material-id", type=str, default=None,
                    help="Only process this material_id (for debugging)")
    args = ap.parse_args()

    env = load_env()
    if not env.get("SUPABASE_URL"):
        print("Missing SUPABASE_URL / NEXT_PUBLIC_SUPABASE_URL", file=sys.stderr)
        sys.exit(2)

    ref = project_ref(env)
    ensure_bucket(env, args.dry_run)

    ok, missing, failed = 0, 0, 0
    updates: list[tuple[str, str]] = []  # (material_id, public_url)

    for material_id, rel in MAPPING.items():
        if args.material_id and args.material_id != material_id:
            continue
        src = CORPUS_ROOT / rel
        if not src.exists():
            print(f"[miss] {material_id}  <-  {src}")
            missing += 1
            continue
        # material_id = pep-g7-vol1-<unit>-<section>
        parts = material_id.split("-")
        book_id = "-".join(parts[:3])   # pep-g7-vol1
        unit = parts[3]                 # u1..u7
        section = "-".join(parts[4:])   # seca-1b / pr-1 / secb-1b
        object_key = f"{book_id}/{unit}/{section}.mp3"
        try:
            url = upload_object(env, object_key, src, args.dry_run)
            updates.append((material_id, url))
            ok += 1
            print(f"[ok]   {material_id}  ->  {url}")
        except Exception as e:
            failed += 1
            print(f"[fail] {material_id}: {e}")

    if updates and not args.dry_run:
        # 一条 SQL 批量回填
        cases = "\n".join(
            f"WHEN '{mid}' THEN '{url}'" for mid, url in updates
        )
        ids = ",".join(f"'{mid}'" for mid, _ in updates)
        sql = f"""
UPDATE public.listening_materials
SET audio_url = CASE material_id
{cases}
END,
updated_at = 2026080602
WHERE material_id IN ({ids});
"""
        run_sql(env, ref, sql)
        print(f"Updated audio_url for {len(updates)} rows.")

    print(f"\nSummary: ok={ok}  missing={missing}  failed={failed}  total={len(MAPPING)}")


if __name__ == "__main__":
    main()
