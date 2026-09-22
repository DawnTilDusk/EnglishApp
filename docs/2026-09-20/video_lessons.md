# 机构视频课

更新日期：2026-09-20

机构上传、本机构学生在 App 观看的 MP4 课程。MVP 不布置作业、不发代币、不种花园。

## 格式

仅 **渐进式 MP4**（容器 `.mp4`，MIME `video/mp4`）。

建议编码（上传方在本地转好，服务端不转码）：

- 视频：H.264，yuv420p，建议 720p
- 音频：AAC
- `faststart`（moov 在文件头），便于边下边播
- 单文件不超过 **200MB**

不支持：HLS / `.m3u8`、MOV / QuickTime、WebM、HEVC/AV1（MVP）。

## 角色

| 角色 | 能力 |
|------|------|
| `agency_admin` | Web `/agency/videos` 上传、预览、上架/下架、删除 |
| `teacher` | SELECT 本机构课（MVP 无管理界面） |
| `student` | App 学习中心「视频课」看**已上架**课 |

跨机构不可见。未上传完成的草稿学生不可见。

## 数据

- 表：`video_lessons`（`agency_id`、`title`、`storage_path`、`is_published` 等）
- Bucket：私有 `agency-videos`
- 对象路径：`{agency_id}/{lesson_id}/source.mp4`
- 播放：`createSignedUrl`（约 2 小时），过期进播放页会重签
- 学生 Storage 读策略必须用对象路径参数（`private.student_can_read_agency_video_object`），不能在 JOIN `students` 的子查询里写未限定的 `name`（会绑到学生姓名，签名 URL 返回 `not_found`）

## 客户端

- Web：`web/src/app/agency/videos/`
- App：`VideoLessonListScreen` / `VideoLessonPlayerScreen`（Media3 `PlayerView`）
- 迁移：`supabase/migrations/042_agency_video_lessons.sql`、`043_fix_video_storage_student_rls.sql`
