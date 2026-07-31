-- Practice assignments for reading / listening modules.
-- Teachers assign catalog sets/materials to named students with a due time.
-- Students submit once; answer_payload supports read-only review.

CREATE TABLE IF NOT EXISTS public.practice_assignments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    teacher_id UUID NOT NULL REFERENCES public.teachers(id) ON DELETE CASCADE,
    agency_id UUID NOT NULL REFERENCES public.agencies(id) ON DELETE CASCADE,
    module_id TEXT NOT NULL CHECK (module_id IN ('reading', 'listening')),
    title TEXT NOT NULL,
    due_at TIMESTAMPTZ NOT NULL,
    allow_late BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS practice_assignments_teacher_created_idx
    ON public.practice_assignments (teacher_id, created_at DESC);

CREATE INDEX IF NOT EXISTS practice_assignments_module_due_idx
    ON public.practice_assignments (module_id, due_at);

CREATE TABLE IF NOT EXISTS public.practice_assignment_items (
    assignment_id UUID NOT NULL REFERENCES public.practice_assignments(id) ON DELETE CASCADE,
    item_ref TEXT NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    PRIMARY KEY (assignment_id, item_ref)
);

CREATE INDEX IF NOT EXISTS practice_assignment_items_sort_idx
    ON public.practice_assignment_items (assignment_id, sort_order);

CREATE TABLE IF NOT EXISTS public.practice_assignment_recipients (
    assignment_id UUID NOT NULL REFERENCES public.practice_assignments(id) ON DELETE CASCADE,
    student_id UUID NOT NULL REFERENCES public.students(id) ON DELETE CASCADE,
    PRIMARY KEY (assignment_id, student_id)
);

CREATE INDEX IF NOT EXISTS practice_assignment_recipients_student_idx
    ON public.practice_assignment_recipients (student_id);

CREATE TABLE IF NOT EXISTS public.practice_assignment_submissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    assignment_id UUID NOT NULL REFERENCES public.practice_assignments(id) ON DELETE CASCADE,
    student_id UUID NOT NULL REFERENCES public.students(id) ON DELETE CASCADE,
    status TEXT NOT NULL DEFAULT 'pending'
        CHECK (status IN ('pending', 'in_progress', 'submitted')),
    started_at TIMESTAMPTZ,
    submitted_at TIMESTAMPTZ,
    correct_count INT NOT NULL DEFAULT 0,
    total_count INT NOT NULL DEFAULT 0,
    earned_tokens INT NOT NULL DEFAULT 0,
    answer_payload JSONB,
    UNIQUE (assignment_id, student_id)
);

CREATE INDEX IF NOT EXISTS practice_assignment_submissions_student_status_idx
    ON public.practice_assignment_submissions (student_id, status);

CREATE INDEX IF NOT EXISTS practice_assignment_submissions_assignment_idx
    ON public.practice_assignment_submissions (assignment_id);

ALTER TABLE public.practice_assignments ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.practice_assignment_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.practice_assignment_recipients ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.practice_assignment_submissions ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Teachers view own practice assignments" ON public.practice_assignments;
CREATE POLICY "Teachers view own practice assignments"
    ON public.practice_assignments FOR SELECT
    USING (teacher_id = auth.uid());

DROP POLICY IF EXISTS "Students view received practice assignments" ON public.practice_assignments;
CREATE POLICY "Students view received practice assignments"
    ON public.practice_assignments FOR SELECT
    USING (
        EXISTS (
            SELECT 1 FROM public.practice_assignment_recipients r
            WHERE r.assignment_id = practice_assignments.id
              AND r.student_id = auth.uid()
        )
    );

DROP POLICY IF EXISTS "Teachers view own assignment items" ON public.practice_assignment_items;
CREATE POLICY "Teachers view own assignment items"
    ON public.practice_assignment_items FOR SELECT
    USING (
        EXISTS (
            SELECT 1 FROM public.practice_assignments a
            WHERE a.id = practice_assignment_items.assignment_id
              AND a.teacher_id = auth.uid()
        )
    );

DROP POLICY IF EXISTS "Students view received assignment items" ON public.practice_assignment_items;
CREATE POLICY "Students view received assignment items"
    ON public.practice_assignment_items FOR SELECT
    USING (
        EXISTS (
            SELECT 1 FROM public.practice_assignment_recipients r
            WHERE r.assignment_id = practice_assignment_items.assignment_id
              AND r.student_id = auth.uid()
        )
    );

DROP POLICY IF EXISTS "Teachers view own assignment recipients" ON public.practice_assignment_recipients;
CREATE POLICY "Teachers view own assignment recipients"
    ON public.practice_assignment_recipients FOR SELECT
    USING (
        EXISTS (
            SELECT 1 FROM public.practice_assignments a
            WHERE a.id = practice_assignment_recipients.assignment_id
              AND a.teacher_id = auth.uid()
        )
    );

DROP POLICY IF EXISTS "Students view own assignment recipient rows" ON public.practice_assignment_recipients;
CREATE POLICY "Students view own assignment recipient rows"
    ON public.practice_assignment_recipients FOR SELECT
    USING (student_id = auth.uid());

DROP POLICY IF EXISTS "Teachers view own assignment submissions" ON public.practice_assignment_submissions;
CREATE POLICY "Teachers view own assignment submissions"
    ON public.practice_assignment_submissions FOR SELECT
    USING (
        EXISTS (
            SELECT 1 FROM public.practice_assignments a
            WHERE a.id = practice_assignment_submissions.assignment_id
              AND a.teacher_id = auth.uid()
        )
    );

DROP POLICY IF EXISTS "Students view own assignment submissions" ON public.practice_assignment_submissions;
CREATE POLICY "Students view own assignment submissions"
    ON public.practice_assignment_submissions FOR SELECT
    USING (student_id = auth.uid());

-- ---------------------------------------------------------------------------
-- RPCs
-- ---------------------------------------------------------------------------

CREATE OR REPLACE FUNCTION private.create_practice_assignment(
    p_module_id text,
    p_title text,
    p_due_at timestamptz,
    p_item_refs text[],
    p_student_ids uuid[],
    p_allow_late boolean DEFAULT false
)
RETURNS jsonb
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path TO 'public', 'private'
AS $function$
DECLARE
    v_teacher_id uuid := auth.uid();
    v_agency_id uuid;
    v_assignment_id uuid;
    v_ref text;
    v_student_id uuid;
    v_idx int;
    v_found int;
BEGIN
    IF v_teacher_id IS NULL THEN
        RAISE EXCEPTION 'Not authenticated';
    END IF;

    IF private.get_auth_role() IS DISTINCT FROM 'teacher'::public.user_role THEN
        RAISE EXCEPTION 'Only teachers can create assignments';
    END IF;

    IF p_module_id IS DISTINCT FROM 'reading' AND p_module_id IS DISTINCT FROM 'listening' THEN
        RAISE EXCEPTION 'Invalid module_id';
    END IF;

    IF p_title IS NULL OR BTRIM(p_title) = '' THEN
        RAISE EXCEPTION 'Title is required';
    END IF;

    IF p_due_at IS NULL THEN
        RAISE EXCEPTION 'due_at is required';
    END IF;

    IF p_item_refs IS NULL OR cardinality(p_item_refs) = 0 THEN
        RAISE EXCEPTION 'At least one item is required';
    END IF;

    IF p_student_ids IS NULL OR cardinality(p_student_ids) = 0 THEN
        RAISE EXCEPTION 'At least one student is required';
    END IF;

    SELECT t.agency_id INTO v_agency_id
    FROM public.teachers t
    WHERE t.id = v_teacher_id;

    IF v_agency_id IS NULL THEN
        RAISE EXCEPTION 'Teacher profile not found';
    END IF;

    -- Validate catalog refs
    FOREACH v_ref IN ARRAY p_item_refs LOOP
        IF p_module_id = 'reading' THEN
            SELECT COUNT(*) INTO v_found FROM public.reading_sets WHERE set_id = v_ref;
        ELSE
            SELECT COUNT(*) INTO v_found FROM public.listening_materials WHERE material_id = v_ref;
        END IF;
        IF v_found = 0 THEN
            RAISE EXCEPTION 'Unknown item_ref: %', v_ref;
        END IF;
    END LOOP;

    -- Validate students belong to this teacher
    FOREACH v_student_id IN ARRAY p_student_ids LOOP
        IF NOT EXISTS (
            SELECT 1 FROM public.students s
            WHERE s.id = v_student_id AND s.teacher_id = v_teacher_id
        ) THEN
            RAISE EXCEPTION 'Student not assigned to teacher: %', v_student_id;
        END IF;
    END LOOP;

    INSERT INTO public.practice_assignments (
        teacher_id, agency_id, module_id, title, due_at, allow_late
    ) VALUES (
        v_teacher_id, v_agency_id, p_module_id, BTRIM(p_title), p_due_at, COALESCE(p_allow_late, false)
    )
    RETURNING id INTO v_assignment_id;

    v_idx := 0;
    FOREACH v_ref IN ARRAY p_item_refs LOOP
        INSERT INTO public.practice_assignment_items (assignment_id, item_ref, sort_order)
        VALUES (v_assignment_id, v_ref, v_idx)
        ON CONFLICT DO NOTHING;
        v_idx := v_idx + 1;
    END LOOP;

    FOREACH v_student_id IN ARRAY p_student_ids LOOP
        INSERT INTO public.practice_assignment_recipients (assignment_id, student_id)
        VALUES (v_assignment_id, v_student_id)
        ON CONFLICT DO NOTHING;

        INSERT INTO public.practice_assignment_submissions (assignment_id, student_id, status)
        VALUES (v_assignment_id, v_student_id, 'pending')
        ON CONFLICT DO NOTHING;
    END LOOP;

    RETURN jsonb_build_object(
        'assignment_id', v_assignment_id,
        'item_count', (SELECT COUNT(*) FROM public.practice_assignment_items WHERE assignment_id = v_assignment_id),
        'student_count', (SELECT COUNT(*) FROM public.practice_assignment_recipients WHERE assignment_id = v_assignment_id)
    );
END;
$function$;

CREATE OR REPLACE FUNCTION private.start_practice_assignment(p_submission_id uuid)
RETURNS jsonb
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path TO 'public', 'private'
AS $function$
DECLARE
    v_row public.practice_assignment_submissions%ROWTYPE;
    v_assignment public.practice_assignments%ROWTYPE;
BEGIN
    IF auth.uid() IS NULL THEN
        RAISE EXCEPTION 'Not authenticated';
    END IF;

    SELECT * INTO v_row
    FROM public.practice_assignment_submissions
    WHERE id = p_submission_id AND student_id = auth.uid();

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Submission not found';
    END IF;

    IF v_row.status = 'submitted' THEN
        RAISE EXCEPTION 'Assignment already submitted';
    END IF;

    SELECT * INTO v_assignment
    FROM public.practice_assignments
    WHERE id = v_row.assignment_id;

    IF now() > v_assignment.due_at AND NOT v_assignment.allow_late THEN
        RAISE EXCEPTION 'Assignment is past due';
    END IF;

    IF v_row.status = 'pending' THEN
        UPDATE public.practice_assignment_submissions
        SET status = 'in_progress',
            started_at = COALESCE(started_at, now())
        WHERE id = p_submission_id
        RETURNING * INTO v_row;
    END IF;

    RETURN jsonb_build_object(
        'submission_id', v_row.id,
        'assignment_id', v_row.assignment_id,
        'status', v_row.status
    );
END;
$function$;

CREATE OR REPLACE FUNCTION private.submit_practice_assignment(
    p_submission_id uuid,
    p_correct_count int,
    p_total_count int,
    p_earned_tokens int,
    p_answer_payload jsonb
)
RETURNS jsonb
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path TO 'public', 'private'
AS $function$
DECLARE
    v_row public.practice_assignment_submissions%ROWTYPE;
    v_assignment public.practice_assignments%ROWTYPE;
BEGIN
    IF auth.uid() IS NULL THEN
        RAISE EXCEPTION 'Not authenticated';
    END IF;

    SELECT * INTO v_row
    FROM public.practice_assignment_submissions
    WHERE id = p_submission_id AND student_id = auth.uid()
    FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Submission not found';
    END IF;

    IF v_row.status = 'submitted' THEN
        RAISE EXCEPTION 'Assignment already submitted';
    END IF;

    SELECT * INTO v_assignment
    FROM public.practice_assignments
    WHERE id = v_row.assignment_id;

    IF now() > v_assignment.due_at AND NOT v_assignment.allow_late THEN
        RAISE EXCEPTION 'Assignment is past due';
    END IF;

    UPDATE public.practice_assignment_submissions
    SET
        status = 'submitted',
        submitted_at = now(),
        started_at = COALESCE(started_at, now()),
        correct_count = COALESCE(p_correct_count, 0),
        total_count = COALESCE(p_total_count, 0),
        earned_tokens = COALESCE(p_earned_tokens, 0),
        answer_payload = p_answer_payload
    WHERE id = p_submission_id
    RETURNING * INTO v_row;

    RETURN jsonb_build_object(
        'submission_id', v_row.id,
        'assignment_id', v_row.assignment_id,
        'status', v_row.status,
        'submitted_at', v_row.submitted_at,
        'correct_count', v_row.correct_count,
        'total_count', v_row.total_count,
        'earned_tokens', v_row.earned_tokens
    );
END;
$function$;

REVOKE ALL ON FUNCTION private.create_practice_assignment(text, text, timestamptz, text[], uuid[], boolean) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION private.create_practice_assignment(text, text, timestamptz, text[], uuid[], boolean) TO authenticated;
GRANT EXECUTE ON FUNCTION private.create_practice_assignment(text, text, timestamptz, text[], uuid[], boolean) TO service_role;

REVOKE ALL ON FUNCTION private.start_practice_assignment(uuid) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION private.start_practice_assignment(uuid) TO authenticated;
GRANT EXECUTE ON FUNCTION private.start_practice_assignment(uuid) TO service_role;

REVOKE ALL ON FUNCTION private.submit_practice_assignment(uuid, int, int, int, jsonb) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION private.submit_practice_assignment(uuid, int, int, int, jsonb) TO authenticated;
GRANT EXECUTE ON FUNCTION private.submit_practice_assignment(uuid, int, int, int, jsonb) TO service_role;

CREATE OR REPLACE FUNCTION public.create_practice_assignment(
    p_module_id text,
    p_title text,
    p_due_at timestamptz,
    p_item_refs text[],
    p_student_ids uuid[],
    p_allow_late boolean DEFAULT false
)
RETURNS jsonb
LANGUAGE sql
SECURITY INVOKER
SET search_path TO 'public', 'private'
AS $$
  SELECT private.create_practice_assignment(
      p_module_id, p_title, p_due_at, p_item_refs, p_student_ids, p_allow_late
  );
$$;

CREATE OR REPLACE FUNCTION public.start_practice_assignment(p_submission_id uuid)
RETURNS jsonb
LANGUAGE sql
SECURITY INVOKER
SET search_path TO 'public', 'private'
AS $$
  SELECT private.start_practice_assignment(p_submission_id);
$$;

CREATE OR REPLACE FUNCTION public.submit_practice_assignment(
    p_submission_id uuid,
    p_correct_count int,
    p_total_count int,
    p_earned_tokens int,
    p_answer_payload jsonb
)
RETURNS jsonb
LANGUAGE sql
SECURITY INVOKER
SET search_path TO 'public', 'private'
AS $$
  SELECT private.submit_practice_assignment(
      p_submission_id, p_correct_count, p_total_count, p_earned_tokens, p_answer_payload
  );
$$;

REVOKE ALL ON FUNCTION public.create_practice_assignment(text, text, timestamptz, text[], uuid[], boolean) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.create_practice_assignment(text, text, timestamptz, text[], uuid[], boolean) TO authenticated;
GRANT EXECUTE ON FUNCTION public.create_practice_assignment(text, text, timestamptz, text[], uuid[], boolean) TO service_role;

REVOKE ALL ON FUNCTION public.start_practice_assignment(uuid) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.start_practice_assignment(uuid) TO authenticated;
GRANT EXECUTE ON FUNCTION public.start_practice_assignment(uuid) TO service_role;

REVOKE ALL ON FUNCTION public.submit_practice_assignment(uuid, int, int, int, jsonb) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.submit_practice_assignment(uuid, int, int, int, jsonb) TO authenticated;
GRANT EXECUTE ON FUNCTION public.submit_practice_assignment(uuid, int, int, int, jsonb) TO service_role;
