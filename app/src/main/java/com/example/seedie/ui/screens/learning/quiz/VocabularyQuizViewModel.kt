package com.example.seedie.ui.screens.learning.quiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.seedie.data.local.entity.VocabularyWordEntity
import com.example.seedie.data.repository.VocabularyOptionBuilder
import com.example.seedie.data.repository.VocabularyQuizSessionFactory
import com.example.seedie.domain.model.GardenSpeciesCatalog
import com.example.seedie.domain.model.StudyResult
import com.example.seedie.domain.quiz.GradeBandVocabularyEstimator
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
    private val repository: VocabularyQuizRepository,
    private val optionBuilder: VocabularyOptionBuilder
) : ViewModel() {
    private val _uiState = MutableStateFlow(VocabularyQuizUiState())
    val uiState = _uiState.asStateFlow()

    private val _studyResults = MutableSharedFlow<StudyResult>()
    val studyResults = _studyResults.asSharedFlow()

    private var sessionId: String? = null
    private var selectedSpeciesId: String = GardenSpeciesCatalog.DEFAULT_SPECIES_ID
    private var wordsByBookId: Map<String, List<VocabularyWordEntity>> = emptyMap()
    private var bandQuestions: List<VocabularyQuizQuestion> = emptyList()
    private var bandIndex: Int = 0
    private var bandCorrect: Int = 0
    private var bandAnswered: Int = 0
    private val bandScores = mutableListOf<GradeBandVocabularyEstimator.BandScore>()
    private var sessionFinished = false
    private var timerJob: Job? = null
    private val wrongWordIds = linkedSetOf<String>()
    private val wrongWords = mutableListOf<VocabularyQuizWrongWord>()
    private var sessionOpenedAtMillis: Long = 0L

    fun setSelectedSpeciesId(speciesId: String) {
        selectedSpeciesId = speciesId.ifBlank { GardenSpeciesCatalog.DEFAULT_SPECIES_ID }
    }

    fun initialize(sessionId: String = UUID.randomUUID().toString()) {
        if (this.sessionId == sessionId && _uiState.value.stage != VocabularyQuizStage.Error) return
        loadPool(sessionId)
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
        if (bandAnswered > 0 && bandScores.none { it.bandIndex == bandIndex }) {
            bandScores += GradeBandVocabularyEstimator.BandScore(
                bandIndex = bandIndex,
                correct = bandCorrect,
                total = bandAnswered
            )
        }
        if (bandScores.isNotEmpty()) {
            val estimated = GradeBandVocabularyEstimator.estimate(bandScores.toList())
            _uiState.update { it.copy(estimatedVocabulary = estimated) }
        }
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

        bandAnswered += 1
        if (selectedOption.isCorrect) {
            bandCorrect += 1
            val dewDelta = if (question.rewardToken >= 2) 2 else 1
            _uiState.update {
                it.copy(
                    stage = VocabularyQuizStage.AnswerEvaluated,
                    answerStatus = AnswerStatus.Correct,
                    feedbackMessage = "回答正确",
                    correctCount = it.correctCount + 1,
                    earnedTokens = it.earnedTokens + question.rewardToken,
                    earnedDews = it.earnedDews + dewDelta,
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
        val nextIndex = _uiState.value.currentIndex + 1
        if (nextIndex >= bandQuestions.size) {
            onBandFinished()
            return
        }
        showQuestion(nextIndex)
    }

    fun onRetryLoad() {
        sessionId?.let(::loadPool)
    }

    fun onFinishSession() {
        finishSession(isCompleted = true)
    }

    private fun loadPool(id: String) {
        resetSession()
        sessionId = id
        sessionOpenedAtMillis = System.currentTimeMillis()
        _uiState.value = VocabularyQuizUiState(
            stage = VocabularyQuizStage.Loading,
            sessionOpenedAtMillis = sessionOpenedAtMillis
        )
        viewModelScope.launch {
            runCatching {
                repository.loadWordPool(sessionId = id)
            }.onSuccess { pool ->
                wordsByBookId = pool.wordsByBookId
                startTimer()
                startBand(0)
            }.onFailure { throwable ->
                _uiState.value = VocabularyQuizUiState(
                    stage = VocabularyQuizStage.Error,
                    sessionOpenedAtMillis = sessionOpenedAtMillis,
                    errorMessage = throwable.message ?: "词汇测验加载失败"
                )
            }
        }
    }

    private fun startBand(index: Int) {
        val band = VocabularyQuizConstants.GRADE_BANDS.getOrNull(index)
            ?: run {
                completeWithEstimate()
                return
            }
        val bookWords = wordsByBookId[band.bookId].orEmpty()
        val distractors = wordsByBookId.values.flatten()
        val sid = sessionId ?: return
        bandIndex = index
        bandCorrect = 0
        bandAnswered = 0
        bandQuestions = VocabularyQuizSessionFactory.createBandQuestions(
            sessionId = sid,
            bandIndex = index,
            bookWords = bookWords,
            distractorPool = distractors,
            optionBuilder = optionBuilder
        )
        _uiState.update {
            it.copy(
                stage = VocabularyQuizStage.Ready,
                sessionId = sid,
                totalCount = bandQuestions.size,
                bandIndex = index,
                bandCount = VocabularyQuizConstants.GRADE_BANDS.size,
                bandTitle = band.title,
                bandProgressLabel = "第 ${index + 1}/${VocabularyQuizConstants.GRADE_BANDS.size} 档 · ${band.title}",
                currentQuestion = null
            )
        }
        showQuestion(0)
    }

    private fun onBandFinished() {
        val score = GradeBandVocabularyEstimator.BandScore(
            bandIndex = bandIndex,
            correct = bandCorrect,
            total = bandAnswered
        )
        bandScores += score
        val advance = GradeBandVocabularyEstimator.shouldAdvance(bandCorrect, bandAnswered)
        if (advance && bandIndex < VocabularyQuizConstants.GRADE_BANDS.lastIndex) {
            startBand(bandIndex + 1)
        } else {
            completeWithEstimate()
        }
    }

    private fun completeWithEstimate() {
        stopTimer()
        val estimated = GradeBandVocabularyEstimator.estimate(bandScores.toList())
        _uiState.update {
            it.copy(
                stage = VocabularyQuizStage.Completed,
                currentQuestion = null,
                selectedOptionId = null,
                canSubmitAnswer = false,
                estimatedVocabulary = estimated
            )
        }
    }

    private fun showQuestion(questionIndex: Int) {
        val question = bandQuestions[questionIndex]
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
        val sid = state.sessionId ?: sessionId ?: return
        // If exiting without completing UI, still try to attach estimate if present
        val estimated = state.estimatedVocabulary
            ?: if (bandScores.isNotEmpty() || bandAnswered > 0) {
                val scores = bandScores.toMutableList()
                if (bandAnswered > 0 && scores.none { it.bandIndex == bandIndex }) {
                    scores += GradeBandVocabularyEstimator.BandScore(bandIndex, bandCorrect, bandAnswered)
                }
                GradeBandVocabularyEstimator.estimate(scores)
            } else {
                null
            }
        val answeredCount = state.correctCount + state.wrongCount
        val result = StudyResult(
            sessionId = sid,
            moduleId = VocabularyQuizConstants.MODULE_ID,
            isCompleted = isCompleted || estimated != null,
            completedQuestionCount = answeredCount,
            correctCount = state.correctCount,
            wrongCount = state.wrongCount,
            skippedCount = 0,
            accuracy = if (answeredCount == 0) 0f else state.correctCount.toFloat() / answeredCount,
            earnedTokens = 0,
            earnedDews = state.earnedDews,
            studyDurationSec = state.elapsedSeconds,
            vocabularyDelta = 0,
            wrongWordIds = wrongWordIds.toList(),
            estimatedVocabulary = estimated,
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
        sessionId = null
        sessionOpenedAtMillis = 0L
        wordsByBookId = emptyMap()
        bandQuestions = emptyList()
        bandIndex = 0
        bandCorrect = 0
        bandAnswered = 0
        bandScores.clear()
        sessionFinished = false
        wrongWordIds.clear()
        wrongWords.clear()
    }

    override fun onCleared() {
        stopTimer()
        super.onCleared()
    }
}
