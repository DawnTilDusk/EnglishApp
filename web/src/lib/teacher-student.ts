import { requireTeacher } from "@/lib/auth";
import { createClient } from "@/lib/supabase/server";
import type { Profile, StudentRow } from "@/lib/types";
import { notFound } from "next/navigation";

export type AssignedStudent = Pick<
  StudentRow,
  "id" | "name" | "student_no" | "class_id" | "teacher_id"
>;

export async function requireAssignedStudent(studentId: string): Promise<{
  profile: Profile;
  student: AssignedStudent;
  supabase: Awaited<ReturnType<typeof createClient>>;
}> {
  const profile = await requireTeacher();
  const supabase = await createClient();

  const { data: student } = await supabase
    .from("students")
    .select("id, name, student_no, class_id, teacher_id")
    .eq("id", studentId)
    .eq("teacher_id", profile.id)
    .maybeSingle();

  if (!student) {
    notFound();
  }

  return {
    profile,
    student: student as AssignedStudent,
    supabase,
  };
}
