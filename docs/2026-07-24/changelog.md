# Changelog — 2026-07-24

## Database Linter 安全硬化（search_path / EXECUTE / Storage）

### 摘要

按混合策略收紧 `SECURITY DEFINER`：补 `search_path`、对 `anon`/`PUBLIC` 撤权、触发器与内部函数不对客户端开放；产品 RPC 仍由 `authenticated` 调用。顺带去掉 `word-audio` 可 listing 的 SELECT 策略。

### Schema

| 项 | 说明 |
|----|------|
| SQL | [`016_security_definer_hardening.sql`](../../supabase/migrations/016_security_definer_hardening.sql) |
| Storage | `DROP POLICY "public read audio"` on `storage.objects`（公开 URL 仍可用） |

### 运维

- 应用 `016` 后复跑 Database Linter：`function_search_path_mutable`、`anon_security_definer_*`、`public_bucket_allows_listing` 应消失。
- `authenticated_security_definer_function_executable`（0029）对产品 RPC / RLS 助手会残留，见 [permission_model.md](./permission_model.md)。
- Dashboard → Auth → 开启 **Leaked password protection**（`auth_leaked_password_protection`）。

### 文档

- 修订 [permission_model.md](./permission_model.md)、[supabase_table_map.md](./supabase_table_map.md)

## Supabase 表地图文档

新建 [supabase_table_map.md](./supabase_table_map.md)：每张在用/已删表的用途与交互；澄清经济双账本与 `content_assignments` 原意图。

## Supabase 废表与废函数清理

### 摘要

删除早期多租户内容/积分/兑换残留表，以及未使用的购买别名与单条经济 RPC；主链路不变。

详细说明见 [legacy_schema_cleanup.md](./legacy_schema_cleanup.md)。

### Schema / 代码

| 项 | 说明 |
|----|------|
| SQL | [`015_drop_legacy_tables_and_rpcs.sql`](../../supabase/migrations/015_drop_legacy_tables_and_rpcs.sql)：DROP 6 表 + `purchase_shop_product` + `record_my_economy_transaction` |
| App | 删除未使用的 `EconomyRemoteDataSource.recordMyEconomyTransaction` |
| 保留 | `agencies`、词书表、`redemption_status`、`submit_shop_order` / `sync_my_economy_transactions` |

### 已删表

`content_assignments`、`content_items`、`redemptions`、`rewards`、`study_events`、`points_ledger`

### 运维

- 应用 `015` 前对 6 表跑 `COUNT(*)` 预检；**远端已于 2026-07-24 预检全 0 后执行 DROP**，复查表/函数均已不存在；`agencies` / `user_economy_transactions` / `shop_*` 保留。
- 不自动回滚：表为空时可从历史 migration 重建结构，无需数据恢复。

### 文档

- 新建 [legacy_schema_cleanup.md](./legacy_schema_cleanup.md)、[supabase_table_map.md](./supabase_table_map.md)
- 修订 [economy_token_system.md](./economy_token_system.md)、[permission_model.md](./permission_model.md)
- 修订 `docs/2026-06-28/teacher_setup.md`、`docs/2026-06-30/supabase_schema_health_report.md`
- 修订 `supabase/migrations/004_verify_teacher_setup.sql`

## 学生代币虚增修复与经济链路硬化

### 摘要

修复学生登录/联网后代币异常增加，并清理相关技术债：无主流水认领、云端非幂等同步、假补差流水、每日任务未按用户隔离、Session 与 Economy 双轨代币。

### 根因（简述）

1. `claimOrphanTransactions` 把 `userId = ''` 的历史流水认领给当前登录用户并上传。
2. `sync_my_economy_transactions` 对非法 UUID 走无 id 插入，重传可重复入账。
3. `refreshBalanceFromCloud` 在 cloud>local 时插入 `"Cloud balance sync"`，固化虚高。

### 代码与 Schema

| 项 | 说明 |
|----|------|
| App Room | v7→v8：`economy_transactions.refId`；`daily_tasks.userId` |
| Syncer | 删 orphan；只推 PENDING；成功后按 id 标 SYNCED |
| EconomyManager | 余额投影 `cloud + PENDING`；`addTokens(..., refId)`；不再写假补差 |
| Session | 停止 `addTokens`；代币只认 EconomyManager |
| SQL | [`014_economy_sync_hardening.sql`](../../supabase/migrations/014_economy_sync_hardening.sql)：幂等 sync + `(user_id, ref_id)` 唯一索引 |

### 风险与回滚

- 已污染云端余额**不会**自动下降；见 [economy_token_system.md](./economy_token_system.md) 排查 SQL。
- 014 建唯一索引前会删除同 `(user_id, ref_id)` 的重复行（保留 `created_at` 最早）。
- App 回退即可；SQL 可用 007 函数定义覆盖（索引保留无害）。不要恢复 claim orphan。

### 文档

- 新建 [economy_token_system.md](./economy_token_system.md)
- 修订 `docs/2026-06-28/teacher_setup.md`、`project_overview.md`
- 修订 `docs/2026-06-30/supabase_schema_health_report.md` 短注
