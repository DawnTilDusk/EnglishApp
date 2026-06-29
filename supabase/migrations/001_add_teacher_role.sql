-- Teacher role, teachers table, and student-teacher binding
CREATE EXTENSION IF NOT EXISTS pgcrypto;

ALTER TYPE public.user_role ADD VALUE IF NOT EXISTS 'teacher';

CREATE TABLE IF NOT EXISTS public.teachers (
    id UUID PRIMARY KEY REFERENCES public.profiles(id) ON DELETE CASCADE,
    display_name TEXT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

ALTER TABLE public.students
    ADD COLUMN IF NOT EXISTS teacher_id UUID REFERENCES public.teachers(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS students_teacher_id_idx ON public.students (teacher_id);

ALTER TABLE public.teachers ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Teachers view own record" ON public.teachers;
CREATE POLICY "Teachers view own record" ON public.teachers
    FOR SELECT USING (id = auth.uid());

DROP POLICY IF EXISTS "Teachers update own record" ON public.teachers;
CREATE POLICY "Teachers update own record" ON public.teachers
    FOR UPDATE USING (id = auth.uid());

DROP POLICY IF EXISTS "Students view their teacher" ON public.teachers;
CREATE POLICY "Students view their teacher" ON public.teachers
    FOR SELECT USING (
        EXISTS (
            SELECT 1 FROM public.students s
            WHERE s.id = auth.uid() AND s.teacher_id = teachers.id
        )
    );

DROP POLICY IF EXISTS "Company admins manage teachers" ON public.teachers;
CREATE POLICY "Company admins manage teachers" ON public.teachers
    FOR ALL USING (public.get_auth_role() = 'company_admin'::public.user_role);
