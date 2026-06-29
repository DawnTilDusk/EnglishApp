-- Batch sync client-side economy ledger to cloud (handles invalid UUID ids)

CREATE OR REPLACE FUNCTION public.sync_my_economy_transactions(p_entries JSONB)
RETURNS INT AS $$
DECLARE
    entry JSONB;
    v_uid UUID;
    v_id UUID;
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
            INSERT INTO public.user_economy_transactions (id, user_id, amount, reason)
            VALUES (
                v_id,
                v_uid,
                (entry->>'amount')::int,
                COALESCE(entry->>'reason', 'App sync')
            )
            ON CONFLICT (id) DO NOTHING;
        EXCEPTION
            WHEN invalid_text_representation THEN
                INSERT INTO public.user_economy_transactions (user_id, amount, reason)
                VALUES (
                    v_uid,
                    (entry->>'amount')::int,
                    COALESCE(entry->>'reason', 'App sync')
                );
        END;
    END LOOP;

    RETURN public.get_user_token_balance(v_uid);
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

GRANT EXECUTE ON FUNCTION public.sync_my_economy_transactions(JSONB) TO authenticated;
