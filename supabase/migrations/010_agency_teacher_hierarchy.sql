-- 010: Teachers belong to agencies; same-agency bind constraint helpers

ALTER TABLE public.teachers
    ADD COLUMN IF NOT EXISTS agency_id UUID REFERENCES public.agencies(id) ON DELETE RESTRICT;

-- Backfill from profiles
UPDATE public.teachers t
SET agency_id = p.agency_id
FROM public.profiles p
WHERE t.id = p.id
  AND t.agency_id IS NULL
  AND p.agency_id IS NOT NULL;

-- Backfill remaining from students that point at this teacher
UPDATE public.teachers t
SET agency_id = s.agency_id
FROM (
    SELECT teacher_id, MIN(agency_id::text)::uuid AS agency_id
    FROM public.students
    WHERE teacher_id IS NOT NULL
    GROUP BY teacher_id
) s
WHERE t.id = s.teacher_id
  AND t.agency_id IS NULL;

CREATE INDEX IF NOT EXISTS teachers_agency_id_idx ON public.teachers (agency_id);

-- Enforce NOT NULL only when clean
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM public.teachers WHERE agency_id IS NULL) THEN
        RAISE NOTICE '010: some teachers still missing agency_id; NOT NULL not applied';
    ELSE
        ALTER TABLE public.teachers ALTER COLUMN agency_id SET NOT NULL;
    END IF;
END $$;

-- Keep teacher profiles.agency_id in sync with teachers.agency_id
UPDATE public.profiles p
SET agency_id = t.agency_id
FROM public.teachers t
WHERE p.id = t.id
  AND t.agency_id IS NOT NULL
  AND p.agency_id IS DISTINCT FROM t.agency_id;

-- Binding must stay within one agency
CREATE OR REPLACE FUNCTION public.enforce_student_teacher_same_agency()
RETURNS trigger AS $$
DECLARE
    v_teacher_agency UUID;
BEGIN
    IF NEW.teacher_id IS NULL THEN
        RETURN NEW;
    END IF;

    SELECT agency_id INTO v_teacher_agency
    FROM public.teachers
    WHERE id = NEW.teacher_id;

    IF v_teacher_agency IS NULL THEN
        RAISE EXCEPTION 'Teacher % has no agency_id', NEW.teacher_id;
    END IF;

    IF NEW.agency_id IS DISTINCT FROM v_teacher_agency THEN
        RAISE EXCEPTION 'Student agency_id (%) must match teacher agency_id (%)',
            NEW.agency_id, v_teacher_agency;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- PostgreSQL 15: EXECUTE FUNCTION; older: PROCEDURE. Prefer PROCEDURE for wider compatibility.
DROP TRIGGER IF EXISTS trg_students_same_agency_as_teacher ON public.students;
CREATE TRIGGER trg_students_same_agency_as_teacher
    BEFORE INSERT OR UPDATE OF teacher_id, agency_id ON public.students
    FOR EACH ROW
    EXECUTE PROCEDURE public.enforce_student_teacher_same_agency();

-- Helper predicates
CREATE OR REPLACE FUNCTION public.is_service_role()
RETURNS boolean
LANGUAGE sql
STABLE
AS $$
    SELECT coalesce(auth.role(), '') = 'service_role';
$$;

CREATE OR REPLACE FUNCTION public.is_agency_admin_of(p_agency_id UUID)
RETURNS boolean
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
    SELECT public.get_auth_role() = 'agency_admin'::public.user_role
       AND public.get_auth_agency_id() IS NOT DISTINCT FROM p_agency_id;
$$;

CREATE OR REPLACE FUNCTION public.is_teacher_in_agency(p_agency_id UUID)
RETURNS boolean
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
    SELECT EXISTS (
        SELECT 1
        FROM public.teachers t
        WHERE t.id = auth.uid()
          AND t.agency_id IS NOT DISTINCT FROM p_agency_id
    );
$$;

-- Teachers RLS: agency admins manage own agency; drop company_admin-only ALL
DROP POLICY IF EXISTS "Company admins manage teachers" ON public.teachers;
DROP POLICY IF EXISTS "Agency admins manage agency teachers" ON public.teachers;
CREATE POLICY "Agency admins manage agency teachers" ON public.teachers
    FOR ALL
    USING (public.is_agency_admin_of(agency_id))
    WITH CHECK (public.is_agency_admin_of(agency_id));

DROP POLICY IF EXISTS "Agency admins view agency teachers" ON public.teachers;
-- covered by ALL above

DROP POLICY IF EXISTS "Teachers view peers in agency" ON public.teachers;
CREATE POLICY "Teachers view peers in agency" ON public.teachers
    FOR SELECT
    USING (public.is_teacher_in_agency(agency_id));
