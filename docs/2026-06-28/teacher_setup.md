# 教师 / 机构 / 学生 — Supabase 与客户端操作指南

项目 Supabase URL：`https://xojbnrxnkgkqacrkcmqc.supabase.co`

权限说明见 [`docs/2026-07-24/permission_model.md`](../2026-07-24/permission_model.md)。

## 客户端边界

| 角色 | 客户端 |
|------|--------|
| 学生 | Android App |
| 教师 | Web `web/` → `/teacher` |
| 机构管理员 | Web `web/` → `/agency` |
| 开发者 | Dashboard / `service_role` / 脚本（非产品登录） |

## 1. 执行 Migration（按顺序）

在 Dashboard → **SQL Editor** 中依次运行（或配置 `SUPABASE_DB_URL` 后执行 `python scripts/apply_migrations.py`）：

### 既有（若环境已部署可跳过）

0. `000_user_sync_tables.sql` … `009`（见历史文档）

### 本次分层 + 机构商城

1. [`supabase/migrations/010_agency_teacher_hierarchy.sql`](../../supabase/migrations/010_agency_teacher_hierarchy.sql)
2. [`supabase/migrations/011_agency_shop.sql`](../../supabase/migrations/011_agency_shop.sql)
3. [`supabase/migrations/012_account_rpc_and_triggers.sql`](../../supabase/migrations/012_account_rpc_and_triggers.sql)
4. [`supabase/migrations/013_rls_progress_hierarchy.sql`](../../supabase/migrations/013_rls_progress_hierarchy.sql)

跑完后：

```bash
python scripts/audit_hierarchy.py
```

## 2. 创建机构管理员（仅 service_role）

在 SQL Editor（postgres）或带 service_role 的脚本中：

```sql
SELECT public.create_agency_admin(
  'agency1@example.com',
  'YourPassword123',
  '<agency_uuid>',
  '某机构管理员'
);
```

## 3. 创建教师（机构管理员或 service_role）

机构管理员登录 Web 后在「教师」页创建，或：

```sql
-- 作为 service_role 时必须传 p_agency_id
SELECT public.create_teacher_account(
  'teacher1@example.com',
  'YourPassword123',
  '张老师',
  '<agency_uuid>'
);
```

## 4. 绑定学生到教师

Web 机构台「学生」页，或：

```sql
SELECT public.bind_student_to_teacher('<student_uuid>', '<teacher_uuid>');
```

## 5. 机构商城

- 一机构一店：`shop_products.agency_id` / `shop_orders.agency_id`
- 机构 Web 管理商品；教师 Web 只读；学生 App 浏览并购买
- 购买 RPC：`submit_shop_order` — **即时扣款**，订单状态 `completed`
- **已移除** `approve_shop_order` / `reject_shop_order`

测试商品（机构 UUID）：

```sql
INSERT INTO public.shop_products (agency_id, name, description, price_tokens, stock, is_active)
VALUES ('<agency_uuid>', '文具套装', '测试商品', 50, 10, true);
```

## 6. Web 本地启动

```bash
cd web
cp .env.example .env.local   # 填入 NEXT_PUBLIC_SUPABASE_URL / ANON_KEY
npm install
npm run dev
```

## 6.1 教师 Web 能力（`/teacher`）

教师邮箱密码登录后进入 `/teacher`。学生账号登录 Web 会被拒绝并提示使用 App。

| 页面 | 能力 |
|------|------|
| `/teacher` | 名下学生列表；`?q=` 按姓名 `ilike` 筛选 |
| `/teacher/students/[id]` | 概览：档案 + `get_teacher_student_stats` KPI（签到天数、学习分钟、已学词、代币余额） |
| 同上 `?section=checkins` | 只读 `user_check_ins`（最近 90 条） |
| 同上 `?section=vocab` | 只读词书进度 + 学习轮次（各最近 50 条） |
| 同上 `?section=economy` | 余额（RPC）+ 只读 `user_economy_transactions`（最近 100 条） |
| `/teacher/shop`、`/teacher/orders` | 本机构商城商品 / 订单只读 |

约束：

- 仅能看 `students.teacher_id = 自己` 的学生；非名下 id → 404。
- **只读已同步到云端的数据**（App 本地未 sync 的进度/流水在 Web 不可见）。
- 教师不能改学生进度、补代币或建账号（机构管理员 / `service_role` 负责）。

实现入口：`web/src/lib/teacher-student.ts`（归属校验）、`web/src/app/teacher/`。

## 7. App 联调

| 步骤 | 操作 | 预期 |
|------|------|------|
| 1 | 学生登录 App | 仅学生角色可进 |
| 2 | 学习赚代币并同步 | `user_economy_transactions` 有记录；同 session 重试不双倍 |
| 3 | 机构商城购买 | 即时扣款；`shop_orders.status = completed` |
| 4 | 机构 Web 看订单 | 可见该订单；无审单按钮 |
| 5 | 共用设备换账号 | 后登录者余额不继承前用户本地流水 |

代币同步与余额异常排查见 [`docs/2026-07-24/economy_token_system.md`](../2026-07-24/economy_token_system.md)。

## 8. 日常运维

| 需求 | 方式 |
|------|------|
| 新机构管理员 | `create_agency_admin` + service_role |
| 新教师 | 机构 Web 或 `create_teacher_account` |
| 换绑学生 | `bind_student_to_teacher` / 机构 Web |
| 查订单 | 机构 Web 或 Table Editor → `shop_orders` |
| 补代币差额 | 仅 service_role 可调 `reconcile_my_token_balance`（**App 不调用**） |
| 余额异常 / 疑似重复入账 | 见 [economy_token_system.md](../2026-07-24/economy_token_system.md) §8；人工对账，勿对 authenticated 开放 reconcile |
