-- Fix infinite recursion in practice_assignments RLS policies.
-- Child-table policies queried practice_assignments under RLS, and the
-- student policy on practice_assignments queried recipients under RLS,
-- which re-entered practice_assignments → recursion.
-- Use SECURITY DEFINER helpers so ownership/recipient checks bypass RLS.

CREATE OR REPLACE FUNCTION private.is_practice_assignment_teacher(p_assignment_id uuid)
RETURNS boolean
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path TO 'public', 'private'
AS $$
  SELECT EXISTS (
    SELECT 1
    FROM public.practice_assignments a
    WHERE a.id = p_assignment_id
      AND a.teacher_id = auth.uid()
  );
$$;

CREATE OR REPLACE FUNCTION private.is_practice_assignment_recipient(p_assignment_id uuid)
RETURNS boolean
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path TO 'public', 'private'
AS $$
  SELECT EXISTS (
    SELECT 1
    FROM public.practice_assignment_recipients r
    WHERE r.assignment_id = p_assignment_id
      AND r.student_id = auth.uid()
  );
$$;

REVOKE ALL ON FUNCTION private.is_practice_assignment_teacher(uuid) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION private.is_practice_assignment_teacher(uuid) TO authenticated;
GRANT EXECUTE ON FUNCTION private.is_practice_assignment_teacher(uuid) TO service_role;

REVOKE ALL ON FUNCTION private.is_practice_assignment_recipient(uuid) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION private.is_practice_assignment_recipient(uuid) TO authenticated;
GRANT EXECUTE ON FUNCTION private.is_practice_assignment_recipient(uuid) TO service_role;

-- practice_assignments
DROP POLICY IF EXISTS "Teachers view own practice assignments" ON public.practice_assignments;
CREATE POLICY "Teachers view own practice assignments"
    ON public.practice_assignments FOR SELECT
    USING (teacher_id = auth.uid());

DROP POLICY IF EXISTS "Students view received practice assignments" ON public.practice_assignments;
CREATE POLICY "Students view received practice assignments"
    ON public.practice_assignments FOR SELECT
    USING (private.is_practice_assignment_recipient(id));

-- practice_assignment_items
DROP POLICY IF EXISTS "Teachers view own assignment items" ON public.practice_assignment_items;
CREATE POLICY "Teachers view own assignment items"
    ON public.practice_assignment_items FOR SELECT
    USING (private.is_practice_assignment_teacher(assignment_id));

DROP POLICY IF EXISTS "Students view received assignment items" ON public.practice_assignment_items;
CREATE POLICY "Students view received assignment items"
    ON public.practice_assignment_items FOR SELECT
    USING (private.is_practice_assignment_recipient(assignment_id));

-- practice_assignment_recipients
DROP POLICY IF EXISTS "Teachers view own assignment recipients" ON public.practice_assignment_recipients;
CREATE POLICY "Teachers view own assignment recipients"
    ON public.practice_assignment_recipients FOR SELECT
    USING (private.is_practice_assignment_teacher(assignment_id));

DROP POLICY IF EXISTS "Students view own assignment recipient rows" ON public.practice_assignment_recipients;
CREATE POLICY "Students view own assignment recipient rows"
    ON public.practice_assignment_recipients FOR SELECT
    USING (student_id = auth.uid());

-- practice_assignment_submissions
DROP POLICY IF EXISTS "Teachers view own assignment submissions" ON public.practice_assignment_submissions;
CREATE POLICY "Teachers view own assignment submissions"
    ON public.practice_assignment_submissions FOR SELECT
    USING (private.is_practice_assignment_teacher(assignment_id));

DROP POLICY IF EXISTS "Students view own assignment submissions" ON public.practice_assignment_submissions;
CREATE POLICY "Students view own assignment submissions"
    ON public.practice_assignment_submissions FOR SELECT
    USING (student_id = auth.uid());
