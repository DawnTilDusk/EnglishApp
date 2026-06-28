package com.example.seedie.data.repository

import com.example.seedie.data.local.BundledWordAudioResolver
import com.example.seedie.domain.repository.ListeningPracticeRepository
import com.example.seedie.ui.screens.learning.listening.ListeningPracticeSession
import com.example.seedie.ui.screens.learning.listening.ListeningQuestion
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class ListeningPracticeRepositoryImpl @Inject constructor(
    private val wordBookSeeder: WordBookSeeder,
    private val vocabularyOptionBuilder: VocabularyOptionBuilder,
    private val bundledWordAudioResolver: BundledWordAudioResolver
) : ListeningPracticeRepository {

    override suspend fun createSession(
        questionCount: Int,
        difficulty: String,
        sessionId: String
    ): ListeningPracticeSession {
        val activeBook = wordBookSeeder.loadActiveBook()
        val wordBank = wordBookSeeder.loadWordsForBook(activeBook.bookId)
        if (wordBank.size < 4) {
            error("词书词量不足，至少需要 4 个单词")
        }

        val filteredPool = wordBookSeeder.filterByDifficulty(
            wordBank = wordBank,
            requestedDifficulty = difficulty
        )
        if (filteredPool.isEmpty()) {
            error("当前难度下没有可用单词")
        }

        val random = Random(sessionId.hashCode())
        val actualCount = questionCount.coerceAtMost(filteredPool.size)
        val selectedWords = filteredPool.shuffled(random).take(actualCount)

        val questions = selectedWords.mapIndexed { index, word ->
            ListeningQuestion(
                questionId = "${sessionId}_q_${index + 1}",
                wordId = word.wordId,
                english = word.english,
                phonetic = word.phonetic,
                translation = word.translation,
                rewardToken = word.rewardToken,
                options = vocabularyOptionBuilder.buildEnglishOptions(
                    entity = word,
                    allEntries = wordBank,
                    random = random
                ),
                audioRawResId = bundledWordAudioResolver.rawResId(word.wordId)
            )
        }

        return ListeningPracticeSession(
            sessionId = sessionId,
            questions = questions
        )
    }
}
