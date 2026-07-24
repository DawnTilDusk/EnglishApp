-- 018: Purge residual rows for deleted agency/student accounts
-- Keep only rows tied to existing public.profiles (and shop rows tied to live students/agencies).
-- Also remove auth.users that no longer have a profile.

BEGIN;

-- Learning / economy orphans (user_id with no profile)
DELETE FROM public.user_check_ins c
WHERE NOT EXISTS (SELECT 1 FROM public.profiles p WHERE p.id = c.user_id);

DELETE FROM public.user_vocabulary_word_learning_progress x
WHERE NOT EXISTS (SELECT 1 FROM public.profiles p WHERE p.id = x.user_id);

DELETE FROM public.user_vocabulary_study_rounds x
WHERE NOT EXISTS (SELECT 1 FROM public.profiles p WHERE p.id = x.user_id);

DELETE FROM public.user_vocabulary_book_progress x
WHERE NOT EXISTS (SELECT 1 FROM public.profiles p WHERE p.id = x.user_id);

DELETE FROM public.user_economy_transactions e
WHERE NOT EXISTS (SELECT 1 FROM public.profiles p WHERE p.id = e.user_id);

-- Shop orphans (defensive; FK usually cascades)
DELETE FROM public.shop_orders o
WHERE NOT EXISTS (SELECT 1 FROM public.students s WHERE s.id = o.student_id);

DELETE FROM public.shop_orders o
WHERE o.agency_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM public.agencies a WHERE a.id = o.agency_id);

DELETE FROM public.shop_products sp
WHERE sp.agency_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM public.agencies a WHERE a.id = sp.agency_id);

-- Identity orphans: students/teachers/profiles without auth (defensive)
DELETE FROM public.students s
WHERE NOT EXISTS (SELECT 1 FROM auth.users u WHERE u.id = s.id);

DELETE FROM public.teachers t
WHERE NOT EXISTS (SELECT 1 FROM auth.users u WHERE u.id = t.id);

DELETE FROM public.profiles p
WHERE NOT EXISTS (SELECT 1 FROM auth.users u WHERE u.id = p.id);

-- Auth users left behind after profile/student deletion
DELETE FROM auth.users u
WHERE NOT EXISTS (SELECT 1 FROM public.profiles p WHERE p.id = u.id);

COMMIT;
