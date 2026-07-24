-- 012: Harden account RPCs, bind student, revoke reconcile, fix signup triggers

-- Drop old signatures before recreate (parameter lists changed)
DROP FUNCTION IF EXISTS public.create_agency_admin(TEXT, TEXT, UUID, TEXT);
DROP FUNCTION IF EXISTS public.create_teacher_account(TEXT, TEXT, TEXT);
DROP FUNCTION IF EXISTS public.create_teacher_account(TEXT, TEXT, TEXT, UUID);
DROP FUNCTION IF EXISTS public.create_student_account(TEXT, TEXT, UUID, TEXT, TEXT, TEXT);
DROP FUNCTION IF EXISTS public.create_student_account(TEXT, TEXT, UUID, TEXT, TEXT, TEXT, UUID);

-- ---------- create_agency_admin: service_role only ----------
CREATE OR REPLACE FUNCTION public.create_agency_admin(
    p_email TEXT,
    p_password TEXT,
    p_agency_id UUID,
    p_display_name TEXT DEFAULT NULL
) RETURNS UUID AS $$
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
        crypt(p_password, gen_salt('bf')),
        NOW(), NULL, NULL, '{"provider":"email","providers":["email"]}',
        jsonb_build_object('role', 'agency_admin', 'agency_id', p_agency_id::text),
        NOW(), NOW(), '', '', '', ''
    );

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
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public;

REVOKE ALL ON FUNCTION public.create_agency_admin(TEXT, TEXT, UUID, TEXT) FROM PUBLIC, anon, authenticated;
GRANT EXECUTE ON FUNCTION public.create_agency_admin(TEXT, TEXT, UUID, TEXT) TO service_role;

-- ---------- create_teacher_account: agency_admin (own agency) or service_role ----------
CREATE OR REPLACE FUNCTION public.create_teacher_account(
    p_email TEXT,
    p_password TEXT,
    p_display_name TEXT DEFAULT NULL,
    p_agency_id UUID DEFAULT NULL
) RETURNS UUID AS $$
DECLARE
    v_user_id UUID;
    v_display_name TEXT;
    v_agency_id UUID;
    v_caller_role public.user_role;
BEGIN
    v_caller_role := public.get_auth_role();
    v_display_name := COALESCE(p_display_name, p_email);

    IF public.is_service_role() THEN
        v_agency_id := p_agency_id;
        IF v_agency_id IS NULL THEN
            RAISE EXCEPTION 'p_agency_id is required when calling as service_role';
        END IF;
    ELSIF v_caller_role = 'agency_admin'::public.user_role THEN
        v_agency_id := public.get_auth_agency_id();
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
        crypt(p_password, gen_salt('bf')),
        NOW(), NULL, NULL, '{"provider":"email","providers":["email"]}',
        jsonb_build_object('role', 'teacher', 'agency_id', v_agency_id::text),
        NOW(), NOW(), '', '', '', ''
    );

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
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public;

GRANT EXECUTE ON FUNCTION public.create_teacher_account(TEXT, TEXT, TEXT, UUID) TO authenticated, service_role;

-- ---------- create_student_account: optional teacher bind ----------
CREATE OR REPLACE FUNCTION public.create_student_account(
    p_email TEXT,
    p_password TEXT,
    p_agency_id UUID,
    p_student_name TEXT,
    p_student_no TEXT DEFAULT NULL,
    p_class_id TEXT DEFAULT NULL,
    p_teacher_id UUID DEFAULT NULL
) RETURNS UUID AS $$
DECLARE
    v_user_id UUID;
    v_caller_role public.user_role;
    v_caller_agency_id UUID;
BEGIN
    v_caller_role := public.get_auth_role();
    v_caller_agency_id := public.get_auth_agency_id();

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
        crypt(p_password, gen_salt('bf')),
        NOW(), NULL, NULL, '{"provider":"email","providers":["email"]}',
        jsonb_build_object('role', 'student', 'agency_id', p_agency_id::text),
        NOW(), NOW(), '', '', '', ''
    );

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
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public;

GRANT EXECUTE ON FUNCTION public.create_student_account(TEXT, TEXT, UUID, TEXT, TEXT, TEXT, UUID)
    TO authenticated, service_role;

-- ---------- bind_student_to_teacher ----------
CREATE OR REPLACE FUNCTION public.bind_student_to_teacher(
    p_student_id UUID,
    p_teacher_id UUID
) RETURNS VOID AS $$
DECLARE
    v_caller_role public.user_role;
    v_agency_id UUID;
    v_teacher_agency UUID;
BEGIN
    v_caller_role := public.get_auth_role();

    SELECT agency_id INTO v_agency_id FROM public.students WHERE id = p_student_id;
    IF v_agency_id IS NULL THEN
        RAISE EXCEPTION 'Student not found';
    END IF;

    SELECT agency_id INTO v_teacher_agency FROM public.teachers WHERE id = p_teacher_id;
    IF v_teacher_agency IS NULL THEN
        RAISE EXCEPTION 'Teacher not found';
    END IF;

    IF v_agency_id IS DISTINCT FROM v_teacher_agency THEN
        RAISE EXCEPTION 'Student and teacher must belong to the same agency';
    END IF;

    IF public.is_service_role() THEN
        NULL;
    ELSIF v_caller_role = 'agency_admin'::public.user_role THEN
        IF public.get_auth_agency_id() IS DISTINCT FROM v_agency_id THEN
            RAISE EXCEPTION 'Access denied';
        END IF;
    ELSE
        RAISE EXCEPTION 'Access denied. Only agency_admin or service_role can bind students.';
    END IF;

    UPDATE public.students
    SET teacher_id = p_teacher_id
    WHERE id = p_student_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public;

GRANT EXECUTE ON FUNCTION public.bind_student_to_teacher(UUID, UUID) TO authenticated, service_role;

-- ---------- Revoke client-side balance minting ----------
DO $$
BEGIN
    REVOKE ALL ON FUNCTION public.reconcile_my_token_balance(INT) FROM PUBLIC, anon, authenticated;
    GRANT EXECUTE ON FUNCTION public.reconcile_my_token_balance(INT) TO service_role;
EXCEPTION
    WHEN undefined_function THEN
        RAISE NOTICE 'reconcile_my_token_balance not present; skip revoke';
END $$;

-- ---------- Signup trigger: minimal profile only; no auto student unless meta says so ----------
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS trigger AS $$
DECLARE
    v_role TEXT;
    v_agency_id UUID;
    v_default_name TEXT;
BEGIN
    v_default_name := split_part(new.email, '@', 1);
    IF v_default_name IS NULL OR v_default_name = '' THEN
        v_default_name := 'New User';
    END IF;

    v_role := coalesce(new.raw_user_meta_data ->> 'role', '');
    BEGIN
        v_agency_id := NULLIF(new.raw_user_meta_data ->> 'agency_id', '')::uuid;
    EXCEPTION WHEN OTHERS THEN
        v_agency_id := NULL;
    END;

    IF v_role = 'agency_admin' AND v_agency_id IS NOT NULL THEN
        INSERT INTO public.profiles (id, role, status, display_name, agency_id, email)
        VALUES (new.id, 'agency_admin'::public.user_role, 'active', v_default_name, v_agency_id, new.email)
        ON CONFLICT (id) DO UPDATE
        SET email = EXCLUDED.email;
        RETURN new;
    END IF;

    IF v_role = 'teacher' AND v_agency_id IS NOT NULL THEN
        INSERT INTO public.profiles (id, role, status, display_name, agency_id, email)
        VALUES (new.id, 'teacher'::public.user_role, 'active', v_default_name, v_agency_id, new.email)
        ON CONFLICT (id) DO UPDATE
        SET email = EXCLUDED.email;
        RETURN new;
    END IF;

    IF v_role = 'student' AND v_agency_id IS NOT NULL THEN
        INSERT INTO public.profiles (id, role, status, display_name, agency_id, email)
        VALUES (new.id, 'student'::public.user_role, 'inactive', v_default_name, v_agency_id, new.email)
        ON CONFLICT (id) DO UPDATE
        SET email = EXCLUDED.email;

        INSERT INTO public.students (id, name, agency_id)
        VALUES (new.id, v_default_name, v_agency_id)
        ON CONFLICT (id) DO NOTHING;
        RETURN new;
    END IF;

    -- Fallback: profile placeholder only (no students row, no hardcoded agency)
    INSERT INTO public.profiles (id, role, status, display_name, agency_id, email)
    VALUES (new.id, 'student'::public.user_role, 'inactive', v_default_name, NULL, new.email)
    ON CONFLICT (id) DO UPDATE
    SET email = EXCLUDED.email;

    RETURN new;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public;

-- Do not force role=student over teacher/agency profiles
CREATE OR REPLACE FUNCTION public.sync_student_to_profile()
RETURNS trigger AS $$
BEGIN
    UPDATE public.profiles
    SET
        agency_id = NEW.agency_id,
        display_name = NEW.name
        -- intentionally do NOT overwrite role
    WHERE id = NEW.id
      AND role = 'student'::public.user_role;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public;
