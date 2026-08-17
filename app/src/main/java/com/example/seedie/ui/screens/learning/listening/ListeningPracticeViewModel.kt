package com.example.seedie.ui.screens.learning.listening

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.seedie.domain.model.GardenSpeciesCatalog
import com.example.seedie.domain.model.PracticeAssignmentMode
import com.example.seedie.domain.model.StudyResult
import com.example.seedie.domain.repository.ListeningPracticeRepository
import com.example.seedie.domain.repository.PracticeAssignmentRepository
import com.example.seedie.domain.repository.PracticeCatalogRepository
import com.example.seedie.ui.screens.learning.assignments.PracticeAssignmentArgs
import com.example.seedie.ui.screens.learning.catalog.FreePracticeArgs
import com.example.seedie.ui.screens.learning.practice.AnswerStatus
import dagger.hilt.android.lifecycle.HiltViewModel
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
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.util.UUID

@HiltViewModel
class ListeningPracticeViewModel @Inject constructor(
    private val repository: ListeningPracticeRepository,
    private val assignmentRepository: PracticeAssignmentRepository,
    private val catalogRepository: PracticeCatalogRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ListeningPracticeUiState())
    val uiState = _uiState.asStateFlow()

    private val _studyResults = MutableSharedFlow<StudyResult>()
    val studyResults = _studyResults.asSharedFlow()

    private var selectedSpeciesId: String = GardenSpeciesCatalog.DEFAULT_SPECIES_ID
    private var sessionOpenedAtMillis: Long = 0L

    fun setSelectedSpeciesId(speciesId: String) {
        selectedSpeciesId = speciesId.ifBlank { GardenSpeciesCatalog.DEFAULT_SPECIES_ID }
    }

    private val _playAudioEvents = MutableSharedFlow<PlayAudioEvent>()
    val playAudioEvents = _playAudioEvents.asSharedFlow()

    private var session: ListeningPracticeSession? = null
    private var sessionFinished = false
    private var timerJob: Job? = null
    private val wrongQuestionIds = linkedSetOf<String>()
    private val allAnswers = linkedMapOf<String, String>()
    private var assignmentArgs: PracticeAssignmentArgs? = null
    private var freeArgs: FreePracticeArgs? = null
    private var isFreePractice = false
    private var isReviewMode = false
    private var submittedSuccessfully = false

    fun initializeAssignment(args: PracticeAssignmentArgs) {
        if (!isFreePractice &&
            assignmentArgs?.submissionId == args.submissionId &&
            assignmentArgs?.mode == args.mode &&
            _uiState.value.stage != ListeningPracticeStage.Error
        ) {
            return
        }
        freeArgs = null
        isFreePractice = false
        assignmentArgs = args
        isReviewMode = args.mode == PracticeAssignmentMode.Review
        loadAssignment(args)
    }

    fun initializeFree(args: FreePracticeArgs) {
        if (isFreePractice &&
            freeArgs?.itemRef == args.itemRef &&
            _uiState.value.stage != ListeningPracticeStage.Error
        ) {
            return
        }
        assignmentArgs = null
        isFreePractice = true
        isReviewMode = false
        freeArgs = args
        loadFree(args)
    }

    fun onBackClick() {
        if (_uiState.value.stage == ListeningPracticeStage.Completed) {
            finishSession(isCompleted = true, awardTokens = !isReviewMode && submittedSuccessfully)
            return
        }
        if (isReviewMode) {
            finishSession(isCompleted = true, awardTokens = false)
            return
        }
        _uiState.update { it.copy(showExitConfirmDialog = true) }
    }

    fun onConfirmExit() {
        _uiState.update { it.copy(showExitConfirmDialog = false) }
        finishSession(isCompleted = false, awardTokens = false)
    }

    fun onDismissExitDialog() {
        _uiState.update { it.copy(showExitConfirmDialog = false) }
    }

    fun onOptionSelected(optionId: String) {
        if (isReviewMode) return
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
        if (isReviewMode) return
        val state = _uiState.value
        val question = state.currentQuestion ?: return
        if (state.stage != ListeningPracticeStage.Ready) return
        val selectedOptionId = state.selectedOptionId ?: return
        val selectedOption = question.options.firstOrNull { it.optionId == selectedOptionId } ?: return
        allAnswers[question.questionId] = selectedOptionId

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
                if (isReviewMode) {
                    _uiState.update {
                        it.copy(
                            stage = ListeningPracticeStage.Completed,
                            currentQuestion = null,
                            selectedOptionId = null,
                            canSubmitAnswer = false,
                            currentQuestionOrdinal = it.totalQuestionCount
                        )
                    }
                } else if (isFreePractice) {
                    completeFreePractice()
                } else {
                    submitAssignmentAndComplete()
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
        when {
            isFreePractice -> freeArgs?.let(::loadFree)
            else -> assignmentArgs?.let(::loadAssignment)
        }
    }

    fun onFinishSession() {
        finishSession(isCompleted = true, awardTokens = !isReviewMode && submittedSuccessfully)
    }

    private fun loadFree(args: FreePracticeArgs) {
        resetSession()
        sessionOpenedAtMillis = System.currentTimeMillis()
        _uiState.value = ListeningPracticeUiState(
            stage = ListeningPracticeStage.Loading,
            sessionOpenedAtMillis = sessionOpenedAtMillis
        )
        viewModelScope.launch {
            runCatching {
                val attemptSessionId = "${args.sessionId}:${UUID.randomUUID()}"
                repository.createSession(
                    sessionId = attemptSessionId,
                    itemRefs = listOf(args.itemRef)
                )
            }.onSuccess { loadedSession ->
                if (loadedSession.materials.isEmpty() ||
                    loadedSession.materials.all { it.questions.isEmpty() }
                ) {
                    _uiState.value = ListeningPracticeUiState(
                        stage = ListeningPracticeStage.Error,
                        sessionOpenedAtMillis = sessionOpenedAtMillis,
                        errorMessage = "题目不存在或已下架"
                    )
                } else {
                    session = loadedSession
                    val totalQuestionCount = loadedSession.materials.sumOf { it.questions.size }
                    _uiState.value = ListeningPracticeUiState(
                        stage = ListeningPracticeStage.Ready,
                        sessionId = loadedSession.sessionId,
                        totalMaterialCount = loadedSession.materials.size,
                        totalQuestionCount = totalQuestionCount,
                        sessionOpenedAtMillis = sessionOpenedAtMillis,
                        isReviewMode = false
                    )
                    startTimer()
                    showQuestion(loadedSession, 0, 0)
                }
            }.onFailure { throwable ->
                _uiState.value = ListeningPracticeUiState(
                    stage = ListeningPracticeStage.Error,
                    sessionOpenedAtMillis = sessionOpenedAtMillis,
                    errorMessage = throwable.message?.takeIf { it.isNotBlank() }
                        ?: "听力练习加载失败"
                )
            }
        }
    }

    private fun loadAssignment(args: PracticeAssignmentArgs) {
        resetSession()
        sessionOpenedAtMillis = System.currentTimeMillis()
        _uiState.value = ListeningPracticeUiState(
            stage = ListeningPracticeStage.Loading,
            sessionOpenedAtMillis = sessionOpenedAtMillis
        )
        viewModelScope.launch {
            runCatching {
                val detail = assignmentRepository.getDetail(args.submissionId)
                if (detail.moduleId != "listening") {
                    error("作业模块不匹配")
                }
                if (args.mode == PracticeAssignmentMode.Answer) {
                    if (detail.status == "submitted") {
                        error("作业已提交，请从已完成列表查看")
                    }
                    val now = System.currentTimeMillis()
                    if (now > detail.dueAtEpochMs && !detail.allowLate) {
                        error("作业已过截止时间，无法作答")
                    }
                    assignmentRepository.start(args.submissionId)
                } else if (detail.status != "submitted") {
                    error("作业尚未提交，无法回顾")
                }

                val payloadAnswers = detail.answerPayload
                    ?.get("answers")
                    ?.jsonObject
                    ?.mapValues { (_, value) -> value.jsonPrimitive.contentOrNull.orEmpty() }
                    ?.filterValues { it.isNotEmpty() }
                    .orEmpty()

                if (args.mode == PracticeAssignmentMode.Review) {
                    allAnswers.clear()
                    allAnswers.putAll(payloadAnswers)
                }

                val loadedSession = repository.createSession(
                    sessionId = args.submissionId,
                    itemRefs = detail.itemRefs
                )
                detail to loadedSession
            }.onSuccess { (detail, loadedSession) ->
                if (loadedSession.materials.isEmpty() ||
                    loadedSession.materials.all { it.questions.isEmpty() }
                ) {
                    _uiState.value = ListeningPracticeUiState(
                        stage = ListeningPracticeStage.Error,
                        sessionOpenedAtMillis = sessionOpenedAtMillis,
                        errorMessage = "作业题目为空"
                    )
                } else {
                    session = loadedSession
                    val totalQuestionCount = loadedSession.materials.sumOf { it.questions.size }
                    if (isReviewMode) {
                        var correct = 0
                        var wrong = 0
                        var tokens = 0
                        loadedSession.materials.forEach { material ->
                            material.questions.forEach { question ->
                                val selected = allAnswers[question.questionId]
                                if (selected == null) return@forEach
                                if (selected == question.correctOptionId) {
                                    correct += 1
                                    tokens += question.rewardToken
                                } else {
                                    wrong += 1
                                    wrongQuestionIds += question.questionId
                                }
                            }
                        }
                        _uiState.value = ListeningPracticeUiState(
                            stage = ListeningPracticeStage.AnswerEvaluated,
                            sessionId = loadedSession.sessionId,
                            totalMaterialCount = loadedSession.materials.size,
                            totalQuestionCount = totalQuestionCount,
                            correctCount = detail.correctCount.takeIf { it > 0 } ?: correct,
                            wrongCount = wrong,
                            earnedTokens = detail.earnedTokens.takeIf { it > 0 } ?: tokens,
                            sessionOpenedAtMillis = sessionOpenedAtMillis,
                            isReviewMode = true
                        )
                        showQuestion(loadedSession, 0, 0, forceReview = true)
                    } else {
                        _uiState.value = ListeningPracticeUiState(
                            stage = ListeningPracticeStage.Ready,
                            sessionId = loadedSession.sessionId,
                            totalMaterialCount = loadedSession.materials.size,
                            totalQuestionCount = totalQuestionCount,
                            sessionOpenedAtMillis = sessionOpenedAtMillis,
                            isReviewMode = false
                        )
                        startTimer()
                        showQuestion(loadedSession, 0, 0)
                    }
                }
            }.onFailure { throwable ->
                _uiState.value = ListeningPracticeUiState(
                    stage = ListeningPracticeStage.Error,
                    sessionOpenedAtMillis = sessionOpenedAtMillis,
                    errorMessage = throwable.message?.takeIf { it.isNotBlank() }
                        ?: "听力作业加载失败"
                )
            }
        }
    }

    private fun completeFreePractice() {
        val args = freeArgs ?: return
        _uiState.update { it.copy(stage = ListeningPracticeStage.Loading) }
        viewModelScope.launch {
            runCatching {
                catalogRepository.markCompleted(args.moduleId, listOf(args.itemRef))
            }.onSuccess {
                submittedSuccessfully = true
                _uiState.update {
                    it.copy(
                        stage = ListeningPracticeStage.Completed,
                        currentQuestion = null,
                        selectedOptionId = null,
                        canSubmitAnswer = false,
                        currentQuestionOrdinal = it.totalQuestionCount
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        stage = ListeningPracticeStage.Error,
                        errorMessage = error.message ?: "保存进度失败"
                    )
                }
            }
        }
    }

    private fun submitAssignmentAndComplete() {
        val args = assignmentArgs ?: return
        val state = _uiState.value
        _uiState.update { it.copy(stage = ListeningPracticeStage.Loading) }
        viewModelScope.launch {
            runCatching {
                val payload = buildJsonObject {
                    put(
                        "answers",
                        buildJsonObject {
                            allAnswers.forEach { (qid, oid) -> put(qid, oid) }
                        }
                    )
                }
                val total = state.correctCount + state.wrongCount
                assignmentRepository.submit(
                    submissionId = args.submissionId,
                    correctCount = state.correctCount,
                    totalCount = total,
                    earnedTokens = 0,
                    answerPayload = payload
                )
            }.onSuccess {
                submittedSuccessfully = true
                _uiState.update {
                    it.copy(
                        stage = ListeningPracticeStage.Completed,
                        currentQuestion = null,
                        selectedOptionId = null,
                        canSubmitAnswer = false,
                        currentQuestionOrdinal = it.totalQuestionCount
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        stage = ListeningPracticeStage.Error,
                        errorMessage = error.message ?: "提交失败"
                    )
                }
            }
        }
    }

    private fun showQuestion(
        session: ListeningPracticeSession,
        materialIndex: Int,
        questionIndex: Int,
        forceReview: Boolean = false
    ) {
        val material = session.materials[materialIndex]
        val question = material.questions[questionIndex]
        val questionOrdinal = session.materials
            .take(materialIndex)
            .sumOf { it.questions.size } + questionIndex + 1

        if (isReviewMode || forceReview) {
            val selected = allAnswers[question.questionId]
            val isCorrect = selected == question.correctOptionId
            val correctAnswer = question.options
                .firstOrNull { it.optionId == question.correctOptionId }
                ?.label
                ?: question.correctOptionId
            _uiState.update {
                it.copy(
                    stage = ListeningPracticeStage.AnswerEvaluated,
                    currentMaterialIndex = materialIndex,
                    totalMaterialCount = session.materials.size,
                    currentMaterial = material,
                    currentQuestionIndexInMaterial = questionIndex,
                    currentQuestionCountInMaterial = material.questions.size,
                    currentQuestionOrdinal = questionOrdinal,
                    totalQuestionCount = session.materials.sumOf { m -> m.questions.size },
                    currentQuestion = question,
                    selectedOptionId = selected,
                    canSubmitAnswer = false,
                    answerStatus = when {
                        selected == null -> AnswerStatus.Unanswered
                        isCorrect -> AnswerStatus.Correct
                        else -> AnswerStatus.Wrong
                    },
                    feedbackMessage = when {
                        selected == null -> "未作答"
                        isCorrect -> "回答正确"
                        else -> "正确答案：$correctAnswer"
                    },
                    isReviewMode = true
                )
            }
            emitPlayEvent(material, question)
        } else {
            _uiState.update {
                it.copy(
                    stage = ListeningPracticeStage.Ready,
                    currentMaterialIndex = materialIndex,
                    totalMaterialCount = session.materials.size,
                    currentMaterial = material,
                    currentQuestionIndexInMaterial = questionIndex,
                    currentQuestionCountInMaterial = material.questions.size,
                    currentQuestionOrdinal = questionOrdinal,
                    totalQuestionCount = session.materials.sumOf { m -> m.questions.size },
                    currentQuestion = question,
                    selectedOptionId = null,
                    canSubmitAnswer = false,
                    answerStatus = AnswerStatus.Unanswered,
                    feedbackMessage = ""
                )
            }
            emitPlayEvent(material, question)
        }
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

    private fun finishSession(isCompleted: Boolean, awardTokens: Boolean) {
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
            earnedTokens = 0,
            studyDurationSec = state.elapsedSeconds,
            vocabularyDelta = 0,
            wrongWordIds = wrongQuestionIds.toList(),
            selectedSpeciesId = selectedSpeciesId,
            sessionOpenedAtMillis = sessionOpenedAtMillis
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
        submittedSuccessfully = false
        sessionOpenedAtMillis = 0L
        wrongQuestionIds.clear()
        allAnswers.clear()
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
