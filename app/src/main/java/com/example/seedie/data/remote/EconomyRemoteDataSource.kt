package com.example.seedie.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

data class EconomyTransactionEntry(
    val id: String,
    val amount: Int,
    val reason: String,
    val refId: String? = null
)

@Singleton
class EconomyRemoteDataSource @Inject constructor(
    private val client: SupabaseClient
) : EconomyCloudGateway {
    override suspend fun fetchMyTokenBalance(): Int {
        return decodeRpcInt(
            client.postgrest.rpc("get_my_token_balance").decodeAs<JsonElement>()
        )
    }

    override suspend fun syncMyEconomyTransactions(entries: List<EconomyTransactionEntry>): Int {
        val payload = buildJsonArray {
            entries.forEach { entry ->
                add(
                    buildJsonObject {
                        put("id", entry.id)
                        put("amount", entry.amount)
                        put("reason", entry.reason)
                        entry.refId?.let { put("ref_id", it) }
                    }
                )
            }
        }
        return decodeRpcInt(
            client.postgrest.rpc(
                "sync_my_economy_transactions",
                buildJsonObject { put("p_entries", payload) }
            ).decodeAs<JsonElement>()
        )
    }

    suspend fun recordMyEconomyTransaction(
        id: String,
        amount: Int,
        reason: String,
        refId: String? = null
    ) {
        client.postgrest.rpc(
            "record_my_economy_transaction",
            buildJsonObject {
                put("p_id", id)
                put("p_amount", amount)
                put("p_reason", reason)
                if (refId != null) put("p_ref_id", refId)
            }
        )
    }

    private fun decodeRpcInt(element: JsonElement): Int {
        return when (element) {
            is JsonPrimitive -> element.content.toIntOrNull()
                ?: element.content.trim('"').toIntOrNull()
                ?: 0
            is JsonObject -> element.intField("value")
                ?: element.values.firstOrNull()?.let { decodeRpcInt(it) }
                ?: 0
            is JsonArray -> element.firstOrNull()?.let { decodeRpcInt(it) } ?: 0
            JsonNull -> 0
            else -> 0
        }
    }

    private fun JsonObject.intField(key: String): Int? {
        val value = this[key] ?: return null
        if (value is JsonNull) return null
        return value.jsonPrimitive.content.toIntOrNull()
    }
}
