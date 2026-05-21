package com.example.seedie.ui.screens.learning.practice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.seedie.domain.model.StudyResult
import com.example.seedie.domain.repository.VocabularyPracticeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

@HiltViewModel
class VocabularyPracticeViewModel @Inject constructor(
    private val repository: VocabularyPracticeRepository
) : ViewModel() {
    private companion object {
        const val STAGE_ONE_ACTIVE_QUEUE_SIZE = 4
        const val CURSOR_END_SENTINEL = Int.MAX_VALUE
    }

    private val _uiState = MutableStateFlow(VocabularyPracticeUiState())
    val uiState = _uiState.asStateFlow()

    private val _studyResults = MutableSharedFlow<StudyResult>()
    val studyResults = _studyResults.asSharedFlow()

    private val _pronunciationEvents = MutableSharedFlow<String>()
    val pronunciationEvents = _pronunciationEvents.asSharedFlow()

    private var initializedArgs: VocabularyPracticeArgs? = null
    private var timerJob: Job? = null
    private var reviewHintJob: Job? = null
    private val wrongWordIds = linkedSetOf<String>()
    private val studyQueue = ArrayDeque<String>()
    private val reviewQueue = ArrayDeque<String>()
    private val wordMap = linkedMapOf<String, VocabularyPracticeWord>()
    private val progressMap = linkedMapOf<String, VocabularyWordProgress>()
    private val studyWordOrder = mutableListOf<String>()
    private val pronouncedWordKeys = linkedSetOf<String>()
    private var currentSession: VocabularyPracticeSession? = null
    private var currentBookId: String? = null
    private var currentRoundId: String? = null
    private var currentEntryMode: VocabularyPracticeMode = VocabularyPracticeMode.Study
    private var carryoverResult: StudyResult? = null
    private var nextStudyWordIndex = 0
    private var nextStudyWordSortOrderCursor = CURSOR_END_SENTINEL
    private var introducedStudyCount = 0
    private var studyTargetCount = 0
    private var masteredStudyCount = 0
    private var completedReviewCount = 0
    private var sentBackToStudyCount = 0
    private var sessionFinished = false
    private val studyQuestionTypes = listOf(
        VocabularyQuestionType.StudyEnglishToChinese,
        VocabularyQuestionType.StudyChineseToEnglish,
        VocabularyQuestionType.StudyContextChoice
    )

    fun initialize(args: VocabularyPracticeArgs) {
        if (initializedArgs == args && currentSession != null) return
        initializedArgs = args
        loadSession(args)
    }

    fun onBackClick() {
        if (_uiState.value.stage == VocabularyPracticeStage.Completed) {
            if (_uiState.value.canStartImmediateReview) {
                onDeferReview()
            } else {
                onFinishSession()
            }
            return
        }
        _uiState.update { it.copy(showExitConfirmDialog = true) }
    }

    fun onConfirmExit() {
        _uiState.update { it.copy(showExitConfirmDialog = false) }
        finishSession(isCompleted = false)
    }

    fun onDismissExitDialog() {
        _uiState.update { it.copy(showExitConfirmDialog = false) }
    }

    fun onOptionSelected(optionId: String) {
        _uiState.update { state ->
            if (state.stage != VocabularyPracticeStage.Ready) {
                state
            } else {
                state.copy(
                    selectedOptionId = optionId,
                    canSubmitAnswer = true
                )
            }
        }
    }

    fun onSpellingInputChanged(value: String) {
        val prompt = _uiState.value.currentPrompt ?: return
        if (_uiState.value.stage != VocabularyPracticeStage.Ready ||
            prompt.questionType != VocabularyQuestionType.ReviewSpelling
        ) {
            return
        }

        _uiState.update {
            it.copy(
                spellingInput = value,
                canSubmitAnswer = value.isNotBlank(),
                showFirstLetterHint = false
            )
        }
        restartReviewHintTimer()
    }

    fun onSubmitAnswer() {
        val state = _uiState.value
        val prompt = state.currentPrompt ?: return
        if (state.stage != VocabularyPracticeStage.Ready) return

        when (prompt.questionType) {
            VocabularyQuestionType.ReviewSpelling -> handleReviewSubmit(
                prompt = prompt,
                typedAnswer = state.spellingInput
            )

            VocabularyQuestionType.StudyEnglishToChinese,
            VocabularyQuestionType.StudyChineseToEnglish,
            VocabularyQuestionType.StudyContextChoice -> {
                val selectedOptionId = state.selectedOptionId ?: return
                handleStudySubmit(prompt = prompt, selectedOptionId = selectedOptionId)
            }
        }
    }

    fun onRevealAnswer() {
        val state = _uiState.value
        val prompt = state.currentPrompt ?: return
        if (state.stage != VocabularyPracticeStage.Ready || prompt.section != VocabularyPracticeMode.Study) return

        val wordId = prompt.word.wordId
        val progress = progressMap[wordId] ?: return
        moveCurrentStudyWordToTail(wordId)
        progressMap[wordId] = progress.copy(
            totalWrongCount = progress.totalWrongCount + 1,
            revealCount = progress.revealCount + 1
        )
        wrongWordIds += wordId

        submitRecord(
            prompt = prompt,
            selectedOptionId = null,
            typedAnswer = null,
            isCorrect = false,
            isSkipped = true,
            usedRevealAnswer = true,
            usedFirstLetterHint = false
        )

        presentEvaluatedState(
            answerStatus = AnswerStatus.Revealed,
            feedbackMessage = "已直接看答案：${prompt.correctAnswerText}。该单词回到学习队尾；后续将随机进入其未通过关卡。",
            correctDelta = 0,
            wrongDelta = 0,
            skippedDelta = 1,
            earnedTokensDelta = 0
        )
        persistStudySnapshot()
    }

    fun onReplayPronunciation() {
        val english = _uiState.value.currentPrompt?.word?.english ?: return
        viewModelScope.launch {
            _pronunciationEvents.emit(english)
        }
    }

    fun onRevealPhoneticHint() {
        val state = _uiState.value
        val prompt = state.currentPrompt ?: return
        if (
            state.stage != VocabularyPracticeStage.Ready ||
            prompt.questionType != VocabularyQuestionType.StudyChineseToEnglish
        ) {
            return
        }
        _uiState.update { it.copy(showPhoneticHint = true) }
        viewModelScope.launch {
            _pronunciationEvents.emit(prompt.word.english)
        }
    }

    fun onNextQuestion() {
        if (_uiState.value.stage != VocabularyPracticeStage.AnswerEvaluated) return
        advanceToNextPrompt()
    }

    fun onRetryLoad() {
        initializedArgs?.let(::loadSession)
    }

    fun onFinishSession() {
        finishSession(isCompleted = true)
    }

    fun onStartImmediateReview() {
        val completedRoundId = _uiState.value.completedRoundId ?: return
        carryoverResult = buildStudyResult(_uiState.value, isCompleted = true)
        val args = VocabularyPracticeArgs(
            sourceModuleId = "vocabulary_review",
            planId = initializedArgs?.planId,
            difficulty = initializedArgs?.difficulty ?: "easy",
            entryMode = VocabularyPracticeMode.Review,
            targetRoundId = completedRoundId
        )
        viewModelScope.launch {
            initializedArgs = args
            loadSession(args)
        }
    }

    fun onDeferReview() {
        finishSession(isCompleted = true)
    }

    private fun loadSession(args: VocabularyPracticeArgs) {
        resetSessionState()
        currentEntryMode = args.entryMode
        stopTimer()
        _uiState.value = VocabularyPracticeUiState(stage = VocabularyPracticeStage.Loading)
        viewModelScope.launch {
            runCatching {
                repository.getPracticeSession(args)
            }.onSuccess { session ->
                if (session.studyWords.isEmpty() && session.reviewWords.isEmpty()) {
                    _uiState.value = VocabularyPracticeUiState(stage = VocabularyPracticeStage.Empty)
                } else {
                    currentSession = session
                    currentBookId = session.resumeSnapshot?.bookId
                        ?: session.studyWords.firstOrNull()?.bookId
                        ?: session.reviewWords.firstOrNull()?.bookId
                    currentRoundId = session.resumeSnapshot?.roundId ?: args.targetRoundId
                    studyTargetCount = session.resumeSnapshot?.studyTargetCount ?: session.studyWords.size
                    introducedStudyCount = session.resumeSnapshot?.introducedStudyCount ?: 0
                    masteredStudyCount = session.resumeSnapshot?.masteredStudyCount ?: 0
                    nextStudyWordSortOrderCursor =
                        session.resumeSnapshot?.nextWordSortOrderCursor ?: CURSOR_END_SENTINEL
                    session.studyWords.forEach { word ->
                        wordMap[word.wordId] = word
                        studyWordOrder += word.wordId
                    }
                    if (currentEntryMode == VocabularyPracticeMode.Study && session.resumeSnapshot != null) {
                        restoreStudyState(session)
                    } else if (currentEntryMode == VocabularyPracticeMode.Study) {
                        session.studyWords.forEach { word ->
                            progressMap[word.wordId] = VocabularyWordProgress(wordId = word.wordId)
                        }
                        fillStudyQueueIfNeeded()
                    }
                    session.reviewWords.forEach { word ->
                        wordMap[word.wordId] = word
                        progressMap[word.wordId] = progressMap[word.wordId]
                            ?: VocabularyWordProgress(wordId = word.wordId)
                        reviewQueue.addLast(word.wordId)
                    }
                    _uiState.value = VocabularyPracticeUiState(
                        stage = VocabularyPracticeStage.Ready,
                        sessionMeta = session.sessionMeta,
                        session = session,
                        studyQueueSize = studyQueue.size,
                        reviewQueueSize = reviewQueue.size,
                        introducedStudyCount = introducedStudyCount,
                        studyTargetCount = studyTargetCount,
                        masteredStudyCount = masteredStudyCount
                    )
                    startTimer()
                    if (currentEntryMode == VocabularyPracticeMode.Study) {
                        persistStudySnapshot()
                    }
                    advanceToNextPrompt()
                }
            }.onFailure { throwable ->
                _uiState.value = VocabularyPracticeUiState(
                    stage = VocabularyPracticeStage.Error,
                    errorMessage = throwable.message ?: "词包加载失败，请稍后重试"
                )
            }
        }
    }

    private fun restoreStudyState(session: VocabularyPracticeSession) {
        val snapshot = session.resumeSnapshot ?: return
        val progressByWordId = snapshot.wordProgressList.associateBy { it.wordId }
        studyQueue.clear()
        snapshot.activeWordIds.forEach { wordId ->
            studyQueue.addLast(wordId)
            val progress = progressByWordId[wordId]
            progressMap[wordId] = VocabularyWordProgress(
                wordId = wordId,
                passedStudyQuestionTypes = progress?.passedStudyQuestionTypes.orEmpty(),
                hasSeenStudyWord = progress?.hasSeenStudyWord ?: false,
                totalWrongCount = progress?.totalWrongCount ?: 0,
                revealCount = progress?.revealCount ?: 0
            )
        }
        studyWordOrder.forEach { wordId ->
            if (wordId !in progressMap) {
                progressMap[wordId] = VocabularyWordProgress(wordId = wordId)
            }
        }
        nextStudyWordIndex = snapshot.activeWordIds.size.coerceAtMost(studyWordOrder.size)
    }

    private fun finishSession(isCompleted: Boolean) {
        if (sessionFinished) return
        val state = _uiState.value
        val sessionMeta = state.sessionMeta ?: return
        stopTimer()
        stopReviewHintTimer()
        sessionFinished = true

        val result = carryoverResult
            ?.let { baseResult -> mergeStudyResults(baseResult, buildStudyResult(state, isCompleted)) }
            ?: buildStudyResult(state, isCompleted)
        carryoverResult = null

        viewModelScope.launch {
            repository.finishPracticeSession(result)
            _studyResults.emit(result)
        }
    }

    private fun buildCurrentStudySnapshot(): VocabularyPracticeResumeSnapshot? {
        if (currentEntryMode != VocabularyPracticeMode.Study) return null
        val roundId = currentRoundId ?: return null
        val bookId = currentBookId ?: return null
        return VocabularyPracticeResumeSnapshot(
            roundId = roundId,
            bookId = bookId,
            activeWordIds = studyQueue.toList(),
            introducedStudyCount = introducedStudyCount,
            studyTargetCount = studyTargetCount,
            nextWordSortOrderCursor = nextStudyWordSortOrderCursor,
            masteredStudyCount = masteredStudyCount,
            wordProgressList = studyQueue.mapNotNull { wordId ->
                val progress = progressMap[wordId] ?: return@mapNotNull null
                VocabularyResumeWordProgress(
                    wordId = wordId,
                    passedStudyQuestionTypes = progress.passedStudyQuestionTypes,
                    hasSeenStudyWord = progress.hasSeenStudyWord,
                    totalWrongCount = progress.totalWrongCount,
                    revealCount = progress.revealCount
                )
            }
        )
    }

    private fun persistStudySnapshot() {
        val snapshot = buildCurrentStudySnapshot() ?: return
        viewModelScope.launch {
            repository.saveStudyRoundSnapshot(snapshot)
        }
    }

    private fun startTimer() {
        stopTimer()
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                _uiState.update { state ->
                    if (state.stage == VocabularyPracticeStage.Completed ||
                        state.stage == VocabularyPracticeStage.Empty ||
                        state.stage == VocabularyPracticeStage.Error
                    ) {
                        state
                    } else {
                        state.copy(elapsedSeconds = state.elapsedSeconds + 1)
                    }
                }
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    private fun handleStudySubmit(
        prompt: VocabularyPracticePrompt,
        selectedOptionId: String
    ) {
        val selectedOption = prompt.optionList.firstOrNull { it.optionId == selectedOptionId } ?: return
        val isCorrect = selectedOption.isCorrect
        if (isCorrect) {
            handleStudyCorrect(prompt = prompt, selectedOptionId = selectedOptionId)
        } else {
            handleStudyWrong(prompt = prompt, selectedOptionId = selectedOptionId)
        }
    }

    private fun handleStudyCorrect(
        prompt: VocabularyPracticePrompt,
        selectedOptionId: String
    ) {
        val wordId = prompt.word.wordId
        val progress = progressMap[wordId] ?: return
        if (prompt.questionType !in studyQuestionTypes) return
        val passedTypes = progress.passedStudyQuestionTypes + prompt.questionType
        val allPassed = passedTypes.containsAll(studyQuestionTypes)
        progressMap[wordId] = progress.copy(
            passedStudyQuestionTypes = passedTypes,
            isMasteredToday = allPassed
        )

        val feedbackMessage = if (allPassed) {
            removeWordFromStudyQueue(wordId)
            masteredStudyCount += 1
            fillStudyQueueIfNeeded()
            "当前单词 3 个关卡均通过，已标记掌握。"
        } else {
            moveCurrentStudyWordToTail(wordId)
            "本关已通过，单词移至队尾；后续将随机进入该单词未通过关卡。"
        }

        submitRecord(
            prompt = prompt,
            selectedOptionId = selectedOptionId,
            typedAnswer = null,
            isCorrect = true,
            isSkipped = false,
            usedRevealAnswer = false,
            usedFirstLetterHint = false
        )

        presentEvaluatedState(
            answerStatus = AnswerStatus.Correct,
            feedbackMessage = feedbackMessage,
            correctDelta = 1,
            wrongDelta = 0,
            skippedDelta = 0,
            earnedTokensDelta = prompt.word.rewardToken
        )
        persistStudySnapshot()
    }

    private fun handleStudyWrong(
        prompt: VocabularyPracticePrompt,
        selectedOptionId: String
    ) {
        val wordId = prompt.word.wordId
        val progress = progressMap[wordId] ?: return
        if (prompt.questionType !in studyQuestionTypes) return
        moveCurrentStudyWordToTail(wordId)
        progressMap[wordId] = progress.copy(
            totalWrongCount = progress.totalWrongCount + 1
        )
        wrongWordIds += wordId

        submitRecord(
            prompt = prompt,
            selectedOptionId = selectedOptionId,
            typedAnswer = null,
            isCorrect = false,
            isSkipped = false,
            usedRevealAnswer = false,
            usedFirstLetterHint = false
        )

        presentEvaluatedState(
            answerStatus = AnswerStatus.Wrong,
            feedbackMessage = "答错了，正确答案是 ${prompt.correctAnswerText}。该单词已移到队尾；下次将随机进入它的未通过关卡。",
            correctDelta = 0,
            wrongDelta = 1,
            skippedDelta = 0,
            earnedTokensDelta = 0
        )
        persistStudySnapshot()
    }

    private fun handleReviewSubmit(
        prompt: VocabularyPracticePrompt,
        typedAnswer: String
    ) {
        val wordId = prompt.word.wordId
        val progress = progressMap[wordId] ?: return
        val normalizedAnswer = normalizeAnswer(typedAnswer)
        val correctAnswer = normalizeAnswer(prompt.word.english)
        val isCorrect = normalizedAnswer.equals(correctAnswer, ignoreCase = true)

        submitRecord(
            prompt = prompt,
            selectedOptionId = null,
            typedAnswer = typedAnswer,
            isCorrect = isCorrect,
            isSkipped = false,
            usedRevealAnswer = false,
            usedFirstLetterHint = _uiState.value.showFirstLetterHint
        )

        if (reviewQueue.firstOrNull() == wordId) {
            reviewQueue.removeFirst()
        }

        if (isCorrect) {
            progressMap[wordId] = progress.copy(
                consecutiveReviewWrongCount = 0,
                isReviewCompleted = true
            )
            completedReviewCount += 1
            val roundId = currentRoundId
            if (roundId == null) {
                presentEvaluatedState(
                    answerStatus = AnswerStatus.Correct,
                    feedbackMessage = "拼写正确，当前复习单词已完成。",
                    correctDelta = 1,
                    wrongDelta = 0,
                    skippedDelta = 0,
                    earnedTokensDelta = prompt.word.rewardToken
                )
            } else {
                _uiState.update {
                    it.copy(
                        canSubmitAnswer = false,
                        canGoNext = false
                    )
                }
                viewModelScope.launch {
                    val reviewUpdateResult = repository.markReviewWordMastered(
                        roundId = roundId,
                        wordId = wordId
                    )
                    presentEvaluatedState(
                        answerStatus = AnswerStatus.Correct,
                        feedbackMessage = if (reviewUpdateResult.isRoundCompleted) {
                            "拼写正确，本轮复习已全部完成。"
                        } else {
                            "拼写正确，当前复习单词已完成。"
                        },
                        correctDelta = 1,
                        wrongDelta = 0,
                        skippedDelta = 0,
                        earnedTokensDelta = prompt.word.rewardToken
                    )
                }
            }
            return
        }

        wrongWordIds += wordId
        val wrongStreak = progress.consecutiveReviewWrongCount + 1
        val updatedProgress = progress.copy(
            consecutiveReviewWrongCount = wrongStreak,
            totalWrongCount = progress.totalWrongCount + 1
        )

        if (wrongStreak >= 4) {
            progressMap[wordId] = updatedProgress.copy(
                passedStudyQuestionTypes = emptySet(),
                hasSeenStudyWord = false,
                consecutiveReviewWrongCount = 0,
                isMasteredToday = false,
                isReviewCompleted = false
            )
            sentBackToStudyCount += 1
            val roundId = currentRoundId
            if (roundId == null) {
                presentEvaluatedState(
                    answerStatus = AnswerStatus.Wrong,
                    feedbackMessage = "连续拼错 4 次，正确答案是 ${prompt.word.english}。该单词已打回后续学习，将从第 1 关重新开始。",
                    correctDelta = 0,
                    wrongDelta = 1,
                    skippedDelta = 0,
                    earnedTokensDelta = 0
                )
            } else {
                _uiState.update {
                    it.copy(
                        canSubmitAnswer = false,
                        canGoNext = false
                    )
                }
                viewModelScope.launch {
                    val reviewUpdateResult = repository.markReviewWordSentBackToLearning(
                        roundId = roundId,
                        wordId = wordId
                    )
                    presentEvaluatedState(
                        answerStatus = AnswerStatus.Wrong,
                        feedbackMessage = if (reviewUpdateResult.isRoundCompleted) {
                            "连续拼错 4 次，正确答案是 ${prompt.word.english}。该单词已打回后续学习，本轮立即复习已结束。"
                        } else {
                            "连续拼错 4 次，正确答案是 ${prompt.word.english}。该单词已移出本轮复习，将在后续学习中从第 1 关重新开始。"
                        },
                        correctDelta = 0,
                        wrongDelta = 1,
                        skippedDelta = 0,
                        earnedTokensDelta = 0
                    )
                }
            }
        } else {
            progressMap[wordId] = updatedProgress
            reviewQueue.addLast(wordId)
            presentEvaluatedState(
                answerStatus = AnswerStatus.Wrong,
                feedbackMessage = "拼写错误，正确答案是 ${prompt.word.english}。已移到复习队尾，连续错误 $wrongStreak/4。",
                correctDelta = 0,
                wrongDelta = 1,
                skippedDelta = 0,
                earnedTokensDelta = 0
            )
        }
    }

    private fun advanceToNextPrompt() {
        stopReviewHintTimer()
        val nextEntry = determineNextWord() ?: run {
            val completedRoundId = currentRoundId
            if (currentEntryMode == VocabularyPracticeMode.Study && completedRoundId != null) {
                _uiState.update {
                    it.copy(
                        canGoNext = false,
                        canSubmitAnswer = false
                    )
                }
                viewModelScope.launch {
                    repository.markStudyRoundReviewPending(completedRoundId)
                    showCompletedState(completedRoundId)
                }
                return
            }
            showCompletedState(completedRoundId)
            return
        }

        val (section, wordId) = nextEntry
        val word = wordMap[wordId] ?: return
        var progress = progressMap[wordId] ?: VocabularyWordProgress(wordId = wordId)
        if (section == VocabularyPracticeMode.Study && progress.passedStudyQuestionTypes.containsAll(studyQuestionTypes)) {
            removeWordFromStudyQueue(wordId)
            if (!progress.isMasteredToday) {
                masteredStudyCount += 1
                progress = progress.copy(isMasteredToday = true)
                progressMap[wordId] = progress
            }
            advanceToNextPrompt()
            return
        }
        val randomStudyQuestionType = if (section == VocabularyPracticeMode.Study) {
            if (!progress.hasSeenStudyWord) {
                progress = progress.copy(hasSeenStudyWord = true)
                progressMap[wordId] = progress
                VocabularyQuestionType.StudyEnglishToChinese
            } else {
                pickRandomUnpassedStudyQuestionType(progress)
            }
        } else {
            null
        }
        val prompt = buildPrompt(
            section = section,
            word = word,
            progress = progress,
            forcedStudyQuestionType = randomStudyQuestionType
        )

        _uiState.update {
            it.copy(
                stage = VocabularyPracticeStage.Ready,
                session = currentSession,
                currentSection = section,
                currentPrompt = prompt,
                currentWordProgress = progress,
                selectedOptionId = null,
                spellingInput = "",
                answerStatus = AnswerStatus.Unanswered,
                feedbackMessage = "",
                reviewHintCountdownSec = 5,
                showFirstLetterHint = false,
                showPhoneticHint = false,
                firstLetterHint = prompt.firstLetterHint,
                studyQueueSize = studyQueue.size,
                reviewQueueSize = reviewQueue.size,
                introducedStudyCount = introducedStudyCount,
                studyTargetCount = studyTargetCount,
                masteredStudyCount = masteredStudyCount,
                completedReviewCount = completedReviewCount,
                sentBackToStudyCount = sentBackToStudyCount,
                canSubmitAnswer = prompt.questionType != VocabularyQuestionType.ReviewSpelling && prompt.optionList.isNotEmpty(),
                canGoNext = false,
                showExitConfirmDialog = false
            )
        }

        persistStudySnapshot()
        maybePronounce(prompt)
        restartReviewHintTimer()
    }

    private fun showCompletedState(completedRoundId: String?) {
        stopTimer()
        _uiState.update {
            it.copy(
                stage = VocabularyPracticeStage.Completed,
                currentPrompt = null,
                currentWordProgress = null,
                selectedOptionId = null,
                spellingInput = "",
                feedbackMessage = "",
                canSubmitAnswer = false,
                canGoNext = false,
                showFinishDialog = currentEntryMode != VocabularyPracticeMode.Study,
                studyQueueSize = studyQueue.size,
                reviewQueueSize = reviewQueue.size,
                introducedStudyCount = introducedStudyCount,
                studyTargetCount = studyTargetCount,
                masteredStudyCount = masteredStudyCount,
                completedReviewCount = completedReviewCount,
                sentBackToStudyCount = sentBackToStudyCount,
                completedRoundId = completedRoundId,
                pendingReviewWordCount = if (currentEntryMode == VocabularyPracticeMode.Study) {
                    masteredStudyCount
                } else {
                    0
                },
                canStartImmediateReview = currentEntryMode == VocabularyPracticeMode.Study &&
                    masteredStudyCount > 0
            )
        }
    }

    private fun determineNextWord(): Pair<VocabularyPracticeMode, String>? {
        val studyWordId = studyQueue.firstOrNull()
        if (studyWordId != null) {
            return VocabularyPracticeMode.Study to studyWordId
        }
        val reviewWordId = reviewQueue.firstOrNull()
        if (reviewWordId != null) {
            return VocabularyPracticeMode.Review to reviewWordId
        }
        return null
    }

    private fun buildPrompt(
        section: VocabularyPracticeMode,
        word: VocabularyPracticeWord,
        progress: VocabularyWordProgress,
        forcedStudyQuestionType: VocabularyQuestionType? = null
    ): VocabularyPracticePrompt {
        return if (section == VocabularyPracticeMode.Study) {
            val currentStageTitle = "第${progress.passedStudyQuestionTypes.size + 1}关"
            when (forcedStudyQuestionType) {
                VocabularyQuestionType.StudyEnglishToChinese -> VocabularyPracticePrompt(
                    promptId = "${word.wordId}_study_1",
                    word = word,
                    section = section,
                    questionType = VocabularyQuestionType.StudyEnglishToChinese,
                    stageTitle = currentStageTitle,
                    promptTitle = word.english,
                    promptBody = "根据英文选择正确中文释义",
                    helperText = "${word.phonetic}  ${word.partOfSpeech}",
                    optionList = word.translationOptions,
                    correctAnswerText = word.translation,
                    firstLetterHint = word.english.firstOrNull()?.uppercaseChar()?.toString()
                )

                VocabularyQuestionType.StudyChineseToEnglish -> VocabularyPracticePrompt(
                    promptId = "${word.wordId}_study_2",
                    word = word,
                    section = section,
                    questionType = VocabularyQuestionType.StudyChineseToEnglish,
                    stageTitle = currentStageTitle,
                    promptTitle = word.translation,
                    promptBody = "根据中文选择正确英文单词",
                    helperText = "${word.partOfSpeech}  ${word.phonetic}",
                    optionList = word.englishOptions,
                    correctAnswerText = word.english,
                    firstLetterHint = word.english.firstOrNull()?.uppercaseChar()?.toString()
                )

                VocabularyQuestionType.StudyContextChoice -> VocabularyPracticePrompt(
                    promptId = "${word.wordId}_study_3",
                    word = word,
                    section = section,
                    questionType = VocabularyQuestionType.StudyContextChoice,
                    stageTitle = currentStageTitle,
                    promptTitle = word.contextSentence,
                    promptBody = "中文释义：${word.translation}",
                    helperText = "",
                    optionList = word.contextOptions,
                    correctAnswerText = word.english,
                    firstLetterHint = word.english.firstOrNull()?.uppercaseChar()?.toString()
                )
                else -> error("学习板块没有可用的未通过关卡")
            }
        } else {
            VocabularyPracticePrompt(
                promptId = "${word.wordId}_review_spelling",
                word = word,
                section = section,
                questionType = VocabularyQuestionType.ReviewSpelling,
                stageTitle = "复习拼写",
                promptTitle = word.translation,
                promptBody = "根据中文词义和发音拼写英文单词",
                helperText = "连续错误 ${progress.consecutiveReviewWrongCount}/4 · ${word.phonetic}",
                correctAnswerText = word.english,
                firstLetterHint = word.english.firstOrNull()?.uppercaseChar()?.toString()
            )
        }
    }

    private fun presentEvaluatedState(
        answerStatus: AnswerStatus,
        feedbackMessage: String,
        correctDelta: Int,
        wrongDelta: Int,
        skippedDelta: Int,
        earnedTokensDelta: Int
    ) {
        stopReviewHintTimer()
        _uiState.update {
            it.copy(
                stage = VocabularyPracticeStage.AnswerEvaluated,
                answerStatus = answerStatus,
                feedbackMessage = feedbackMessage,
                correctCount = it.correctCount + correctDelta,
                wrongCount = it.wrongCount + wrongDelta,
                skippedCount = it.skippedCount + skippedDelta,
                earnedTokens = it.earnedTokens + earnedTokensDelta,
                studyQueueSize = studyQueue.size,
                reviewQueueSize = reviewQueue.size,
                introducedStudyCount = introducedStudyCount,
                studyTargetCount = studyTargetCount,
                masteredStudyCount = masteredStudyCount,
                completedReviewCount = completedReviewCount,
                sentBackToStudyCount = sentBackToStudyCount,
                canSubmitAnswer = false,
                canGoNext = true
            )
        }
    }

    private fun restartReviewHintTimer() {
        stopReviewHintTimer()
        val state = _uiState.value
        val prompt = state.currentPrompt ?: return
        if (state.stage != VocabularyPracticeStage.Ready ||
            prompt.questionType != VocabularyQuestionType.ReviewSpelling
        ) {
            return
        }

        _uiState.update {
            it.copy(
                reviewHintCountdownSec = 5,
                showFirstLetterHint = false,
                firstLetterHint = prompt.firstLetterHint
            )
        }

        reviewHintJob = viewModelScope.launch {
            for (remaining in 4 downTo 0) {
                delay(1000)
                val latestState = _uiState.value
                if (latestState.stage != VocabularyPracticeStage.Ready ||
                    latestState.currentPrompt?.promptId != prompt.promptId
                ) {
                    return@launch
                }
                _uiState.update { current ->
                    current.copy(reviewHintCountdownSec = remaining)
                }
            }
            handleReviewTimeout(prompt)
        }
    }

    private fun handleReviewTimeout(prompt: VocabularyPracticePrompt) {
        val latestState = _uiState.value
        if (latestState.stage != VocabularyPracticeStage.Ready ||
            latestState.currentPrompt?.promptId != prompt.promptId
        ) {
            return
        }

        val wordId = prompt.word.wordId
        if (reviewQueue.firstOrNull() == wordId) {
            reviewQueue.removeFirst()
            reviewQueue.addLast(wordId)
        }

        submitRecord(
            prompt = prompt,
            selectedOptionId = null,
            typedAnswer = latestState.spellingInput,
            isCorrect = false,
            isSkipped = true,
            usedRevealAnswer = false,
            usedFirstLetterHint = true
        )

        presentEvaluatedState(
            answerStatus = AnswerStatus.TimedOut,
            feedbackMessage = "5 秒未完成作答，首字母提示：${prompt.firstLetterHint}。该单词已回到复习队尾，稍后再次拼写。",
            correctDelta = 0,
            wrongDelta = 0,
            skippedDelta = 1,
            earnedTokensDelta = 0
        )

        _uiState.update {
            it.copy(
                showFirstLetterHint = true,
                firstLetterHint = prompt.firstLetterHint
            )
        }
    }

    private fun maybePronounce(prompt: VocabularyPracticePrompt) {
        val wordKey = "${prompt.section}:${prompt.word.wordId}"
        if (!pronouncedWordKeys.add(wordKey)) return
        viewModelScope.launch {
            _pronunciationEvents.emit(prompt.word.english)
        }
    }

    private fun moveCurrentStudyWordToTail(wordId: String) {
        if (studyQueue.firstOrNull() == wordId) {
            studyQueue.removeFirst()
            studyQueue.addLast(wordId)
        }
    }

    private fun removeWordFromStudyQueue(wordId: String) {
        studyQueue.remove(wordId)
    }

    private fun fillStudyQueueIfNeeded() {
        while (studyQueue.size < STAGE_ONE_ACTIVE_QUEUE_SIZE) {
            val appended = enqueueNextStudyWordIfAvailable()
            if (!appended) break
        }
    }

    private fun enqueueNextStudyWordIfAvailable(): Boolean {
        if (nextStudyWordIndex >= studyWordOrder.size) return false
        val wordId = studyWordOrder[nextStudyWordIndex]
        nextStudyWordIndex += 1
        introducedStudyCount += 1
        if (studyQueue.none { it == wordId }) {
            studyQueue.addLast(wordId)
        }
        nextStudyWordSortOrderCursor = studyWordOrder.getOrNull(nextStudyWordIndex)
            ?.let { nextWordId -> wordMap[nextWordId]?.sortOrder }
            ?: CURSOR_END_SENTINEL
        return true
    }

    private fun pickRandomUnpassedStudyQuestionType(progress: VocabularyWordProgress): VocabularyQuestionType? {
        val pendingTypes = studyQuestionTypes.filterNot { it in progress.passedStudyQuestionTypes }
        if (pendingTypes.isEmpty()) return null
        return pendingTypes.random(Random.Default)
    }

    private fun submitRecord(
        prompt: VocabularyPracticePrompt,
        selectedOptionId: String?,
        typedAnswer: String?,
        isCorrect: Boolean,
        isSkipped: Boolean,
        usedRevealAnswer: Boolean,
        usedFirstLetterHint: Boolean
    ) {
        viewModelScope.launch {
            repository.submitQuestionRecord(
                VocabularyQuestionRecord(
                    sessionId = _uiState.value.sessionMeta?.sessionId.orEmpty(),
                    promptId = prompt.promptId,
                    wordId = prompt.word.wordId,
                    section = prompt.section,
                    questionType = prompt.questionType,
                    selectedOptionId = selectedOptionId,
                    typedAnswer = typedAnswer,
                    isCorrect = isCorrect,
                    isSkipped = isSkipped,
                    usedRevealAnswer = usedRevealAnswer,
                    usedFirstLetterHint = usedFirstLetterHint,
                    elapsedSeconds = _uiState.value.elapsedSeconds
                )
            )
        }
    }

    private fun stopReviewHintTimer() {
        reviewHintJob?.cancel()
        reviewHintJob = null
    }

    private fun normalizeAnswer(value: String): String {
        return value.trim().lowercase()
    }

    private fun resetSessionState() {
        stopReviewHintTimer()
        wrongWordIds.clear()
        studyQueue.clear()
        reviewQueue.clear()
        wordMap.clear()
        progressMap.clear()
        studyWordOrder.clear()
        pronouncedWordKeys.clear()
        currentSession = null
        currentBookId = null
        currentRoundId = null
        currentEntryMode = VocabularyPracticeMode.Study
        nextStudyWordIndex = 0
        nextStudyWordSortOrderCursor = CURSOR_END_SENTINEL
        introducedStudyCount = 0
        studyTargetCount = 0
        masteredStudyCount = 0
        completedReviewCount = 0
        sentBackToStudyCount = 0
        sessionFinished = false
    }

    private fun buildStudyResult(
        state: VocabularyPracticeUiState,
        isCompleted: Boolean
    ): StudyResult {
        val sessionMeta = state.sessionMeta ?: error("sessionMeta should not be null when building result")
        val completedQuestionCount = state.correctCount + state.wrongCount + state.skippedCount
        val accuracy = if (completedQuestionCount == 0) 0f else state.correctCount.toFloat() / completedQuestionCount
        return StudyResult(
            sessionId = sessionMeta.sessionId,
            moduleId = sessionMeta.moduleId,
            isCompleted = isCompleted,
            completedQuestionCount = completedQuestionCount,
            correctCount = state.correctCount,
            wrongCount = state.wrongCount,
            skippedCount = state.skippedCount,
            accuracy = accuracy,
            earnedTokens = state.earnedTokens,
            studyDurationSec = state.elapsedSeconds,
            vocabularyDelta = state.masteredStudyCount,
            wrongWordIds = wrongWordIds.toList()
        )
    }

    private fun mergeStudyResults(
        baseResult: StudyResult,
        currentResult: StudyResult
    ): StudyResult {
        val completedQuestionCount = baseResult.completedQuestionCount + currentResult.completedQuestionCount
        val correctCount = baseResult.correctCount + currentResult.correctCount
        val wrongCount = baseResult.wrongCount + currentResult.wrongCount
        val skippedCount = baseResult.skippedCount + currentResult.skippedCount
        val accuracy = if (completedQuestionCount == 0) 0f else correctCount.toFloat() / completedQuestionCount
        return StudyResult(
            sessionId = baseResult.sessionId,
            moduleId = baseResult.moduleId,
            isCompleted = baseResult.isCompleted && currentResult.isCompleted,
            completedQuestionCount = completedQuestionCount,
            correctCount = correctCount,
            wrongCount = wrongCount,
            skippedCount = skippedCount,
            accuracy = accuracy,
            earnedTokens = baseResult.earnedTokens + currentResult.earnedTokens,
            studyDurationSec = baseResult.studyDurationSec + currentResult.studyDurationSec,
            vocabularyDelta = baseResult.vocabularyDelta,
            wrongWordIds = (baseResult.wrongWordIds + currentResult.wrongWordIds).distinct()
        )
    }

    override fun onCleared() {
        stopTimer()
        stopReviewHintTimer()
        super.onCleared()
    }
}
