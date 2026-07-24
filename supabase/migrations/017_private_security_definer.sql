-- 017: Move SECURITY DEFINER implementations to private schema
-- Clears authenticated_security_definer_function_executable (0029) for exposed public API.

CREATE SCHEMA IF NOT EXISTS private;
REVOKE ALL ON SCHEMA private FROM PUBLIC;
GRANT USAGE ON SCHEMA private TO authenticated, service_role;

-- ===== private helpers / internal =====
CREATE OR REPLACE FUNCTION private.get_auth_agency_id()
 RETURNS uuid
 LANGUAGE sql
 SECURITY DEFINER
 SET search_path TO 'public', 'private'
AS $function$
  SELECT agency_id FROM public.profiles WHERE id = auth.uid() LIMIT 1;
$function$;

CREATE OR REPLACE FUNCTION private.get_auth_role()
 RETURNS user_role
 LANGUAGE sql
 SECURITY DEFINER
 SET search_path TO 'public', 'private'
AS $function$
  SELECT role FROM public.profiles WHERE id = auth.uid() LIMIT 1;
$function$;

CREATE OR REPLACE FUNCTION private.get_user_token_balance(p_user_id uuid)
 RETURNS integer
 LANGUAGE sql
 STABLE SECURITY DEFINER
 SET search_path TO 'public', 'private'
AS $function$
    SELECT COALESCE(SUM(amount), 0)::INT
    FROM public.user_economy_transactions
    WHERE user_id = p_user_id;
$function$;

CREATE OR REPLACE FUNCTION private.is_agency_admin_of(p_agency_id uuid)
 RETURNS boolean
 LANGUAGE sql
 STABLE SECURITY DEFINER
 SET search_path TO 'public', 'private'
AS $function$
    SELECT private.get_auth_role() = 'agency_admin'::public.user_role
       AND private.get_auth_agency_id() IS NOT DISTINCT FROM p_agency_id;
$function$;

CREATE OR REPLACE FUNCTION private.is_teacher_in_agency(p_agency_id uuid)
 RETURNS boolean
 LANGUAGE sql
 STABLE SECURITY DEFINER
 SET search_path TO 'public', 'private'
AS $function$
    SELECT EXISTS (
        SELECT 1
        FROM public.teachers t
        WHERE t.id = auth.uid()
          AND t.agency_id IS NOT DISTINCT FROM p_agency_id
    );
$function$;

-- ===== private product RPC bodies =====
CREATE OR REPLACE FUNCTION private.bind_student_to_teacher(p_student_id uuid, p_teacher_id uuid)
 RETURNS void
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public', 'private'
AS $function$
DECLARE
    v_caller_role public.user_role;
    v_agency_id UUID;
    v_teacher_agency UUID;
BEGIN
    v_caller_role := private.get_auth_role();

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
        IF private.get_auth_agency_id() IS DISTINCT FROM v_agency_id THEN
            RAISE EXCEPTION 'Access denied';
        END IF;
    ELSE
        RAISE EXCEPTION 'Access denied. Only agency_admin or service_role can bind students.';
    END IF;

    UPDATE public.students
    SET teacher_id = p_teacher_id
    WHERE id = p_student_id;
END;
$function$;

CREATE OR REPLACE FUNCTION private.create_student_account(p_email text, p_password text, p_agency_id uuid, p_student_name text, p_student_no text DEFAULT NULL::text, p_class_id text DEFAULT NULL::text, p_teacher_id uuid DEFAULT NULL::uuid)
 RETURNS uuid
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public', 'private'
AS $function$
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
$function$;

CREATE OR REPLACE FUNCTION private.create_teacher_account(p_email text, p_password text, p_display_name text DEFAULT NULL::text, p_agency_id uuid DEFAULT NULL::uuid)
 RETURNS uuid
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public', 'private'
AS $function$
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
$function$;

CREATE OR REPLACE FUNCTION private.get_my_token_balance()
 RETURNS integer
 LANGUAGE sql
 STABLE SECURITY DEFINER
 SET search_path TO 'public', 'private'
AS $function$
    SELECT private.get_user_token_balance(auth.uid());
$function$;

CREATE OR REPLACE FUNCTION private.get_teacher_student_stats(p_student_id uuid)
 RETURNS jsonb
 LANGUAGE plpgsql
 STABLE SECURITY DEFINER
 SET search_path TO 'public', 'private'
AS $function$
DECLARE
    v_result JSONB;
BEGIN
    IF auth.uid() IS NULL THEN
        RAISE EXCEPTION 'Not authenticated';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM public.students s
        WHERE s.id = p_student_id AND s.teacher_id = auth.uid()
    ) THEN
        RAISE EXCEPTION 'Access denied';
    END IF;

    SELECT jsonb_build_object(
        'student_id', p_student_id,
        'name', s.name,
        'student_no', s.student_no,
        'class_id', s.class_id,
        'total_check_ins', (
            SELECT COUNT(*) FROM public.user_check_ins c
            WHERE c.user_id = p_student_id AND c.is_checked_in = true
        ),
        'total_study_minutes', (
            SELECT COALESCE(SUM(c.study_time_minutes), 0)
            FROM public.user_check_ins c WHERE c.user_id = p_student_id
        ),
        'learned_word_count', (
            SELECT COALESCE(SUM(bp.learned_word_count), 0)
            FROM public.user_vocabulary_book_progress bp
            WHERE bp.user_id::text = p_student_id::text
        ),
        'token_balance', private.get_user_token_balance(p_student_id)
    ) INTO v_result
    FROM public.students s
    WHERE s.id = p_student_id;

    RETURN v_result;
END;
$function$;

CREATE OR REPLACE FUNCTION private.set_my_device_id(p_device_id text)
 RETURNS void
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public', 'private'
AS $function$
BEGIN
  IF auth.uid() IS NULL THEN
    RAISE EXCEPTION 'Not authenticated';
  END IF;

  UPDATE public.profiles
  SET
    current_device_id = p_device_id
  WHERE id = auth.uid();
END;
$function$;

CREATE OR REPLACE FUNCTION private.set_my_phone(p_phone text)
 RETURNS void
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public', 'private'
AS $function$
BEGIN
    IF auth.uid() IS NULL THEN
        RAISE EXCEPTION 'Not authenticated';
    END IF;

    UPDATE public.profiles
    SET
        phone = NULLIF(BTRIM(p_phone), ''),
        phone_verified = false,
        phone_updated_at = NOW(),
        status = COALESCE(status, 'active')
    WHERE id = auth.uid();

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Profile not found';
    END IF;
END;
$function$;

CREATE OR REPLACE FUNCTION private.set_my_profile(p_display_name text DEFAULT NULL::text, p_grade text DEFAULT NULL::text, p_avatar_tone integer DEFAULT NULL::integer)
 RETURNS void
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public', 'private'
AS $function$
DECLARE
    v_grade TEXT;
BEGIN
    IF auth.uid() IS NULL THEN
        RAISE EXCEPTION 'Not authenticated';
    END IF;

    IF p_avatar_tone IS NOT NULL AND (p_avatar_tone < 0 OR p_avatar_tone > 2) THEN
        RAISE EXCEPTION 'Invalid avatar_tone';
    END IF;

    v_grade := COALESCE(NULLIF(BTRIM(p_grade), ''), '一年级');
    IF v_grade NOT IN (
        '一年级', '二年级', '三年级', '四年级', '五年级', '六年级',
        '初一', '初二', '初三',
        '高一', '高二', '高三',
        '其他'
    ) THEN
        v_grade := '一年级';
    END IF;

    UPDATE public.profiles
    SET
        display_name = NULLIF(BTRIM(p_display_name), ''),
        grade = v_grade,
        avatar_tone = COALESCE(p_avatar_tone, avatar_tone),
        status = COALESCE(status, 'active')
    WHERE id = auth.uid();

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Profile not found';
    END IF;
END;
$function$;

CREATE OR REPLACE FUNCTION private.submit_shop_order(p_product_id uuid)
 RETURNS uuid
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public', 'private'
AS $function$
DECLARE
    v_student_id UUID;
    v_agency_id UUID;
    v_price INT;
    v_stock INT;
    v_balance INT;
    v_order_id UUID;
    v_product_name TEXT;
BEGIN
    v_student_id := auth.uid();
    IF v_student_id IS NULL THEN
        RAISE EXCEPTION 'Not authenticated';
    END IF;

    SELECT s.agency_id INTO v_agency_id
    FROM public.students s
    WHERE s.id = v_student_id;

    IF v_agency_id IS NULL THEN
        RAISE EXCEPTION 'Student has no agency';
    END IF;

    SELECT p.price_tokens, p.stock, p.name
    INTO v_price, v_stock, v_product_name
    FROM public.shop_products p
    WHERE p.id = p_product_id
      AND p.agency_id = v_agency_id
      AND p.is_active = true
    FOR UPDATE;

    IF v_price IS NULL THEN
        RAISE EXCEPTION 'Product not found or not available';
    END IF;

    IF v_stock = 0 THEN
        RAISE EXCEPTION 'Product out of stock';
    END IF;

    v_balance := private.get_user_token_balance(v_student_id);
    IF v_balance < v_price THEN
        RAISE EXCEPTION 'Insufficient tokens';
    END IF;

    UPDATE public.shop_products
    SET stock = CASE WHEN stock = -1 THEN -1 ELSE stock - 1 END
    WHERE id = p_product_id
      AND (stock = -1 OR stock > 0);

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Product out of stock';
    END IF;

    v_order_id := gen_random_uuid();

    INSERT INTO public.user_economy_transactions (id, user_id, amount, reason, ref_id)
    VALUES (
        gen_random_uuid(),
        v_student_id,
        -v_price,
        'Shop purchase: ' || v_product_name,
        v_order_id::TEXT
    );

    INSERT INTO public.shop_orders (
        id, student_id, agency_id, product_id, tokens_amount, status, resolved_at
    ) VALUES (
        v_order_id,
        v_student_id,
        v_agency_id,
        p_product_id,
        v_price,
        'completed'::public.redemption_status,
        NOW()
    );

    RETURN v_order_id;
END;
$function$;

CREATE OR REPLACE FUNCTION private.sync_my_economy_transactions(p_entries jsonb)
 RETURNS integer
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public', 'private'
AS $function$
DECLARE
    entry JSONB;
    v_uid UUID;
    v_id UUID;
    v_ref TEXT;
BEGIN
    v_uid := auth.uid();
    IF v_uid IS NULL THEN
        RAISE EXCEPTION 'Not authenticated';
    END IF;

    IF p_entries IS NULL OR jsonb_typeof(p_entries) != 'array' THEN
        RETURN private.get_user_token_balance(v_uid);
    END IF;

    FOR entry IN SELECT * FROM jsonb_array_elements(p_entries)
    LOOP
        BEGIN
            v_id := (entry->>'id')::uuid;
        EXCEPTION
            WHEN invalid_text_representation THEN
                CONTINUE;
        END;

        IF v_id IS NULL THEN
            CONTINUE;
        END IF;

        v_ref := NULLIF(TRIM(entry->>'ref_id'), '');

        BEGIN
            INSERT INTO public.user_economy_transactions (id, user_id, amount, reason, ref_id)
            VALUES (
                v_id,
                v_uid,
                (entry->>'amount')::int,
                COALESCE(NULLIF(TRIM(entry->>'reason'), ''), 'App sync'),
                v_ref
            )
            ON CONFLICT (id) DO NOTHING;
        EXCEPTION
            WHEN unique_violation THEN
                CONTINUE;
        END;
    END LOOP;

    RETURN private.get_user_token_balance(v_uid);
END;
$function$;;

-- ===== private grants =====
REVOKE ALL ON FUNCTION private.bind_student_to_teacher(p_student_id uuid, p_teacher_id uuid) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION private.bind_student_to_teacher(p_student_id uuid, p_teacher_id uuid) TO authenticated;

REVOKE ALL ON FUNCTION private.create_student_account(p_email text, p_password text, p_agency_id uuid, p_student_name text, p_student_no text, p_class_id text, p_teacher_id uuid) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION private.create_student_account(p_email text, p_password text, p_agency_id uuid, p_student_name text, p_student_no text, p_class_id text, p_teacher_id uuid) TO authenticated;

REVOKE ALL ON FUNCTION private.create_teacher_account(p_email text, p_password text, p_display_name text, p_agency_id uuid) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION private.create_teacher_account(p_email text, p_password text, p_display_name text, p_agency_id uuid) TO authenticated;

REVOKE ALL ON FUNCTION private.get_auth_agency_id() FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION private.get_auth_agency_id() TO authenticated;

REVOKE ALL ON FUNCTION private.get_auth_role() FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION private.get_auth_role() TO authenticated;

REVOKE ALL ON FUNCTION private.get_my_token_balance() FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION private.get_my_token_balance() TO authenticated;

REVOKE ALL ON FUNCTION private.get_teacher_student_stats(p_student_id uuid) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION private.get_teacher_student_stats(p_student_id uuid) TO authenticated;

REVOKE ALL ON FUNCTION private.is_agency_admin_of(p_agency_id uuid) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION private.is_agency_admin_of(p_agency_id uuid) TO authenticated;

REVOKE ALL ON FUNCTION private.is_teacher_in_agency(p_agency_id uuid) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION private.is_teacher_in_agency(p_agency_id uuid) TO authenticated;

REVOKE ALL ON FUNCTION private.set_my_device_id(p_device_id text) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION private.set_my_device_id(p_device_id text) TO authenticated;

REVOKE ALL ON FUNCTION private.set_my_phone(p_phone text) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION private.set_my_phone(p_phone text) TO authenticated;

REVOKE ALL ON FUNCTION private.set_my_profile(p_display_name text, p_grade text, p_avatar_tone integer) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION private.set_my_profile(p_display_name text, p_grade text, p_avatar_tone integer) TO authenticated;

REVOKE ALL ON FUNCTION private.submit_shop_order(p_product_id uuid) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION private.submit_shop_order(p_product_id uuid) TO authenticated;

REVOKE ALL ON FUNCTION private.sync_my_economy_transactions(p_entries jsonb) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION private.sync_my_economy_transactions(p_entries jsonb) TO authenticated;

REVOKE ALL ON FUNCTION private.get_user_token_balance(uuid) FROM PUBLIC, anon, authenticated;

GRANT EXECUTE ON FUNCTION private.create_teacher_account(text, text, text, uuid) TO service_role;
GRANT EXECUTE ON FUNCTION private.create_student_account(text, text, uuid, text, text, text, uuid) TO service_role;
GRANT EXECUTE ON FUNCTION private.bind_student_to_teacher(uuid, uuid) TO service_role;

-- ===== rewrite RLS policies to private.* helpers =====
DROP POLICY IF EXISTS "Agency admins view own agency" ON public.agencies;
CREATE POLICY "Agency admins view own agency" ON public.agencies FOR SELECT USING (((id = private.get_auth_agency_id()) AND (private.get_auth_role() = 'agency_admin'::user_role)));

DROP POLICY IF EXISTS "Company admins manage agencies" ON public.agencies;
CREATE POLICY "Company admins manage agencies" ON public.agencies FOR ALL USING ((private.get_auth_role() = 'company_admin'::user_role));

DROP POLICY IF EXISTS "Students view own agency" ON public.agencies;
CREATE POLICY "Students view own agency" ON public.agencies FOR SELECT USING (((id = private.get_auth_agency_id()) AND (private.get_auth_role() = 'student'::user_role)));

DROP POLICY IF EXISTS "Agency admins manage own agency profiles" ON public.profiles;
CREATE POLICY "Agency admins manage own agency profiles" ON public.profiles FOR ALL USING (((agency_id = private.get_auth_agency_id()) AND (private.get_auth_role() = 'agency_admin'::user_role)));

DROP POLICY IF EXISTS "Agency admins view agency profiles" ON public.profiles;
CREATE POLICY "Agency admins view agency profiles" ON public.profiles FOR SELECT USING (((private.get_auth_role() = 'agency_admin'::user_role) AND (NOT (agency_id IS DISTINCT FROM private.get_auth_agency_id()))));

DROP POLICY IF EXISTS "Company admins manage all profiles" ON public.profiles;
CREATE POLICY "Company admins manage all profiles" ON public.profiles FOR ALL USING ((private.get_auth_role() = 'company_admin'::user_role));

DROP POLICY IF EXISTS "Agency admins view agency orders" ON public.shop_orders;
CREATE POLICY "Agency admins view agency orders" ON public.shop_orders FOR SELECT USING (private.is_agency_admin_of(agency_id));

DROP POLICY IF EXISTS "Teachers view agency orders" ON public.shop_orders;
CREATE POLICY "Teachers view agency orders" ON public.shop_orders FOR SELECT USING (private.is_teacher_in_agency(agency_id));

DROP POLICY IF EXISTS "Agency admins manage agency products" ON public.shop_products;
CREATE POLICY "Agency admins manage agency products" ON public.shop_products FOR ALL USING (private.is_agency_admin_of(agency_id)) WITH CHECK (private.is_agency_admin_of(agency_id));

DROP POLICY IF EXISTS "Teachers view agency products" ON public.shop_products;
CREATE POLICY "Teachers view agency products" ON public.shop_products FOR SELECT USING (private.is_teacher_in_agency(agency_id));

DROP POLICY IF EXISTS "Agency admins manage agency students" ON public.students;
CREATE POLICY "Agency admins manage agency students" ON public.students FOR ALL USING (private.is_agency_admin_of(agency_id)) WITH CHECK (private.is_agency_admin_of(agency_id));

DROP POLICY IF EXISTS "Agency admins manage own agency students" ON public.students;
CREATE POLICY "Agency admins manage own agency students" ON public.students FOR ALL USING (((agency_id = private.get_auth_agency_id()) AND (private.get_auth_role() = 'agency_admin'::user_role)));

DROP POLICY IF EXISTS "Company admins manage all students" ON public.students;
CREATE POLICY "Company admins manage all students" ON public.students FOR ALL USING ((private.get_auth_role() = 'company_admin'::user_role));

DROP POLICY IF EXISTS "Agency admins manage agency teachers" ON public.teachers;
CREATE POLICY "Agency admins manage agency teachers" ON public.teachers FOR ALL USING (private.is_agency_admin_of(agency_id)) WITH CHECK (private.is_agency_admin_of(agency_id));

DROP POLICY IF EXISTS "Teachers view peers in agency" ON public.teachers;
CREATE POLICY "Teachers view peers in agency" ON public.teachers FOR SELECT USING (private.is_teacher_in_agency(agency_id));

DROP POLICY IF EXISTS "Agency admins view agency check_ins" ON public.user_check_ins;
CREATE POLICY "Agency admins view agency check_ins" ON public.user_check_ins FOR SELECT USING ((EXISTS ( SELECT 1
   FROM students s
  WHERE (((s.id)::text = (user_check_ins.user_id)::text) AND private.is_agency_admin_of(s.agency_id)))));

DROP POLICY IF EXISTS "Agency admins view agency economy transactions" ON public.user_economy_transactions;
CREATE POLICY "Agency admins view agency economy transactions" ON public.user_economy_transactions FOR SELECT USING ((EXISTS ( SELECT 1
   FROM students s
  WHERE (((s.id)::text = (user_economy_transactions.user_id)::text) AND private.is_agency_admin_of(s.agency_id)))));

DROP POLICY IF EXISTS "Agency admins view agency book progress" ON public.user_vocabulary_book_progress;
CREATE POLICY "Agency admins view agency book progress" ON public.user_vocabulary_book_progress FOR SELECT USING ((EXISTS ( SELECT 1
   FROM students s
  WHERE (((s.id)::text = (user_vocabulary_book_progress.user_id)::text) AND private.is_agency_admin_of(s.agency_id)))));

DROP POLICY IF EXISTS "Agency admins view agency study rounds" ON public.user_vocabulary_study_rounds;
CREATE POLICY "Agency admins view agency study rounds" ON public.user_vocabulary_study_rounds FOR SELECT USING ((EXISTS ( SELECT 1
   FROM students s
  WHERE (((s.id)::text = (user_vocabulary_study_rounds.user_id)::text) AND private.is_agency_admin_of(s.agency_id)))));

DROP POLICY IF EXISTS "Agency admins view agency word progress" ON public.user_vocabulary_word_learning_progress;
CREATE POLICY "Agency admins view agency word progress" ON public.user_vocabulary_word_learning_progress FOR SELECT USING ((EXISTS ( SELECT 1
   FROM students s
  WHERE (((s.id)::text = (user_vocabulary_word_learning_progress.user_id)::text) AND private.is_agency_admin_of(s.agency_id)))));

-- ===== drop public helpers (no longer exposed) =====
DROP FUNCTION IF EXISTS public.get_auth_agency_id();
DROP FUNCTION IF EXISTS public.get_auth_role();
DROP FUNCTION IF EXISTS public.is_agency_admin_of(p_agency_id uuid);
DROP FUNCTION IF EXISTS public.is_teacher_in_agency(p_agency_id uuid);
DROP FUNCTION IF EXISTS public.get_user_token_balance(uuid);

-- ===== public INVOKER wrappers for product RPCs =====
DROP FUNCTION IF EXISTS public.bind_student_to_teacher(p_student_id uuid, p_teacher_id uuid);
CREATE OR REPLACE FUNCTION public.bind_student_to_teacher(p_student_id uuid, p_teacher_id uuid)
 RETURNS void
 LANGUAGE sql
 SECURITY INVOKER
 SET search_path TO 'public', 'private'
AS $function$
  SELECT private.bind_student_to_teacher(p_student_id, p_teacher_id);
$function$;;
REVOKE ALL ON FUNCTION public.bind_student_to_teacher(p_student_id uuid, p_teacher_id uuid) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.bind_student_to_teacher(p_student_id uuid, p_teacher_id uuid) TO authenticated;
GRANT EXECUTE ON FUNCTION public.bind_student_to_teacher(p_student_id uuid, p_teacher_id uuid) TO service_role;

DROP FUNCTION IF EXISTS public.create_student_account(p_email text, p_password text, p_agency_id uuid, p_student_name text, p_student_no text, p_class_id text, p_teacher_id uuid);
CREATE OR REPLACE FUNCTION public.create_student_account(p_email text, p_password text, p_agency_id uuid, p_student_name text, p_student_no text, p_class_id text, p_teacher_id uuid)
 RETURNS uuid
 LANGUAGE sql
 SECURITY INVOKER
 SET search_path TO 'public', 'private'
AS $function$
  SELECT private.create_student_account(p_email, p_password, p_agency_id, p_student_name, p_student_no, p_class_id, p_teacher_id);
$function$;;
REVOKE ALL ON FUNCTION public.create_student_account(p_email text, p_password text, p_agency_id uuid, p_student_name text, p_student_no text, p_class_id text, p_teacher_id uuid) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.create_student_account(p_email text, p_password text, p_agency_id uuid, p_student_name text, p_student_no text, p_class_id text, p_teacher_id uuid) TO authenticated;
GRANT EXECUTE ON FUNCTION public.create_student_account(p_email text, p_password text, p_agency_id uuid, p_student_name text, p_student_no text, p_class_id text, p_teacher_id uuid) TO service_role;

DROP FUNCTION IF EXISTS public.create_teacher_account(p_email text, p_password text, p_display_name text, p_agency_id uuid);
CREATE OR REPLACE FUNCTION public.create_teacher_account(p_email text, p_password text, p_display_name text, p_agency_id uuid)
 RETURNS uuid
 LANGUAGE sql
 SECURITY INVOKER
 SET search_path TO 'public', 'private'
AS $function$
  SELECT private.create_teacher_account(p_email, p_password, p_display_name, p_agency_id);
$function$;;
REVOKE ALL ON FUNCTION public.create_teacher_account(p_email text, p_password text, p_display_name text, p_agency_id uuid) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.create_teacher_account(p_email text, p_password text, p_display_name text, p_agency_id uuid) TO authenticated;
GRANT EXECUTE ON FUNCTION public.create_teacher_account(p_email text, p_password text, p_display_name text, p_agency_id uuid) TO service_role;

DROP FUNCTION IF EXISTS public.get_my_token_balance();
CREATE OR REPLACE FUNCTION public.get_my_token_balance()
 RETURNS integer
 LANGUAGE sql
 SECURITY INVOKER
 SET search_path TO 'public', 'private'
AS $function$
  SELECT private.get_my_token_balance();
$function$;;
REVOKE ALL ON FUNCTION public.get_my_token_balance() FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.get_my_token_balance() TO authenticated;

DROP FUNCTION IF EXISTS public.get_teacher_student_stats(p_student_id uuid);
CREATE OR REPLACE FUNCTION public.get_teacher_student_stats(p_student_id uuid)
 RETURNS jsonb
 LANGUAGE sql
 SECURITY INVOKER
 SET search_path TO 'public', 'private'
AS $function$
  SELECT private.get_teacher_student_stats(p_student_id);
$function$;;
REVOKE ALL ON FUNCTION public.get_teacher_student_stats(p_student_id uuid) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.get_teacher_student_stats(p_student_id uuid) TO authenticated;

DROP FUNCTION IF EXISTS public.set_my_device_id(p_device_id text);
CREATE OR REPLACE FUNCTION public.set_my_device_id(p_device_id text)
 RETURNS void
 LANGUAGE sql
 SECURITY INVOKER
 SET search_path TO 'public', 'private'
AS $function$
  SELECT private.set_my_device_id(p_device_id);
$function$;;
REVOKE ALL ON FUNCTION public.set_my_device_id(p_device_id text) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.set_my_device_id(p_device_id text) TO authenticated;

DROP FUNCTION IF EXISTS public.set_my_phone(p_phone text);
CREATE OR REPLACE FUNCTION public.set_my_phone(p_phone text)
 RETURNS void
 LANGUAGE sql
 SECURITY INVOKER
 SET search_path TO 'public', 'private'
AS $function$
  SELECT private.set_my_phone(p_phone);
$function$;;
REVOKE ALL ON FUNCTION public.set_my_phone(p_phone text) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.set_my_phone(p_phone text) TO authenticated;

DROP FUNCTION IF EXISTS public.set_my_profile(p_display_name text, p_grade text, p_avatar_tone integer);
CREATE OR REPLACE FUNCTION public.set_my_profile(p_display_name text, p_grade text, p_avatar_tone integer)
 RETURNS void
 LANGUAGE sql
 SECURITY INVOKER
 SET search_path TO 'public', 'private'
AS $function$
  SELECT private.set_my_profile(p_display_name, p_grade, p_avatar_tone);
$function$;;
REVOKE ALL ON FUNCTION public.set_my_profile(p_display_name text, p_grade text, p_avatar_tone integer) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.set_my_profile(p_display_name text, p_grade text, p_avatar_tone integer) TO authenticated;

DROP FUNCTION IF EXISTS public.submit_shop_order(p_product_id uuid);
CREATE OR REPLACE FUNCTION public.submit_shop_order(p_product_id uuid)
 RETURNS uuid
 LANGUAGE sql
 SECURITY INVOKER
 SET search_path TO 'public', 'private'
AS $function$
  SELECT private.submit_shop_order(p_product_id);
$function$;;
REVOKE ALL ON FUNCTION public.submit_shop_order(p_product_id uuid) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.submit_shop_order(p_product_id uuid) TO authenticated;

DROP FUNCTION IF EXISTS public.sync_my_economy_transactions(p_entries jsonb);
CREATE OR REPLACE FUNCTION public.sync_my_economy_transactions(p_entries jsonb)
 RETURNS integer
 LANGUAGE sql
 SECURITY INVOKER
 SET search_path TO 'public', 'private'
AS $function$
  SELECT private.sync_my_economy_transactions(p_entries);
$function$;;
REVOKE ALL ON FUNCTION public.sync_my_economy_transactions(p_entries jsonb) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.sync_my_economy_transactions(p_entries jsonb) TO authenticated;

-- Point remaining public DEFINER funcs at private balance helper if present
DO $$
BEGIN
  IF EXISTS (
    SELECT 1 FROM pg_proc p
    JOIN pg_namespace n ON n.oid = p.pronamespace
    WHERE n.nspname = 'public' AND p.proname = 'reconcile_my_token_balance'
  ) THEN
    EXECUTE $f$
      CREATE OR REPLACE FUNCTION public.reconcile_my_token_balance(p_target_balance integer)
       RETURNS integer
       LANGUAGE plpgsql
       SECURITY DEFINER
       SET search_path TO 'public', 'private'
      AS $function$
      DECLARE
          v_uid UUID;
          v_current INT;
          v_diff INT;
      BEGIN
          v_uid := auth.uid();
          IF v_uid IS NULL THEN
              RAISE EXCEPTION 'Not authenticated';
          END IF;

          IF p_target_balance IS NULL OR p_target_balance < 0 THEN
              RETURN private.get_user_token_balance(v_uid);
          END IF;

          v_current := private.get_user_token_balance(v_uid);

          IF p_target_balance > v_current THEN
              v_diff := p_target_balance - v_current;
              INSERT INTO public.user_economy_transactions (user_id, amount, reason)
              VALUES (v_uid, v_diff, 'Balance reconciliation');
          END IF;

          RETURN private.get_user_token_balance(v_uid);
      END;
      $function$;
    $f$;
    REVOKE ALL ON FUNCTION public.reconcile_my_token_balance(integer) FROM PUBLIC, anon, authenticated;
    GRANT EXECUTE ON FUNCTION public.reconcile_my_token_balance(integer) TO service_role;
  END IF;
END $$;

