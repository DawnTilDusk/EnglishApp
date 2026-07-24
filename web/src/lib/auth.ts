import { createClient } from "@/lib/supabase/server";
import type { Profile } from "@/lib/types";
import { redirect } from "next/navigation";

export async function requireProfile(): Promise<Profile> {
  const supabase = await createClient();
  const {
    data: { user },
  } = await supabase.auth.getUser();

  if (!user) {
    redirect("/login");
  }

  const { data: profile, error } = await supabase
    .from("profiles")
    .select("id, role, agency_id, display_name, email")
    .eq("id", user.id)
    .single();

  if (error || !profile) {
    redirect("/login?error=profile");
  }

  if (profile.role === "student") {
    redirect("/login?error=student_use_app");
  }

  return profile as Profile;
}

export async function requireAgencyAdmin(): Promise<Profile> {
  const profile = await requireProfile();
  if (profile.role !== "agency_admin") {
    redirect("/teacher");
  }
  return profile;
}

export async function requireTeacher(): Promise<Profile> {
  const profile = await requireProfile();
  if (profile.role !== "teacher") {
    redirect("/agency");
  }
  return profile;
}
