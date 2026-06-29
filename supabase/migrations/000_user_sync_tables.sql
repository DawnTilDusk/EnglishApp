-- App sync tables (Phase 1-2). Safe to re-run with IF NOT EXISTS.

CREATE TABLE IF NOT EXISTS public.user_check_ins (
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    date TEXT NOT NULL,
    is_checked_in BOOLEAN NOT NULL DEFAULT false,
    study_time_minutes INT NOT NULL DEFAULT 0,
    PRIMARY KEY (user_id, date)
);
ALTER TABLE public.user_check_ins ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Users manage own check_ins" ON public.user_check_ins;
CREATE POLICY "Users manage own check_ins" ON public.user_check_ins
    FOR ALL USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);

CREATE TABLE IF NOT EXISTS public.user_vocabulary_word_learning_progress (
    user_id TEXT NOT NULL,
    book_id TEXT NOT NULL,
    word_id TEXT NOT NULL,
    status TEXT NOT NULL,
    last_studied_at BIGINT NOT NULL,
    learned_at BIGINT,
    PRIMARY KEY (user_id, book_id, word_id)
);
ALTER TABLE public.user_vocabulary_word_learning_progress ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Users manage own word progress" ON public.user_vocabulary_word_learning_progress;
CREATE POLICY "Users manage own word progress" ON public.user_vocabulary_word_learning_progress
    FOR ALL USING (auth.uid()::text = user_id) WITH CHECK (auth.uid()::text = user_id);

CREATE TABLE IF NOT EXISTS public.user_vocabulary_study_rounds (
    round_id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL,
    book_id TEXT NOT NULL,
    status TEXT NOT NULL,
    target_word_count INT NOT NULL,
    introduced_word_count INT NOT NULL,
    mastered_word_count INT NOT NULL,
    active_queue_size INT NOT NULL,
    next_word_sort_order_cursor INT NOT NULL,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
);
ALTER TABLE public.user_vocabulary_study_rounds ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Users manage own study rounds" ON public.user_vocabulary_study_rounds;
CREATE POLICY "Users manage own study rounds" ON public.user_vocabulary_study_rounds
    FOR ALL USING (auth.uid()::text = user_id) WITH CHECK (auth.uid()::text = user_id);

CREATE TABLE IF NOT EXISTS public.user_vocabulary_book_progress (
    user_id TEXT NOT NULL,
    book_id TEXT NOT NULL,
    next_word_sort_order_cursor INT NOT NULL,
    active_round_id TEXT,
    learned_word_count INT NOT NULL,
    updated_at BIGINT NOT NULL,
    PRIMARY KEY (user_id, book_id)
);
ALTER TABLE public.user_vocabulary_book_progress ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Users manage own book progress" ON public.user_vocabulary_book_progress;
CREATE POLICY "Users manage own book progress" ON public.user_vocabulary_book_progress
    FOR ALL USING (auth.uid()::text = user_id) WITH CHECK (auth.uid()::text = user_id);
