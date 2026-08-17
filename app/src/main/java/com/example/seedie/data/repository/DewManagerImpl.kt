package com.example.seedie.data.repository

import com.example.seedie.data.local.dao.DewTransactionDao
import com.example.seedie.data.local.entity.DewTransactionEntity
import com.example.seedie.data.remote.AuthService
import com.example.seedie.domain.model.ConvertTokensToDewsResult
import com.example.seedie.domain.model.DewConstants
import com.example.seedie.domain.model.shouldSkipDuplicateTokenGrant
import com.example.seedie.domain.repository.DewManager
import com.example.seedie.domain.repository.EconomyManager
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.min

@Singleton
class DewManagerImpl @Inject constructor(
    private val dewTransactionDao: DewTransactionDao,
    private val authService: AuthService,
    private val economyManager: EconomyManager
) : DewManager {

    /** Serializes local balance and daily-cap changes for this process. */
    private val dewMutex = Mutex()

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
    ): Int = dewMutex.withLock {
        addDewsLocked(amount, reason, refId, respectCap)
    }

    private suspend fun addDewsLocked(
        amount: Int,
        reason: String,
        refId: String?,
        respectCap: Boolean
    ): Int {
        if (amount <= 0) return 0
        val userId = authService.currentSession.value?.userId ?: return 0
        val existing = if (!refId.isNullOrBlank()) {
            dewTransactionDao.findByRefId(userId, refId) != null
        } else {
            false
        }
        if (shouldSkipDuplicateTokenGrant(refId, existing)) return 0

        val granted = if (respectCap) {
            val (dayStart, dayEnd) = todayMillisBounds()
            val todaySoFar = dewTransactionDao.getTodayDewIncomeSum(userId, dayStart, dayEnd)
                .coerceAtLeast(0)
            val room = (DewConstants.DAILY_DEW_CAP - todaySoFar).coerceAtLeast(0)
            if (room <= 0) return 0
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
        return if (dewTransactionDao.insertTransaction(transaction) == -1L) 0 else granted
    }

    override suspend fun spendDews(amount: Int, item: String, refId: String?): Boolean = dewMutex.withLock {
        if (amount <= 0) return@withLock false
        val userId = authService.currentSession.value?.userId ?: return@withLock false
        if (!refId.isNullOrBlank() && dewTransactionDao.findByRefId(userId, refId) != null) {
            return@withLock true
        }
        val balance = dewTransactionDao.getTotalDewsOnce(userId)
        if (balance < amount) return@withLock false

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
            return@withLock !refId.isNullOrBlank() && dewTransactionDao.findByRefId(userId, refId) != null
        }
        true
    }

    override suspend fun convertTokensToDews(tokenAmount: Int): ConvertTokensToDewsResult = dewMutex.withLock {
        if (tokenAmount <= 0) return@withLock ConvertTokensToDewsResult.InvalidAmount
        val userId = authService.currentSession.value?.userId
            ?: return@withLock ConvertTokensToDewsResult.NotLoggedIn
        val (dayStart, dayEnd) = todayMillisBounds()
        val todayConvertedDews = dewTransactionDao
            .getTodayConvertedDewSum(userId, dayStart, dayEnd)
            .coerceAtLeast(0)
        val todayConvertedTokens = todayConvertedDews / DewConstants.TOKEN_TO_DEW_RATE
        val tokenAllowance = (DewConstants.DAILY_CONVERT_TOKEN_MAX - todayConvertedTokens)
            .coerceAtLeast(0)
        if (tokenAmount > tokenAllowance) {
            return@withLock ConvertTokensToDewsResult.DailyConvertCapReached
        }

        val todayDewIncome = dewTransactionDao
            .getTodayDewIncomeSum(userId, dayStart, dayEnd)
            .coerceAtLeast(0)
        val dewAllowance = (DewConstants.DAILY_DEW_CAP - todayDewIncome)
            .coerceAtLeast(0) / DewConstants.TOKEN_TO_DEW_RATE
        if (tokenAmount > min(tokenAllowance, dewAllowance)) {
            return@withLock ConvertTokensToDewsResult.DailyDewCapReached
        }

        val convertRefId = "convert_token_to_dew:$userId:${todayDateString()}:$todayConvertedDews:$tokenAmount"
        val spent = economyManager.spendTokens(
            amount = tokenAmount,
            item = "Convert to dew ($tokenAmount tokens)",
            refId = convertRefId
        )
        if (!spent) {
            return@withLock ConvertTokensToDewsResult.NotEnoughTokens
        }
        val dewsAmount = tokenAmount * DewConstants.TOKEN_TO_DEW_RATE
        try {
            val granted = addDewsLocked(
                amount = dewsAmount,
                reason = "Convert: $tokenAmount tokens",
                refId = "dew:$convertRefId",
                respectCap = false
            )
            check(granted == dewsAmount) { "Dew conversion grant was not recorded" }
        } catch (t: Throwable) {
            economyManager.addTokens(
                amount = tokenAmount,
                reason = "Rollback dew convert failure ($tokenAmount tokens)",
                refId = "rollback_$convertRefId"
            )
            throw t
        }
        ConvertTokensToDewsResult.Success
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
