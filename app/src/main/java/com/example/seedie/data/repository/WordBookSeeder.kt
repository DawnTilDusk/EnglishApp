package com.example.seedie.data.repository

import com.example.seedie.data.local.dao.VocabularyWordDao
import com.example.seedie.data.local.dao.WordBookDao
import com.example.seedie.data.local.entity.VocabularyWordEntity
import com.example.seedie.data.local.entity.WordBookEntity
import com.example.seedie.domain.repository.WordBookDownloadStatus
import com.example.seedie.domain.repository.asStorageValue
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WordBookSeeder @Inject constructor(
    private val wordBookDao: WordBookDao,
    private val vocabularyWordDao: VocabularyWordDao
) {
    suspend fun ensureSeeded() {
        if (wordBookDao.getBookById(VocabularyStaticWordPack.DEFAULT_BOOK_ID) == null) {
            wordBookDao.insertBook(
                VocabularyStaticWordPack.defaultBookEntity().copy(
                    isActive = wordBookDao.getActiveBook() == null
                )
            )
        }
        if (vocabularyWordDao.getWordCountByBook(VocabularyStaticWordPack.DEFAULT_BOOK_ID) == 0) {
            vocabularyWordDao.insertWords(VocabularyStaticWordPack.defaultWordEntities())
        }
    }

    suspend fun loadActiveBook(): WordBookEntity {
        ensureSeeded()
        val downloadedStatus = WordBookDownloadStatus.DOWNLOADED.asStorageValue()
        val ready = wordBookDao.getActiveBook()?.takeIf { it.downloadStatus == downloadedStatus }
            ?: wordBookDao.getLatestBookByDownloadStatus(downloadedStatus)
        if (ready != null) return ready
        return activateBundledBook()
    }

    private suspend fun activateBundledBook(): WordBookEntity {
        val bundled = VocabularyStaticWordPack.defaultBookEntity().copy(isActive = true)
        wordBookDao.deactivateAllBooks()
        wordBookDao.insertBook(bundled)
        if (vocabularyWordDao.getWordCountByBook(bundled.bookId) == 0) {
            vocabularyWordDao.insertWords(VocabularyStaticWordPack.defaultWordEntities())
        }
        return wordBookDao.getBookById(bundled.bookId) ?: bundled
    }

    suspend fun loadWordsForBook(bookId: String): List<VocabularyWordEntity> {
        return vocabularyWordDao.getWordsByBook(bookId)
    }

    fun filterByDifficulty(
        wordBank: List<VocabularyWordEntity>,
        requestedDifficulty: String
    ): List<VocabularyWordEntity> {
        return when (requestedDifficulty.trim().lowercase()) {
            "", "mixed", "all" -> wordBank
            else -> wordBank.filter { it.difficultyLevel == requestedDifficulty.trim().lowercase() }
        }.ifEmpty { wordBank }
    }
}
