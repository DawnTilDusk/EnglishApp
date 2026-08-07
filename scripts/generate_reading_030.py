"""Generate supabase/migrations/030_reading_grade_bands.sql (incremental).

Staging content lives under scripts/reading_seed/. Do not rewrite 020.
"""
from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SEED_ROOT = ROOT / "scripts" / "reading_seed"
OUT = ROOT / "supabase" / "migrations" / "030_reading_grade_bands.sql"
UPDATED_AT = 2026080701
DEFAULT_REWARD = 2

BAND_META = {
    "g7a": {"grade": 7, "label": "七上", "sort_base": 100},
    "g7b": {"grade": 7, "label": "七下", "sort_base": 200},
    "g8a": {"grade": 8, "label": "八上", "sort_base": 300},
    "g8b": {"grade": 8, "label": "八下", "sort_base": 400},
    "g9a": {"grade": 9, "label": "九上", "sort_base": 500},
    "g9b": {"grade": 9, "label": "九下", "sort_base": 600},
}


def dollar_quote(text: str, tag: str = "seedie") -> str:
    base = tag
    n = 0
    while True:
        t = base if n == 0 else f"{base}{n}"
        marker = f"${t}$"
        if marker not in text:
            return f"{marker}{text}{marker}"
        n += 1


def sql_str(text: str | None) -> str:
    if text is None:
        return "NULL"
    return dollar_quote(text)


def word_count(passage: str) -> int:
    cleaned = passage.replace("<<", "").replace(">>", "")
    return len(cleaned.split())


def q(
    qid: str,
    qtype: str,
    stem: str,
    correct: str,
    options: list[tuple[str, str]],
    explanation: str,
    highlight: str | None = None,
) -> dict:
    return {
        "id": qid,
        "type": qtype,
        "stem": stem,
        "correctOptionId": correct,
        "explanation": explanation,
        "highlight": highlight,
        "options": [{"id": oid, "text": text} for oid, text in options],
    }


def make_set(
    set_id: str,
    band: str,
    title: str,
    title_zh: str,
    difficulty: str,
    topic: str,
    passage: str,
    questions: list[dict],
    estimated_minutes: int,
    index_in_band: int,
) -> dict:
    meta = BAND_META[band]
    return {
        "id": set_id,
        "grade_band": band,
        "grade": meta["grade"],
        "title": title,
        "titleZh": title_zh,
        "difficulty": difficulty,
        "topic": topic,
        "passage": passage,
        "wordCount": word_count(passage),
        "estimatedMinutes": estimated_minutes,
        "sort_order": meta["sort_base"] + index_in_band,
        "questions": questions,
    }


def build_new_sets() -> list[dict]:
    """25 original sets: 5 each for g7a,g7b,g8b,g9a,g9b (g8a already seeded as r08-*)."""
    sets: list[dict] = []

    # ----- g7a (七上) -----
    sets.append(
        make_set(
            "r07a-01",
            "g7a",
            "My First Week at Middle School",
            "我的初中第一周",
            "easy",
            "school_life",
            """Last Monday was my first day at Green Hill Middle School. I felt both excited and nervous. The campus was larger than my old primary school. There were three tall buildings and a big playground with green trees.

My new classroom is on the second floor. Our class teacher, Mr. Wang, smiled and said, "Welcome, Class 7A." He asked us to introduce ourselves in English. At first, my face turned red. Then I said my name and my favorite sport—football. Some classmates clapped softly.

At lunchtime, I got lost near the library. A girl from Class 7B helped me find the dining hall. We talked about our hobbies. She likes reading animal stories, and I like collecting stamps. After school, I joined the football club for thirty minutes. My legs were tired, but I felt happy.

In the evening, I wrote in my diary: "Middle school is new, but kindness makes it easier." I hope next week will be even better.""",
            [
                q("r07a-01-q1", "detail", "Where is the writer's classroom?", "B",
                  [("A", "On the first floor"), ("B", "On the second floor"), ("C", "Near the playground"), ("D", "In the library")],
                  "The passage says the classroom is on the second floor."),
                q("r07a-01-q2", "detail", "Who helped the writer find the dining hall?", "C",
                  [("A", "Mr. Wang"), ("B", "A football coach"), ("C", "A girl from Class 7B"), ("D", "A librarian")],
                  "A girl from Class 7B helped near the library."),
                q("r07a-01-q3", "main_idea", "What is the passage mainly about?", "A",
                  [("A", "The writer's first days at a new school"), ("B", "How to play football well"), ("C", "A big library building"), ("D", "A stamp collection show")],
                  "The text describes the first week at middle school."),
                q("r07a-01-q4", "vocabulary", "What does \"nervous\" mean in the first paragraph?", "B",
                  [("A", "Angry"), ("B", "A little worried or uneasy"), ("C", "Hungry"), ("D", "Bored")],
                  "Nervous means uneasy or worried about something new.", "nervous"),
                q("r07a-01-q5", "inference", "Why did the writer feel happy after the football club?", "D",
                  [("A", "He won a big match"), ("B", "He got a new stamp"), ("C", "Mr. Wang gave him a prize"), ("D", "He enjoyed trying something new despite being tired")],
                  "He was tired but happy after joining the club."),
            ],
            7,
            0,
        )
    )
    sets.append(
        make_set(
            "r07a-02",
            "g7a",
            "A Rainy Picnic Plan",
            "被雨打乱的野餐计划",
            "easy",
            "family_life",
            """Last Saturday, my family planned a picnic in Riverside Park. Mum made sandwiches and fruit salad. Dad put a blanket and a ball into the car. My little brother packed his toy cars. We left home at nine o'clock under a bright sky.

Halfway to the park, dark clouds covered the sun. Then heavy rain began to fall. Dad parked near a small café. We waited and watched the rain dance on the windows. My brother looked sad. "Our picnic is broken," he said.

Mum smiled and said, "We can have an indoor picnic." We bought warm tea and sat at a corner table. We shared the sandwiches and played a card game. Dad told a funny story about a picnic when he was a boy. Soon my brother was laughing again.

When the rain stopped, we walked under the trees for ten minutes. The air smelled fresh. On the way home, I thought: a good plan can change, but a happy family day can still happen.""",
            [
                q("r07a-02-q1", "detail", "What did Mum prepare for the picnic?", "A",
                  [("A", "Sandwiches and fruit salad"), ("B", "Pizza and ice cream"), ("C", "Only warm tea"), ("D", "Toy cars")],
                  "Mum made sandwiches and fruit salad."),
                q("r07a-02-q2", "detail", "Where did the family wait during the rain?", "C",
                  [("A", "At home"), ("B", "In the library"), ("C", "In a small café"), ("D", "On the playground")],
                  "Dad parked near a small café."),
                q("r07a-02-q3", "main_idea", "What lesson does the writer learn?", "B",
                  [("A", "Never leave home on Saturday"), ("B", "Plans can change but family time can still be happy"), ("C", "Rain always ruins weekends"), ("D", "Card games are better than picnics")],
                  "The ending shows happiness can remain after a plan changes."),
                q("r07a-02-q4", "inference", "Why did the little brother look sad at first?", "A",
                  [("A", "He thought the picnic was ruined by rain"), ("B", "He lost his toy cars"), ("C", "He disliked sandwiches"), ("D", "Dad refused to stop")],
                  "He said the picnic was broken when rain came."),
                q("r07a-02-q5", "writer_attitude", "How does Mum react to the rain?", "D",
                  [("A", "She gets angry at Dad"), ("B", "She cancels the whole day"), ("C", "She ignores the children"), ("D", "She stays positive and suggests a new idea")],
                  "Mum suggests an indoor picnic with a smile."),
            ],
            7,
            1,
        )
    )
    sets.append(
        make_set(
            "r07a-03",
            "g7a",
            "The Lost Library Book",
            "丢失的图书馆书",
            "easy",
            "school_life",
            """Two weeks ago, I borrowed a book called *Star Friends* from the school library. I planned to finish it before the due date. But after PE class, I could not find it in my bag.

I checked my desk, the classroom cupboard, and the playground bench. Nothing. I felt worried because library rules say late or lost books must be reported. At last I told Miss Li, the librarian. She did not shout. She asked me where I last read the book.

Then I remembered: I read under the tree near the art room at lunch. We walked there together. The book was under a pile of leaves! Miss Li was glad, and so was I. She reminded me to put books back into my bag at once.

Now I keep a small checklist: borrow, read, return to bag, check due date. Looking after books is part of being a responsible student.""",
            [
                q("r07a-03-q1", "detail", "What was the name of the borrowed book?", "B",
                  [("A", "Moon Friends"), ("B", "Star Friends"), ("C", "Art Rules"), ("D", "PE Stories")],
                  "The book was called Star Friends."),
                q("r07a-03-q2", "detail", "Where was the book finally found?", "C",
                  [("A", "In the cupboard"), ("B", "On a playground bench"), ("C", "Under leaves near the art room"), ("D", "In Miss Li's office")],
                  "It was under leaves by the art room tree."),
                q("r07a-03-q3", "vocabulary", "What does \"due date\" mean here?", "A",
                  [("A", "The day when the book must be returned"), ("B", "The writer's birthday"), ("C", "The day of a PE test"), ("D", "The library opening hour")],
                  "Due date is the return deadline.", "due"),
                q("r07a-03-q4", "main_idea", "What is the main point of the story?", "D",
                  [("A", "Art rooms are dangerous"), ("B", "PE class is too long"), ("C", "Miss Li likes shouting"), ("D", "Students should care for borrowed books")],
                  "The ending stresses responsibility for library books."),
                q("r07a-03-q5", "inference", "Why didn't Miss Li shout at the student?", "B",
                  [("A", "She did not care about books"), ("B", "She preferred helping the student solve the problem calmly"), ("C", "The book was not important"), ("D", "The student paid money first")],
                  "She asked questions and helped search instead of shouting."),
            ],
            7,
            2,
        )
    )
    sets.append(
        make_set(
            "r07a-04",
            "g7a",
            "Feeding Birds in Winter",
            "冬天喂鸟",
            "easy",
            "nature",
            """In winter, many birds find it hard to get food. Snow covers the ground, and insects hide. Last January, our science club made simple bird feeders from clean plastic bottles. We cut small holes, filled them with seeds, and hung them on trees near the school gate.

Every morning, we checked the feeders. Sparrows and two magpies often came. We also wrote short notes: the weather, the number of birds, and what they ate. One cold day, fewer birds came. Our teacher said strong wind might make them stay in safer places.

We learned that helping animals needs patience. We should not chase birds or make loud noises. Clean water is also important on freezing days. At the end of the month, we shared photos in a class poster.

I used to walk past trees without looking. Now I notice wings, songs, and small lives around us. Caring for nature can start with a bottle of seeds.""",
            [
                q("r07a-04-q1", "detail", "What did the club use to make bird feeders?", "A",
                  [("A", "Clean plastic bottles"), ("B", "Metal boxes"), ("C", "Glass cups"), ("D", "Paper bags")],
                  "They used clean plastic bottles."),
                q("r07a-04-q2", "detail", "What did students record each morning?", "C",
                  [("A", "Only the temperature"), ("B", "Homework scores"), ("C", "Weather, bird numbers, and food"), ("D", "Bus times")],
                  "They noted weather, numbers, and what birds ate."),
                q("r07a-04-q3", "inference", "Why might fewer birds come on a windy cold day?", "B",
                  [("A", "The feeders were empty forever"), ("B", "Birds may stay in safer places in strong wind"), ("C", "All birds flew to another city"), ("D", "Students removed the feeders")],
                  "The teacher suggested wind made them stay safer."),
                q("r07a-04-q4", "main_idea", "What does the writer want readers to understand?", "D",
                  [("A", "Plastic bottles are toys"), ("B", "Magpies are dangerous"), ("C", "Winter is too cold for science"), ("D", "Small actions can help wildlife and open our eyes")],
                  "The ending stresses noticing and caring for nature."),
                q("r07a-04-q5", "detail", "What else is important for birds on freezing days?", "A",
                  [("A", "Clean water"), ("B", "Loud music"), ("C", "Chasing games"), ("D", "Closed windows only")],
                  "Clean water is also important on freezing days."),
            ],
            7,
            3,
        )
    )
    sets.append(
        make_set(
            "r07a-05",
            "g7a",
            "Learning to Ride a Bike",
            "学骑自行车",
            "easy",
            "daily_life",
            """I got a blue bicycle on my twelfth birthday, but I could not ride it well. Dad held the back of the seat while I pushed the pedals. Every time he let go, I stopped or fell onto the soft grass. I wanted to give up.

Grandma said, "Practice a little every day." So after dinner, I rode on the quiet path near our building. At first I looked at my feet. Then Dad told me to look forward. That small tip helped a lot. On the fifth evening, I rode twenty meters alone!

I still move slowly, and I wear a helmet every time. Mum says safety comes first. Next month I hope to ride to the community library with my cousin. Learning a new skill takes time, but each small success feels bright—like a green traffic light in my heart.""",
            [
                q("r07a-05-q1", "detail", "When did the writer get the bicycle?", "B",
                  [("A", "On New Year's Day"), ("B", "On the twelfth birthday"), ("C", "After a school race"), ("D", "From a teacher")],
                  "It was a twelfth birthday gift."),
                q("r07a-05-q2", "detail", "What tip from Dad helped most?", "A",
                  [("A", "Look forward while riding"), ("B", "Ride without a helmet"), ("C", "Practice only once a month"), ("D", "Close both eyes")],
                  "Dad told the writer to look forward."),
                q("r07a-05-q3", "vocabulary", "What does \"give up\" mean in the first paragraph?", "C",
                  [("A", "Give a gift"), ("B", "Ride faster"), ("C", "Stop trying"), ("D", "Ask for money")],
                  "Give up means stop trying.", "give"),
                q("r07a-05-q4", "main_idea", "What is the passage mainly about?", "D",
                  [("A", "Buying a expensive car"), ("B", "A dangerous road accident"), ("C", "A library rule book"), ("D", "Learning to ride a bike with practice and care")],
                  "The story is about learning to ride through practice."),
                q("r07a-05-q5", "inference", "Why does Mum insist on a helmet?", "B",
                  [("A", "It makes the bike faster"), ("B", "Safety is more important than speed"), ("C", "Helmets look fashionable only"), ("D", "The school requires a blue helmet")],
                  "Mum says safety comes first."),
            ],
            7,
            4,
        )
    )

    # ----- g7b (七下) -----
    sets.append(
        make_set(
            "r07b-01",
            "g7b",
            "A Letter to My Future Self",
            "给未来自己的一封信",
            "easy",
            "growth",
            """Our English teacher asked us to write a letter to ourselves three years later. I sat by the window and thought about who I might become.

Dear Future Me,

How is high school? Do you still play the <<flute>> on weekends? Today I am in Grade 7. I sometimes feel shy when I speak English in front of the class, but I practice with a mirror every night. I hope you are braver.

Please remember Grandma's garden. The roses need water in hot summers. Also, be kind to classmates who join our school late. Someone once helped me find the dining hall on my first day.

If you feel tired, rest—but do not forget small goals. Finish one page of reading each evening. Say thank you more often. I believe you can do it.

Yours,
Present Me""",
            [
                q("r07b-01-q1", "detail", "Who asked students to write the letter?", "A",
                  [("A", "The English teacher"), ("B", "Grandma"), ("C", "A high school coach"), ("D", "A mirror")],
                  "The English teacher gave the task."),
                q("r07b-01-q2", "detail", "What instrument does the writer mention?", "C",
                  [("A", "Piano"), ("B", "Guitar"), ("C", "Flute"), ("D", "Drum")],
                  "The letter asks about playing the flute.", "flute"),
                q("r07b-01-q3", "main_idea", "What is the letter mainly trying to do?", "B",
                  [("A", "Complain about Grade 7"), ("B", "Encourage the future self with hopes and reminders"), ("C", "Teach gardening skills only"), ("D", "Cancel weekend practice")],
                  "It shares hopes, reminders, and encouragement."),
                q("r07b-01-q4", "inference", "Why does the writer practice English with a mirror?", "D",
                  [("A", "To break the mirror"), ("B", "Because the teacher forbids speaking in class"), ("C", "To avoid homework"), ("D", "To become less shy when speaking")],
                  "The writer feels shy and practices to become braver."),
                q("r07b-01-q5", "detail", "What small daily goal is mentioned?", "A",
                  [("A", "Finish one page of reading each evening"), ("B", "Plant a new rose each hour"), ("C", "Skip saying thank you"), ("D", "Sleep in class")],
                  "The letter mentions one page of reading each evening."),
            ],
            7,
            0,
        )
    )
    sets.append(
        make_set(
            "r07b-02",
            "g7b",
            "The School Lost-and-Found Box",
            "学校失物招领箱",
            "easy",
            "school_life",
            """Near our school gate stands a yellow lost-and-found box. Students put keys, scarves, and water bottles inside. Every Friday, Student Council members sort the things and post a short list on the notice board.

Last month I lost my blue scarf. I checked the box and found three blue scarves! I looked for a small star sticker on the label—my mum had put it there. That sticker helped me take the right one home.

The council also reminds us: write your name on cups and put valuable cards in zip pockets. Finding lost things feels good, but keeping things safe feels better. The yellow box teaches care and honesty at the same time.""",
            [
                q("r07b-02-q1", "detail", "Where is the lost-and-found box?", "B",
                  [("A", "In the library"), ("B", "Near the school gate"), ("C", "On the playground roof"), ("D", "In the dining hall")],
                  "It stands near the school gate."),
                q("r07b-02-q2", "detail", "How did the writer identify the correct scarf?", "A",
                  [("A", "By a small star sticker on the label"), ("B", "By the price tag"), ("C", "By asking a stranger"), ("D", "By the smell of perfume")],
                  "Mum's star sticker marked the scarf."),
                q("r07b-02-q3", "main_idea", "What does the passage mainly tell us?", "C",
                  [("A", "How to make a yellow box"), ("B", "Why scarves are expensive"), ("C", "How a lost-and-found system works and why care matters"), ("D", "Student Council election rules")],
                  "It explains the box and lessons about care and honesty."),
                q("r07b-02-q4", "detail", "When do council members sort the items?", "D",
                  [("A", "Every Monday morning"), ("B", "Only in winter"), ("C", "Every day at noon"), ("D", "Every Friday")],
                  "Sorting happens every Friday."),
                q("r07b-02-q5", "inference", "Why does the council suggest writing names on cups?", "B",
                  [("A", "To decorate cups"), ("B", "To make it easier to return items to owners"), ("C", "To sell cups online"), ("D", "To hide valuable cards")],
                  "Names help return lost items correctly."),
            ],
            7,
            1,
        )
    )
    sets.append(
        make_set(
            "r07b-03",
            "g7b",
            "Planting Trees on Tree-Planting Day",
            "植树节种树",
            "medium",
            "nature",
            """On March 12, our class went to East Hill to plant trees. Each group carried a young sapling, a bucket, and gloves. The soil was soft after spring rain.

First we dug a hole deep enough for the roots. Then we placed the sapling carefully and covered the roots with soil. We poured water slowly so it could sink down. Our teacher said roots need air too, so we should not press the soil too hard.

After two hours, twenty new trees stood in a line. We made wooden labels with the date and our group names. Some students joked that the trees would grow taller than us. I hope they grow strong and clean the air for people who walk on East Hill later.""",
            [
                q("r07b-03-q1", "detail", "When did the class plant trees?", "A",
                  [("A", "On March 12"), ("B", "On New Year's Day"), ("C", "In late autumn only"), ("D", "On every Friday")],
                  "Tree-Planting Day activity was on March 12."),
                q("r07b-03-q2", "detail", "Why shouldn't students press the soil too hard?", "C",
                  [("A", "Hard soil looks ugly"), ("B", "Buckets will break"), ("C", "Roots need air"), ("D", "Labels will fall")],
                  "The teacher said roots need air."),
                q("r07b-03-q3", "main_idea", "What is the passage mainly about?", "B",
                  [("A", "A sports meeting on East Hill"), ("B", "A class tree-planting activity and its hope for the future"), ("C", "How to make plastic labels"), ("D", "A rainstorm warning")],
                  "It describes planting trees and hoping they help the hill."),
                q("r07b-03-q4", "detail", "How many new trees did they plant?", "D",
                  [("A", "Two"), ("B", "Twelve"), ("C", "Twenty-two"), ("D", "Twenty")],
                  "Twenty new trees stood in a line."),
                q("r07b-03-q5", "inference", "Why did they make wooden labels?", "A",
                  [("A", "To record the date and group names for the trees"), ("B", "To cook food"), ("C", "To block the wind forever"), ("D", "To replace the buckets")],
                  "Labels showed the date and group names."),
            ],
            8,
            2,
        )
    )
    sets.append(
        make_set(
            "r07b-04",
            "g7b",
            "My Neighbour's Guide Dog",
            "邻居的导盲犬",
            "easy",
            "values_narrative",
            """Uncle Zhao lives next door. He cannot see well, so he walks with a guide dog named Lucky. Lucky wears a special harness and walks steadily on the pavement.

At first I wanted to pet Lucky, but Uncle Zhao explained politely that guide dogs are working. We should not call them or offer food while they are on duty. That surprised me, but it made sense.

One evening, Lucky stopped before a broken tile on the path. Uncle Zhao thanked the dog softly and chose another way. I learned that respect can be quiet: give working animals space, and help people when they ask. Now when I see Lucky, I smile but I keep my hands to myself.""",
            [
                q("r07b-04-q1", "detail", "What is the guide dog's name?", "B",
                  [("A", "Happy"), ("B", "Lucky"), ("C", "Uncle"), ("D", "Tile")],
                  "The dog is named Lucky."),
                q("r07b-04-q2", "detail", "What should people avoid while a guide dog is working?", "A",
                  [("A", "Calling it or offering food"), ("B", "Walking on the pavement"), ("C", "Smiling politely"), ("D", "Wearing a harness themselves")],
                  "Do not call or feed working guide dogs."),
                q("r07b-04-q3", "main_idea", "What lesson does the writer learn?", "C",
                  [("A", "All dogs dislike people"), ("B", "Broken tiles are toys"), ("C", "Respect means giving working animals space"), ("D", "Guide dogs need loud praise all day")],
                  "Respect can be quiet space for working animals."),
                q("r07b-04-q4", "inference", "Why did Lucky stop before the broken tile?", "D",
                  [("A", "To play a game"), ("B", "To ask for food"), ("C", "Because Uncle Zhao shouted"), ("D", "To keep Uncle Zhao safe from danger")],
                  "Stopping protected Uncle Zhao from the broken tile."),
                q("r07b-04-q5", "writer_attitude", "How does the writer treat Lucky now?", "B",
                  [("A", "Pets the dog every time"), ("B", "Smiles but does not touch while it works"), ("C", "Offers candy on the path"), ("D", "Calls the dog loudly")],
                  "The writer smiles but keeps hands away."),
            ],
            7,
            3,
        )
    )
    sets.append(
        make_set(
            "r07b-05",
            "g7b",
            "Cooking Fried Rice with Dad",
            "和爸爸一起炒饭",
            "easy",
            "family_life",
            """Sunday afternoon, Dad taught me to cook egg fried rice. We washed rice, cut spring onions, and beat two eggs. Dad said the pan must be hot before the oil goes in.

I poured the eggs too early and they stuck a little. Dad laughed and said mistakes are part of learning. We tried again. This time the rice jumped in the pan with a happy sound. We added peas and a little salt.

When Mum came home, she tasted a spoonful and gave a thumbs-up. The kitchen smelled wonderful. I wrote the steps in my notebook so I can cook again next week. Food made together tastes warmer than food bought outside.""",
            [
                q("r07b-05-q1", "detail", "What dish did they cook?", "A",
                  [("A", "Egg fried rice"), ("B", "Tomato soup"), ("C", "Birthday cake"), ("D", "Dumplings")],
                  "They cooked egg fried rice."),
                q("r07b-05-q2", "detail", "What happened when eggs were poured too early?", "C",
                  [("A", "The pan broke"), ("B", "Mum got angry"), ("C", "The eggs stuck a little"), ("D", "The rice disappeared")],
                  "The eggs stuck a little."),
                q("r07b-05-q3", "main_idea", "What is the passage mainly about?", "B",
                  [("A", "Buying dinner outside"), ("B", "Learning to cook with Dad and enjoying family food"), ("C", "A restaurant review"), ("D", "How to grow peas")],
                  "It is about cooking together and family warmth."),
                q("r07b-05-q4", "inference", "Why did the writer write the steps down?", "D",
                  [("A", "For an English test only"), ("B", "To sell the notebook"), ("C", "Because Dad forbade cooking again"), ("D", "To remember how to cook the dish later")],
                  "The notebook helps cook again next week."),
                q("r07b-05-q5", "writer_attitude", "How does Dad treat the writer's mistake?", "A",
                  [("A", "Kindly, saying mistakes are part of learning"), ("B", "Angrily, forbidding practice"), ("C", "Silently, leaving the kitchen"), ("D", "By calling a chef at once")],
                  "Dad laughed and encouraged learning from mistakes."),
            ],
            7,
            4,
        )
    )

    # Continue with g8b, g9a, g9b in a second helper to keep this file structured
    sets.extend(_build_g8b_sets())
    sets.extend(_build_g9_sets())
    return sets


def _build_g8b_sets() -> list[dict]:
    out: list[dict] = []
    out.append(
        make_set(
            "r08b-01",
            "g8b",
            "A Weekend Museum Volunteer",
            "周末博物馆志愿者",
            "medium",
            "public_life",
            """Last month I became a weekend volunteer at the City History Museum. My job was simple but important: welcome visitors, point to the cloakroom, and remind people not to touch the old maps.

At first I spoke too quietly. An older volunteer, Aunt Hua, showed me how to stand near the entrance and smile. She said clear words help guests feel safe in a new place. After two Saturdays, I could answer common questions about opening hours and ticket prices.

One afternoon, a boy about seven years old cried because he lost his mum in the crowd. I stayed with him and used the museum phone to call the broadcast desk. Soon his mum arrived. She thanked me again and again.

Volunteering does not always mean doing grand things. Sometimes it means being calm, helpful, and ready. I still have much to learn, but I like the feeling of being useful outside my classroom.""",
            [
                q("r08b-01-q1", "detail", "Where did the writer volunteer?", "B",
                  [("A", "A hospital"), ("B", "The City History Museum"), ("C", "A football club"), ("D", "A bookstore")],
                  "The writer volunteered at the City History Museum."),
                q("r08b-01-q2", "detail", "What did Aunt Hua teach the writer?", "A",
                  [("A", "How to welcome guests with clear words and a smile"), ("B", "How to repair old maps"), ("C", "How to sell tickets online"), ("D", "How to drive a bus")],
                  "Aunt Hua coached welcoming skills."),
                q("r08b-01-q3", "main_idea", "What is the main idea?", "C",
                  [("A", "Museums should close on weekends"), ("B", "Children should never visit museums"), ("C", "Volunteering can mean quiet, practical help"), ("D", "Only adults can volunteer")],
                  "The ending stresses being useful through calm help."),
                q("r08b-01-q4", "inference", "Why did the writer call the broadcast desk?", "D",
                  [("A", "To order lunch"), ("B", "To complain about maps"), ("C", "To ask for a higher salary"), ("D", "To help find the lost boy's mother")],
                  "The boy was lost and needed his mum."),
                q("r08b-01-q5", "writer_attitude", "How does the writer feel about volunteering?", "B",
                  [("A", "Bored and angry"), ("B", "Glad to be useful while still learning"), ("C", "Ready to quit forever"), ("D", "Only interested in money")],
                  "The writer likes feeling useful and still learning."),
            ],
            8,
            0,
        )
    )
    out.append(
        make_set(
            "r08b-02",
            "g8b",
            "Why We Recycle Paper at School",
            "为什么学校要回收废纸",
            "medium",
            "environment",
            """Our school placed blue bins for waste paper in every hallway. At first, some students threw snack bags into them. The student environment club made posters explaining what can and cannot go into the bins.

Paper recycling saves trees and energy. When used paper is collected cleanly, factories can turn it into new notebooks. Food oil and wet tissues, however, can <<spoil>> a whole bag of paper. That is why we ask everyone to keep the bins dry and tidy.

Each month, the club weighs the paper and reports the number in the morning broadcast. Last term we collected over two hundred kilograms. The money from recycling bought plants for the courtyard.

Recycling is not only a rule; it is a habit. If we take three seconds to choose the right bin, we protect resources we cannot quickly replace.""",
            [
                q("r08b-02-q1", "detail", "What colour are the paper bins?", "A",
                  [("A", "Blue"), ("B", "Red"), ("C", "Yellow"), ("D", "Black")],
                  "Blue bins were placed for waste paper."),
                q("r08b-02-q2", "vocabulary", "What does \"spoil\" mean in this passage?", "C",
                  [("A", "Clean carefully"), ("B", "Sell quickly"), ("C", "Damage or ruin"), ("D", "Decorate")],
                  "Oil and wet tissues can ruin recyclable paper.", "spoil"),
                q("r08b-02-q3", "detail", "How much paper did they collect last term?", "B",
                  [("A", "Twenty kilograms"), ("B", "Over two hundred kilograms"), ("C", "Two kilograms"), ("D", "Two thousand tons")],
                  "Over two hundred kilograms were collected."),
                q("r08b-02-q4", "main_idea", "What is the writer's main message?", "D",
                  [("A", "Snack bags belong in paper bins"), ("B", "Recycling is useless"), ("C", "Only teachers may recycle"), ("D", "Careful recycling is a small habit with real benefits")],
                  "The text links correct recycling to saving resources."),
                q("r08b-02-q5", "inference", "Why did the club make posters?", "A",
                  [("A", "To teach what should and should not enter the bins"), ("B", "To advertise snack shops"), ("C", "To cancel the morning broadcast"), ("D", "To sell blue paint")],
                  "Posters explained bin rules after misuse."),
            ],
            8,
            1,
        )
    )
    out.append(
        make_set(
            "r08b-03",
            "g8b",
            "The Chess Club Final",
            "象棋社决赛",
            "medium",
            "school_life",
            """On Friday afternoon, the school chess club held its spring final. I faced Liu Mei from Class 8C. The library meeting room was quiet except for the soft click of chess pieces.

I started well and took a pawn. Then I became impatient and moved too fast. Liu Mei stayed calm, protected her king, and slowly turned the game around. When I realized my mistake, it was too late. She won with a clever checkmate.

After the match, she shook my hand and said, "You played bravely in the opening." I felt disappointed, but not angry. Coach Zhang reminded us that chess trains both skill and character. Waiting and thinking matter more than rushing.

That evening I reviewed the game in my notebook. Next season I hope to be patient enough to finish what I start well.""",
            [
                q("r08b-03-q1", "detail", "Who was the writer's opponent?", "C",
                  [("A", "Coach Zhang"), ("B", "A primary student"), ("C", "Liu Mei from Class 8C"), ("D", "A library teacher")],
                  "The opponent was Liu Mei from Class 8C."),
                q("r08b-03-q2", "detail", "Why did the writer lose after a good start?", "A",
                  [("A", "Impatience led to fast, weak moves"), ("B", "The lights went out"), ("C", "Liu Mei broke the rules"), ("D", "The coach stopped the game")],
                  "The writer moved too fast and lost the advantage."),
                q("r08b-03-q3", "main_idea", "What does the writer learn from the final?", "B",
                  [("A", "Chess is only about luck"), ("B", "Patience and thinking matter as much as early success"), ("C", "Handshakes are unnecessary"), ("D", "Notebooks are useless")],
                  "Coach and reflection stress patience and character."),
                q("r08b-03-q4", "inference", "How did Liu Mei behave after winning?", "D",
                  [("A", "She laughed at the writer"), ("B", "She refused to shake hands"), ("C", "She left without a word"), ("D", "She was polite and encouraging")],
                  "She shook hands and praised the opening."),
                q("r08b-03-q5", "writer_attitude", "How does the writer feel that evening?", "A",
                  [("A", "Disappointed but ready to improve"), ("B", "Proud of rushing"), ("C", "Angry at the coach"), ("D", "Sure that chess should be banned")],
                  "The writer reviews the game to improve next season."),
            ],
            8,
            2,
        )
    )
    out.append(
        make_set(
            "r08b-04",
            "g8b",
            "A Power Cut on Study Night",
            "自习夜停电",
            "medium",
            "daily_life",
            """Last Wednesday night, I was reviewing maths when the lights suddenly went out. The whole building fell silent for a second, then neighbours opened doors with phone flashlights.

Mum found candles and placed them on the table carefully. Dad said the power company might be repairing lines after strong wind. Without the computer, I could not watch online lessons, so I opened my paper workbook instead.

My sister and I took turns reading English aloud by candlelight. The words seemed slower and clearer. When the power returned forty minutes later, we cheered—but we also kept reading for another page.

Sometimes a small inconvenience teaches us to use what we have. Screens help, but books and calm minds still work when the Wi-Fi disappears.""",
            [
                q("r08b-04-q1", "detail", "What was the writer doing when the power failed?", "B",
                  [("A", "Cooking dinner"), ("B", "Reviewing maths"), ("C", "Playing football"), ("D", "Repairing lines")],
                  "The writer was reviewing maths."),
                q("r08b-04-q2", "detail", "How long did the power cut last?", "C",
                  [("A", "Four minutes"), ("B", "Four hours"), ("C", "Forty minutes"), ("D", "All night")],
                  "Power returned after forty minutes."),
                q("r08b-04-q3", "main_idea", "What point does the writer make?", "A",
                  [("A", "We can keep learning with simple tools when technology fails"), ("B", "Candles are better than electricity forever"), ("C", "Online lessons should be cancelled"), ("D", "Neighbours should never open doors")],
                  "Books and calm study still work without Wi-Fi."),
                q("r08b-04-q4", "inference", "Why might the power company be repairing lines?", "D",
                  [("A", "Because of a festival parade"), ("B", "Because Mum bought candles"), ("C", "Because the writer finished maths"), ("D", "Because strong wind may have caused problems")],
                  "Dad mentioned repairs after strong wind."),
                q("r08b-04-q5", "detail", "What did the siblings do by candlelight?", "A",
                  [("A", "Took turns reading English aloud"), ("B", "Watched online videos"), ("C", "Repaired the computer"), ("D", "Went out to play")],
                  "They read English aloud by candlelight."),
            ],
            8,
            3,
        )
    )
    out.append(
        make_set(
            "r08b-05",
            "g8b",
            "Interviewing a Local Firefighter",
            "采访本地消防员",
            "medium",
            "community",
            """For our class newspaper, my partner and I interviewed Firefighter Chen at the station near the river. He has served for twelve years. His day includes equipment checks, practice drills, and community safety talks.

When we asked about danger, he said fear is normal, but training helps the team act together. The most important rule is never enter a risky place alone. He also told students to know two family meeting places in case of fire at home.

I used to think firefighters only run toward flames. Now I see their work includes prevention: teaching people to keep exits clear and to test smoke alarms. Before we left, Firefighter Chen let us try a heavy hose on the training ground—under safe guidance.

Courage, he said, is not the absence of fear. It is choosing responsibility when others need help.""",
            [
                q("r08b-05-q1", "detail", "How long has Firefighter Chen served?", "C",
                  [("A", "Two years"), ("B", "Twenty years"), ("C", "Twelve years"), ("D", "Twelve months")],
                  "He has served for twelve years."),
                q("r08b-05-q2", "detail", "What home safety tip did he give?", "A",
                  [("A", "Know two family meeting places in case of fire"), ("B", "Hide under the bed forever"), ("C", "Remove all smoke alarms"), ("D", "Enter smoke alone to explore")],
                  "He advised two family meeting places."),
                q("r08b-05-q3", "main_idea", "What new understanding does the writer gain?", "B",
                  [("A", "Firefighters never feel fear"), ("B", "Firefighting includes prevention and teamwork, not only fighting flames"), ("C", "Hoses are toys for students"), ("D", "Newspapers should avoid interviews")],
                  "Prevention and training are part of the work."),
                q("r08b-05-q4", "vocabulary", "What does \"prevention\" refer to here?", "D",
                  [("A", "Only putting out big fires"), ("B", "Ignoring alarms"), ("C", "Running alone into danger"), ("D", "Actions that stop fires or reduce harm before they happen")],
                  "Prevention includes teaching and safety habits."),
                q("r08b-05-q5", "writer_attitude", "How does Firefighter Chen define courage?", "A",
                  [("A", "Choosing responsibility even when afraid"), ("B", "Never feeling fear"), ("C", "Working without training"), ("D", "Avoiding all community talks")],
                  "Courage is responsibility when others need help."),
            ],
            8,
            4,
        )
    )
    return out


def _build_g9_sets() -> list[dict]:
    out: list[dict] = []
    # g9a
    specs_9a = [
        (
            "r09a-01",
            "The Quiet Power of Note-Taking",
            "记笔记的安静力量",
            "medium",
            "study_skills",
            """Many Grade 9 students type quickly on tablets, yet they forget ideas after class. Research and classroom experience both suggest that writing short notes by hand can deepen memory.

Good notes are not a full copy of the textbook. They capture key terms, examples, and questions you still do not understand. Leaving a blank margin helps you add answers later. Colour can mark definitions, but too much colour becomes decoration without thinking.

I used to photograph every slide. Now I write three sentences after each lesson: what mattered, what confused me, and what I will review tonight. The habit takes five minutes and saves panic before exams.

Tools change, but attention is still the real study engine. A pen cannot replace thinking—yet it often invites it.""",
        ),
        (
            "r09a-02",
            "Should Homework Have a Time Limit?",
            "作业该不该设时限？",
            "medium",
            "school_debate",
            """Our school surveyed students about homework load. Many said they spend more than two hours each night, especially before exams. Teachers argued that practice builds skill, while some parents worried about sleep and stress.

A fair policy may not delete homework, but it can set clearer limits. For example, major subjects could coordinate so that big projects are not due on the same day. Teachers might offer challenge tasks as optional extensions instead of endless worksheets.

Students also share responsibility. Starting early and asking questions in class reduce last-minute pressure. Homework should deepen learning, not prove who can stay awake longest.

The debate continues, but listening to both sides is already a step toward balance.""",
        ),
        (
            "r09a-03",
            "A City Without Plastic Bags",
            "没有塑料袋的城市",
            "medium",
            "environment",
            """Last year our city encouraged shops to stop giving free plastic bags. At first, people complained. They forgot cloth bags and held milk bottles awkwardly in their hands.

After three months, habits shifted. Markets sold cheap foldable bags near the entrance. Students designed posters showing how plastic breaks into tiny pieces that harm fish. Some cafés offered a small discount to customers who brought cups.

Change rarely feels comfortable on day one. Yet when rules, design, and education work together, daily life can become cleaner. I still forget my bag sometimes—but now I turn back for it, because the new normal expects me to.""",
        ),
        (
            "r09a-04",
            "The Science Behind a Fever",
            "发烧背后的科学",
            "medium",
            "health_science",
            """When you catch a cold, your body temperature may rise. A fever is not the enemy itself; it is often a sign that the immune system is fighting invaders. Higher temperature can make it harder for some germs to grow.

However, a very high fever needs care. Doctors advise rest, enough water, and medicine when necessary. Cold baths that shock the body are not always helpful. Watching for warning signs—such as unusual sleepiness or lasting pain—matters more than guessing alone.

Understanding fever helps us respond calmly. Panic wastes energy that the body needs for recovery. Science turns a scary feeling into a signal we can manage with good habits and timely help.""",
        ),
        (
            "r09a-05",
            "Remembering My Grandfather's Workshop",
            "祖父的修理铺",
            "medium",
            "values_narrative",
            """My grandfather repaired bicycles in a small workshop behind our old house. The air smelled of oil and metal. Neighbours came with broken chains and flat tires, and he always found a way.

He taught me to name tools before using them. \"Respect the work,\" he said, \"and the work will respect your hands.\" When I rushed, screws fell; when I slowed down, problems became puzzles.

He passed away last winter, but I still keep his small wrench. Whenever a project at school feels messy, I remember his patience. Skill is not only talent. It is attention repeated until care becomes natural.""",
        ),
    ]
    questions_9a = [
        [
            ("detail", "What habit does the writer use after each lesson?", "B",
             [("A", "Photograph every slide only"), ("B", "Write three review sentences"), ("C", "Skip all notes"), ("D", "Sleep immediately")],
             "Three sentences: mattered, confused, review tonight."),
            ("main_idea", "What is the main claim?", "A",
             [("A", "Thoughtful note-taking supports memory better than empty copying"), ("B", "Tablets must be banned"), ("C", "Colour is always bad"), ("D", "Exams need no review")],
             "Hand notes that capture thinking deepen memory."),
            ("inference", "Why can too much colour be a problem?", "C",
             [("A", "Pens run out of ink only"), ("B", "Teachers dislike art"), ("C", "Decoration may replace real thinking"), ("D", "Margins become illegal")],
             "Too much colour becomes decoration without thinking."),
            ("detail", "How long does the writer's after-class habit take?", "D",
             [("A", "One hour"), ("B", "Half a day"), ("C", "Thirty seconds"), ("D", "About five minutes")],
             "The habit takes five minutes."),
            ("writer_attitude", "How does the writer view pens and thinking?", "B",
             [("A", "Pens replace thinking completely"), ("B", "Pens cannot replace thinking but often invite it"), ("C", "Thinking is useless"), ("D", "Only tablets invite attention")],
             "A pen invites thinking but does not replace it."),
        ],
        [
            ("detail", "What did many students report about homework time?", "A",
             [("A", "More than two hours each night"), ("B", "No homework at all"), ("C", "Only five minutes"), ("D", "Homework only on weekends")],
             "Many spend more than two hours nightly."),
            ("main_idea", "What balanced idea does the writer support?", "C",
             [("A", "Delete all homework forever"), ("B", "Ignore sleep and stress"), ("C", "Keep practice but coordinate limits and share responsibility"), ("D", "Give only online games")],
             "Limits, coordination, and student responsibility."),
            ("inference", "Why might optional challenge tasks help?", "B",
             [("A", "They replace sleep"), ("B", "They offer extension without forcing endless worksheets on everyone"), ("C", "They cancel major subjects"), ("D", "They hide survey results")],
             "Optional extensions avoid endless required worksheets."),
            ("detail", "What can reduce last-minute pressure for students?", "D",
             [("A", "Starting early and asking questions in class"), ("B", "Never studying"), ("C", "Hiding due dates"), ("D", "Waiting until midnight only")],
             "Starting early and asking in class help—option A."),
            ("writer_attitude", "How does the writer view the debate?", "A",
             [("A", "Listening to both sides helps move toward balance"), ("B", "Only teachers are right"), ("C", "Only students are right"), ("D", "Surveys are meaningless")],
             "Listening to both sides is a step toward balance."),
        ],
        [
            ("detail", "What policy did the city encourage?", "B",
             [("A", "Free plastic bags forever"), ("B", "Shops stopping free plastic bags"), ("C", "Banning cloth bags"), ("D", "Closing all markets")],
             "Shops were encouraged to stop free plastic bags."),
            ("detail", "What discount did some cafés offer?", "A",
             [("A", "A small discount for bringing cups"), ("B", "Free phones"), ("C", "Double plastic bags"), ("D", "Free fish")],
             "Cafés discounted customers who brought cups."),
            ("main_idea", "What does the passage suggest about change?", "C",
             [("A", "Change is impossible"), ("B", "Complaints stop all rules"), ("C", "Rules, design, and education together can shift daily habits"), ("D", "Only posters matter")],
             "Combined measures shift habits over time."),
            ("inference", "Why does the writer sometimes turn back for a bag?", "D",
             [("A", "Bags are illegal"), ("B", "Milk bottles are lighter"), ("C", "The café pays cash for bags"), ("D", "The new normal expects bringing one's own bag")],
             "The new normal expects people to bring bags."),
            ("vocabulary", "What does \"new normal\" mean here?", "B",
             [("A", "A temporary holiday"), ("B", "A changed everyday expectation"), ("C", "A broken rule"), ("D", "A science exam")],
             "New normal means the changed everyday habit."),
        ],
        [
            ("detail", "What can a fever often signal?", "A",
             [("A", "The immune system is fighting invaders"), ("B", "The body wants more sugar only"), ("C", "Exercise is forbidden forever"), ("D", "Germs always disappear instantly")],
             "Fever often shows the immune system fighting."),
            ("detail", "What do doctors advise for fever care?", "C",
             [("A", "Shocking cold baths only"), ("B", "Ignoring all warning signs"), ("C", "Rest, water, and medicine when needed"), ("D", "Running a marathon")],
             "Rest, water, and medicine when necessary."),
            ("main_idea", "What is the writer's key message?", "B",
             [("A", "Fever is always harmless"), ("B", "Understanding fever helps calm, informed responses"), ("C", "Science is useless in illness"), ("D", "Students should never see doctors")],
             "Science helps manage fever calmly."),
            ("inference", "Why is panic unhelpful during illness?", "D",
             [("A", "It cools the body perfectly"), ("B", "It replaces medicine forever"), ("C", "It shortens fever instantly"), ("D", "It wastes energy needed for recovery")],
             "Panic wastes energy the body needs."),
            ("writer_attitude", "How should people treat a very high fever?", "A",
             [("A", "With care and attention to warning signs"), ("B", "By guessing alone without help"), ("C", "By celebrating it"), ("D", "By avoiding water")],
             "Very high fever needs care and watching signs."),
        ],
        [
            ("detail", "What did the grandfather repair?", "B",
             [("A", "Computers"), ("B", "Bicycles"), ("C", "Airplanes"), ("D", "Phones")],
             "He repaired bicycles."),
            ("detail", "What saying did he teach about work?", "A",
             [("A", "Respect the work, and the work will respect your hands"), ("B", "Rush every screw"), ("C", "Talent needs no attention"), ("D", "Tools have no names")],
             "He taught respect for the work."),
            ("main_idea", "What does the writer learn from Grandfather?", "C",
             [("A", "Oil smells bad only"), ("B", "Workshops should close"), ("C", "Skill grows from patient, repeated attention"), ("D", "Wrenches are only decorations")],
             "Skill is attention repeated until care is natural."),
            ("inference", "Why keep the small wrench?", "D",
             [("A", "To sell it online"), ("B", "To forget the past"), ("C", "Because school requires metal tools"), ("D", "As a reminder of patience when work feels messy")],
             "The wrench recalls Grandfather's patience."),
            ("writer_attitude", "How does the writer view rushing?", "B",
             [("A", "Rushing always improves quality"), ("B", "Rushing causes mistakes; slowing turns problems into puzzles"), ("C", "Rushing impresses neighbours"), ("D", "Rushing replaces naming tools")],
             "When rushing, screws fell; slowing helped."),
        ],
    ]
    for i, (sid, title, title_zh, diff, topic, passage) in enumerate(specs_9a):
        qs_spec = questions_9a[i]
        questions = []
        for qi, (qtype, stem, correct, options, expl) in enumerate(qs_spec):
            # fix wrong option for homework q4 - I duplicated A in options incorrectly
            questions.append(q(f"{sid}-q{qi+1}", qtype, stem, correct, options, expl))
        # Fix r09a-02-q4 manually after loop if needed
        out.append(make_set(sid, "g9a", title, title_zh, diff, topic, passage, questions, 9, i))

    # Fix homework question 4 correct answer and options
    for s in out:
        if s["id"] == "r09a-02":
            s["questions"][3] = q(
                "r09a-02-q4",
                "detail",
                "What can reduce last-minute pressure for students?",
                "A",
                [
                    ("A", "Starting early and asking questions in class"),
                    ("B", "Never studying"),
                    ("C", "Hiding due dates"),
                    ("D", "Waiting until midnight only"),
                ],
                "Starting early and asking questions in class reduce pressure.",
            )

    # g9b
    specs_9b = [
        (
            "r09b-01",
            "Preparing for the Senior High Entrance Mindset",
            "中考心态准备",
            "hard",
            "exam_life",
            """As the senior high entrance exam approaches, many students focus only on more papers. Equal attention should go to mindset. Sleep, short breaks, and honest talks with teachers reduce the fog of anxiety.

Comparing scores with classmates every hour rarely helps. A better question is: which weak skill improved this week? Small progress charts turn fear into information. Parents can support by listening first, not only by adding tasks.

Exam days test knowledge, but the months before test habits. Calm is not empty; it is trained. A clear desk, a realistic timetable, and kindness toward yourself are part of preparation too.""",
        ),
        (
            "r09b-02",
            "How Online Rumours Travel",
            "网络谣言如何传播",
            "hard",
            "media_literacy",
            """A rumour can travel faster than a fact because emotion moves quickly. Shocking headlines invite clicks, and friends forward messages before checking sources. By the time a correction appears, many people have already formed an opinion.

Media literacy asks simple questions: Who published this? What evidence is shown? Can another reliable source confirm it? Screenshots without dates or links deserve extra doubt.

Schools now teach students to pause before sharing. Silence is not weakness when information is unclear; it is responsibility. In a networked world, each forward is a choice that shapes what others believe.""",
        ),
        (
            "r09b-03",
            "The Return of Second-Hand Book Markets",
            "旧书市的回归",
            "medium",
            "public_life",
            """On weekends, a second-hand book market opens near the old train station. Tables hold novels, textbooks, and comics with soft corners. Prices are low, and conversations are long.

Buyers look for knowledge and for stories behind the pages—notes in margins, tickets used as bookmarks. Sellers clear shelves at home and keep books circulating instead of becoming waste.

I bought a geography atlas for five yuan. Someone had underlined rivers in blue ink. Their study path briefly met mine. In a digital age, paper still connects strangers through shared curiosity.""",
        ),
        (
            "r09b-04",
            "Teamwork in a Robot Contest",
            "机器人比赛中的团队协作",
            "hard",
            "stem",
            """Our school robot team entered a city contest. Building the machine was hard, but sharing roles was harder. One member wanted to code alone; another insisted on redesigning the arms every night.

We held a short meeting and wrote a task board: sensors, code, testing, and documentation. Arguments became decisions with deadlines. When a wheel failed on stage, the tester already had a spare because the checklist required it.

We did not win first prize, but judges praised our clear logbook. Innovation needs talent; finishing needs teamwork. The robot moved, and so did our way of working together.""",
        ),
        (
            "r09b-05",
            "A Thank-You Speech I Almost Skipped",
            "差点跳过的感谢词",
            "medium",
            "growth",
            """At the end-of-term ceremony, I was asked to thank our volunteers in one minute. I nearly refused because public speaking still scares me. My teacher said courage grows in small rooms before big halls.

I wrote three lines: who helped, what they did, and why it mattered. I practiced twice in an empty classroom. On stage my voice shook, yet I finished. Applause was short, but relief was long.

Avoiding fear keeps it large; facing it with a plan makes it manageable. The speech was not perfect. It was honest—and that was enough for a Grade 9 afternoon.""",
        ),
    ]
    questions_9b = [
        [
            ("main_idea", "What does the writer emphasize besides doing papers?", "B",
             [("A", "Ignoring sleep"), ("B", "Mindset, rest, and healthy habits"), ("C", "Comparing scores every hour"), ("D", "Adding endless tasks only")],
             "Mindset and rest matter as much as papers."),
            ("detail", "What is a better question than constant score comparison?", "A",
             [("A", "Which weak skill improved this week?"), ("B", "Who is always first?"), ("C", "How to stay awake longest?"), ("D", "How to hide anxiety?")],
             "Focus on weekly skill improvement."),
            ("inference", "How can parents best support students?", "C",
             [("A", "Only by adding more tasks"), ("B", "By forbidding all breaks"), ("C", "By listening first, not only assigning work"), ("D", "By deleting all timetables")],
             "Parents can support by listening first."),
            ("vocabulary", "What does \"fog of anxiety\" suggest?", "D",
             [("A", "Clear sunny weather"), ("B", "A science experiment only"), ("C", "Perfect calm"), ("D", "Confused, heavy worry that clouds thinking")],
             "Fog of anxiety means worry that clouds the mind."),
            ("writer_attitude", "How is calm described?", "A",
             [("A", "Trained, not empty"), ("B", "Impossible before exams"), ("C", "A sign of laziness"), ("D", "Only for teachers")],
             "Calm is not empty; it is trained."),
        ],
        [
            ("main_idea", "Why can rumours spread quickly?", "A",
             [("A", "Emotion and forwarding outrun careful checking"), ("B", "Facts always move faster"), ("C", "Schools ban all news"), ("D", "Screenshots never lie")],
             "Emotion and unverified forwards spread fast."),
            ("detail", "Which question supports media literacy?", "C",
             [("A", "How shocking is the headline?"), ("B", "How many friends liked it?"), ("C", "Who published this and what evidence is shown?"), ("D", "How quickly can I forward it?")],
             "Ask about publisher and evidence."),
            ("inference", "Why may silence be responsible?", "B",
             [("A", "Because sharing is always illegal"), ("B", "Because unclear information should not be spread further"), ("C", "Because corrections are useless"), ("D", "Because friends dislike facts")],
             "Pause when information is unclear."),
            ("detail", "What deserves extra doubt?", "D",
             [("A", "Dated articles with links from reliable sources"), ("B", "Official corrections"), ("C", "Classroom textbooks"), ("D", "Screenshots without dates or links")],
             "Undated, unlinkable screenshots need doubt."),
            ("writer_attitude", "How does the writer view each forward?", "A",
             [("A", "As a choice that shapes others' beliefs"), ("B", "As meaningless noise"), ("C", "As always harmless"), ("D", "As required by schools")],
             "Each forward shapes what others believe."),
        ],
        [
            ("detail", "Where does the book market open?", "B",
             [("A", "Inside a digital app only"), ("B", "Near the old train station"), ("C", "On a school rooftop"), ("D", "At a hospital gate")],
             "Near the old train station."),
            ("detail", "How much did the writer pay for the atlas?", "A",
             [("A", "Five yuan"), ("B", "Fifty yuan"), ("C", "Five hundred yuan"), ("D", "Nothing")],
             "The atlas cost five yuan."),
            ("main_idea", "What value does the market show?", "C",
             [("A", "Paper books are worthless"), ("B", "Strangers cannot share curiosity"), ("C", "Second-hand books circulate knowledge and connect people"), ("D", "Only comics matter")],
             "Books circulate and connect strangers."),
            ("inference", "Why mention notes in margins?", "D",
             [("A", "To ban writing in books"), ("B", "To prove books are dirty"), ("C", "To sell ink"), ("D", "To show earlier readers leave traces that meet new readers")],
             "Margin notes connect past and present readers."),
            ("writer_attitude", "How does the writer feel about paper books in a digital age?", "B",
             [("A", "They are completely useless"), ("B", "They still connect people through shared curiosity"), ("C", "They should all be burned"), ("D", "They replace all digital tools")],
             "Paper still connects strangers through curiosity."),
        ],
        [
            ("detail", "What made teamwork harder than building?", "A",
             [("A", "Sharing roles and settling disagreements"), ("B", "Buying snacks"), ("C", "Finding the city map"), ("D", "Writing poems")],
             "Sharing roles was harder than building."),
            ("detail", "Why was a spare wheel ready?", "C",
             [("A", "Luck only"), ("B", "A judge donated it on stage"), ("C", "The checklist required spare parts for testing"), ("D", "The coder hid it as a joke")],
             "The tester had a spare because of the checklist."),
            ("main_idea", "What does the contest teach?", "B",
             [("A", "Talent alone always wins first prize"), ("B", "Innovation needs talent, but finishing needs teamwork"), ("C", "Logbooks are unimportant"), ("D", "Meetings waste all time")],
             "Teamwork enables finishing, not only talent."),
            ("inference", "How did the task board help?", "D",
             [("A", "It cancelled documentation"), ("B", "It removed all deadlines"), ("C", "It made arguments louder"), ("D", "It turned arguments into clear decisions with deadlines")],
             "Arguments became decisions with deadlines."),
            ("writer_attitude", "How does the team view not winning first prize?", "A",
             [("A", "Still valuable because judges praised their clear process"), ("B", "A total failure with no lessons"), ("C", "A reason to quit STEM"), ("D", "Proof that checklists fail")],
             "Judges praised the logbook; teamwork improved."),
        ],
        [
            ("detail", "Why did the writer nearly refuse the speech?", "B",
             [("A", "There was no microphone"), ("B", "Public speaking was still scary"), ("C", "Volunteers cancelled the ceremony"), ("D", "The teacher forbade speaking")],
             "Public speaking still scared the writer."),
            ("detail", "What three lines were in the draft?", "A",
             [("A", "Who helped, what they did, why it mattered"), ("B", "Jokes only"), ("C", "A full textbook chapter"), ("D", "A list of enemies")],
             "Who, what, why—three clear lines."),
            ("main_idea", "What lesson does the writer draw?", "C",
             [("A", "Avoid all stages forever"), ("B", "Perfect speeches are required"), ("C", "Facing fear with a plan makes it manageable"), ("D", "Applause is the only goal")],
             "Facing fear with a plan shrinks it."),
            ("inference", "Why practice in an empty classroom?", "D",
             [("A", "To hide from teachers forever"), ("B", "To avoid writing lines"), ("C", "Because stages need no practice"), ("D", "To build courage in a smaller space first")],
             "Courage grows in small rooms before big halls."),
            ("writer_attitude", "How does the writer judge the speech afterward?", "A",
             [("A", "Not perfect, but honest and enough"), ("B", "A complete disaster"), ("C", "Better than any professional talk"), ("D", "Useless because the voice shook")],
             "Honest was enough for that afternoon."),
        ],
    ]
    for i, (sid, title, title_zh, diff, topic, passage) in enumerate(specs_9b):
        qs = [
            q(f"{sid}-q{qi+1}", qtype, stem, correct, options, expl)
            for qi, (qtype, stem, correct, options, expl) in enumerate(questions_9b[i])
        ]
        out.append(make_set(sid, "g9b", title, title_zh, diff, topic, passage, qs, 9, i))
    return out


def build_ddl_and_backfill() -> str:
    return f"""-- Reading grade bands + content origin + expanded catalog
-- Generated by scripts/generate_reading_030.py — prefer regenerating over hand-edits.

-- 1) Schema
ALTER TABLE public.reading_sets
    ADD COLUMN IF NOT EXISTS grade_band TEXT;

ALTER TABLE public.reading_sets
    ADD COLUMN IF NOT EXISTS content_origin TEXT NOT NULL DEFAULT 'ai_generated';

UPDATE public.reading_sets
SET grade_band = 'g8a',
    content_origin = 'ai_generated'
WHERE set_id IN ('r08-01', 'r08-02', 'r08-03', 'r08-04', 'r08-05');

UPDATE public.reading_sets
SET content_origin = 'ai_generated'
WHERE content_origin IS NULL OR content_origin = '';

ALTER TABLE public.reading_sets
    ALTER COLUMN grade_band SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'reading_sets_grade_band_check'
    ) THEN
        ALTER TABLE public.reading_sets
            ADD CONSTRAINT reading_sets_grade_band_check
            CHECK (grade_band IN ('g7a', 'g7b', 'g8a', 'g8b', 'g9a', 'g9b'));
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'reading_sets_content_origin_check'
    ) THEN
        ALTER TABLE public.reading_sets
            ADD CONSTRAINT reading_sets_content_origin_check
            CHECK (content_origin IN ('ai_generated', 'editorial', 'licensed'));
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS reading_sets_grade_band_sort_idx
    ON public.reading_sets (grade_band, sort_order);

-- Reorder existing grade-8 starter sets into g8a sort range
UPDATE public.reading_sets SET sort_order = 300 WHERE set_id = 'r08-01';
UPDATE public.reading_sets SET sort_order = 301 WHERE set_id = 'r08-02';
UPDATE public.reading_sets SET sort_order = 302 WHERE set_id = 'r08-03';
UPDATE public.reading_sets SET sort_order = 303 WHERE set_id = 'r08-04';
UPDATE public.reading_sets SET sort_order = 304 WHERE set_id = 'r08-05';
"""


def build_seed(sets: list[dict]) -> str:
    lines: list[str] = ["-- 2) Seed new reading sets (AI-generated original practice)"]
    set_ids = [s["id"] for s in sets]
    id_list = ", ".join(f"'{sid}'" for sid in set_ids)

    for s in sets:
        lines.append(
            f"""
INSERT INTO public.reading_sets (
    set_id, title, title_zh, grade, grade_band, content_origin, difficulty, topic, passage,
    word_count, estimated_minutes, sort_order, version, updated_at
) VALUES (
    '{s["id"]}',
    {sql_str(s["title"])},
    {sql_str(s.get("titleZh"))},
    {int(s["grade"])},
    '{s["grade_band"]}',
    'ai_generated',
    {sql_str(s.get("difficulty", "medium"))},
    {sql_str(s.get("topic"))},
    {sql_str(s["passage"])},
    {int(s.get("wordCount", 0))},
    {int(s.get("estimatedMinutes", 8))},
    {int(s["sort_order"])},
    1,
    {UPDATED_AT}
)
ON CONFLICT (set_id) DO UPDATE SET
    title = EXCLUDED.title,
    title_zh = EXCLUDED.title_zh,
    grade = EXCLUDED.grade,
    grade_band = EXCLUDED.grade_band,
    content_origin = EXCLUDED.content_origin,
    difficulty = EXCLUDED.difficulty,
    topic = EXCLUDED.topic,
    passage = EXCLUDED.passage,
    word_count = EXCLUDED.word_count,
    estimated_minutes = EXCLUDED.estimated_minutes,
    sort_order = EXCLUDED.sort_order,
    version = EXCLUDED.version,
    updated_at = EXCLUDED.updated_at;"""
        )

    lines.append(
        f"""
DELETE FROM public.reading_questions
WHERE set_id IN ({id_list});"""
    )

    for s in sets:
        for q_index, question in enumerate(s["questions"]):
            highlight = question.get("highlight")
            highlight_sql = sql_str(highlight) if highlight else "NULL"
            lines.append(
                f"""
INSERT INTO public.reading_questions (
    question_id, set_id, question_type, stem, sort_order,
    correct_option_id, explanation, highlight_word, reward_token
) VALUES (
    '{question["id"]}',
    '{s["id"]}',
    {sql_str(question.get("type"))},
    {sql_str(question["stem"])},
    {q_index},
    '{question["correctOptionId"]}',
    {sql_str(question["explanation"])},
    {highlight_sql},
    {DEFAULT_REWARD}
);"""
            )
            for o_index, opt in enumerate(question["options"]):
                lines.append(
                    f"""
INSERT INTO public.reading_options (
    question_id, option_id, option_text, sort_order
) VALUES (
    '{question["id"]}',
    '{opt["id"]}',
    {sql_str(opt["text"])},
    {o_index}
)
ON CONFLICT (question_id, option_id) DO UPDATE SET
    option_text = EXCLUDED.option_text,
    sort_order = EXCLUDED.sort_order;"""
                )

    return "\n".join(lines)


def main() -> None:
    # Remove leftover broken draft list if any
    sets = build_new_sets()
    assert len(sets) == 25, f"expected 25 new sets, got {len(sets)}"
    by_band: dict[str, int] = {}
    for s in sets:
        by_band[s["grade_band"]] = by_band.get(s["grade_band"], 0) + 1
    assert by_band == {"g7a": 5, "g7b": 5, "g8b": 5, "g9a": 5, "g9b": 5}, by_band

    SEED_ROOT.mkdir(parents=True, exist_ok=True)
    catalog_path = SEED_ROOT / "catalog_030.json"
    catalog_path.write_text(
        json.dumps({"sets": [{"id": s["id"], "grade_band": s["grade_band"]} for s in sets]}, indent=2),
        encoding="utf-8",
    )

    sql = build_ddl_and_backfill() + "\n" + build_seed(sets) + "\n"
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(sql, encoding="utf-8")
    q_count = sum(len(s["questions"]) for s in sets)
    o_count = sum(len(q["options"]) for s in sets for q in s["questions"])
    print(f"Wrote {OUT}")
    print(f"new_sets={len(sets)} questions={q_count} options={o_count}")
    print(f"bands={by_band}")
    print(f"catalog={catalog_path}")


if __name__ == "__main__":
    main()
