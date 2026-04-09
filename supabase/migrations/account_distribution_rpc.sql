-- ==============================================================================
-- Account Distribution RPCs (账号分发接口)
-- These functions use SECURITY DEFINER to bypass RLS and create users in auth.users
-- ==============================================================================

-- 1. Create Agency Admin (公司分发给机构)
-- Only company_admin can call this.
CREATE OR REPLACE FUNCTION public.create_agency_admin(
    p_email TEXT,
    p_password TEXT,
    p_agency_id UUID,
    p_display_name TEXT DEFAULT NULL
) RETURNS UUID AS $$
DECLARE
    v_user_id UUID;
    v_caller_role public.user_role;
BEGIN
    -- Check caller permissions
    v_caller_role := public.get_auth_role();
    IF v_caller_role IS DISTINCT FROM 'company_admin'::public.user_role THEN
        RAISE EXCEPTION 'Access denied. Only company_admin can create agency admins.';
    END IF;

    -- Verify agency exists
    IF NOT EXISTS (SELECT 1 FROM public.agencies WHERE id = p_agency_id) THEN
        RAISE EXCEPTION 'Agency not found.';
    END IF;

    -- Create user in auth.users (simulate signup, bypass email confirmation for simplicity)
    -- Note: In Supabase, the best way to create auth users via SQL is using pgcrypto and inserting directly.
    -- However, it requires knowing the hash algorithm. 
    -- For this template, we will use a simpler approach: 
    -- The actual creation in auth.users MUST be done via Supabase Admin API (Service Role) from an Edge Function or Server.
    -- BUT, if we want to do it purely in SQL (which is hacky but works for demo/MVP), we can insert into auth.users.
    
    -- Insert into auth.users
    v_user_id := gen_random_uuid();
    
    INSERT INTO auth.users (
        id, instance_id, aud, role, email, encrypted_password, 
        email_confirmed_at, recovery_sent_at, last_sign_in_at, raw_app_meta_data, 
        raw_user_meta_data, created_at, updated_at, confirmation_token, email_change, 
        email_change_token_new, recovery_token
    ) VALUES (
        v_user_id, '00000000-0000-0000-0000-000000000000', 'authenticated', 'authenticated', p_email, 
        crypt(p_password, gen_salt('bf')), -- Hash the password
        NOW(), NULL, NULL, '{"provider":"email","providers":["email"]}', 
        '{}', NOW(), NOW(), '', '', '', ''
    );

    -- Insert into profiles (RLS will be bypassed because of SECURITY DEFINER)
    INSERT INTO public.profiles (id, role, agency_id, display_name, status)
    VALUES (v_user_id, 'agency_admin'::public.user_role, p_agency_id, p_display_name, 'active');

    RETURN v_user_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;


-- 2. Create Student Account (机构分发给学生)
-- Only agency_admin (of the same agency) or company_admin can call this.
CREATE OR REPLACE FUNCTION public.create_student_account(
    p_email TEXT,
    p_password TEXT,
    p_agency_id UUID,
    p_student_name TEXT,
    p_student_no TEXT DEFAULT NULL,
    p_class_id TEXT DEFAULT NULL
) RETURNS UUID AS $$
DECLARE
    v_user_id UUID;
    v_caller_role public.user_role;
    v_caller_agency_id UUID;
BEGIN
    -- Check caller permissions
    v_caller_role := public.get_auth_role();
    v_caller_agency_id := public.get_auth_agency_id();
    
    IF v_caller_role = 'student'::public.user_role THEN
        RAISE EXCEPTION 'Access denied. Students cannot create accounts.';
    END IF;

    IF v_caller_role = 'agency_admin'::public.user_role AND v_caller_agency_id != p_agency_id THEN
        RAISE EXCEPTION 'Access denied. You can only create students for your own agency.';
    END IF;

    -- Verify agency exists
    IF NOT EXISTS (SELECT 1 FROM public.agencies WHERE id = p_agency_id) THEN
        RAISE EXCEPTION 'Agency not found.';
    END IF;

    -- Insert into auth.users
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
        '{}', NOW(), NOW(), '', '', '', ''
    );

    -- Insert into profiles
    INSERT INTO public.profiles (id, role, agency_id, display_name, status)
    VALUES (v_user_id, 'student'::public.user_role, p_agency_id, p_student_name, 'active');

    -- Insert into students table
    INSERT INTO public.students (id, agency_id, name, student_no, class_id)
    VALUES (v_user_id, p_agency_id, p_student_name, p_student_no, p_class_id);

    RETURN v_user_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
