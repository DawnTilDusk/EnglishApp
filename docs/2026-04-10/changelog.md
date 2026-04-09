# Seedie App Changelog (2026-04-10)

> 记录本轮围绕 Supabase 登录、会话缓存、启动路由与基础账号绑定的实现进展与当前可验证状态。

## 本轮新增能力
- **Supabase 登录已接通**: 支持邮箱+密码登录，并接入基础错误提示与加载态。
- **会话自动缓存已启用**: 登录成功后会在本机持久化 Auth Session（Token），冷启动可自动恢复登录态。
- **启动路由已基于 SessionStatus**: App 启动先进入“检查中”状态，等待 Supabase SDK 完成本地会话恢复后，再路由到登录流程或主业务流程。
- **登录页新增手机号绑定入口**: 学生首次登录可要求填写手机号完成绑定；手机号支持基础正则校验与必填限制（MVP）。

## 数据与后端进展（Supabase）
- **profiles 扩展字段已补齐**: 增加 email、phone 等字段用于业务侧展示与管理（认证仍以 auth.users 为准）。
- **手机号绑定 RPC 已落地**: 提供 `set_my_phone` 用于学生自助写入 profiles.phone，并同步到 auth user_metadata（当前 phone_verified 仍为 false）。
- **触发器/同步策略已对齐**: users -> students，students/agency -> profiles 的数据流向已明确并落库。

## 工程与依赖进展
- **Supabase 依赖已修正**: 移除过期的 `gotrue-kt` 依赖并替换为 `auth-kt`，避免 Gradle 解析警告/失败。
- **AuthService 业务会话缓存已建立**: 使用 `StateFlow` 在内存中维护 `AuthSession`（role、agency_id、studentName），供 UI 与后续功能复用。

## 当前可验证结果
- **免二次登录有效**: 登录成功后杀进程冷启动，App 可在本地会话恢复完成后自动进入主流程（无需再次输入账号密码）。
- **离线行为可预期**: 离线时如果本机存在 session，仍可进入主流程；需要网络的资料拉取会失败但不阻塞进入（业务信息可稍后补齐）。
- **路由抖动已降低**: 业务资料拉取（profiles/students）不再阻塞登录态判定，提升启动体感并减少弱网误判。

## 当前实现边界
- **业务资料拉取仍为串行请求**: profiles/students 当前分两次请求，后续可合并为单个 RPC 以减少时延与失败面。
- **手机号验证仍为 MVP**: 目前不做短信验证码与 phone_verified 真正校验，仅完成“绑定与同步”的最小闭环。
