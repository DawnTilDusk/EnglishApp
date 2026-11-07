"""New AI-authored fiction and informational practice; not exam or publisher material."""
from .common import reading as R, question as Q

READING = [
R('g7a', 'quiet-table', 'A Table for Quiet Reading', '安静阅读桌', '''
Our school library is small. After lunch, some students read there, but others talk about their games. Mia likes reading stories, yet she often loses her place when people speak loudly.

On Monday, she tells Mr Lee about the problem. He does not ask everyone to leave. Instead, he puts a blue card on one table near the window. The card says, "Quiet reading here." Students can still talk softly at the other tables.

Mia makes a small box for bookmarks. Anyone at the blue-card table can borrow one and return it before leaving. On Friday, six students sit at the table. One boy is reading his first story in English. "I can understand it better here," he says.

Mia is happy. She has not made the library bigger. She has helped people use the same room in a better way.
''', [
Q('detail', 'What makes reading difficult for Mia at first?', 'People speaking loudly.', ['The room being too cold.', 'Stories having no pictures.', 'The library closing after lunch.'], '第一段说明别人说话大声时，她容易找不到读到的位置。', 'she often loses her place when people speak loudly'),
Q('detail', 'Where does Mr Lee put the blue card?', 'On a table near the window.', ['On the library door.', 'Inside a game box.', 'On every chair.'], '第二段明确蓝卡放在靠窗的一张桌子上。', 'one table near the window'),
Q('detail', 'What should students do with a borrowed bookmark?', 'Return it before leaving.', ['Take it home forever.', 'Write a game score on it.', 'Give it only to Mr Lee.'], '借用的书签要在离开前归还。', 'return it before leaving'),
Q('inference', 'What does the boy think of the quiet table?', 'It helps him read the story.', ['It needs more games.', 'It should be outside.', 'It is only for older students.'], '男孩说在这里能更好地理解故事，说明安静桌帮助了阅读。', 'I can understand it better here'),
Q('main_idea', 'What is the story mainly about?', 'Making better use of a small library.', ['Building a bigger school.', 'Winning a reading prize.', 'Buying expensive bookmarks.'], '结尾点明没有扩大图书馆，而是改善同一空间的使用方式。', 'use the same room in a better way')]),

R('g7a', 'rainy-delivery', 'The Rainy-Day Delivery', '雨天送书', '''
Ben and his sister Lily want to take three books to their grandmother on Saturday. She lives near the bus station and enjoys reading at home. Before they leave, it starts to rain.

Ben puts the books in a paper bag. Lily looks at the wet street and says, "That bag may get wet." She finds a clean plastic box with a lid. The books fit inside, but the box is too large for Ben's schoolbag. They carry it together.

At the bus stop, an old man drops his ticket. Lily picks it up while Ben holds the box. They take the next bus and arrive ten minutes later than planned.

Grandmother opens the box. The books are dry. "Thank you for being careful," she says. Ben tells her about the ticket. "A little late is fine when you stop to help someone," she replies.
''', [
Q('detail', 'Who are the books for?', 'Ben and Lily’s grandmother.', ['Their bus driver.', 'An old man at school.', 'Their English teacher.'], '第一段说明他们要把三本书送给奶奶。', 'take three books to their grandmother'),
Q('reason', 'Why does Lily choose a plastic box?', 'To keep the books dry.', ['To carry more tickets.', 'To collect rainwater.', 'To hide a birthday cake.'], '纸袋可能淋湿，因此改用带盖的塑料盒保护书。', 'That bag may get wet'),
Q('detail', 'Why do they carry the box together?', 'It is too large for Ben’s schoolbag.', ['The lid is missing.', 'They have no hands free.', 'Grandmother asks them to.'], '盒子放不进书包，兄妹便一起搬。', "the box is too large for Ben's schoolbag"),
Q('detail', 'What does Lily pick up at the bus stop?', 'An old man’s ticket.', ['Her grandmother’s book.', 'A broken umbrella.', 'Ben’s schoolbag.'], '第三段先写老人掉车票，再写莉莉捡起它。', 'an old man drops his ticket. Lily picks it up'),
Q('inference', 'How does Grandmother feel about their late arrival?', 'She understands why they are late.', ['She refuses to read the books.', 'She wants them to leave.', 'She thinks helping is a mistake.'], '奶奶认为为了帮助别人而稍晚到达可以接受。', 'A little late is fine when you stop to help someone')]),

R('g7a', 'photo-labels', 'Names Behind the Photos', '照片背后的名字', '''
There are eight old photos in our classroom cupboard. They show sports days, school trips and a music show. No one knows all the people in them.

Our teacher asks each group to choose one photo and learn its story. My group chooses a picture of students beside a red bus. We first ask the school cook. She knows the girl at the front because the girl is her daughter. The cook tells us the photo was taken on a trip to a farm.

We write the date and the names we know on a card. We do not write on the photo itself. Then we put a question on the card: "Can you help us name the others?"

Two days later, a former student visits the school. He adds four names. Our card is not complete yet, but the photo is no longer just a picture of strangers.
''', [
Q('detail', 'Where do the students find the photos?', 'In a classroom cupboard.', ['Inside a red bus.', 'At a farm shop.', 'In the cook’s kitchen.'], '开头交代旧照片放在教室柜子里。', 'in our classroom cupboard'),
Q('detail', 'Which photo does the writer’s group choose?', 'Students beside a red bus.', ['A teacher playing the piano.', 'A cook preparing lunch.', 'Runners crossing a bridge.'], '第二段明确该组选择红色巴士旁的学生合影。', 'students beside a red bus'),
Q('reason', 'How does the cook recognize the girl?', 'The girl is her daughter.', ['The girl wears a name card.', 'The girl is the new teacher.', 'The girl works on the bus.'], '厨师认识照片前方女孩，因为那是她的女儿。', 'the girl is her daughter'),
Q('detail', 'Where do the students write the information?', 'On a separate card.', ['On the front of the photo.', 'On a bus window.', 'On the cupboard wall.'], '信息写在卡片上，原文还特意说明不写在照片上。', 'We do not write on the photo itself'),
Q('main_idea', 'What are the students trying to do?', 'Discover and record the stories of old photos.', ['Choose a new school cook.', 'Plan their own farm trip.', 'Sell photos to former students.'], '老师要求寻找照片故事，学生不断补充日期和人物信息。', 'choose one photo and learn its story')]),

R('g7a', 'small-stage', 'A Small Part in the Play', '小角色的作用', '''
Tom wants to play the king in the school play. However, the teacher gives him the part of a messenger. He has only two sentences to say. Tom feels disappointed and does not practise much.

During the first rehearsal, the king waits for a letter. Tom forgets to bring it onto the stage, so the next scene cannot begin. Everyone stops. The teacher explains, "Your letter tells the king where to go. A short part can still be important."

That evening, Tom puts a paper letter beside his shoes. Every time he practises entering the room, he remembers to take it. He also helps another student learn a long speech.

On the day of the show, Tom brings the letter at exactly the right moment. The story moves smoothly. He still says only two sentences, but this time he understands why they matter.
''', [
Q('detail', 'Which part does Tom receive?', 'A messenger.', ['A king.', 'A teacher.', 'A farmer.'], '老师分给汤姆的是信使角色。', 'the part of a messenger'),
Q('reason', 'Why does the first rehearsal stop?', 'Tom forgets the letter.', ['The audience leaves early.', 'The king loses his shoes.', 'The teacher changes the play.'], '信件没有送上舞台，下一场无法开始。', 'Tom forgets to bring it onto the stage'),
Q('detail', 'Where does Tom put the practice letter that evening?', 'Beside his shoes.', ['Under the school stage.', 'Inside the king’s bag.', 'Next to the classroom clock.'], '第三段写到他把纸信放在鞋子旁边提醒自己。', 'a paper letter beside his shoes'),
Q('inference', 'How does Tom change?', 'He takes his small role seriously.', ['He decides to stop acting.', 'He asks to remove the letter.', 'He refuses to help others.'], '他反复练习并准时递信，说明开始认真对待角色。', 'at exactly the right moment'),
Q('main_idea', 'What is the main lesson of the story?', 'A small role can make a real difference.', ['The longest speech is always best.', 'Good actors never need practice.', 'Only kings can lead a story.'], '老师的解释与结尾共同说明，台词少的角色也很重要。', 'A short part can still be important')]),

R('g7b', 'weekend-map', 'The Map That Changed', '会变化的周末地图', '''
Nora makes a walking map for her cousin, who will visit on Sunday. She marks the bakery, the river path and a little bookshop. The whole walk should take about an hour.

On Saturday, Nora checks the route herself. A sign says the river path is closed for repairs. She draws a new line through Green Street. This route is ten minutes longer, but it has a wide pavement. Near the bookshop, she notices that the shop does not open until eleven on Sundays. She moves their starting time from nine thirty to ten thirty.

When her cousin arrives, Nora gives him the revised map. Small notes explain the changes. "I thought a map only showed places," he says. Nora smiles. "This one also tells us when we can visit them."

They reach the bakery first, share a warm roll and continue along Green Street. The plan has changed, but the walk is a success.
''', [
Q('detail', 'Why does Nora make the map?', 'To guide her visiting cousin.', ['To repair the river path.', 'To advertise her bakery.', 'To find a lost schoolbag.'], '首句说明地图是为周日来访的表亲准备的。', 'makes a walking map for her cousin'),
Q('reason', 'Why does Nora change the route?', 'The river path is closed.', ['Green Street has no pavement.', 'The bakery moves away.', 'Her cousin cannot walk.'], '她实地检查时发现河边小路因维修关闭。', 'closed for repairs'),
Q('detail', 'How much longer is the new route?', 'Ten minutes.', ['Five minutes.', 'Thirty minutes.', 'One hour.'], '第二段明确新路线多走十分钟。', 'This route is ten minutes longer'),
Q('reason', 'Why do they start at ten thirty instead of nine thirty?', 'The bookshop opens later on Sundays.', ['The bakery closes at ten.', 'The river path opens at ten.', 'Her cousin dislikes breakfast.'], '书店周日十一点开门，推迟出发可配合营业时间。', 'the shop does not open until eleven on Sundays'),
Q('inference', 'What makes Nora’s plan useful?', 'She checks real conditions and updates it.', ['She keeps the shortest route at any cost.', 'She removes all the notes.', 'She visits only places she owns.'], '她实地核查路况与营业时间并修订地图，体现计划的实用性。', 'Small notes explain the changes')]),

R('g7b', 'seed-envelopes', 'One Envelope, Two Gardens', '一封种子两座花园', '''
At the spring fair, Asha receives an envelope of bean seeds. A note inside says, "Keep half and share half." She gives five seeds to her neighbour Mr Wu and plants the other five in pots on her balcony.

For the next two weeks, Asha and Mr Wu compare notes. Her pots get morning sunlight. His garden gets more afternoon sunlight. Asha waters a little every day, while Mr Wu checks the soil before watering. Neither of them says that one way is always best.

One of Asha's pots does not produce a plant. She wants to throw the soil away, but Mr Wu suggests waiting a few more days. A small green shoot finally appears on the eighteenth day.

At the next fair, Asha brings a notebook instead of a bag of beans. It contains dates, drawings and questions from both gardens. Sharing the seeds has given her someone to learn with.
''', [
Q('detail', 'How many seeds does Asha keep?', 'Five.', ['Two.', 'Ten.', 'Eighteen.'], '第一段说明她送出五粒，另五粒种在自家阳台。', 'plants the other five'),
Q('detail', 'When do Asha’s pots get sunlight?', 'In the morning.', ['Only at night.', 'Only in the late afternoon.', 'Never during the day.'], '第二段明确她的花盆接受早晨阳光。', 'Her pots get morning sunlight'),
Q('detail', 'What does Mr Wu do before watering?', 'He checks the soil.', ['He moves every plant indoors.', 'He counts all the leaves.', 'He asks Asha for water.'], '文中明确吴先生先检查土壤，再决定浇水。', 'checks the soil before watering'),
Q('detail', 'When does the late shoot appear?', 'On the eighteenth day.', ['On the first day.', 'After two months.', 'At the next spring fair.'], '第三段末句给出第十八天。', 'on the eighteenth day'),
Q('main_idea', 'What does Asha gain most from sharing the seeds?', 'A partner for learning.', ['A bigger balcony.', 'A prize for the largest bean.', 'A job selling envelopes.'], '最后一句直接点明，分享种子让她有了共同学习的人。', 'someone to learn with')]),

R('g7b', 'cupboard-list', 'The List on the Cupboard', '柜门上的清单', '''
Our art club has a cupboard full of brushes and paint. At first, anyone can take things, but nobody writes down what they use. One Thursday, we plan to paint a large poster and find that there is almost no blue paint left.

We could buy new paint immediately, but our club leader suggests checking the cupboard first. We find three small blue bottles behind a box. Some brushes are also hidden under old paper. We have enough materials after all.

After cleaning, we put a list on the door. It shows each material and its amount. A student who takes a bottle changes the number on the list. We check the numbers together at the end of each meeting.

A month later, we need yellow paint. This time, the list helps us order it before it runs out. Keeping things tidy does not mean we never buy anything; it means we know what we really need.
''', [
Q('detail', 'What problem do the students notice on Thursday?', 'They seem to have almost no blue paint.', ['The cupboard has no door.', 'Their poster is missing.', 'The art club has no members.'], '第一段说准备画海报时发现蓝色颜料似乎快没了。', 'almost no blue paint left'),
Q('detail', 'Where are the three blue bottles?', 'Behind a box.', ['Outside the classroom.', 'Under the poster.', 'In the leader’s car.'], '第二段明确蓝色小瓶藏在盒子后面。', 'three small blue bottles behind a box'),
Q('detail', 'What should a student do after taking a bottle?', 'Change the number on the list.', ['Remove the cupboard door.', 'Buy three new bottles.', 'Stop attending meetings.'], '取用者要更新清单上的数量。', 'changes the number on the list'),
Q('reason', 'Why can the club order yellow paint in time?', 'The list shows what is running low.', ['Yellow paint is always free.', 'The shop sends paint every day.', 'They no longer use blue paint.'], '持续记录库存帮助他们在黄色颜料用完前订购。', 'order it before it runs out'),
Q('main_idea', 'What does the passage mainly show?', 'Good records help a group use materials wisely.', ['Painting posters wastes all paint.', 'Small bottles are never useful.', 'Buying more is the only solution.'], '整理与库存记录让社团清楚真正所需的材料。', 'we know what we really need')]),

R('g7b', 'different-finish', 'Finishing in a Different Way', '另一种完成', '''
Ella enters a school fun run with her friend Dan. She wants to finish the two-kilometre route without stopping. They practise twice before the event, and she feels ready.

Halfway through the run, Dan slows down. His shoelace has come loose. Ella stops with him because the path is crowded. After he ties it, they start again. Later, a younger pupil cannot find the turn towards the playground. Ella points to the sign and walks with her until a teacher takes over.

Ella finishes later than she expected. At first, she looks unhappy. Her sports teacher asks what happened, then says, "You did not follow your original plan exactly, but you kept yourself and others safe."

Ella writes two notes in her training book. The first is to check her shoes before running. The second is to leave room in a plan for other people. Next time she will still try to run without stopping, but she will not ignore someone who needs help.
''', [
Q('detail', 'What is Ella’s original goal?', 'To finish the route without stopping.', ['To win a national race.', 'To run five kilometres.', 'To train every day for a month.'], '第一段直接写明她想不中途停下完成路线。', 'finish the two-kilometre route without stopping'),
Q('reason', 'Why does Dan slow down?', 'His shoelace is loose.', ['He loses his race number.', 'He cannot see a teacher.', 'He wants to visit the library.'], '第二段说明丹的鞋带松了。', 'His shoelace has come loose'),
Q('detail', 'How does Ella help the younger pupil?', 'She shows and walks part of the correct way.', ['She carries her to the finish.', 'She asks her to leave the event.', 'She gives her a new pair of shoes.'], '她指明标志并陪走到老师接手。', 'walks with her until a teacher takes over'),
Q('inference', 'What does the teacher value in Ella’s actions?', 'Care for safety and other people.', ['Finishing before everyone else.', 'Following a plan without any change.', 'Never speaking during a run.'], '老师称赞她保障自己和他人的安全。', 'kept yourself and others safe'),
Q('main_idea', 'What does Ella learn?', 'A goal can matter while helping others still comes first.', ['A training plan is always useless.', 'Helping others makes sport impossible.', 'Stopping once means failing completely.'], '结尾保留跑步目标，同时明确不忽视需要帮助的人。', 'she will not ignore someone who needs help')]),

R('g8a', 'repair-notes', 'The Repair Notebook', '维修记录本', '''
When a wheel on the classroom book trolley began to shake, everyone had an opinion. Some students wanted to replace the whole trolley. Others suggested adding more tape. Sixteen-year-old volunteer Kai asked a different question: had the problem happened before?

The librarian brought out a notebook from the cupboard. It recorded repairs made during the past year. Kai found that the same wheel had been tightened three times. Each repair worked for a few days, then the shaking returned. Simply repeating the same action was unlikely to solve the problem.

With permission, the librarian asked a trained repair worker to examine it. A small metal part was worn out. After replacing that part, the worker explained how to check the wheel safely. Students were told to report problems rather than attempt repairs themselves.

Kai added a short entry to the notebook, including the date and the worker's advice. A month later, the wheel was still steady. The trolley had not needed replacing, and the old notes had helped the school ask a better question.
''', [
Q('detail', 'What does Kai ask first?', 'Whether the problem has happened before.', ['How much tape each student owns.', 'Who will buy a new cupboard.', 'Whether books should be thrown away.'], '第一段末句是凯的首个问题：故障以前是否出现过。', 'had the problem happened before'),
Q('detail', 'What do the old notes reveal?', 'The same wheel has already been tightened three times.', ['The trolley has never been repaired.', 'The librarian built the trolley.', 'All four wheels are missing.'], '维修本记录同一车轮此前已拧紧三次。', 'the same wheel had been tightened three times'),
Q('reason', 'Why is repeating the earlier repair unlikely to help?', 'Its effects have repeatedly lasted only a few days.', ['The students dislike keeping records.', 'Tape is too expensive for the school.', 'The trolley is used only once a year.'], '每次修理只维持几天，说明同样操作没有解决根本原因。', 'Each repair worked for a few days'),
Q('detail', 'Who replaces the worn part?', 'A trained repair worker.', ['Kai working alone.', 'Every student in the class.', 'A visiting author.'], '图书管理员请专业维修人员检查并更换部件。', 'After replacing that part, the worker explained'),
Q('main_idea', 'What is the main value of the notebook?', 'Past records help people investigate a recurring problem.', ['It teaches children to repair all machines alone.', 'It proves every old trolley must be replaced.', 'It makes safety checks unnecessary.'], '记录帮助识别反复故障并提出更有针对性的问题。', 'the old notes had helped the school ask a better question')]),

R('g8a', 'lantern-feedback', 'A Better Evening Display', '改进晚间展览', '''
The community centre planned a small lantern display for families. A student team designed bright posters and shared a photo of the entrance online. They expected the colourful decorations to be the most useful part of their publicity.

After the first evening, visitors left comments. Several praised the lanterns, but one parent said the entrance was hard to find after dark. Another visitor had not known whether there were steps. The team realised their attractive photograph did not answer these practical questions.

For the second evening, they kept the picture but added a simple map. A volunteer checked the route and marked the entrance without steps. The new notice also stated the closing time and explained that children needed an adult with them. Volunteers stood at the two corners where visitors had previously become confused.

The next set of comments mentioned fewer difficulties finding the display. The team could not prove that the notice alone caused this change; the extra volunteers may also have helped. Still, they learned that useful information should be planned as carefully as an attractive design.
''', [
Q('detail', 'What does the team initially expect to attract families?', 'The colourful decorations in their publicity.', ['A free bus service.', 'An indoor sports competition.', 'A new restaurant menu.'], '第一段写团队原先重视装饰与漂亮的入口照片。', 'the colourful decorations to be the most useful part'),
Q('detail', 'What question does a visitor have after the first evening?', 'Whether there are steps at the entrance.', ['Whether the lanterns are for sale.', 'Whether children can design posters.', 'Whether the centre has a swimming pool.'], '第二段直接提及访客不了解入口是否有台阶。', 'whether there were steps'),
Q('detail', 'What does the revised notice include?', 'A map and the closing time.', ['The names of every visitor.', 'A promise of free food.', 'Directions to a different town.'], '新版信息既有地图，也写了闭馆时间。', 'The new notice also stated the closing time'),
Q('inference', 'Why can’t the team credit only the new notice for the improvement?', 'Extra volunteers may have helped too.', ['No visitors came the second evening.', 'All comments were written before the changes.', 'The team removed the lanterns.'], '团队同时增加志愿者，不能把变化只归因于公告。', 'the extra volunteers may also have helped'),
Q('main_idea', 'What does the team learn about publicity?', 'Practical information deserves as much care as appearance.', ['A beautiful picture answers every question.', 'Visitor comments should always be ignored.', 'Maps make all volunteers unnecessary.'], '结尾强调实用信息应与吸引人的设计一样认真规划。', 'useful information should be planned as carefully as an attractive design')]),
R('g8a', 'book-choices', 'Choosing Books for the Corner', '阅读角的选书', '''
The school gave Class Eight enough money for twelve new books. Each student suggested a favourite title. At first, the class planned to buy the twelve titles with the most votes. Then their teacher asked them to look at who had voted.

Almost all the suggestions came from students who already used the reading corner. Several quieter classmates had not written anything. When the book team spoke with them, they discovered that some wanted short books with clear pictures, while others hoped for books about practical skills.

The team did not cancel the vote. Instead, they reserved four places for needs that the first list had missed. They also borrowed sample copies before placing the order. One popular book turned out to have print that was too small for comfortable reading, so they chose another edition of the same story.

After a month, different students were borrowing the new books. The team kept a suggestion sheet beside the shelves. They had learned that listening to the loudest group was a starting point, rather than a complete picture of what the class needed.
''', [
Q('detail', 'How many new books can the class buy?', 'Twelve.', ['Four.', 'Eight.', 'Twenty.'], '首句写明经费足够购买十二本书。', 'twelve new books'),
Q('detail', 'Who supplied most of the first suggestions?', 'Students already using the reading corner.', ['Parents visiting the school.', 'Teachers from other classes.', 'Students from the bookshop.'], '第二段说明最初建议主要来自原本就在使用阅读角的学生。', 'students who already used the reading corner'),
Q('detail', 'What does the team do with four places on the list?', 'Reserve them for overlooked needs.', ['Remove them to save all the money.', 'Give them only to teachers.', 'Fill them with identical dictionaries.'], '团队预留四个名额满足初始清单遗漏的需求。', 'reserved four places for needs that the first list had missed'),
Q('reason', 'Why does the team change the edition of one story?', 'The original edition has uncomfortable small print.', ['The story has no ending.', 'The author withdraws the book.', 'No student likes the story.'], '样书试读发现字号太小，所以更换同一故事的版本。', 'print that was too small for comfortable reading'),
Q('main_idea', 'What is the passage mainly about?', 'Considering less-heard needs when making a group choice.', ['Avoiding all decisions by voting.', 'Choosing only the longest books.', 'Making every student read the same story.'], '结尾强调最积极群体的意见不能代表全班所有需求。', 'rather than a complete picture of what the class needed')]),

R('g8b', 'noise-diary', 'The Noise Diary', '噪声日记', '''
Residents near Elm Square disagreed about the noise from evening activities. Some blamed the basketball court, while others thought most noise came from delivery vehicles. The neighbourhood youth group offered to keep a diary instead of guessing.

For one week, volunteers stood at two agreed places between six and eight each evening. They noted the time, the kind of sound and whether it interrupted a short conversation. They did not record people's voices or photograph individuals. Their purpose was to describe a shared problem, not identify people to blame.

The diary showed that a brief period of vehicle unloading often overlapped with the final basketball game. On rainy evenings, both activities were less frequent. The volunteers were careful not to call their notes precise scientific measurements; different people might judge the same sound differently.

At a meeting, residents agreed to try moving unloading fifteen minutes earlier and reminding players of the closing time. They planned another week of notes after the changes. The diary had not settled every disagreement, but it had given neighbours specific times and situations to discuss.
''', [
Q('detail', 'Why does the youth group keep a diary?', 'To replace guesses with observations.', ['To sell recordings of residents.', 'To identify the best players.', 'To choose a new name for the square.'], '首段指出大家对来源有争议，青年组提议记录而非猜测。', 'keep a diary instead of guessing'),
Q('detail', 'What do the volunteers avoid doing?', 'Recording people’s voices.', ['Noting the time.', 'Standing at agreed places.', 'Describing types of sound.'], '第二段明确不录人声、不拍摄个人。', "They did not record people's voices"),
Q('detail', 'What often happens at the same time?', 'Vehicle unloading and the final basketball game.', ['A concert and a swimming lesson.', 'A school exam and a delivery break.', 'Morning shopping and late-night cleaning.'], '第三段记载卸货与最后一场篮球活动常有重叠。', 'vehicle unloading often overlapped with the final basketball game'),
Q('inference', 'Why do volunteers avoid calling the diary precise scientific measurements?', 'People can judge the same sound differently.', ['They forget to write any times.', 'They work only on one sunny morning.', 'They measure every sound with identical equipment.'], '原文明确人的主观判断存在差异。', 'different people might judge the same sound differently'),
Q('purpose', 'Why is another week of notes planned?', 'To observe what happens after the changes.', ['To erase the first diary.', 'To prove the players are always wrong.', 'To replace all evening activities with silence.'], '先调整时间，再记录一周，是为了观察调整后的情况。', 'another week of notes after the changes')]),

R('g8b', 'museum-caption', 'More Than a Short Label', '展品标签不只是短句', '''
For a local history exhibition, students wrote labels for everyday objects donated by residents. Lina's group received a metal lunch box. Their first label described its colour, size and age. It was accurate, but visitors usually walked past without stopping.

The group returned to the woman who had donated it. She explained that her father had carried the box to work, then used it to store letters after retiring. A dent in the lid came from a fall during a bus journey, not from factory work as the students had guessed.

With the woman's permission, they added two sentences about those memories. They kept the basic description and clearly marked the memories as her account. They did not present every detail as something proved by the object itself.

More visitors paused at the display, and one asked how families decide which ordinary things to keep. Lina realised that a good label could help people ask questions. It did not need to turn a lunch box into a treasure or hide the limits of what was known.
''', [
Q('detail', 'What does the first label mainly describe?', 'The box’s physical features and age.', ['The donor’s journey abroad.', 'A factory’s daily timetable.', 'The price of a new lunch box.'], '第一段列出初版标签写了颜色、大小和年代。', 'its colour, size and age'),
Q('detail', 'What caused the dent in the lid?', 'A fall during a bus journey.', ['A machine in a factory.', 'A student dropping it at the exhibition.', 'A fire at the donor’s home.'], '捐赠者说明凹痕来自乘公交时摔落，并非学生猜测的工作事故。', 'a fall during a bus journey'),
Q('detail', 'How does the group present the memories?', 'As the donor’s account, with permission.', ['As laboratory evidence.', 'As a story invented by visitors.', 'As facts with no source named.'], '第三段说明经同意使用回忆，并明确标示来源。', 'clearly marked the memories as her account'),
Q('inference', 'Which claim would the students be least justified in making?', 'Every remembered detail is proved by the box itself.', ['The lid has a dent.', 'A woman donated the box.', 'The first label listed the box’s size.'], '原文特别区分实物可证明的信息和个人回忆。', 'did not present every detail as something proved by the object itself'),
Q('main_idea', 'What makes the revised label better?', 'It adds human context while keeping uncertainty clear.', ['It gives the object an invented high price.', 'It removes every physical description.', 'It replaces the object with a photograph.'], '新版加入人物故事，同时没有隐去证据边界。', 'hide the limits of what was known')]),

R('g8b', 'practice-calendar', 'A Calendar with Blank Spaces', '留白的练习日历', '''
When Leo joined the school guitar group, he filled every evening of his calendar with an hour of practice. He thought a serious learner should never leave a blank space. During the first week, however, homework and a family visit meant he missed two evenings. He felt the whole plan had failed.

His music teacher asked him to describe what happened during the evenings he did practise. Leo admitted that he often played the easy opening repeatedly and avoided a difficult change between two chords. The number of minutes alone did not show what he was learning.

They made a smaller plan: fifteen focused minutes on the difficult change, followed by a song he enjoyed. Two evenings were left unplanned so he could move a session when necessary. In his notebook, he wrote what had become easier and what still needed attention.

After three weeks, Leo could make the change more smoothly. He did not conclude that short practice was always better for everyone. He had found a plan that matched his present needs and was easier to keep.
''', [
Q('detail', 'How much practice does Leo first plan each evening?', 'One hour.', ['Ten minutes.', 'Fifteen minutes.', 'Three hours.'], '第一段写他最初每晚安排一小时。', 'an hour of practice'),
Q('detail', 'What does Leo tend to avoid during practice?', 'A difficult change between two chords.', ['The easy opening.', 'His favourite song.', 'Writing the date.'], '第二段交代他重复简单开头、回避难点。', 'avoided a difficult change between two chords'),
Q('reason', 'Why are two evenings left unplanned?', 'To allow sessions to move when necessary.', ['To prevent all future practice.', 'To make room for daily competitions.', 'To force his family to change their plans.'], '留白是为了必要时调整练习时间。', 'so he could move a session when necessary'),
Q('detail', 'What does Leo record in his notebook?', 'Improvements and remaining difficulties.', ['Only the price of his guitar.', 'Every song on the radio.', 'The names of all concert audiences.'], '笔记记录哪些内容变容易、哪些仍需关注。', 'what had become easier and what still needed attention'),
Q('inference', 'Which conclusion best matches Leo’s experience?', 'A workable plan should fit the learner’s current needs.', ['Fifteen minutes is best for every skill and person.', 'Missing one evening makes all practice useless.', 'Easy parts should be the only practice focus.'], '结尾避免一概而论，强调适合当前需求且可持续的计划。', 'matched his present needs and was easier to keep')]),

R('g9a', 'headline-correction', 'When the Headline Was Wrong', '标题出错之后', '''
The student newspaper published a headline saying that the school would remove all paper notices. It was based on a short interview about a trial digital noticeboard in one corridor. By lunchtime, several students were asking how clubs without regular computer access would advertise events.

Editor Mei checked the interview notes. The speaker had described a limited trial, not a decision covering the whole school. Mei could have quietly changed the online headline. Instead, she discussed a visible correction with the team. They replaced the headline, added a note explaining the error, and linked to a fuller description of the trial.

Some readers thanked them; others said the mistake had already caused confusion. Mei accepted that a correction could not erase the original effect. The team also changed its routine. Future headlines about policy would be checked against the source notes by a second editor before publication.

A month later, another report mentioned a proposal rather than an approved decision. This time the headline preserved that distinction. The newspaper had not become incapable of making mistakes, but it had made both its correction process and its checking process more visible.
''', [
Q('detail', 'What was the interview actually about?', 'A digital noticeboard trial in one corridor.', ['An approved school-wide paper ban.', 'A plan to close every school club.', 'A newspaper printing competition.'], '第一段说明采访只涉及一条走廊的试点。', 'a trial digital noticeboard in one corridor'),
Q('detail', 'How does Mei correct the report?', 'She adds a visible explanation and a fuller source description.', ['She removes all reader comments without explanation.', 'She blames the clubs for misunderstanding.', 'She keeps the headline and deletes the interview notes.'], '第二段记载改标题、公开解释错误并链接更完整说明。', 'added a note explaining the error'),
Q('inference', 'Why does Mei accept the critical comments?', 'The correction cannot undo all earlier confusion.', ['She knows the original headline was accurate.', 'She wants to stop checking sources.', 'She has already closed the newspaper.'], '第三段明确更正无法抹去原错误产生的影响。', 'a correction could not erase the original effect'),
Q('detail', 'What checking rule does the team introduce?', 'A second editor compares policy headlines with source notes.', ['Every headline must mention computers.', 'Only readers may see interview notes.', 'Policy reports will no longer be published.'], '新流程要求第二名编辑核对政策标题与原始笔记。', 'checked against the source notes by a second editor'),
Q('main_idea', 'What is the passage mainly about?', 'Taking responsibility for errors and improving verification.', ['Replacing every printed notice with technology.', 'Proving newspapers never need criticism.', 'Choosing exciting headlines over accurate reporting.'], '全文围绕公开纠错及改进发布前核查展开。', 'both its correction process and its checking process more visible')]),

R('g9a', 'access-walk', 'Testing the Route Together', '一起检验路线', '''
A youth committee designed a route from the bus stop to a new community reading room. On the map, the shortest path looked obvious: cross the square and use the side entrance. Committee members initially called it the easiest route.

Before printing the map, they invited residents with different needs to test it. A wheelchair user found that a low step blocked the side entrance. A parent pushing a buggy found the turn beside a planter too narrow. An older resident said the small arrows were difficult to see. These were not problems the committee had noticed during its first walk.

The building manager confirmed that the main entrance had a step-free approach. The group revised the route, which added about two minutes, and used larger, higher-contrast arrows. They also described the surface and entrances in words, so visitors could decide which route suited them.

The revised map did not claim to remove every possible barrier. A contact number invited reports of new difficulties. The committee had learned that calling a route easy required more than timing a walk by people whose needs were alike.
''', [
Q('detail', 'What does the committee first use to judge the route?', 'It appears to be the shortest path.', ['It has been tested by every resident.', 'It passes the largest shop.', 'It avoids all outdoor areas.'], '首段说明最初主要依据地图上路线最短。', 'the shortest path looked obvious'),
Q('detail', 'What blocks the wheelchair user?', 'A low step at the side entrance.', ['A missing bus ticket.', 'A closed reading room inside.', 'A long queue at the main entrance.'], '第二段明确侧门有低台阶阻碍通行。', 'a low step blocked the side entrance'),
Q('detail', 'How much extra time does the revised route take?', 'About two minutes.', ['About twenty minutes.', 'Half an hour.', 'No time at all.'], '第三段明确改道约多两分钟。', 'added about two minutes'),
Q('reason', 'Why does the map include descriptions in words?', 'To help visitors judge whether a route fits their needs.', ['To hide the location of the main entrance.', 'To replace the contact number with advertising.', 'To discourage visitors from giving feedback.'], '文字说明路面与入口，让使用者自行判断适合的路线。', 'so visitors could decide which route suited them'),
Q('main_idea', 'What lesson does the committee learn?', 'Different users should help evaluate an apparently simple design.', ['The shortest route is always easiest for everyone.', 'One test guarantees that no barriers remain.', 'Maps are useful only for committee members.'], '结尾反思了只由需求相近的人试走的局限。', 'people whose needs were alike')]),

R('g9a', 'survey-denominator', 'What the Percentage Hid', '百分比背后', '''
At a student council meeting, two clubs reported their survey results. The film club said that eighty percent of its respondents wanted a Friday screening. The science club reported sixty percent support for a Saturday workshop. Some members immediately argued that the film event had wider support.

Jules asked to see the numbers behind the percentages. The film club had received twenty responses, so sixteen people supported its proposal. The science club had received forty responses, of whom twenty-four supported the workshop. Neither survey included every student, and each club had mainly asked its own members.

This did not prove that the science event was more popular across the school. It showed that the percentages answered a narrower question: what share of each responding group supported that group's proposal? The groups differed in size and were not selected to represent the whole school.

The council decided to send both proposals through the same school-wide channel, with one response allowed per student and a clear closing date. Jules still used percentages in the final report, but placed the counts beside them. A neat number, he had learned, needs enough context to be understood.
''', [
Q('detail', 'How many film-club respondents support the screening?', 'Sixteen.', ['Twenty.', 'Twenty-four.', 'Forty.'], '二十人的百分之八十为十六人，第二段也明确给出。', 'sixteen people supported its proposal'),
Q('detail', 'How many science-club respondents support the workshop?', 'Twenty-four.', ['Twelve.', 'Sixteen.', 'Sixty.'], '第二段明确四十名答卷者中二十四人支持工作坊。', 'twenty-four supported the workshop'),
Q('inference', 'Why can’t the first two percentages establish school-wide popularity?', 'The responding groups differ and mainly contain club members.', ['Percentages can never be calculated correctly.', 'Both surveys include every student.', 'The two activities have identical schedules.'], '样本规模与成员构成不同，不能直接推断全校偏好。', 'each club had mainly asked its own members'),
Q('detail', 'What does the council change in the new survey?', 'It presents both proposals through the same school-wide channel.', ['It allows unlimited responses from film-club members.', 'It removes the closing date.', 'It asks only the smaller club.'], '新调查统一渠道、面向全校，并限定每人一份。', 'through the same school-wide channel'),
Q('main_idea', 'What is the central message?', 'Percentages need counts and sampling context.', ['The higher percentage always means more people.', 'Club members should never answer surveys.', 'A larger sample proves every proposal is popular.'], '结尾强调把人数放在百分比旁边并提供必要背景。', 'placed the counts beside them')]),

R('g9b', 'recommendation-window', 'Opening the Recommendation Window', '打开推荐窗口', '''
At the start of a reading project, Hana used an app that suggested articles similar to ones she had opened before. After she read several pieces about mountain travel, the suggestions filled with hiking routes and equipment reviews. She enjoyed them, but began to assume that this was what most people were discussing.

During a class exchange, other students brought articles about local theatre, public transport and food traditions. Hana realised that her personal list was a window shaped partly by her own earlier choices. It was not a neutral picture of everything being published.

She did not delete the app. Instead, she kept its recommendations for quick browsing and added a weekly visit to a general library page. She also chose one topic suggested by a classmate and read beyond the first headline. In her project diary, she recorded where each article had come from.

The new habit sometimes led to articles she did not enjoy, and it took more effort than simply following the next suggestion. Yet it helped her separate "often shown to me" from "important to everyone." Useful recommendations could remain useful without becoming her only way of discovering ideas.
''', [
Q('detail', 'Why does Hana receive many hiking-related suggestions?', 'She has opened several mountain-travel articles.', ['Every publisher has stopped writing about other topics.', 'Her teacher has banned library pages.', 'The app has surveyed every person at school.'], '首段明确推荐与她此前打开的文章相似。', 'similar to ones she had opened before'),
Q('inference', 'What does Hana realise about her personal list?', 'It is influenced by her own previous choices.', ['It measures what everyone considers important.', 'It contains every article published that week.', 'It has no connection with her reading history.'], '第二段指出推荐窗口部分由个人早先的选择塑造。', 'shaped partly by her own earlier choices'),
Q('detail', 'Which new habit does Hana adopt?', 'Visiting a general library page each week.', ['Reading only equipment reviews.', 'Deleting every article from classmates.', 'Ignoring where articles come from.'], '第三段给出每周访问综合图书馆页面的新习惯。', 'a weekly visit to a general library page'),
Q('detail', 'What is one cost of Hana’s new approach?', 'It requires more effort.', ['It prevents all enjoyable reading.', 'It removes her access to the app.', 'It forces classmates to change their interests.'], '第四段明确主动探索比顺着推荐更费力。', 'it took more effort'),
Q('main_idea', 'What does the passage encourage?', 'Using recommendations while also exploring other sources.', ['Treating a personal feed as a complete view of the world.', 'Rejecting every digital reading tool.', 'Choosing all reading by popularity alone.'], '结尾认可推荐的作用，同时强调不要让它成为唯一入口。', 'without becoming her only way of discovering ideas')]),

R('g9b', 'courtyard-pilot', 'A Trial in the Courtyard', '庭院里的小试验', '''
Students at Brook School wanted more comfortable places to read outdoors. The facilities team agreed to test a movable shade above one courtyard bench for two weeks. A student group planned to compare how often that bench was used before and during the trial.

Their first chart looked impressive: afternoon use rose from six recorded visits in the earlier week to eighteen in the trial week. However, Mara noticed that the earlier week had included three rainy afternoons, while the trial week was dry. A reading club had also begun meeting in the courtyard. These differences made a simple before-and-after claim less convincing.

The group revised its report. They described the increase but did not attribute all of it to the shade. They also asked users about comfort, noted weather conditions and observed another bench nearby. The evidence still had limits: two benches could differ in location, and a short trial could not represent every season.

The team recommended keeping the movable shade for further testing rather than immediately ordering one for every outdoor space. Their report sounded less dramatic than the first chart, but it gave the school a clearer basis for its next decision.
''', [
Q('detail', 'What is being tested?', 'A movable shade above a bench.', ['A new indoor examination hall.', 'A ban on outdoor reading.', 'A permanent roof over the whole school.'], '第一段说明试用的是长椅上方可移动遮阳设施。', 'a movable shade above one courtyard bench'),
Q('detail', 'How many visits are recorded in the trial week?', 'Eighteen.', ['Six.', 'Twelve.', 'Thirty-six.'], '第二段给出试用周十八次使用记录。', 'eighteen in the trial week'),
Q('inference', 'What weakens a simple claim that the shade caused the entire increase?', 'Weather and club activities changed at the same time.', ['Both weeks have identical conditions.', 'The bench has never been observed.', 'No students use the courtyard during the trial.'], '雨天数量和社团活动同时变化，存在其他解释。', 'A reading club had also begun meeting in the courtyard'),
Q('detail', 'What limitation remains after adding another bench for comparison?', 'The benches may differ in location.', ['The school has no record of weather.', 'Students cannot describe comfort.', 'The trial lasts for every season.'], '第三段指出两条长椅位置可能不同。', 'two benches could differ in location'),
Q('main_idea', 'Why is the revised report more useful?', 'It separates observations from stronger claims and supports further testing.', ['It hides all inconvenient information.', 'It proves every courtyard needs the same shade.', 'It replaces evidence with a more exciting chart.'], '报告呈现变化、说明限制，并建议继续试验以支持决策。', 'a clearer basis for its next decision')]),

R('g9b', 'cup-return', 'The Missing Half of Reuse', '重复使用的另一半', '''
A youth centre introduced reusable cups for its weekend events. Organisers expected the change to reduce the number of disposable cups bought each month. In the first three weekends, however, many reusable cups disappeared from the return area, and volunteers repeatedly had to search meeting rooms for them.

Instead of assuming visitors did not care, the team followed the cups' journey. The drinks table was beside the entrance, but the only return box stood behind a door near the kitchen. Some guests thought they should leave empty cups on tables for staff. Others could not find the box before their bus arrived.

The centre placed clearly marked return boxes beside both exits and explained the return process when serving drinks. It also arranged enough time and staff for washing between sessions. A reusable item was useful only if the system made repeated use practical.

By the following month, more cups were being returned and fewer replacements were needed. The team continued tracking losses and cleaning needs rather than claiming the change had no costs. The project taught them to design the return journey with as much care as the moment when an item was first handed out.
''', [
Q('detail', 'What problem appears during the first three weekends?', 'Many cups are not reaching the return area.', ['All visitors refuse to drink.', 'Disposable cups become free.', 'The centre stops holding events.'], '首段写可重复使用杯未回到归还区，志愿者需四处寻找。', 'many reusable cups disappeared from the return area'),
Q('detail', 'Where is the original return box?', 'Behind a door near the kitchen.', ['Beside both exits.', 'At the bus station.', 'On every meeting-room table.'], '第二段明确原归还箱在厨房附近门后。', 'behind a door near the kitchen'),
Q('inference', 'Why do organisers follow the cups’ journey?', 'To understand practical barriers before blaming visitors.', ['To prove every guest intentionally takes cups.', 'To avoid planning any cleaning.', 'To remove all return boxes permanently.'], '第二段先提出不急于归咎访客，再调查实际使用路径。', 'Instead of assuming visitors did not care'),
Q('detail', 'What does the team arrange in addition to better return boxes?', 'Time and staff for washing cups.', ['Free buses for all visitors.', 'New cups for every drink served.', 'A ban on discussing the project’s costs.'], '第三段说明还需安排清洗所需时间与人员。', 'enough time and staff for washing'),
Q('main_idea', 'What is the main lesson?', 'Reuse needs a workable return and cleaning system.', ['Labelling an item reusable solves every problem.', 'Visitor behaviour can never be influenced by design.', 'Counting losses is unnecessary after a good month.'], '全文强调重复使用不仅是发出杯子，还需归还和清洗流程。', 'design the return journey with as much care')]),
]
