package com.example.seedie.ui.screens.learning.listening

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.seedie.domain.model.StudyResult
import com.example.seedie.domain.repository.ListeningPracticeRepository
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
import com.example.seedie.ui.screens.learning.practice.AnswerStatus

@HiltViewModel
class ListeningPracticeViewModel @Inject constructor(
    private val repository: ListeningPracticeRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ListeningPracticeUiState())
    val uiState = _uiState.asStateFlow()

    private val _studyResults = MutableSharedFlow<StudyResult>()
    val studyResults = _studyResults.asSharedFlow()

    private val _playAudioEvents = MutableSharedFlow<PlayAudioEvent>()
    val playAudioEvents = _playAudioEvents.asSharedFlow()

    private var session: ListeningPracticeSession? = null
    private var sessionFinished = false
    private var timerJob: Job? = null
    private val wrongWordIds = linkedSetOf<String>()

    fun initialize(sessionId: String = UUID.randomUUID().toString()) {
        if (session?.sessionId == sessionId && _uiState.value.stage != ListeningPracticeStage.Error) return
        loadSession(sessionId)
    }

    fun onBackClick() {
        if (_uiState.value.stage == ListeningPracticeStage.Completed) {
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
            if (state.stage != ListeningPracticeStage.Ready) {
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
        if (state.stage != ListeningPracticeStage.Ready) return
        val selectedOptionId = state.selectedOptionId ?: return
        val selectedOption = question.options.firstOrNull { it.optionId == selectedOptionId } ?: return

        if (selectedOption.isCorrect) {
            _uiState.update {
                it.copy(
                    stage = ListeningPracticeStage.AnswerEvaluated,
                    answerStatus = AnswerStatus.Correct,
                    feedbackMessage = "回答正确",
                    correctCount = it.correctCount + 1,
                    earnedTokens = it.earnedTokens + question.rewardToken,
                    canSubmitAnswer = false
                )
            }
        } else {
            wrongWordIds += question.wordId
            _uiState.update {
                it.copy(
                    stage = ListeningPracticeStage.AnswerEvaluated,
                    answerStatus = AnswerStatus.Wrong,
                    feedbackMessage = "正确答案：${question.english}",
                    wrongCount = it.wrongCount + 1,
                    canSubmitAnswer = false
                )
            }
        }
    }

    fun onNextQuestion() {
        if (_uiState.value.stage != ListeningPracticeStage.AnswerEvaluated) return
        val currentSession = session ?: return
        val nextIndex = _uiState.value.currentIndex + 1
        if (nextIndex >= currentSession.questions.size) {
            stopTimer()
            _uiState.update {
                it.copy(
                    stage = ListeningPracticeStage.Completed,
                    currentQuestion = null,
                    selectedOptionId = null,
                    canSubmitAnswer = false
                )
            }
            return
        }
        showQuestion(currentSession, nextIndex)
    }

    fun onReplayAudio() {
        val question = _uiState.value.currentQuestion ?: return
        emitPlayEvent(question)
    }

    fun onRetryLoad() {
        session?.sessionId?.let(::loadSession)
    }

    fun onFinishSession() {
        finishSession(isCompleted = true)
    }

    private fun loadSession(sessionId: String) {
        resetSession()
        _uiState.value = ListeningPracticeUiState(stage = ListeningPracticeStage.Loading)
        viewModelScope.launch {
            runCatching {
                repository.createSession(sessionId = sessionId)
            }.onSuccess { loadedSession ->
                if (loadedSession.questions.isEmpty()) {
                    _uiState.value = ListeningPracticeUiState(
                        stage = ListeningPracticeStage.Error,
                        errorMessage = "没有可用的听力题目"
                    )
                } else {
                    session = loadedSession
                    _uiState.value = ListeningPracticeUiState(
                        stage = ListeningPracticeStage.Ready,
                        sessionId = loadedSession.sessionId,
                        totalCount = loadedSession.questions.size
                    )
                    startTimer()
                    showQuestion(loadedSession, questionIndex = 0)
                }
            }.onFailure { throwable ->
                _uiState.value = ListeningPracticeUiState(
                    stage = ListeningPracticeStage.Error,
                    errorMessage = throwable.message ?: "听力练习加载失败"
                )
            }
        }
    }

    private fun showQuestion(session: ListeningPracticeSession, questionIndex: Int) {
        val question = session.questions[questionIndex]
        _uiState.update {
            it.copy(
                stage = ListeningPracticeStage.Ready,
                currentIndex = questionIndex,
                currentQuestion = question,
                selectedOptionId = null,
                canSubmitAnswer = false,
                answerStatus = AnswerStatus.Unanswered,
                feedbackMessage = ""
            )
        }
        emitPlayEvent(question)
    }

    private fun emitPlayEvent(question: ListeningQuestion) {
        viewModelScope.launch {
            _playAudioEvents.emit(
                PlayAudioEvent(
                    rawResId = question.audioRawResId,
                    fallbackEnglish = question.english
                )
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
            moduleId = "listening",
            isCompleted = isCompleted,
            completedQuestionCount = answeredCount,
            correctCount = state.correctCount,
            wrongCount = state.wrongCount,
            skippedCount = 0,
            accuracy = if (answeredCount == 0) 0f else state.correctCount.toFloat() / answeredCount,
            earnedTokens = state.earnedTokens,
            studyDurationSec = state.elapsedSeconds,
            vocabularyDelta = 0,
            wrongWordIds = wrongWordIds.toList()
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
    }

    override fun onCleared() {
        stopTimer()
        super.onCleared()
    }
}

data class PlayAudioEvent(
    val rawResId: Int,
    val fallbackEnglish: String
)
