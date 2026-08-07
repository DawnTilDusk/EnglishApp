-- Add numeric grade-based difficulty to word books and vocabulary_words,
-- and introduce vocabulary_master as a cross-book unique-word index.

ALTER TABLE public.word_books
    ADD COLUMN IF NOT EXISTS grade_level INT;

ALTER TABLE public.vocabulary_words
    ADD COLUMN IF NOT EXISTS difficulty_value INT;

ALTER TABLE public.vocabulary_words
    ADD COLUMN IF NOT EXISTS master_id TEXT;

CREATE INDEX IF NOT EXISTS vocabulary_words_master_id_idx
    ON public.vocabulary_words (master_id);

CREATE TABLE IF NOT EXISTS public.vocabulary_master (
    master_id TEXT PRIMARY KEY,
    english TEXT NOT NULL UNIQUE,
    phonetic TEXT,
    senses JSONB NOT NULL DEFAULT '[]'::jsonb,
    audio_url TEXT,
    difficulty_value NUMERIC(4, 2),
    occurrence_count INT NOT NULL DEFAULT 0,
    is_textbook BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS vocabulary_master_english_idx
    ON public.vocabulary_master (english);

CREATE INDEX IF NOT EXISTS vocabulary_master_difficulty_idx
    ON public.vocabulary_master (difficulty_value);

ALTER TABLE public.vocabulary_master ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Anyone can view vocabulary master" ON public.vocabulary_master;
CREATE POLICY "Anyone can view vocabulary master"
    ON public.vocabulary_master FOR SELECT USING (true);

GRANT SELECT ON public.vocabulary_master TO anon, authenticated;
