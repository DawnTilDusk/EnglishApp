package com.example.seedie.domain.quiz

import com.example.seedie.data.local.entity.VocabularyWordEntity
import kotlin.random.Random

object VocabularyQuizWordSelector {
    fun selectFromBook(
        wordBank: List<VocabularyWordEntity>,
        random: Random,
        count: Int = VocabularyQuizConstants.WORDS_PER_BAND
    ): List<VocabularyWordEntity> {
        require(wordBank.size >= count) {
            "词书词量不足，需要至少 $count 个单词，当前 ${wordBank.size}"
        }
        return wordBank.shuffled(random).take(count)
    }

    @Deprecated("Use selectFromBook for grade-band quiz")
    fun selectBalanced(
        wordBank: List<VocabularyWordEntity>,
        random: Random,
        perLevel: Int = VocabularyQuizConstants.WORDS_PER_DIFFICULTY,
        levels: List<String> = VocabularyQuizConstants.DIFFICULTY_LEVELS
    ): List<VocabularyWordEntity> {
        return levels.flatMap { level ->
            wordBank
                .filter { it.difficultyLevel == level }
                .shuffled(random)
                .take(perLevel)
        }
    }

    fun selectRandom(
        wordBank: List<VocabularyWordEntity>,
        random: Random,
        questionCount: Int
    ): List<VocabularyWordEntity> {
        return wordBank.shuffled(random).take(questionCount.coerceAtMost(wordBank.size))
    }
}
