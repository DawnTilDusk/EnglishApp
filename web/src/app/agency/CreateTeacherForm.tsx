"use client";

import { useState, useTransition } from "react";

type ActionResult = { error: string | null };

export function CreateTeacherForm({
  action,
}: {
  action: (formData: FormData) => Promise<ActionResult>;
}) {
  const [error, setError] = useState<string | null>(null);
  const [pending, startTransition] = useTransition();

  return (
    <form
      className="stack"
      onSubmit={(e) => {
        e.preventDefault();
        const formData = new FormData(e.currentTarget);
        startTransition(async () => {
          const result = await action(formData);
          setError(result.error);
          if (!result.error) {
            e.currentTarget.reset();
          }
        });
      }}
    >
      {error && <p className="error">{error}</p>}
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
