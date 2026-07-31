"use client";

import { useActionState, useMemo, useState } from "react";
import { initialActionState } from "@/lib/action-state";
import { createPracticeAssignmentAction } from "./actions";

type CatalogItem = {
  id: string;
  title: string;
  subtitle?: string | null;
};

type StudentItem = {
  id: string;
  name: string;
  student_no: string | null;
  class_id: string | null;
};

type ModuleId = "reading" | "listening" | "writing";

export function CreateAssignmentForm({
  readingItems,
  listeningItems,
  writingItems,
  students,
}: {
  readingItems: CatalogItem[];
  listeningItems: CatalogItem[];
  writingItems: CatalogItem[];
  students: StudentItem[];
}) {
  const [state, formAction, pending] = useActionState(
    createPracticeAssignmentAction,
    initialActionState
  );
  const [moduleId, setModuleId] = useState<ModuleId>("reading");
  const [classFilter, setClassFilter] = useState("");
  const [selectedStudents, setSelectedStudents] = useState<Set<string>>(
    () => new Set(students.map((s) => s.id))
  );

  const catalog =
    moduleId === "reading"
      ? readingItems
      : moduleId === "listening"
        ? listeningItems
        : writingItems;

  const classOptions = useMemo(() => {
    const set = new Set<string>();
    for (const s of students) {
      if (s.class_id) set.add(s.class_id);
    }
    return Array.from(set).sort();
  }, [students]);

  const filteredStudents = useMemo(() => {
    if (!classFilter) return students;
    return students.filter((s) => (s.class_id ?? "") === classFilter);
  }, [students, classFilter]);

  function toggleStudent(id: string) {
    setSelectedStudents((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  }

  function selectAllFiltered() {
    setSelectedStudents((prev) => {
      const next = new Set(prev);
      for (const s of filteredStudents) next.add(s.id);
      return next;
    });
  }

  function clearFiltered() {
    setSelectedStudents((prev) => {
      const next = new Set(prev);
      for (const s of filteredStudents) next.delete(s.id);
      return next;
    });
  }

  const defaultDue = useMemo(() => {
    const d = new Date();
    d.setDate(d.getDate() + 1);
    d.setMinutes(0, 0, 0);
    const pad = (n: number) => String(n).padStart(2, "0");
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
  }, []);

  return (
    <form className="stack" action={formAction}>
      {state.error && <p className="error">{state.error}</p>}

      <label>
        模块
        <select
          name="module_id"
          value={moduleId}
          onChange={(e) => {
            const v = e.target.value;
            if (v === "listening" || v === "writing" || v === "reading") {
              setModuleId(v);
            }
          }}
        >
          <option value="reading">阅读训练</option>
          <option value="listening">听力训练</option>
          <option value="writing">写作训练</option>
        </select>
      </label>

      <label>
        标题
        <input
          name="title"
          type="text"
          required
          placeholder={
            moduleId === "writing"
              ? "例如：作文练习 7/31"
              : "例如：阅读练习 7/31"
          }
        />
      </label>

      <label>
        截止时间
        <input name="due_at" type="datetime-local" required defaultValue={defaultDue} />
      </label>

      <label className="row" style={{ gap: 8 }}>
        <input name="allow_late" type="checkbox" />
        允许截止后补交
      </label>

      <fieldset className="stack" style={{ border: "1px solid var(--line)", borderRadius: 12, padding: 16 }}>
        <legend>{moduleId === "writing" ? "选题（作文题目）" : "选题（按套）"}</legend>
        {catalog.length === 0 && <p className="muted">题库为空。</p>}
        {catalog.map((item) => (
          <label key={item.id} className="row" style={{ gap: 8 }}>
            <input
              type={moduleId === "writing" ? "radio" : "checkbox"}
              name="item_ref"
              value={item.id}
              required={moduleId === "writing"}
            />
            <span>
              {item.title}
              {item.subtitle ? (
                <span className="muted"> · {item.subtitle}</span>
              ) : null}
            </span>
          </label>
        ))}
      </fieldset>

      <fieldset className="stack" style={{ border: "1px solid var(--line)", borderRadius: 12, padding: 16 }}>
        <legend>学员</legend>
        <div className="row">
          <label style={{ flex: 1 }}>
            班级筛选
            <select
              value={classFilter}
              onChange={(e) => setClassFilter(e.target.value)}
            >
              <option value="">全部</option>
              {classOptions.map((c) => (
                <option key={c} value={c}>
                  {c}
                </option>
              ))}
            </select>
          </label>
          <button className="btn secondary" type="button" onClick={selectAllFiltered}>
            全选
          </button>
          <button className="btn secondary" type="button" onClick={clearFiltered}>
            取消全选
          </button>
        </div>
        {filteredStudents.map((s) => (
          <label key={s.id} className="row" style={{ gap: 8 }}>
            <input
              type="checkbox"
              checked={selectedStudents.has(s.id)}
              onChange={() => toggleStudent(s.id)}
            />
            <span>
              {s.name}
              {s.student_no ? ` · ${s.student_no}` : ""}
              {s.class_id ? (
                <span className="muted"> · {s.class_id}</span>
              ) : null}
            </span>
          </label>
        ))}
        {Array.from(selectedStudents).map((id) => (
          <input key={`hidden-${id}`} type="hidden" name="student_id" value={id} />
        ))}
        {filteredStudents.length === 0 && (
          <p className="muted">当前筛选下没有学员。</p>
        )}
        <p className="muted">已选 {selectedStudents.size} 人</p>
      </fieldset>

      <button className="btn" type="submit" disabled={pending || students.length === 0}>
        {pending ? "布置中…" : "布置作业"}
      </button>
    </form>
  );
}
