"use client";

import { useActionState } from "react";
import { initialActionState } from "@/lib/action-state";
import { deleteVideoLessonAction } from "./actions";

export function DeleteVideoButton({ lessonId }: { lessonId: string }) {
  const [state, formAction, pending] = useActionState(
    deleteVideoLessonAction,
    initialActionState
  );

  return (
    <form
      action={formAction}
      className="stack"
      style={{ gap: 4 }}
      onSubmit={(event) => {
        if (!window.confirm("确定删除这节视频课？文件也会一并删除。")) {
          event.preventDefault();
        }
      }}
    >
      <input type="hidden" name="id" value={lessonId} />
      <button className="btn secondary" type="submit" disabled={pending}>
        {pending ? "删除中…" : "删除"}
      </button>
      {state.error && <span className="error">{state.error}</span>}
    </form>
  );
}
