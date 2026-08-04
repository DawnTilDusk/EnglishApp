package com.example.seedie.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

@Serializable
private data class PracticeItemRefRow(
    @SerialName("item_ref") val item_ref: String,
    @SerialName("user_id") val user_id: String? = null,
    @SerialName("module_id") val module_id: String? = null
)

@Singleton
class PracticeCatalogRemoteDataSource @Inject constructor(
    private val client: SupabaseClient
) {
    suspend fun fetchCompletedItemRefs(moduleId: String): Set<String> {
        return client.postgrest["user_practice_item_completions"].select {
            filter { eq("module_id", moduleId) }
        }.decodeList<PracticeItemRefRow>().map { it.item_ref }.toSet()
    }

    suspend fun markCompleted(moduleId: String, itemRefs: List<String>) {
        if (itemRefs.isEmpty()) return
        client.postgrest.rpc(
            "mark_my_practice_items_completed",
            buildJsonObject {
                put("p_module_id", moduleId)
                putJsonArray("p_item_refs") {
                    itemRefs.forEach { add(it) }
                }
            }
        )
    }
}
