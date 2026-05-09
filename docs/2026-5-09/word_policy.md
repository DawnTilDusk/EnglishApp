**背单词持久化状态设计清单**

**一、目标**

- 让背单词支持连续学习，而不是每次进来都像新开一局。
- 让系统记住“学到哪本词书的哪里”“当前这批词学到什么程度”“哪些词可以立即拼写复习”“哪些词以后再复习”。

**二、建议新增的数据层**

- 词书进度表
- 单词进度表
- 学习批次表
- 学习批次-单词关联表
- 学习会话记录表
- 复习会话记录表

**三、每张表建议存什么**

**词书进度表**

- 作用：记录某本词书整体学到哪里。
- 建议字段：
- bookId
- currentSortOrderCursor
- learnedWordCount
- masteredWordCount
- reviewDueCount
- hasActiveBatch
- lastStudyAt
- lastReviewAt

**单词进度表**

- 作用：记录每个单词当前属于“新词/学习中/待复习/已掌握”等哪种状态。
- 建议字段：
- wordId
- bookId
- status
- passedStages
- learningWrongCount
- revealCount
- reviewWrongStreak
- reviewSuccessCount
- lastStudiedAt
- lastReviewedAt
- nextReviewAt
- lastResult

**学习批次表**

- 作用：记录“这一轮 5 个词”这个容器本身，支持中断恢复。
- 建议字段：
- batchId
- bookId
- batchType
- status
- createdAt
- completedAt
- sourceStartSortOrder
- sourceEndSortOrder
- shuffledSeed

**学习批次-单词关联表**

- 作用：记录这批 5 个词分别有哪些，以及它们在批次里的顺序。
- 建议字段：
- batchId
- wordId
- queueOrder
- isCompletedInBatch
- isRemovedFromQueue

**学习会话记录表**

- 作用：记录一次学习过程，用于统计和恢复。
- 建议字段：
- sessionId
- batchId
- bookId
- mode
- startedAt
- endedAt
- isCompleted
- correctCount
- wrongCount
- skippedCount

**复习会话记录表**

- 作用：记录拼写复习结果。
- 建议字段：
- reviewSessionId
- bookId
- sourceBatchId
- startedAt
- endedAt
- completedWordCount
- sentBackToLearningCount

**四、状态定义建议**

**单词状态**

- NEW：新词，还没进入学习
- LEARNING：进入了学习批次，但三关还没过完
- LEARNED：三关全部通过，等待拼写复习
- REVIEW\_READY：可进入复习池
- REVIEWING：正在拼写复习
- MASTERED：复习稳定通过
- REPAIRED 可选：从复习失败后打回学习

**批次状态**

- ACTIVE：当前正在学
- LEARNING\_DONE：本轮 5 个词三关已全过
- REVIEW\_PENDING：等待点击拼写复习
- REVIEW\_DONE：本轮拼写复习也做完
- ABANDONED 可选：人为放弃

**五、你现在这版学习机制建议怎么落地**

**学习初始化**

- 从当前词书按 sortOrder 顺序取 5 个新词。
- 这 5 个词取出后，再随机打乱入队。
- 这批词生成一个 batchId。
- 如果已有未完成批次，优先恢复这个批次，不重新取词。

**学习过程中**

- 每个词维护 passedStages，例如：
- \[]
- \[英选中]
- \[英选中, 中选英]
- \[英选中, 中选英, 语境选词]
- 当前词不管答对答错，都移到队尾。
- 下一页始终展示“下一个词”的随机一个未通过关卡。
- 某词 3 关都过后：
- 从当前学习队列移除
- 标记该词为 LEARNED
- 更新 learnedWordCount

**一轮学习完成后**

- 当这批 5 个词全部三关通过：
- 批次状态改为 LEARNING\_DONE
- 页面显示按钮：开始拼写复习
- 同时学习中心显示：本轮待复习 5 个

**六、拼写复习按钮的建议交互**

**完成页按钮**

- 主按钮：开始拼写复习
- 次按钮：稍后再说

**按钮逻辑**

- 点 开始拼写复习：
- 进入复习模式
- 优先只复习“刚刚这一批”5 个词
- 点 稍后再说：
- 返回学习中心
- 但保留 REVIEW\_PENDING

**学习中心入口**

- 增加一个固定卡片：
- 拼写复习
- 待复习 X 个
- 如果有“刚学完待复习”的批次，优先展示“继续本轮拼写复习”

**七、复习机制建议**

**立即复习**

- 来源：刚完成一轮学习
- 对象：当前这批 5 个词
- 目的：巩固短时记忆

**计划复习**

- 来源：历史已学词
- 对象：达到 nextReviewAt 的词
- 目的：走长期记忆曲线

**建议**

- 第一版先只做“立即复习”
- nextReviewAt 留字段，但先不做复杂调度
- 这样能最快闭环

**八、中断恢复规则**

- 学习中退出：必须恢复当前批次
- 复习中退出：必须恢复当前复习批次
- 只有当前批次彻底完成，才允许推进词书游标
- 不建议退出后直接重抽新词，否则用户会很混乱

**九、统计口径建议**

- 已学习单词数：三关全部通过的词数
- 已掌握单词数：拼写复习稳定通过的词数
- 当前待复习数：状态为 REVIEW\_PENDING 或到期复习的词数
- 当前学习进度：词书游标位置 + 已完成批次数

**十、容易遗漏的点**

**词书切换**

- 所有进度必须和 bookId 绑定
- 切换词书后，旧词书进度要保留

**重学词**

- 复习 4 连错打回学习时，不建议混入“新词批次”
- 建议进入单独的“返修池”或“待返修批次”

**直接看答案**

- 不要算通过
- 要单独记录 revealCount
- 后面可作为“这个词非常陌生”的判断依据

**数据恢复**

- App 闪退、切后台、横竖屏重建后，都应能恢复当前批次

**随机性**

- 词是“顺序取 5 个，再随机打乱”
- 关卡是“从未通过关卡里随机一个”
- 如果要可恢复同一轮随机结果，建议保存 shuffledSeed

**十一、建议优先级**

**第一期必做**

- 词书进度表
- 单词进度表
- 学习批次表
- 未完成批次恢复
- 学完一轮后显示 开始拼写复习

**第二期再做**

- 历史到期复习
- nextReviewAt
- 返修池
- 复习强度分层
- 统计报表

**十二、我建议你先拍板的产品规则**

- 一轮学习固定就是 5 个词
- 学习中断后默认恢复当前批次
- 三关完成算“已学习”
- 拼写复习通过算“进一步掌握”
- 学完后复习可以跳过，但入口要始终可见
- 复习 4 连错打回学习，不混入新词，优先进入返修池

**十三、最小可行闭环**

- 顺序取 5 个词
- 打乱入学习队列
- 记录每词已过关卡
- 批次完成后显示 开始拼写复习
- 进入复习页练这 5 个词
- 退出后能恢复批次
- 下次继续从词书游标后面拿新词

如果你愿意，我下一步可以继续帮你整理成更工程化的一版：

- 需要新增的 Room Entity 清单
- 每个 Entity 的字段草案
- 学习/复习/恢复 的状态流转图

