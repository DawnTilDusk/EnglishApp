-- Fix community feed SELECT when PostgREST runs as anon (missing/expired JWT).
-- Without EXECUTE on the RLS helper (and SELECT on the table), clients get 42501
-- instead of an empty list. Assignment policies only compare auth.uid(), so they
-- degrade quietly; community must match that UX.
--
-- can_read_community_post returns false when auth.uid() is null — no data leak.

GRANT SELECT ON TABLE public.community_posts TO anon, authenticated;
REVOKE INSERT, UPDATE, DELETE, TRUNCATE ON TABLE public.community_posts FROM anon, authenticated;

GRANT EXECUTE ON FUNCTION private.can_read_community_post(uuid, uuid, text, text, text, text[], boolean)
  TO anon, authenticated, service_role;
