# Seedie Web Console

机构管理员与教师的 B 端控制台（Next.js + Supabase）。

## Setup

1. Copy env:

```bash
cp .env.example .env.local
```

Fill with the same project URL / anon key as the Android app (never put `service_role` here).

2. Install and run:

```bash
npm install
npm run dev
```

Open http://localhost:3000

## Roles

| Role | Routes |
|------|--------|
| `agency_admin` | `/agency/*` |
| `teacher` | `/teacher/*` |
| `student` | rejected (use Android app) |

Apply SQL migrations `010`–`013` in Supabase SQL Editor before using shop / teacher-agency features.
