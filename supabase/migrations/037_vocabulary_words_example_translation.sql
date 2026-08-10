-- vocabulary_words: 例句中文翻译
-- 例句本身走既有的 example_sentence 列，这里只补它对应的中文，
-- 让背单词的例句提示可以中英对照展示。
ALTER TABLE public.vocabulary_words
  ADD COLUMN IF NOT EXISTS example_translation TEXT;
