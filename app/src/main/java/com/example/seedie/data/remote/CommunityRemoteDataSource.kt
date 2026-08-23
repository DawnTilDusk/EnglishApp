package com.example.seedie.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommunityRemoteDataSource @Inject constructor(
    private val client: SupabaseClient
) {
    suspend fun fetchPosts(limit: Int = 50): List<SupabaseCommunityPost> {
        return client.postgrest["community_posts"].select {
            order("created_at", Order.DESCENDING)
            limit(limit.toLong())
        }.decodeList()
    }

    suspend fun createPost(title: String?, body: String): String {
        val raw = client.postgrest.rpc(
            "create_community_post",
            buildJsonObject {
                if (title.isNullOrBlank()) {
                    put("p_title", JsonNull)
                } else {
                    put("p_title", title.trim())
                }
                put("p_body", body)
                put("p_target_grades", JsonNull)
            }
        ).decodeAs<String>()
        return raw.trim().trim('"')
    }

    suspend fun deletePost(postId: String) {
        client.postgrest.rpc(
            "delete_community_post",
            buildJsonObject { put("p_post_id", postId) }
        )
    }
}
