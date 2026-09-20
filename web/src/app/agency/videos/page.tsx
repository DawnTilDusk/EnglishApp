import { requireAgencyAdmin } from "@/lib/auth";
import { ConsoleShell } from "@/components/ConsoleShell";
import { createClient } from "@/lib/supabase/server";
import { agencyTabs } from "@/lib/agency-tabs";
import type { VideoLesson } from "@/lib/video-lessons";
import { DeleteVideoButton } from "./DeleteVideoButton";
import { PreviewVideoButton } from "./PreviewVideoButton";
import { TogglePublishedButton } from "./TogglePublishedButton";
import { UploadVideoForm } from "./UploadVideoForm";

function formatDuration(seconds: number | null): string {
  if (seconds == null || seconds < 0) return "—";
  const m = Math.floor(seconds / 60);
  const s = seconds % 60;
  return `${m}:${s.toString().padStart(2, "0")}`;
}

export default async function AgencyVideosPage() {
  const profile = await requireAgencyAdmin();
  const supabase = await createClient();
  const { data } = await supabase
    .from("video_lessons")
    .select(
      "id, agency_id, title, description, storage_path, duration_seconds, sort_order, is_published, created_at, updated_at"
    )
    .eq("agency_id", profile.agency_id!)
    .order("sort_order", { ascending: true })
    .order("created_at", { ascending: false });

  const list = (data ?? []) as VideoLesson[];

  return (
    <ConsoleShell
      profile={profile}
      title="视频课"
      tabs={agencyTabs("videos")}
    >
      <div className="stack">
        <div className="card">
          <h2 style={{ marginTop: 0 }}>上传视频课</h2>
          <p className="muted" style={{ marginTop: 0 }}>
            仅本机构学生可在 App 学习中心观看已上架的课。格式：渐进式 MP4（H.264 + AAC）。
          </p>
          <UploadVideoForm agencyId={profile.agency_id!} />
        </div>

        <div className="card stack">
          <h2 style={{ margin: 0 }}>本机构视频</h2>
          <table>
            <thead>
              <tr>
                <th>标题</th>
                <th>时长</th>
                <th>状态</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              {list.map((lesson) => (
                <tr key={lesson.id}>
                  <td>
                    <div>{lesson.title}</div>
                    {lesson.description && (
                      <div className="muted">{lesson.description}</div>
                    )}
                    {!lesson.storage_path && (
                      <div className="error">文件未上传完成</div>
                    )}
                  </td>
                  <td>{formatDuration(lesson.duration_seconds)}</td>
                  <td>{lesson.is_published ? "已上架" : "草稿"}</td>
                  <td>
                    <div className="row" style={{ gap: 8, alignItems: "flex-start" }}>
                      {lesson.storage_path && (
                        <PreviewVideoButton
                          storagePath={lesson.storage_path}
                          title={lesson.title}
                        />
                      )}
                      <TogglePublishedButton
                        lessonId={lesson.id}
                        isPublished={lesson.is_published}
                        canPublish={Boolean(lesson.storage_path)}
                      />
                      <DeleteVideoButton lessonId={lesson.id} />
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {list.length === 0 && (
            <p className="muted">还没有视频课，请先上传 MP4。</p>
          )}
        </div>
      </div>
    </ConsoleShell>
  );
}
