-- 人教版 2024 秋新教材·七年级上册 Unit 2-7 词汇 seed
-- 来源：EngResource/7上/新版/【1】7上音频（新教材）/七上新教材单词默写表/《基础词汇默写》/*.docx

-- 更新 word_count：Unit1(48) + Unit2(53) + Unit3(33) + Unit4(25) + Unit5(17) + Unit6(12) + Unit7(19) = 207
UPDATE public.word_books
SET word_count = 207,
    description = '人教版 2024 秋新教材·七年级上册（Unit 1-7 完整）',
    version = 2026080602,
    updated_at = 2026080602
WHERE book_id = 'pep-g7-vol1';

INSERT INTO public.word_book_modules (module_id, book_id, title, sort_order, word_count) VALUES
    ('pep-g7-vol1-u02', 'pep-g7-vol1', 'Unit 2 We''re Family!',       1, 53),
    ('pep-g7-vol1-u03', 'pep-g7-vol1', 'Unit 3 My School',            2, 33),
    ('pep-g7-vol1-u04', 'pep-g7-vol1', 'Unit 4 My Favourite Subject', 3, 25),
    ('pep-g7-vol1-u05', 'pep-g7-vol1', 'Unit 5 Fun Clubs',            4, 17),
    ('pep-g7-vol1-u06', 'pep-g7-vol1', 'Unit 6 A Day in the Life',    5, 12),
    ('pep-g7-vol1-u07', 'pep-g7-vol1', 'Unit 7 Happy Birthday!',      6, 19)
ON CONFLICT (module_id) DO UPDATE SET
    title = EXCLUDED.title,
    sort_order = EXCLUDED.sort_order,
    word_count = EXCLUDED.word_count;

-- Unit 2 We're Family!  (sort_order 48-100)
INSERT INTO public.vocabulary_words (
    word_id, book_id, module_id, english, phonetic, part_of_speech,
    translation, example_sentence, difficulty_level, difficulty_value,
    reward_token, estimated_duration_sec, sort_order, audio_url, senses, master_id
) VALUES
('pep-g7-vol1-u02-0049','pep-g7-vol1','pep-g7-vol1-u02','or',NULL,'conj.','或者；也不（用于否定句）',NULL,'mixed',13,5,10,48,NULL,'[{"part_of_speech":"conj.","translation":"或者；也不（用于否定句）"}]'::jsonb,'mv-or'),
('pep-g7-vol1-u02-0050','pep-g7-vol1','pep-g7-vol1-u02','mother',NULL,'n.','母亲',NULL,'mixed',13,5,10,49,NULL,'[{"part_of_speech":"n.","translation":"母亲"}]'::jsonb,'mv-mother'),
('pep-g7-vol1-u02-0051','pep-g7-vol1','pep-g7-vol1-u02','cousin',NULL,'n.','堂兄（弟、姊、妹）；表兄（弟、姊、妹）',NULL,'mixed',13,5,10,50,NULL,'[{"part_of_speech":"n.","translation":"堂兄（弟、姊、妹）；表兄（弟、姊、妹）"}]'::jsonb,'mv-cousin'),
('pep-g7-vol1-u02-0052','pep-g7-vol1','pep-g7-vol1-u02','child',NULL,'n.','儿童；小孩',NULL,'mixed',13,5,10,51,NULL,'[{"part_of_speech":"n.","translation":"儿童；小孩"}]'::jsonb,'mv-child'),
('pep-g7-vol1-u02-0053','pep-g7-vol1','pep-g7-vol1-u02','aunt',NULL,'n.','姑（姨、伯、婶、舅）母',NULL,'mixed',13,5,10,52,NULL,'[{"part_of_speech":"n.","translation":"姑（姨、伯、婶、舅）母"}]'::jsonb,'mv-aunt'),
('pep-g7-vol1-u02-0054','pep-g7-vol1','pep-g7-vol1-u02','sister',NULL,'n.','姐；妹',NULL,'mixed',13,5,10,53,NULL,'[{"part_of_speech":"n.","translation":"姐；妹"}]'::jsonb,'mv-sister'),
('pep-g7-vol1-u02-0055','pep-g7-vol1','pep-g7-vol1-u02','grandmother',NULL,'n.','奶奶；外婆',NULL,'mixed',13,5,10,54,NULL,'[{"part_of_speech":"n.","translation":"奶奶；外婆"}]'::jsonb,'mv-grandmother'),
('pep-g7-vol1-u02-0056','pep-g7-vol1','pep-g7-vol1-u02','brother',NULL,'n.','兄；弟',NULL,'mixed',13,5,10,55,NULL,'[{"part_of_speech":"n.","translation":"兄；弟"}]'::jsonb,'mv-brother'),
('pep-g7-vol1-u02-0057','pep-g7-vol1','pep-g7-vol1-u02','grandfather',NULL,'n.','爷爷；外公',NULL,'mixed',13,5,10,56,NULL,'[{"part_of_speech":"n.","translation":"爷爷；外公"}]'::jsonb,'mv-grandfather'),
('pep-g7-vol1-u02-0058','pep-g7-vol1','pep-g7-vol1-u02','come',NULL,'v.','来；来到',NULL,'mixed',13,5,10,57,NULL,'[{"part_of_speech":"v.","translation":"来；来到"}]'::jsonb,'mv-come'),
('pep-g7-vol1-u02-0059','pep-g7-vol1','pep-g7-vol1-u02','ping-pong',NULL,'n.','乒乓球运动',NULL,'mixed',13,5,10,58,NULL,'[{"part_of_speech":"n.","translation":"乒乓球运动"}]'::jsonb,'mv-ping-pong'),
('pep-g7-vol1-u02-0060','pep-g7-vol1','pep-g7-vol1-u02','whose',NULL,'pron.','谁的',NULL,'mixed',13,5,10,59,NULL,'[{"part_of_speech":"pron.","translation":"谁的"}]'::jsonb,'mv-whose'),
('pep-g7-vol1-u02-0061','pep-g7-vol1','pep-g7-vol1-u02','well',NULL,'interj. & adv. & adj.','嗯；好吧；令人满意地；健康的',NULL,'mixed',13,5,10,60,NULL,'[{"part_of_speech":"interj.","translation":"嗯；好吧"},{"part_of_speech":"adv.","translation":"好；令人满意地"},{"part_of_speech":"adj.","translation":"健康的"}]'::jsonb,'mv-well'),
('pep-g7-vol1-u02-0062','pep-g7-vol1','pep-g7-vol1-u02','every',NULL,'adj.','每一；每个',NULL,'mixed',13,5,10,61,NULL,'[{"part_of_speech":"adj.","translation":"每一；每个"}]'::jsonb,'mv-every'),
('pep-g7-vol1-u02-0063','pep-g7-vol1','pep-g7-vol1-u02','day',NULL,'n.','一天；白天',NULL,'mixed',13,5,10,62,NULL,'[{"part_of_speech":"n.","translation":"一天；白天"}]'::jsonb,'mv-day'),
('pep-g7-vol1-u02-0064','pep-g7-vol1','pep-g7-vol1-u02','week',NULL,'n.','周',NULL,'mixed',13,5,10,63,NULL,'[{"part_of_speech":"n.","translation":"周"}]'::jsonb,'mv-week'),
('pep-g7-vol1-u02-0065','pep-g7-vol1','pep-g7-vol1-u02','fish',NULL,'v. & n.','钓鱼；鱼；鱼肉',NULL,'mixed',13,5,10,64,NULL,'[{"part_of_speech":"v.","translation":"钓鱼"},{"part_of_speech":"n.","translation":"鱼；鱼肉"}]'::jsonb,'mv-fish'),
('pep-g7-vol1-u02-0066','pep-g7-vol1','pep-g7-vol1-u02','father',NULL,'n.','父亲；爸爸',NULL,'mixed',13,5,10,65,NULL,'[{"part_of_speech":"n.","translation":"父亲；爸爸"}]'::jsonb,'mv-father'),
('pep-g7-vol1-u02-0067','pep-g7-vol1','pep-g7-vol1-u02','piano',NULL,'n.','钢琴',NULL,'mixed',13,5,10,66,NULL,'[{"part_of_speech":"n.","translation":"钢琴"}]'::jsonb,'mv-piano'),
('pep-g7-vol1-u02-0068','pep-g7-vol1','pep-g7-vol1-u02','book',NULL,'n.','书',NULL,'mixed',13,5,10,67,NULL,'[{"part_of_speech":"n.","translation":"书"}]'::jsonb,'mv-book'),
('pep-g7-vol1-u02-0069','pep-g7-vol1','pep-g7-vol1-u02','basketball',NULL,'n.','篮球',NULL,'mixed',13,5,10,68,NULL,'[{"part_of_speech":"n.","translation":"篮球"}]'::jsonb,'mv-basketball'),
('pep-g7-vol1-u02-0070','pep-g7-vol1','pep-g7-vol1-u02','read',NULL,'v.','读；阅读',NULL,'mixed',13,5,10,69,NULL,'[{"part_of_speech":"v.","translation":"读；阅读"}]'::jsonb,'mv-read'),
('pep-g7-vol1-u02-0071','pep-g7-vol1','pep-g7-vol1-u02','classroom',NULL,'n.','教室',NULL,'mixed',13,5,10,70,NULL,'[{"part_of_speech":"n.","translation":"教室"}]'::jsonb,'mv-classroom'),
('pep-g7-vol1-u02-0072','pep-g7-vol1','pep-g7-vol1-u02','their',NULL,'pron.','他（她、它）们的',NULL,'mixed',13,5,10,71,NULL,'[{"part_of_speech":"pron.","translation":"他（她、它）们的"}]'::jsonb,'mv-their'),
('pep-g7-vol1-u02-0073','pep-g7-vol1','pep-g7-vol1-u02','clean',NULL,'adj. & v.','干净的；使……干净；打扫',NULL,'mixed',13,5,10,72,NULL,'[{"part_of_speech":"adj.","translation":"干净的"},{"part_of_speech":"v.","translation":"使……干净；打扫"}]'::jsonb,'mv-clean'),
('pep-g7-vol1-u02-0074','pep-g7-vol1','pep-g7-vol1-u02','wear',NULL,'v.','穿；戴',NULL,'mixed',13,5,10,73,NULL,'[{"part_of_speech":"v.","translation":"穿；戴"}]'::jsonb,'mv-wear'),
('pep-g7-vol1-u02-0075','pep-g7-vol1','pep-g7-vol1-u02','talk',NULL,'v. & n.','说话；交谈',NULL,'mixed',13,5,10,74,NULL,'[{"part_of_speech":"v. & n.","translation":"说话；交谈"}]'::jsonb,'mv-talk'),
('pep-g7-vol1-u02-0076','pep-g7-vol1','pep-g7-vol1-u02','tall',NULL,'adj.','高的',NULL,'mixed',13,5,10,75,NULL,'[{"part_of_speech":"adj.","translation":"高的"}]'::jsonb,'mv-tall'),
('pep-g7-vol1-u02-0077','pep-g7-vol1','pep-g7-vol1-u02','short',NULL,'adj.','短的；矮的',NULL,'mixed',13,5,10,76,NULL,'[{"part_of_speech":"adj.","translation":"短的；矮的"}]'::jsonb,'mv-short'),
('pep-g7-vol1-u02-0078','pep-g7-vol1','pep-g7-vol1-u02','hair',NULL,'n.','头发',NULL,'mixed',13,5,10,77,NULL,'[{"part_of_speech":"n.","translation":"头发"}]'::jsonb,'mv-hair'),
('pep-g7-vol1-u02-0079','pep-g7-vol1','pep-g7-vol1-u02','long',NULL,'adj.','长的',NULL,'mixed',13,5,10,78,NULL,'[{"part_of_speech":"adj.","translation":"长的"}]'::jsonb,'mv-long'),
('pep-g7-vol1-u02-0080','pep-g7-vol1','pep-g7-vol1-u02','quiet',NULL,'adj.','安静的',NULL,'mixed',13,5,10,79,NULL,'[{"part_of_speech":"adj.","translation":"安静的"}]'::jsonb,'mv-quiet'),
('pep-g7-vol1-u02-0081','pep-g7-vol1','pep-g7-vol1-u02','girl',NULL,'n.','女孩',NULL,'mixed',13,5,10,80,NULL,'[{"part_of_speech":"n.","translation":"女孩"}]'::jsonb,'mv-girl'),
('pep-g7-vol1-u02-0082','pep-g7-vol1','pep-g7-vol1-u02','but',NULL,'conj.','但是',NULL,'mixed',13,5,10,81,NULL,'[{"part_of_speech":"conj.","translation":"但是"}]'::jsonb,'mv-but'),
('pep-g7-vol1-u02-0083','pep-g7-vol1','pep-g7-vol1-u02','all',NULL,'adj. & pron.','所有（的）；全部（的）',NULL,'mixed',13,5,10,82,NULL,'[{"part_of_speech":"adj. & pron.","translation":"所有（的）；全部（的）"}]'::jsonb,'mv-all'),
('pep-g7-vol1-u02-0084','pep-g7-vol1','pep-g7-vol1-u02','any',NULL,'adj. & pron.','任何（的）；任一（的）',NULL,'mixed',13,5,10,83,NULL,'[{"part_of_speech":"adj. & pron.","translation":"任何（的）；任一（的）"}]'::jsonb,'mv-any'),
('pep-g7-vol1-u02-0085','pep-g7-vol1','pep-g7-vol1-u02','photo',NULL,'n.','照片',NULL,'mixed',13,5,10,84,NULL,'[{"part_of_speech":"n.","translation":"照片"}]'::jsonb,'mv-photo'),
('pep-g7-vol1-u02-0086','pep-g7-vol1','pep-g7-vol1-u02','left',NULL,'n. & adv.','左边；向左边',NULL,'mixed',13,5,10,85,NULL,'[{"part_of_speech":"n.","translation":"左边"},{"part_of_speech":"adv.","translation":"向左边"}]'::jsonb,'mv-left'),
('pep-g7-vol1-u02-0087','pep-g7-vol1','pep-g7-vol1-u02','little',NULL,'adj.','小的；年幼的',NULL,'mixed',13,5,10,86,NULL,'[{"part_of_speech":"adj.","translation":"小的；年幼的"}]'::jsonb,'mv-little'),
('pep-g7-vol1-u02-0088','pep-g7-vol1','pep-g7-vol1-u02','right',NULL,'n. & adv. & adj.','右边；向右边；正确的；适当的',NULL,'mixed',13,5,10,87,NULL,'[{"part_of_speech":"n.","translation":"右边"},{"part_of_speech":"adv.","translation":"向右边"},{"part_of_speech":"adj.","translation":"正确的；适当的"}]'::jsonb,'mv-right'),
('pep-g7-vol1-u02-0089','pep-g7-vol1','pep-g7-vol1-u02','always',NULL,'adv.','总是',NULL,'mixed',13,5,10,88,NULL,'[{"part_of_speech":"adv.","translation":"总是"}]'::jsonb,'mv-always'),
('pep-g7-vol1-u02-0090','pep-g7-vol1','pep-g7-vol1-u02','story',NULL,'n.','故事',NULL,'mixed',13,5,10,89,NULL,'[{"part_of_speech":"n.","translation":"故事"}]'::jsonb,'mv-story'),
('pep-g7-vol1-u02-0091','pep-g7-vol1','pep-g7-vol1-u02','night',NULL,'n.','夜晚',NULL,'mixed',13,5,10,90,NULL,'[{"part_of_speech":"n.","translation":"夜晚"}]'::jsonb,'mv-night'),
('pep-g7-vol1-u02-0092','pep-g7-vol1','pep-g7-vol1-u02','middle',NULL,'n. & adj.','中间；中间的',NULL,'mixed',13,5,10,91,NULL,'[{"part_of_speech":"n.","translation":"中间"},{"part_of_speech":"adj.","translation":"中间的"}]'::jsonb,'mv-middle'),
('pep-g7-vol1-u02-0093','pep-g7-vol1','pep-g7-vol1-u02','say',NULL,'v.','说',NULL,'mixed',13,5,10,92,NULL,'[{"part_of_speech":"v.","translation":"说"}]'::jsonb,'mv-say'),
('pep-g7-vol1-u02-0094','pep-g7-vol1','pep-g7-vol1-u02','think',NULL,'v.','思考',NULL,'mixed',13,5,10,93,NULL,'[{"part_of_speech":"v.","translation":"思考"}]'::jsonb,'mv-think'),
('pep-g7-vol1-u02-0095','pep-g7-vol1','pep-g7-vol1-u02','football',NULL,'n.','足球',NULL,'mixed',13,5,10,94,NULL,'[{"part_of_speech":"n.","translation":"足球"}]'::jsonb,'mv-football'),
('pep-g7-vol1-u02-0096','pep-g7-vol1','pep-g7-vol1-u02','eye',NULL,'n.','眼睛',NULL,'mixed',13,5,10,95,NULL,'[{"part_of_speech":"n.","translation":"眼睛"}]'::jsonb,'mv-eye'),
('pep-g7-vol1-u02-0097','pep-g7-vol1','pep-g7-vol1-u02','clever',NULL,'adj.','聪明的',NULL,'mixed',13,5,10,96,NULL,'[{"part_of_speech":"adj.","translation":"聪明的"}]'::jsonb,'mv-clever'),
('pep-g7-vol1-u02-0098','pep-g7-vol1','pep-g7-vol1-u02','next',NULL,'adj. & pron.','下一个（的）',NULL,'mixed',13,5,10,97,NULL,'[{"part_of_speech":"adj. & pron.","translation":"下一个（的）"}]'::jsonb,'mv-next'),
('pep-g7-vol1-u02-0099','pep-g7-vol1','pep-g7-vol1-u02','him',NULL,'pron.','（he 的宾格）他',NULL,'mixed',13,5,10,98,NULL,'[{"part_of_speech":"pron.","translation":"（he 的宾格）他"}]'::jsonb,'mv-him'),
('pep-g7-vol1-u02-0100','pep-g7-vol1','pep-g7-vol1-u02','happy',NULL,'adj.','快乐的',NULL,'mixed',13,5,10,99,NULL,'[{"part_of_speech":"adj.","translation":"快乐的"}]'::jsonb,'mv-happy'),
('pep-g7-vol1-u02-0101','pep-g7-vol1','pep-g7-vol1-u02','help',NULL,'v. & n.','帮助',NULL,'mixed',13,5,10,100,NULL,'[{"part_of_speech":"v. & n.","translation":"帮助"}]'::jsonb,'mv-help');

-- Unit 3 My School  (sort_order 101-133)
INSERT INTO public.vocabulary_words (
    word_id, book_id, module_id, english, phonetic, part_of_speech,
    translation, example_sentence, difficulty_level, difficulty_value,
    reward_token, estimated_duration_sec, sort_order, audio_url, senses, master_id
) VALUES
('pep-g7-vol1-u03-0102','pep-g7-vol1','pep-g7-vol1-u03','front',NULL,'n.','前面',NULL,'mixed',13,5,10,101,NULL,'[{"part_of_speech":"n.","translation":"前面"}]'::jsonb,'mv-front'),
('pep-g7-vol1-u03-0103','pep-g7-vol1','pep-g7-vol1-u03','between',NULL,'prep.','在……之间',NULL,'mixed',13,5,10,102,NULL,'[{"part_of_speech":"prep.","translation":"在……之间"}]'::jsonb,'mv-between'),
('pep-g7-vol1-u03-0104','pep-g7-vol1','pep-g7-vol1-u03','library',NULL,'n.','图书馆',NULL,'mixed',13,5,10,103,NULL,'[{"part_of_speech":"n.","translation":"图书馆"}]'::jsonb,'mv-library'),
('pep-g7-vol1-u03-0105','pep-g7-vol1','pep-g7-vol1-u03','computer',NULL,'n.','电脑',NULL,'mixed',13,5,10,104,NULL,'[{"part_of_speech":"n.","translation":"电脑"}]'::jsonb,'mv-computer'),
('pep-g7-vol1-u03-0106','pep-g7-vol1','pep-g7-vol1-u03','art',NULL,'n.','艺术；美术',NULL,'mixed',13,5,10,105,NULL,'[{"part_of_speech":"n.","translation":"艺术；美术"}]'::jsonb,'mv-art'),
('pep-g7-vol1-u03-0107','pep-g7-vol1','pep-g7-vol1-u03','shop',NULL,'n.','商店',NULL,'mixed',13,5,10,106,NULL,'[{"part_of_speech":"n.","translation":"商店"}]'::jsonb,'mv-shop'),
('pep-g7-vol1-u03-0108','pep-g7-vol1','pep-g7-vol1-u03','science',NULL,'n.','科学',NULL,'mixed',13,5,10,107,NULL,'[{"part_of_speech":"n.","translation":"科学"}]'::jsonb,'mv-science'),
('pep-g7-vol1-u03-0109','pep-g7-vol1','pep-g7-vol1-u03','Mrs',NULL,'n.','（对已婚妇女的礼貌称呼）夫人；太太',NULL,'mixed',13,5,10,108,NULL,'[{"part_of_speech":"n.","translation":"（对已婚妇女的礼貌称呼）夫人；太太"}]'::jsonb,'mv-mrs'),
('pep-g7-vol1-u03-0110','pep-g7-vol1','pep-g7-vol1-u03','student',NULL,'n.','学生',NULL,'mixed',13,5,10,109,NULL,'[{"part_of_speech":"n.","translation":"学生"}]'::jsonb,'mv-student'),
('pep-g7-vol1-u03-0111','pep-g7-vol1','pep-g7-vol1-u03','blackboard',NULL,'n.','黑板',NULL,'mixed',13,5,10,110,NULL,'[{"part_of_speech":"n.","translation":"黑板"}]'::jsonb,'mv-blackboard'),
('pep-g7-vol1-u03-0112','pep-g7-vol1','pep-g7-vol1-u03','sit',NULL,'v.','坐',NULL,'mixed',13,5,10,111,NULL,'[{"part_of_speech":"v.","translation":"坐"}]'::jsonb,'mv-sit'),
('pep-g7-vol1-u03-0113','pep-g7-vol1','pep-g7-vol1-u03','up',NULL,'adv.','向上',NULL,'mixed',13,5,10,112,NULL,'[{"part_of_speech":"adv.","translation":"向上"}]'::jsonb,'mv-up'),
('pep-g7-vol1-u03-0114','pep-g7-vol1','pep-g7-vol1-u03','back',NULL,'n. & adv.','后面；背部；回来；回原处',NULL,'mixed',13,5,10,113,NULL,'[{"part_of_speech":"n.","translation":"后面；背部"},{"part_of_speech":"adv.","translation":"回来；回原处"}]'::jsonb,'mv-back'),
('pep-g7-vol1-u03-0115','pep-g7-vol1','pep-g7-vol1-u03','clock',NULL,'n.','时钟；钟',NULL,'mixed',13,5,10,114,NULL,'[{"part_of_speech":"n.","translation":"时钟；钟"}]'::jsonb,'mv-clock'),
('pep-g7-vol1-u03-0116','pep-g7-vol1','pep-g7-vol1-u03','map',NULL,'n.','地图',NULL,'mixed',13,5,10,115,NULL,'[{"part_of_speech":"n.","translation":"地图"}]'::jsonb,'mv-map'),
('pep-g7-vol1-u03-0117','pep-g7-vol1','pep-g7-vol1-u03','window',NULL,'n.','窗户',NULL,'mixed',13,5,10,116,NULL,'[{"part_of_speech":"n.","translation":"窗户"}]'::jsonb,'mv-window'),
('pep-g7-vol1-u03-0118','pep-g7-vol1','pep-g7-vol1-u03','picture',NULL,'n.','照片；图画',NULL,'mixed',13,5,10,117,NULL,'[{"part_of_speech":"n.","translation":"照片；图画"}]'::jsonb,'mv-picture'),
('pep-g7-vol1-u03-0119','pep-g7-vol1','pep-g7-vol1-u03','famous',NULL,'adj.','著名的',NULL,'mixed',13,5,10,118,NULL,'[{"part_of_speech":"adj.","translation":"著名的"}]'::jsonb,'mv-famous'),
('pep-g7-vol1-u03-0120','pep-g7-vol1','pep-g7-vol1-u03','wall',NULL,'n.','墙',NULL,'mixed',13,5,10,119,NULL,'[{"part_of_speech":"n.","translation":"墙"}]'::jsonb,'mv-wall'),
('pep-g7-vol1-u03-0121','pep-g7-vol1','pep-g7-vol1-u03','table',NULL,'n.','桌子',NULL,'mixed',13,5,10,120,NULL,'[{"part_of_speech":"n.","translation":"桌子"}]'::jsonb,'mv-table'),
('pep-g7-vol1-u03-0122','pep-g7-vol1','pep-g7-vol1-u03','today',NULL,'adv. & n.','在今天；今天',NULL,'mixed',13,5,10,121,NULL,'[{"part_of_speech":"adv.","translation":"在今天"},{"part_of_speech":"n.","translation":"今天"}]'::jsonb,'mv-today'),
('pep-g7-vol1-u03-0123','pep-g7-vol1','pep-g7-vol1-u03','email',NULL,'n.','电子邮件',NULL,'mixed',13,5,10,122,NULL,'[{"part_of_speech":"n.","translation":"电子邮件"}]'::jsonb,'mv-email'),
('pep-g7-vol1-u03-0124','pep-g7-vol1','pep-g7-vol1-u03','answer',NULL,'n.','答案',NULL,'mixed',13,5,10,123,NULL,'[{"part_of_speech":"n.","translation":"答案"}]'::jsonb,'mv-answer'),
('pep-g7-vol1-u03-0125','pep-g7-vol1','pep-g7-vol1-u03','question',NULL,'n.','问题',NULL,'mixed',13,5,10,124,NULL,'[{"part_of_speech":"n.","translation":"问题"}]'::jsonb,'mv-question'),
('pep-g7-vol1-u03-0126','pep-g7-vol1','pep-g7-vol1-u03','exercise',NULL,'v. & n.','运动；锻炼；练习',NULL,'mixed',13,5,10,125,NULL,'[{"part_of_speech":"v. & n.","translation":"运动；锻炼；练习"}]'::jsonb,'mv-exercise'),
('pep-g7-vol1-u03-0127','pep-g7-vol1','pep-g7-vol1-u03','way',NULL,'n.','方式；道路',NULL,'mixed',13,5,10,126,NULL,'[{"part_of_speech":"n.","translation":"方式；道路"}]'::jsonb,'mv-way'),
('pep-g7-vol1-u03-0128','pep-g7-vol1','pep-g7-vol1-u03','best',NULL,'adj. & adv.','最好的；最',NULL,'mixed',13,5,10,127,NULL,'[{"part_of_speech":"adj.","translation":"最好的"},{"part_of_speech":"adv.","translation":"最"}]'::jsonb,'mv-best'),
('pep-g7-vol1-u03-0129','pep-g7-vol1','pep-g7-vol1-u03','place',NULL,'n.','地方；地点',NULL,'mixed',13,5,10,128,NULL,'[{"part_of_speech":"n.","translation":"地方；地点"}]'::jsonb,'mv-place'),
('pep-g7-vol1-u03-0130','pep-g7-vol1','pep-g7-vol1-u03','because',NULL,'conj.','因为',NULL,'mixed',13,5,10,129,NULL,'[{"part_of_speech":"conj.","translation":"因为"}]'::jsonb,'mv-because'),
('pep-g7-vol1-u03-0131','pep-g7-vol1','pep-g7-vol1-u03','why',NULL,'adv.','为什么',NULL,'mixed',13,5,10,130,NULL,'[{"part_of_speech":"adv.","translation":"为什么"}]'::jsonb,'mv-why'),
('pep-g7-vol1-u03-0132','pep-g7-vol1','pep-g7-vol1-u03','dear',NULL,'adj.','亲爱的',NULL,'mixed',13,5,10,131,NULL,'[{"part_of_speech":"adj.","translation":"亲爱的"}]'::jsonb,'mv-dear'),
('pep-g7-vol1-u03-0133','pep-g7-vol1','pep-g7-vol1-u03','tell',NULL,'v.','告诉',NULL,'mixed',13,5,10,132,NULL,'[{"part_of_speech":"v.","translation":"告诉"}]'::jsonb,'mv-tell'),
('pep-g7-vol1-u03-0134','pep-g7-vol1','pep-g7-vol1-u03','interesting',NULL,'adj.','有趣的',NULL,'mixed',13,5,10,133,NULL,'[{"part_of_speech":"adj.","translation":"有趣的"}]'::jsonb,'mv-interesting');

-- Unit 4 My Favourite Subject  (sort_order 134-158)
INSERT INTO public.vocabulary_words (
    word_id, book_id, module_id, english, phonetic, part_of_speech,
    translation, example_sentence, difficulty_level, difficulty_value,
    reward_token, estimated_duration_sec, sort_order, audio_url, senses, master_id
) VALUES
('pep-g7-vol1-u04-0135','pep-g7-vol1','pep-g7-vol1-u04','subject',NULL,'n.','学科；科目',NULL,'mixed',13,5,10,134,NULL,'[{"part_of_speech":"n.","translation":"学科；科目"}]'::jsonb,'mv-subject'),
('pep-g7-vol1-u04-0136','pep-g7-vol1','pep-g7-vol1-u04','learn',NULL,'v.','学习；得知',NULL,'mixed',13,5,10,135,NULL,'[{"part_of_speech":"v.","translation":"学习；得知"}]'::jsonb,'mv-learn'),
('pep-g7-vol1-u04-0137','pep-g7-vol1','pep-g7-vol1-u04','maths',NULL,'n.','数学',NULL,'mixed',13,5,10,136,NULL,'[{"part_of_speech":"n.","translation":"数学（= mathematics/math）"}]'::jsonb,'mv-maths'),
('pep-g7-vol1-u04-0138','pep-g7-vol1','pep-g7-vol1-u04','PE',NULL,'n.','体育',NULL,'mixed',13,5,10,137,NULL,'[{"part_of_speech":"n.","translation":"体育（= physical education）"}]'::jsonb,'mv-pe'),
('pep-g7-vol1-u04-0139','pep-g7-vol1','pep-g7-vol1-u04','hard',NULL,'adj. & adv.','困难的；努力地',NULL,'mixed',13,5,10,138,NULL,'[{"part_of_speech":"adj.","translation":"困难的"},{"part_of_speech":"adv.","translation":"努力地"}]'::jsonb,'mv-hard'),
('pep-g7-vol1-u04-0140','pep-g7-vol1','pep-g7-vol1-u04','sure',NULL,'adv.','当然；一定',NULL,'mixed',13,5,10,139,NULL,'[{"part_of_speech":"adv.","translation":"当然；一定"}]'::jsonb,'mv-sure'),
('pep-g7-vol1-u04-0141','pep-g7-vol1','pep-g7-vol1-u04','difficult',NULL,'adj.','困难的',NULL,'mixed',13,5,10,140,NULL,'[{"part_of_speech":"adj.","translation":"困难的"}]'::jsonb,'mv-difficult'),
('pep-g7-vol1-u04-0142','pep-g7-vol1','pep-g7-vol1-u04','easy',NULL,'adj.','容易的',NULL,'mixed',13,5,10,141,NULL,'[{"part_of_speech":"adj.","translation":"容易的"}]'::jsonb,'mv-easy'),
('pep-g7-vol1-u04-0143','pep-g7-vol1','pep-g7-vol1-u04','use',NULL,'v. & n.','使用；利用',NULL,'mixed',13,5,10,142,NULL,'[{"part_of_speech":"v. & n.","translation":"使用；利用"}]'::jsonb,'mv-use'),
('pep-g7-vol1-u04-0144','pep-g7-vol1','pep-g7-vol1-u04','give',NULL,'v.','给；送给；供给',NULL,'mixed',13,5,10,143,NULL,'[{"part_of_speech":"v.","translation":"给；送给；供给"}]'::jsonb,'mv-give'),
('pep-g7-vol1-u04-0145','pep-g7-vol1','pep-g7-vol1-u04','idea',NULL,'n.','想法；主意',NULL,'mixed',13,5,10,144,NULL,'[{"part_of_speech":"n.","translation":"想法；主意"}]'::jsonb,'mv-idea'),
('pep-g7-vol1-u04-0146','pep-g7-vol1','pep-g7-vol1-u04','listen',NULL,'v.','听',NULL,'mixed',13,5,10,145,NULL,'[{"part_of_speech":"v.","translation":"听"}]'::jsonb,'mv-listen'),
('pep-g7-vol1-u04-0147','pep-g7-vol1','pep-g7-vol1-u04','draw',NULL,'v.','画画',NULL,'mixed',13,5,10,146,NULL,'[{"part_of_speech":"v.","translation":"画画"}]'::jsonb,'mv-draw'),
('pep-g7-vol1-u04-0148','pep-g7-vol1','pep-g7-vol1-u04','travel',NULL,'v. & n.','旅行；游历',NULL,'mixed',13,5,10,147,NULL,'[{"part_of_speech":"v. & n.","translation":"旅行；游历"}]'::jsonb,'mv-travel'),
('pep-g7-vol1-u04-0149','pep-g7-vol1','pep-g7-vol1-u04','walk',NULL,'v. & n.','行走；步行',NULL,'mixed',13,5,10,148,NULL,'[{"part_of_speech":"v. & n.","translation":"行走；步行"}]'::jsonb,'mv-walk'),
('pep-g7-vol1-u04-0150','pep-g7-vol1','pep-g7-vol1-u04','afternoon',NULL,'n.','下午',NULL,'mixed',13,5,10,149,NULL,'[{"part_of_speech":"n.","translation":"下午"}]'::jsonb,'mv-afternoon'),
('pep-g7-vol1-u04-0151','pep-g7-vol1','pep-g7-vol1-u04','then',NULL,'adv.','那时；然后；那么',NULL,'mixed',13,5,10,150,NULL,'[{"part_of_speech":"adv.","translation":"那时；然后；那么"}]'::jsonb,'mv-then'),
('pep-g7-vol1-u04-0152','pep-g7-vol1','pep-g7-vol1-u04','Miss',NULL,'n.','（对未婚女子的礼貌称呼）小姐；女士',NULL,'mixed',13,5,10,151,NULL,'[{"part_of_speech":"n.","translation":"（对未婚女子的礼貌称呼）小姐；女士"}]'::jsonb,'mv-miss'),
('pep-g7-vol1-u04-0153','pep-g7-vol1','pep-g7-vol1-u04','work',NULL,'v. & n.','工作',NULL,'mixed',13,5,10,152,NULL,'[{"part_of_speech":"v. & n.","translation":"工作"}]'::jsonb,'mv-work'),
('pep-g7-vol1-u04-0154','pep-g7-vol1','pep-g7-vol1-u04','sometimes',NULL,'adv.','有时',NULL,'mixed',13,5,10,153,NULL,'[{"part_of_speech":"adv.","translation":"有时"}]'::jsonb,'mv-sometimes'),
('pep-g7-vol1-u04-0155','pep-g7-vol1','pep-g7-vol1-u04','feel',NULL,'v.','感觉；觉得',NULL,'mixed',13,5,10,154,NULL,'[{"part_of_speech":"v.","translation":"感觉；觉得"}]'::jsonb,'mv-feel'),
('pep-g7-vol1-u04-0156','pep-g7-vol1','pep-g7-vol1-u04','busy',NULL,'adj.','忙碌的；无暇的',NULL,'mixed',13,5,10,155,NULL,'[{"part_of_speech":"adj.","translation":"忙碌的；无暇的"}]'::jsonb,'mv-busy'),
('pep-g7-vol1-u04-0157','pep-g7-vol1','pep-g7-vol1-u04','study',NULL,'v.','学习',NULL,'mixed',13,5,10,156,NULL,'[{"part_of_speech":"v.","translation":"学习"}]'::jsonb,'mv-study'),
('pep-g7-vol1-u04-0158','pep-g7-vol1','pep-g7-vol1-u04','song',NULL,'n.','歌曲',NULL,'mixed',13,5,10,157,NULL,'[{"part_of_speech":"n.","translation":"歌曲"}]'::jsonb,'mv-song'),
('pep-g7-vol1-u04-0159','pep-g7-vol1','pep-g7-vol1-u04','out',NULL,'adv. & prep.','（从……里）出来；出去',NULL,'mixed',13,5,10,158,NULL,'[{"part_of_speech":"adv. & prep.","translation":"（从……里）出来；出去"}]'::jsonb,'mv-out');

-- Unit 5 Fun Clubs  (sort_order 159-175)
INSERT INTO public.vocabulary_words (
    word_id, book_id, module_id, english, phonetic, part_of_speech,
    translation, example_sentence, difficulty_level, difficulty_value,
    reward_token, estimated_duration_sec, sort_order, audio_url, senses, master_id
) VALUES
('pep-g7-vol1-u05-0160','pep-g7-vol1','pep-g7-vol1-u05','sing',NULL,'v.','唱歌',NULL,'mixed',13,5,10,159,NULL,'[{"part_of_speech":"v.","translation":"唱歌"}]'::jsonb,'mv-sing'),
('pep-g7-vol1-u05-0161','pep-g7-vol1','pep-g7-vol1-u05','swim',NULL,'v.','游泳',NULL,'mixed',13,5,10,160,NULL,'[{"part_of_speech":"v.","translation":"游泳"}]'::jsonb,'mv-swim'),
('pep-g7-vol1-u05-0162','pep-g7-vol1','pep-g7-vol1-u05','run',NULL,'v.','跑；跑步',NULL,'mixed',13,5,10,161,NULL,'[{"part_of_speech":"v.","translation":"跑；跑步"}]'::jsonb,'mv-run'),
('pep-g7-vol1-u05-0163','pep-g7-vol1','pep-g7-vol1-u05','fast',NULL,'adv. & adj.','快地（的）',NULL,'mixed',13,5,10,162,NULL,'[{"part_of_speech":"adv. & adj.","translation":"快地（的）"}]'::jsonb,'mv-fast'),
('pep-g7-vol1-u05-0164','pep-g7-vol1','pep-g7-vol1-u05','dance',NULL,'n. & v.','跳舞',NULL,'mixed',13,5,10,163,NULL,'[{"part_of_speech":"n. & v.","translation":"跳舞"}]'::jsonb,'mv-dance'),
('pep-g7-vol1-u05-0165','pep-g7-vol1','pep-g7-vol1-u05','fly',NULL,'v.','飞',NULL,'mixed',13,5,10,164,NULL,'[{"part_of_speech":"v.","translation":"飞"}]'::jsonb,'mv-fly'),
('pep-g7-vol1-u05-0166','pep-g7-vol1','pep-g7-vol1-u05','so',NULL,'adv. & conj.','这么；那么；所以',NULL,'mixed',13,5,10,165,NULL,'[{"part_of_speech":"adv.","translation":"这么；那么"},{"part_of_speech":"conj.","translation":"用来引出评论或问题；所以"}]'::jsonb,'mv-so'),
('pep-g7-vol1-u05-0167','pep-g7-vol1','pep-g7-vol1-u05','watch',NULL,'v. & n.','注视；观看；表；手表',NULL,'mixed',13,5,10,166,NULL,'[{"part_of_speech":"v.","translation":"注视；观看"},{"part_of_speech":"n.","translation":"表；手表"}]'::jsonb,'mv-watch'),
('pep-g7-vol1-u05-0168','pep-g7-vol1','pep-g7-vol1-u05','cake',NULL,'n.','蛋糕',NULL,'mixed',13,5,10,167,NULL,'[{"part_of_speech":"n.","translation":"蛋糕"}]'::jsonb,'mv-cake'),
('pep-g7-vol1-u05-0169','pep-g7-vol1','pep-g7-vol1-u05','cook',NULL,'v.','做饭',NULL,'mixed',13,5,10,168,NULL,'[{"part_of_speech":"v.","translation":"做饭"}]'::jsonb,'mv-cook'),
('pep-g7-vol1-u05-0170','pep-g7-vol1','pep-g7-vol1-u05','noodle',NULL,'n.','面条',NULL,'mixed',13,5,10,169,NULL,'[{"part_of_speech":"n.","translation":"面条"}]'::jsonb,'mv-noodle'),
('pep-g7-vol1-u05-0171','pep-g7-vol1','pep-g7-vol1-u05','open',NULL,'v. & adj.','打开；开放的；敞开的',NULL,'mixed',13,5,10,170,NULL,'[{"part_of_speech":"v.","translation":"打开"},{"part_of_speech":"adj.","translation":"开放的；敞开的"}]'::jsonb,'mv-open'),
('pep-g7-vol1-u05-0172','pep-g7-vol1','pep-g7-vol1-u05','take',NULL,'v.','拍照；拿；取；买下',NULL,'mixed',13,5,10,171,NULL,'[{"part_of_speech":"v.","translation":"拍照；拿；取；买下"}]'::jsonb,'mv-take'),
('pep-g7-vol1-u05-0173','pep-g7-vol1','pep-g7-vol1-u05','visit',NULL,'v. & n.','参观；拜访',NULL,'mixed',13,5,10,172,NULL,'[{"part_of_speech":"v. & n.","translation":"参观；拜访"}]'::jsonb,'mv-visit'),
('pep-g7-vol1-u05-0174','pep-g7-vol1','pep-g7-vol1-u05','park',NULL,'n.','公园',NULL,'mixed',13,5,10,173,NULL,'[{"part_of_speech":"n.","translation":"公园"}]'::jsonb,'mv-park'),
('pep-g7-vol1-u05-0175','pep-g7-vol1','pep-g7-vol1-u05','when',NULL,'adv.','什么时候',NULL,'mixed',13,5,10,174,NULL,'[{"part_of_speech":"adv.","translation":"什么时候"}]'::jsonb,'mv-when'),
('pep-g7-vol1-u05-0176','pep-g7-vol1','pep-g7-vol1-u05','share',NULL,'v.','分享；合用；分担',NULL,'mixed',13,5,10,175,NULL,'[{"part_of_speech":"v.","translation":"分享；合用；分担"}]'::jsonb,'mv-share');

-- Unit 6 A Day in the Life  (sort_order 176-187)
INSERT INTO public.vocabulary_words (
    word_id, book_id, module_id, english, phonetic, part_of_speech,
    translation, example_sentence, difficulty_level, difficulty_value,
    reward_token, estimated_duration_sec, sort_order, audio_url, senses, master_id
) VALUES
('pep-g7-vol1-u06-0177','pep-g7-vol1','pep-g7-vol1-u06','o''clock',NULL,'adv.','（表示整点）……点钟',NULL,'mixed',13,5,10,176,NULL,'[{"part_of_speech":"adv.","translation":"（表示整点）……点钟"}]'::jsonb,'mv-o-clock'),
('pep-g7-vol1-u06-0178','pep-g7-vol1','pep-g7-vol1-u06','dress',NULL,'v. & n.','穿衣服；连衣裙',NULL,'mixed',13,5,10,177,NULL,'[{"part_of_speech":"v.","translation":"穿衣服"},{"part_of_speech":"n.","translation":"连衣裙"}]'::jsonb,'mv-dress'),
('pep-g7-vol1-u06-0179','pep-g7-vol1','pep-g7-vol1-u06','breakfast',NULL,'n.','早餐',NULL,'mixed',13,5,10,178,NULL,'[{"part_of_speech":"n.","translation":"早餐"}]'::jsonb,'mv-breakfast'),
('pep-g7-vol1-u06-0180','pep-g7-vol1','pep-g7-vol1-u06','before',NULL,'prep. & conj. & adv.','在……以前；以前',NULL,'mixed',13,5,10,179,NULL,'[{"part_of_speech":"prep. & conj.","translation":"在……以前"},{"part_of_speech":"adv.","translation":"以前"}]'::jsonb,'mv-before'),
('pep-g7-vol1-u06-0181','pep-g7-vol1','pep-g7-vol1-u06','begin',NULL,'v.','开始',NULL,'mixed',13,5,10,180,NULL,'[{"part_of_speech":"v.","translation":"开始"}]'::jsonb,'mv-begin'),
('pep-g7-vol1-u06-0182','pep-g7-vol1','pep-g7-vol1-u06','dinner',NULL,'n.','正餐；主餐',NULL,'mixed',13,5,10,181,NULL,'[{"part_of_speech":"n.","translation":"正餐；主餐"}]'::jsonb,'mv-dinner'),
('pep-g7-vol1-u06-0183','pep-g7-vol1','pep-g7-vol1-u06','early',NULL,'adj. & adv.','早的；早期的；提前；在早期',NULL,'mixed',13,5,10,182,NULL,'[{"part_of_speech":"adj.","translation":"早的；早期的"},{"part_of_speech":"adv.","translation":"提前；在早期"}]'::jsonb,'mv-early'),
('pep-g7-vol1-u06-0184','pep-g7-vol1','pep-g7-vol1-u06','ask',NULL,'v.','询问；请求',NULL,'mixed',13,5,10,183,NULL,'[{"part_of_speech":"v.","translation":"询问；请求"}]'::jsonb,'mv-ask'),
('pep-g7-vol1-u06-0185','pep-g7-vol1','pep-g7-vol1-u06','lunch',NULL,'n.','午餐',NULL,'mixed',13,5,10,184,NULL,'[{"part_of_speech":"n.","translation":"午餐"}]'::jsonb,'mv-lunch'),
('pep-g7-vol1-u06-0186','pep-g7-vol1','pep-g7-vol1-u06','film',NULL,'n.','电影',NULL,'mixed',13,5,10,185,NULL,'[{"part_of_speech":"n.","translation":"电影"}]'::jsonb,'mv-film'),
('pep-g7-vol1-u06-0187','pep-g7-vol1','pep-g7-vol1-u06','lesson',NULL,'n.','课；一节课',NULL,'mixed',13,5,10,186,NULL,'[{"part_of_speech":"n.","translation":"课；一节课"}]'::jsonb,'mv-lesson'),
('pep-g7-vol1-u06-0188','pep-g7-vol1','pep-g7-vol1-u06','ice',NULL,'n.','冰；冰块',NULL,'mixed',13,5,10,187,NULL,'[{"part_of_speech":"n.","translation":"冰；冰块"}]'::jsonb,'mv-ice');

-- Unit 7 Happy Birthday!  (sort_order 188-206)
INSERT INTO public.vocabulary_words (
    word_id, book_id, module_id, english, phonetic, part_of_speech,
    translation, example_sentence, difficulty_level, difficulty_value,
    reward_token, estimated_duration_sec, sort_order, audio_url, senses, master_id
) VALUES
('pep-g7-vol1-u07-0189','pep-g7-vol1','pep-g7-vol1-u07','birthday',NULL,'n.','生日',NULL,'mixed',13,5,10,188,NULL,'[{"part_of_speech":"n.","translation":"生日"}]'::jsonb,'mv-birthday'),
('pep-g7-vol1-u07-0190','pep-g7-vol1','pep-g7-vol1-u07','month',NULL,'n.','月份',NULL,'mixed',13,5,10,189,NULL,'[{"part_of_speech":"n.","translation":"月份"}]'::jsonb,'mv-month'),
('pep-g7-vol1-u07-0191','pep-g7-vol1','pep-g7-vol1-u07','gift',NULL,'n.','礼物',NULL,'mixed',13,5,10,190,NULL,'[{"part_of_speech":"n.","translation":"礼物"}]'::jsonb,'mv-gift'),
('pep-g7-vol1-u07-0192','pep-g7-vol1','pep-g7-vol1-u07','party',NULL,'n.','聚会',NULL,'mixed',13,5,10,191,NULL,'[{"part_of_speech":"n.","translation":"聚会"}]'::jsonb,'mv-party'),
('pep-g7-vol1-u07-0193','pep-g7-vol1','pep-g7-vol1-u07','buy',NULL,'v.','买',NULL,'mixed',13,5,10,192,NULL,'[{"part_of_speech":"v.","translation":"买"}]'::jsonb,'mv-buy'),
('pep-g7-vol1-u07-0194','pep-g7-vol1','pep-g7-vol1-u07','woman',NULL,'n.','女人',NULL,'mixed',13,5,10,193,NULL,'[{"part_of_speech":"n.","translation":"女人"}]'::jsonb,'mv-woman'),
('pep-g7-vol1-u07-0195','pep-g7-vol1','pep-g7-vol1-u07','candle',NULL,'n.','蜡烛',NULL,'mixed',13,5,10,194,NULL,'[{"part_of_speech":"n.","translation":"蜡烛"}]'::jsonb,'mv-candle'),
('pep-g7-vol1-u07-0196','pep-g7-vol1','pep-g7-vol1-u07','will',NULL,'modal v.','将要；会',NULL,'mixed',13,5,10,195,NULL,'[{"part_of_speech":"modal v.","translation":"将要；会"}]'::jsonb,'mv-will'),
('pep-g7-vol1-u07-0197','pep-g7-vol1','pep-g7-vol1-u07','egg',NULL,'n.','蛋',NULL,'mixed',13,5,10,196,NULL,'[{"part_of_speech":"n.","translation":"蛋"}]'::jsonb,'mv-egg'),
('pep-g7-vol1-u07-0198','pep-g7-vol1','pep-g7-vol1-u07','juice',NULL,'n.','果汁',NULL,'mixed',13,5,10,197,NULL,'[{"part_of_speech":"n.","translation":"果汁"}]'::jsonb,'mv-juice'),
('pep-g7-vol1-u07-0199','pep-g7-vol1','pep-g7-vol1-u07','milk',NULL,'n.','牛奶',NULL,'mixed',13,5,10,198,NULL,'[{"part_of_speech":"n.","translation":"牛奶"}]'::jsonb,'mv-milk'),
('pep-g7-vol1-u07-0200','pep-g7-vol1','pep-g7-vol1-u07','banana',NULL,'n.','香蕉',NULL,'mixed',13,5,10,199,NULL,'[{"part_of_speech":"n.","translation":"香蕉"}]'::jsonb,'mv-banana'),
('pep-g7-vol1-u07-0201','pep-g7-vol1','pep-g7-vol1-u07','drink',NULL,'n. & v.','饮品；喝',NULL,'mixed',13,5,10,200,NULL,'[{"part_of_speech":"n.","translation":"饮品"},{"part_of_speech":"v.","translation":"喝"}]'::jsonb,'mv-drink'),
('pep-g7-vol1-u07-0202','pep-g7-vol1','pep-g7-vol1-u07','eat',NULL,'v.','吃',NULL,'mixed',13,5,10,201,NULL,'[{"part_of_speech":"v.","translation":"吃"}]'::jsonb,'mv-eat'),
('pep-g7-vol1-u07-0203','pep-g7-vol1','pep-g7-vol1-u07','nurse',NULL,'n.','护士',NULL,'mixed',13,5,10,202,NULL,'[{"part_of_speech":"n.","translation":"护士"}]'::jsonb,'mv-nurse'),
('pep-g7-vol1-u07-0204','pep-g7-vol1','pep-g7-vol1-u07','wish',NULL,'n. & v.','愿望；希望；祝愿',NULL,'mixed',13,5,10,203,NULL,'[{"part_of_speech":"n.","translation":"愿望"},{"part_of_speech":"v.","translation":"希望；祝愿"}]'::jsonb,'mv-wish'),
('pep-g7-vol1-u07-0205','pep-g7-vol1','pep-g7-vol1-u07','hear',NULL,'v.','听到',NULL,'mixed',13,5,10,204,NULL,'[{"part_of_speech":"v.","translation":"听到"}]'::jsonb,'mv-hear'),
('pep-g7-vol1-u07-0206','pep-g7-vol1','pep-g7-vol1-u07','door',NULL,'n.','门',NULL,'mixed',13,5,10,205,NULL,'[{"part_of_speech":"n.","translation":"门"}]'::jsonb,'mv-door'),
('pep-g7-vol1-u07-0207','pep-g7-vol1','pep-g7-vol1-u07','world',NULL,'n.','世界',NULL,'mixed',13,5,10,206,NULL,'[{"part_of_speech":"n.","translation":"世界"}]'::jsonb,'mv-world');

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
    ROUND(avg_difficulty::numeric, 2), occurrences::int, is_textbook, 2026080602
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
