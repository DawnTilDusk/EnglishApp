# Seedie Supabase 表结构体检报告

日期：2026-06-30

## 1. 体检范围
- 远端 `Supabase` 的 `public` schema 全表结构
- 本地 `supabase/migrations` 历史迁移文件
- Android 客户端当前对远端表的直接读写与同步引用

本报告的目标不是立即删表，而是先回答 3 个问题：
- 哪些表是当前业务主链路
- 哪些表只是预留或用途不清
- 哪些表高概率属于旧方案残留或进行到一半的烂尾设计

## 2. 总体结论
- 当前 `public` schema 共识别出 20 张表。
- 没发现那种命名非常随意的测试垃圾表，例如 `test_xxx`、`tmp_xxx`、`bak_xxx`。
- 但存在明显的“架构漂移”问题：早期做过一套“机构 + 内容分发 + 积分台账 + 奖励兑换”的多租户模型，后期又落地了一套更贴近当前 App 的“教师商城 + 用户经济流水 + 学习进度同步”模型。
- 结果是：库里保留了一批结构完整、关系齐全、但当前客户端几乎不用的旧表，它们更像“旧架构残留”，而不是临时乱建。

## 3. 分类总览

### A. 当前主链路表
这些表和当前 Android 客户端、已存在的 RPC、远端数据量都能对应上，判断为“在用”。

| 表名 | 状态 | 主要用途 | 依据 |
| --- | --- | --- | --- |
| `profiles` | 在用 | 用户资料、角色、手机号、Profile 页面资料 | 客户端直接查询；最新 `009_profile_real_data.sql` 继续扩展该表 |
| `students` | 在用 | 学生附加资料、教师绑定、学生身份业务 | 登录后读取；商城/教师端都依赖 |
| `teachers` | 在用 | 教师身份扩展 | 教师商城与教师看板依赖 |
| `shop_products` | 在用 | 教师商城商品 | 客户端直接读写 |
| `shop_orders` | 在用 | 商品兑换订单 | 客户端直接查询，提交/通过/拒绝走 RPC |
| `user_economy_transactions` | 在用 | 当前主经济流水表 | 多个 migration 在持续围绕它补强 |
| `user_check_ins` | 在用 | 云端签到同步 | `CheckInSyncer` 直接 upsert |
| `user_vocabulary_word_learning_progress` | 在用 | 单词学习进度同步 | `VocabularyProgressSyncer` 直接 upsert |
| `user_vocabulary_study_rounds` | 在用 | 学习轮次同步 | `VocabularyProgressSyncer` 直接 upsert |
| `user_vocabulary_book_progress` | 在用 | 词书学习进度同步 | `VocabularyProgressSyncer` 直接 upsert |

### B. 待确认表
这些表不适合直接判废，但目前“远端存在”和“客户端实时依赖”之间还有断层。

| 表名 | 状态 | 观察 |
| --- | --- | --- |
| `word_books` | 待确认 | 远端有数据，但客户端当前更像使用本地 `Room` 镜像表，而不是直接从 Supabase 实时读 |
| `word_book_modules` | 待确认 | 与本地词书模块数据结构对应，远端像内容源或导入源 |
| `vocabulary_words` | 待确认 | 远端有明显数据量，客户端本地实体也对应，但未看到当前直接从远端实时取词的主链路 |

### C. 高疑似旧方案残留 / 烂尾表
这些表出自同一批早期多租户 migration，远端几乎没有有效数据，客户端也几乎没有实际使用痕迹，判断为高疑似残留。

| 表名 | 状态 | 判断理由 |
| --- | --- | --- |
| `agencies` | 高疑似残留 | 作为多租户主表被大量外键引用，但远端估算行数为 0，客户端没有围绕机构的真实业务链路 |
| `content_items` | 高疑似残留 | 早期“内容库”设计，客户端未直接使用 |
| `content_assignments` | 高疑似残留 | 早期“内容下发”设计，客户端未直接使用 |
| `study_events` | 高疑似残留 | 早期学习事件表，客户端未直接写入；后续学习同步改成了专门的进度同步表 |
| `points_ledger` | 高疑似残留 | 与后来的 `user_economy_transactions` 功能高度重叠，当前 App 主经济系统已改走新表 |
| `rewards` | 高疑似残留 | 早期“奖励兑换”模型的一部分，后续商城功能改成了 `shop_products` |
| `redemptions` | 高疑似残留 | 与后来的 `shop_orders` 业务重叠，当前客户端未接这张表 |

## 4. 关键证据

### 4.1 早期多租户旧架构集中出现在 `init_multi_tenant_schema.sql`
在 [init_multi_tenant_schema.sql](file:///d:/MainFile/work/android/AndroidStudioProjects/Seedie/supabase/migrations/init_multi_tenant_schema.sql) 中，一次性创建了以下整套对象：
- `agencies`
- `profiles`
- `students`
- `content_items`
- `content_assignments`
- `study_events`
- `points_ledger`
- `rewards`
- `redemptions`

这说明它们原本属于同一套顶层设计，而不是后期逐步演进出来的当前主业务模型。

### 4.2 后续商城与经济系统另起了一套更贴近当前产品的实现
在 [002_shop_and_economy.sql](file:///d:/MainFile/work/android/AndroidStudioProjects/Seedie/supabase/migrations/002_shop_and_economy.sql) 中，又单独引入了：
- `shop_products`
- `shop_orders`
- `user_economy_transactions`

而且这套表配合 [003_teacher_shop_rpc.sql](file:///d:/MainFile/work/android/AndroidStudioProjects/Seedie/supabase/migrations/003_teacher_shop_rpc.sql) 的 RPC 形成了当前真正可跑通的商城与代币业务闭环。

这意味着：
- `points_ledger` 与 `user_economy_transactions` 存在明显重复建模
- `rewards/redemptions` 与 `shop_products/shop_orders` 存在明显业务重叠

### 4.3 当前 Android 客户端实际接入的是“新链路”
从代码引用看，当前客户端主要直接读写以下远端表：
- `profiles`
- `students`
- `shop_products`
- `shop_orders`
- `user_check_ins`
- `user_vocabulary_word_learning_progress`
- `user_vocabulary_study_rounds`
- `user_vocabulary_book_progress`

关键代码位置：
- [AuthService.kt](file:///d:/MainFile/work/android/AndroidStudioProjects/Seedie/app/src/main/java/com/example/seedie/data/remote/AuthService.kt)
- [ProfileRepositoryImpl.kt](file:///d:/MainFile/work/android/AndroidStudioProjects/Seedie/app/src/main/java/com/example/seedie/data/repository/ProfileRepositoryImpl.kt)
- [ShopRemoteDataSource.kt](file:///d:/MainFile/work/android/AndroidStudioProjects/Seedie/app/src/main/java/com/example/seedie/data/remote/ShopRemoteDataSource.kt)
- [CheckInSyncer.kt](file:///d:/MainFile/work/android/AndroidStudioProjects/Seedie/app/src/main/java/com/example/seedie/data/sync/syncer/CheckInSyncer.kt)
- [VocabularyProgressSyncer.kt](file:///d:/MainFile/work/android/AndroidStudioProjects/Seedie/app/src/main/java/com/example/seedie/data/sync/syncer/VocabularyProgressSyncer.kt)

而 `content_items / content_assignments / study_events / points_ledger / rewards / redemptions / agencies` 在客户端主代码中基本没有实际远端读写链路。

### 4.4 远端行数估算支持“旧表闲置”判断
远端 `public` schema 的表状态显示：
- `profiles`、`shop_orders`、`user_economy_transactions`、`user_check_ins`、词汇进度同步表都有实际数据
- `agencies`、`content_items`、`content_assignments`、`study_events`、`points_ledger`、`rewards`、`redemptions` 的估算数据量几乎都为 0

这进一步说明上述旧表并不是当前线上业务的主要承载对象。

## 5. 逐组判断

### 5.1 用户与身份组

#### `profiles`
- 当前核心资料表
- 承担角色、邮箱、手机号、Profile 页面展示资料
- 最新 `Profile` 真实业务接入也继续围绕它扩展
- 结论：保留，继续作为主资料表演进

#### `students`
- 当前学生端真实业务仍依赖
- 登录恢复、教师学生关系、商城商品可见性、教师看板统计都用到
- 结论：保留

#### `teachers`
- 是教师商城和教师端统计的基础身份表
- 结论：保留

#### `agencies`
- 结构上是整个旧多租户模型的根
- 但当前 App 没有机构端真实功能闭环，远端估算也没有数据
- 结论：高疑似旧架构残留，先不要删，先标记为 legacy 候选

### 5.2 学习同步组

#### `user_check_ins`
- 与本地签到持久化同步逻辑一致
- 教师统计 RPC 也会汇总它
- 结论：保留

#### `user_vocabulary_word_learning_progress`
#### `user_vocabulary_study_rounds`
#### `user_vocabulary_book_progress`
- 三张表形成当前云端学习进度同步主链路
- 结论：保留

#### `study_events`
- 理论上是更通用的学习行为事件流
- 但当前实际已经不是走这个思路，而是直接同步结果表
- 结论：高疑似烂尾设计

### 5.3 经济与兑换组

#### `user_economy_transactions`
- 当前主经济流水表
- 后续 reconciliation 和 batch sync 也都基于它
- 结论：保留

#### `points_ledger`
- 和 `user_economy_transactions` 职责重复
- 当前客户端、当前 RPC 主链路都不围绕它
- 结论：高疑似旧版积分系统残留

#### `shop_products`
#### `shop_orders`
- 当前教师商城主链路
- 已有完整 RPC 和客户端调用
- 结论：保留

#### `rewards`
#### `redemptions`
- 更像早期的“奖励兑换”模型
- 被教师商城这套表替代的迹象很明显
- 结论：高疑似旧方案残留

### 5.4 内容组

#### `content_items`
#### `content_assignments`
- 更像早期“机构给学生分配内容”的设计
- 当前客户端未见对应消费入口
- 结论：高疑似烂尾 / 旧方案残留

#### `word_books`
#### `word_book_modules`
#### `vocabulary_words`
- 远端存在、字段也完整
- 但客户端当前核心学习流程更像先落地到本地 `Room`
- 结论：待确认，不建议贸然清理

## 6. 当前最值得警惕的结构问题

### 6.1 重复建模
- `points_ledger` vs `user_economy_transactions`
- `rewards/redemptions` vs `shop_products/shop_orders`

这说明历史上至少切换过一次业务实现方向，旧表未回收。

### 6.2 多租户设计停在 schema 层
- `agencies`
- `agency_admin`
- 机构维度 RLS

这些设计在数据库层很完整，但客户端没有对应产品闭环，说明多租户方案大概率停在早期阶段。

### 6.3 用户标识类型不完全统一
- `profiles.id`、`students.id`、`user_check_ins.user_id` 是 `UUID`
- 词汇同步三张表中的 `user_id` 使用 `TEXT`

虽然当前能工作，但这是明显的技术债，后续清理 schema 时值得统一。

## 7. 建议处理顺序

### 第一阶段：先做标记，不删表
- 将以下表先标记为“疑似 legacy”：
  - `agencies`
  - `content_items`
  - `content_assignments`
  - `study_events`
  - `points_ledger`
  - `rewards`
  - `redemptions`

### 第二阶段：补一轮“函数 / 策略 / 触发器”体检
删表前必须继续检查：
- 是否还有 RPC 依赖旧表
- 是否还有 RLS policy 仍服务于旧表
- 是否还有 trigger 或后台脚本引用旧表

目前仅从表和客户端代码看，这批表非常像旧方案残留，但删除决策还不能只靠表结构本身。

### 第三阶段：决定最终命运
- 如果确认没有任何 RPC / Edge Function / 后台脚本使用
  - 进入“归档后删除”流程
- 如果仍有后台人工流程依赖
  - 保留，但必须文档化，明确写成“后台专用表”

## 8. 当前建议结论

### 可继续演进的主表
- `profiles`
- `students`
- `teachers`
- `shop_products`
- `shop_orders`
- `user_economy_transactions`
- `user_check_ins`
- `user_vocabulary_word_learning_progress`
- `user_vocabulary_study_rounds`
- `user_vocabulary_book_progress`

### 暂不动、但要继续确认用途的表
- `word_books`
- `word_book_modules`
- `vocabulary_words`

### 高优先级排查的 legacy 候选表
- `agencies`
- `content_items`
- `content_assignments`
- `study_events`
- `points_ledger`
- `rewards`
- `redemptions`

## 9. 一句话总结
- 当前 Supabase 没有明显的“随手乱建垃圾表”，但确实有一批“旧架构没清干净”的表。
- 最像烂尾的是早期多租户内容/积分兑换那一套。
- 当前真实在跑的主业务已经切换到 `profiles + students + 教师商城 + 用户经济流水 + 学习进度同步` 这条新链路。

## 10. 2026-07-24 经济同步补注

- Migration `014_economy_sync_hardening.sql`：`sync_my_economy_transactions` 仅接受合法 UUID，`ON CONFLICT (id) DO NOTHING`；非法 id 跳过（不再无 id 插入）。
- 新增部分唯一索引 `(user_id, ref_id) WHERE ref_id IS NOT NULL`。
- `points_ledger` 仍为残留表，**本次不删除**；主经济路径继续只用 `user_economy_transactions`。
- 设计与排障：[`docs/2026-07-24/economy_token_system.md`](../2026-07-24/economy_token_system.md)。
