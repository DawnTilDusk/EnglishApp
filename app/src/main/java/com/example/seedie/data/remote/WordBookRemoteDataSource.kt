package com.example.seedie.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WordBookRemoteDataSource @Inject constructor(
    private val client: SupabaseClient
) {
    suspend fun fetchAllBooks(): List<SupabaseWordBook> {
        return client.postgrest["word_books"].select {
            order("updated_at", Order.DESCENDING)
        }.decodeList<SupabaseWordBook>()
    }

    suspend fun fetchModulesByBook(bookId: String): List<SupabaseWordBookModule> {
        return client.postgrest["word_book_modules"].select {
            filter { eq("book_id", bookId) }
            order("sort_order", Order.ASCENDING)
        }.decodeList<SupabaseWordBookModule>()
    }

    suspend fun fetchWordsByBook(bookId: String): List<SupabaseVocabularyWord> {
        return client.postgrest["vocabulary_words"].select {
            filter { eq("book_id", bookId) }
            order("sort_order", Order.ASCENDING)
        }.decodeList<SupabaseVocabularyWord>()
    }
}
