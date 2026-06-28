package com.example.seedie.data.repository

import com.example.seedie.data.local.dao.VocabularyWordDao
import com.example.seedie.data.local.dao.WordBookDao
import com.example.seedie.data.local.entity.VocabularyWordEntity
import com.example.seedie.data.local.entity.WordBookEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WordBookSeeder @Inject constructor(
    private val wordBookDao: WordBookDao,
    private val vocabularyWordDao: VocabularyWordDao
) {
    suspend fun ensureSeeded() {
        if (wordBookDao.getBookCount() > 0) return
        wordBookDao.insertBook(VocabularyStaticWordPack.defaultBookEntity())
        vocabularyWordDao.insertWords(VocabularyStaticWordPack.defaultWordEntities())
    }

    suspend fun loadActiveBook(): WordBookEntity {
        ensureSeeded()
        return wordBookDao.getActiveBook() ?: wordBookDao.getLatestBook()
            ?: error("未找到可用词书")
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
