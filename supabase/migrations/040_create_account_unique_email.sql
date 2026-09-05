-- 040: Friendly unique-email handling for account-creation RPCs.
-- auth.users.email is unique via users_email_partial_key (non-SSO).
-- Duplicate inserts previously bubbled the raw Postgres constraint name to Web.

CREATE OR REPLACE FUNCTION private.normalize_unused_auth_email(p_email text)
RETURNS text
LANGUAGE plpgsql
STABLE
SECURITY DEFINER
SET search_path TO public, private
AS $$
DECLARE
    v_email text;
BEGIN
    v_email := lower(trim(COALESCE(p_email, '')));
    IF v_email = '' OR position('@' IN v_email) = 0 THEN
        RAISE EXCEPTION '请填写有效邮箱。';
    END IF;
    IF EXISTS (
        SELECT 1
        FROM auth.users u
        WHERE u.email IS NOT NULL
          AND lower(u.email::text) = v_email
    ) THEN
        RAISE EXCEPTION '该邮箱已被使用。';
    END IF;
    RETURN v_email;
END;
$$;

REVOKE ALL ON FUNCTION private.normalize_unused_auth_email(text) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION private.normalize_unused_auth_email(text) TO authenticated, service_role;

CREATE OR REPLACE FUNCTION public.create_student_account(
    p_email text,
    p_password text,
    p_agency_id uuid,
    p_student_name text,
    p_student_no text,
    p_class_id text,
    p_teacher_id uuid
)
RETURNS uuid
LANGUAGE plpgsql
SECURITY INVOKER
SET search_path TO public, private
AS $$
DECLARE
    v_email text;
    v_id uuid;
BEGIN
    v_email := private.normalize_unused_auth_email(p_email);
    BEGIN
        v_id := private.create_student_account(
            v_email, p_password, p_agency_id, p_student_name, p_student_no, p_class_id, p_teacher_id
        );
    EXCEPTION
        WHEN unique_violation THEN
            RAISE EXCEPTION '该邮箱已被使用。';
    END;
    RETURN v_id;
END;
$$;

REVOKE ALL ON FUNCTION public.create_student_account(text, text, uuid, text, text, text, uuid)
    FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.create_student_account(text, text, uuid, text, text, text, uuid)
    TO authenticated, service_role;

CREATE OR REPLACE FUNCTION public.create_teacher_account(
    p_email text,
    p_password text,
    p_display_name text,
    p_agency_id uuid
)
RETURNS uuid
LANGUAGE plpgsql
SECURITY INVOKER
SET search_path TO public, private
AS $$
DECLARE
    v_email text;
    v_id uuid;
BEGIN
    v_email := private.normalize_unused_auth_email(p_email);
    BEGIN
        v_id := private.create_teacher_account(v_email, p_password, p_display_name, p_agency_id);
    EXCEPTION
        WHEN unique_violation THEN
            RAISE EXCEPTION '该邮箱已被使用。';
    END;
    RETURN v_id;
END;
$$;

REVOKE ALL ON FUNCTION public.create_teacher_account(text, text, text, uuid) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.create_teacher_account(text, text, text, uuid)
    TO authenticated, service_role;
