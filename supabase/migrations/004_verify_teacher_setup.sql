-- Run in Supabase SQL Editor to verify teacher/shop setup (read-only checks)

-- 1. Tables exist
SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'public'
  AND table_name IN (
    'teachers', 'shop_products', 'shop_orders', 'user_economy_transactions',
    'user_check_ins', 'user_vocabulary_book_progress'
  )
ORDER BY table_name;

-- 2. teacher role in enum
SELECT enumlabel
FROM pg_enum e
JOIN pg_type t ON e.enumtypid = t.oid
WHERE t.typname = 'user_role'
ORDER BY enumlabel;

-- 3. RPC functions
SELECT routine_name
FROM information_schema.routines
WHERE routine_schema = 'public'
  AND routine_name IN (
    'create_teacher_account',
    'get_teacher_student_stats',
    'submit_shop_order',
    'approve_shop_order',
    'reject_shop_order'
  )
ORDER BY routine_name;

-- 4. Teachers and student bindings (visible as postgres role in SQL Editor)
SELECT t.id AS teacher_id, t.display_name, COUNT(s.id) AS student_count
FROM public.teachers t
LEFT JOIN public.students s ON s.teacher_id = t.id
GROUP BY t.id, t.display_name;

-- 5. Students missing teacher binding
SELECT s.id, s.name, s.teacher_id
FROM public.students s
WHERE s.teacher_id IS NULL;
