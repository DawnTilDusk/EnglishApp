# 学生代币系统（Economy Ledger）

更新日期：2026-07-24

## 1. 背景

2026-07 发现部分学生账号在无对应学习行为时余额上升，共用平板换账号后更明显。根因不在「登录礼包」，而在本地同步与云端 RPC 的错误组合。

权威余额：`SUM(amount)` on `public.user_economy_transactions`。  
本地账本：Room `economy_transactions`，仅上传 `PENDING` 行。

详细根因与修复见同目录 [changelog.md](./changelog.md)。

## 2. 不变量

1. **单一写入口**：业务只通过 `EconomyManager` 增减代币。
2. **用户隔离**：本地流水必须有非空 `userId`；`userId = ''` 在同步前删除，永不认领给当前用户。
3. **只上传 PENDING**：`SYNCED` 不重传；云端按交易 `id`（UUID）`ON CONFLICT DO NOTHING`。
4. **业务幂等**：学习/任务发放带稳定 `refId`；本地按 `(userId, refId)` 去重；云端有部分唯一索引 `(user_id, ref_id) WHERE ref_id IS NOT NULL`。
5. **余额是投影，不是假账**：禁止再插入 `"Cloud balance sync"` 之类对齐用流水。
6. **客户端禁止抬高云端**：`reconcile_my_token_balance` 仅 `service_role`；App 不调用。

## 3. 余额公式

```text
displayed = if (cloudCache known) cloudBalance + sum(PENDING)
            else sum(all local rows for user)   // cold start
```

实现：`projectDisplayedTokenBalance`（`domain/model/EconomyBalance.kt`），由 `EconomyManagerImpl.totalTokens` / `refreshBalanceFromCloud` 使用。

## 4. refId 约定

| 场景 | refId |
|------|--------|
| 学习结算 | `study:{sessionId}` |
| 每日任务领取 | `task:{userId}:{yyyy-MM-dd}:{normalizedTitle}` |
| 花园消费等 | 可空（无业务幂等键时） |

## 5. 同步状态机

```text
insert local (PENDING) → syncAllToCloud(PENDING only) → mark SYNCED
failure → remain PENDING，下次重试
```

`EconomyTransactionSyncer` 入口会先 `deleteOrphanTransactions()`。

## 6. 每日任务

`daily_tasks` 含 `userId`；Dashboard / Main 只读写当前 session 用户。历史 `userId = ''` 行对新用户不可见。

## 7. 非目标（尚未做）

- 花园服务端权威扣款
- 客户端镜像全量云端流水
- 自动清洗已污染生产账户

（`points_ledger` 已删除，见 [legacy_schema_cleanup.md](./legacy_schema_cleanup.md) / migration `015`。）

## 8. 运维排查（只读示例）

```sql
-- 某学生云端余额
SELECT public.get_user_token_balance('<user_uuid>'::uuid);

-- 流水明细
SELECT id, amount, reason, ref_id, created_at
FROM public.user_economy_transactions
WHERE user_id = '<user_uuid>'
ORDER BY created_at DESC;

-- 同 ref_id 重复（索引建立后不应再出现）
SELECT user_id, ref_id, COUNT(*)
FROM public.user_economy_transactions
WHERE ref_id IS NOT NULL
GROUP BY user_id, ref_id
HAVING COUNT(*) > 1;

-- 短时间大量同 reason（疑似历史非幂等重传）
SELECT reason, COUNT(*), SUM(amount)
FROM public.user_economy_transactions
WHERE user_id = '<user_uuid>'
GROUP BY reason
ORDER BY COUNT(*) DESC;
```

修正污染数据需 `service_role` 人工删重复行或调账；**不要**对 authenticated 开放 reconcile。
