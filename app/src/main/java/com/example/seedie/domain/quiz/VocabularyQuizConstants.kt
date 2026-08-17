package com.example.seedie.domain.quiz

data class GradeBand(
    val band: String,
    val bookId: String,
    val title: String,
    val quota: Int
)

object VocabularyQuizConstants {
    const val MODULE_ID = "quiz"
    const val WORDS_PER_BAND = 12
    const val ADVANCE_MIN_CORRECT = 7
    const val ESTIMATE_CAP = 1800
    const val COMPLETION_BONUS = 5
    const val ESTIMATE_DISCLAIMER = "按外研分册随机抽样估测，上限参照 1800"
    const val NONE_OF_ABOVE_LABEL = "以上都不对"
    const val NONE_OF_ABOVE_PROBABILITY = 0.4f

    /** Frozen quotas from fltrp_junior_words.json (sum == 1800). */
    val GRADE_BANDS: List<GradeBand> = listOf(
        GradeBand("g7a", "fltrp-g7-vol1", "七年级上册", 549),
        GradeBand("g7b", "fltrp-g7-vol2", "七年级下册", 354),
        GradeBand("g8a", "fltrp-g8-vol1", "八年级上册", 268),
        GradeBand("g8b", "fltrp-g8-vol2", "八年级下册", 230),
        GradeBand("g9a", "fltrp-g9-vol1", "九年级上册", 294),
        GradeBand("g9b", "fltrp-g9-vol2", "九年级下册", 105)
    )

    val QUIZ_BOOK_IDS: List<String> = GRADE_BANDS.map { it.bookId }

    @Deprecated("Replaced by grade-band quiz", ReplaceWith("WORDS_PER_BAND"))
    const val QUESTION_COUNT = WORDS_PER_BAND

    @Deprecated("Replaced by grade-band quiz")
    const val WORDS_PER_DIFFICULTY = 4

    @Deprecated("Replaced by grade-band quiz")
    val DIFFICULTY_LEVELS = listOf("easy", "medium", "hard")
}
