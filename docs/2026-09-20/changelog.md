# Changelog 2026-09-20

## App 播放视频课报 not_found

### 做了什么

- 根因：学生读 `agency-videos` 的 Storage 策略 JOIN 了 `students`，未限定的 `name` 被解析成学生姓名，签名 URL 一律 404
- 改为 `private.student_can_read_agency_video_object(storage.objects.name)`；App 把 `not_found` 显示成可读错误

### 关键路径

- [`043_fix_video_storage_student_rls.sql`](../../supabase/migrations/043_fix_video_storage_student_rls.sql)

### 文档

- 修订 [video_lessons.md](./video_lessons.md)

---

## 机构视频课（MP4 播放 + 机构端上传）

### 做了什么

- 新增机构隔离的视频课：`agency_admin` 在 Web 上传渐进式 MP4，上架后本机构学生可在 App 学习中心观看
- 远端已建表 `video_lessons` 与私有 bucket `agency-videos`（仅 `video/mp4`，200MB）
- 观看不发代币、不种花园；作业布置留到后续

### 关键路径

- [`042_agency_video_lessons.sql`](../../supabase/migrations/042_agency_video_lessons.sql)
- [`web/src/app/agency/videos/`](../../web/src/app/agency/videos/)
- [`VideoLessonListScreen.kt`](../../app/src/main/java/com/example/seedie/ui/screens/learning/video/VideoLessonListScreen.kt) / [`VideoLessonPlayerScreen.kt`](../../app/src/main/java/com/example/seedie/ui/screens/learning/video/VideoLessonPlayerScreen.kt)

### 文档

- 新增 [video_lessons.md](./video_lessons.md)
- 修订 [permission_model.md](../2026-07-24/permission_model.md)、[supabase_table_map.md](../2026-07-24/supabase_table_map.md)、[teacher_setup.md](../2026-06-28/teacher_setup.md) §5.1
