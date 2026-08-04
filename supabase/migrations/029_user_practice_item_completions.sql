-- Free-practice + homework completion markers for reading/listening catalog items.
-- SSOT for "做过" badges; synced across devices.

CREATE TABLE IF NOT EXISTS public.user_practice_item_completions (
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    module_id TEXT NOT NULL CHECK (module_id IN ('reading', 'listening')),
    item_ref TEXT NOT NULL,
    first_completed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, module_id, item_ref)
);

CREATE INDEX IF NOT EXISTS user_practice_item_completions_user_module_idx
    ON public.user_practice_item_completions (user_id, module_id);

ALTER TABLE public.user_practice_item_completions ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Users manage own practice item completions"
    ON public.user_practice_item_completions;
CREATE POLICY "Users manage own practice item completions"
    ON public.user_practice_item_completions
    FOR ALL
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

GRANT SELECT, INSERT, UPDATE ON public.user_practice_item_completions TO authenticated;

-- ---------------------------------------------------------------------------
-- mark_my_practice_items_completed: free practice finish
-- ---------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION private.mark_my_practice_items_completed(
    p_module_id text,
    p_item_refs text[]
)
RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path TO 'public', 'private'
AS $function$
DECLARE
    v_ref text;
BEGIN
    IF auth.uid() IS NULL THEN
        RAISE EXCEPTION 'Not authenticated';
    END IF;

    IF p_module_id IS DISTINCT FROM 'reading' AND p_module_id IS DISTINCT FROM 'listening' THEN
        RAISE EXCEPTION 'Invalid module_id';
    END IF;

    IF p_item_refs IS NULL OR cardinality(p_item_refs) = 0 THEN
        RETURN;
    END IF;

    FOREACH v_ref IN ARRAY p_item_refs
    LOOP
        IF v_ref IS NULL OR BTRIM(v_ref) = '' THEN
            CONTINUE;
        END IF;
        INSERT INTO public.user_practice_item_completions (user_id, module_id, item_ref)
        VALUES (auth.uid(), p_module_id, BTRIM(v_ref))
        ON CONFLICT (user_id, module_id, item_ref) DO NOTHING;
    END LOOP;
END;
$function$;

REVOKE ALL ON FUNCTION private.mark_my_practice_items_completed(text, text[]) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION private.mark_my_practice_items_completed(text, text[]) TO authenticated;
GRANT EXECUTE ON FUNCTION private.mark_my_practice_items_completed(text, text[]) TO service_role;

CREATE OR REPLACE FUNCTION public.mark_my_practice_items_completed(
    p_module_id text,
    p_item_refs text[]
)
RETURNS void
LANGUAGE sql
SECURITY INVOKER
SET search_path TO 'public', 'private'
AS $function$
  SELECT private.mark_my_practice_items_completed(p_module_id, p_item_refs);
$function$;

REVOKE ALL ON FUNCTION public.mark_my_practice_items_completed(text, text[]) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.mark_my_practice_items_completed(text, text[]) TO authenticated;
GRANT EXECUTE ON FUNCTION public.mark_my_practice_items_completed(text, text[]) TO service_role;

-- ---------------------------------------------------------------------------
-- submit_practice_assignment: also upsert completions for assignment items
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

    IF v_assignment.module_id IN ('reading', 'listening') THEN
        INSERT INTO public.user_practice_item_completions (user_id, module_id, item_ref)
        SELECT auth.uid(), v_assignment.module_id, i.item_ref
        FROM public.practice_assignment_items i
        WHERE i.assignment_id = v_assignment.id
        ON CONFLICT (user_id, module_id, item_ref) DO NOTHING;
    END IF;

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
-- Backfill from already-submitted reading/listening homework
-- ---------------------------------------------------------------------------
INSERT INTO public.user_practice_item_completions (user_id, module_id, item_ref, first_completed_at)
SELECT
    s.student_id,
    a.module_id,
    i.item_ref,
    COALESCE(s.submitted_at, now())
FROM public.practice_assignment_submissions s
JOIN public.practice_assignments a ON a.id = s.assignment_id
JOIN public.practice_assignment_items i ON i.assignment_id = a.id
WHERE s.status IN ('submitted', 'returned')
  AND a.module_id IN ('reading', 'listening')
ON CONFLICT (user_id, module_id, item_ref) DO NOTHING;
