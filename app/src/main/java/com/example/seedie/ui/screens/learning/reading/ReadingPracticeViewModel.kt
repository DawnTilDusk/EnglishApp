package com.example.seedie.ui.screens.learning.reading

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.seedie.domain.model.StudyResult
import com.example.seedie.domain.reading.ReadingPracticeConstants
import com.example.seedie.domain.reading.ReadingPracticeScorer
import com.example.seedie.domain.repository.ReadingPracticeRepository
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
class ReadingPracticeViewModel @Inject constructor(
    private val repository: ReadingPracticeRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ReadingPracticeUiState())
    val uiState = _uiState.asStateFlow()

    private val _studyResults = MutableSharedFlow<StudyResult>()
    val studyResults = _studyResults.asSharedFlow()

    private var session: ReadingPracticeSession? = null
    private var sessionFinished = false
    private var timerJob: Job? = null
    private var completionBonusApplied = false

    fun initialize(sessionId: String = UUID.randomUUID().toString()) {
        if (session?.sessionId == sessionId && _uiState.value.stage != ReadingPracticeStage.Error) return
        loadSession(sessionId)
    }

    fun onBackClick() {
        if (_uiState.value.stage == ReadingPracticeStage.Completed) {
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

    fun onOptionSelected(questionId: String, optionId: String) {
        _uiState.update { state ->
            if (state.stage != ReadingPracticeStage.Answering) {
                state
            } else {
                val currentSet = state.currentSet ?: return@update state
                val nextAnswers = state.answers + (questionId to optionId)
                val allAnswered = currentSet.questions.all { nextAnswers.containsKey(it.questionId) }
                state.copy(
                    answers = nextAnswers,
                    canSubmitSet = allAnswered,
                    submitHint = if (allAnswered) "" else "请答完所有题目"
                )
            }
        }
    }

    fun onSubmitSet() {
        val state = _uiState.value
        val currentSet = state.currentSet ?: return
        if (state.stage != ReadingPracticeStage.Answering) return
        if (!currentSet.questions.all { state.answers.containsKey(it.questionId) }) {
            _uiState.update { it.copy(submitHint = "请答完所有题目", canSubmitSet = false) }
            return
        }
        val score = ReadingPracticeScorer.scoreSet(currentSet, state.answers)
        _uiState.update {
            it.copy(
                stage = ReadingPracticeStage.Reviewing,
                correctCount = it.correctCount + score.correctCount,
                wrongCount = it.wrongCount + score.wrongCount,
                earnedTokens = it.earnedTokens + score.earnedTokens,
                canSubmitSet = false,
                submitHint = ""
            )
        }
    }

    fun onNextSet() {
        if (_uiState.value.stage != ReadingPracticeStage.Reviewing) return
        val currentSession = session ?: return
        val nextIndex = _uiState.value.currentSetIndex + 1
        if (nextIndex >= currentSession.sets.size) {
            stopTimer()
            if (!completionBonusApplied) {
                completionBonusApplied = true
                _uiState.update {
                    it.copy(earnedTokens = it.earnedTokens + ReadingPracticeConstants.COMPLETION_BONUS)
                }
            }
            _uiState.update {
                it.copy(
                    stage = ReadingPracticeStage.Completed,
                    currentSet = null,
                    answers = emptyMap(),
                    canSubmitSet = false
                )
            }
            return
        }
        showSet(currentSession, nextIndex)
    }

    fun onRetryLoad() {
        session?.sessionId?.let(::loadSession) ?: initialize()
    }

    fun onFinishSession() {
        finishSession(isCompleted = true)
    }

    private fun loadSession(sessionId: String) {
        resetSession()
        _uiState.value = ReadingPracticeUiState(stage = ReadingPracticeStage.Loading)
        viewModelScope.launch {
            runCatching {
                repository.createSession(sessionId = sessionId)
            }.onSuccess { loadedSession ->
                if (loadedSession.sets.isEmpty()) {
                    _uiState.value = ReadingPracticeUiState(
                        stage = ReadingPracticeStage.Error,
                        errorMessage = "阅读题库为空"
                    )
                } else {
                    session = loadedSession
                    _uiState.value = ReadingPracticeUiState(
                        stage = ReadingPracticeStage.Answering,
                        sessionId = loadedSession.sessionId,
                        totalSetCount = loadedSession.sets.size
                    )
                    startTimer()
                    showSet(loadedSession, setIndex = 0)
                }
            }.onFailure { throwable ->
                _uiState.value = ReadingPracticeUiState(
                    stage = ReadingPracticeStage.Error,
                    errorMessage = throwable.message ?: "阅读练习加载失败"
                )
            }
        }
    }

    private fun showSet(session: ReadingPracticeSession, setIndex: Int) {
        val set = session.sets[setIndex]
        _uiState.update {
            it.copy(
                stage = ReadingPracticeStage.Answering,
                currentSetIndex = setIndex,
                totalSetCount = session.sets.size,
                currentSet = set,
                answers = emptyMap(),
                canSubmitSet = false,
                submitHint = ""
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
            moduleId = ReadingPracticeConstants.MODULE_ID,
            isCompleted = isCompleted,
            completedQuestionCount = answeredCount,
            correctCount = state.correctCount,
            wrongCount = state.wrongCount,
            skippedCount = 0,
            accuracy = if (answeredCount == 0) 0f else state.correctCount.toFloat() / answeredCount,
            earnedTokens = state.earnedTokens,
            studyDurationSec = state.elapsedSeconds,
            vocabularyDelta = 0,
            wrongWordIds = emptyList()
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
        completionBonusApplied = false
    }

    override fun onCleared() {
        stopTimer()
        super.onCleared()
    }
}
