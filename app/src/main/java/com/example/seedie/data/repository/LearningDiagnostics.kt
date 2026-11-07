package com.example.seedie.data.repository

import com.example.seedie.data.local.SeedieDatabase
import com.example.seedie.data.local.entity.PracticeDraftEntity
import com.example.seedie.data.remote.AuthService
import com.example.seedie.di.ApplicationScope
import javax.inject.Inject
import javax.inject.Singleton
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Trial diagnostics stay on the device. Payloads never include student answers, files or signed URLs. */
@Singleton
class LearningDiagnostics @Inject constructor(
    private val database: SeedieDatabase,
    private val auth: AuthService,
    @param:ApplicationScope private val scope: CoroutineScope
) {
    fun record(event: String, moduleId: String, referenceId: String) {
        val userId = auth.currentSession.value?.userId ?: return
        val now = System.currentTimeMillis()
        scope.launch {
            runCatching {
                val payload = buildJsonObject {
                    put("event", event); put("module", moduleId); put("reference_id", referenceId); put("at", now)
                }
                database.learningStateDao().saveDraft(PracticeDraftEntity(userId, "event:${UUID.randomUUID()}", payload.toString(), now))
                database.learningStateDao().pruneDiagnostics(now - 30L * 24 * 60 * 60 * 1000)
            }
        }
    }
}
