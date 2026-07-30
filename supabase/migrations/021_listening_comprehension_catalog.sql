CREATE TABLE IF NOT EXISTS public.listening_materials (
    material_id TEXT PRIMARY KEY,
    title TEXT,
    title_zh TEXT,
    material_type TEXT NOT NULL DEFAULT 'dialogue',
    prompt_text TEXT,
    transcript TEXT,
    audio_url TEXT,
    estimated_seconds INT NOT NULL DEFAULT 60,
    sort_order INT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 1,
    updated_at BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS public.listening_questions (
    question_id TEXT PRIMARY KEY,
    material_id TEXT NOT NULL REFERENCES public.listening_materials(material_id) ON DELETE CASCADE,
    question_type TEXT,
    stem TEXT,
    sort_order INT NOT NULL DEFAULT 0,
    correct_option_id TEXT,
    explanation TEXT,
    reward_token INT NOT NULL DEFAULT 2
);

CREATE TABLE IF NOT EXISTS public.listening_options (
    question_id TEXT NOT NULL REFERENCES public.listening_questions(question_id) ON DELETE CASCADE,
    option_id TEXT NOT NULL,
    option_text TEXT,
    sort_order INT NOT NULL DEFAULT 0,
    PRIMARY KEY (question_id, option_id)
);

CREATE INDEX IF NOT EXISTS listening_materials_sort_order_idx
    ON public.listening_materials (sort_order);

CREATE INDEX IF NOT EXISTS listening_questions_material_sort_idx
    ON public.listening_questions (material_id, sort_order);

ALTER TABLE public.listening_materials ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.listening_questions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.listening_options ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Anyone can view listening materials" ON public.listening_materials;
CREATE POLICY "Anyone can view listening materials"
    ON public.listening_materials FOR SELECT USING (true);

DROP POLICY IF EXISTS "Anyone can view listening questions" ON public.listening_questions;
CREATE POLICY "Anyone can view listening questions"
    ON public.listening_questions FOR SELECT USING (true);

DROP POLICY IF EXISTS "Anyone can view listening options" ON public.listening_options;
CREATE POLICY "Anyone can view listening options"
    ON public.listening_options FOR SELECT USING (true);

GRANT SELECT ON public.listening_materials TO anon, authenticated;
GRANT SELECT ON public.listening_questions TO anon, authenticated;
GRANT SELECT ON public.listening_options TO anon, authenticated;

DELETE FROM public.listening_materials
WHERE material_id IN ('l08-01');

INSERT INTO public.listening_materials (
    material_id,
    title,
    title_zh,
    material_type,
    prompt_text,
    transcript,
    audio_url,
    estimated_seconds,
    sort_order,
    version,
    updated_at
) VALUES (
    'l08-01',
    $seedie$After-School Volunteer Plan$seedie$,
    $seedie$放学后的志愿活动安排$seedie$,
    $seedie$dialogue$seedie$,
    $seedie$Listen to the short dialogue and choose the best answer for each question.$seedie$,
    $seedie$Amy: Hi, Leo. Are you going to the school volunteer fair this Friday?
Leo: Yes. I want to join a weekend team, but I am not sure which one is best.
Amy: I signed up for the library helper group. They need students to sort books and read stories to younger children.
Leo: That sounds nice. I was thinking about the park clean-up team.
Amy: They meet on Saturday morning, right?
Leo: Yes, from 9 to 11. My cousin joined last month and said the teacher in charge was very friendly.
Amy: Then it may be a good choice for you. You like outdoor activities.
Leo: True. But I also want to improve my speaking skills.
Amy: In that case, the museum guide team may help. They welcome visitors and answer simple questions.
Leo: Good point. I will talk to the teacher at the fair and decide there.$seedie$,
    $seedie$https://www.w3schools.com/html/horse.mp3$seedie$,
    72,
    0,
    1,
    2026073001
)
ON CONFLICT (material_id) DO UPDATE SET
    title = EXCLUDED.title,
    title_zh = EXCLUDED.title_zh,
    material_type = EXCLUDED.material_type,
    prompt_text = EXCLUDED.prompt_text,
    transcript = EXCLUDED.transcript,
    audio_url = EXCLUDED.audio_url,
    estimated_seconds = EXCLUDED.estimated_seconds,
    sort_order = EXCLUDED.sort_order,
    version = EXCLUDED.version,
    updated_at = EXCLUDED.updated_at;

DELETE FROM public.listening_questions
WHERE material_id IN ('l08-01');

INSERT INTO public.listening_questions (
    question_id,
    material_id,
    question_type,
    stem,
    sort_order,
    correct_option_id,
    explanation,
    reward_token
) VALUES (
    'l08-01-q1',
    'l08-01',
    $seedie$detail$seedie$,
    $seedie$Which volunteer group has Amy already signed up for?$seedie$,
    0,
    'B',
    $seedie$Amy says she signed up for the library helper group, so B is correct.$seedie$,
    2
), (
    'l08-01-q2',
    'l08-01',
    $seedie$detail$seedie$,
    $seedie$When does the park clean-up team meet?$seedie$,
    1,
    'A',
    $seedie$Leo says the park clean-up team meets on Saturday morning from 9 to 11.$seedie$,
    2
), (
    'l08-01-q3',
    'l08-01',
    $seedie$inference$seedie$,
    $seedie$Why does Amy suggest the museum guide team to Leo?$seedie$,
    2,
    'D',
    $seedie$Amy connects the museum guide team with Leo's wish to improve his speaking skills.$seedie$,
    2
);

INSERT INTO public.listening_options (question_id, option_id, option_text, sort_order) VALUES
    ('l08-01-q1', 'A', $seedie$The museum guide group$seedie$, 0),
    ('l08-01-q1', 'B', $seedie$The library helper group$seedie$, 1),
    ('l08-01-q1', 'C', $seedie$The park clean-up group$seedie$, 2),
    ('l08-01-q1', 'D', $seedie$The school radio group$seedie$, 3),
    ('l08-01-q2', 'A', $seedie$On Saturday morning$seedie$, 0),
    ('l08-01-q2', 'B', $seedie$On Friday afternoon$seedie$, 1),
    ('l08-01-q2', 'C', $seedie$On Sunday evening$seedie$, 2),
    ('l08-01-q2', 'D', $seedie$Every weekday after school$seedie$, 3),
    ('l08-01-q3', 'A', $seedie$Because Leo dislikes outdoor work$seedie$, 0),
    ('l08-01-q3', 'B', $seedie$Because the museum is next to his home$seedie$, 1),
    ('l08-01-q3', 'C', $seedie$Because Amy wants to join that team too$seedie$, 2),
    ('l08-01-q3', 'D', $seedie$Because it can help him practice speaking$seedie$, 3);
