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

Migration [`016_security_definer_hardening.sql`](../../supabase/migrations/016_security_definer_hardening.sql)：

- 所有相关函数固定 `search_path = public`。
- **产品 RPC**（`authenticated` 可调）：`create_teacher_account`、`create_student_account`、`bind_student_to_teacher`、`submit_shop_order`、`sync_my_economy_transactions`、`get_my_token_balance`、`get_teacher_student_stats`、`set_my_profile` / `set_my_phone` / `set_my_device_id`。一律 `REVOKE` 掉 `anon` / `PUBLIC`。
- **RLS 助手**（`authenticated` 可 EXECUTE，供 policy 调用；非业务 API）：`get_auth_role`、`get_auth_agency_id`、`is_agency_admin_of`、`is_teacher_in_agency`。
- **触发器 / 内部**：`handle_new_user`、`sync_*`、`enforce_student_teacher_same_agency`、`get_user_token_balance`、`is_service_role` — 对 `anon` / `authenticated` 无 EXECUTE；`reconcile_my_token_balance` / `create_agency_admin` 仅 `service_role`。
- 业务 RPC 仍为 `SECURITY DEFINER`（写 `auth.users`、跨表事务等）；Database Linter 的 `authenticated_security_definer_function_executable`（0029）对这批会残留，属预期。
- Storage：`word-audio` 去掉可 listing 的 broad SELECT；公开对象 URL 仍可用。
- Auth：在 Dashboard 开启 **Leaked password protection**（HaveIBeenPwned）；无法用 SQL migration 开关。
