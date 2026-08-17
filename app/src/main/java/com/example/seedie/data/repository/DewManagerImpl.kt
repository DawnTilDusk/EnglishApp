package com.example.seedie.data.repository

import com.example.seedie.data.local.dao.DewTransactionDao
import com.example.seedie.data.local.entity.DewTransactionEntity
import com.example.seedie.data.remote.AuthService
import com.example.seedie.domain.model.ConvertTokensToDewsResult
import com.example.seedie.domain.model.DewConstants
import com.example.seedie.domain.model.shouldSkipDuplicateTokenGrant
import com.example.seedie.domain.repository.DewManager
import com.example.seedie.domain.repository.EconomyManager
import dagger.hilt.android.scopes.ActivityRetainedScoped
import java.util.Calendar
import java.util.TimeZone
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

@Singleton
class DewManagerImpl @Inject constructor(
    private val dewTransactionDao: DewTransactionDao,
    private val authService: AuthService,
    private val economyManager: EconomyManager
) : DewManager {

    @OptIn(ExperimentalCoroutinesApi::class)
    override val totalDews: Flow<Int> = authService.currentSession
        .flatMapLatest { session ->
            val userId = session?.userId
            if (userId.isNullOrBlank()) {
                flowOf(0)
            } else {
                dewTransactionDao.getTotalDews(userId).map { (it ?: 0).coerceAtLeast(0) }
            }
        }

    override suspend fun addDews(
        amount: Int,
        reason: String,
        refId: String?,
        respectCap: Boolean
    ) {
        if (amount <= 0) return
        val userId = authService.currentSession.value?.userId ?: return
        val existing = if (!refId.isNullOrBlank()) {
            dewTransactionDao.findByRefId(userId, refId) != null
        } else {
            false
        }
        if (shouldSkipDuplicateTokenGrant(refId, existing)) return

        val granted = if (respectCap) {
            val (dayStart, dayEnd) = todayMillisBounds()
            val todaySoFar = dewTransactionDao.getTodayDewIncomeSum(userId, dayStart, dayEnd)
                .coerceAtLeast(0)
            val room = (DewConstants.DAILY_DEW_CAP - todaySoFar).coerceAtLeast(0)
            if (room <= 0) return
            amount.coerceAtMost(room)
        } else {
            amount
        }

        val transaction = DewTransactionEntity(
            id = UUID.randomUUID().toString(),
            userId = userId,
            timestamp = System.currentTimeMillis(),
            amount = granted,
            reason = reason,
            refId = refId?.takeIf { it.isNotBlank() }
        )
        dewTransactionDao.insertTransaction(transaction)
    }

    override suspend fun spendDews(amount: Int, item: String, refId: String?): Boolean {
        if (amount <= 0) return false
        val userId = authService.currentSession.value?.userId ?: return false
        if (!refId.isNullOrBlank() && dewTransactionDao.findByRefId(userId, refId) != null) {
            return true
        }
        val balance = dewTransactionDao.getTotalDewsOnce(userId)
        if (balance < amount) return false

        val transaction = DewTransactionEntity(
            id = UUID.randomUUID().toString(),
            userId = userId,
            timestamp = System.currentTimeMillis(),
            amount = -amount,
            reason = "Bought: $item",
            refId = refId?.takeIf { it.isNotBlank() }
        )
        val inserted = dewTransactionDao.insertTransaction(transaction)
        if (inserted == -1L) {
            return !refId.isNullOrBlank() && dewTransactionDao.findByRefId(userId, refId) != null
        }
        return true
    }

    override suspend fun convertTokensToDews(tokenAmount: Int): ConvertTokensToDewsResult {
        if (tokenAmount <= 0) return ConvertTokensToDewsResult.InvalidAmount
        val userId = authService.currentSession.value?.userId
            ?: return ConvertTokensToDewsResult.NotLoggedIn
        val (dayStart, dayEnd) = todayMillisBounds()
        val todayTokenEquivalent = dewTransactionDao
            .getTodayConvertTokenEquivalent(userId, dayStart, dayEnd)
        if (todayTokenEquivalent + tokenAmount > DewConstants.DAILY_CONVERT_TOKEN_MAX) {
            return ConvertTokensToDewsResult.DailyConvertCapReached
        }
        val convertRefId = "convert_token_to_dew:$userId:${todayDateString()}:$tokenAmount:$todayTokenEquivalent"
        val spent = economyManager.spendTokens(
            amount = tokenAmount,
            item = "Convert to dew ($tokenAmount tokens)",
            refId = convertRefId
        )
        if (!spent) {
            return ConvertTokensToDewsResult.NotEnoughTokens
        }
        val dewsAmount = tokenAmount * DewConstants.TOKEN_TO_DEW_RATE
        try {
            addDews(
                amount = dewsAmount,
                reason = "Convert: $tokenAmount tokens",
                refId = "dew:$convertRefId",
                respectCap = true
            )
        } catch (t: Throwable) {
            economyManager.addTokens(
                amount = tokenAmount,
                reason = "Rollback dew convert failure ($tokenAmount tokens)",
                refId = "rollback_$convertRefId"
            )
            throw t
        }
        return ConvertTokensToDewsResult.Success
    }

    private fun todayMillisBounds(): Pair<Long, Long> {
        val cal = Calendar.getInstance(TimeZone.getDefault())
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, 1)
        val end = cal.timeInMillis
        return start to end
    }

    private fun todayDateString(): String {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        return sdf.format(java.util.Date())
    }
}
