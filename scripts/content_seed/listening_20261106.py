"""AI-authored single-speaker announcements; playback uses the device's English TTS."""
from .common import listening as L, question as Q

LISTENING = [
L(7, 'chess-room', 'Chess Club Room Change', '国际象棋社换教室', '''
Hello, chess club members. Our Tuesday meeting will be in Room Fourteen this week. Room Ten is being painted. Please come at four fifteen, after your last lesson. You do not need to bring a chess set; there are six sets in the new room. Bring your notebook, because we will write down one new move. The meeting finishes at five. New members can watch a game before joining in.
''', '国际象棋社的同学们好。本周二的活动改在十四号教室，十号教室正在粉刷。请在最后一节课后四点十五分到。新教室有六副棋，不用自带。请带笔记本，我们要记下一种新走法。活动五点结束。新成员可以先看一局再参加。', [
Q('detail', 'Where will the club meet this week?', 'Room Fourteen.', ['Room Ten.', 'The painting room.', 'The school library.'], '活动在十四号教室，十号教室正在粉刷。', 'in Room Fourteen this week'),
Q('detail', 'What should members bring?', 'A notebook.', ['A chess set.', 'A paintbrush.', 'A lunch box.'], '通知明确要求带笔记本，不必带棋具。', 'Bring your notebook'),
Q('detail', 'When will the meeting end?', 'At five.', ['At four.', 'At four fifteen.', 'At six.'], '结束时间为五点，四点十五分是开始时间。', 'finishes at five')]),

L(7, 'bakery-order', 'Collecting the Class Bread', '领取班级面包', '''
Good afternoon. This is a message for Class Seven B. Your bread order is ready at the bakery opposite the park. There are twelve rolls in a large paper bag. The shop closes at six today, so please collect the bag before five thirty. Ask for the order under the name of Miss Lin. You have already paid. If it is raining, bring a box to keep the paper bag dry.
''', '下午好，这是给七年级B班的留言。你们订的面包已经做好，在公园对面的面包店。大纸袋里有十二个小面包。今天店铺六点关门，请在五点半前领取。领取时报林老师的名字。费用已经付过。如果下雨，请带一个盒子保护纸袋不被淋湿。', [
Q('detail', 'Where is the bakery?', 'Opposite the park.', ['Behind the school library.', 'Inside the bus station.', 'Next to Miss Lin’s home.'], '店铺位于公园对面。', 'opposite the park'),
Q('detail', 'When should the bread be collected?', 'Before five thirty.', ['After six.', 'At six thirty.', 'Before three.'], '通知要求五点半前领，六点为关店时间。', 'before five thirty'),
Q('reason', 'Why might students need a box?', 'To keep the paper bag dry.', ['To pay for the bread.', 'To carry twelve drinks.', 'To return a broken roll.'], '下雨时带盒子是为了保护纸袋。', 'keep the paper bag dry')]),

L(7, 'plant-label', 'Labels for the Class Plants', '给班级植物做标签', '''
Tomorrow we will make labels for the plants outside our classroom. Each pair of students needs one pencil and two clean wooden sticks. Please do not bring paint. We will use the classroom pens to write the plant names. Meet at the back door at nine forty, not at the main gate. If the ground is wet, we will write the labels indoors and put them outside on Friday instead.
''', '明天我们为教室外的植物制作标签。每两位同学准备一支铅笔和两根干净木条，不要带颜料。我们用教室里的笔写植物名称。九点四十分在后门集合，不是在大门口。如果地面湿，我们先在室内写标签，周五再把它们放到外面。', [
Q('detail', 'How many wooden sticks does each pair need?', 'Two.', ['One.', 'Three.', 'Four.'], '每组两人要准备两根木条。', 'two clean wooden sticks'),
Q('detail', 'Where should students meet?', 'At the back door.', ['At the main gate.', 'At the bus stop.', 'Inside the art shop.'], '通知特意区分后门与大门口。', 'at the back door'),
Q('detail', 'What will happen if the ground is wet?', 'Students will write the labels indoors first.', ['The plants will be thrown away.', 'The class will buy paint.', 'Everyone will meet at the main gate.'], '地面湿时先在室内制作，周五再放出去。', 'write the labels indoors')]),

L(7, 'lost-scarf', 'A Scarf at the Sports Hall', '体育馆的围巾', '''
A blue scarf was found beside the sports hall this morning. It has a small white star at one end. It is not the green scarf that was found yesterday. If the blue scarf is yours, please go to the office after lunch. Tell Mrs King what is written on the small label inside. We will keep it in the office until Friday, and then put it in the school lost-property cupboard.
''', '今天早上有人在体育馆旁捡到一条蓝围巾，一端有一颗白色小星星。它不是昨天捡到的绿色围巾。如果蓝围巾是你的，请午餐后去办公室，告诉金老师里面小标签上写着什么。围巾在办公室保管到周五，之后放入学校失物招领柜。', [
Q('detail', 'What colour is the scarf found today?', 'Blue.', ['Green.', 'White.', 'Black.'], '今天捡到的是蓝围巾，绿色是昨天的。', 'A blue scarf was found'),
Q('detail', 'When should the owner visit the office?', 'After lunch.', ['Before breakfast.', 'During the first lesson.', 'After the office closes.'], '失主应午饭后到办公室。', 'go to the office after lunch'),
Q('detail', 'What information should the owner provide?', 'The writing on the inside label.', ['The price of the sports hall.', 'The name of yesterday’s finder.', 'The colour of Mrs King’s coat.'], '通过说出内侧标签文字确认归属。', 'what is written on the small label inside')]),

L(7, 'bus-exhibit', 'The Bus for the Art Visit', '参观画展的班车', '''
Here is the plan for Thursday's art visit. The bus leaves school at eight fifty. Please be at the side gate ten minutes earlier. Take water and a small lunch, but leave large schoolbags in your classroom. At the gallery, we will draw one picture in our notebooks. The gift shop is closed that day, so you will not need spending money. We expect to return to school at one thirty.
''', '这是周四参观画展的安排。班车八点五十分从学校出发，请提前十分钟到侧门。带水和简单午餐，大书包留在教室。在美术馆里，我们在笔记本上画一幅画。当天礼品店不营业，无需带购物钱。预计下午一点半回到学校。', [
Q('detail', 'When should students be at the side gate?', 'At eight forty.', ['At eight fifty.', 'At nine.', 'At one thirty.'], '八点五十分出发，提前十分钟集合，即八点四十分。', 'ten minutes earlier'),
Q('detail', 'What should students leave in the classroom?', 'Large schoolbags.', ['Their notebooks.', 'Their water.', 'Their lunch.'], '大书包应留在教室，水和午餐要带。', 'leave large schoolbags in your classroom'),
Q('reason', 'Why is spending money unnecessary?', 'The gift shop will be closed.', ['The gallery will give away all its gifts.', 'Students will return before breakfast.', 'The bus driver will buy every student a gift.'], '不用购物钱是因为礼品店当天关门。', 'The gift shop is closed that day')]),

L(7, 'kitchen-helper', 'Helping in the Cooking Club', '烹饪社的小帮手', '''
Welcome to the cooking club. Today we are making fruit cups, not a hot meal. Wash your hands before touching the fruit. An adult will cut the apples, and students will divide the pieces into cups. Each cup needs three apple pieces and two grapes. If you cannot eat either fruit, tell the teacher before we start. Please keep the table clear so everyone has enough space to work safely.
''', '欢迎参加烹饪社。今天我们做水果杯，不做热食。接触水果前请洗手。苹果由成年人切，同学们负责把果块分装入杯。每杯放三块苹果和两颗葡萄。不能吃其中任何一种水果的同学，请在开始前告诉老师。保持桌面整洁，给大家留出安全操作的空间。', [
Q('detail', 'What is the club making?', 'Fruit cups.', ['Hot soup.', 'Apple pies.', 'Grape juice.'], '今天制作的是水果杯。', 'making fruit cups'),
Q('detail', 'Who will cut the apples?', 'An adult.', ['Every new student.', 'Only the youngest pupil.', 'A visiting athlete.'], '切苹果由成年人负责。', 'An adult will cut the apples'),
Q('detail', 'What goes into each cup?', 'Three apple pieces and two grapes.', ['Two apple pieces and three grapes.', 'Three apples and two cups.', 'One grape and no apple.'], '分装标准是三块苹果加两颗葡萄。', 'three apple pieces and two grapes')]),

L(7, 'library-return', 'Returning Library Books', '归还图书的提醒', '''
Our library desk will be closed during Friday's school meeting. If you need to return a book, use the return box outside the library door before three o'clock. Do not put wet or damaged books in the box; bring those to Mrs Bell on Monday so she can check them. The return box is for books only. Please keep any bookmarks or notes you want before you return your book.
''', '周五学校开会期间，图书馆服务台关闭。需要还书的同学，请在三点前使用图书馆门外的还书箱。受潮或损坏的书不要投入箱内，应周一交给贝尔老师检查。还书箱只收图书。还书前，请取出你想保留的书签和笔记。', [
Q('reason', 'Why will the library desk be closed on Friday?', 'There is a school meeting.', ['All books have been sold.', 'Mrs Bell is painting the door.', 'The return box is broken.'], '服务台关闭是因为学校会议。', "during Friday's school meeting"),
Q('detail', 'Where is the return box?', 'Outside the library door.', ['Inside Mrs Bell’s car.', 'Beside the sports field.', 'Under the classroom clock.'], '还书箱位于图书馆门外。', 'outside the library door'),
Q('detail', 'What should students do with a damaged book?', 'Bring it to Mrs Bell on Monday.', ['Hide it in the return box.', 'Leave it in the rain.', 'Give it to another student to keep.'], '损坏图书应周一交给老师检查。', 'bring those to Mrs Bell on Monday')]),

L(7, 'team-badges', 'Badges for Sports Day', '运动会队徽', '''
Sports day helpers, please collect your team badges from Room Six after school today. The red badges are for the running team, and the yellow ones are for the jumping team. Each helper should take only one badge. If you are helping with both events, take a yellow badge and write your name on the running list. Return all badges next Monday. We will use them again at the summer games.
''', '运动会志愿者请今天放学后到六号教室领取队徽。跑步组用红色，跳跃组用黄色。每人只取一个。两个项目都帮忙的同学领取黄色队徽，并在跑步名单上登记。下周一归还全部队徽，夏季运动会还会再次使用。', [
Q('detail', 'Where can helpers collect badges?', 'Room Six.', ['The main gate.', 'The school kitchen.', 'Room Sixteen.'], '领取地点为六号教室。', 'from Room Six'),
Q('detail', 'Which badge should a helper for both events take?', 'A yellow badge.', ['A red and a yellow badge.', 'Only a blue badge.', 'No badge at all.'], '两项都参加者只拿黄色队徽，并另行登记跑步名单。', 'take a yellow badge'),
Q('reason', 'Why must the badges be returned?', 'They will be used again.', ['They are made of gold.', 'The room will close forever.', 'Helpers must buy new colours daily.'], '归还是为了夏季运动会再次使用。', 'use them again at the summer games')]),

L(8, 'walk-feedback', 'Feedback on the Walking Tour', '步行导览反馈', '''
Thank you for trying our student-led walking tour. We would like feedback about the route, not about how loudly the guide spoke. Please use the short form near the exit. Question one asks which sign was hardest to follow. Question two asks where you wanted a rest. You can leave your name blank. We will compare the forms before changing next month's route. If you noticed an urgent safety problem, tell a staff member now rather than waiting for the form to be read.
''', '感谢试走学生导览路线。本次希望收集路线反馈，而非导游音量评价。请填写出口附近的短表。第一题问哪个标志最难看懂，第二题问在哪里想休息。姓名可以不填。我们会比较反馈后再调整下月路线。若发现紧急安全问题，请现在告知工作人员，不要等表格被阅读后才处理。', [
Q('detail', 'What is the feedback mainly about?', 'The walking route.', ['The guide’s speaking volume.', 'The price of uniforms.', 'The school lunch menu.'], '开头明确本次收集路线方面的反馈。', 'feedback about the route'),
Q('detail', 'Which information is optional on the form?', 'The visitor’s name.', ['The location of the exit.', 'The date of next month’s tour.', 'The staff member’s age.'], '通知允许姓名留空。', 'leave your name blank'),
Q('detail', 'What should visitors do about an urgent safety problem?', 'Tell a staff member immediately.', ['Wait until next month.', 'Write only their name.', 'Change the route on their own.'], '紧急问题应立即告知工作人员。', 'tell a staff member now')]),

L(8, 'recording-plan', 'Recording the School Story', '录制校园故事', '''
The history club will record a short school story on Wednesday. We originally booked the music room, but a rehearsal is now taking place there. We will use the small meeting room beside the office instead. Please arrive at three forty-five for a sound check; recording begins at four. Bring the final script with your latest corrections. If your story names a real person, check that they have agreed to be included. We can remove names without changing the main story. Do not upload the recording until the club teacher has checked the finished version.
''', '历史社周三录制校园短故事。原订音乐教室当天有排练，改用办公室旁的小会议室。请三点四十五分到场试音，四点开始录制。带上最新修改后的定稿。涉及真人姓名时应先征得同意，也可删去姓名而保留故事主线。成品经社团老师检查后再上传。', [
Q('reason', 'Why has the recording room changed?', 'The music room is needed for a rehearsal.', ['The script has been lost.', 'The office has no chairs.', 'The teacher has cancelled the project.'], '音乐教室有排练，所以改用小会议室。', 'a rehearsal is now taking place there'),
Q('detail', 'What happens at three forty-five?', 'A sound check.', ['The public upload.', 'The end of recording.', 'A history examination.'], '三点四十五分试音，四点才正式录音。', 'three forty-five for a sound check'),
Q('detail', 'What must happen before the recording is uploaded?', 'The club teacher must check it.', ['Every real person’s name must be added.', 'The script must be thrown away.', 'The music room must be repainted.'], '通知明确成品经老师检查才能上传。', 'the club teacher has checked the finished version')]),

L(8, 'swap-tokens', 'How the Puzzle Swap Works', '拼图交换规则', '''
At Saturday's puzzle swap, volunteers will check that each puzzle has all its pieces before giving out a swap token. A token can be exchanged for one checked puzzle, regardless of the size of the box. Please do not bring puzzles with missing pieces to the swap table. We will have a separate craft box for those, but they will not earn a token. The event runs from ten until twelve. To give volunteers enough checking time, the last puzzle will be accepted at eleven thirty. You may attend simply to look around without bringing anything.
''', '周六拼图交换活动中，志愿者确认拼图片数完整后才发交换券。每张券可换一份检查过的拼图，不按盒子大小区分。缺片拼图放在另设的手工材料箱，不换券。活动十点到十二点，最后收件时间十一点半，以便检查。也欢迎不带物品、只来参观的人。', [
Q('detail', 'When is a swap token given?', 'After a puzzle is checked for completeness.', ['Whenever someone brings any empty box.', 'Only after the event closes.', 'Before volunteers see the puzzle.'], '确认完整后发券。', 'has all its pieces before giving out a swap token'),
Q('detail', 'What happens to puzzles with missing pieces?', 'They can go into a craft box without earning a token.', ['They earn two tokens.', 'They are sold as complete puzzles.', 'They must be put on the main swap table.'], '缺片拼图有单独材料箱，但不能获得交换券。', 'they will not earn a token'),
Q('detail', 'What is the last time for bringing in a puzzle?', 'Eleven thirty.', ['Ten.', 'Twelve.', 'Twelve thirty.'], '十一点半停止收件，十二点为活动结束时间。', 'accepted at eleven thirty')]),

L(8, 'weather-notes', 'Notes for the Outdoor Lesson', '户外课观察记录', '''
For tomorrow's outdoor lesson, each group will compare two places in the playground. Choose one open area and one area beside a wall. Record the time and describe how the air feels at each place. We are practising observation, so do not invent temperatures if you do not have a thermometer. Use the same words on both parts of your record sheet where possible. If rain becomes heavy, return to the classroom with your teacher. We can discuss what we have already observed instead of finishing every box outside.
''', '明天户外课每组比较操场两处：一处开阔地和一处墙边。记录时间，描述两处空气给人的感受。这是观察练习，没有温度计就不要编造温度。尽量使用一致的词描述两处。若下大雨，应随老师回教室，可以讨论已有观察，无须坚持在外面填满所有格子。', [
Q('detail', 'Which two places should each group compare?', 'An open area and an area beside a wall.', ['Two rooms inside the library.', 'A bus stop and a shop.', 'Two places in another town.'], '通知指定开阔区域与墙边区域。', 'one open area and one area beside a wall'),
Q('detail', 'What should students avoid without a thermometer?', 'Inventing temperature numbers.', ['Writing down the time.', 'Describing their observations.', 'Using the record sheet.'], '没有仪器不能虚构精确温度。', 'do not invent temperatures'),
Q('detail', 'What should students do if rain becomes heavy?', 'Return with the teacher and discuss existing observations.', ['Stay outside until every box is filled.', 'Go home without telling anyone.', 'Change every observation to a guessed number.'], '大雨时返回教室，再讨论已经观察到的情况。', 'return to the classroom with your teacher')]),

L(8, 'drama-signup', 'Signing Up for the Drama Project', '戏剧项目报名', '''
Our drama project needs people for acting, sound and making props. You do not have to act to join. Write your first and second choices on the form outside the hall by Thursday afternoon. We cannot promise everyone their first choice, but we will speak with you before assigning a different role. The first meeting is next Monday at four, and attendance at that meeting is required. Later practice times will depend on your role. If you are interested but cannot attend on Monday, contact Mr Cole before the form closes rather than signing up without explaining.
''', '戏剧项目招募表演、音效和道具人员，不表演也能加入。周四下午前在礼堂外表格填写第一和第二志愿。无法保证都分到第一志愿，但调整前会沟通。首次会议为下周一四点，必须参加；后续时间按分工安排。感兴趣却无法周一参加的人，应在报名截止前联系科尔老师说明。', [
Q('detail', 'Which statement about joining is correct?', 'Students can help without acting.', ['Every member must play a lead role.', 'Only students making props may apply.', 'First choices are guaranteed.'], '项目同时需要音效和道具人员，明确不必上台表演。', 'You do not have to act to join'),
Q('detail', 'When is the form due?', 'Thursday afternoon.', ['Monday morning.', 'Friday evening.', 'Next month.'], '填写截止时间为周四下午。', 'by Thursday afternoon'),
Q('detail', 'What should an interested student do if Monday attendance is impossible?', 'Contact Mr Cole before the deadline.', ['Sign up and say nothing.', 'Ask to remove all meetings.', 'Write someone else’s name.'], '不能参加首次会议者需要提前联系说明。', 'contact Mr Cole before the form closes')]),

L(8, 'family-audio', 'Preparing a Family Audio Story', '准备家庭音频故事', '''
For our English project, you may record a family member describing a favourite place. The story should be between one and two minutes long. Ask permission before recording, and explain that only your class and teacher will hear the file. You may use a made-up name if the speaker prefers. After recording, listen once to check that the speech is clear. Please do not add background music; it can make quiet words harder to hear. Upload the file by Sunday evening. If recording at home is difficult, book a school room with me on Friday.
''', '英语项目可以录一位家人讲述喜欢的地方，时长一到两分钟。录制前应征得同意，并说明只有本班和老师收听。受访者愿意时可用化名。录后听一遍检查清晰度，不要加背景音乐，以免遮住轻声话语。周日晚上前上传；家中不方便录音的，可周五找老师预约学校房间。', [
Q('detail', 'How long should the story be?', 'Between one and two minutes.', ['Less than ten seconds.', 'Exactly five minutes.', 'At least half an hour.'], '录制要求是一至两分钟。', 'between one and two minutes long'),
Q('reason', 'Why should background music be avoided?', 'It may make quiet words harder to hear.', ['It always increases file permissions.', 'The speaker must sing instead.', 'The story must be silent.'], '音乐可能遮住声音较小的词语。', 'make quiet words harder to hear'),
Q('detail', 'What help is available if home recording is difficult?', 'Booking a school room through the teacher.', ['Replacing the file with a shop advertisement.', 'Skipping permission from the speaker.', 'Uploading an unrelated song.'], '老师提供预约学校房间的替代方案。', 'book a school room with me on Friday')]),

L(9, 'trial-feedback', 'Interpreting the Trial Results', '理解试点结果', '''
Our homework-planner trial has finished its first week. Eighteen of the thirty students who returned a feedback form said the planner helped them remember deadlines. That is encouraging, but it does not tell us what all students think. Some students tried the planner without returning a form, and others did not use it at all. In next week's report, please give the number of responses as well as the percentage. We will invite feedback from non-users too, especially about reasons they did not try it. The aim is to improve the tool, not to make the first results sound more impressive than they are.
''', '作业计划工具第一周试点结束。交反馈表的三十名学生中，十八名认为工具帮助记住截止时间。这是积极信号，但不能代表所有学生：有人使用却没交表，也有人未使用。下周报告请同时写答卷人数和百分比，并向未使用者了解原因。目标是改进工具，而非夸大首轮结果。', [
Q('detail', 'How many respondents say the planner helped?', 'Eighteen.', ['Thirty.', 'Twelve.', 'Sixty.'], '三十份反馈中十八份认为有帮助。', 'Eighteen of the thirty students'),
Q('inference', 'Why can’t the feedback represent every student’s opinion?', 'It covers only those who returned forms.', ['Every student has already answered.', 'The planner has no deadlines.', 'All non-users dislike the tool.'], '样本只包括交表者，既有未交表使用者，也有未使用者。', 'tried the planner without returning a form'),
Q('detail', 'What will the team ask non-users about?', 'Reasons they did not try the planner.', ['Which respondent should win a prize.', 'How to hide response numbers.', 'Whether deadlines should be removed from all subjects.'], '通知明确要了解未尝试的原因。', 'reasons they did not try it')]),

L(9, 'archive-caption', 'Checking a Photo Caption', '核对照片说明', '''
Before we publish the archive photo, we need to correct its caption. The original note says the picture shows the opening of the town pool in nineteen eighty-four. However, the banner in the photograph names a school sports event, and a second record gives a different date. We have confirmed the location, but not the year. For now, describe it as a school event at the town pool, with the date still being checked. Please keep the old note in our records rather than deleting it. It helps explain how the earlier description was formed and why we have changed it.
''', '发布档案照片前需要修订说明。原注称其为一九八四年镇泳池开幕，但照片横幅写的是学校体育活动，另一份记录日期也不同。目前确认了地点，尚未确认年份。暂写为镇泳池的学校活动，日期待核查。旧说明保留在档案中，便于解释原来的说法如何形成以及为什么修改。', [
Q('detail', 'Which information has been confirmed?', 'The location.', ['The exact year.', 'Every person’s name.', 'The opening ceremony claim.'], '原文明确地点已确认，年份未确认。', 'confirmed the location, but not the year'),
Q('detail', 'How should the event be described for now?', 'A school event at the town pool, with the date being checked.', ['The pool’s opening in a confirmed year.', 'A concert in another town.', 'An event with no possible connection to a school.'], '临时说明需保留已知活动性质与地点，并说明日期未定。', 'with the date still being checked'),
Q('reason', 'Why should the old note be kept?', 'To document the earlier description and the reason for changing it.', ['To prove its date must be right.', 'To replace the photograph entirely.', 'To prevent any future correction.'], '旧记录有助追溯说法形成及更正原因。', 'why we have changed it')]),

L(9, 'transport-choice', 'Choosing a Shuttle Timetable', '选择接驳车时刻', '''
The community centre is considering two shuttle timetables. The first would bring visitors just before the morning classes. The second would arrive twenty minutes earlier, leaving more waiting time but allowing passengers to visit the reception desk before lessons. We are not asking which timetable has the shorter journey; the journey itself is the same. Please choose the arrival time that better suits your needs and explain why. People who do not currently use the shuttle may also respond. Their answers may reveal barriers that a survey of existing passengers alone would miss. No timetable change will take effect until a final notice is issued.
''', '社区中心正在比较两种接驳车时刻：一种在上午课程开始前不久到达，另一种提前二十分钟到，等待更久，但可先去服务台。两种方案车程相同，调查不是比较行车时间。请选更适合的到达时刻并说明原因。未使用接驳车者也可答，以了解现有乘客调查可能遗漏的障碍。最终通知发出前不会改时刻。', [
Q('detail', 'What differs between the two proposals?', 'The arrival time.', ['The length of the journey.', 'The destination town.', 'The type of lesson taught.'], '两方案差别是到达时刻，车程相同。', 'choose the arrival time'),
Q('reason', 'Why are non-users invited to respond?', 'They may identify barriers missed by current passengers.', ['They have already approved the new timetable.', 'They must replace every passenger.', 'They are the only people allowed to attend classes.'], '邀请未使用者是为了发现现有乘客调查可能遗漏的障碍。', 'barriers that a survey of existing passengers alone would miss'),
Q('detail', 'When will any change take effect?', 'After a final notice is issued.', ['Immediately after one response.', 'Before the survey begins.', 'Only when every lesson is cancelled.'], '通知明确最终公告前不变更时刻。', 'until a final notice is issued')]),

L(9, 'translation-review', 'Reviewing the Welcome Guide', '审核欢迎指南译文', '''
Our volunteer team has drafted an English welcome guide for the community centre. Before it is printed, please check the practical details against the current noticeboard. In particular, the weekend opening time changed last month, and the room names in the old leaflet are no longer reliable. Clear English is important, but an elegant sentence with the wrong time can still send a visitor to a closed door. Mark any detail you cannot confirm instead of guessing. A staff member will resolve those points. We will then ask two new visitors to follow the guide and tell us where they become confused.
''', '志愿者团队已写好社区中心英文欢迎指南。印刷前请对照当前公告板核实实用信息。周末开放时间上月调整过，旧传单的房间名称也不可靠。英语流畅很重要，但时间错了仍会让访客吃闭门羹。无法确认的信息先标记，不要猜，由工作人员核实。之后请两位新访客按指南试走，指出困惑。', [
Q('detail', 'What should reviewers use to check details?', 'The current noticeboard.', ['Only the old leaflet.', 'A guessed weekly timetable.', 'An unrelated travel guide.'], '要求对照当前公告板核实。', 'against the current noticeboard'),
Q('inference', 'What point does the closed-door example illustrate?', 'Accurate details matter as well as good language.', ['Longer sentences are always more helpful.', 'Visitors should avoid weekend activities.', 'Translation makes opening times unnecessary.'], '句子再优美，错误时间也会误导访客，强调语言与事实都要准确。', 'an elegant sentence with the wrong time'),
Q('detail', 'What should happen to an unconfirmed detail?', 'It should be marked for staff to resolve.', ['It should be replaced with a confident guess.', 'It should be printed without review.', 'It should be decided by a random visitor.'], '先标记不确定处，再由工作人员解决。', 'A staff member will resolve those points')]),

L(9, 'basket-pilot', 'Evaluating the Borrowing Baskets', '评估借用篮子', '''
Our market tested borrowing baskets for two weekends. Volunteers counted forty returns on the first weekend and fifty on the second. Those figures alone do not show that the return rate improved, because more baskets were also borrowed on the second weekend. Please compare returns with the number borrowed in each period. We should also keep records of baskets that were returned late, since a basket missing on Sunday may arrive on Monday. The next report will separate same-day returns from later returns. This will help us plan how many baskets must be available without treating every delayed return as a permanent loss.
''', '市场借篮试点进行了两个周末，回收数分别四十和五十。第二周末借出数量也更多，因此单凭回收数量不能断言归还率提高。应比较各期归还数与借出数，也要记录晚还的篮子：周日没回来的可能周一归还。下份报告区分当日归还与迟还，以便安排库存，避免把迟还全算作永久丢失。', [
Q('detail', 'How many baskets are returned on the second weekend?', 'Fifty.', ['Forty.', 'Ninety.', 'Twenty.'], '第二周末记录五十个归还。', 'fifty on the second'),
Q('inference', 'Why don’t the return counts alone prove a better return rate?', 'The numbers borrowed also differ.', ['Every basket was lost on Monday.', 'No baskets were borrowed the first weekend.', 'The market refuses all late returns.'], '借出总数不同，不能只比归还绝对数量来判断比例。', 'more baskets were also borrowed'),
Q('detail', 'What will the next report distinguish?', 'Same-day returns and later returns.', ['Wooden baskets and paper cups.', 'Volunteers and market sellers only.', 'Morning prices and evening prices.'], '报告会区分当日与较晚归还。', 'separate same-day returns from later returns')]),

L(9, 'discussion-notes', 'Writing Fair Discussion Notes', '记录公平的讨论纪要', '''
When writing notes from our student discussion, please separate decisions from suggestions. Several people supported opening the study room on Saturday, but the group has not yet agreed on staffing. It is therefore a proposal, not an approved arrangement. Include the main concern about supervision as well as the reasons in favour. You do not need to name each speaker; group the points by topic. Send the draft to participants for factual corrections before posting the final version. This review is intended to check whether their points were represented accurately, not to remove every opinion that someone disagrees with.
''', '记录学生讨论时，请区分决定与建议。有人支持周六开放自习室，但值班安排尚未达成共识，因此只是提议而非获准方案。应同时记下支持理由与管理监督方面的主要担忧。不必逐个点名，按议题组织。公布前把草稿发给参与者核对事实，目的是准确表达各方观点，而不是删去一切有分歧的意见。', [
Q('detail', 'Why is Saturday opening still only a proposal?', 'Staffing has not been agreed.', ['Nobody supports using the room.', 'The room no longer exists.', 'A final public notice has already approved it.'], '值班人员安排未谈妥，不能当作已批准。', 'has not yet agreed on staffing'),
Q('detail', 'How should the discussion points be organised?', 'By topic.', ['By each speaker’s age.', 'Only by the order of applause.', 'As a list of unrelated names.'], '通知要求按议题归类，不必逐个列发言者。', 'group the points by topic'),
Q('purpose', 'What is the purpose of reviewing the draft?', 'To check that participants’ points are accurately represented.', ['To delete all disagreement.', 'To approve staffing automatically.', 'To replace suggestions with invented decisions.'], '最后一句直接说明核对是为准确呈现观点。', 'whether their points were represented accurately')]),
]
