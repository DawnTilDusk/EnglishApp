# 教师 / 学生双模式与商城 — Supabase 操作指南

项目 Supabase URL：`https://xojbnrxnkgkqacrkcmqc.supabase.co`

## 1. 执行 Migration（按顺序）

在 Dashboard → **SQL Editor** 中依次运行：

0. [`supabase/migrations/000_user_sync_tables.sql`](../supabase/migrations/000_user_sync_tables.sql)（若尚无 4 张 `user_*` 同步表）
1. [`supabase/migrations/001_add_teacher_role.sql`](../supabase/migrations/001_add_teacher_role.sql)
2. [`supabase/migrations/002_shop_and_economy.sql`](../supabase/migrations/002_shop_and_economy.sql)
3. [`supabase/migrations/003_teacher_shop_rpc.sql`](../supabase/migrations/003_teacher_shop_rpc.sql)

若从未部署基础 schema，需先运行 `supabase/migrations/` 下原有 7 个文件。

若缺少 App 同步用的 4 张 `user_*` 表，请参考 [`docs/2026-05-19/claude_project.md`](2026-05-19/claude_project.md) 或在 SQL Editor 中按 App DTO 字段补建。

## 2. 创建教师账号

```sql
SELECT public.create_teacher_account(
  'teacher1@example.com',
  'YourPassword123',
  '张老师'
);
-- 记下返回的 UUID
```

## 3. 绑定学生到教师

```sql
UPDATE public.students
SET teacher_id = '<teacher_uuid>'
WHERE id = '<student_uuid>';
```

验证：

```sql
SELECT s.name, t.display_name AS teacher
FROM public.students s
JOIN public.teachers t ON t.id = s.teacher_id
WHERE s.id = '<student_uuid>';
```

## 4. 测试商品（可选）

```sql
INSERT INTO public.shop_products (teacher_id, name, description, price_tokens, stock, is_active)
VALUES ('<teacher_uuid>', '文具套装', '测试商品', 50, 10, true);
```

## 5. App 联调流程

| 步骤 | 操作 | 预期 |
|------|------|------|
| 1 | 学生登录 → 学习赚代币 | 本地 Room 有交易记录 |
| 2 | 联网 sync | `user_economy_transactions` 有正向记录 |
| 3 | 学生 → 我的 → 教师商城 → 购买 | `shop_orders.status = pending` |
| 4 | 教师登录 → 商城 → 批准/拒绝 | 状态变更；拒绝时退款 |

## 6. 日常运维

| 需求 | SQL / 位置 |
|------|------------|
| 新教师 | `create_teacher_account(...)` |
| 换绑学生 | `UPDATE students SET teacher_id = ...` |
| 查订单 | Table Editor → `shop_orders` |
| 查代币 | `SELECT SUM(amount) FROM user_economy_transactions WHERE user_id = '...'` |

## 7. 注意事项

- 学生未绑定 `teacher_id` 时，App 商城入口会提示无法购物。
- 购买需联网，走 RPC 原子扣款。
- legacy 表 `rewards` / `redemptions` 本次不使用，商城数据在 `shop_products` / `shop_orders`。
