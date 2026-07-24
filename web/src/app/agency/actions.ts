"use server";

import { createClient } from "@/lib/supabase/server";
import { revalidatePath } from "next/cache";

export async function createTeacherAction(formData: FormData) {
  const email = String(formData.get("email") || "").trim();
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
    return { error: error.message };
  }
  revalidatePath("/agency");
  revalidatePath("/agency/teachers");
  return { error: null };
}

export async function bindStudentAction(formData: FormData) {
  const studentId = String(formData.get("student_id") || "");
  const teacherId = String(formData.get("teacher_id") || "");
  const supabase = await createClient();
  const { error } = await supabase.rpc("bind_student_to_teacher", {
    p_student_id: studentId,
    p_teacher_id: teacherId,
  });
  if (error) {
    return { error: error.message };
  }
  revalidatePath("/agency");
  revalidatePath("/agency/students");
  return { error: null };
}

export async function upsertProductAction(formData: FormData) {
  const id = String(formData.get("id") || "");
  const name = String(formData.get("name") || "").trim();
  const description = String(formData.get("description") || "").trim() || null;
  const priceTokens = Number(formData.get("price_tokens") || 0);
  const stock = Number(formData.get("stock") || -1);
  const isActive = formData.get("is_active") === "on";

  const supabase = await createClient();
  const {
    data: { user },
  } = await supabase.auth.getUser();
  if (!user) return { error: "Not authenticated" };

  const { data: profile } = await supabase
    .from("profiles")
    .select("agency_id")
    .eq("id", user.id)
    .single();

  if (!profile?.agency_id) return { error: "Missing agency_id" };

  if (id) {
    const { error } = await supabase
      .from("shop_products")
      .update({
        name,
        description,
        price_tokens: priceTokens,
        stock,
        is_active: isActive,
      })
      .eq("id", id);
    if (error) return { error: error.message };
  } else {
    const { error } = await supabase.from("shop_products").insert({
      agency_id: profile.agency_id,
      name,
      description,
      price_tokens: priceTokens,
      stock,
      is_active: isActive,
    });
    if (error) return { error: error.message };
  }

  revalidatePath("/agency/shop");
  return { error: null };
}
