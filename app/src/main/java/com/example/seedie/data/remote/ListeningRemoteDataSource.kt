package com.example.seedie.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ListeningRemoteDataSource @Inject constructor(
    private val client: SupabaseClient
) {
    suspend fun fetchAllMaterials(): List<SupabaseListeningMaterial> {
        return client.postgrest["listening_materials"].select {
            order("sort_order", Order.ASCENDING)
        }.decodeList()
    }

    suspend fun fetchMaterialsByBook(bookId: String): List<SupabaseListeningMaterial> {
        return client.postgrest["listening_materials"].select {
            filter { eq("book_id", bookId) }
            order("sort_order", Order.ASCENDING)
        }.decodeList()
    }

    suspend fun fetchAllQuestions(): List<SupabaseListeningQuestion> {
        return client.postgrest["listening_questions"].select {
            order("sort_order", Order.ASCENDING)
        }.decodeList()
    }

    suspend fun fetchAllOptions(): List<SupabaseListeningOption> {
        return client.postgrest["listening_options"].select {
            order("sort_order", Order.ASCENDING)
        }.decodeList()
    }
}
