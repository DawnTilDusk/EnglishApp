"use client";

import { useActionState, useEffect, useRef } from "react";
import type { TeacherRow } from "@/lib/types";
import { initialActionState } from "@/lib/action-state";
import { createStudentAction } from "./actions";

export function CreateStudentForm({
  teachers,
}: {
  teachers: Pick<TeacherRow, "id" | "display_name">[];
}) {
  const [state, formAction, pending] = useActionState(
    createStudentAction,
    initialActionState
  );
  const formRef = useRef<HTMLFormElement>(null);

  useEffect(() => {
    if (state.ok) {
      formRef.current?.reset();
    }
  }, [state]);

  return (
    <form ref={formRef} className="stack" action={formAction} autoComplete="off">
      {state.error && <p className="error">{state.error}</p>}
      <label>
        邮箱
        <input
          name="student_email"
          type="email"
          required
          autoComplete="off"
        />
      </label>
      <label>
        密码
        <input
          name="password"
          type="password"
          required
          minLength={6}
          autoComplete="new-password"
        />
      </label>
      <label>
        姓名
        <input name="student_name" type="text" required />
      </label>
      <div className="row">
        <label style={{ flex: 1 }}>
          学号
          <input name="student_no" type="text" />
        </label>
        <label style={{ flex: 1 }}>
          班级
          <input name="class_id" type="text" />
        </label>
      </div>
      <label>
        绑定教师
        <select name="teacher_id" required defaultValue="">
          <option value="" disabled>
            选择教师
          </option>
          {teachers.map((t) => (
            <option key={t.id} value={t.id}>
              {t.display_name}
            </option>
          ))}
        </select>
      </label>
      {teachers.length === 0 && (
        <p className="muted">请先在「教师」页创建至少一名教师。</p>
      )}
      <button
        className="btn"
        type="submit"
        disabled={pending || teachers.length === 0}
      >
        {pending ? "创建中…" : "创建学生"}
      </button>
    </form>
  );
}
