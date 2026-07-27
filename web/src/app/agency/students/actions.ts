"use server";

import { createClient } from "@/lib/supabase/server";
import { revalidatePath } from "next/cache";
import type { ActionState } from "@/lib/action-state";

export async function createStudentAction(
  _prev: ActionState,
  formData: FormData
): Promise<ActionState> {
  const email = String(formData.get("email") || "").trim();
  const password = String(formData.get("password") || "");
  const studentName = String(formData.get("student_name") || "").trim();
  const studentNo = String(formData.get("student_no") || "").trim() || null;
  const classId = String(formData.get("class_id") || "").trim() || null;
  const teacherId = String(formData.get("teacher_id") || "").trim();

  if (!email || !password || !studentName || !teacherId) {
    return { error: "请填写邮箱、密码、姓名并选择教师。", ok: false };
  }

  const supabase = await createClient();
  const {
    data: { user },
  } = await supabase.auth.getUser();
  if (!user) return { error: "Not authenticated", ok: false };

  const { data: profile } = await supabase
    .from("profiles")
    .select("agency_id, role")
    .eq("id", user.id)
    .single();

  if (profile?.role !== "agency_admin" || !profile.agency_id) {
    return { error: "仅机构管理员可创建学生。", ok: false };
  }

  const { error } = await supabase.rpc("create_student_account", {
    p_email: email,
    p_password: password,
    p_agency_id: profile.agency_id,
    p_student_name: studentName,
    p_student_no: studentNo,
    p_class_id: classId,
    p_teacher_id: teacherId,
  });

  if (error) {
    return { error: error.message, ok: false };
  }

  revalidatePath("/agency");
  revalidatePath("/agency/students");
  return { error: null, ok: true };
}

export async function bindStudentAction(
  _prev: ActionState,
  formData: FormData
): Promise<ActionState> {
  const studentId = String(formData.get("student_id") || "");
  const teacherId = String(formData.get("teacher_id") || "");
  const supabase = await createClient();
  const { error } = await supabase.rpc("bind_student_to_teacher", {
    p_student_id: studentId,
    p_teacher_id: teacherId,
  });
  if (error) {
    return { error: error.message, ok: false };
  }
  revalidatePath("/agency");
  revalidatePath("/agency/students");
  return { error: null, ok: true };
}
