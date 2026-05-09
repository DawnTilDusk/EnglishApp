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

@HiltViewModel
class VocabularyPracticeViewModel @Inject constructor(
    private val repository: VocabularyPracticeRepository
) : ViewModel() {

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
    private val pronouncedWordKeys = linkedSetOf<String>()
    private var currentSession: VocabularyPracticeSession? = null
    private var masteredStudyCount = 0
    private var completedReviewCount = 0
    private var sentBackToStudyCount = 0
    private var sessionFinished = false

    fun initialize(args: VocabularyPracticeArgs) {
        if (initializedArgs == args && currentSession != null) return
        initializedArgs = args
        loadSession(args)
    }

    fun onBackClick() {
        if (_uiState.value.stage == VocabularyPracticeStage.Completed) {
            onFinishSession()
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
            studyStageIndex = 0,
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
            feedbackMessage = "已直接看答案：${prompt.correctAnswerText}。该单词回到学习队尾，并从第 1 关重新开始。",
            correctDelta = 0,
            wrongDelta = 0,
            skippedDelta = 1,
            earnedTokensDelta = 0
        )
    }

    fun onReplayPronunciation() {
        val english = _uiState.value.currentPrompt?.word?.english ?: return
        viewModelScope.launch {
            _pronunciationEvents.emit(english)
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

    private fun loadSession(args: VocabularyPracticeArgs) {
        resetSessionState()
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
                    session.studyWords.forEach { word ->
                        wordMap[word.wordId] = word
                        progressMap[word.wordId] = VocabularyWordProgress(wordId = word.wordId)
                        studyQueue.addLast(word.wordId)
                    }
                    session.reviewWords.forEach { word ->
                        wordMap[word.wordId] = word
                        progressMap[word.wordId] = progressMap[word.wordId] ?: VocabularyWordProgress(wordId = word.wordId)
                        reviewQueue.addLast(word.wordId)
                    }
                    _uiState.value = VocabularyPracticeUiState(
                        stage = VocabularyPracticeStage.Ready,
                        sessionMeta = session.sessionMeta,
                        session = session,
                        studyQueueSize = studyQueue.size,
                        reviewQueueSize = reviewQueue.size
                    )
                    startTimer()
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

    private fun finishSession(isCompleted: Boolean) {
        if (sessionFinished) return
        val state = _uiState.value
        val sessionMeta = state.sessionMeta ?: return
        stopTimer()
        stopReviewHintTimer()
        sessionFinished = true

        val completedQuestionCount = state.correctCount + state.wrongCount + state.skippedCount
        val accuracy = if (completedQuestionCount == 0) 0f else state.correctCount.toFloat() / completedQuestionCount
        val result = StudyResult(
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

        viewModelScope.launch {
            repository.finishPracticeSession(result)
            _studyResults.emit(result)
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
        val feedbackMessage = when (prompt.questionType) {
            VocabularyQuestionType.StudyEnglishToChinese -> {
                progressMap[wordId] = progress.copy(studyStageIndex = 1)
                "第 1 关通过，进入第 2 关：根据中文选英文。"
            }

            VocabularyQuestionType.StudyChineseToEnglish -> {
                progressMap[wordId] = progress.copy(studyStageIndex = 2)
                "第 2 关通过，进入第 3 关：结合语境选词。"
            }

            VocabularyQuestionType.StudyContextChoice -> {
                progressMap[wordId] = progress.copy(
                    studyStageIndex = 3,
                    isMasteredToday = true
                )
                if (studyQueue.firstOrNull() == wordId) {
                    studyQueue.removeFirst()
                }
                masteredStudyCount += 1
                "三阶段全部通过，已加入今日已掌握。"
            }

            VocabularyQuestionType.ReviewSpelling -> return
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
    }

    private fun handleStudyWrong(
        prompt: VocabularyPracticePrompt,
        selectedOptionId: String
    ) {
        val wordId = prompt.word.wordId
        val progress = progressMap[wordId] ?: return
        val nextStudyStageIndex = when (prompt.questionType) {
            VocabularyQuestionType.StudyEnglishToChinese -> 0
            VocabularyQuestionType.StudyChineseToEnglish -> 0
            VocabularyQuestionType.StudyContextChoice -> 1
            VocabularyQuestionType.ReviewSpelling -> return
        }
        moveCurrentStudyWordToTail(wordId)
        progressMap[wordId] = progress.copy(
            studyStageIndex = nextStudyStageIndex,
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

        val fallbackStageLabel = when (nextStudyStageIndex) {
            0 -> "第 1 关"
            1 -> "第 2 关"
            else -> "当前关卡"
        }
        presentEvaluatedState(
            answerStatus = AnswerStatus.Wrong,
            feedbackMessage = "答错了，正确答案是 ${prompt.correctAnswerText}。该单词已移到队尾，并回退到$fallbackStageLabel。",
            correctDelta = 0,
            wrongDelta = 1,
            skippedDelta = 0,
            earnedTokensDelta = 0
        )
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
            presentEvaluatedState(
                answerStatus = AnswerStatus.Correct,
                feedbackMessage = "拼写正确，当前复习单词已完成。",
                correctDelta = 1,
                wrongDelta = 0,
                skippedDelta = 0,
                earnedTokensDelta = prompt.word.rewardToken
            )
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
                studyStageIndex = 0,
                consecutiveReviewWrongCount = 0,
                isMasteredToday = false,
                isReviewCompleted = false
            )
            if (studyQueue.none { it == wordId }) {
                studyQueue.addLast(wordId)
            }
            sentBackToStudyCount += 1
            presentEvaluatedState(
                answerStatus = AnswerStatus.Wrong,
                feedbackMessage = "连续拼错 4 次，正确答案是 ${prompt.word.english}。该单词已打回学习第 1 关。",
                correctDelta = 0,
                wrongDelta = 1,
                skippedDelta = 0,
                earnedTokensDelta = 0
            )
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
                    showFinishDialog = true,
                    studyQueueSize = studyQueue.size,
                    reviewQueueSize = reviewQueue.size,
                    masteredStudyCount = masteredStudyCount,
                    completedReviewCount = completedReviewCount,
                    sentBackToStudyCount = sentBackToStudyCount
                )
            }
            return
        }

        val (section, wordId) = nextEntry
        val word = wordMap[wordId] ?: return
        val progress = progressMap[wordId] ?: VocabularyWordProgress(wordId = wordId)
        val prompt = buildPrompt(
            section = section,
            word = word,
            progress = progress
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
                firstLetterHint = prompt.firstLetterHint,
                studyQueueSize = studyQueue.size,
                reviewQueueSize = reviewQueue.size,
                masteredStudyCount = masteredStudyCount,
                completedReviewCount = completedReviewCount,
                sentBackToStudyCount = sentBackToStudyCount,
                canSubmitAnswer = prompt.questionType != VocabularyQuestionType.ReviewSpelling && prompt.optionList.isNotEmpty(),
                canGoNext = false,
                showExitConfirmDialog = false
            )
        }

        maybePronounce(prompt)
        restartReviewHintTimer()
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
        progress: VocabularyWordProgress
    ): VocabularyPracticePrompt {
        return if (section == VocabularyPracticeMode.Study) {
            when (progress.studyStageIndex) {
                0 -> VocabularyPracticePrompt(
                    promptId = "${word.wordId}_study_1",
                    word = word,
                    section = section,
                    questionType = VocabularyQuestionType.StudyEnglishToChinese,
                    stageTitle = "第1关",
                    promptTitle = word.english,
                    promptBody = "根据英文选择正确中文释义",
                    helperText = "${word.phonetic}  ${word.partOfSpeech}",
                    optionList = word.translationOptions,
                    correctAnswerText = word.translation,
                    firstLetterHint = word.english.firstOrNull()?.uppercaseChar()?.toString()
                )

                1 -> VocabularyPracticePrompt(
                    promptId = "${word.wordId}_study_2",
                    word = word,
                    section = section,
                    questionType = VocabularyQuestionType.StudyChineseToEnglish,
                    stageTitle = "第2关",
                    promptTitle = word.translation,
                    promptBody = "根据中文选择正确英文单词",
                    helperText = "${word.partOfSpeech}  ${word.phonetic}",
                    optionList = word.englishOptions,
                    correctAnswerText = word.english,
                    firstLetterHint = word.english.firstOrNull()?.uppercaseChar()?.toString()
                )

                else -> VocabularyPracticePrompt(
                    promptId = "${word.wordId}_study_3",
                    word = word,
                    section = section,
                    questionType = VocabularyQuestionType.StudyContextChoice,
                    stageTitle = "第3关",
                    promptTitle = word.contextSentence,
                    promptBody = "中文释义：${word.translation}",
                    helperText = "",
                    optionList = word.contextOptions,
                    correctAnswerText = word.english,
                    firstLetterHint = word.english.firstOrNull()?.uppercaseChar()?.toString()
                )
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
        pronouncedWordKeys.clear()
        currentSession = null
        masteredStudyCount = 0
        completedReviewCount = 0
        sentBackToStudyCount = 0
        sessionFinished = false
    }

    override fun onCleared() {
        stopTimer()
        stopReviewHintTimer()
        super.onCleared()
    }
}
