# 权限模型与客户端边界

更新日期：2026-07-24

## 身份分层

| 层 | 身份 | 如何认证 | 客户端 |
|----|------|----------|--------|
| 平台 | 开发者 | `service_role` / Dashboard / 本地脚本 | 无产品 UI |
| 业务 | `agency_admin` | Supabase Auth + `profiles.role` | Web `/agency` |
| 业务 | `teacher` | 同上 | Web `/teacher` |
| 业务 | `student` | 同上 | Android App |

`company_admin` 已废弃，不再作为产品身份；平台运维一律用 `service_role`。

## 权限链

```
service_role → agency_admin → teacher → student
```

- 机构管理员：本机构教师账号、学生绑定、**机构店**商品读写、本机构订单与学习数据只读汇总。
- 教师：绑定学生的学习数据；对本机构店商品/订单**只读**。
- 学生：本人学习数据；浏览本机构上架商品并下单。

## 商城模型（机构一店）

- 一机构一店：`shop_products.agency_id` / `shop_orders.agency_id`。
- **无审单**：`submit_shop_order` 校验余额与库存后**即时扣款**，订单状态直接为 `completed`。
- 已废弃：`approve_shop_order`、`reject_shop_order`、业务上的 `pending` 工作流。
- 教师不再拥有独立店铺。

## 客户端边界

| 能力 | Android | Web |
|------|---------|-----|
| 学习 / 打卡 / 词汇 | 学生 | — |
| 商城购买 | 学生 | — |
| 机构店商品管理 | — | 机构 |
| 订单查看（机构维度） | 学生看自己的 | 机构全部；教师本机构只读 |
| 教师账号 / 绑定学生 | — | 机构 |
| 学生进度（教师视角） | — | 教师 |

学生账号登录 Web → 拒绝并提示使用 App。  
教师 / 机构账号登录 App → 拒绝并提示使用网页端。

学生代币账本、同步与余额排查见 [`economy_token_system.md`](./economy_token_system.md)。

## SECURITY DEFINER 与 EXECUTE

Migrations：[`016_security_definer_hardening.sql`](../../supabase/migrations/016_security_definer_hardening.sql)、[`017_private_security_definer.sql`](../../supabase/migrations/017_private_security_definer.sql)。

- **016**：固定 `search_path`；`REVOKE` `anon`/`PUBLIC`；触发器与内部函数不对客户端开放；去掉 `word-audio` listing SELECT。
- **017**（清 lint 0029）：真正的 `SECURITY DEFINER` 实现放在未暴露的 `private` schema；`public` 上同名产品 RPC 仅为 `SECURITY INVOKER` 薄包装（客户端仍调 `/rest/v1/rpc/...`）。
- **RLS 助手**仅存在于 `private`：`get_auth_role`、`get_auth_agency_id`、`is_agency_admin_of`、`is_teacher_in_agency`（policy 已改为 `private.*`）。
- **产品 RPC**（`public` INVOKER → `private` DEFINER）：`create_teacher_account`、`create_student_account`、`bind_student_to_teacher`、`submit_shop_order`、`sync_my_economy_transactions`、`get_my_token_balance`、`get_teacher_student_stats`、`set_my_profile` / `set_my_phone` / `set_my_device_id`。
- **内部**：`private.get_user_token_balance`；`reconcile_my_token_balance` / `create_agency_admin` 仅 `service_role`。
- Auth：**Leaked password protection**（HIBP）需 Pro 及以上；Free 计划 API 返回 402，无法开启。
