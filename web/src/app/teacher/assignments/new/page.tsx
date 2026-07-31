import { requireTeacher } from "@/lib/auth";
import { ConsoleShell } from "@/components/ConsoleShell";
import { createClient } from "@/lib/supabase/server";
import Link from "next/link";
import { teacherTabs } from "@/lib/teacher-assignments";
import { CreateAssignmentForm } from "../CreateAssignmentForm";

export default async function NewAssignmentPage() {
  const profile = await requireTeacher();
  const supabase = await createClient();

  const [
    { data: readingSets },
    { data: listeningMaterials },
    { data: writingPrompts },
    { data: students },
  ] = await Promise.all([
    supabase
      .from("reading_sets")
      .select("set_id, title, title_zh, topic")
      .order("sort_order"),
    supabase
      .from("listening_materials")
      .select("material_id, title, title_zh, material_type")
      .order("sort_order"),
    supabase
      .from("writing_prompts")
      .select("prompt_id, title, title_zh, topic, max_score")
      .order("sort_order"),
    supabase
      .from("students")
      .select("id, name, student_no, class_id")
      .eq("teacher_id", profile.id)
      .order("name"),
  ]);

  return (
    <ConsoleShell
      profile={profile}
      title="教师控制台"
      tabs={teacherTabs("assignments")}
    >
      <div className="card stack">
        <div className="row" style={{ justifyContent: "space-between" }}>
          <h2 style={{ margin: 0 }}>布置作业</h2>
          <Link className="btn secondary" href="/teacher/assignments">
            返回列表
          </Link>
        </div>
        <CreateAssignmentForm
          readingItems={(readingSets ?? []).map((s) => ({
            id: s.set_id as string,
            title: (s.title_zh as string) || (s.title as string),
            subtitle: s.topic as string | null,
          }))}
          listeningItems={(listeningMaterials ?? []).map((m) => ({
            id: m.material_id as string,
            title:
              (m.title_zh as string) ||
              (m.title as string) ||
              (m.material_id as string),
            subtitle: m.material_type as string | null,
          }))}
          writingItems={(writingPrompts ?? []).map((p) => ({
            id: p.prompt_id as string,
            title: (p.title_zh as string) || (p.title as string),
            subtitle: p.topic
              ? `${p.topic} · 满分 ${p.max_score as number}`
              : `满分 ${p.max_score as number}`,
          }))}
          students={(students ?? []).map((s) => ({
            id: s.id as string,
            name: s.name as string,
            student_no: (s.student_no as string | null) ?? null,
            class_id: (s.class_id as string | null) ?? null,
          }))}
        />
      </div>
    </ConsoleShell>
  );
}
