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

    private var initializedArgs: VocabularyPracticeArgs? = null
    private var timerJob: Job? = null
    private val wrongWordIds = linkedSetOf<String>()

    fun initialize(args: VocabularyPracticeArgs) {
        if (initializedArgs == args && _uiState.value.questions.isNotEmpty()) return
        initializedArgs = args
        wrongWordIds.clear()
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

    fun onSubmitAnswer() {
        val state = _uiState.value
        val question = state.currentQuestion ?: return
        val selectedOptionId = state.selectedOptionId ?: return
        if (state.stage != VocabularyPracticeStage.Ready) return

        val selectedOption = question.optionList.firstOrNull { it.optionId == selectedOptionId } ?: return
        val isCorrect = selectedOption.isCorrect
        if (!isCorrect) {
            wrongWordIds += question.wordId
        }

        viewModelScope.launch {
            repository.submitQuestionRecord(
                VocabularyQuestionRecord(
                    sessionId = state.sessionMeta?.sessionId.orEmpty(),
                    questionId = question.questionId,
                    wordId = question.wordId,
                    selectedOptionId = selectedOptionId,
                    isCorrect = isCorrect,
                    isSkipped = false,
                    elapsedSeconds = state.elapsedSeconds
                )
            )
        }

        _uiState.update {
            it.copy(
                stage = VocabularyPracticeStage.AnswerEvaluated,
                answerStatus = if (isCorrect) AnswerStatus.Correct else AnswerStatus.Wrong,
                correctCount = it.correctCount + if (isCorrect) 1 else 0,
                wrongCount = it.wrongCount + if (isCorrect) 0 else 1,
                earnedTokens = it.earnedTokens + if (isCorrect) question.rewardToken else 0,
                canSubmitAnswer = false,
                canGoNext = true
            )
        }
    }

    fun onNextQuestion() {
        val state = _uiState.value
        val nextIndex = state.currentQuestionIndex + 1
        if (state.stage != VocabularyPracticeStage.AnswerEvaluated) return

        if (nextIndex >= state.questions.size) {
            stopTimer()
            _uiState.update {
                it.copy(
                    stage = VocabularyPracticeStage.Completed,
                    currentQuestion = null,
                    selectedOptionId = null,
                    canGoNext = false,
                    showFinishDialog = true
                )
            }
            return
        }

        val nextQuestion = state.questions[nextIndex]
        _uiState.update {
            it.copy(
                stage = VocabularyPracticeStage.Ready,
                currentQuestionIndex = nextIndex,
                currentQuestion = nextQuestion,
                selectedOptionId = null,
                answerStatus = AnswerStatus.Unanswered,
                remainingCount = (it.questions.size - nextIndex - 1).coerceAtLeast(0),
                canSubmitAnswer = false,
                canGoNext = false
            )
        }
    }

    fun onSkipQuestion() {
        val state = _uiState.value
        val question = state.currentQuestion ?: return
        if (state.stage != VocabularyPracticeStage.Ready) return

        wrongWordIds += question.wordId

        viewModelScope.launch {
            repository.submitQuestionRecord(
                VocabularyQuestionRecord(
                    sessionId = state.sessionMeta?.sessionId.orEmpty(),
                    questionId = question.questionId,
                    wordId = question.wordId,
                    selectedOptionId = null,
                    isCorrect = false,
                    isSkipped = true,
                    elapsedSeconds = state.elapsedSeconds
                )
            )
        }

        _uiState.update {
            it.copy(
                stage = VocabularyPracticeStage.AnswerEvaluated,
                answerStatus = AnswerStatus.Skipped,
                skippedCount = it.skippedCount + 1,
                selectedOptionId = null,
                canSubmitAnswer = false,
                canGoNext = true
            )
        }
    }

    fun onRetryLoad() {
        initializedArgs?.let(::loadSession)
    }

    fun onFinishSession() {
        finishSession(isCompleted = true)
    }

    private fun loadSession(args: VocabularyPracticeArgs) {
        stopTimer()
        _uiState.value = VocabularyPracticeUiState(stage = VocabularyPracticeStage.Loading)
        viewModelScope.launch {
            runCatching {
                repository.getPracticeSession(args)
            }.onSuccess { session ->
                if (session.questions.isEmpty()) {
                    _uiState.value = VocabularyPracticeUiState(stage = VocabularyPracticeStage.Empty)
                } else {
                    _uiState.value = VocabularyPracticeUiState(
                        stage = VocabularyPracticeStage.Ready,
                        sessionMeta = session.sessionMeta,
                        questions = session.questions,
                        currentQuestionIndex = 0,
                        currentQuestion = session.questions.first(),
                        remainingCount = (session.questions.size - 1).coerceAtLeast(0)
                    )
                    startTimer()
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
        val state = _uiState.value
        val sessionMeta = state.sessionMeta ?: return
        stopTimer()

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
            vocabularyDelta = state.correctCount,
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

    override fun onCleared() {
        stopTimer()
        super.onCleared()
    }
}
