-- 人教版 2024 秋新教材·七年级上册 Unit 1 You and Me 词汇 seed
-- 来源：EngResource/7上/新版/【1】7上音频（新教材）/七上新教材单词默写表/《基础词汇默写》/Unit 1 You and Me 基础词汇默写.docx
-- 后续 Unit 2-7 会用相同格式扩展；audio 单独由 upload_pep_audio.py 上传后回填。

INSERT INTO public.word_books (
    book_id, title, description, language, difficulty, grade_level,
    version, word_count, cover_url, updated_at
) VALUES (
    'pep-g7-vol1',
    '人教版 英语 七年级上册',
    '人教版 2024 秋新教材·七年级上册（Unit 1 已录入，其余 Unit 待补充）',
    'en-US',
    'mixed',
    13,
    2026080601,
    48,
    NULL,
    2026080601
)
ON CONFLICT (book_id) DO UPDATE SET
    title = EXCLUDED.title,
    description = EXCLUDED.description,
    language = EXCLUDED.language,
    grade_level = EXCLUDED.grade_level,
    version = EXCLUDED.version,
    word_count = EXCLUDED.word_count,
    updated_at = EXCLUDED.updated_at;

DELETE FROM public.word_book_modules WHERE book_id = 'pep-g7-vol1';
INSERT INTO public.word_book_modules (module_id, book_id, title, sort_order, word_count) VALUES
    ('pep-g7-vol1-u01', 'pep-g7-vol1', 'Unit 1 You and Me', 0, 48);

DELETE FROM public.vocabulary_words WHERE book_id = 'pep-g7-vol1';
INSERT INTO public.vocabulary_words (
    word_id, book_id, module_id, english, phonetic, part_of_speech,
    translation, example_sentence, difficulty_level, difficulty_value,
    reward_token, estimated_duration_sec, sort_order, audio_url, senses, master_id
) VALUES
('pep-g7-vol1-u01-0001','pep-g7-vol1','pep-g7-vol1-u01','we',NULL,'pron.','我们',NULL,'mixed',13,5,10,0,NULL,'[{"part_of_speech":"pron.","translation":"我们"}]'::jsonb,'mv-we'),
('pep-g7-vol1-u01-0002','pep-g7-vol1','pep-g7-vol1-u01','make',NULL,'v.','使成为；制造',NULL,'mixed',13,5,10,1,NULL,'[{"part_of_speech":"v.","translation":"使成为；制造"}]'::jsonb,'mv-make'),
('pep-g7-vol1-u01-0003','pep-g7-vol1','pep-g7-vol1-u01','friend',NULL,'n.','朋友',NULL,'mixed',13,5,10,2,NULL,'[{"part_of_speech":"n.","translation":"朋友"}]'::jsonb,'mv-friend'),
('pep-g7-vol1-u01-0004','pep-g7-vol1','pep-g7-vol1-u01','get',NULL,'v.','去取（或带来）；得到',NULL,'mixed',13,5,10,3,NULL,'[{"part_of_speech":"v.","translation":"去取（或带来）；得到"}]'::jsonb,'mv-get'),
('pep-g7-vol1-u01-0005','pep-g7-vol1','pep-g7-vol1-u01','know',NULL,'v.','知道',NULL,'mixed',13,5,10,4,NULL,'[{"part_of_speech":"v.","translation":"知道"}]'::jsonb,'mv-know'),
('pep-g7-vol1-u01-0006','pep-g7-vol1','pep-g7-vol1-u01','old',NULL,'adj.','老的；旧的',NULL,'mixed',13,5,10,5,NULL,'[{"part_of_speech":"adj.","translation":"老的；旧的"}]'::jsonb,'mv-old'),
('pep-g7-vol1-u01-0007','pep-g7-vol1','pep-g7-vol1-u01','from',NULL,'prep.','从……来；从……开始',NULL,'mixed',13,5,10,6,NULL,'[{"part_of_speech":"prep.","translation":"从……来；从……开始"}]'::jsonb,'mv-from'),
('pep-g7-vol1-u01-0008','pep-g7-vol1','pep-g7-vol1-u01','last',NULL,'adj.','最后的；末尾的',NULL,'mixed',13,5,10,7,NULL,'[{"part_of_speech":"adj.","translation":"最后的；末尾的"}]'::jsonb,'mv-last'),
('pep-g7-vol1-u01-0009','pep-g7-vol1','pep-g7-vol1-u01','year',NULL,'n.','年',NULL,'mixed',13,5,10,8,NULL,'[{"part_of_speech":"n.","translation":"年"}]'::jsonb,'mv-year'),
('pep-g7-vol1-u01-0010','pep-g7-vol1','pep-g7-vol1-u01','yes',NULL,'interj.','是的；可以',NULL,'mixed',13,5,10,9,NULL,'[{"part_of_speech":"interj.","translation":"是的；可以"}]'::jsonb,'mv-yes'),
('pep-g7-vol1-u01-0011','pep-g7-vol1','pep-g7-vol1-u01','Mr',NULL,'n.','（用于男子的姓氏或姓名前）先生',NULL,'mixed',13,5,10,10,NULL,'[{"part_of_speech":"n.","translation":"（用于男子的姓氏或姓名前）先生"}]'::jsonb,'mv-mr'),
('pep-g7-vol1-u01-0012','pep-g7-vol1','pep-g7-vol1-u01','our',NULL,'pron.','我们的',NULL,'mixed',13,5,10,11,NULL,'[{"part_of_speech":"pron.","translation":"我们的"}]'::jsonb,'mv-our'),
('pep-g7-vol1-u01-0013','pep-g7-vol1','pep-g7-vol1-u01','teacher',NULL,'n.','教师',NULL,'mixed',13,5,10,12,NULL,'[{"part_of_speech":"n.","translation":"教师"}]'::jsonb,'mv-teacher'),
('pep-g7-vol1-u01-0014','pep-g7-vol1','pep-g7-vol1-u01','which',NULL,'pron.','哪一个；哪一些',NULL,'mixed',13,5,10,13,NULL,'[{"part_of_speech":"pron.","translation":"哪一个；哪一些"}]'::jsonb,'mv-which'),
('pep-g7-vol1-u01-0015','pep-g7-vol1','pep-g7-vol1-u01','who',NULL,'pron.','谁；什么人',NULL,'mixed',13,5,10,14,NULL,'[{"part_of_speech":"pron.","translation":"谁；什么人"}]'::jsonb,'mv-who'),
('pep-g7-vol1-u01-0016','pep-g7-vol1','pep-g7-vol1-u01','job',NULL,'n.','工作',NULL,'mixed',13,5,10,15,NULL,'[{"part_of_speech":"n.","translation":"工作"}]'::jsonb,'mv-job'),
('pep-g7-vol1-u01-0017','pep-g7-vol1','pep-g7-vol1-u01','she',NULL,'pron.','她',NULL,'mixed',13,5,10,16,NULL,'[{"part_of_speech":"pron.","translation":"她"}]'::jsonb,'mv-she'),
('pep-g7-vol1-u01-0018','pep-g7-vol1','pep-g7-vol1-u01','favourite',NULL,'adj.','最喜欢的；最爱的',NULL,'mixed',13,5,10,17,NULL,'[{"part_of_speech":"adj.","translation":"最喜欢的；最爱的"}]'::jsonb,'mv-favourite'),
('pep-g7-vol1-u01-0019','pep-g7-vol1','pep-g7-vol1-u01','pet',NULL,'n.','宠物',NULL,'mixed',13,5,10,18,NULL,'[{"part_of_speech":"n.","translation":"宠物"}]'::jsonb,'mv-pet'),
('pep-g7-vol1-u01-0020','pep-g7-vol1','pep-g7-vol1-u01','very',NULL,'adv.','很；非常',NULL,'mixed',13,5,10,19,NULL,'[{"part_of_speech":"adv.","translation":"很；非常"}]'::jsonb,'mv-very'),
('pep-g7-vol1-u01-0021','pep-g7-vol1','pep-g7-vol1-u01','much',NULL,'pron. & adj.','许多；大量；多少',NULL,'mixed',13,5,10,20,NULL,'[{"part_of_speech":"pron. & adj.","translation":"许多；大量；多少"}]'::jsonb,'mv-much'),
('pep-g7-vol1-u01-0022','pep-g7-vol1','pep-g7-vol1-u01','cute',NULL,'adj.','可爱的',NULL,'mixed',13,5,10,21,NULL,'[{"part_of_speech":"adj.","translation":"可爱的"}]'::jsonb,'mv-cute'),
('pep-g7-vol1-u01-0023','pep-g7-vol1','pep-g7-vol1-u01','school',NULL,'n.','学校',NULL,'mixed',13,5,10,22,NULL,'[{"part_of_speech":"n.","translation":"学校"}]'::jsonb,'mv-school'),
('pep-g7-vol1-u01-0024','pep-g7-vol1','pep-g7-vol1-u01','China',NULL,'n.','中国',NULL,'mixed',13,5,10,23,NULL,'[{"part_of_speech":"n.","translation":"中国"}]'::jsonb,'mv-china'),
('pep-g7-vol1-u01-0025','pep-g7-vol1','pep-g7-vol1-u01','panda',NULL,'n.','熊猫',NULL,'mixed',13,5,10,24,NULL,'[{"part_of_speech":"n.","translation":"熊猫"}]'::jsonb,'mv-panda'),
('pep-g7-vol1-u01-0026','pep-g7-vol1','pep-g7-vol1-u01','hot',NULL,'adj.','热的；炎热的',NULL,'mixed',13,5,10,25,NULL,'[{"part_of_speech":"adj.","translation":"热的；炎热的"}]'::jsonb,'mv-hot'),
('pep-g7-vol1-u01-0027','pep-g7-vol1','pep-g7-vol1-u01','also',NULL,'adv.','也；而且',NULL,'mixed',13,5,10,26,NULL,'[{"part_of_speech":"adv.","translation":"也；而且"}]'::jsonb,'mv-also'),
('pep-g7-vol1-u01-0028','pep-g7-vol1','pep-g7-vol1-u01','live',NULL,'v.','居住；生活',NULL,'mixed',13,5,10,27,NULL,'[{"part_of_speech":"v.","translation":"居住；生活"}]'::jsonb,'mv-live'),
('pep-g7-vol1-u01-0029','pep-g7-vol1','pep-g7-vol1-u01','with',NULL,'prep.','和……在一起；带有；使用',NULL,'mixed',13,5,10,28,NULL,'[{"part_of_speech":"prep.","translation":"和……在一起；带有；使用"}]'::jsonb,'mv-with'),
('pep-g7-vol1-u01-0030','pep-g7-vol1','pep-g7-vol1-u01','parent',NULL,'n.','父（母）亲',NULL,'mixed',13,5,10,29,NULL,'[{"part_of_speech":"n.","translation":"父（母）亲"}]'::jsonb,'mv-parent'),
('pep-g7-vol1-u01-0031','pep-g7-vol1','pep-g7-vol1-u01','Chinese',NULL,'n. & adj.','中国人；汉语；中国的',NULL,'mixed',13,5,10,30,NULL,'[{"part_of_speech":"n.","translation":"中国人；汉语"},{"part_of_speech":"adj.","translation":"中国的"}]'::jsonb,'mv-chinese'),
('pep-g7-vol1-u01-0032','pep-g7-vol1','pep-g7-vol1-u01','food',NULL,'n.','食物',NULL,'mixed',13,5,10,31,NULL,'[{"part_of_speech":"n.","translation":"食物"}]'::jsonb,'mv-food'),
('pep-g7-vol1-u01-0033','pep-g7-vol1','pep-g7-vol1-u01','about',NULL,'prep. & adv.','关于；大约',NULL,'mixed',13,5,10,32,NULL,'[{"part_of_speech":"prep.","translation":"关于"},{"part_of_speech":"adv.","translation":"大约"}]'::jsonb,'mv-about'),
('pep-g7-vol1-u01-0034','pep-g7-vol1','pep-g7-vol1-u01','hour',NULL,'n.','小时',NULL,'mixed',13,5,10,33,NULL,'[{"part_of_speech":"n.","translation":"小时"}]'::jsonb,'mv-hour'),
('pep-g7-vol1-u01-0035','pep-g7-vol1','pep-g7-vol1-u01','ago',NULL,'adv.','以前',NULL,'mixed',13,5,10,34,NULL,'[{"part_of_speech":"adv.","translation":"以前"}]'::jsonb,'mv-ago'),
('pep-g7-vol1-u01-0036','pep-g7-vol1','pep-g7-vol1-u01','family',NULL,'n.','家庭',NULL,'mixed',13,5,10,35,NULL,'[{"part_of_speech":"n.","translation":"家庭"}]'::jsonb,'mv-family'),
('pep-g7-vol1-u01-0037','pep-g7-vol1','pep-g7-vol1-u01','speak',NULL,'v.','说（某种语言）；说话',NULL,'mixed',13,5,10,36,NULL,'[{"part_of_speech":"v.","translation":"说（某种语言）；说话"}]'::jsonb,'mv-speak'),
('pep-g7-vol1-u01-0038','pep-g7-vol1','pep-g7-vol1-u01','some',NULL,'adj. & pron.','一些；某些',NULL,'mixed',13,5,10,37,NULL,'[{"part_of_speech":"adj. & pron.","translation":"一些；某些"}]'::jsonb,'mv-some'),
('pep-g7-vol1-u01-0039','pep-g7-vol1','pep-g7-vol1-u01','sport',NULL,'n.','运动',NULL,'mixed',13,5,10,38,NULL,'[{"part_of_speech":"n.","translation":"运动"}]'::jsonb,'mv-sport'),
('pep-g7-vol1-u01-0040','pep-g7-vol1','pep-g7-vol1-u01','often',NULL,'adv.','时常；常常',NULL,'mixed',13,5,10,39,NULL,'[{"part_of_speech":"adv.","translation":"时常；常常"}]'::jsonb,'mv-often'),
('pep-g7-vol1-u01-0041','pep-g7-vol1','pep-g7-vol1-u01','play',NULL,'v.','玩',NULL,'mixed',13,5,10,40,NULL,'[{"part_of_speech":"v.","translation":"玩"}]'::jsonb,'mv-play'),
('pep-g7-vol1-u01-0042','pep-g7-vol1','pep-g7-vol1-u01','after',NULL,'prep. & conj.','在……以后',NULL,'mixed',13,5,10,41,NULL,'[{"part_of_speech":"prep. & conj.","translation":"在……以后"}]'::jsonb,'mv-after'),
('pep-g7-vol1-u01-0043','pep-g7-vol1','pep-g7-vol1-u01','want',NULL,'v.','想要',NULL,'mixed',13,5,10,42,NULL,'[{"part_of_speech":"v.","translation":"想要"}]'::jsonb,'mv-want'),
('pep-g7-vol1-u01-0044','pep-g7-vol1','pep-g7-vol1-u01','music',NULL,'n.','音乐',NULL,'mixed',13,5,10,43,NULL,'[{"part_of_speech":"n.","translation":"音乐"}]'::jsonb,'mv-music'),
('pep-g7-vol1-u01-0045','pep-g7-vol1','pep-g7-vol1-u01','age',NULL,'n.','年龄',NULL,'mixed',13,5,10,44,NULL,'[{"part_of_speech":"n.","translation":"年龄"}]'::jsonb,'mv-age'),
('pep-g7-vol1-u01-0046','pep-g7-vol1','pep-g7-vol1-u01','love',NULL,'v. & n.','喜爱；爱',NULL,'mixed',13,5,10,45,NULL,'[{"part_of_speech":"v. & n.","translation":"喜爱；爱"}]'::jsonb,'mv-love'),
('pep-g7-vol1-u01-0047','pep-g7-vol1','pep-g7-vol1-u01','please',NULL,'interj.','（用于客气地请求或吩咐）请',NULL,'mixed',13,5,10,46,NULL,'[{"part_of_speech":"interj.","translation":"（用于客气地请求或吩咐）请"}]'::jsonb,'mv-please'),
('pep-g7-vol1-u01-0048','pep-g7-vol1','pep-g7-vol1-u01','write',NULL,'v.','写',NULL,'mixed',13,5,10,47,NULL,'[{"part_of_speech":"v.","translation":"写"}]'::jsonb,'mv-write');

-- 刷新 master 聚合
WITH per_word AS (
    SELECT w.master_id, w.english, w.phonetic, w.senses, w.audio_url, b.grade_level
    FROM public.vocabulary_words w
    JOIN public.word_books b ON b.book_id = w.book_id
    WHERE w.master_id IS NOT NULL AND w.english IS NOT NULL AND w.english <> ''
),
agg AS (
    SELECT master_id,
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
INSERT INTO public.vocabulary_master (master_id, english, phonetic, senses, audio_url, difficulty_value, occurrence_count, is_textbook, updated_at)
SELECT master_id, english, phonetic, COALESCE(senses, '[]'::jsonb), audio_url,
    ROUND(avg_difficulty::numeric, 2), occurrences::int, is_textbook, 2026080601
FROM agg
ON CONFLICT (master_id) DO UPDATE SET
    english = EXCLUDED.english,
    phonetic = COALESCE(EXCLUDED.phonetic, public.vocabulary_master.phonetic),
    senses = CASE WHEN jsonb_array_length(EXCLUDED.senses) > 0 THEN EXCLUDED.senses ELSE public.vocabulary_master.senses END,
    audio_url = COALESCE(EXCLUDED.audio_url, public.vocabulary_master.audio_url),
    difficulty_value = EXCLUDED.difficulty_value,
    occurrence_count = EXCLUDED.occurrence_count,
    is_textbook = EXCLUDED.is_textbook,
    updated_at = EXCLUDED.updated_at;
