-- 016: Harden SECURITY DEFINER / trigger helpers for database linter
-- - SET search_path on mutable-path functions
-- - REVOKE EXECUTE from PUBLIC/anon (and from authenticated for non-RPC internals)
-- - Keep intentional client RPCs executable by authenticated only
-- Does not clear authenticated_security_definer (0029) for intentional product RPCs.

-- ========== search_path ==========
ALTER FUNCTION public.get_auth_role() SET search_path = public;
ALTER FUNCTION public.get_auth_agency_id() SET search_path = public;
ALTER FUNCTION public.set_my_device_id(TEXT) SET search_path = public;
ALTER FUNCTION public.sync_auth_user_email_to_profile() SET search_path = public;
ALTER FUNCTION public.get_user_token_balance(UUID) SET search_path = public;
ALTER FUNCTION public.get_my_token_balance() SET search_path = public;
ALTER FUNCTION public.enforce_student_teacher_same_agency() SET search_path = public;
ALTER FUNCTION public.is_service_role() SET search_path = public;
ALTER FUNCTION public.get_teacher_student_stats(UUID) SET search_path = public;
ALTER FUNCTION public.sync_my_economy_transactions(JSONB) SET search_path = public;
ALTER FUNCTION public.reconcile_my_token_balance(INT) SET search_path = public;

-- ========== Trigger-only: no client EXECUTE ==========
REVOKE ALL ON FUNCTION public.handle_new_user() FROM PUBLIC, anon, authenticated;
REVOKE ALL ON FUNCTION public.sync_auth_user_email_to_profile() FROM PUBLIC, anon, authenticated;
REVOKE ALL ON FUNCTION public.sync_student_to_profile() FROM PUBLIC, anon, authenticated;
REVOKE ALL ON FUNCTION public.enforce_student_teacher_same_agency() FROM PUBLIC, anon, authenticated;

-- ========== Internal helpers (not product RPC) ==========
-- get_user_token_balance: still callable from DEFINER owners; clients use get_my_token_balance
REVOKE ALL ON FUNCTION public.get_user_token_balance(UUID) FROM PUBLIC, anon, authenticated;

-- is_service_role: only used inside other DEFINER RPCs
REVOKE ALL ON FUNCTION public.is_service_role() FROM PUBLIC, anon, authenticated;

-- reconcile: service_role only (re-assert 012)
REVOKE ALL ON FUNCTION public.reconcile_my_token_balance(INT) FROM PUBLIC, anon, authenticated;
GRANT EXECUTE ON FUNCTION public.reconcile_my_token_balance(INT) TO service_role;

-- ========== RLS helpers: authenticated needs EXECUTE for policies; no anon ==========
REVOKE ALL ON FUNCTION public.get_auth_role() FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.get_auth_role() TO authenticated;

REVOKE ALL ON FUNCTION public.get_auth_agency_id() FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.get_auth_agency_id() TO authenticated;

REVOKE ALL ON FUNCTION public.is_agency_admin_of(UUID) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.is_agency_admin_of(UUID) TO authenticated;

REVOKE ALL ON FUNCTION public.is_teacher_in_agency(UUID) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.is_teacher_in_agency(UUID) TO authenticated;

-- ========== Client / agency product RPCs: authenticated (+ service_role where already used) ==========
REVOKE ALL ON FUNCTION public.get_my_token_balance() FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.get_my_token_balance() TO authenticated;

REVOKE ALL ON FUNCTION public.sync_my_economy_transactions(JSONB) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.sync_my_economy_transactions(JSONB) TO authenticated;

REVOKE ALL ON FUNCTION public.submit_shop_order(UUID) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.submit_shop_order(UUID) TO authenticated;

REVOKE ALL ON FUNCTION public.get_teacher_student_stats(UUID) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.get_teacher_student_stats(UUID) TO authenticated;

REVOKE ALL ON FUNCTION public.set_my_device_id(TEXT) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.set_my_device_id(TEXT) TO authenticated;

REVOKE ALL ON FUNCTION public.set_my_phone(TEXT) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.set_my_phone(TEXT) TO authenticated;

REVOKE ALL ON FUNCTION public.set_my_profile(TEXT, TEXT, INTEGER) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.set_my_profile(TEXT, TEXT, INTEGER) TO authenticated;

REVOKE ALL ON FUNCTION public.create_teacher_account(TEXT, TEXT, TEXT, UUID) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.create_teacher_account(TEXT, TEXT, TEXT, UUID)
    TO authenticated, service_role;

REVOKE ALL ON FUNCTION public.create_student_account(TEXT, TEXT, UUID, TEXT, TEXT, TEXT, UUID)
    FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.create_student_account(TEXT, TEXT, UUID, TEXT, TEXT, TEXT, UUID)
    TO authenticated, service_role;

REVOKE ALL ON FUNCTION public.bind_student_to_teacher(UUID, UUID) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.bind_student_to_teacher(UUID, UUID)
    TO authenticated, service_role;

-- create_agency_admin: re-assert service_role only
DO $$
BEGIN
    REVOKE ALL ON FUNCTION public.create_agency_admin(TEXT, TEXT, UUID, TEXT)
        FROM PUBLIC, anon, authenticated;
    GRANT EXECUTE ON FUNCTION public.create_agency_admin(TEXT, TEXT, UUID, TEXT) TO service_role;
EXCEPTION
    WHEN undefined_function THEN
        RAISE NOTICE '016: create_agency_admin not present; skip';
END $$;

-- ========== Storage: public bucket word-audio must not allow listing ==========
-- Public object URLs still work without a broad SELECT policy on storage.objects.
DROP POLICY IF EXISTS "public read audio" ON storage.objects;
