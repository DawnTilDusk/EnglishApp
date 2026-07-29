package com.example.seedie.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReadingRemoteDataSource @Inject constructor(
    private val client: SupabaseClient
) {
    suspend fun fetchAllSets(): List<SupabaseReadingSet> {
        return client.postgrest["reading_sets"].select {
            order("sort_order", Order.ASCENDING)
        }.decodeList()
    }

    suspend fun fetchAllQuestions(): List<SupabaseReadingQuestion> {
        return client.postgrest["reading_questions"].select {
            order("sort_order", Order.ASCENDING)
        }.decodeList()
    }

    suspend fun fetchAllOptions(): List<SupabaseReadingOption> {
        return client.postgrest["reading_options"].select {
            order("sort_order", Order.ASCENDING)
        }.decodeList()
    }
}
