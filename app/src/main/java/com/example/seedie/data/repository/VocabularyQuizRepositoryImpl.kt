package com.example.seedie.data.repository

import com.example.seedie.data.local.dao.VocabularyWordDao
import com.example.seedie.data.local.entity.VocabularyWordEntity
import com.example.seedie.data.remote.SupabaseVocabularyWord
import com.example.seedie.data.remote.WordBookRemoteDataSource
import com.example.seedie.domain.quiz.VocabularyQuizConstants
import com.example.seedie.domain.repository.VocabularyQuizRepository
import com.example.seedie.domain.repository.VocabularyQuizWordPool
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VocabularyQuizRepositoryImpl @Inject constructor(
    private val vocabularyWordDao: VocabularyWordDao,
    private val remoteDataSource: WordBookRemoteDataSource,
    private val wordBookSeeder: WordBookSeeder
) : VocabularyQuizRepository {

    override suspend fun loadWordPool(sessionId: String): VocabularyQuizWordPool {
        wordBookSeeder.ensureSeeded()
        val wordsByBookId = linkedMapOf<String, List<VocabularyWordEntity>>()
        val missing = mutableListOf<String>()

        for (bookId in VocabularyQuizConstants.QUIZ_BOOK_IDS) {
            // Prefer remote so gloss/senses fixes (e.g. woman) refresh Room cache.
            val remote = runCatching { remoteDataSource.fetchWordsByBook(bookId) }
                .getOrElse { emptyList() }
            val words = if (remote.isNotEmpty()) {
                val entities = remote.map { it.toQuizEntity() }
                vocabularyWordDao.insertWords(entities)
                entities
            } else {
                vocabularyWordDao.getWordsByBook(bookId)
            }
            if (words.size < VocabularyQuizConstants.WORDS_PER_BAND) {
                missing += bookId
            } else {
                wordsByBookId[bookId] = words
            }
        }

        if (missing.isNotEmpty()) {
            error("词汇检测词库未就绪，缺少：${missing.joinToString()}")
        }

        return VocabularyQuizWordPool(
            sessionId = sessionId,
            wordsByBookId = wordsByBookId
        )
    }
}

private fun SupabaseVocabularyWord.toQuizEntity(): VocabularyWordEntity {
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
        exampleTranslation = example_translation.orEmpty()
    )
}
