-- Vocabulary estimate history (one row per quiz estimate).
-- profiles.vocabulary_size remains the latest estimate; history drives garden trend.

CREATE TABLE IF NOT EXISTS public.user_vocabulary_estimates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    vocabulary_size INT NOT NULL CHECK (vocabulary_size >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS user_vocabulary_estimates_user_created_idx
    ON public.user_vocabulary_estimates (user_id, created_at ASC);

ALTER TABLE public.user_vocabulary_estimates ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Users read own vocabulary estimates"
    ON public.user_vocabulary_estimates;
CREATE POLICY "Users read own vocabulary estimates"
    ON public.user_vocabulary_estimates
    FOR SELECT
    USING (auth.uid() = user_id);

GRANT SELECT ON public.user_vocabulary_estimates TO authenticated;

-- Keep latest on profiles and append a history row.
CREATE OR REPLACE FUNCTION public.set_my_vocabulary_estimate(p_size INT)
RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
  IF auth.uid() IS NULL THEN
    RAISE EXCEPTION 'Not authenticated';
  END IF;
  IF p_size IS NULL OR p_size < 0 THEN
    RAISE EXCEPTION 'invalid vocabulary estimate';
  END IF;
  UPDATE public.profiles
  SET vocabulary_size = p_size,
      vocabulary_estimated_at = NOW()
  WHERE id = auth.uid();

  INSERT INTO public.user_vocabulary_estimates (user_id, vocabulary_size)
  VALUES (auth.uid(), p_size);
END;
$$;

GRANT EXECUTE ON FUNCTION public.set_my_vocabulary_estimate(INT) TO authenticated;

-- Seed one history row from existing profile estimates (pre-history last value).
INSERT INTO public.user_vocabulary_estimates (user_id, vocabulary_size, created_at)
SELECT p.id, p.vocabulary_size, p.vocabulary_estimated_at
FROM public.profiles p
WHERE p.vocabulary_estimated_at IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM public.user_vocabulary_estimates e
      WHERE e.user_id = p.id
  );
