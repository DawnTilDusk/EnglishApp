"use client";

import { useActionState, useEffect, useRef } from "react";
import { initialActionState } from "@/lib/action-state";
import { createTeacherAction } from "./actions";

export function CreateTeacherForm() {
  const [state, formAction, pending] = useActionState(
    createTeacherAction,
    initialActionState
  );
  const formRef = useRef<HTMLFormElement>(null);

  useEffect(() => {
    if (state.ok) {
      formRef.current?.reset();
    }
  }, [state]);

  return (
    <form ref={formRef} className="stack" action={formAction}>
      {state.error && <p className="error">{state.error}</p>}
      <label>
        邮箱
        <input name="email" type="email" required />
      </label>
      <label>
        密码
        <input name="password" type="password" required minLength={6} />
      </label>
      <label>
        显示名
        <input name="display_name" type="text" />
      </label>
      <button className="btn" type="submit" disabled={pending}>
        {pending ? "创建中…" : "创建教师"}
      </button>
    </form>
  );
}
