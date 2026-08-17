-- Fix FLTRP woman: plural/phonetic note was stored as the Chinese gloss.
UPDATE public.vocabulary_words
SET translation = '女人',
    senses = '[{"part_of_speech":"n.","translation":"女人"}]'::jsonb
WHERE word_id = 'fltrp-g7-vol1-0202'
   OR (book_id LIKE 'fltrp-%' AND lower(english) = 'woman');
