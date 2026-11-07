package com.example.seedie.data.repository

import android.util.Log
import com.example.seedie.data.local.SeedieDatabase
import com.example.seedie.data.local.entity.StudySettlementEntity
import com.example.seedie.data.remote.AuthService
import com.example.seedie.di.ApplicationScope
import com.example.seedie.domain.model.RewardEvent
import com.example.seedie.domain.model.StudyDay
import com.example.seedie.domain.model.StudyResult
import com.example.seedie.domain.model.tokenRewardRef
import com.example.seedie.domain.model.dewRewardRef
import com.example.seedie.domain.repository.DewManager
import com.example.seedie.domain.repository.EconomyManager
import com.example.seedie.domain.repository.ProfileRepository
import com.example.seedie.domain.usecase.GardenEngine
import com.example.seedie.domain.usecase.RewardEventBus
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Durable outbox: navigation is never the only owner of a completed learning result. */
@Singleton
class StudySettlementManager @Inject constructor(
    private val database: SeedieDatabase,
    private val auth: AuthService,
    private val tokens: EconomyManager,
    private val dews: DewManager,
    private val tasks: DailyTaskManager,
    private val garden: GardenEngine,
    private val profile: ProfileRepository,
    private val rewards: RewardEventBus,
    private val diagnostics: LearningDiagnostics,
    @param:ApplicationScope private val scope: CoroutineScope
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val mutex = Mutex()
    private val requests = Channel<Unit>(Channel.CONFLATED)

    init {
        scope.launch {
            for (request in requests) replay()
        }
        scope.launch {
            auth.currentSession.map { it?.userId }.distinctUntilChanged().collectLatest { userId ->
                if (userId == null) return@collectLatest
                while (true) {
                    try {
                        tasks.reconcileVocabulary(userId, StudyDay.date())
                        requests.trySend(Unit)
                    } catch (e: CancellationException) { throw e }
                    catch (e: Exception) { Log.w("StudySettlement", "Daily progress retry needed") }
                    delay(30_000)
                }
            }
        }
    }

    fun currentUserId(): String? = auth.currentSession.value?.userId

    suspend fun enqueue(result: StudyResult, ownerUserId: String? = currentUserId()) {
        // Review sessions carry no completion/reward effects.
        if (!result.countsTowardTasks && result.earnedTokens <= 0 && result.earnedDews <= 0) return
        val userId = auth.currentSession.value?.userId ?: return
        if (userId != ownerUserId) return
        database.learningStateDao().enqueue(StudySettlementEntity(
            userId, "${result.moduleId}:${result.sessionId}", json.encodeToString(result), result.completedAtMillis
        ))
        diagnostics.record("result_saved", result.moduleId, result.sessionId)
        requests.trySend(Unit)
    }

    suspend fun replay() = mutex.withLock {
        val userId = auth.currentSession.value?.userId ?: return@withLock
        for (row in database.learningStateDao().pending(userId)) {
            try {
                check(auth.currentSession.value?.userId == userId)
                val result = json.decodeFromString<StudyResult>(row.payload)
                val tokenRef = tokenRef(result)
                if (result.earnedTokens > 0) {
                    val existed = database.economyTransactionDao().findByRefId(userId, tokenRef) != null
                    tokens.addTokens(result.earnedTokens, "Study: ${result.moduleId}", tokenRef)
                    if (!existed) rewards.emit(RewardEvent.TokenDropped(result.earnedTokens))
                }
                check(auth.currentSession.value?.userId == userId)
                if (result.earnedDews > 0) {
                    val granted = dews.addDews(result.earnedDews, "Study dew: ${result.moduleId}", dewRef(result))
                    if (granted > 0) rewards.emit(RewardEvent.DewDropped(granted))
                }
                check(auth.currentSession.value?.userId == userId)
                tasks.completeFromResult(userId, result)
                if (result.isCompleted && result.moduleId in setOf("reading", "listening", "writing")) {
                    garden.recordFromStudyResult(result)
                }
                result.estimatedVocabulary?.let { profile.setMyVocabularyEstimate(it) }
                database.learningStateDao().markSettled(userId, row.settlementId)
                diagnostics.record("settlement_completed", result.moduleId, result.sessionId)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                // Do not log payloads, signed URLs or student content.
                Log.w("StudySettlement", "Settlement retained for retry")
                diagnostics.record("settlement_retry_needed", "settlement", row.settlementId)
            }
        }
    }

    suspend fun rewardSummary(result: StudyResult): String {
        val userId = auth.currentSession.value?.userId ?: return ""
        val parts = mutableListOf<String>()
        if (result.earnedTokens > 0) {
            val tx = database.economyTransactionDao().findByRefId(userId, tokenRef(result))
            parts += when {
                tx == null -> "代币结算待处理"
                tx.syncStatus == "SYNCED" -> "+${tx.amount} 代币（已同步）"
                else -> "+${tx.amount} 代币（已记入，待同步）"
            }
        }
        if (result.earnedDews > 0) {
            val tx = database.dewTransactionDao().findByRefId(userId, dewRef(result))
            parts += when {
                tx == null -> "露珠结算待处理"
                tx.amount == 0 -> "露珠已达今日上限"
                tx.amount < result.earnedDews -> "+${tx.amount} 露珠（本地，已达今日上限）"
                else -> "+${tx.amount} 露珠（本地）"
            }
        }
        return parts.joinToString("，")
    }

    private fun tokenRef(result: StudyResult) = result.tokenRewardRef()
    private fun dewRef(result: StudyResult) = result.dewRewardRef()
}
