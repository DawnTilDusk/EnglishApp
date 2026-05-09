**背单词滚动学习队列状态机草案**

**一、目标**

- 让背单词支持连续学习，而不是每次进来都像新开一局。
- 将“当前同时学习几个词”和“这一轮总共学习几个词”解耦，降低瞬时负担，同时保持整轮推进效率。
- 让系统记住“学到词书哪里”“当前活跃队列里有哪些词”“每个词已通过哪些关卡”“这一轮是否可以进入拼写复习”。

**二、核心结论**

- 活跃学习队列固定维持 `4` 个单词，除非到达一轮尾声或词书剩余不足。
- 一轮总学习目标固定为 `10` 个新词，后续可以参数化。
- 新词进入学习队列时，第一次出现固定走 `英选中`。
- 同一个词后续再次出现时，从“尚未通过的关卡”里随机抽取一关。
- 词完成三关后离开活跃队列；如果本轮目标还没满，则立即从词书补入 `1` 个全新单词。
- 当“本轮已引入新词数达到目标”且“活跃队列清空”时，本轮学习结束，并展示 `开始拼写复习`。

**三、建议新增的数据层**

- 词书进度表
- 单词进度表
- 学习轮次表
- 学习轮次-单词关联表
- 学习会话记录表
- 复习会话记录表

**四、每张表建议存什么**

**词书进度表**

- 作用：记录某本词书整体学到哪里，以及当前滚动学习轮的总进度。
- 建议字段：
- `bookId`
- `nextWordSortOrderCursor`
- `learnedWordCount`
- `masteredWordCount`
- `reviewDueCount`
- `activeRoundId`
- `lastStudyAt`
- `lastReviewAt`

**单词进度表**

- 作用：记录每个单词当前学习状态、已通过关卡和复习状态。
- 建议字段：
- `wordId`
- `bookId`
- `status`
- `passedStages`
- `hasSeenInLearning`
- `learningWrongCount`
- `revealCount`
- `reviewWrongStreak`
- `reviewSuccessCount`
- `lastStudiedAt`
- `lastReviewedAt`
- `nextReviewAt`
- `lastResult`

**学习轮次表**

- 作用：记录“一轮滚动学习”的总目标、当前活跃窗口与中断恢复信息。
- 建议字段：
- `roundId`
- `bookId`
- `status`
- `activeQueueSize`
- `targetWordCount`
- `introducedWordCount`
- `masteredWordCountInRound`
- `createdAt`
- `completedAt`
- `nextSourceSortOrder`

**学习轮次-单词关联表**

- 作用：记录某一轮中出现过哪些词，以及它们当前是否仍在活跃队列。
- 建议字段：
- `roundId`
- `wordId`
- `queueOrder`
- `isActiveInQueue`
- `isMasteredInRound`
- `introducedAt`
- `completedAt`

**学习会话记录表**

- 作用：记录一次学习过程，用于统计和恢复。
- 建议字段：
- `sessionId`
- `roundId`
- `bookId`
- `startedAt`
- `endedAt`
- `isCompleted`
- `correctCount`
- `wrongCount`
- `skippedCount`

**复习会话记录表**

- 作用：记录拼写复习结果。
- 建议字段：
- `reviewSessionId`
- `bookId`
- `sourceRoundId`
- `startedAt`
- `endedAt`
- `completedWordCount`
- `sentBackToLearningCount`

**五、状态定义建议**

**单词状态**

- `NEW`：新词，还未进入任何学习轮。
- `LEARNING`：已进入滚动学习轮，但三关尚未全部通过。
- `LEARNED`：三关全部通过，等待立即拼写复习或计划复习。
- `REVIEW_READY`：可以进入复习池。
- `REVIEWING`：正在拼写复习。
- `MASTERED`：拼写复习稳定通过。
- `REPAIRED` 可选：从复习失败后打回学习。

**学习轮状态**

- `ACTIVE`：当前正在滚动学习。
- `LEARNING_DONE`：本轮目标词数已全部学完，可进入拼写复习。
- `REVIEW_PENDING`：等待点击 `开始拼写复习`。
- `REVIEW_DONE`：本轮对应的立即复习已完成。
- `ABANDONED` 可选：人为放弃。

**六、滚动学习队列状态机草案**

**关键参数**

- `activeQueueSize = 4`
- `roundTargetWordCount = 10`
- `firstEncounterQuestionType = 英选中`
- `laterEncounterQuestionType = 随机未通过关卡`

**初始化**

- 若存在未完成学习轮，优先恢复该学习轮，不重新抽词。
- 若不存在未完成学习轮，则新建一个 `roundId`。
- 从当前词书的 `nextWordSortOrderCursor` 开始顺序取词，直到活跃队列装满 `4` 个词。
- 每个新词入队时：
- 标记 `status = LEARNING`
- 标记 `hasSeenInLearning = false`
- 标记 `passedStages = []`
- `introducedWordCount + 1`

**出题规则**

- 取活跃队列队首的单词出题。
- 如果该词 `hasSeenInLearning = false`：
- 固定出 `英选中`
- 出题后标记 `hasSeenInLearning = true`
- 如果该词 `hasSeenInLearning = true`：
- 从 `passedStages` 之外的关卡里随机抽取一关

**作答后轮转规则**

- 当前词答对当前关卡：
- 将该关加入 `passedStages`
- 若该词尚未三关全过，则移到活跃队列队尾
- 若该词三关全过，则从活跃队列移除，并标记 `status = LEARNED`
- 当前词答错当前关卡：
- `passedStages` 不变
- 直接移到活跃队列队尾
- 当前词点击“没见过/直接看答案”：
- 不记通过
- 记录 `revealCount`
- 移到活跃队列队尾

**补词规则**

- 当某个词三关全过并从活跃队列移除后：
- 若 `introducedWordCount < roundTargetWordCount`
- 则从词书游标继续顺序拉取 `1` 个全新单词补到队尾
- 新补入单词第一次出现仍固定出 `英选中`
- 若 `introducedWordCount == roundTargetWordCount`
- 则不再补新词，只等待活跃队列自然清空

**结束条件**

- 同时满足以下两点时，本轮学习结束：
- `introducedWordCount == roundTargetWordCount`
- `activeQueue` 为空
- 学习结束后：
- 学习轮状态改为 `LEARNING_DONE`
- 页面展示 `开始拼写复习`
- 学习中心展示 `待复习 X 个`

**七、状态流转图（文字版）**

**学习主流程**

- `NEW -> LEARNING`
- `LEARNING -> LEARNING`：答对但未三关全过，或答错后回队尾
- `LEARNING -> LEARNED`：三关全部通过
- `LEARNED -> REVIEW_PENDING`：学习轮完成，等待立即复习
- `REVIEW_PENDING -> REVIEWING`：点击 `开始拼写复习`
- `REVIEWING -> MASTERED`：拼写复习稳定通过
- `REVIEWING -> REPAIRED`：拼写复习连续失败，被打回学习

**队列视角**

- `入队 -> 队首出题 -> 队尾轮转`
- `队首出题 -> 三关全过 -> 离队`
- `离队 -> 本轮未满 -> 补 1 个新词入队`
- `离队 -> 本轮已满 -> 不补词`

**八、拼写复习按钮与入口**

**完成页按钮**

- 主按钮：`开始拼写复习`
- 次按钮：`稍后再说`

**按钮逻辑**

- 点击 `开始拼写复习`：
- 进入复习模式
- 优先只复习“刚刚这一轮”完成学习的词
- 点击 `稍后再说`：
- 返回学习中心
- 保留 `REVIEW_PENDING`

**学习中心入口**

- 固定显示 `拼写复习`
- 固定显示 `待复习 X 个`
- 若存在刚学完但未复习的学习轮，优先展示 `继续本轮拼写复习`

**九、复习机制建议**

**立即复习**

- 来源：刚完成一轮滚动学习
- 对象：当前这轮完成学习的词
- 目的：巩固短时记忆

**计划复习**

- 来源：历史已学词
- 对象：达到 `nextReviewAt` 的词
- 目的：走长期记忆曲线

**建议**

- 第一版先只做“立即复习”
- `nextReviewAt` 先留字段，不急着做复杂调度
- 优先把“学习完成 -> 立即复习”闭环跑通

**十、中断恢复规则**

- 学习中退出：必须恢复当前学习轮
- 复习中退出：必须恢复当前复习轮
- 恢复时必须保存：
- 当前 `roundId`
- 活跃队列顺序
- `introducedWordCount`
- `nextWordSortOrderCursor`
- 每个活跃词的 `passedStages`
- 每个活跃词是否已经首次见词
- 只有当前学习轮彻底完成，才允许推进词书游标并开启下一轮

**十一、统计口径建议**

- `已学习单词数`：三关全部通过的词数
- `已掌握单词数`：拼写复习稳定通过的词数
- `当前待复习数`：状态为 `REVIEW_PENDING` 或到期复习的词数
- `本轮已引入数`：当前学习轮中已从词书拉入过的单词数
- `本轮已完成数`：当前学习轮中已三关全过的单词数
- `当前学习进度`：词书游标位置 + 已完成学习轮数量

**十二、容易遗漏的点**

**词书切换**

- 所有进度必须和 `bookId` 绑定
- 切换词书后，旧词书进度要保留

**返修词**

- 复习 `4` 连错打回学习时，不建议直接混入“新词滚动窗口”
- 建议进入单独的返修池，避免污染新词学习节奏

**直接看答案**

- 不算通过
- 要单独记录 `revealCount`
- 后续可作为“该词非常陌生”的判断依据

**随机性**

- 词是“顺序补词”
- 不是“一次整组打乱后学完”
- 关卡是“首次固定英选中，后续随机未通过关卡”

**十三、建议优先级**

**第一期必做**

- 词书进度表
- 单词进度表
- 学习轮次表
- 未完成学习轮恢复
- 活跃队列固定维持 `4`
- 学完一轮后显示 `开始拼写复习`

**第二期再做**

- 历史到期复习
- `nextReviewAt`
- 返修池
- 复习强度分层
- 统计报表

**十四、建议先拍板的产品规则**

- 活跃学习队列固定 `4` 个词
- 一轮总学习目标固定 `10` 个词
- 学习中断后默认恢复当前学习轮
- 三关完成算“已学习”
- 拼写复习稳定通过算“已掌握”
- 学完后复习可以跳过，但入口要始终可见
- 复习 `4` 连错打回学习，不混入新词滚动窗口，优先进入返修池

**十五、最小可行闭环**

- 从词书顺序拉取 `4` 个词进入活跃队列
- 首次见词固定 `英选中`
- 后续按未通过关卡随机出题
- 某词三关全过后离队
- 若本轮未满 `10` 个词，则立即补 `1` 个新词
- 当本轮已引入满 `10` 个词且活跃队列清空时，学习轮完成
- 完成后显示 `开始拼写复习`
- 进入复习页练本轮刚完成学习的词
- 退出后能恢复学习轮或复习轮
