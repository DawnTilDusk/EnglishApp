# 2026-08-23

## 修复：改年级后社区仍看不到教师年级帖

- **原因**：`CommunityFeedViewModel` 挂在 `MainScreen` 导航作用域，切换底栏 Tab 不会销毁；若先打开社区（年级未改或尚无帖）再在个人资料改年级，列表不会自动重拉。服务端 RLS 按当前 `profiles.grade` 判断，数据本身正确。
- **修复**：进入社区 Tab 时递增 `enterKey` 触发刷新；资料保存年级后通过 `CommunityFeedRefreshBus` 通知社区重载。

### 关键路径

- `app/.../domain/community/CommunityFeedRefreshBus.kt`
- `app/.../ui/screens/community/CommunityFeedViewModel.kt`
- `app/.../ui/screens/community/CommunityFeedScreen.kt`
- `app/.../ui/screens/main/MainScreen.kt`
- `app/.../ui/screens/profile/ProfileViewModel.kt`

### 文档

- [community_posts.md](../2026-08-21/community_posts.md)
