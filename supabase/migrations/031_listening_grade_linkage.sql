-- Link listening materials to a source word_book/grade for grade-based navigation
-- and speaking-module reuse. All new columns are NULL-allowed so existing
-- rows (e.g. l08-01) remain valid.

ALTER TABLE public.listening_materials
    ADD COLUMN IF NOT EXISTS book_id TEXT;

ALTER TABLE public.listening_materials
    ADD COLUMN IF NOT EXISTS grade_level INT;

ALTER TABLE public.listening_materials
    ADD COLUMN IF NOT EXISTS unit_ref TEXT;

ALTER TABLE public.listening_materials
    ADD COLUMN IF NOT EXISTS section_ref TEXT;

CREATE INDEX IF NOT EXISTS listening_materials_book_grade_sort_idx
    ON public.listening_materials (book_id, grade_level, sort_order);
