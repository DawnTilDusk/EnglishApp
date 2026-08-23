-- Snapshot author display name on community posts (students cannot read peers' profiles).

ALTER TABLE public.community_posts
    ADD COLUMN IF NOT EXISTS author_display_name TEXT;

UPDATE public.community_posts p
SET author_display_name = COALESCE(
    (
        SELECT NULLIF(BTRIM(s.name), '')
        FROM public.students s
        WHERE s.id = p.author_id
    ),
    (
        SELECT NULLIF(BTRIM(pr.display_name), '')
        FROM public.profiles pr
        WHERE pr.id = p.author_id
    ),
    '用户'
)
WHERE author_display_name IS NULL;

ALTER TABLE public.community_posts
    ALTER COLUMN author_display_name SET DEFAULT '用户';

UPDATE public.community_posts
SET author_display_name = '用户'
WHERE author_display_name IS NULL;

ALTER TABLE public.community_posts
    ALTER COLUMN author_display_name SET NOT NULL;

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
    v_display text;
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
        SELECT s.agency_id, NULLIF(BTRIM(s.class_id), ''), NULLIF(BTRIM(s.name), '')
        INTO v_agency, v_class, v_display
        FROM public.students s
        WHERE s.id = v_uid;

        IF v_agency IS NULL THEN
            RAISE EXCEPTION '学生资料不存在';
        END IF;
        IF v_class IS NULL THEN
            RAISE EXCEPTION '请先联系机构完善班级后再发帖';
        END IF;

        IF v_display IS NULL THEN
            SELECT NULLIF(BTRIM(p.display_name), '') INTO v_display
            FROM public.profiles p
            WHERE p.id = v_uid;
        END IF;
        v_display := COALESCE(v_display, '同学');

        SELECT p.grade INTO v_grade
        FROM public.profiles p
        WHERE p.id = v_uid;

        IF v_grade IS NULL OR NOT private.is_allowed_profile_grade(v_grade) THEN
            RAISE EXCEPTION '年级无效，请先完善个人资料';
        END IF;

        INSERT INTO public.community_posts (
            agency_id, author_id, author_role, author_display_name, title, body,
            class_id, author_grade, target_grades, is_featured
        )
        VALUES (
            v_agency, v_uid, 'student', v_display, v_title, v_body,
            v_class, v_grade, NULL, false
        )
        RETURNING id INTO v_post_id;

        RETURN v_post_id;
    END IF;

    -- teacher
    SELECT t.agency_id, NULLIF(BTRIM(t.display_name), '')
    INTO v_agency, v_display
    FROM public.teachers t
    WHERE t.id = v_uid;

    IF v_agency IS NULL THEN
        RAISE EXCEPTION '教师资料不存在';
    END IF;

    IF v_display IS NULL THEN
        SELECT NULLIF(BTRIM(p.display_name), '') INTO v_display
        FROM public.profiles p
        WHERE p.id = v_uid;
    END IF;
    v_display := COALESCE(v_display, '老师');

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
        agency_id, author_id, author_role, author_display_name, title, body,
        class_id, author_grade, target_grades, is_featured
    )
    VALUES (
        v_agency, v_uid, 'teacher', v_display, v_title, v_body,
        NULL, NULL, v_grades, false
    )
    RETURNING id INTO v_post_id;

    RETURN v_post_id;
END;
$function$;
