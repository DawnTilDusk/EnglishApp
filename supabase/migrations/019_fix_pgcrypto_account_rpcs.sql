-- 019: Make pgcrypto (extensions.gen_salt / crypt) visible to account-creation RPCs.
-- Root cause: DEFINER functions use search_path = public[, private] only;
-- on this project pgcrypto lives in schema `extensions`, so gen_salt('bf') fails
-- with "function gen_salt(unknown) does not exist".

CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA extensions;

ALTER FUNCTION private.create_teacher_account(text, text, text, uuid)
  SET search_path TO public, private, extensions;

ALTER FUNCTION private.create_student_account(text, text, uuid, text, text, text, uuid)
  SET search_path TO public, private, extensions;

ALTER FUNCTION public.create_agency_admin(text, text, uuid, text)
  SET search_path TO public, extensions;

-- Belt-and-suspenders: schema-qualify crypt/gen_salt inside DEFINER bodies
-- and ensure auth.identities exists for email password login.

CREATE OR REPLACE FUNCTION private.create_student_account(
    p_email text,
    p_password text,
    p_agency_id uuid,
    p_student_name text,
    p_student_no text DEFAULT NULL::text,
    p_class_id text DEFAULT NULL::text,
    p_teacher_id uuid DEFAULT NULL::uuid
)
RETURNS uuid
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path TO public, private, extensions
AS $$
DECLARE
    v_user_id UUID;
    v_caller_role public.user_role;
    v_caller_agency_id UUID;
BEGIN
    v_caller_role := private.get_auth_role();
    v_caller_agency_id := private.get_auth_agency_id();

    IF public.is_service_role() THEN
        NULL;
    ELSIF v_caller_role = 'agency_admin'::public.user_role THEN
        IF v_caller_agency_id IS DISTINCT FROM p_agency_id THEN
            RAISE EXCEPTION 'Access denied. You can only create students for your own agency.';
        END IF;
    ELSE
        RAISE EXCEPTION 'Access denied. Only agency_admin or service_role can create students.';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM public.agencies WHERE id = p_agency_id) THEN
        RAISE EXCEPTION 'Agency not found.';
    END IF;

    IF p_teacher_id IS NOT NULL THEN
        IF NOT EXISTS (
            SELECT 1 FROM public.teachers t
            WHERE t.id = p_teacher_id AND t.agency_id = p_agency_id
        ) THEN
            RAISE EXCEPTION 'Teacher not found in this agency.';
        END IF;
    END IF;

    v_user_id := gen_random_uuid();

    INSERT INTO auth.users (
        id, instance_id, aud, role, email, encrypted_password,
        email_confirmed_at, recovery_sent_at, last_sign_in_at, raw_app_meta_data,
        raw_user_meta_data, created_at, updated_at, confirmation_token, email_change,
        email_change_token_new, recovery_token
    ) VALUES (
        v_user_id, '00000000-0000-0000-0000-000000000000', 'authenticated', 'authenticated', p_email,
        extensions.crypt(p_password, extensions.gen_salt('bf')),
        NOW(), NULL, NULL, '{"provider":"email","providers":["email"]}',
        jsonb_build_object('role', 'student', 'agency_id', p_agency_id::text),
        NOW(), NOW(), '', '', '', ''
    );

    INSERT INTO auth.identities (
        id, user_id, identity_data, provider, provider_id, last_sign_in_at, created_at, updated_at
    ) VALUES (
        v_user_id,
        v_user_id,
        jsonb_build_object('sub', v_user_id::text, 'email', p_email),
        'email',
        v_user_id::text,
        NOW(), NOW(), NOW()
    )
    ON CONFLICT DO NOTHING;

    INSERT INTO public.profiles (id, role, agency_id, display_name, status, email)
    VALUES (
        v_user_id,
        'student'::public.user_role,
        p_agency_id,
        p_student_name,
        'inactive',
        p_email
    )
    ON CONFLICT (id) DO UPDATE
    SET role = EXCLUDED.role,
        agency_id = EXCLUDED.agency_id,
        display_name = EXCLUDED.display_name,
        status = EXCLUDED.status,
        email = EXCLUDED.email;

    INSERT INTO public.students (id, agency_id, name, student_no, class_id, teacher_id)
    VALUES (v_user_id, p_agency_id, p_student_name, p_student_no, p_class_id, p_teacher_id)
    ON CONFLICT (id) DO UPDATE
    SET agency_id = EXCLUDED.agency_id,
        name = EXCLUDED.name,
        student_no = EXCLUDED.student_no,
        class_id = EXCLUDED.class_id,
        teacher_id = EXCLUDED.teacher_id;

    RETURN v_user_id;
END;
$$;

CREATE OR REPLACE FUNCTION private.create_teacher_account(
    p_email text,
    p_password text,
    p_display_name text DEFAULT NULL::text,
    p_agency_id uuid DEFAULT NULL::uuid
)
RETURNS uuid
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path TO public, private, extensions
AS $$
DECLARE
    v_user_id UUID;
    v_display_name TEXT;
    v_agency_id UUID;
    v_caller_role public.user_role;
BEGIN
    v_caller_role := private.get_auth_role();
    v_display_name := COALESCE(p_display_name, p_email);

    IF public.is_service_role() THEN
        v_agency_id := p_agency_id;
        IF v_agency_id IS NULL THEN
            RAISE EXCEPTION 'p_agency_id is required when calling as service_role';
        END IF;
    ELSIF v_caller_role = 'agency_admin'::public.user_role THEN
        v_agency_id := private.get_auth_agency_id();
        IF v_agency_id IS NULL THEN
            RAISE EXCEPTION 'Agency admin has no agency_id';
        END IF;
        IF p_agency_id IS NOT NULL AND p_agency_id IS DISTINCT FROM v_agency_id THEN
            RAISE EXCEPTION 'Access denied. Cannot create teachers for another agency.';
        END IF;
    ELSE
        RAISE EXCEPTION 'Access denied. Only agency_admin or service_role can create teachers.';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM public.agencies WHERE id = v_agency_id) THEN
        RAISE EXCEPTION 'Agency not found.';
    END IF;

    v_user_id := gen_random_uuid();

    INSERT INTO auth.users (
        id, instance_id, aud, role, email, encrypted_password,
        email_confirmed_at, recovery_sent_at, last_sign_in_at, raw_app_meta_data,
        raw_user_meta_data, created_at, updated_at, confirmation_token, email_change,
        email_change_token_new, recovery_token
    ) VALUES (
        v_user_id, '00000000-0000-0000-0000-000000000000', 'authenticated', 'authenticated', p_email,
        extensions.crypt(p_password, extensions.gen_salt('bf')),
        NOW(), NULL, NULL, '{"provider":"email","providers":["email"]}',
        jsonb_build_object('role', 'teacher', 'agency_id', v_agency_id::text),
        NOW(), NOW(), '', '', '', ''
    );

    INSERT INTO auth.identities (
        id, user_id, identity_data, provider, provider_id, last_sign_in_at, created_at, updated_at
    ) VALUES (
        v_user_id,
        v_user_id,
        jsonb_build_object('sub', v_user_id::text, 'email', p_email),
        'email',
        v_user_id::text,
        NOW(), NOW(), NOW()
    )
    ON CONFLICT DO NOTHING;

    INSERT INTO public.profiles (id, role, agency_id, display_name, status, email)
    VALUES (
        v_user_id,
        'teacher'::public.user_role,
        v_agency_id,
        v_display_name,
        'active',
        p_email
    )
    ON CONFLICT (id) DO UPDATE
    SET role = EXCLUDED.role,
        agency_id = EXCLUDED.agency_id,
        display_name = EXCLUDED.display_name,
        status = EXCLUDED.status,
        email = EXCLUDED.email;

    DELETE FROM public.students WHERE id = v_user_id;

    INSERT INTO public.teachers (id, display_name, agency_id)
    VALUES (v_user_id, v_display_name, v_agency_id)
    ON CONFLICT (id) DO UPDATE
    SET display_name = EXCLUDED.display_name,
        agency_id = EXCLUDED.agency_id;

    RETURN v_user_id;
END;
$$;

CREATE OR REPLACE FUNCTION public.create_agency_admin(
    p_email TEXT,
    p_password TEXT,
    p_agency_id UUID,
    p_display_name TEXT DEFAULT NULL
) RETURNS UUID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path TO public, extensions
AS $$
DECLARE
    v_user_id UUID;
BEGIN
    IF NOT public.is_service_role() THEN
        RAISE EXCEPTION 'Access denied. Only service_role can create agency admins.';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM public.agencies WHERE id = p_agency_id) THEN
        RAISE EXCEPTION 'Agency not found.';
    END IF;

    v_user_id := gen_random_uuid();

    INSERT INTO auth.users (
        id, instance_id, aud, role, email, encrypted_password,
        email_confirmed_at, recovery_sent_at, last_sign_in_at, raw_app_meta_data,
        raw_user_meta_data, created_at, updated_at, confirmation_token, email_change,
        email_change_token_new, recovery_token
    ) VALUES (
        v_user_id, '00000000-0000-0000-0000-000000000000', 'authenticated', 'authenticated', p_email,
        extensions.crypt(p_password, extensions.gen_salt('bf')),
        NOW(), NULL, NULL, '{"provider":"email","providers":["email"]}',
        jsonb_build_object('role', 'agency_admin', 'agency_id', p_agency_id::text),
        NOW(), NOW(), '', '', '', ''
    );

    INSERT INTO auth.identities (
        id, user_id, identity_data, provider, provider_id, last_sign_in_at, created_at, updated_at
    ) VALUES (
        v_user_id,
        v_user_id,
        jsonb_build_object('sub', v_user_id::text, 'email', p_email),
        'email',
        v_user_id::text,
        NOW(), NOW(), NOW()
    )
    ON CONFLICT DO NOTHING;

    INSERT INTO public.profiles (id, role, agency_id, display_name, status, email)
    VALUES (
        v_user_id,
        'agency_admin'::public.user_role,
        p_agency_id,
        COALESCE(p_display_name, p_email),
        'active',
        p_email
    )
    ON CONFLICT (id) DO UPDATE
    SET role = EXCLUDED.role,
        agency_id = EXCLUDED.agency_id,
        display_name = EXCLUDED.display_name,
        status = EXCLUDED.status,
        email = EXCLUDED.email;

    DELETE FROM public.students WHERE id = v_user_id;

    RETURN v_user_id;
END;
$$;

REVOKE ALL ON FUNCTION public.create_agency_admin(TEXT, TEXT, UUID, TEXT) FROM PUBLIC, anon, authenticated;
GRANT EXECUTE ON FUNCTION public.create_agency_admin(TEXT, TEXT, UUID, TEXT) TO service_role;
