-- Reconcile cloud token balance up to client-reported local target (after batch sync gap)

CREATE OR REPLACE FUNCTION public.reconcile_my_token_balance(p_target_balance INT)
RETURNS INT AS $$
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
        RETURN public.get_user_token_balance(v_uid);
    END IF;

    v_current := public.get_user_token_balance(v_uid);

    IF p_target_balance > v_current THEN
        v_diff := p_target_balance - v_current;
        INSERT INTO public.user_economy_transactions (user_id, amount, reason)
        VALUES (v_uid, v_diff, 'Balance reconciliation');
    END IF;

    RETURN public.get_user_token_balance(v_uid);
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

GRANT EXECUTE ON FUNCTION public.reconcile_my_token_balance(INT) TO authenticated;
