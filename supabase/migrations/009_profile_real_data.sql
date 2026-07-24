ALTER TABLE public.profiles
    ADD COLUMN IF NOT EXISTS grade TEXT,
    ADD COLUMN IF NOT EXISTS avatar_tone INTEGER;

UPDATE public.profiles
SET grade = '一年级'
WHERE grade IS NULL
   OR NULLIF(BTRIM(grade), '') IS NULL
   OR BTRIM(grade) NOT IN (
       '一年级', '二年级', '三年级', '四年级', '五年级', '六年级',
       '初一', '初二', '初三',
       '高一', '高二', '高三',
       '其他'
   );

UPDATE public.profiles
SET avatar_tone = 0
WHERE avatar_tone IS NULL;

ALTER TABLE public.profiles
    ALTER COLUMN grade SET DEFAULT '一年级',
    ALTER COLUMN grade SET NOT NULL,
    ALTER COLUMN avatar_tone SET DEFAULT 0,
    ALTER COLUMN avatar_tone SET NOT NULL;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'profiles_grade_allowed_check'
          AND conrelid = 'public.profiles'::regclass
    ) THEN
        ALTER TABLE public.profiles
            DROP CONSTRAINT profiles_grade_allowed_check;
    END IF;
END;
$$;

ALTER TABLE public.profiles
    ADD CONSTRAINT profiles_grade_allowed_check
    CHECK (
        grade IN (
            '一年级', '二年级', '三年级', '四年级', '五年级', '六年级',
            '初一', '初二', '初三',
            '高一', '高二', '高三',
            '其他'
        )
    );

CREATE OR REPLACE FUNCTION public.set_my_profile(
    p_display_name TEXT DEFAULT NULL,
    p_grade TEXT DEFAULT NULL,
    p_avatar_tone INTEGER DEFAULT NULL
)
RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
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
$$;

CREATE OR REPLACE FUNCTION public.set_my_phone(p_phone TEXT)
RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
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
        status = COALESCE(status, 'active')
    WHERE id = auth.uid();

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Profile not found';
    END IF;
END;
$$;

REVOKE ALL ON FUNCTION public.set_my_profile(TEXT, TEXT, INTEGER) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.set_my_profile(TEXT, TEXT, INTEGER) TO authenticated;

REVOKE ALL ON FUNCTION public.set_my_phone(TEXT) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.set_my_phone(TEXT) TO authenticated;
