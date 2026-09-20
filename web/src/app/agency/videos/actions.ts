"use server";

import { createClient } from "@/lib/supabase/server";
import { revalidatePath } from "next/cache";
import type { ActionState } from "@/lib/action-state";

const VIDEO_BUCKET = "agency-videos";

async function requireAgencyContext() {
  const supabase = await createClient();
  const {
    data: { user },
  } = await supabase.auth.getUser();
  if (!user) return { error: "Not authenticated" as const, supabase, profile: null };

  const { data: profile } = await supabase
    .from("profiles")
    .select("agency_id, role")
    .eq("id", user.id)
    .single();

  if (profile?.role !== "agency_admin" || !profile.agency_id) {
    return { error: "仅机构管理员可管理视频课。" as const, supabase, profile: null };
  }

  return { error: null, supabase, profile: { ...profile, userId: user.id } };
}

export async function setVideoPublishedAction(
  _prev: ActionState,
  formData: FormData
): Promise<ActionState> {
  const id = String(formData.get("id") || "");
  const nextPublished = String(formData.get("is_published") || "") === "true";
  if (!id) return { error: "Missing video id", ok: false };

  const ctx = await requireAgencyContext();
  if (ctx.error || !ctx.profile) return { error: ctx.error ?? "Not authenticated", ok: false };

  const { data: lesson, error: loadError } = await ctx.supabase
    .from("video_lessons")
    .select("id, storage_path")
    .eq("id", id)
    .eq("agency_id", ctx.profile.agency_id)
    .maybeSingle();
  if (loadError) return { error: loadError.message, ok: false };
  if (!lesson) return { error: "视频课不存在。", ok: false };
  if (nextPublished && !lesson.storage_path) {
    return { error: "请先上传 MP4 再上架。", ok: false };
  }

  const { error } = await ctx.supabase
    .from("video_lessons")
    .update({ is_published: nextPublished, updated_at: new Date().toISOString() })
    .eq("id", id)
    .eq("agency_id", ctx.profile.agency_id);
  if (error) return { error: error.message, ok: false };

  revalidatePath("/agency/videos");
  return { error: null, ok: true };
}

export async function deleteVideoLessonAction(
  _prev: ActionState,
  formData: FormData
): Promise<ActionState> {
  const id = String(formData.get("id") || "");
  if (!id) return { error: "Missing video id", ok: false };

  const ctx = await requireAgencyContext();
  if (ctx.error || !ctx.profile) return { error: ctx.error ?? "Not authenticated", ok: false };

  const { data: lesson, error: loadError } = await ctx.supabase
    .from("video_lessons")
    .select("id, storage_path")
    .eq("id", id)
    .eq("agency_id", ctx.profile.agency_id)
    .maybeSingle();
  if (loadError) return { error: loadError.message, ok: false };
  if (!lesson) return { error: "视频课不存在。", ok: false };

  if (lesson.storage_path) {
    const { error: storageError } = await ctx.supabase.storage
      .from(VIDEO_BUCKET)
      .remove([lesson.storage_path]);
    if (storageError) return { error: storageError.message, ok: false };
  }

  const { error } = await ctx.supabase
    .from("video_lessons")
    .delete()
    .eq("id", id)
    .eq("agency_id", ctx.profile.agency_id);
  if (error) return { error: error.message, ok: false };

  revalidatePath("/agency/videos");
  return { error: null, ok: true };
}
