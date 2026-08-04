package com.example.seedie.domain.quiz

import org.junit.Assert.assertEquals
import org.junit.Test

class VocabularyEstimateCalculatorTest {

    @Test
    fun estimate_delegatesToBandScores() {
        val scores = listOf(
            GradeBandVocabularyEstimator.BandScore(0, correct = 7, total = 12)
        )
        assertEquals(
            GradeBandVocabularyEstimator.estimate(scores),
            VocabularyEstimateCalculator.estimate(
                VocabularyEstimateCalculator.Input(
                    correctCount = 7,
                    totalCount = 12,
                    bandScores = scores
                )
            )
        )
    }

    @Test
    fun estimate_legacyInputUsesFirstBandProportion() {
        val expected = GradeBandVocabularyEstimator.estimate(
            listOf(GradeBandVocabularyEstimator.BandScore(0, 0, 12))
        )
        assertEquals(
            expected,
            VocabularyEstimateCalculator.estimate(
                VocabularyEstimateCalculator.Input(
                    correctCount = 0,
                    totalCount = 12
                )
            )
        )
    }
}
