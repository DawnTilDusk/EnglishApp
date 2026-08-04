package com.example.seedie.domain.quiz

/**
 * Linear grade-band vocabulary size estimator.
 * Quotas are frozen in [VocabularyQuizConstants.GRADE_BANDS] and sum to [VocabularyQuizConstants.ESTIMATE_CAP].
 */
object GradeBandVocabularyEstimator {
    data class BandScore(
        val bandIndex: Int,
        val correct: Int,
        val total: Int
    )

    fun shouldAdvance(
        correct: Int,
        total: Int = VocabularyQuizConstants.WORDS_PER_BAND,
        advanceMin: Int = VocabularyQuizConstants.ADVANCE_MIN_CORRECT
    ): Boolean = total >= VocabularyQuizConstants.WORDS_PER_BAND && correct >= advanceMin

    /**
     * @param scores ordered band results (completed or partial quit on last).
     * Passed band (full [wordsPerBand] answered and correct >= advanceMin) → full quota.
     * Stopping / partial band → (correct / wordsPerBand) * quota.
     * All six bands passed → [cap].
     */
    fun estimate(
        scores: List<BandScore>,
        bands: List<GradeBand> = VocabularyQuizConstants.GRADE_BANDS,
        cap: Int = VocabularyQuizConstants.ESTIMATE_CAP,
        wordsPerBand: Int = VocabularyQuizConstants.WORDS_PER_BAND,
        advanceMin: Int = VocabularyQuizConstants.ADVANCE_MIN_CORRECT
    ): Int {
        if (scores.isEmpty()) return 0

        val allPassed = scores.size == bands.size &&
            scores.all { it.total >= wordsPerBand && it.correct >= advanceMin }
        if (allPassed) return cap

        var total = 0
        for (score in scores) {
            val quota = bands.getOrNull(score.bandIndex)?.quota ?: continue
            val passed = score.total >= wordsPerBand && score.correct >= advanceMin
            total += if (passed) {
                quota
            } else {
                ((score.correct.toFloat() / wordsPerBand) * quota).toInt()
            }
        }
        return total.coerceIn(0, cap)
    }
}
