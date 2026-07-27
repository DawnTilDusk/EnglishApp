package com.example.seedie.domain.repository

import kotlinx.coroutines.flow.Flow

enum class WordBookSourceType {
    BUNDLED,
    REMOTE
}

enum class WordBookDownloadStatus {
    NOT_DOWNLOADED,
    DOWNLOADING,
    DOWNLOADED,
    FAILED
}

data class ManagedWordBook(
    val bookId: String,
    val title: String,
    val description: String,
    val language: String,
    val difficulty: String,
    val version: Int,
    val sourceType: WordBookSourceType,
    val downloadStatus: WordBookDownloadStatus,
    val isActive: Boolean,
    val wordCount: Int,
    val updatedAt: Long,
    val coverUrl: String? = null
)

interface WordBookRepository {
    fun observeWordBooks(): Flow<List<ManagedWordBook>>

    suspend fun refreshWordBooks(): Result<List<ManagedWordBook>>

    suspend fun downloadWordBook(bookId: String): Result<Unit>

    suspend fun setActiveWordBook(bookId: String): Result<Unit>
}

fun WordBookSourceType.asStorageValue(): String = name.lowercase()

fun WordBookDownloadStatus.asStorageValue(): String = name.lowercase()

fun String.toWordBookSourceType(): WordBookSourceType {
    return WordBookSourceType.values().firstOrNull { it.asStorageValue() == lowercase() }
        ?: WordBookSourceType.REMOTE
}

fun String.toWordBookDownloadStatus(): WordBookDownloadStatus {
    return WordBookDownloadStatus.values().firstOrNull { it.asStorageValue() == lowercase() }
        ?: WordBookDownloadStatus.NOT_DOWNLOADED
}
