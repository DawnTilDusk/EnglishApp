package com.example.seedie.domain.model

/**
 * Displayed token balance.
 * When [cloudBalance] is known: cloud + unsynced pending delta.
 * When unknown (cold start): fall back to full local ledger sum.
 */
fun projectDisplayedTokenBalance(
    cloudBalance: Int?,
    localLedgerSum: Int,
    pendingSum: Int
): Int {
    return if (cloudBalance != null) {
        cloudBalance + pendingSum
    } else {
        localLedgerSum
    }
}

/** Skip granting when a non-blank business [refId] already exists locally. */
fun shouldSkipDuplicateTokenGrant(refId: String?, existingWithSameRef: Boolean): Boolean {
    return !refId.isNullOrBlank() && existingWithSameRef
}
