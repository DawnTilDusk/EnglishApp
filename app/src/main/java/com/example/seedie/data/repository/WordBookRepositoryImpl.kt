package com.example.seedie.data.repository

import androidx.room.withTransaction
import com.example.seedie.data.local.SeedieDatabase
import com.example.seedie.data.local.dao.VocabularyWordDao
import com.example.seedie.data.local.dao.WordBookDao
import com.example.seedie.data.local.dao.WordBookModuleDao
import com.example.seedie.data.local.entity.VocabularyWordEntity
import com.example.seedie.data.local.entity.WordBookEntity
import com.example.seedie.data.local.entity.WordBookModuleEntity
import com.example.seedie.data.remote.SupabaseVocabularyWord
import com.example.seedie.data.remote.SupabaseWordBook
import com.example.seedie.data.remote.SupabaseWordBookModule
import com.example.seedie.data.remote.WordBookRemoteDataSource
import com.example.seedie.domain.repository.ManagedWordBook
import com.example.seedie.domain.repository.WordBookDownloadStatus
import com.example.seedie.domain.repository.WordBookRepository
import com.example.seedie.domain.repository.WordBookSourceType
import com.example.seedie.domain.repository.asStorageValue
import com.example.seedie.domain.repository.toWordBookDownloadStatus
import com.example.seedie.domain.repository.toWordBookSourceType
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class WordBookRepositoryImpl @Inject constructor(
    private val database: SeedieDatabase,
    private val wordBookDao: WordBookDao,
    private val wordBookModuleDao: WordBookModuleDao,
    private val vocabularyWordDao: VocabularyWordDao,
    private val remoteDataSource: WordBookRemoteDataSource,
    private val wordBookSeeder: WordBookSeeder
) : WordBookRepository {

    override fun observeWordBooks(): Flow<List<ManagedWordBook>> {
        return wordBookDao.getAllBooks().map { books -> books.map { it.toManagedWordBook() } }
    }

    override suspend fun refreshWordBooks(): Result<List<ManagedWordBook>> = runCatching {
        wordBookSeeder.ensureSeeded()

        val localBooks = wordBookDao.getAllBooksOnce().associateBy { it.bookId }
        val mergedRemoteBooks = remoteDataSource.fetchAllBooks().map { remoteBook ->
            remoteBook.toWordBookEntity(previous = localBooks[remoteBook.book_id])
        }

        wordBookDao.insertBooks(mergedRemoteBooks)
        wordBookDao.getAllBooksOnce().map { it.toManagedWordBook() }
    }

    override suspend fun downloadWordBook(bookId: String): Result<Unit> = runCatching {
        wordBookSeeder.ensureSeeded()
        ensureBookMetadata(bookId)
        wordBookDao.updateDownloadStatus(
            bookId = bookId,
            status = WordBookDownloadStatus.DOWNLOADING.asStorageValue()
        )

        try {
            val modules = remoteDataSource.fetchModulesByBook(bookId)
            val words = remoteDataSource.fetchWordsByBook(bookId)
            require(words.isNotEmpty()) { "词书内容为空" }

            database.withTransaction {
                val currentBook = wordBookDao.getBookById(bookId)
                    ?: error("词书元数据不存在")

                wordBookModuleDao.deleteModulesByBook(bookId)
                vocabularyWordDao.deleteWordsByBook(bookId)
                if (modules.isNotEmpty()) {
                    wordBookModuleDao.insertModules(modules.map { it.toEntity() })
                }
                vocabularyWordDao.insertWords(words.map { it.toEntity() })

                wordBookDao.insertBook(
                    currentBook.copy(
                        sourceType = WordBookSourceType.REMOTE.asStorageValue(),
                        downloadStatus = WordBookDownloadStatus.DOWNLOADED.asStorageValue(),
                        wordCount = words.size
                    )
                )
            }
        } catch (error: Exception) {
            wordBookDao.updateDownloadStatus(
                bookId = bookId,
                status = WordBookDownloadStatus.FAILED.asStorageValue()
            )
            throw error
        }
    }

    override suspend fun setActiveWordBook(bookId: String): Result<Unit> = runCatching {
        wordBookSeeder.ensureSeeded()
        val book = ensureBookMetadata(bookId)
        require(book.downloadStatus.toWordBookDownloadStatus() == WordBookDownloadStatus.DOWNLOADED) {
            "词书尚未下载"
        }

        database.withTransaction {
            wordBookDao.deactivateAllBooks()
            wordBookDao.setActiveBook(bookId)
        }
    }

    private suspend fun ensureBookMetadata(bookId: String): WordBookEntity {
        wordBookDao.getBookById(bookId)?.let { return it }
        refreshWordBooks().getOrThrow()
        return wordBookDao.getBookById(bookId)
            ?: throw IllegalArgumentException("未找到词书：$bookId")
    }
}

private fun SupabaseWordBook.toWordBookEntity(previous: WordBookEntity?): WordBookEntity {
    val sourceType = WordBookSourceType.REMOTE
    val previousStatus = previous?.downloadStatus?.toWordBookDownloadStatus()
    val isSameVersion = previous?.version == version
    val downloadStatus = when {
        previous == null -> WordBookDownloadStatus.NOT_DOWNLOADED
        isSameVersion && previousStatus != null -> previousStatus
        else -> WordBookDownloadStatus.NOT_DOWNLOADED
    }
    val isActive = previous?.isActive == true && downloadStatus == WordBookDownloadStatus.DOWNLOADED

    return WordBookEntity(
        bookId = book_id,
        title = title,
        description = description.orEmpty(),
        language = language ?: "en-US",
        difficulty = difficulty ?: "mixed",
        version = version,
        sourceType = sourceType.asStorageValue(),
        downloadStatus = downloadStatus.asStorageValue(),
        isActive = isActive,
        wordCount = word_count,
        updatedAt = updated_at,
        coverUrl = cover_url,
        gradeLevel = grade_level
    )
}

private fun SupabaseWordBookModule.toEntity(): WordBookModuleEntity {
    return WordBookModuleEntity(
        moduleId = module_id,
        bookId = book_id,
        title = title,
        sortOrder = sort_order,
        wordCount = word_count
    )
}

private fun SupabaseVocabularyWord.toEntity(): VocabularyWordEntity {
    val senseModels = senses.orEmpty().mapNotNull { sense ->
        val pos = sense.part_of_speech?.trim().orEmpty()
        val zh = sense.translation?.trim().orEmpty()
        if (pos.isBlank() || zh.isBlank()) null
        else com.example.seedie.domain.model.WordSense(partOfSpeech = pos, translation = zh)
    }
    val sensesJson = if (senseModels.isNotEmpty()) {
        com.example.seedie.domain.model.WordSenseFormat.encodeSensesJson(senseModels)
    } else {
        "[]"
    }
    return VocabularyWordEntity(
        wordId = word_id,
        bookId = book_id,
        english = english,
        phonetic = phonetic.orEmpty(),
        partOfSpeech = part_of_speech.orEmpty(),
        translation = translation.orEmpty(),
        exampleSentence = example_sentence.orEmpty(),
        difficultyLevel = difficulty_level ?: "mixed",
        rewardToken = reward_token,
        estimatedDurationSec = estimated_duration_sec,
        sortOrder = sort_order,
        moduleId = module_id,
        audioUrl = audio_url,
        sensesJson = sensesJson,
        difficultyValue = difficulty_value,
        masterId = master_id,
        exampleTranslation = example_translation.orEmpty()
    )
}

private fun WordBookEntity.toManagedWordBook(): ManagedWordBook {
    return ManagedWordBook(
        bookId = bookId,
        title = title,
        description = description,
        language = language,
        difficulty = difficulty,
        version = version,
        sourceType = sourceType.toWordBookSourceType(),
        downloadStatus = downloadStatus.toWordBookDownloadStatus(),
        isActive = isActive,
        wordCount = wordCount,
        updatedAt = updatedAt,
        coverUrl = coverUrl
    )
}
