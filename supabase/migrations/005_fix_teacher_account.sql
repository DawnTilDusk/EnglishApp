-- Fix create_teacher_account to avoid conflict with auth trigger placeholder profile.
-- This migration also normalizes existing teacher accounts created before this fix.

CREATE OR REPLACE FUNCTION public.create_teacher_account(
    p_email TEXT,
    p_password TEXT,
    p_display_name TEXT DEFAULT NULL
) RETURNS UUID AS $$
DECLARE
    v_user_id UUID;
    v_display_name TEXT;
BEGIN
    v_user_id := gen_random_uuid();
    v_display_name := COALESCE(p_display_name, p_email);

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

    -- handle_new_user trigger may have inserted a placeholder student profile first.
    INSERT INTO public.profiles (id, role, display_name, status, email)
    VALUES (
        v_user_id,
        'teacher'::public.user_role,
        v_display_name,
        'active',
        p_email
    )
    ON CONFLICT (id) DO UPDATE
    SET role = EXCLUDED.role,
        display_name = EXCLUDED.display_name,
        status = EXCLUDED.status,
        email = EXCLUDED.email;

    -- Remove placeholder student row created by trigger for teacher account.
    DELETE FROM public.students
    WHERE id = v_user_id;

    INSERT INTO public.teachers (id, display_name)
    VALUES (v_user_id, v_display_name)
    ON CONFLICT (id) DO UPDATE
    SET display_name = EXCLUDED.display_name;

    RETURN v_user_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- One-time repair for existing teacher rows that were left with student role.
UPDATE public.profiles p
SET role = 'teacher'::public.user_role,
    status = COALESCE(p.status, 'active')
WHERE p.id IN (SELECT t.id FROM public.teachers t)
  AND p.role <> 'teacher'::public.user_role;
