# Changelog — 2026-07-24

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
