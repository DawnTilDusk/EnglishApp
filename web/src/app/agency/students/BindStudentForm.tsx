"use client";

import { useActionState, useEffect, useRef } from "react";
import type { StudentRow, TeacherRow } from "@/lib/types";
import { initialActionState } from "@/lib/action-state";
import { bindStudentAction } from "./actions";

export function BindStudentForm({
  students,
  teachers,
}: {
  students: Pick<StudentRow, "id" | "name">[];
  teachers: Pick<TeacherRow, "id" | "display_name">[];
}) {
  const [state, formAction, pending] = useActionState(
    bindStudentAction,
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
        学生
        <select name="student_id" required defaultValue="">
          <option value="" disabled>
            选择学生
          </option>
          {students.map((s) => (
            <option key={s.id} value={s.id}>
              {s.name}
            </option>
          ))}
        </select>
      </label>
      <label>
        教师
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
      <button className="btn" type="submit" disabled={pending}>
        {pending ? "绑定中…" : "绑定"}
      </button>
    </form>
  );
}
