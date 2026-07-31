"use client";

import { useActionState, useState } from "react";
import { initialActionState } from "@/lib/action-state";
import { createClient } from "@/lib/supabase/client";
import { returnWritingAssignmentAction } from "../../../actions";
import { downloadFromUrl } from "./downloadFromUrl";
import { WritingAnnotator } from "./WritingAnnotator";

const ALLOWED_EXT = new Set(["jpg", "jpeg", "png", "webp", "pdf"]);

function extensionOf(file: File): string | null {
  const fromName = file.name.split(".").pop()?.toLowerCase();
  if (fromName && ALLOWED_EXT.has(fromName === "jpeg" ? "jpg" : fromName)) {
    return fromName === "jpeg" ? "jpg" : fromName;
  }
  if (file.type === "image/jpeg") return "jpg";
  if (file.type === "image/png") return "png";
  if (file.type === "image/webp") return "webp";
  if (file.type === "application/pdf") return "pdf";
  return null;
}

export function GradeWritingForm({
  assignmentId,
  submissionId,
  maxScore,
  originalSignedUrl,
  originalIsPdf,
  readOnly,
  existingScore,
  existingFeedback,
  annotatedSignedUrl,
  annotatedIsPdf,
}: {
  assignmentId: string;
  submissionId: string;
  maxScore: number;
  originalSignedUrl: string | null;
  originalIsPdf: boolean;
  readOnly: boolean;
  existingScore: number | null;
  existingFeedback: string | null;
  annotatedSignedUrl: string | null;
  annotatedIsPdf: boolean;
}) {
  const [state, formAction, pending] = useActionState(
    returnWritingAssignmentAction,
    initialActionState
  );
  const [annotatedPath, setAnnotatedPath] = useState("");
  const [uploadError, setUploadError] = useState<string | null>(null);
  const [uploading, setUploading] = useState(false);
  const [annotating, setAnnotating] = useState(false);
  const [downloadError, setDownloadError] = useState<string | null>(null);
  const [downloading, setDownloading] = useState(false);

  async function handleDownload(url: string, filename: string) {
    setDownloadError(null);
    setDownloading(true);
    try {
      await downloadFromUrl(url, filename);
    } catch (e) {
      setDownloadError(e instanceof Error ? e.message : "下载失败");
    } finally {
      setDownloading(false);
    }
  }

  async function uploadAnnotatedBlob(blob: Blob, ext: string, contentType: string) {
    const path = `${assignmentId}/${submissionId}/annotated.${ext}`;
    const supabase = createClient();
    const { error } = await supabase.storage
      .from("writing-submissions")
      .upload(path, blob, { upsert: true, contentType });
    if (error) throw new Error(error.message);
    setAnnotatedPath(path);
    return path;
  }

  async function onAnnotatorComplete(blob: Blob) {
    setUploadError(null);
    setUploading(true);
    try {
      // Annotator exports JPEG to stay under Storage 10MB limit
      await uploadAnnotatedBlob(blob, "jpg", "image/jpeg");
      setAnnotating(false);
    } catch (e) {
      setUploadError(e instanceof Error ? e.message : "上传失败");
      throw e;
    } finally {
      setUploading(false);
    }
  }

  async function onFileChange(file: File | null) {
    setUploadError(null);
    setAnnotatedPath("");
    if (!file) return;
    const ext = extensionOf(file);
    if (!ext) {
      setUploadError("仅支持 JPG / PNG / WebP / PDF。");
      return;
    }
    if (file.size > 10 * 1024 * 1024) {
      setUploadError("文件不能超过 10MB。");
      return;
    }
    setUploading(true);
    try {
      await uploadAnnotatedBlob(file, ext, file.type || "application/octet-stream");
    } catch (e) {
      setUploadError(e instanceof Error ? e.message : "上传失败");
    } finally {
      setUploading(false);
    }
  }

  if (annotating && originalSignedUrl && !originalIsPdf) {
    return (
      <div className="stack">
        <h3 style={{ marginBottom: 0 }}>页内批改</h3>
        <p className="muted" style={{ margin: 0 }}>
          在图片上圈画批注，完成后上传，再填写分数返还学生。
        </p>
        {uploadError && <p className="error">{uploadError}</p>}
        <WritingAnnotator
          imageUrl={originalSignedUrl}
          onCancel={() => setAnnotating(false)}
          onComplete={onAnnotatorComplete}
        />
      </div>
    );
  }

  return (
    <div className="stack">
      <section className="stack">
        <h3 style={{ marginBottom: 0 }}>学生原件</h3>
        {!originalSignedUrl && <p className="muted">暂无原件预览。</p>}
        {downloadError && <p className="error">{downloadError}</p>}

        {originalSignedUrl && originalIsPdf && (
          <div className="row" style={{ gap: 8, flexWrap: "wrap" }}>
            <a className="btn secondary" href={originalSignedUrl} target="_blank" rel="noreferrer">
              打开 PDF 原件
            </a>
            <button
              className="btn secondary"
              type="button"
              disabled={downloading}
              onClick={() =>
                void handleDownload(originalSignedUrl, `original-${submissionId}.pdf`)
              }
            >
              {downloading ? "下载中…" : "下载 PDF"}
            </button>
          </div>
        )}

        {originalSignedUrl && !originalIsPdf && (
          <>
            {/* eslint-disable-next-line @next/next/no-img-element */}
            <img
              src={originalSignedUrl}
              alt="学生提交原件"
              style={{ maxWidth: "100%", borderRadius: 8, border: "1px solid var(--line)" }}
            />
            <div className="row" style={{ gap: 8, flexWrap: "wrap" }}>
              <button
                className="btn secondary"
                type="button"
                disabled={downloading}
                onClick={() =>
                  void handleDownload(originalSignedUrl, `original-${submissionId}.jpg`)
                }
              >
                {downloading ? "下载中…" : "下载原件"}
              </button>
              {!readOnly ? (
                <button
                  className="btn"
                  type="button"
                  onClick={() => {
                    setUploadError(null);
                    setAnnotating(true);
                  }}
                >
                  开始批改
                </button>
              ) : null}
            </div>
          </>
        )}
      </section>

      {readOnly ? (
        <section className="stack">
          <h3 style={{ marginBottom: 0 }}>批改结果</h3>
          <p style={{ margin: 0 }}>
            分数：{existingScore ?? "—"} / {maxScore}
          </p>
          {existingFeedback ? (
            <p className="muted" style={{ whiteSpace: "pre-wrap" }}>
              {existingFeedback}
            </p>
          ) : null}
          {annotatedSignedUrl && annotatedIsPdf && (
            <div className="row" style={{ gap: 8, flexWrap: "wrap" }}>
              <a
                className="btn secondary"
                href={annotatedSignedUrl}
                target="_blank"
                rel="noreferrer"
              >
                打开批改 PDF
              </a>
              <button
                className="btn secondary"
                type="button"
                disabled={downloading}
                onClick={() =>
                  void handleDownload(annotatedSignedUrl, `annotated-${submissionId}.pdf`)
                }
              >
                下载批改件
              </button>
            </div>
          )}
          {annotatedSignedUrl && !annotatedIsPdf && (
            <>
              {/* eslint-disable-next-line @next/next/no-img-element */}
              <img
                src={annotatedSignedUrl}
                alt="教师批改件"
                style={{ maxWidth: "100%", borderRadius: 8, border: "1px solid var(--line)" }}
              />
              <button
                className="btn secondary"
                type="button"
                disabled={downloading}
                onClick={() =>
                  void handleDownload(annotatedSignedUrl, `annotated-${submissionId}.png`)
                }
              >
                下载批改件
              </button>
            </>
          )}
        </section>
      ) : (
        <form className="stack" action={formAction}>
          {state.error && <p className="error">{state.error}</p>}
          {uploadError && <p className="error">{uploadError}</p>}
          <input type="hidden" name="assignment_id" value={assignmentId} />
          <input type="hidden" name="submission_id" value={submissionId} />
          <input type="hidden" name="annotated_path" value={annotatedPath} />

          {!originalIsPdf ? (
            <p className="muted" style={{ margin: 0 }}>
              {annotatedPath
                ? "圈画已上传，填写分数后即可返还。"
                : "请先点击「开始批改」在图片上圈画并上传。"}
            </p>
          ) : (
            <label>
              上传批改后的文件（PDF 无法页内圈画）
              <input
                type="file"
                accept="image/jpeg,image/png,image/webp,application/pdf,.jpg,.jpeg,.png,.webp,.pdf"
                onChange={(e) => void onFileChange(e.target.files?.[0] ?? null)}
              />
            </label>
          )}

          {annotatedPath ? (
            <p className="muted" style={{ margin: 0 }}>
              已上传批改件：{annotatedPath}
            </p>
          ) : null}

          <label>
            分数（0–{maxScore}）
            <input
              name="score"
              type="number"
              min={0}
              max={maxScore}
              required
              defaultValue={Math.round(maxScore * 0.8)}
            />
          </label>

          <label>
            评语（可选）
            <textarea name="feedback_text" rows={4} placeholder="对学生的简要反馈" />
          </label>

          <button
            className="btn"
            type="submit"
            disabled={pending || uploading || !annotatedPath}
          >
            {uploading ? "上传中…" : pending ? "返还中…" : "打分并返还"}
          </button>
        </form>
      )}
    </div>
  );
}
