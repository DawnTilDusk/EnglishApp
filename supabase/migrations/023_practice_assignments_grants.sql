-- Grant authenticated SELECT on practice assignment tables.
-- RLS policies alone are not enough; without table grants PostgREST
-- returns permission denied and the teacher UI shows an empty list.

GRANT SELECT ON public.practice_assignments TO authenticated;
GRANT SELECT ON public.practice_assignment_items TO authenticated;
GRANT SELECT ON public.practice_assignment_recipients TO authenticated;
GRANT SELECT ON public.practice_assignment_submissions TO authenticated;
