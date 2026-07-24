package com.example.seedie.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EconomyBalanceTest {

    @Test
    fun projectDisplayedTokenBalance_usesCloudPlusPendingWhenCloudKnown() {
        assertEquals(
            60,
            projectDisplayedTokenBalance(
                cloudBalance = 50,
                localLedgerSum = 80,
                pendingSum = 10
            )
        )
    }

    @Test
    fun projectDisplayedTokenBalance_fallsBackToLocalSumWhenCloudUnknown() {
        assertEquals(
            80,
            projectDisplayedTokenBalance(
                cloudBalance = null,
                localLedgerSum = 80,
                pendingSum = 10
            )
        )
    }

    @Test
    fun shouldSkipDuplicateTokenGrant_whenRefPresentAndExists() {
        assertTrue(shouldSkipDuplicateTokenGrant("study:abc", existingWithSameRef = true))
        assertFalse(shouldSkipDuplicateTokenGrant("study:abc", existingWithSameRef = false))
        assertFalse(shouldSkipDuplicateTokenGrant(null, existingWithSameRef = true))
        assertFalse(shouldSkipDuplicateTokenGrant("  ", existingWithSameRef = true))
    }
}
