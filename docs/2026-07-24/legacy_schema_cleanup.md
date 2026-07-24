# Supabase 废表与废函数清理

更新日期：2026-07-24

## 背景

早期 `init_multi_tenant_schema` 曾引入一套「机构内容下发 + 积分台账 + 奖励兑换」模型；后续产品改走教师/机构商城（`shop_*`）与 `user_economy_transactions`。旧表长期无客户端引用、远端行数估为 0。另有两个未使用 RPC：`purchase_shop_product`（`submit_shop_order` 别名）、`record_my_economy_transaction`（已被 batch `sync_my_economy_transactions` 取代）。

## 变更摘要

Migration：[`015_drop_legacy_tables_and_rpcs.sql`](../../supabase/migrations/015_drop_legacy_tables_and_rpcs.sql)

### 已删除的表

| 表 | 原用途（已废弃） |
|----|------------------|
| `content_assignments` | 内容下发 |
| `content_items` | 内容库 |
| `redemptions` | 旧奖励兑换单 |
| `rewards` | 旧奖励商品 |
| `study_events` | 旧学习事件流 |
| `points_ledger` | 旧积分流水（与 `user_economy_transactions` 重叠） |

### 已删除的函数

| 函数 | 说明 |
|------|------|
| `purchase_shop_product(UUID)` | 仅转发到 `submit_shop_order` 的别名 |
| `record_my_economy_transaction(UUID, INT, TEXT, TEXT)` | 单条入账 RPC；App 只走 batch sync |

`approve_shop_order` / `reject_shop_order` 已在 `011` 删除，本次未再处理。

### 明确保留

- `agencies`（机构层级 + 机构店，`010`/`011` + Web `/agency`）
- 词书：`word_books` / `word_book_modules` / `vocabulary_words`
- `redemption_status` enum（`shop_orders.status` 仍在用）
- `company_admin` enum 值（产品身份已弃用，类型层不动）
- `submit_shop_order`、`sync_my_economy_transactions`、`reconcile_my_token_balance`（service_role）

### App

- 删除 [`EconomyRemoteDataSource.recordMyEconomyTransaction`](../../app/src/main/java/com/example/seedie/data/remote/EconomyRemoteDataSource.kt)（不在 `EconomyCloudGateway` 上，无调用方）

### 文档与校验脚本

- 本文件；[`changelog.md`](./changelog.md) 同日条目
- [`economy_token_system.md`](./economy_token_system.md)：非目标中去掉「废弃 points_ledger」
- [`permission_model.md`](./permission_model.md)、[`teacher_setup.md`](../2026-06-28/teacher_setup.md)：购买 RPC 只写 `submit_shop_order`
- [`supabase_schema_health_report.md`](../2026-06-30/supabase_schema_health_report.md) §11 补注
- [`004_verify_teacher_setup.sql`](../../supabase/migrations/004_verify_teacher_setup.sql)：RPC 检查去掉已废弃审单函数

## 运维

应用 `015` 前建议对 6 表做 `COUNT(*)`（见 migration 文件头注释）。  
**远端已于 2026-07-24 预检全 0 后执行 DROP**；复查 6 表与 2 个函数均已不存在。

## 不做

- 不改写历史 CREATE 迁移（`init_*` / `006` / `011` 等）
- 不删词书表、不删 `redemption_status` / `agencies`
- 不改经济主路径业务逻辑
