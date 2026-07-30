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
    private val wrongQuestionIds = linkedSetOf<String>()
    private var requestedSessionId: String? = null

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
            val correctAnswer = question.options
                .firstOrNull { option -> option.optionId == question.correctOptionId }
                ?.label
                ?: question.correctOptionId
            wrongQuestionIds += question.questionId
            _uiState.update {
                it.copy(
                    stage = ListeningPracticeStage.AnswerEvaluated,
                    answerStatus = AnswerStatus.Wrong,
                    feedbackMessage = "正确答案：$correctAnswer",
                    wrongCount = it.wrongCount + 1,
                    canSubmitAnswer = false
                )
            }
        }
    }

    fun onNextQuestion() {
        if (_uiState.value.stage != ListeningPracticeStage.AnswerEvaluated) return
        val currentSession = session ?: return
        val nextIndex = _uiState.value.currentQuestionIndexInMaterial + 1
        val currentMaterial = _uiState.value.currentMaterial ?: return
        if (nextIndex >= currentMaterial.questions.size) {
            val nextMaterialIndex = _uiState.value.currentMaterialIndex + 1
            if (nextMaterialIndex >= currentSession.materials.size) {
                stopTimer()
                _uiState.update {
                    it.copy(
                        stage = ListeningPracticeStage.Completed,
                        currentQuestion = null,
                        selectedOptionId = null,
                        canSubmitAnswer = false,
                        currentQuestionOrdinal = it.totalQuestionCount
                    )
                }
            } else {
                showQuestion(
                    session = currentSession,
                    materialIndex = nextMaterialIndex,
                    questionIndex = 0
                )
            }
            return
        }
        showQuestion(
            session = currentSession,
            materialIndex = _uiState.value.currentMaterialIndex,
            questionIndex = nextIndex
        )
    }

    fun onReplayAudio() {
        val material = _uiState.value.currentMaterial ?: return
        val question = _uiState.value.currentQuestion ?: return
        emitPlayEvent(material, question)
    }

    fun onRetryLoad() {
        requestedSessionId?.let(::loadSession)
    }

    fun onFinishSession() {
        finishSession(isCompleted = true)
    }

    private fun loadSession(sessionId: String) {
        resetSession()
        requestedSessionId = sessionId
        _uiState.value = ListeningPracticeUiState(stage = ListeningPracticeStage.Loading)
        viewModelScope.launch {
            runCatching {
                repository.createSession(sessionId = sessionId)
            }.onSuccess { loadedSession ->
                if (loadedSession.materials.isEmpty() || loadedSession.materials.all { it.questions.isEmpty() }) {
                    _uiState.value = ListeningPracticeUiState(
                        stage = ListeningPracticeStage.Error,
                        errorMessage = "暂无可用的短文或对话听力材料"
                    )
                } else {
                    val firstMaterial = loadedSession.materials.first()
                    val totalQuestionCount = loadedSession.materials.sumOf { it.questions.size }
                    session = loadedSession
                    _uiState.value = ListeningPracticeUiState(
                        stage = ListeningPracticeStage.Ready,
                        sessionId = loadedSession.sessionId,
                        currentMaterialIndex = 0,
                        totalMaterialCount = loadedSession.materials.size,
                        currentMaterial = firstMaterial,
                        currentQuestionCountInMaterial = firstMaterial.questions.size,
                        totalQuestionCount = totalQuestionCount
                    )
                    startTimer()
                    showQuestion(
                        session = loadedSession,
                        materialIndex = 0,
                        questionIndex = 0
                    )
                }
            }.onFailure { throwable ->
                _uiState.value = ListeningPracticeUiState(
                    stage = ListeningPracticeStage.Error,
                    errorMessage = throwable.message?.takeIf { it.isNotBlank() }
                        ?: "听力内容加载失败，请检查网络后重试"
                )
            }
        }
    }

    private fun showQuestion(
        session: ListeningPracticeSession,
        materialIndex: Int,
        questionIndex: Int
    ) {
        val material = session.materials[materialIndex]
        val question = material.questions[questionIndex]
        val questionOrdinal = session.materials
            .take(materialIndex)
            .sumOf { it.questions.size } + questionIndex + 1
        _uiState.update {
            it.copy(
                stage = ListeningPracticeStage.Ready,
                currentMaterialIndex = materialIndex,
                totalMaterialCount = session.materials.size,
                currentMaterial = material,
                currentQuestionIndexInMaterial = questionIndex,
                currentQuestionCountInMaterial = material.questions.size,
                currentQuestionOrdinal = questionOrdinal,
                totalQuestionCount = session.materials.sumOf { currentMaterial -> currentMaterial.questions.size },
                currentQuestion = question,
                selectedOptionId = null,
                canSubmitAnswer = false,
                answerStatus = AnswerStatus.Unanswered,
                feedbackMessage = ""
            )
        }
        emitPlayEvent(material, question)
    }

    private fun emitPlayEvent(
        material: ListeningMaterialItem,
        question: ListeningQuestionItem
    ) {
        viewModelScope.launch {
            _playAudioEvents.emit(
                PlayAudioEvent(
                    audioUrl = material.audioUrl,
                    fallbackText = material.transcript
                        ?.takeIf { it.isNotBlank() }
                        ?: material.promptText
                        ?.takeIf { it.isNotBlank() }
                        ?: question.stem
                )
            )
        }
    }

    private fun finishSession(isCompleted: Boolean) {
        val state = _uiState.value
        val sessionId = state.sessionId ?: session?.sessionId ?: return
        if (sessionFinished) return
        sessionFinished = true
        stopTimer()
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
            wrongWordIds = wrongQuestionIds.toList()
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
        wrongQuestionIds.clear()
    }

    override fun onCleared() {
        stopTimer()
        super.onCleared()
    }
}

data class PlayAudioEvent(
    val audioUrl: String?,
    val fallbackText: String
)
