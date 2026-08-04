-- profiles: vocabulary size from quiz estimate (not practice mastery)
ALTER TABLE public.profiles
  ADD COLUMN IF NOT EXISTS vocabulary_size INT NOT NULL DEFAULT 0;

ALTER TABLE public.profiles
  ADD COLUMN IF NOT EXISTS vocabulary_estimated_at TIMESTAMPTZ;

CREATE OR REPLACE FUNCTION public.set_my_vocabulary_estimate(p_size INT)
RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
  IF p_size IS NULL OR p_size < 0 THEN
    RAISE EXCEPTION 'invalid vocabulary estimate';
  END IF;
  UPDATE public.profiles
  SET vocabulary_size = p_size,
      vocabulary_estimated_at = NOW()
  WHERE id = auth.uid();
END;
$$;

GRANT EXECUTE ON FUNCTION public.set_my_vocabulary_estimate(INT) TO authenticated;
