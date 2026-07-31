-- Writing / composition prompt catalog (Zhongkao-style essay topics)

CREATE TABLE IF NOT EXISTS public.writing_prompts (
    prompt_id TEXT PRIMARY KEY,
    title TEXT NOT NULL,
    title_zh TEXT,
    grade INT NOT NULL DEFAULT 9,
    topic TEXT,
    prompt_text TEXT NOT NULL,
    prompt_text_zh TEXT,
    word_count_min INT NOT NULL DEFAULT 80,
    word_count_hint INT NOT NULL DEFAULT 100,
    max_score INT NOT NULL DEFAULT 15,
    reward_token INT NOT NULL DEFAULT 5,
    sort_order INT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 1,
    updated_at BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS writing_prompts_sort_order_idx
    ON public.writing_prompts (sort_order);

ALTER TABLE public.writing_prompts ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Anyone can view writing prompts" ON public.writing_prompts;
CREATE POLICY "Anyone can view writing prompts"
    ON public.writing_prompts FOR SELECT USING (true);

GRANT SELECT ON public.writing_prompts TO anon, authenticated;

-- Seed Zhongkao-style English writing prompts

INSERT INTO public.writing_prompts (
    prompt_id, title, title_zh, grade, topic,
    prompt_text, prompt_text_zh,
    word_count_min, word_count_hint, max_score, reward_token,
    sort_order, version, updated_at
) VALUES
(
    'w09-01',
    $seedie$An Email to a Pen Friend$seedie$,
    $seedie$给笔友的一封邮件$seedie$,
    9,
    $seedie$email$seedie$,
    $seedie$Suppose you are Li Hua. Your pen friend Tom from the UK wants to know about your school life. Write an email to him. You should include:
1. your favorite subject and why;
2. an after-school activity you enjoy;
3. one thing you hope to do with Tom if he visits China.
Write about 80–100 words. Begin with "Dear Tom," and end properly.$seedie$,
    $seedie$假设你是李华。英国笔友 Tom 想了解你的学校生活。请给他写一封邮件，内容包括：最喜欢的科目及原因；一项课外活动；如果他来中国，你希望一起做的一件事。80–100 词。$seedie$,
    80, 100, 15, 5, 1, 1, 0
),
(
    'w09-02',
    $seedie$A Day to Remember$seedie$,
    $seedie$难忘的一天$seedie$,
    9,
    $seedie$narrative$seedie$,
    $seedie$Write a short essay about a day you will never forget. You may write about:
1. when and where it happened;
2. what you did or what happened;
3. how you felt and what you learned.
Write about 80–100 words. Give your essay a suitable title.$seedie$,
    $seedie$写一篇短文，讲述你难忘的一天。可写：时间地点；发生了什么；感受与收获。80–100 词，自拟标题。$seedie$,
    80, 100, 15, 5, 2, 1, 0
),
(
    'w09-03',
    $seedie$Should Students Have More Free Time?$seedie$,
    $seedie$学生是否应有更多空闲时间$seedie$,
    9,
    $seedie$opinion$seedie$,
    $seedie$Some students think they should have more free time after school. Others think they should spend more time on study. What is your opinion? Write an essay. You should:
1. state your opinion clearly;
2. give at least two reasons;
3. give a short conclusion.
Write about 80–100 words.$seedie$,
    $seedie$有人认为放学后应有更多空闲时间，有人认为应多花时间学习。请发表你的看法：明确观点；至少两个理由；简短总结。80–100 词。$seedie$,
    80, 100, 15, 5, 3, 1, 0
),
(
    'w09-04',
    $seedie$How to Protect the Environment$seedie$,
    $seedie$如何保护环境$seedie$,
    9,
    $seedie$advice$seedie$,
    $seedie$Your school is holding an English writing competition on "How to Protect the Environment". Write an essay. You should include:
1. why protecting the environment is important;
2. two or three things students can do in daily life;
3. a call to action.
Write about 80–100 words.$seedie$,
    $seedie$学校举办以「如何保护环境」为主题的英语写作比赛。请写一篇短文：说明重要性；列举学生日常可做的两三件事；发出倡议。80–100 词。$seedie$,
    80, 100, 15, 5, 4, 1, 0
),
(
    'w09-05',
    $seedie$A Notice for the English Club$seedie$,
    $seedie$英语俱乐部活动通知$seedie$,
    9,
    $seedie$notice$seedie$,
    $seedie$You are the organizer of the school English Club. Write a notice to invite students to a weekend English Corner. Include:
1. time and place;
2. what activities there will be (e.g. free talk, games, songs);
3. who can join and how to sign up.
Write about 80–100 words. Begin with "Notice" and end with your name and date.$seedie$,
    $seedie$你是学校英语俱乐部组织者。写一则通知，邀请同学参加周末 English Corner：时间地点；活动内容；参加对象与报名方式。80–100 词。$seedie$,
    80, 100, 15, 5, 5, 1, 0
),
(
    'w09-06',
    $seedie$My Favorite Book or Film$seedie$,
    $seedie$我最喜欢的书或电影$seedie$,
    9,
    $seedie$description$seedie$,
    $seedie$Write about your favorite book or film. You should include:
1. its name and what it is about (briefly);
2. why you like it;
3. who you would recommend it to and why.
Write about 80–100 words.$seedie$,
    $seedie$写写你最喜欢的一本书或一部电影：名称与简介；喜欢的原因；推荐给谁及理由。80–100 词。$seedie$,
    80, 100, 15, 5, 6, 1, 0
),
(
    'w09-07',
    $seedie$A Reply to a Friend's Letter$seedie$,
    $seedie$给朋友的回信$seedie$,
    9,
    $seedie$letter$seedie$,
    $seedie$Your friend Mary wrote that she feels nervous about the coming exams and asked for your advice. Write a reply letter. You should:
1. show understanding;
2. give two pieces of useful advice;
3. encourage her.
Write about 80–100 words. Begin with "Dear Mary,".$seedie$,
    $seedie$朋友 Mary 来信说临近考试很紧张，向你求助。请回信：表示理解；给出两条建议；鼓励她。80–100 词。$seedie$,
    80, 100, 15, 5, 7, 1, 0
),
(
    'w09-08',
    $seedie$A Helpful Person Around Me$seedie$,
    $seedie$我身边乐于助人的人$seedie$,
    9,
    $seedie$person$seedie$,
    $seedie$Write about a person who often helps others (a classmate, teacher, or family member). Include:
1. who the person is;
2. one example of how they helped someone;
3. what you have learned from them.
Write about 80–100 words.$seedie$,
    $seedie$写一位常帮助他人的人（同学、老师或家人）：是谁；一次具体帮助的事例；你从中学到什么。80–100 词。$seedie$,
    80, 100, 15, 5, 8, 1, 0
)
ON CONFLICT (prompt_id) DO UPDATE SET
    title = EXCLUDED.title,
    title_zh = EXCLUDED.title_zh,
    grade = EXCLUDED.grade,
    topic = EXCLUDED.topic,
    prompt_text = EXCLUDED.prompt_text,
    prompt_text_zh = EXCLUDED.prompt_text_zh,
    word_count_min = EXCLUDED.word_count_min,
    word_count_hint = EXCLUDED.word_count_hint,
    max_score = EXCLUDED.max_score,
    reward_token = EXCLUDED.reward_token,
    sort_order = EXCLUDED.sort_order,
    version = EXCLUDED.version,
    updated_at = EXCLUDED.updated_at;
