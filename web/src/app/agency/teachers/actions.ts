"use server";

import { createClient } from "@/lib/supabase/server";
import { revalidatePath } from "next/cache";
import { mapCreateAccountError, type ActionState } from "@/lib/action-state";

export async function createTeacherAction(
  _prev: ActionState,
  formData: FormData
): Promise<ActionState> {
  const email = String(
    formData.get("teacher_email") || formData.get("email") || ""
  ).trim();
  const password = String(formData.get("password") || "");
  const displayName = String(formData.get("display_name") || "").trim() || null;
  const supabase = await createClient();
  const { error } = await supabase.rpc("create_teacher_account", {
    p_email: email,
    p_password: password,
    p_display_name: displayName,
    p_agency_id: null,
  });
  if (error) {
    return { error: mapCreateAccountError(error.message), ok: false };
  }
  revalidatePath("/agency");
  revalidatePath("/agency/teachers");
  return { error: null, ok: true };
}
