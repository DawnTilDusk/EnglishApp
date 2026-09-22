"use client";

import { useRouter } from "next/navigation";
import { useState, type FormEvent } from "react";
import { createClient } from "@/lib/supabase/client";

const VIDEO_BUCKET = "agency-videos";
const MAX_BYTES = 209715200;
const ACCEPT_MIME = "video/mp4";

function isAllowedMp4(file: File): boolean {
  const name = file.name.toLowerCase();
  if (!name.endsWith(".mp4")) return false;
  if (file.type && file.type !== ACCEPT_MIME) return false;
  return true;
}

function readDurationSeconds(file: File): Promise<number | null> {
  return new Promise((resolve) => {
    const url = URL.createObjectURL(file);
    const video = document.createElement("video");
    video.preload = "metadata";
    video.onloadedmetadata = () => {
      const duration = video.duration;
      URL.revokeObjectURL(url);
      resolve(Number.isFinite(duration) ? Math.round(duration) : null);
    };
    video.onerror = () => {
      URL.revokeObjectURL(url);
      resolve(null);
    };
    video.src = url;
  });
}

export function UploadVideoForm({ agencyId }: { agencyId: string }) {
  const router = useRouter();
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [file, setFile] = useState<File | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);
    const trimmed = title.trim();
    if (!trimmed) {
      setError("请填写标题。");
      return;
    }
    if (!file) {
      setError("请选择 MP4 文件。");
      return;
    }
    if (!isAllowedMp4(file)) {
      setError("仅支持渐进式 MP4（H.264 + AAC，扩展名 .mp4）。");
      return;
    }
    if (file.size > MAX_BYTES) {
      setError("文件不能超过 200MB。");
      return;
    }

    setPending(true);
    const supabase = createClient();
    const {
      data: { user },
    } = await supabase.auth.getUser();
    if (!user) {
      setPending(false);
      setError("Not authenticated");
      return;
    }

    const durationSeconds = await readDurationSeconds(file);
    const { data: inserted, error: insertError } = await supabase
      .from("video_lessons")
      .insert({
        agency_id: agencyId,
        title: trimmed,
        description: description.trim() || null,
        is_published: false,
        created_by: user.id,
      })
      .select("id")
      .single();

    if (insertError || !inserted) {
      setPending(false);
      setError(insertError?.message ?? "创建视频课失败。");
      return;
    }

    const storagePath = `${agencyId}/${inserted.id}/source.mp4`;
    const { error: uploadError } = await supabase.storage
      .from(VIDEO_BUCKET)
      .upload(storagePath, file, {
        contentType: ACCEPT_MIME,
        upsert: true,
      });

    if (uploadError) {
      setPending(false);
      setError(`文件上传失败：${uploadError.message}。草稿已保留，可删除后重试。`);
      router.refresh();
      return;
    }

    const { error: updateError } = await supabase
      .from("video_lessons")
      .update({
        storage_path: storagePath,
        duration_seconds: durationSeconds,
        updated_at: new Date().toISOString(),
      })
      .eq("id", inserted.id)
      .eq("agency_id", agencyId);

    setPending(false);
    if (updateError) {
      setError(updateError.message);
      router.refresh();
      return;
    }

    setTitle("");
    setDescription("");
    setFile(null);
    (event.target as HTMLFormElement).reset();
    router.refresh();
  }

  return (
    <form className="stack" onSubmit={onSubmit}>
      {error && <p className="error">{error}</p>}
      <label>
        标题
        <input
          name="title"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          required
        />
      </label>
      <label>
        描述（可选）
        <textarea
          name="description"
          rows={3}
          value={description}
          onChange={(e) => setDescription(e.target.value)}
        />
      </label>
      <label>
        MP4 文件（最大 200MB）
        <input
          name="file"
          type="file"
          accept=".mp4,video/mp4"
          required
          onChange={(e) => setFile(e.target.files?.[0] ?? null)}
        />
      </label>
      <p className="muted" style={{ margin: 0 }}>
        请上传 H.264 + AAC 的渐进式 MP4（建议 720p，moov 在文件头 / faststart）。上传后需手动上架，学生才会看到。
      </p>
      <button className="btn" type="submit" disabled={pending}>
        {pending ? "上传中…" : "上传"}
      </button>
    </form>
  );
}
