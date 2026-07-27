DO $$
BEGIN
    INSERT INTO public.word_books (
        book_id,
        title,
        description,
        language,
        difficulty,
        version,
        word_count,
        cover_url,
        updated_at
    ) VALUES (
        'seedie-growth-reader-1',
        'Seedie 成长阅读词书',
        '围绕校园、自然和表达主题整理的成长型英语词书，适合在日常练习中持续积累。',
        'en-US',
        'mixed',
        1,
        12,
        NULL,
        2026072701
    )
    ON CONFLICT (book_id) DO UPDATE SET
        title = EXCLUDED.title,
        description = EXCLUDED.description,
        language = EXCLUDED.language,
        difficulty = EXCLUDED.difficulty,
        version = EXCLUDED.version,
        word_count = EXCLUDED.word_count,
        cover_url = EXCLUDED.cover_url,
        updated_at = EXCLUDED.updated_at;

    DELETE FROM public.vocabulary_words
    WHERE book_id = 'seedie-growth-reader-1';

    DELETE FROM public.word_book_modules
    WHERE book_id = 'seedie-growth-reader-1';

    INSERT INTO public.word_book_modules (
        module_id,
        book_id,
        title,
        sort_order,
        word_count
    ) VALUES
        ('seedie-growth-reader-1-m1', 'seedie-growth-reader-1', '校园日常', 0, 6),
        ('seedie-growth-reader-1-m2', 'seedie-growth-reader-1', '自然探索', 1, 6)
    ON CONFLICT (module_id) DO UPDATE SET
        book_id = EXCLUDED.book_id,
        title = EXCLUDED.title,
        sort_order = EXCLUDED.sort_order,
        word_count = EXCLUDED.word_count;

    INSERT INTO public.vocabulary_words (
        word_id,
        book_id,
        module_id,
        english,
        phonetic,
        part_of_speech,
        translation,
        example_sentence,
        difficulty_level,
        reward_token,
        estimated_duration_sec,
        sort_order,
        audio_url
    ) VALUES
        ('sgr1-w1', 'seedie-growth-reader-1', 'seedie-growth-reader-1-m1', 'library', '/ˈlaɪ.brer.i/', 'n.', '图书馆', 'We finish our reading task in the library after class.', 'easy', 3, 8, 0, NULL),
        ('sgr1-w2', 'seedie-growth-reader-1', 'seedie-growth-reader-1-m1', 'homework', '/ˈhəʊm.wɜːk/', 'n.', '家庭作业', 'I check my homework before I hand it in.', 'easy', 3, 8, 1, NULL),
        ('sgr1-w3', 'seedie-growth-reader-1', 'seedie-growth-reader-1-m1', 'practice', '/ˈpræk.tɪs/', 'v.', '练习', 'We practice new words together every morning.', 'easy', 3, 8, 2, NULL),
        ('sgr1-w4', 'seedie-growth-reader-1', 'seedie-growth-reader-1-m1', 'schedule', '/ˈskedʒ.uːl/', 'n.', '日程安排', 'Our class schedule helps us stay organized.', 'medium', 4, 10, 3, NULL),
        ('sgr1-w5', 'seedie-growth-reader-1', 'seedie-growth-reader-1-m1', 'respect', '/rɪˈspekt/', 'v.', '尊重', 'Good students respect each other in discussions.', 'medium', 4, 10, 4, NULL),
        ('sgr1-w6', 'seedie-growth-reader-1', 'seedie-growth-reader-1-m1', 'confident', '/ˈkɒn.fɪ.dənt/', 'adj.', '自信的', 'She feels confident when she answers in English.', 'hard', 5, 12, 5, NULL),
        ('sgr1-w7', 'seedie-growth-reader-1', 'seedie-growth-reader-1-m2', 'valley', '/ˈvæl.i/', 'n.', '山谷', 'A small river runs through the valley.', 'easy', 3, 8, 6, NULL),
        ('sgr1-w8', 'seedie-growth-reader-1', 'seedie-growth-reader-1-m2', 'stream', '/striːm/', 'n.', '溪流', 'The stream sounds gentle after the rain.', 'easy', 3, 8, 7, NULL),
        ('sgr1-w9', 'seedie-growth-reader-1', 'seedie-growth-reader-1-m2', 'explore', '/ɪkˈsplɔːr/', 'v.', '探索', 'Children explore the forest trail with their teacher.', 'medium', 4, 10, 8, NULL),
        ('sgr1-w10', 'seedie-growth-reader-1', 'seedie-growth-reader-1-m2', 'shelter', '/ˈʃel.tər/', 'n.', '庇护所', 'Birds look for shelter when the wind becomes strong.', 'medium', 4, 10, 9, NULL),
        ('sgr1-w11', 'seedie-growth-reader-1', 'seedie-growth-reader-1-m2', 'observe', '/əbˈzɜːv/', 'v.', '观察', 'We observe how the leaves change in autumn.', 'hard', 5, 12, 10, NULL),
        ('sgr1-w12', 'seedie-growth-reader-1', 'seedie-growth-reader-1-m2', 'resourceful', '/rɪˈzɔːs.fəl/', 'adj.', '善于利用资源的', 'A resourceful learner can solve problems in different ways.', 'hard', 5, 12, 11, NULL)
    ON CONFLICT (word_id) DO UPDATE SET
        book_id = EXCLUDED.book_id,
        module_id = EXCLUDED.module_id,
        english = EXCLUDED.english,
        phonetic = EXCLUDED.phonetic,
        part_of_speech = EXCLUDED.part_of_speech,
        translation = EXCLUDED.translation,
        example_sentence = EXCLUDED.example_sentence,
        difficulty_level = EXCLUDED.difficulty_level,
        reward_token = EXCLUDED.reward_token,
        estimated_duration_sec = EXCLUDED.estimated_duration_sec,
        sort_order = EXCLUDED.sort_order,
        audio_url = EXCLUDED.audio_url;
END $$;
