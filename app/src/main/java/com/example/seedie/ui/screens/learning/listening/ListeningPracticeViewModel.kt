package com.example.seedie.ui.screens.learning.listening

import com.example.seedie.data.repository.PracticeDraftStore
import com.example.seedie.data.repository.StudySettlementManager
import com.example.seedie.data.repository.ListeningCheckpoint
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.coroutines.CancellationException
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
    private val catalogRepository: PracticeCatalogRepository,
    private val drafts: PracticeDraftStore,
    private val settlements: StudySettlementManager
) : ViewModel() {
    private val ownerUserId = settlements.currentUserId()
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
    private val draftAnswers = linkedMapOf<String, String>()
    private var assignmentArgs: PracticeAssignmentArgs? = null
    private var freeArgs: FreePracticeArgs? = null
    private var isFreePractice = false
    private var isReviewMode = false
    private var submittedSuccessfully = false

    private val checkpointJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private var submissionPending = false
    private var completedAt = 0L
    var playbackSnapshot: com.example.seedie.domain.model.ListeningPlaybackSnapshot? = null
        private set
    fun savePlayback(snapshot: com.example.seedie.domain.model.ListeningPlaybackSnapshot?) {
        if (snapshot == null) return
        playbackSnapshot = snapshot
        saveCheckpoint()
    }
    private val draftKey: String get() = "listening:" + (assignmentArgs?.submissionId ?: freeArgs?.itemRef.orEmpty())

    init {
        viewModelScope.launch {
            _uiState.collect { state ->
                if (state.stage !in setOf(ListeningPracticeStage.Loading, ListeningPracticeStage.Error)) saveCheckpoint()
            }
        }
    }

    private fun saveCheckpoint() {
        if (isReviewMode || sessionFinished || drafts.userId() != ownerUserId) return
        val current = session ?: return
        val state = _uiState.value
        if (state.stage in setOf(ListeningPracticeStage.Loading, ListeningPracticeStage.Error)) return
        drafts.save(draftKey, checkpointJson.encodeToString(ListeningCheckpoint(
            current, state.copy(showExitConfirmDialog = false), allAnswers.toMap(),
            draftAnswers.toMap(), wrongQuestionIds.toSet(),
            selectedSpeciesId, submissionPending, submittedSuccessfully, completedAt, playbackSnapshot
        )))
    }

    private suspend fun restoreCheckpoint(): Boolean {
        if (isReviewMode || drafts.userId() != ownerUserId) return false
        val text = drafts.load(draftKey) ?: return false
        val saved = runCatching { checkpointJson.decodeFromString<ListeningCheckpoint>(text) }.getOrNull() ?: return false
        session = saved.session
        allAnswers.clear(); allAnswers.putAll(saved.answers)
        draftAnswers.clear(); draftAnswers.putAll(saved.drafts)
        wrongQuestionIds.clear(); wrongQuestionIds.addAll(saved.wrongIds)
        selectedSpeciesId = saved.speciesId
        submissionPending = saved.submissionPending
        submittedSuccessfully = saved.submitted
        completedAt = saved.completedAt
        playbackSnapshot = saved.playback
        sessionOpenedAtMillis = saved.state.sessionOpenedAtMillis
        _uiState.value = saved.state.copy(showExitConfirmDialog = false, errorMessage = null)
        if (saved.state.stage != ListeningPracticeStage.Completed && !submissionPending) startTimer()
        return true
    }

    private suspend fun recordCompletion() {
        if (isReviewMode) return
        if (completedAt == 0L) completedAt = System.currentTimeMillis()
        settlements.enqueue(buildResult(isCompleted = true, awardTokens = true), ownerUserId)
    }

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
            finishSession(isCompleted = true, awardTokens = submittedSuccessfully)
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
        if (_uiState.value.stage != ListeningPracticeStage.Ready) return
        val question = _uiState.value.currentQuestion ?: return
        draftAnswers[question.questionId] = optionId
        if (isFreePractice) {
            allAnswers[question.questionId] = optionId
        }
        _uiState.update { state ->
            if (state.stage != ListeningPracticeStage.Ready) {
                state
            } else {
                state.copy(
                    selectedOptionId = optionId,
                    selectedAnswers = draftAnswers.toMap(),
                    canSubmitAnswer = !isFreePractice,
                    answeredQuestionCount = allAnswers.size
                )
            }
        }
    }

    fun onSubmitAnswer() {
        if (isReviewMode || isFreePractice) return
        val state = _uiState.value
        val question = state.currentQuestion ?: return
        if (state.stage != ListeningPracticeStage.Ready) return
        val selectedOptionId = state.selectedOptionId ?: return
        val selectedOption = question.options.firstOrNull { it.optionId == selectedOptionId } ?: return
        allAnswers[question.questionId] = selectedOptionId

        if (selectedOption.isCorrect) {
            val dewDelta = if (question.rewardToken >= 2) 2 else 1
            _uiState.update {
                it.copy(
                    stage = ListeningPracticeStage.AnswerEvaluated,
                    evaluatedQuestionIds = allAnswers.keys.toSet(),
                    answerStatus = AnswerStatus.Correct,
                    feedbackMessage = "回答正确",
                    correctCount = it.correctCount + 1,
                    earnedTokens = it.earnedTokens + question.rewardToken,
                    earnedDews = it.earnedDews + dewDelta,
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
                    evaluatedQuestionIds = allAnswers.keys.toSet(),
                    answerStatus = AnswerStatus.Wrong,
                    feedbackMessage = "正确答案：$correctAnswer",
                    wrongCount = it.wrongCount + 1,
                    canSubmitAnswer = false
                )
            }
        }
    }

    fun onNextQuestion() {
        if (isFreePractice) {
            moveToNextFreePracticeQuestion()
            return
        }
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
                    questionIndex = 0,
                    autoPlayMaterial = true
                )
            }
            return
        }
        showQuestion(
            session = currentSession,
            materialIndex = _uiState.value.currentMaterialIndex,
            questionIndex = nextIndex,
            autoPlayMaterial = false
        )
    }

    fun onQuestionPageSelected(page: Int) {
        if (!canNavigateFreePracticeQuestions()) return
        val currentSession = session ?: return
        val questions = currentSession.materials.flatMap { it.questions }
        if (page !in questions.indices) return
        if (!isFreePractice && !isReviewMode) {
            val firstUnanswered = questions.indexOfFirst { it.questionId !in allAnswers }
            if (firstUnanswered >= 0 && page > firstUnanswered) return
        }
        if (page == _uiState.value.currentQuestionOrdinal - 1) return
        var remaining = page
        currentSession.materials.forEachIndexed { materialIndex, material ->
            if (remaining < material.questions.size) {
                showQuestion(currentSession, materialIndex, remaining,
                    autoPlayMaterial = materialIndex != _uiState.value.currentMaterialIndex)
                return
            }
            remaining -= material.questions.size
        }
    }

    fun onPreviousQuestion() {
        if (!canNavigateFreePracticeQuestions()) return
        val currentSession = session ?: return
        val state = _uiState.value
        val previousMaterialIndex: Int
        val previousQuestionIndex: Int

        if (state.currentQuestionIndexInMaterial > 0) {
            previousMaterialIndex = state.currentMaterialIndex
            previousQuestionIndex = state.currentQuestionIndexInMaterial - 1
        } else {
            if (state.currentMaterialIndex == 0) return
            previousMaterialIndex = state.currentMaterialIndex - 1
            previousQuestionIndex = currentSession.materials[previousMaterialIndex].questions.lastIndex
        }

        showQuestion(
            session = currentSession,
            materialIndex = previousMaterialIndex,
            questionIndex = previousQuestionIndex,
            autoPlayMaterial = previousMaterialIndex != state.currentMaterialIndex
        )
    }

    fun onSubmitFreePractice() {
        if (!isFreePractice || _uiState.value.stage != ListeningPracticeStage.Ready) return
        val currentSession = session ?: return
        val state = _uiState.value
        if (state.answeredQuestionCount < state.totalQuestionCount) return

        var correctCount = 0
        var wrongCount = 0
        var earnedTokens = 0
        var earnedDews = 0
        wrongQuestionIds.clear()
        currentSession.materials.forEach { material ->
            material.questions.forEach { question ->
                val answer = allAnswers[question.questionId] ?: return@forEach
                if (answer == question.correctOptionId) {
                    correctCount += 1
                    earnedTokens += question.rewardToken
                    earnedDews += if (question.rewardToken >= 2) 2 else 1
                } else {
                    wrongCount += 1
                    wrongQuestionIds += question.questionId
                }
            }
        }
        _uiState.update {
            it.copy(
                correctCount = correctCount,
                wrongCount = wrongCount,
                earnedTokens = earnedTokens,
                earnedDews = earnedDews
            )
        }
        stopTimer()
        completeFreePractice()
    }

    fun onReviewAnswers() {
        if (_uiState.value.stage != ListeningPracticeStage.Completed) return
        val currentSession = session ?: return
        isReviewMode = true
        showQuestion(
            session = currentSession,
            materialIndex = 0,
            questionIndex = 0,
            forceReview = true
        )
    }

    fun onReplayAudio() {
        val material = _uiState.value.currentMaterial ?: return
        val question = _uiState.value.currentQuestion ?: return
        emitPlayEvent(material, question)
    }

    fun onRetryLoad() {
        if (submissionPending) {
            if (isFreePractice) completeFreePractice() else submitAssignmentAndComplete()
            return
        }
        when {
            isFreePractice -> freeArgs?.let(::loadFree)
            else -> assignmentArgs?.let(::loadAssignment)
        }
    }

    fun onFinishSession() {
        finishSession(isCompleted = true, awardTokens = submittedSuccessfully)
    }

    private fun loadFree(args: FreePracticeArgs) {
        resetSession()
        sessionOpenedAtMillis = System.currentTimeMillis()
        _uiState.value = ListeningPracticeUiState(
            stage = ListeningPracticeStage.Loading,
            sessionOpenedAtMillis = sessionOpenedAtMillis
        )
        viewModelScope.launch {
            if (restoreCheckpoint()) {
                if (submissionPending) completeFreePractice()
                return@launch
            }
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
                        isReviewMode = false,
                        isFreePractice = true
                    )
                    startTimer()
                    showQuestion(loadedSession, 0, 0, autoPlayMaterial = true)
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
                        if (restoreCheckpoint()) {
                            submittedSuccessfully = true
                            submissionPending = false
                            completedAt = detail.submittedAtEpochMs ?: completedAt
                            recordCompletion()
                            _uiState.update { it.copy(stage = ListeningPracticeStage.Completed) }
                            return@launch
                        }
                        isReviewMode = true
                    }
                    val now = System.currentTimeMillis()
                    if (now > detail.dueAtEpochMs && !detail.allowLate) {
                        error("作业已过截止时间，无法作答")
                    }
                    assignmentRepository.start(args.submissionId)
                } else if (detail.status != "submitted") {
                    error("作业尚未提交，无法回顾")
                }

                if (!isReviewMode && restoreCheckpoint()) {
                    if (submissionPending) submitAssignmentAndComplete()
                    return@launch
                }

                val payloadAnswers = detail.answerPayload
                    ?.get("answers")
                    ?.jsonObject
                    ?.mapValues { (_, value) -> value.jsonPrimitive.contentOrNull.orEmpty() }
                    ?.filterValues { it.isNotEmpty() }
                    .orEmpty()

                if (isReviewMode) {
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
                        showQuestion(
                            loadedSession,
                            0,
                            0,
                            forceReview = true,
                            autoPlayMaterial = true
                        )
                    } else {
                        _uiState.value = ListeningPracticeUiState(
                            stage = ListeningPracticeStage.Ready,
                            sessionId = loadedSession.sessionId,
                            totalMaterialCount = loadedSession.materials.size,
                            totalQuestionCount = totalQuestionCount,
                            sessionOpenedAtMillis = sessionOpenedAtMillis,
                            isReviewMode = false,
                            isFreePractice = false
                        )
                        startTimer()
                        showQuestion(loadedSession, 0, 0, autoPlayMaterial = true)
                    }
                }
            }.onFailure { throwable ->
                if (throwable is CancellationException) throw throwable
                if ((throwable is java.io.IOException || throwable is java.nio.channels.UnresolvedAddressException) && restoreCheckpoint()) return@launch
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
        if (submissionPending && _uiState.value.stage == ListeningPracticeStage.Loading) return
        submissionPending = true
        saveCheckpoint()
        val args = freeArgs ?: return
        _uiState.update { it.copy(stage = ListeningPracticeStage.Loading) }
        viewModelScope.launch {
            runCatching {
                drafts.flush()
                catalogRepository.markCompleted(args.moduleId, listOf(args.itemRef))
            }.onSuccess {
                submittedSuccessfully = true
                submissionPending = false
                recordCompletion()
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
                if (error is CancellationException) throw error
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
        if (submissionPending && _uiState.value.stage == ListeningPracticeStage.Loading) return
        submissionPending = true
        saveCheckpoint()
        val args = assignmentArgs ?: return
        val state = _uiState.value
        _uiState.update { it.copy(stage = ListeningPracticeStage.Loading) }
        viewModelScope.launch {
            runCatching {
                drafts.flush()
                val existing = assignmentRepository.getDetail(args.submissionId)
                if (existing.status == "submitted") {
                    completedAt = existing.submittedAtEpochMs ?: completedAt
                    return@runCatching
                }
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
                submissionPending = false
                recordCompletion()
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
                if (error is CancellationException) throw error
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
        forceReview: Boolean = false,
        autoPlayMaterial: Boolean = false
    ) {
        val material = session.materials[materialIndex]
        val question = material.questions[questionIndex]
        val questionOrdinal = session.materials
            .take(materialIndex)
            .sumOf { it.questions.size } + questionIndex + 1
        _uiState.update {
            it.copy(
                materials = session.materials,
                selectedAnswers = draftAnswers + allAnswers,
                evaluatedQuestionIds = if (isFreePractice && !isReviewMode) emptySet() else allAnswers.keys.toSet()
            )
        }

        if (isReviewMode || forceReview || (!isFreePractice && question.questionId in allAnswers)) {
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
                    isReviewMode = isReviewMode || forceReview
                )
            }
            if (autoPlayMaterial) {
                emitPlayEvent(material, question)
            }
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
                    selectedOptionId = if (isFreePractice) allAnswers[question.questionId] else draftAnswers[question.questionId],
                    canSubmitAnswer = !isFreePractice && question.questionId in draftAnswers,
                    answeredQuestionCount = allAnswers.size,
                    answerStatus = AnswerStatus.Unanswered,
                    feedbackMessage = ""
                )
            }
            if (autoPlayMaterial) {
                emitPlayEvent(material, question)
            }
        }
    }

    private fun moveToNextFreePracticeQuestion() {
        if (!canNavigateFreePracticeQuestions()) return
        val currentSession = session ?: return
        val state = _uiState.value
        val currentMaterial = state.currentMaterial ?: return
        val nextMaterialIndex: Int
        val nextQuestionIndex: Int

        if (state.currentQuestionIndexInMaterial < currentMaterial.questions.lastIndex) {
            nextMaterialIndex = state.currentMaterialIndex
            nextQuestionIndex = state.currentQuestionIndexInMaterial + 1
        } else {
            if (state.currentMaterialIndex >= currentSession.materials.lastIndex) return
            nextMaterialIndex = state.currentMaterialIndex + 1
            nextQuestionIndex = 0
        }

        showQuestion(
            session = currentSession,
            materialIndex = nextMaterialIndex,
            questionIndex = nextQuestionIndex,
            autoPlayMaterial = nextMaterialIndex != state.currentMaterialIndex
        )
    }

    private fun canNavigateFreePracticeQuestions(): Boolean {
        return _uiState.value.stage == ListeningPracticeStage.Ready ||
            _uiState.value.stage == ListeningPracticeStage.AnswerEvaluated
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

    private fun buildResult(isCompleted: Boolean, awardTokens: Boolean): StudyResult {
        val state = _uiState.value
        val sessionId = state.sessionId ?: requireNotNull(session).sessionId
        val answeredCount = state.correctCount + state.wrongCount
        return StudyResult(
            sessionId = sessionId,
            moduleId = "listening",
            isCompleted = isCompleted && awardTokens,
            completedQuestionCount = answeredCount,
            correctCount = state.correctCount,
            wrongCount = state.wrongCount,
            skippedCount = 0,
            accuracy = if (answeredCount == 0) 0f else state.correctCount.toFloat() / answeredCount,
            earnedTokens = 0,
            earnedDews = if (awardTokens) state.earnedDews else 0,
            studyDurationSec = state.elapsedSeconds,
            vocabularyDelta = 0,
            wrongWordIds = wrongQuestionIds.toList(),
            selectedSpeciesId = selectedSpeciesId,
            sessionOpenedAtMillis = sessionOpenedAtMillis,
            countsTowardTasks = awardTokens,
            completedAtMillis = completedAt.takeIf { it > 0 } ?: System.currentTimeMillis()
        )
    }

    private fun finishSession(isCompleted: Boolean, awardTokens: Boolean) {
        if (sessionFinished || session == null) return
        saveCheckpoint()
        sessionFinished = true
        stopTimer()
        val result = buildResult(isCompleted, awardTokens)
        viewModelScope.launch {
            drafts.flush()
            settlements.enqueue(result, ownerUserId)
            if (isCompleted && submittedSuccessfully) drafts.clear(draftKey)
            _studyResults.emit(result)
        }
    }

    private var screenActive = true
    fun onScreenActive(active: Boolean) {
        screenActive = active
        if (active && _uiState.value.stage !in setOf(ListeningPracticeStage.Completed, ListeningPracticeStage.Loading, ListeningPracticeStage.Error)) startTimer()
        else stopTimer()
    }

    private fun startTimer() {
        if (!screenActive) return
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
        submissionPending = false
        completedAt = 0L
        sessionOpenedAtMillis = 0L
        wrongQuestionIds.clear()
        allAnswers.clear()
        draftAnswers.clear()
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
