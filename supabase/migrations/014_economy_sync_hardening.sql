-- 014: Harden economy sync — UUID-only idempotent inserts + business ref_id uniqueness

-- Drop duplicate (user_id, ref_id) rows before unique index (keep earliest created_at)
WITH ranked AS (
    SELECT
        ctid,
        ROW_NUMBER() OVER (
            PARTITION BY user_id, ref_id
            ORDER BY created_at ASC NULLS LAST, id ASC
        ) AS rn
    FROM public.user_economy_transactions
    WHERE ref_id IS NOT NULL
)
DELETE FROM public.user_economy_transactions t
USING ranked r
WHERE t.ctid = r.ctid
  AND r.rn > 1;

CREATE UNIQUE INDEX IF NOT EXISTS user_economy_transactions_user_id_ref_id_uidx
    ON public.user_economy_transactions (user_id, ref_id)
    WHERE ref_id IS NOT NULL;

CREATE OR REPLACE FUNCTION public.sync_my_economy_transactions(p_entries JSONB)
RETURNS INT AS $$
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
        RETURN public.get_user_token_balance(v_uid);
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
                -- (user_id, ref_id) already present under a different id — skip
                CONTINUE;
        END;
    END LOOP;

    RETURN public.get_user_token_balance(v_uid);
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

GRANT EXECUTE ON FUNCTION public.sync_my_economy_transactions(JSONB) TO authenticated;
