package com.example.seedie.domain.quiz

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GradeBandVocabularyEstimatorTest {

    @Test
    fun estimate_emptyScores_returnsZero() {
        assertEquals(0, GradeBandVocabularyEstimator.estimate(emptyList()))
    }

    @Test
    fun estimate_zeroOfTwelveOnFirstBand_returnsZero() {
        val result = GradeBandVocabularyEstimator.estimate(
            listOf(GradeBandVocabularyEstimator.BandScore(0, correct = 0, total = 12))
        )
        assertEquals(0, result)
    }

    @Test
    fun estimate_sevenOfTwelveOnFirstBand_isProportional() {
        val quota = VocabularyQuizConstants.GRADE_BANDS[0].quota
        val expected = ((7f / 12f) * quota).toInt()
        val result = GradeBandVocabularyEstimator.estimate(
            listOf(GradeBandVocabularyEstimator.BandScore(0, correct = 7, total = 12))
        )
        assertEquals(expected, result)
        assertFalse(GradeBandVocabularyEstimator.shouldAdvance(7, 12))
    }

    @Test
    fun estimate_elevenOfTwelveAdvancesFullQuotaThenStopsNext() {
        assertTrue(GradeBandVocabularyEstimator.shouldAdvance(11, 12))
        val result = GradeBandVocabularyEstimator.estimate(
            listOf(
                GradeBandVocabularyEstimator.BandScore(0, 11, 12),
                GradeBandVocabularyEstimator.BandScore(1, 5, 12)
            )
        )
        val expected = VocabularyQuizConstants.GRADE_BANDS[0].quota +
            ((5f / 12f) * VocabularyQuizConstants.GRADE_BANDS[1].quota).toInt()
        assertEquals(expected, result)
    }

    @Test
    fun estimate_allSixBandsPassed_returnsCap() {
        val scores = VocabularyQuizConstants.GRADE_BANDS.indices.map { index ->
            GradeBandVocabularyEstimator.BandScore(index, correct = 12, total = 12)
        }
        assertEquals(1800, GradeBandVocabularyEstimator.estimate(scores))
    }

    @Test
    fun quotas_sumToCap() {
        assertEquals(
            VocabularyQuizConstants.ESTIMATE_CAP,
            VocabularyQuizConstants.GRADE_BANDS.sumOf { it.quota }
        )
    }
}
