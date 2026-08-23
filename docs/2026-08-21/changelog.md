# 2026-08-21

## 社区发帖 MVP

- 新增 `community_posts` 与 RPC：`create_community_post` / `set_community_post_featured` / `delete_community_post`；可见性按同机构本班（`class_id`）/ 年级 / 精华裁剪。
- 教师 Web：`/teacher/community`（发帖选年级、设精华、删除）；顶栏增加「社区」。
- 学生 App：底栏「社区」Tab（online-only 信息流、发帖、确认后自删）；`AuthSession` 暴露 `classId`。
- 远端已 apply：`040_community_posts.sql`、`041_community_post_author_display_name.sql`。

## 社区入口改至底栏

- 主壳改为五 Tab：首页 / 学习 / **社区** / 数据 / 我的；去掉「我的」页内社区按钮与独立 `Screen.Community` 路由。

## 修复教师社区页加载失败

- 原因：RLS 调用 `private.can_read_community_post`，在 JWT 缺失/过期时 PostgREST 以 `anon` 执行，无 EXECUTE/SELECT 会返回 `42501 permission denied`（作业页仅比 `auth.uid()` 故表现为空列表）。
- 迁移 [`042_community_posts_rls_grants.sql`](../../supabase/migrations/042_community_posts_rls_grants.sql)：给 `anon`/`authenticated` 开表 SELECT + 函数 EXECUTE；收回直写 DML。远端已 apply。
- Web：年级多选 checkbox 不再被全局 `input { width:100% }` 撑满。

### 关键路径

- `supabase/migrations/040_community_posts.sql`
- `supabase/migrations/041_community_post_author_display_name.sql`
- `supabase/migrations/042_community_posts_rls_grants.sql`
- `web/src/app/teacher/community/*`
- `web/src/app/globals.css`
- `app/.../ui/screens/community/*`
- `app/.../ui/components/BottomNavigationBar.kt`
- `app/.../ui/screens/main/MainScreen.kt`
- `app/.../data/remote/CommunityRemoteDataSource.kt`

### 文档

- [community_posts.md](./community_posts.md)
- [permission_model.md](../2026-07-24/permission_model.md)
- [supabase_table_map.md](../2026-07-24/supabase_table_map.md)
- [project_overview.md](../2026-06-28/project_overview.md)（主壳五 Tab）
