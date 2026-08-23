-- Community posts MVP: agency-scoped feed with class / grade visibility.
-- SELECT via RLS; writes via private DEFINER + public INVOKER RPCs.

CREATE TABLE IF NOT EXISTS public.community_posts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    agency_id UUID NOT NULL REFERENCES public.agencies(id) ON DELETE CASCADE,
    author_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    author_role TEXT NOT NULL CHECK (author_role IN ('student', 'teacher')),
    author_display_name TEXT NOT NULL DEFAULT '用户',
    title TEXT,
    body TEXT NOT NULL,
    class_id TEXT,
    author_grade TEXT,
    target_grades TEXT[],
    is_featured BOOLEAN NOT NULL DEFAULT false,
    featured_by UUID REFERENCES public.profiles(id) ON DELETE SET NULL,
    featured_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT community_posts_student_scope_check CHECK (
        author_role <> 'student'
        OR (
            class_id IS NOT NULL
            AND NULLIF(BTRIM(class_id), '') IS NOT NULL
            AND author_grade IS NOT NULL
        )
    ),
    CONSTRAINT community_posts_teacher_scope_check CHECK (
        author_role <> 'teacher'
        OR (
            target_grades IS NOT NULL
            AND cardinality(target_grades) > 0
        )
    ),
    CONSTRAINT community_posts_featured_student_only CHECK (
        NOT is_featured OR author_role = 'student'
    )
);

CREATE INDEX IF NOT EXISTS community_posts_agency_created_idx
    ON public.community_posts (agency_id, created_at DESC);

CREATE INDEX IF NOT EXISTS community_posts_author_idx
    ON public.community_posts (author_id, created_at DESC);

CREATE INDEX IF NOT EXISTS community_posts_class_idx
    ON public.community_posts (agency_id, class_id)
    WHERE class_id IS NOT NULL;

ALTER TABLE public.community_posts ENABLE ROW LEVEL SECURITY;

-- Helpers (DEFINER) so RLS does not recurse into profiles/students policies.
CREATE OR REPLACE FUNCTION private.get_auth_profile_grade()
RETURNS text
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path TO 'public', 'private'
AS $$
  SELECT p.grade
  FROM public.profiles p
  WHERE p.id = auth.uid();
$$;

CREATE OR REPLACE FUNCTION private.get_auth_student_class_id()
RETURNS text
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path TO 'public', 'private'
AS $$
  SELECT NULLIF(BTRIM(s.class_id), '')
  FROM public.students s
  WHERE s.id = auth.uid();
$$;

CREATE OR REPLACE FUNCTION private.is_bound_teacher_of_student(p_student_id uuid)
RETURNS boolean
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path TO 'public', 'private'
AS $$
  SELECT EXISTS (
    SELECT 1
    FROM public.students s
    WHERE s.id = p_student_id
      AND s.teacher_id = auth.uid()
  );
$$;

CREATE OR REPLACE FUNCTION private.is_allowed_profile_grade(p_grade text)
RETURNS boolean
LANGUAGE sql
IMMUTABLE
AS $$
  SELECT p_grade IN (
    '一年级', '二年级', '三年级', '四年级', '五年级', '六年级',
    '初一', '初二', '初三',
    '高一', '高二', '高三',
    '其他'
  );
$$;

CREATE OR REPLACE FUNCTION private.can_read_community_post(
    p_agency_id uuid,
    p_author_id uuid,
    p_author_role text,
    p_class_id text,
    p_author_grade text,
    p_target_grades text[],
    p_is_featured boolean
)
RETURNS boolean
LANGUAGE plpgsql
STABLE
SECURITY DEFINER
SET search_path TO 'public', 'private'
AS $function$
DECLARE
    v_role public.user_role;
    v_agency uuid;
    v_grade text;
    v_class text;
BEGIN
    IF auth.uid() IS NULL THEN
        RETURN false;
    END IF;

    v_role := private.get_auth_role();
    v_agency := private.get_auth_agency_id();

    IF v_agency IS NULL OR v_agency IS DISTINCT FROM p_agency_id THEN
        -- Prefer students.agency_id when profile agency is stale/null.
        IF v_role = 'student'::public.user_role THEN
            SELECT s.agency_id INTO v_agency
            FROM public.students s
            WHERE s.id = auth.uid();
        ELSIF v_role = 'teacher'::public.user_role THEN
            SELECT t.agency_id INTO v_agency
            FROM public.teachers t
            WHERE t.id = auth.uid();
        END IF;
    END IF;

    IF v_agency IS NULL OR v_agency IS DISTINCT FROM p_agency_id THEN
        RETURN false;
    END IF;

    IF p_author_id = auth.uid() THEN
        RETURN true;
    END IF;

    IF v_role = 'teacher'::public.user_role THEN
        IF NOT private.is_teacher_in_agency(p_agency_id) THEN
            RETURN false;
        END IF;
        IF p_author_role = 'teacher' THEN
            RETURN true;
        END IF;
        RETURN private.is_bound_teacher_of_student(p_author_id);
    END IF;

    IF v_role = 'student'::public.user_role THEN
        v_grade := private.get_auth_profile_grade();
        v_class := private.get_auth_student_class_id();

        IF p_author_role = 'teacher' THEN
            RETURN p_target_grades IS NOT NULL
               AND v_grade IS NOT NULL
               AND v_grade = ANY (p_target_grades);
        END IF;

        -- Student posts
        IF p_is_featured THEN
            RETURN p_author_grade IS NOT NULL
               AND v_grade IS NOT NULL
               AND p_author_grade = v_grade;
        END IF;

        RETURN v_class IS NOT NULL
           AND p_class_id IS NOT NULL
           AND v_class = p_class_id;
    END IF;

    RETURN false;
END;
$function$;

REVOKE ALL ON FUNCTION private.get_auth_profile_grade() FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION private.get_auth_profile_grade() TO authenticated;
GRANT EXECUTE ON FUNCTION private.get_auth_profile_grade() TO service_role;

REVOKE ALL ON FUNCTION private.get_auth_student_class_id() FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION private.get_auth_student_class_id() TO authenticated;
GRANT EXECUTE ON FUNCTION private.get_auth_student_class_id() TO service_role;

REVOKE ALL ON FUNCTION private.is_bound_teacher_of_student(uuid) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION private.is_bound_teacher_of_student(uuid) TO authenticated;
GRANT EXECUTE ON FUNCTION private.is_bound_teacher_of_student(uuid) TO service_role;

REVOKE ALL ON FUNCTION private.is_allowed_profile_grade(text) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION private.is_allowed_profile_grade(text) TO authenticated;
GRANT EXECUTE ON FUNCTION private.is_allowed_profile_grade(text) TO service_role;

REVOKE ALL ON FUNCTION private.can_read_community_post(uuid, uuid, text, text, text, text[], boolean) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION private.can_read_community_post(uuid, uuid, text, text, text, text[], boolean) TO anon, authenticated;
GRANT EXECUTE ON FUNCTION private.can_read_community_post(uuid, uuid, text, text, text, text[], boolean) TO service_role;

DROP POLICY IF EXISTS "Community posts readable by visibility" ON public.community_posts;
CREATE POLICY "Community posts readable by visibility"
    ON public.community_posts FOR SELECT
    USING (
        private.can_read_community_post(
            agency_id,
            author_id,
            author_role,
            class_id,
            author_grade,
            target_grades,
            is_featured
        )
    );

-- No direct INSERT/UPDATE/DELETE policies — writes go through RPCs only.

-- ---------------------------------------------------------------------------
-- RPCs
-- ---------------------------------------------------------------------------

CREATE OR REPLACE FUNCTION private.create_community_post(
    p_title text,
    p_body text,
    p_target_grades text[] DEFAULT NULL
)
RETURNS uuid
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path TO 'public', 'private'
AS $function$
DECLARE
    v_uid uuid := auth.uid();
    v_role public.user_role;
    v_agency uuid;
    v_title text;
    v_body text;
    v_class text;
    v_grade text;
    v_grades text[];
    v_g text;
    v_post_id uuid;
BEGIN
    IF v_uid IS NULL THEN
        RAISE EXCEPTION 'Not authenticated';
    END IF;

    v_role := private.get_auth_role();
    IF v_role IS DISTINCT FROM 'student'::public.user_role
       AND v_role IS DISTINCT FROM 'teacher'::public.user_role THEN
        RAISE EXCEPTION 'Only students and teachers can create posts';
    END IF;

    v_body := BTRIM(COALESCE(p_body, ''));
    IF v_body = '' THEN
        RAISE EXCEPTION '正文不能为空';
    END IF;
    IF char_length(v_body) > 2000 THEN
        RAISE EXCEPTION '正文不能超过 2000 字';
    END IF;

    v_title := NULLIF(BTRIM(COALESCE(p_title, '')), '');
    IF v_title IS NOT NULL AND char_length(v_title) > 80 THEN
        RAISE EXCEPTION '标题不能超过 80 字';
    END IF;

    IF v_role = 'student'::public.user_role THEN
        SELECT s.agency_id, NULLIF(BTRIM(s.class_id), '')
        INTO v_agency, v_class
        FROM public.students s
        WHERE s.id = v_uid;

        IF v_agency IS NULL THEN
            RAISE EXCEPTION '学生资料不存在';
        END IF;
        IF v_class IS NULL THEN
            RAISE EXCEPTION '请先联系机构完善班级后再发帖';
        END IF;

        SELECT p.grade INTO v_grade
        FROM public.profiles p
        WHERE p.id = v_uid;

        IF v_grade IS NULL OR NOT private.is_allowed_profile_grade(v_grade) THEN
            RAISE EXCEPTION '年级无效，请先完善个人资料';
        END IF;

        INSERT INTO public.community_posts (
            agency_id, author_id, author_role, title, body,
            class_id, author_grade, target_grades, is_featured
        )
        VALUES (
            v_agency, v_uid, 'student', v_title, v_body,
            v_class, v_grade, NULL, false
        )
        RETURNING id INTO v_post_id;

        RETURN v_post_id;
    END IF;

    -- teacher
    SELECT t.agency_id INTO v_agency
    FROM public.teachers t
    WHERE t.id = v_uid;

    IF v_agency IS NULL THEN
        RAISE EXCEPTION '教师资料不存在';
    END IF;

    IF p_target_grades IS NULL OR cardinality(p_target_grades) = 0 THEN
        RAISE EXCEPTION '请至少选择一个可见年级';
    END IF;

    v_grades := ARRAY[]::text[];
    FOREACH v_g IN ARRAY p_target_grades LOOP
        v_g := BTRIM(v_g);
        IF v_g = '' THEN
            CONTINUE;
        END IF;
        IF NOT private.is_allowed_profile_grade(v_g) THEN
            RAISE EXCEPTION '无效年级: %', v_g;
        END IF;
        IF NOT (v_g = ANY (v_grades)) THEN
            v_grades := array_append(v_grades, v_g);
        END IF;
    END LOOP;

    IF cardinality(v_grades) = 0 THEN
        RAISE EXCEPTION '请至少选择一个可见年级';
    END IF;

    INSERT INTO public.community_posts (
        agency_id, author_id, author_role, title, body,
        class_id, author_grade, target_grades, is_featured
    )
    VALUES (
        v_agency, v_uid, 'teacher', v_title, v_body,
        NULL, NULL, v_grades, false
    )
    RETURNING id INTO v_post_id;

    RETURN v_post_id;
END;
$function$;

CREATE OR REPLACE FUNCTION private.set_community_post_featured(
    p_post_id uuid,
    p_featured boolean
)
RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path TO 'public', 'private'
AS $function$
DECLARE
    v_uid uuid := auth.uid();
    v_post public.community_posts%ROWTYPE;
BEGIN
    IF v_uid IS NULL THEN
        RAISE EXCEPTION 'Not authenticated';
    END IF;

    IF private.get_auth_role() IS DISTINCT FROM 'teacher'::public.user_role THEN
        RAISE EXCEPTION 'Only teachers can set featured posts';
    END IF;

    SELECT * INTO v_post
    FROM public.community_posts
    WHERE id = p_post_id;

    IF NOT FOUND THEN
        RAISE EXCEPTION '帖子不存在';
    END IF;

    IF v_post.author_role IS DISTINCT FROM 'student' THEN
        RAISE EXCEPTION '只能将学生帖设为精华';
    END IF;

    IF NOT private.is_teacher_in_agency(v_post.agency_id) THEN
        RAISE EXCEPTION '无权操作其他机构的帖子';
    END IF;

    IF NOT private.is_bound_teacher_of_student(v_post.author_id) THEN
        RAISE EXCEPTION '只能操作名下学生的帖子';
    END IF;

    IF COALESCE(p_featured, false) THEN
        UPDATE public.community_posts
        SET is_featured = true,
            featured_by = v_uid,
            featured_at = now()
        WHERE id = p_post_id;
    ELSE
        UPDATE public.community_posts
        SET is_featured = false,
            featured_by = NULL,
            featured_at = NULL
        WHERE id = p_post_id;
    END IF;
END;
$function$;

CREATE OR REPLACE FUNCTION private.delete_community_post(p_post_id uuid)
RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path TO 'public', 'private'
AS $function$
DECLARE
    v_uid uuid := auth.uid();
    v_post public.community_posts%ROWTYPE;
BEGIN
    IF v_uid IS NULL THEN
        RAISE EXCEPTION 'Not authenticated';
    END IF;

    SELECT * INTO v_post
    FROM public.community_posts
    WHERE id = p_post_id;

    IF NOT FOUND THEN
        RAISE EXCEPTION '帖子不存在';
    END IF;

    IF v_post.author_id = v_uid THEN
        DELETE FROM public.community_posts WHERE id = p_post_id;
        RETURN;
    END IF;

    -- Binding teacher may delete student posts
    IF private.get_auth_role() = 'teacher'::public.user_role
       AND v_post.author_role = 'student'
       AND private.is_teacher_in_agency(v_post.agency_id)
       AND private.is_bound_teacher_of_student(v_post.author_id) THEN
        DELETE FROM public.community_posts WHERE id = p_post_id;
        RETURN;
    END IF;

    RAISE EXCEPTION '无权删除该帖子';
END;
$function$;

REVOKE ALL ON FUNCTION private.create_community_post(text, text, text[]) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION private.create_community_post(text, text, text[]) TO authenticated;
GRANT EXECUTE ON FUNCTION private.create_community_post(text, text, text[]) TO service_role;

REVOKE ALL ON FUNCTION private.set_community_post_featured(uuid, boolean) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION private.set_community_post_featured(uuid, boolean) TO authenticated;
GRANT EXECUTE ON FUNCTION private.set_community_post_featured(uuid, boolean) TO service_role;

REVOKE ALL ON FUNCTION private.delete_community_post(uuid) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION private.delete_community_post(uuid) TO authenticated;
GRANT EXECUTE ON FUNCTION private.delete_community_post(uuid) TO service_role;

-- public INVOKER wrappers
DROP FUNCTION IF EXISTS public.create_community_post(text, text, text[]);
CREATE OR REPLACE FUNCTION public.create_community_post(
    p_title text,
    p_body text,
    p_target_grades text[] DEFAULT NULL
)
RETURNS uuid
LANGUAGE sql
SECURITY INVOKER
SET search_path TO 'public', 'private'
AS $$
  SELECT private.create_community_post(p_title, p_body, p_target_grades);
$$;

DROP FUNCTION IF EXISTS public.set_community_post_featured(uuid, boolean);
CREATE OR REPLACE FUNCTION public.set_community_post_featured(
    p_post_id uuid,
    p_featured boolean
)
RETURNS void
LANGUAGE sql
SECURITY INVOKER
SET search_path TO 'public', 'private'
AS $$
  SELECT private.set_community_post_featured(p_post_id, p_featured);
$$;

DROP FUNCTION IF EXISTS public.delete_community_post(uuid);
CREATE OR REPLACE FUNCTION public.delete_community_post(p_post_id uuid)
RETURNS void
LANGUAGE sql
SECURITY INVOKER
SET search_path TO 'public', 'private'
AS $$
  SELECT private.delete_community_post(p_post_id);
$$;

REVOKE ALL ON FUNCTION public.create_community_post(text, text, text[]) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.create_community_post(text, text, text[]) TO authenticated;
GRANT EXECUTE ON FUNCTION public.create_community_post(text, text, text[]) TO service_role;

REVOKE ALL ON FUNCTION public.set_community_post_featured(uuid, boolean) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.set_community_post_featured(uuid, boolean) TO authenticated;
GRANT EXECUTE ON FUNCTION public.set_community_post_featured(uuid, boolean) TO service_role;

REVOKE ALL ON FUNCTION public.delete_community_post(uuid) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.delete_community_post(uuid) TO authenticated;
GRANT EXECUTE ON FUNCTION public.delete_community_post(uuid) TO service_role;
