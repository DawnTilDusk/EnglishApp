-- 015: Drop unused legacy multi-tenant content/points/rewards tables and unused RPCs.
--
-- DROP:
--   Tables: content_assignments, redemptions, content_items, rewards, study_events, points_ledger
--   Functions: purchase_shop_product(UUID), record_my_economy_transaction(UUID, INT, TEXT, TEXT)
--
-- KEEP (do not touch):
--   agencies, word_books / word_book_modules / vocabulary_words,
--   shop_orders / shop_products / user_economy_transactions,
--   submit_shop_order, sync_my_economy_transactions, reconcile_my_token_balance,
--   redemption_status enum (still used by shop_orders.status), company_admin enum value
--
-- Before applying on production, run the preflight counts below. Expected: all 0.
--
-- SELECT 'content_assignments' AS t, COUNT(*) FROM public.content_assignments
-- UNION ALL SELECT 'redemptions', COUNT(*) FROM public.redemptions
-- UNION ALL SELECT 'content_items', COUNT(*) FROM public.content_items
-- UNION ALL SELECT 'rewards', COUNT(*) FROM public.rewards
-- UNION ALL SELECT 'study_events', COUNT(*) FROM public.study_events
-- UNION ALL SELECT 'points_ledger', COUNT(*) FROM public.points_ledger;

-- Unused alias / single-row RPC (clients use submit_shop_order / sync_my_economy_transactions)
DROP FUNCTION IF EXISTS public.purchase_shop_product(UUID);
DROP FUNCTION IF EXISTS public.record_my_economy_transaction(UUID, INT, TEXT, TEXT);

-- Children first, then parents (CASCADE also drops table RLS policies)
DROP TABLE IF EXISTS public.content_assignments CASCADE;
DROP TABLE IF EXISTS public.redemptions CASCADE;
DROP TABLE IF EXISTS public.content_items CASCADE;
DROP TABLE IF EXISTS public.rewards CASCADE;
DROP TABLE IF EXISTS public.study_events CASCADE;
DROP TABLE IF EXISTS public.points_ledger CASCADE;
