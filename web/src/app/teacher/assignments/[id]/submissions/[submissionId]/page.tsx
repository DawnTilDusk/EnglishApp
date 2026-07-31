import { requireTeacher } from "@/lib/auth";
import { ConsoleShell } from "@/components/ConsoleShell";
import { createClient } from "@/lib/supabase/server";
import Link from "next/link";
import { notFound, redirect } from "next/navigation";
import {
  formatDueAt,
  isUuid,
  submissionStatusLabel,
  teacherTabs,
} from "@/lib/teacher-assignments";
import { GradeWritingForm } from "./GradeWritingForm";

function isPdfPath(path: string | null | undefined): boolean {
  return !!path && path.toLowerCase().endsWith(".pdf");
}

export default async function GradeWritingSubmissionPage({
  params,
}: {
  params: Promise<{ id: string; submissionId: string }>;
}) {
  const { id, submissionId } = await params;
  if (!isUuid(id) || !isUuid(submissionId)) {
    redirect("/teacher/assignments");
  }

  const profile = await requireTeacher();
  const supabase = await createClient();

  const { data: assignment } = await supabase
    .from("practice_assignments")
    .select("id, module_id, title, teacher_id")
    .eq("id", id)
    .eq("teacher_id", profile.id)
    .maybeSingle();

  if (!assignment || assignment.module_id !== "writing") notFound();

  const { data: submission } = await supabase
    .from("practice_assignment_submissions")
    .select(
      "id, student_id, status, submitted_at, original_path, annotated_path, score, max_score, feedback_text, returned_at"
    )
    .eq("id", submissionId)
    .eq("assignment_id", id)
    .maybeSingle();

  if (!submission) notFound();

  const { data: student } = await supabase
    .from("students")
    .select("id, name")
    .eq("id", submission.student_id as string)
    .maybeSingle();

  const { data: items } = await supabase
    .from("practice_assignment_items")
    .select("item_ref, sort_order")
    .eq("assignment_id", id)
    .order("sort_order")
    .limit(1);

  const promptId = (items?.[0]?.item_ref as string | undefined) ?? null;
  let maxScore = (submission.max_score as number | null) ?? 15;
  let promptTitle = promptId ?? "作文题目";
  let promptBody = "";
  if (promptId) {
    const { data: prompt } = await supabase
      .from("writing_prompts")
      .select("title, title_zh, prompt_text, prompt_text_zh, max_score")
      .eq("prompt_id", promptId)
      .maybeSingle();
    if (prompt) {
      promptTitle =
        (prompt.title_zh as string) || (prompt.title as string) || promptTitle;
      promptBody =
        (prompt.prompt_text_zh as string) ||
        (prompt.prompt_text as string) ||
        "";
      maxScore = (prompt.max_score as number) ?? maxScore;
    }
  }

  let originalSignedUrl: string | null = null;
  let annotatedSignedUrl: string | null = null;
  const originalPath = submission.original_path as string | null;
  const annotatedPath = submission.annotated_path as string | null;

  if (originalPath) {
    const { data } = await supabase.storage
      .from("writing-submissions")
      .createSignedUrl(originalPath, 3600);
    originalSignedUrl = data?.signedUrl ?? null;
  }
  if (annotatedPath) {
    const { data } = await supabase.storage
      .from("writing-submissions")
      .createSignedUrl(annotatedPath, 3600);
    annotatedSignedUrl = data?.signedUrl ?? null;
  }

  const readOnly = (submission.status as string) === "returned";

  return (
    <ConsoleShell
      profile={profile}
      title="教师控制台"
      tabs={teacherTabs("assignments")}
    >
      <div className="card stack">
        <div className="row" style={{ justifyContent: "space-between" }}>
          <h2 style={{ margin: 0 }}>
            批改 · {student?.name ?? (submission.student_id as string)}
          </h2>
          <Link className="btn secondary" href={`/teacher/assignments/${id}`}>
            返回作业
          </Link>
        </div>
        <p className="muted" style={{ margin: 0 }}>
          {assignment.title as string} ·{" "}
          {submissionStatusLabel(
            submission.status as string,
            "writing"
          )}
          {submission.submitted_at
            ? ` · 提交于 ${formatDueAt(submission.submitted_at as string)}`
            : ""}
        </p>

        <section className="stack">
          <h3 style={{ marginBottom: 0 }}>{promptTitle}</h3>
          {promptBody ? (
            <p className="muted" style={{ whiteSpace: "pre-wrap" }}>
              {promptBody}
            </p>
          ) : null}
        </section>

        <GradeWritingForm
          assignmentId={id}
          submissionId={submissionId}
          maxScore={maxScore}
          originalSignedUrl={originalSignedUrl}
          originalIsPdf={isPdfPath(originalPath)}
          readOnly={readOnly}
          existingScore={(submission.score as number | null) ?? null}
          existingFeedback={(submission.feedback_text as string | null) ?? null}
          annotatedSignedUrl={annotatedSignedUrl}
          annotatedIsPdf={isPdfPath(annotatedPath)}
        />
      </div>
    </ConsoleShell>
  );
}
