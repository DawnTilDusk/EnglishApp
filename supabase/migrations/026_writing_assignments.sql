-- Writing homework: extend practice_assignments for module writing,
-- returned status, file paths, and teacher return RPC + Storage bucket.

-- ---------------------------------------------------------------------------
-- 1. module_id CHECK: allow writing
-- ---------------------------------------------------------------------------
ALTER TABLE public.practice_assignments
    DROP CONSTRAINT IF EXISTS practice_assignments_module_id_check;

ALTER TABLE public.practice_assignments
    ADD CONSTRAINT practice_assignments_module_id_check
    CHECK (module_id IN ('reading', 'listening', 'writing'));

-- ---------------------------------------------------------------------------
-- 2. submission status + writing columns
-- ---------------------------------------------------------------------------
ALTER TABLE public.practice_assignment_submissions
    DROP CONSTRAINT IF EXISTS practice_assignment_submissions_status_check;

ALTER TABLE public.practice_assignment_submissions
    ADD CONSTRAINT practice_assignment_submissions_status_check
    CHECK (status IN ('pending', 'in_progress', 'submitted', 'returned'));

ALTER TABLE public.practice_assignment_submissions
    ADD COLUMN IF NOT EXISTS score INT,
    ADD COLUMN IF NOT EXISTS max_score INT,
    ADD COLUMN IF NOT EXISTS feedback_text TEXT,
    ADD COLUMN IF NOT EXISTS returned_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS original_path TEXT,
    ADD COLUMN IF NOT EXISTS annotated_path TEXT;

-- ---------------------------------------------------------------------------
-- 3. create_practice_assignment: support writing catalog
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

    IF p_module_id IS DISTINCT FROM 'reading'
       AND p_module_id IS DISTINCT FROM 'listening'
       AND p_module_id IS DISTINCT FROM 'writing' THEN
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

    FOREACH v_ref IN ARRAY p_item_refs LOOP
        IF p_module_id = 'reading' THEN
            SELECT COUNT(*) INTO v_found FROM public.reading_sets WHERE set_id = v_ref;
        ELSIF p_module_id = 'listening' THEN
            SELECT COUNT(*) INTO v_found FROM public.listening_materials WHERE material_id = v_ref;
        ELSE
            SELECT COUNT(*) INTO v_found FROM public.writing_prompts WHERE prompt_id = v_ref;
        END IF;
        IF v_found = 0 THEN
            RAISE EXCEPTION 'Unknown item_ref: %', v_ref;
        END IF;
    END LOOP;

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

-- ---------------------------------------------------------------------------
-- 4. start: block returned as well as submitted
-- ---------------------------------------------------------------------------
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

    IF v_row.status IN ('submitted', 'returned') THEN
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

-- ---------------------------------------------------------------------------
-- 5. submit_practice_assignment: reading/listening only
-- ---------------------------------------------------------------------------
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

    IF v_row.status IN ('submitted', 'returned') THEN
        RAISE EXCEPTION 'Assignment already submitted';
    END IF;

    SELECT * INTO v_assignment
    FROM public.practice_assignments
    WHERE id = v_row.assignment_id;

    IF v_assignment.module_id = 'writing' THEN
        RAISE EXCEPTION 'Use submit_writing_assignment for writing module';
    END IF;

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

-- ---------------------------------------------------------------------------
-- 6. submit_writing_assignment
-- ---------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION private.submit_writing_assignment(
    p_submission_id uuid,
    p_original_path text
)
RETURNS jsonb
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path TO 'public', 'private'
AS $function$
DECLARE
    v_row public.practice_assignment_submissions%ROWTYPE;
    v_assignment public.practice_assignments%ROWTYPE;
    v_expected_prefix text;
BEGIN
    IF auth.uid() IS NULL THEN
        RAISE EXCEPTION 'Not authenticated';
    END IF;

    IF p_original_path IS NULL OR BTRIM(p_original_path) = '' THEN
        RAISE EXCEPTION 'original_path is required';
    END IF;

    SELECT * INTO v_row
    FROM public.practice_assignment_submissions
    WHERE id = p_submission_id AND student_id = auth.uid()
    FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Submission not found';
    END IF;

    IF v_row.status IN ('submitted', 'returned') THEN
        RAISE EXCEPTION 'Assignment already submitted';
    END IF;

    SELECT * INTO v_assignment
    FROM public.practice_assignments
    WHERE id = v_row.assignment_id;

    IF v_assignment.module_id IS DISTINCT FROM 'writing' THEN
        RAISE EXCEPTION 'Not a writing assignment';
    END IF;

    IF now() > v_assignment.due_at AND NOT v_assignment.allow_late THEN
        RAISE EXCEPTION 'Assignment is past due';
    END IF;

    v_expected_prefix := v_assignment.id::text || '/' || v_row.id::text || '/original.';
    IF position(v_expected_prefix in p_original_path) <> 1 THEN
        RAISE EXCEPTION 'Invalid original_path prefix';
    END IF;

    UPDATE public.practice_assignment_submissions
    SET
        status = 'submitted',
        submitted_at = now(),
        started_at = COALESCE(started_at, now()),
        original_path = BTRIM(p_original_path),
        correct_count = 0,
        total_count = 0,
        earned_tokens = 0,
        answer_payload = jsonb_build_object('kind', 'writing', 'original_path', BTRIM(p_original_path))
    WHERE id = p_submission_id
    RETURNING * INTO v_row;

    RETURN jsonb_build_object(
        'submission_id', v_row.id,
        'assignment_id', v_row.assignment_id,
        'status', v_row.status,
        'submitted_at', v_row.submitted_at,
        'original_path', v_row.original_path
    );
END;
$function$;

-- ---------------------------------------------------------------------------
-- 7. return_writing_assignment (teacher)
-- ---------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION private.return_writing_assignment(
    p_submission_id uuid,
    p_annotated_path text,
    p_score int,
    p_feedback_text text DEFAULT NULL
)
RETURNS jsonb
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path TO 'public', 'private'
AS $function$
DECLARE
    v_row public.practice_assignment_submissions%ROWTYPE;
    v_assignment public.practice_assignments%ROWTYPE;
    v_prompt_id text;
    v_max_score int;
    v_reward int;
    v_expected_prefix text;
BEGIN
    IF auth.uid() IS NULL THEN
        RAISE EXCEPTION 'Not authenticated';
    END IF;

    IF private.get_auth_role() IS DISTINCT FROM 'teacher'::public.user_role THEN
        RAISE EXCEPTION 'Only teachers can return writing assignments';
    END IF;

    IF p_annotated_path IS NULL OR BTRIM(p_annotated_path) = '' THEN
        RAISE EXCEPTION 'annotated_path is required';
    END IF;

    SELECT * INTO v_row
    FROM public.practice_assignment_submissions
    WHERE id = p_submission_id
    FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Submission not found';
    END IF;

    SELECT * INTO v_assignment
    FROM public.practice_assignments
    WHERE id = v_row.assignment_id;

    IF v_assignment.teacher_id IS DISTINCT FROM auth.uid() THEN
        RAISE EXCEPTION 'Not your assignment';
    END IF;

    IF v_assignment.module_id IS DISTINCT FROM 'writing' THEN
        RAISE EXCEPTION 'Not a writing assignment';
    END IF;

    IF v_row.status IS DISTINCT FROM 'submitted' THEN
        RAISE EXCEPTION 'Submission is not awaiting grading';
    END IF;

    v_expected_prefix := v_assignment.id::text || '/' || v_row.id::text || '/annotated.';
    IF position(v_expected_prefix in p_annotated_path) <> 1 THEN
        RAISE EXCEPTION 'Invalid annotated_path prefix';
    END IF;

    SELECT i.item_ref INTO v_prompt_id
    FROM public.practice_assignment_items i
    WHERE i.assignment_id = v_assignment.id
    ORDER BY i.sort_order
    LIMIT 1;

    SELECT p.max_score, p.reward_token
    INTO v_max_score, v_reward
    FROM public.writing_prompts p
    WHERE p.prompt_id = v_prompt_id;

    IF v_max_score IS NULL THEN
        v_max_score := 15;
    END IF;
    IF v_reward IS NULL THEN
        v_reward := 5;
    END IF;

    IF p_score IS NULL OR p_score < 0 OR p_score > v_max_score THEN
        RAISE EXCEPTION 'score must be between 0 and %', v_max_score;
    END IF;

    UPDATE public.practice_assignment_submissions
    SET
        status = 'returned',
        annotated_path = BTRIM(p_annotated_path),
        score = p_score,
        max_score = v_max_score,
        feedback_text = NULLIF(BTRIM(COALESCE(p_feedback_text, '')), ''),
        returned_at = now(),
        earned_tokens = v_reward,
        answer_payload = COALESCE(answer_payload, '{}'::jsonb)
            || jsonb_build_object(
                'kind', 'writing',
                'annotated_path', BTRIM(p_annotated_path),
                'score', p_score,
                'max_score', v_max_score
            )
    WHERE id = p_submission_id
    RETURNING * INTO v_row;

    RETURN jsonb_build_object(
        'submission_id', v_row.id,
        'assignment_id', v_row.assignment_id,
        'status', v_row.status,
        'score', v_row.score,
        'max_score', v_row.max_score,
        'earned_tokens', v_row.earned_tokens,
        'annotated_path', v_row.annotated_path,
        'returned_at', v_row.returned_at
    );
END;
$function$;

REVOKE ALL ON FUNCTION private.submit_writing_assignment(uuid, text) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION private.submit_writing_assignment(uuid, text) TO authenticated;
GRANT EXECUTE ON FUNCTION private.submit_writing_assignment(uuid, text) TO service_role;

REVOKE ALL ON FUNCTION private.return_writing_assignment(uuid, text, int, text) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION private.return_writing_assignment(uuid, text, int, text) TO authenticated;
GRANT EXECUTE ON FUNCTION private.return_writing_assignment(uuid, text, int, text) TO service_role;

CREATE OR REPLACE FUNCTION public.submit_writing_assignment(
    p_submission_id uuid,
    p_original_path text
)
RETURNS jsonb
LANGUAGE sql
SECURITY INVOKER
SET search_path TO 'public', 'private'
AS $$
  SELECT private.submit_writing_assignment(p_submission_id, p_original_path);
$$;

CREATE OR REPLACE FUNCTION public.return_writing_assignment(
    p_submission_id uuid,
    p_annotated_path text,
    p_score int,
    p_feedback_text text DEFAULT NULL
)
RETURNS jsonb
LANGUAGE sql
SECURITY INVOKER
SET search_path TO 'public', 'private'
AS $$
  SELECT private.return_writing_assignment(
      p_submission_id, p_annotated_path, p_score, p_feedback_text
  );
$$;

REVOKE ALL ON FUNCTION public.submit_writing_assignment(uuid, text) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.submit_writing_assignment(uuid, text) TO authenticated;
GRANT EXECUTE ON FUNCTION public.submit_writing_assignment(uuid, text) TO service_role;

REVOKE ALL ON FUNCTION public.return_writing_assignment(uuid, text, int, text) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.return_writing_assignment(uuid, text, int, text) TO authenticated;
GRANT EXECUTE ON FUNCTION public.return_writing_assignment(uuid, text, int, text) TO service_role;

-- ---------------------------------------------------------------------------
-- 8. Storage bucket writing-submissions (private)
-- ---------------------------------------------------------------------------
INSERT INTO storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
VALUES (
    'writing-submissions',
    'writing-submissions',
    false,
    10485760,
    ARRAY['image/jpeg', 'image/png', 'image/webp', 'application/pdf']
)
ON CONFLICT (id) DO UPDATE SET
    public = false,
    file_size_limit = EXCLUDED.file_size_limit,
    allowed_mime_types = EXCLUDED.allowed_mime_types;

-- Helpers for storage path: {assignment_id}/{submission_id}/original.ext
CREATE OR REPLACE FUNCTION private.writing_storage_assignment_id(p_name text)
RETURNS uuid
LANGUAGE plpgsql
IMMUTABLE
AS $$
BEGIN
  RETURN NULLIF(split_part(p_name, '/', 1), '')::uuid;
EXCEPTION WHEN others THEN
  RETURN NULL;
END;
$$;

CREATE OR REPLACE FUNCTION private.writing_storage_submission_id(p_name text)
RETURNS uuid
LANGUAGE plpgsql
IMMUTABLE
AS $$
BEGIN
  RETURN NULLIF(split_part(p_name, '/', 2), '')::uuid;
EXCEPTION WHEN others THEN
  RETURN NULL;
END;
$$;

CREATE OR REPLACE FUNCTION private.writing_storage_basename(p_name text)
RETURNS text
LANGUAGE sql
IMMUTABLE
AS $$
  SELECT split_part(p_name, '/', 3);
$$;

REVOKE ALL ON FUNCTION private.writing_storage_assignment_id(text) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION private.writing_storage_assignment_id(text) TO authenticated, service_role;
REVOKE ALL ON FUNCTION private.writing_storage_submission_id(text) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION private.writing_storage_submission_id(text) TO authenticated, service_role;
REVOKE ALL ON FUNCTION private.writing_storage_basename(text) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION private.writing_storage_basename(text) TO authenticated, service_role;

-- Student: upload/read own original*
DROP POLICY IF EXISTS "Students upload writing originals" ON storage.objects;
CREATE POLICY "Students upload writing originals"
    ON storage.objects FOR INSERT TO authenticated
    WITH CHECK (
        bucket_id = 'writing-submissions'
        AND private.writing_storage_basename(name) LIKE 'original.%'
        AND EXISTS (
            SELECT 1
            FROM public.practice_assignment_submissions s
            WHERE s.id = private.writing_storage_submission_id(name)
              AND s.assignment_id = private.writing_storage_assignment_id(name)
              AND s.student_id = auth.uid()
              AND s.status IN ('pending', 'in_progress')
        )
    );

DROP POLICY IF EXISTS "Students update writing originals" ON storage.objects;
CREATE POLICY "Students update writing originals"
    ON storage.objects FOR UPDATE TO authenticated
    USING (
        bucket_id = 'writing-submissions'
        AND private.writing_storage_basename(name) LIKE 'original.%'
        AND EXISTS (
            SELECT 1
            FROM public.practice_assignment_submissions s
            WHERE s.id = private.writing_storage_submission_id(name)
              AND s.assignment_id = private.writing_storage_assignment_id(name)
              AND s.student_id = auth.uid()
              AND s.status IN ('pending', 'in_progress')
        )
    )
    WITH CHECK (
        bucket_id = 'writing-submissions'
        AND private.writing_storage_basename(name) LIKE 'original.%'
        AND EXISTS (
            SELECT 1
            FROM public.practice_assignment_submissions s
            WHERE s.id = private.writing_storage_submission_id(name)
              AND s.assignment_id = private.writing_storage_assignment_id(name)
              AND s.student_id = auth.uid()
        )
    );

DROP POLICY IF EXISTS "Students read own writing files" ON storage.objects;
CREATE POLICY "Students read own writing files"
    ON storage.objects FOR SELECT TO authenticated
    USING (
        bucket_id = 'writing-submissions'
        AND EXISTS (
            SELECT 1
            FROM public.practice_assignment_submissions s
            WHERE s.id = private.writing_storage_submission_id(name)
              AND s.assignment_id = private.writing_storage_assignment_id(name)
              AND s.student_id = auth.uid()
        )
    );

-- Teacher: read all files under own assignments; upload annotated*
DROP POLICY IF EXISTS "Teachers read writing submissions" ON storage.objects;
CREATE POLICY "Teachers read writing submissions"
    ON storage.objects FOR SELECT TO authenticated
    USING (
        bucket_id = 'writing-submissions'
        AND private.is_practice_assignment_teacher(
            private.writing_storage_assignment_id(name)
        )
    );

DROP POLICY IF EXISTS "Teachers upload writing annotations" ON storage.objects;
CREATE POLICY "Teachers upload writing annotations"
    ON storage.objects FOR INSERT TO authenticated
    WITH CHECK (
        bucket_id = 'writing-submissions'
        AND private.writing_storage_basename(name) LIKE 'annotated.%'
        AND private.is_practice_assignment_teacher(
            private.writing_storage_assignment_id(name)
        )
        AND EXISTS (
            SELECT 1
            FROM public.practice_assignment_submissions s
            WHERE s.id = private.writing_storage_submission_id(name)
              AND s.assignment_id = private.writing_storage_assignment_id(name)
              AND s.status = 'submitted'
        )
    );

DROP POLICY IF EXISTS "Teachers update writing annotations" ON storage.objects;
CREATE POLICY "Teachers update writing annotations"
    ON storage.objects FOR UPDATE TO authenticated
    USING (
        bucket_id = 'writing-submissions'
        AND private.writing_storage_basename(name) LIKE 'annotated.%'
        AND private.is_practice_assignment_teacher(
            private.writing_storage_assignment_id(name)
        )
    )
    WITH CHECK (
        bucket_id = 'writing-submissions'
        AND private.writing_storage_basename(name) LIKE 'annotated.%'
        AND private.is_practice_assignment_teacher(
            private.writing_storage_assignment_id(name)
        )
    );
