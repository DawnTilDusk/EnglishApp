-- vocabulary_words: structured multi-POS senses
ALTER TABLE public.vocabulary_words
  ADD COLUMN IF NOT EXISTS senses JSONB NOT NULL DEFAULT '[]'::jsonb;
