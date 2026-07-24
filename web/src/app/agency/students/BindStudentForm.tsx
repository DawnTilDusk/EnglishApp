"use client";

import { useState, useTransition } from "react";
import type { StudentRow, TeacherRow } from "@/lib/types";

type ActionResult = { error: string | null };

export function BindStudentForm({
  action,
  students,
  teachers,
}: {
  action: (formData: FormData) => Promise<ActionResult>;
  students: Pick<StudentRow, "id" | "name">[];
  teachers: Pick<TeacherRow, "id" | "display_name">[];
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
        });
      }}
    >
      {error && <p className="error">{error}</p>}
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
