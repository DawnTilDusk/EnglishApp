-- RPC for syncing client-side economy transactions and querying authoritative balance

CREATE OR REPLACE FUNCTION public.get_my_token_balance()
RETURNS INT AS $$
    SELECT public.get_user_token_balance(auth.uid());
$$ LANGUAGE sql STABLE SECURITY DEFINER;

CREATE OR REPLACE FUNCTION public.record_my_economy_transaction(
    p_id UUID,
    p_amount INT,
    p_reason TEXT,
    p_ref_id TEXT DEFAULT NULL
) RETURNS VOID AS $$
BEGIN
    IF auth.uid() IS NULL THEN
        RAISE EXCEPTION 'Not authenticated';
    END IF;

    INSERT INTO public.user_economy_transactions (id, user_id, amount, reason, ref_id)
    VALUES (p_id, auth.uid(), p_amount, p_reason, p_ref_id)
    ON CONFLICT (id) DO NOTHING;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

GRANT EXECUTE ON FUNCTION public.get_my_token_balance() TO authenticated;
GRANT EXECUTE ON FUNCTION public.record_my_economy_transaction(UUID, INT, TEXT, TEXT) TO authenticated;
