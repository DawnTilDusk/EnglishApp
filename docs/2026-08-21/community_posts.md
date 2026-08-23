# 社区发帖 MVP

更新日期：2026-08-21

## 范围

机构内轻量社区：纯文字帖；无评论/点赞/图/编辑/软删。

| 角色 | 客户端 | 能力 |
|------|--------|------|
| student | Android 底栏「社区」Tab | 发帖（需有 `students.class_id`）、浏览可见流、删自己的帖（确认后硬删） |
| teacher | Web `/teacher/community` | 发帖并多选可见年级、浏览本人/机构教师帖/名下学生帖、设/取消精华、删自己的或名下学生帖 |
| agency_admin | — | 不参与 |

## 可见性（同机构）

| 帖类型 | 学生可见条件 |
|--------|----------------|
| 教师帖 | `profiles.grade` ∈ `target_grades` |
| 学生帖（未精华） | `students.class_id` 与帖快照 `class_id` 相同且非空 |
| 学生帖（精华） | `profiles.grade` = 帖快照 `author_grade` |

本班定义：**同机构 + 相同 `students.class_id` 字符串**（与作业班级筛选一致）。无 `class_id` 的学生发帖会被 RPC 拒绝。

**App 客户端**：社区列表在「进入社区 Tab」或「个人资料保存年级」后会自动刷新；`profiles.grade` 须在 App「我的」保存后才会写入服务端（机构 Web 学生页仅管班级 `class_id`，不能改年级）。

## 数据与合约
- 表：`community_posts`（含发帖时快照的 `author_display_name`、`class_id`、`author_grade`）
- 读：RLS + `private.can_read_community_post`
- 写：`create_community_post` / `set_community_post_featured` / `delete_community_post`（private DEFINER + public INVOKER）
- 迁移：`040_community_posts.sql`、`041_community_post_author_display_name.sql`、`042_community_posts_rls_grants.sql`（`anon`/`authenticated` 可 SELECT + 执行 `can_read_*`；无会话时为空列表而非 42501）


## 对标取舍

对齐 Google Classroom Stream：作者自删、不做编辑、删前确认硬删；教师可删名下学生帖。Classroom「教师仍可见已删」与钉钉话题体系为 Later。

## 非目标

评论、点赞、图片、编辑帖、软删审计、正式班级表、发帖代币、全班 mute。
