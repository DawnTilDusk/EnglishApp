"use client";

import { useActionState } from "react";
import { initialActionState } from "@/lib/action-state";
import { setVideoPublishedAction } from "./actions";

export function TogglePublishedButton({
  lessonId,
  isPublished,
  canPublish,
}: {
  lessonId: string;
  isPublished: boolean;
  canPublish: boolean;
}) {
  const [state, formAction, pending] = useActionState(
    setVideoPublishedAction,
    initialActionState
  );
  const disabled = pending || (!isPublished && !canPublish);

  return (
    <form action={formAction} className="stack" style={{ gap: 4 }}>
      <input type="hidden" name="id" value={lessonId} />
      <input
        type="hidden"
        name="is_published"
        value={isPublished ? "false" : "true"}
      />
      <button className="btn secondary" type="submit" disabled={disabled}>
        {pending ? "…" : isPublished ? "下架" : "上架"}
      </button>
      {state.error && <span className="error">{state.error}</span>}
      {!isPublished && !canPublish && (
        <span className="muted">上传完成后才能上架</span>
      )}
    </form>
  );
}
