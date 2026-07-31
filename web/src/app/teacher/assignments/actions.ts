"use server";

import { createClient } from "@/lib/supabase/server";
import { revalidatePath } from "next/cache";
import { redirect } from "next/navigation";
import type { ActionState } from "@/lib/action-state";
import { parseAssignmentIdFromRpc } from "@/lib/teacher-assignments";

export async function createPracticeAssignmentAction(
  _prev: ActionState,
  formData: FormData
): Promise<ActionState> {
  const moduleId = String(formData.get("module_id") || "").trim();
  const title = String(formData.get("title") || "").trim();
  const dueAtLocal = String(formData.get("due_at") || "").trim();
  const allowLate = formData.get("allow_late") === "on";
  const itemRefs = formData
    .getAll("item_ref")
    .map((v) => String(v).trim())
    .filter(Boolean);
  const studentIds = formData
    .getAll("student_id")
    .map((v) => String(v).trim())
    .filter(Boolean);

  if (
    moduleId !== "reading" &&
    moduleId !== "listening" &&
    moduleId !== "writing"
  ) {
    return { error: "请选择阅读、听力或写作模块。", ok: false };
  }
  if (!title) {
    return { error: "请填写作业标题。", ok: false };
  }
  if (!dueAtLocal) {
    return { error: "请设置截止时间。", ok: false };
  }
  if (itemRefs.length === 0) {
    return { error: "请至少选择一套题目。", ok: false };
  }
  if (studentIds.length === 0) {
    return { error: "请至少选择一名学员。", ok: false };
  }

  // datetime-local is local wall time without offset; treat as local → ISO
  const dueAt = new Date(dueAtLocal);
  if (Number.isNaN(dueAt.getTime())) {
    return { error: "截止时间格式无效。", ok: false };
  }

  const supabase = await createClient();
  const {
    data: { user },
  } = await supabase.auth.getUser();
  if (!user) {
    return { error: "Not authenticated", ok: false };
  }

  const { data, error } = await supabase.rpc("create_practice_assignment", {
    p_module_id: moduleId,
    p_title: title,
    p_due_at: dueAt.toISOString(),
    p_item_refs: itemRefs,
    p_student_ids: studentIds,
    p_allow_late: allowLate,
  });

  if (error) {
    return { error: error.message, ok: false };
  }

  const assignmentId = parseAssignmentIdFromRpc(data);
  if (!assignmentId) {
    console.error("create_practice_assignment: unexpected rpc payload", {
      typeofData: typeof data,
      data,
    });
  }

  revalidatePath("/teacher/assignments");

  if (assignmentId) {
    const { data: row, error: readError } = await supabase
      .from("practice_assignments")
      .select("id")
      .eq("id", assignmentId)
      .eq("teacher_id", user.id)
      .maybeSingle();

    if (row?.id) {
      redirect(`/teacher/assignments/${row.id}`);
    }
    console.error("create_practice_assignment: post-write select missed", {
      assignmentId,
      teacherId: user.id,
      readError: readError?.message ?? null,
    });
  }

  redirect("/teacher/assignments?created=1");
}

export async function returnWritingAssignmentAction(
  _prev: ActionState,
  formData: FormData
): Promise<ActionState> {
  const submissionId = String(formData.get("submission_id") || "").trim();
  const assignmentId = String(formData.get("assignment_id") || "").trim();
  const annotatedPath = String(formData.get("annotated_path") || "").trim();
  const scoreRaw = String(formData.get("score") || "").trim();
  const feedbackText = String(formData.get("feedback_text") || "").trim();

  if (!submissionId || !assignmentId) {
    return { error: "缺少提交信息。", ok: false };
  }
  if (!annotatedPath) {
    return { error: "请先上传批改后的图片或 PDF。", ok: false };
  }
  const score = Number.parseInt(scoreRaw, 10);
  if (!Number.isFinite(score)) {
    return { error: "请填写有效分数。", ok: false };
  }

  const supabase = await createClient();
  const {
    data: { user },
  } = await supabase.auth.getUser();
  if (!user) {
    return { error: "Not authenticated", ok: false };
  }

  const { error } = await supabase.rpc("return_writing_assignment", {
    p_submission_id: submissionId,
    p_annotated_path: annotatedPath,
    p_score: score,
    p_feedback_text: feedbackText || null,
  });

  if (error) {
    return { error: error.message, ok: false };
  }

  revalidatePath(`/teacher/assignments/${assignmentId}`);
  revalidatePath("/teacher/assignments");
  redirect(`/teacher/assignments/${assignmentId}`);
}
