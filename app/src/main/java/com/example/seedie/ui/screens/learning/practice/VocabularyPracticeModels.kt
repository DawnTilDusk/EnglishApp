package com.example.seedie.ui.screens.learning.practice
import com.example.seedie.domain.model.StudyResult

data class VocabularyPracticeArgs(
    val sessionId: String? = null,
    val sourceModuleId: String = "vocabulary",
    val planId: String? = null,
    val wordCountTarget: Int = 10,
    val difficulty: String = "mixed",
    val resumeToken: String? = null,
    val entryMode: VocabularyPracticeMode = VocabularyPracticeMode.Study,
    val targetRoundId: String? = null
)

enum class VocabularyPracticeStage {
    Loading,
    Ready,
    AnswerEvaluated,
    Completed,
    Empty,
    Error
}

enum class VocabularyPracticeMode {
    Study,
    Review
}

enum class VocabularyQuestionType {
    StudyEnglishToChinese,
    StudyChineseToEnglish,
    StudyContextChoice,
    ReviewSpelling
}

enum class AnswerStatus {
    Unanswered,
    Correct,
    Wrong,
    Skipped,
    Revealed,
    TimedOut
}

data class VocabularyPracticeOption(
    val optionId: String,
    val label: String,
    val isCorrect: Boolean,
    val englishHint: String? = null,
    val showFeedbackHint: Boolean = true
)

data class VocabularyPracticeWord(
    val wordId: String,
    val bookId: String,
    val english: String,
    val phonetic: String,
    val partOfSpeech: String,
    val translation: String,
    val exampleSentence: String,
    val difficultyLevel: String,
    val rewardToken: Int,
    val estimatedDurationSec: Int,
    val sortOrder: Int,
    val translationOptions: List<VocabularyPracticeOption>,
    val englishOptions: List<VocabularyPracticeOption>,
    val contextOptions: List<VocabularyPracticeOption>,
    val contextSentence: String
)

data class VocabularyWordProgress(
    val wordId: String,
    val passedStudyQuestionTypes: Set<VocabularyQuestionType> = emptySet(),
    val hasSeenStudyWord: Boolean = false,
    val consecutiveReviewWrongCount: Int = 0,
    val totalWrongCount: Int = 0,
    val revealCount: Int = 0,
    val isMasteredToday: Boolean = false,
    val isReviewCompleted: Boolean = false
)

data class VocabularyPracticePrompt(
    val promptId: String,
    val word: VocabularyPracticeWord,
    val section: VocabularyPracticeMode,
    val questionType: VocabularyQuestionType,
    val stageTitle: String,
    val promptTitle: String,
    val promptBody: String,
    val helperText: String,
    val optionList: List<VocabularyPracticeOption> = emptyList(),
    val correctAnswerText: String,
    val firstLetterHint: String? = null
)

data class VocabularySessionMeta(
    val sessionId: String,
    val moduleId: String,
    val startedAt: Long,
    val targetWordCount: Int,
    val difficulty: String,
    val source: String,
    val resumeSupported: Boolean
)

data class VocabularyResumeWordProgress(
    val wordId: String,
    val passedStudyQuestionTypes: Set<VocabularyQuestionType> = emptySet(),
    val hasSeenStudyWord: Boolean = false,
    val totalWrongCount: Int = 0,
    val revealCount: Int = 0
)

data class VocabularyPracticeResumeSnapshot(
    val roundId: String,
    val bookId: String,
    val activeWordIds: List<String>,
    val introducedStudyCount: Int,
    val studyTargetCount: Int,
    val nextWordSortOrderCursor: Int,
    val masteredStudyCount: Int,
    val wordProgressList: List<VocabularyResumeWordProgress>
)

data class VocabularyPracticeSession(
    val sessionMeta: VocabularySessionMeta,
    val studyWords: List<VocabularyPracticeWord>,
    val reviewWords: List<VocabularyPracticeWord>,
    val resumeSnapshot: VocabularyPracticeResumeSnapshot? = null
)

data class PendingReviewEntry(
    val roundId: String,
    val bookId: String,
    val pendingWordCount: Int
)

data class VocabularyImmediateReviewRequest(
    val result: StudyResult,
    val args: VocabularyPracticeArgs
)

data class VocabularyQuestionRecord(
    val sessionId: String,
    val promptId: String,
    val wordId: String,
    val section: VocabularyPracticeMode,
    val questionType: VocabularyQuestionType,
    val selectedOptionId: String?,
    val typedAnswer: String?,
    val isCorrect: Boolean,
    val isSkipped: Boolean,
    val usedRevealAnswer: Boolean,
    val usedFirstLetterHint: Boolean,
    val elapsedSeconds: Int
)

data class VocabularyPracticeUiState(
    val stage: VocabularyPracticeStage = VocabularyPracticeStage.Loading,
    val sessionMeta: VocabularySessionMeta? = null,
    val session: VocabularyPracticeSession? = null,
    val currentSection: VocabularyPracticeMode = VocabularyPracticeMode.Study,
    val currentPrompt: VocabularyPracticePrompt? = null,
    val currentWordProgress: VocabularyWordProgress? = null,
    val studyQueueSize: Int = 0,
    val reviewQueueSize: Int = 0,
    val introducedStudyCount: Int = 0,
    val studyTargetCount: Int = 0,
    val masteredStudyCount: Int = 0,
    val completedReviewCount: Int = 0,
    val sentBackToStudyCount: Int = 0,
    val completedRoundId: String? = null,
    val pendingReviewWordCount: Int = 0,
    val canStartImmediateReview: Boolean = false,
    val selectedOptionId: String? = null,
    val spellingInput: String = "",
    val answerStatus: AnswerStatus = AnswerStatus.Unanswered,
    val feedbackMessage: String = "",
    val reviewHintCountdownSec: Int = 5,
    val showFirstLetterHint: Boolean = false,
    val showPhoneticHint: Boolean = false,
    val firstLetterHint: String? = null,
    val correctCount: Int = 0,
    val wrongCount: Int = 0,
    val skippedCount: Int = 0,
    val earnedTokens: Int = 0,
    val elapsedSeconds: Int = 0,
    val canSubmitAnswer: Boolean = false,
    val canGoNext: Boolean = false,
    val showExitConfirmDialog: Boolean = false,
    val showFinishDialog: Boolean = false,
    val errorMessage: String? = null
)
