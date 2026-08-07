-- 人教版 7 上 Unit 1 听力材料条目（audio_url 先留空，由 upload_pep_audio.py 上传后回填）
-- 来源：EngResource/7上/新版/【1】7上音频（新教材）/【2】7上听力+单词（细分）/

INSERT INTO public.listening_materials (
    material_id, title, title_zh, material_type, prompt_text, transcript,
    audio_url, estimated_seconds, sort_order, version, updated_at,
    book_id, grade_level, unit_ref, section_ref
) VALUES
('pep-g7-vol1-u1-seca-1b', 'Unit 1 · Section A 1b', '第一单元 · A部分 1b',
 'dialogue', 'Listen to the audio and follow along.', NULL,
 NULL, 30, 0, 1, 2026080601,
 'pep-g7-vol1', 13, 'Unit 1', 'Section A 1b'),
('pep-g7-vol1-u1-seca-1c', 'Unit 1 · Section A 1c', '第一单元 · A部分 1c',
 'dialogue', 'Listen to the audio and follow along.', NULL,
 NULL, 30, 1, 1, 2026080601,
 'pep-g7-vol1', 13, 'Unit 1', 'Section A 1c'),
('pep-g7-vol1-u1-pr-1', 'Unit 1 · Pronunciation 1', '第一单元 · 语音 1',
 'pronunciation', 'Listen to the pronunciation practice.', NULL,
 NULL, 20, 2, 1, 2026080601,
 'pep-g7-vol1', 13, 'Unit 1', 'Pronunciation 1'),
('pep-g7-vol1-u1-pr-2', 'Unit 1 · Pronunciation 2', '第一单元 · 语音 2',
 'pronunciation', 'Listen to the pronunciation practice.', NULL,
 NULL, 20, 3, 1, 2026080601,
 'pep-g7-vol1', 13, 'Unit 1', 'Pronunciation 2'),
('pep-g7-vol1-u1-seca-2a', 'Unit 1 · Section A 2a', '第一单元 · A部分 2a',
 'dialogue', 'Listen to the audio and follow along.', NULL,
 NULL, 45, 4, 1, 2026080601,
 'pep-g7-vol1', 13, 'Unit 1', 'Section A 2a'),
('pep-g7-vol1-u1-seca-2d', 'Unit 1 · Section A 2d', '第一单元 · A部分 2d',
 'dialogue', 'Listen to the audio and follow along.', NULL,
 NULL, 45, 5, 1, 2026080601,
 'pep-g7-vol1', 13, 'Unit 1', 'Section A 2d'),
('pep-g7-vol1-u1-secb-1b', 'Unit 1 · Section B 1b', '第一单元 · B部分 1b',
 'dialogue', 'Listen to the audio and follow along.', NULL,
 NULL, 45, 6, 1, 2026080601,
 'pep-g7-vol1', 13, 'Unit 1', 'Section B 1b')
ON CONFLICT (material_id) DO UPDATE SET
    title = EXCLUDED.title,
    title_zh = EXCLUDED.title_zh,
    material_type = EXCLUDED.material_type,
    prompt_text = EXCLUDED.prompt_text,
    estimated_seconds = EXCLUDED.estimated_seconds,
    sort_order = EXCLUDED.sort_order,
    version = EXCLUDED.version,
    updated_at = EXCLUDED.updated_at,
    book_id = EXCLUDED.book_id,
    grade_level = EXCLUDED.grade_level,
    unit_ref = EXCLUDED.unit_ref,
    section_ref = EXCLUDED.section_ref;
