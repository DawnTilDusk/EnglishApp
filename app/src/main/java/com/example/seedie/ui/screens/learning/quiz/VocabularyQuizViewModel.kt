package com.example.seedie.ui.screens.learning.quiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.seedie.domain.model.StudyResult
import com.example.seedie.domain.quiz.VocabularyEstimateCalculator
import com.example.seedie.domain.quiz.VocabularyQuizConstants
import com.example.seedie.domain.repository.VocabularyQuizRepository
import com.example.seedie.ui.screens.learning.practice.AnswerStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@HiltViewModel
class VocabularyQuizViewModel @Inject constructor(
    private val repository: VocabularyQuizRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(VocabularyQuizUiState())
    val uiState = _uiState.asStateFlow()

    private val _studyResults = MutableSharedFlow<StudyResult>()
    val studyResults = _studyResults.asSharedFlow()

    private var session: VocabularyQuizSession? = null
    private var sessionFinished = false
    private var timerJob: Job? = null
    private val wrongWordIds = linkedSetOf<String>()
    private val wrongWords = mutableListOf<VocabularyQuizWrongWord>()
    private val correctDifficulties = mutableListOf<String>()

    fun initialize(sessionId: String = UUID.randomUUID().toString()) {
        if (session?.sessionId == sessionId && _uiState.value.stage != VocabularyQuizStage.Error) return
        loadSession(sessionId)
    }

    fun onBackClick() {
        if (_uiState.value.stage == VocabularyQuizStage.Completed) {
            finishSession(isCompleted = true)
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
            if (state.stage != VocabularyQuizStage.Ready) {
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
        if (state.stage != VocabularyQuizStage.Ready) return
        val selectedOptionId = state.selectedOptionId ?: return
        val selectedOption = question.options.firstOrNull { it.optionId == selectedOptionId } ?: return

        if (selectedOption.isCorrect) {
            correctDifficulties += question.difficultyLevel
            _uiState.update {
                it.copy(
                    stage = VocabularyQuizStage.AnswerEvaluated,
                    answerStatus = AnswerStatus.Correct,
                    feedbackMessage = "回答正确",
                    correctCount = it.correctCount + 1,
                    earnedTokens = it.earnedTokens + question.rewardToken,
                    canSubmitAnswer = false
                )
            }
        } else {
            wrongWordIds += question.wordId
            wrongWords += VocabularyQuizWrongWord(
                wordId = question.wordId,
                english = question.english,
                translation = question.translation
            )
            _uiState.update {
                it.copy(
                    stage = VocabularyQuizStage.AnswerEvaluated,
                    answerStatus = AnswerStatus.Wrong,
                    feedbackMessage = "正确答案：${question.translation}",
                    wrongCount = it.wrongCount + 1,
                    wrongWords = wrongWords.toList(),
                    canSubmitAnswer = false
                )
            }
        }
    }

    fun onNextQuestion() {
        if (_uiState.value.stage != VocabularyQuizStage.AnswerEvaluated) return
        val currentSession = session ?: return
        val nextIndex = _uiState.value.currentIndex + 1
        if (nextIndex >= currentSession.questions.size) {
            stopTimer()
            val state = _uiState.value
            val estimated = VocabularyEstimateCalculator.estimate(
                VocabularyEstimateCalculator.Input(
                    correctCount = state.correctCount,
                    totalCount = state.totalCount,
                    correctDifficulties = correctDifficulties.toList()
                )
            )
            _uiState.update {
                it.copy(
                    stage = VocabularyQuizStage.Completed,
                    currentQuestion = null,
                    selectedOptionId = null,
                    canSubmitAnswer = false,
                    estimatedVocabulary = estimated
                )
            }
            return
        }
        showQuestion(currentSession, nextIndex)
    }

    fun onRetryLoad() {
        session?.sessionId?.let(::loadSession)
    }

    fun onFinishSession() {
        finishSession(isCompleted = true)
    }

    private fun loadSession(sessionId: String) {
        resetSession()
        _uiState.value = VocabularyQuizUiState(stage = VocabularyQuizStage.Loading)
        viewModelScope.launch {
            runCatching {
                repository.createSession(sessionId = sessionId)
            }.onSuccess { loadedSession ->
                if (loadedSession.questions.isEmpty()) {
                    _uiState.value = VocabularyQuizUiState(
                        stage = VocabularyQuizStage.Error,
                        errorMessage = "没有可用的测验题目"
                    )
                } else {
                    session = loadedSession
                    _uiState.value = VocabularyQuizUiState(
                        stage = VocabularyQuizStage.Ready,
                        sessionId = loadedSession.sessionId,
                        totalCount = loadedSession.questions.size
                    )
                    startTimer()
                    showQuestion(loadedSession, questionIndex = 0)
                }
            }.onFailure { throwable ->
                _uiState.value = VocabularyQuizUiState(
                    stage = VocabularyQuizStage.Error,
                    errorMessage = throwable.message ?: "词汇测验加载失败"
                )
            }
        }
    }

    private fun showQuestion(session: VocabularyQuizSession, questionIndex: Int) {
        val question = session.questions[questionIndex]
        _uiState.update {
            it.copy(
                stage = VocabularyQuizStage.Ready,
                currentIndex = questionIndex,
                currentQuestion = question,
                selectedOptionId = null,
                canSubmitAnswer = false,
                answerStatus = AnswerStatus.Unanswered,
                feedbackMessage = ""
            )
        }
    }

    private fun finishSession(isCompleted: Boolean) {
        if (sessionFinished) return
        sessionFinished = true
        stopTimer()
        val state = _uiState.value
        val sessionId = state.sessionId ?: session?.sessionId ?: return
        val answeredCount = state.correctCount + state.wrongCount
        val result = StudyResult(
            sessionId = sessionId,
            moduleId = VocabularyQuizConstants.MODULE_ID,
            isCompleted = isCompleted,
            completedQuestionCount = answeredCount,
            correctCount = state.correctCount,
            wrongCount = state.wrongCount,
            skippedCount = 0,
            accuracy = if (answeredCount == 0) 0f else state.correctCount.toFloat() / answeredCount,
            earnedTokens = state.earnedTokens + if (isCompleted) {
                VocabularyQuizConstants.COMPLETION_BONUS
            } else {
                0
            },
            studyDurationSec = state.elapsedSeconds,
            vocabularyDelta = 0,
            wrongWordIds = wrongWordIds.toList(),
            estimatedVocabulary = if (isCompleted) state.estimatedVocabulary else null
        )
        viewModelScope.launch {
            _studyResults.emit(result)
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                _uiState.update { it.copy(elapsedSeconds = it.elapsedSeconds + 1) }
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    private fun resetSession() {
        stopTimer()
        session = null
        sessionFinished = false
        wrongWordIds.clear()
        wrongWords.clear()
        correctDifficulties.clear()
    }

    override fun onCleared() {
        stopTimer()
        super.onCleared()
    }
}
