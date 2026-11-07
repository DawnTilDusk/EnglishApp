package com.example.seedie.ui.screens.learning.reading

import com.example.seedie.data.repository.PracticeDraftStore
import com.example.seedie.data.repository.StudySettlementManager
import com.example.seedie.data.repository.ReadingCheckpoint
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.coroutines.CancellationException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.seedie.domain.model.GardenSpeciesCatalog
import com.example.seedie.domain.model.PracticeAssignmentMode
import com.example.seedie.domain.model.StudyResult
import com.example.seedie.domain.reading.ReadingPracticeConstants
import com.example.seedie.domain.reading.ReadingPracticeScorer
import com.example.seedie.domain.repository.PracticeAssignmentRepository
import com.example.seedie.domain.repository.PracticeCatalogRepository
import com.example.seedie.domain.repository.ReadingPracticeRepository
import com.example.seedie.ui.screens.learning.assignments.PracticeAssignmentArgs
import com.example.seedie.ui.screens.learning.catalog.FreePracticeArgs
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
class ReadingPracticeViewModel @Inject constructor(
    private val repository: ReadingPracticeRepository,
    private val assignmentRepository: PracticeAssignmentRepository,
    private val catalogRepository: PracticeCatalogRepository,
    private val drafts: PracticeDraftStore,
    private val settlements: StudySettlementManager
) : ViewModel() {
    private val ownerUserId = settlements.currentUserId()
    private val _uiState = MutableStateFlow(ReadingPracticeUiState())
    val uiState = _uiState.asStateFlow()

    private val _studyResults = MutableSharedFlow<StudyResult>()
    val studyResults = _studyResults.asSharedFlow()

    private var selectedSpeciesId: String = GardenSpeciesCatalog.DEFAULT_SPECIES_ID
    private var sessionOpenedAtMillis: Long = 0L

    fun setSelectedSpeciesId(speciesId: String) {
        selectedSpeciesId = speciesId.ifBlank { GardenSpeciesCatalog.DEFAULT_SPECIES_ID }
    }

    private var session: ReadingPracticeSession? = null
    private var sessionFinished = false
    private var timerJob: Job? = null
    private var assignmentArgs: PracticeAssignmentArgs? = null
    private var freeArgs: FreePracticeArgs? = null
    private var isFreePractice = false
    private var isReviewMode = false
    private var submittedSuccessfully = false
    private val allAnswers = linkedMapOf<String, String>()

    private val checkpointJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private var submissionPending = false
    private var completedAt = 0L
    private val draftKey: String get() = "reading:" + (assignmentArgs?.submissionId ?: freeArgs?.itemRef.orEmpty())

    init {
        viewModelScope.launch {
            _uiState.collect { state ->
                if (state.stage !in setOf(ReadingPracticeStage.Loading, ReadingPracticeStage.Error)) saveCheckpoint()
            }
        }
    }

    private fun saveCheckpoint() {
        if (isReviewMode || sessionFinished || drafts.userId() != ownerUserId) return
        val current = session ?: return
        val state = _uiState.value
        if (state.stage in setOf(ReadingPracticeStage.Loading, ReadingPracticeStage.Error)) return
        drafts.save(draftKey, checkpointJson.encodeToString(ReadingCheckpoint(
            current, state.copy(showExitConfirmDialog = false), allAnswers.toMap(),
            selectedSpeciesId, submissionPending, submittedSuccessfully, completedAt
        )))
    }

    private suspend fun restoreCheckpoint(): Boolean {
        if (isReviewMode || drafts.userId() != ownerUserId) return false
        val text = drafts.load(draftKey) ?: return false
        val saved = runCatching { checkpointJson.decodeFromString<ReadingCheckpoint>(text) }.getOrNull() ?: return false
        session = saved.session
        allAnswers.clear(); allAnswers.putAll(saved.answers)
        selectedSpeciesId = saved.speciesId
        submissionPending = saved.submissionPending
        submittedSuccessfully = saved.submitted
        completedAt = saved.completedAt
        sessionOpenedAtMillis = saved.state.sessionOpenedAtMillis
        _uiState.value = saved.state.copy(showExitConfirmDialog = false, errorMessage = null)
        if (saved.state.stage != ReadingPracticeStage.Completed && !submissionPending) startTimer()
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
            _uiState.value.stage != ReadingPracticeStage.Error
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
            _uiState.value.stage != ReadingPracticeStage.Error
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
        if (_uiState.value.stage == ReadingPracticeStage.Completed) {
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

    fun onOptionSelected(questionId: String, optionId: String) {
        if (isReviewMode) return
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
        if (isReviewMode) return
        val state = _uiState.value
        val currentSet = state.currentSet ?: return
        if (state.stage != ReadingPracticeStage.Answering) return
        if (!currentSet.questions.all { state.answers.containsKey(it.questionId) }) {
            _uiState.update { it.copy(submitHint = "请答完所有题目", canSubmitSet = false) }
            return
        }
        allAnswers.putAll(state.answers)
        val score = ReadingPracticeScorer.scoreSet(currentSet, state.answers)
        _uiState.update {
            it.copy(
                stage = ReadingPracticeStage.Reviewing,
                correctCount = it.correctCount + score.correctCount,
                wrongCount = it.wrongCount + score.wrongCount,
                earnedTokens = it.earnedTokens + score.earnedTokens,
                earnedDews = it.earnedDews + score.earnedDews,
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
            if (isReviewMode) {
                _uiState.update {
                    it.copy(
                        stage = ReadingPracticeStage.Completed,
                        currentSet = null,
                        answers = emptyMap(),
                        canSubmitSet = false
                    )
                }
            } else if (isFreePractice) {
                completeFreePractice()
            } else {
                submitAssignmentAndComplete()
            }
            return
        }
        showSet(currentSession, nextIndex)
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
        _uiState.value = ReadingPracticeUiState(
            stage = ReadingPracticeStage.Loading,
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
                if (loadedSession.sets.isEmpty()) {
                    _uiState.value = ReadingPracticeUiState(
                        stage = ReadingPracticeStage.Error,
                        sessionOpenedAtMillis = sessionOpenedAtMillis,
                        errorMessage = "题目不存在或已下架"
                    )
                } else {
                    session = loadedSession
                    _uiState.value = ReadingPracticeUiState(
                        stage = ReadingPracticeStage.Answering,
                        sessionId = loadedSession.sessionId,
                        totalSetCount = loadedSession.sets.size,
                        sessionOpenedAtMillis = sessionOpenedAtMillis,
                        isReviewMode = false
                    )
                    startTimer()
                    showSet(loadedSession, setIndex = 0)
                }
            }.onFailure { throwable ->
                _uiState.value = ReadingPracticeUiState(
                    stage = ReadingPracticeStage.Error,
                    sessionOpenedAtMillis = sessionOpenedAtMillis,
                    errorMessage = throwable.message ?: "阅读练习加载失败"
                )
            }
        }
    }

    private fun loadAssignment(args: PracticeAssignmentArgs) {
        resetSession()
        sessionOpenedAtMillis = System.currentTimeMillis()
        _uiState.value = ReadingPracticeUiState(
            stage = ReadingPracticeStage.Loading,
            sessionOpenedAtMillis = sessionOpenedAtMillis
        )
        viewModelScope.launch {
            runCatching {
                val detail = assignmentRepository.getDetail(args.submissionId)
                if (detail.moduleId != "reading") {
                    error("作业模块不匹配")
                }
                if (args.mode == PracticeAssignmentMode.Answer) {
                    if (detail.status == "submitted") {
                        if (restoreCheckpoint()) {
                            submittedSuccessfully = true
                            submissionPending = false
                            completedAt = detail.submittedAtEpochMs ?: completedAt
                            recordCompletion()
                            _uiState.update { it.copy(stage = ReadingPracticeStage.Completed) }
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
                Triple(detail, loadedSession, payloadAnswers)
            }.onSuccess { (detail, loadedSession, payloadAnswers) ->
                if (loadedSession.sets.isEmpty()) {
                    _uiState.value = ReadingPracticeUiState(
                        stage = ReadingPracticeStage.Error,
                        sessionOpenedAtMillis = sessionOpenedAtMillis,
                        errorMessage = "作业题目为空"
                    )
                } else {
                    session = loadedSession
                    if (isReviewMode) {
                        var correct = 0
                        var wrong = 0
                        var tokens = 0
                        loadedSession.sets.forEach { set ->
                            val score = ReadingPracticeScorer.scoreSet(set, allAnswers)
                            correct += score.correctCount
                            wrong += score.wrongCount
                            tokens += score.earnedTokens
                        }
                        _uiState.value = ReadingPracticeUiState(
                            stage = ReadingPracticeStage.Reviewing,
                            sessionId = loadedSession.sessionId,
                            totalSetCount = loadedSession.sets.size,
                            correctCount = detail.correctCount.takeIf { it > 0 } ?: correct,
                            wrongCount = wrong,
                            earnedTokens = detail.earnedTokens.takeIf { it > 0 } ?: tokens,
                            sessionOpenedAtMillis = sessionOpenedAtMillis,
                            isReviewMode = true
                        )
                        showSet(loadedSession, setIndex = 0, forceReview = true)
                    } else {
                        _uiState.value = ReadingPracticeUiState(
                            stage = ReadingPracticeStage.Answering,
                            sessionId = loadedSession.sessionId,
                            totalSetCount = loadedSession.sets.size,
                            sessionOpenedAtMillis = sessionOpenedAtMillis,
                            isReviewMode = false
                        )
                        startTimer()
                        showSet(loadedSession, setIndex = 0)
                    }
                }
            }.onFailure { throwable ->
                if (throwable is CancellationException) throw throwable
                if ((throwable is java.io.IOException || throwable is java.nio.channels.UnresolvedAddressException) && restoreCheckpoint()) return@launch
                _uiState.value = ReadingPracticeUiState(
                    stage = ReadingPracticeStage.Error,
                    sessionOpenedAtMillis = sessionOpenedAtMillis,
                    errorMessage = throwable.message ?: "阅读作业加载失败"
                )
            }
        }
    }

    private fun completeFreePractice() {
        if (submissionPending && _uiState.value.stage == ReadingPracticeStage.Loading) return
        submissionPending = true
        saveCheckpoint()
        val args = freeArgs ?: return
        _uiState.update { it.copy(stage = ReadingPracticeStage.Loading, submitHint = "正在保存…") }
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
                        stage = ReadingPracticeStage.Completed,
                        currentSet = null,
                        answers = emptyMap(),
                        canSubmitSet = false,
                        submitHint = ""
                    )
                }
            }.onFailure { error ->
                if (error is CancellationException) throw error
                _uiState.update {
                    it.copy(
                        stage = ReadingPracticeStage.Error,
                        errorMessage = error.message ?: "保存进度失败"
                    )
                }
            }
        }
    }

    private fun submitAssignmentAndComplete() {
        if (submissionPending && _uiState.value.stage == ReadingPracticeStage.Loading) return
        submissionPending = true
        saveCheckpoint()
        val args = assignmentArgs ?: return
        val state = _uiState.value
        _uiState.update { it.copy(stage = ReadingPracticeStage.Loading, submitHint = "正在提交…") }
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
                            allAnswers.forEach { (qid, oid) ->
                                put(qid, oid)
                            }
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
                        stage = ReadingPracticeStage.Completed,
                        currentSet = null,
                        answers = emptyMap(),
                        canSubmitSet = false,
                        submitHint = ""
                    )
                }
            }.onFailure { error ->
                if (error is CancellationException) throw error
                _uiState.update {
                    it.copy(
                        stage = ReadingPracticeStage.Error,
                        errorMessage = error.message ?: "提交失败"
                    )
                }
            }
        }
    }

    private fun showSet(
        session: ReadingPracticeSession,
        setIndex: Int,
        forceReview: Boolean = false
    ) {
        val set = session.sets[setIndex]
        if (isReviewMode || forceReview) {
            val answersForSet = set.questions.associate { q ->
                q.questionId to (allAnswers[q.questionId] ?: "")
            }.filterValues { it.isNotEmpty() }
            _uiState.update {
                it.copy(
                    stage = ReadingPracticeStage.Reviewing,
                    currentSetIndex = setIndex,
                    totalSetCount = session.sets.size,
                    currentSet = set,
                    answers = answersForSet,
                    canSubmitSet = false,
                    submitHint = "",
                    isReviewMode = true
                )
            }
        } else {
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
    }

    private fun buildResult(isCompleted: Boolean, awardTokens: Boolean): StudyResult {
        val state = _uiState.value
        val sessionId = state.sessionId ?: requireNotNull(session).sessionId
        val answeredCount = state.correctCount + state.wrongCount
        return StudyResult(
            sessionId = sessionId,
            moduleId = ReadingPracticeConstants.MODULE_ID,
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
            wrongWordIds = emptyList(),
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
        if (active && _uiState.value.stage !in setOf(ReadingPracticeStage.Completed, ReadingPracticeStage.Loading, ReadingPracticeStage.Error)) startTimer()
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
        allAnswers.clear()
    }

    override fun onCleared() {
        stopTimer()
        super.onCleared()
    }
}
