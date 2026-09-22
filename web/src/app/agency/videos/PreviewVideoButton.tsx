"use client";

import { useState } from "react";
import { createClient } from "@/lib/supabase/client";

const VIDEO_BUCKET = "agency-videos";

export function PreviewVideoButton({
  storagePath,
  title,
}: {
  storagePath: string;
  title: string;
}) {
  const [open, setOpen] = useState(false);
  const [url, setUrl] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  async function openPreview() {
    setError(null);
    setOpen(true);
    if (url) return;
    setLoading(true);
    const supabase = createClient();
    const { data, error: signedError } = await supabase.storage
      .from(VIDEO_BUCKET)
      .createSignedUrl(storagePath, 3600);
    setLoading(false);
    if (signedError || !data?.signedUrl) {
      setError(signedError?.message ?? "无法生成预览链接。");
      return;
    }
    setUrl(data.signedUrl);
  }

  return (
    <div className="stack" style={{ gap: 8 }}>
      <button className="btn secondary" type="button" onClick={openPreview}>
        预览
      </button>
      {open && (
        <div className="stack" style={{ gap: 8 }}>
          {loading && <span className="muted">加载预览…</span>}
          {error && <span className="error">{error}</span>}
          {url && (
            <video
              controls
              playsInline
              src={url}
              style={{ width: "100%", maxWidth: 560, borderRadius: 12 }}
            >
              你的浏览器不支持播放 {title}
            </video>
          )}
        </div>
      )}
    </div>
  );
}
