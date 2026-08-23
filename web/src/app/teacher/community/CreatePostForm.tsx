"use client";

import { useActionState, useEffect, useRef } from "react";
import { initialActionState } from "@/lib/action-state";
import { PROFILE_GRADE_OPTIONS } from "@/lib/profile-grades";
import { createCommunityPostAction } from "./actions";

export function CreatePostForm() {
  const [state, formAction, pending] = useActionState(
    createCommunityPostAction,
    initialActionState
  );
  const formRef = useRef<HTMLFormElement>(null);

  useEffect(() => {
    if (state.ok) {
      formRef.current?.reset();
    }
  }, [state.ok]);

  return (
    <form ref={formRef} className="stack" action={formAction}>
      <h3 style={{ margin: 0 }}>发布教师帖</h3>
      <p className="muted" style={{ margin: 0 }}>
        选择可见年级后，同机构对应年级的学生可在 App 社区看到。
      </p>
      {state.error && <p className="error">{state.error}</p>}
      {state.ok && !state.error && (
        <p style={{ margin: 0, color: "var(--accent)" }}>已发布。</p>
      )}
      <label>
        标题（可选）
        <input name="title" type="text" maxLength={80} placeholder="简短标题" />
      </label>
      <label>
        正文
        <textarea
          name="body"
          required
          rows={4}
          maxLength={2000}
          placeholder="写给年级同学的公告或分享…"
        />
      </label>
      <fieldset
        className="stack"
        style={{ border: "1px solid var(--line)", borderRadius: 12, padding: 16 }}
      >
        <legend>可见年级</legend>
        <div className="row" style={{ flexWrap: "wrap", gap: 8 }}>
          {PROFILE_GRADE_OPTIONS.map((g) => (
            <label key={g} className="row" style={{ gap: 6 }}>
              <input type="checkbox" name="target_grade" value={g} />
              <span>{g}</span>
            </label>
          ))}
        </div>
      </fieldset>
      <button className="btn" type="submit" disabled={pending}>
        {pending ? "发布中…" : "发布"}
      </button>
    </form>
  );
}
