-- Backfill grade_level for existing FLTRP word books, populate difficulty_value on
-- their vocabulary_words rows, and (re)build vocabulary_master aggregation.

-- 1) grade_level for FLTRP books
UPDATE public.word_books SET grade_level = 13 WHERE book_id = 'fltrp-g7-vol1';
UPDATE public.word_books SET grade_level = 14 WHERE book_id = 'fltrp-g7-vol2';
UPDATE public.word_books SET grade_level = 15 WHERE book_id = 'fltrp-g8-vol1';
UPDATE public.word_books SET grade_level = 16 WHERE book_id = 'fltrp-g8-vol2';
UPDATE public.word_books SET grade_level = 17 WHERE book_id = 'fltrp-g9-vol1';
UPDATE public.word_books SET grade_level = 18 WHERE book_id = 'fltrp-g9-vol2';

-- 2) vocabulary_words.difficulty_value inherits from owning book (only when NULL)
UPDATE public.vocabulary_words w
SET difficulty_value = b.grade_level
FROM public.word_books b
WHERE w.book_id = b.book_id
  AND b.grade_level IS NOT NULL
  AND w.difficulty_value IS NULL;

-- 3) Master ID slug: lowercase, non-alnum → '-', collapse repeats, trim '-'
CREATE OR REPLACE FUNCTION public.vocabulary_master_slug(english_text TEXT)
RETURNS TEXT
LANGUAGE sql
IMMUTABLE
AS $$
    SELECT 'mv-' || trim(both '-' from regexp_replace(
        regexp_replace(lower(coalesce(english_text, '')), '[^a-z0-9]+', '-', 'g'),
        '-+', '-', 'g'
    ))
$$;

-- 4) Backfill master_id on vocabulary_words
UPDATE public.vocabulary_words
SET master_id = public.vocabulary_master_slug(english)
WHERE master_id IS NULL AND english IS NOT NULL AND english <> '';

-- 5) Rebuild vocabulary_master from current vocabulary_words + word_books
WITH per_word AS (
    SELECT
        w.master_id,
        w.english,
        w.phonetic,
        w.senses,
        w.audio_url,
        b.grade_level
    FROM public.vocabulary_words w
    JOIN public.word_books b ON b.book_id = w.book_id
    WHERE w.master_id IS NOT NULL AND w.english IS NOT NULL AND w.english <> ''
),
agg AS (
    SELECT
        master_id,
        (array_agg(english ORDER BY english))[1] AS english,
        (array_agg(phonetic) FILTER (WHERE phonetic IS NOT NULL AND phonetic <> ''))[1] AS phonetic,
        (array_agg(senses) FILTER (WHERE senses IS NOT NULL AND jsonb_array_length(senses) > 0))[1] AS senses,
        (array_agg(audio_url) FILTER (WHERE audio_url IS NOT NULL AND audio_url <> ''))[1] AS audio_url,
        AVG(grade_level) FILTER (WHERE grade_level IS NOT NULL) AS avg_difficulty,
        COUNT(*) AS occurrences,
        bool_or(grade_level IS NOT NULL) AS is_textbook
    FROM per_word
    GROUP BY master_id
)
INSERT INTO public.vocabulary_master (
    master_id, english, phonetic, senses, audio_url,
    difficulty_value, occurrence_count, is_textbook, updated_at
)
SELECT
    master_id,
    english,
    phonetic,
    COALESCE(senses, '[]'::jsonb),
    audio_url,
    ROUND(avg_difficulty::numeric, 2),
    occurrences::int,
    is_textbook,
    2026080601
FROM agg
ON CONFLICT (master_id) DO UPDATE SET
    english = EXCLUDED.english,
    phonetic = COALESCE(EXCLUDED.phonetic, public.vocabulary_master.phonetic),
    senses = CASE
        WHEN jsonb_array_length(EXCLUDED.senses) > 0 THEN EXCLUDED.senses
        ELSE public.vocabulary_master.senses
    END,
    audio_url = COALESCE(EXCLUDED.audio_url, public.vocabulary_master.audio_url),
    difficulty_value = EXCLUDED.difficulty_value,
    occurrence_count = EXCLUDED.occurrence_count,
    is_textbook = EXCLUDED.is_textbook,
    updated_at = EXCLUDED.updated_at;
