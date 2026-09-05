-- 041: Binding a phone activates the student profile.
-- 017's COALESCE(status, 'active') left created students as 'inactive' after set_my_phone.

CREATE OR REPLACE FUNCTION private.set_my_phone(p_phone text)
RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path TO public, private
AS $$
BEGIN
    IF auth.uid() IS NULL THEN
        RAISE EXCEPTION 'Not authenticated';
    END IF;

    UPDATE public.profiles
    SET
        phone = NULLIF(BTRIM(p_phone), ''),
        phone_verified = false,
        phone_updated_at = NOW(),
        status = 'active'
    WHERE id = auth.uid();

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Profile not found';
    END IF;
END;
$$;

UPDATE public.profiles
SET status = 'active'
WHERE role = 'student'
  AND phone IS NOT NULL
  AND BTRIM(phone) <> ''
  AND status IS DISTINCT FROM 'active';
