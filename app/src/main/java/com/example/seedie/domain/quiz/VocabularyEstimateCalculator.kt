package com.example.seedie.domain.quiz

/**
 * Thin adapter kept for older call sites/tests; prefer [GradeBandVocabularyEstimator].
 */
@Deprecated("Use GradeBandVocabularyEstimator")
object VocabularyEstimateCalculator {
    data class Input(
        val correctCount: Int,
        val totalCount: Int,
        val correctDifficulties: List<String> = emptyList(),
        val bandScores: List<GradeBandVocabularyEstimator.BandScore> = emptyList()
    )

    fun estimate(input: Input): Int {
        if (input.bandScores.isNotEmpty()) {
            return GradeBandVocabularyEstimator.estimate(input.bandScores)
        }
        // Legacy single-band fallback: treat as g7a only
        val correct = input.correctCount.coerceAtLeast(0)
        val total = input.totalCount.coerceAtLeast(0)
        return GradeBandVocabularyEstimator.estimate(
            listOf(
                GradeBandVocabularyEstimator.BandScore(
                    bandIndex = 0,
                    correct = correct,
                    total = if (total == 0) VocabularyQuizConstants.WORDS_PER_BAND else total
                )
            )
        )
    }
}
