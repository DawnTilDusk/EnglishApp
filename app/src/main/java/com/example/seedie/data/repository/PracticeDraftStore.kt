package com.example.seedie.data.repository

import com.example.seedie.data.local.SeedieDatabase
import com.example.seedie.data.local.entity.PracticeDraftEntity
import com.example.seedie.data.remote.AuthService
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import com.example.seedie.di.ApplicationScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException

/** One writer preserves answer order, and continues saving when a screen is removed. */
@Singleton
class PracticeDraftStore @Inject constructor(
    private val database: SeedieDatabase,
    private val auth: AuthService,
    private val diagnostics: LearningDiagnostics,
    @ApplicationScope scope: CoroutineScope
) {
    private data class Write(val userId: String, val key: String, val payload: String?, val barrier: CompletableDeferred<Unit>? = null)
    private val writes = Channel<Write>(Channel.UNLIMITED)
    private var writeFailure: Exception? = null
    private val observedAttempts = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
    init {
        scope.launch {
            for (write in writes) {
                if (write.barrier != null) {
                    val failure = writeFailure
                    writeFailure = null
                    if (failure == null) write.barrier.complete(Unit)
                    else write.barrier.completeExceptionally(failure)
                    continue
                }
                try {
                    if (write.payload == null) database.learningStateDao().deleteDraft(write.userId, write.key)
                    else database.learningStateDao().saveDraft(PracticeDraftEntity(
                        write.userId, write.key, write.payload, System.currentTimeMillis()
                    ))
                } catch (e: CancellationException) { throw e }
                catch (e: Exception) {
                    writeFailure = e
                    android.util.Log.w("PracticeDraft", "Draft save failed")
                }
            }
        }
    }
    fun save(key: String, payload: String) {
        val userId = auth.currentSession.value?.userId ?: return
        writes.trySend(Write(userId, key, payload))
        if (key.substringBefore(':') in setOf("vocab", "reading", "listening", "writing") && observedAttempts.add("$userId:$key")) {
            diagnostics.record("practice_started", key.substringBefore(':'), key)
        }
    }
    fun clear(key: String) {
        val userId = auth.currentSession.value?.userId ?: return
        writes.trySend(Write(userId, key, null))
        observedAttempts.remove("$userId:$key")
    }
    suspend fun load(key: String, ownerUserId: String? = auth.currentSession.value?.userId): String? {
        flush()
        val userId = ownerUserId ?: return null
        if (auth.currentSession.value?.userId != userId) return null
        val payload = database.learningStateDao().draft(userId, key)?.payload
        if (payload != null && observedAttempts.add("$userId:$key")) {
            diagnostics.record("practice_resumed", key.substringBefore(':'), key)
        }
        return payload
    }
    fun recordFailure(key: String) = diagnostics.record("practice_request_failed", key.substringBefore(':'), key)
    fun userId(): String? = auth.currentSession.value?.userId
    suspend fun flush() {
        val barrier = CompletableDeferred<Unit>()
        writes.send(Write("", "", null, barrier))
        barrier.await()
    }
}
