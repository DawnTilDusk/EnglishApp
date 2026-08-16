-- vocabulary_words: 例句中文翻译，与 example_sentence 成对使用
ALTER TABLE public.vocabulary_words
  ADD COLUMN IF NOT EXISTS example_translation TEXT;
