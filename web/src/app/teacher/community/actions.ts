"use server";

import { createClient } from "@/lib/supabase/server";
import { revalidatePath } from "next/cache";
import type { ActionState } from "@/lib/action-state";
import { PROFILE_GRADE_OPTIONS } from "@/lib/profile-grades";

const ALLOWED = new Set<string>(PROFILE_GRADE_OPTIONS);

export async function createCommunityPostAction(
  _prev: ActionState,
  formData: FormData
): Promise<ActionState> {
  const title = String(formData.get("title") || "").trim();
  const body = String(formData.get("body") || "").trim();
  const grades = formData
    .getAll("target_grade")
    .map((v) => String(v).trim())
    .filter((g) => ALLOWED.has(g));

  if (!body) {
    return { error: "请填写正文。", ok: false };
  }
  if (body.length > 2000) {
    return { error: "正文不能超过 2000 字。", ok: false };
  }
  if (title.length > 80) {
    return { error: "标题不能超过 80 字。", ok: false };
  }
  if (grades.length === 0) {
    return { error: "请至少选择一个可见年级。", ok: false };
  }

  const supabase = await createClient();
  const {
    data: { user },
  } = await supabase.auth.getUser();
  if (!user) {
    return { error: "Not authenticated", ok: false };
  }

  const { error } = await supabase.rpc("create_community_post", {
    p_title: title || null,
    p_body: body,
    p_target_grades: grades,
  });

  if (error) {
    return { error: error.message, ok: false };
  }

  revalidatePath("/teacher/community");
  return { error: null, ok: true };
}

export async function setCommunityPostFeaturedAction(
  _prev: ActionState,
  formData: FormData
): Promise<ActionState> {
  const postId = String(formData.get("post_id") || "").trim();
  const featured = formData.get("featured") === "true";

  if (!postId) {
    return { error: "缺少帖子 ID。", ok: false };
  }

  const supabase = await createClient();
  const {
    data: { user },
  } = await supabase.auth.getUser();
  if (!user) {
    return { error: "Not authenticated", ok: false };
  }

  const { error } = await supabase.rpc("set_community_post_featured", {
    p_post_id: postId,
    p_featured: featured,
  });

  if (error) {
    return { error: error.message, ok: false };
  }

  revalidatePath("/teacher/community");
  return { error: null, ok: true };
}

export async function deleteCommunityPostAction(
  _prev: ActionState,
  formData: FormData
): Promise<ActionState> {
  const postId = String(formData.get("post_id") || "").trim();
  if (!postId) {
    return { error: "缺少帖子 ID。", ok: false };
  }

  const supabase = await createClient();
  const {
    data: { user },
  } = await supabase.auth.getUser();
  if (!user) {
    return { error: "Not authenticated", ok: false };
  }

  const { error } = await supabase.rpc("delete_community_post", {
    p_post_id: postId,
  });

  if (error) {
    return { error: error.message, ok: false };
  }

  revalidatePath("/teacher/community");
  return { error: null, ok: true };
}
