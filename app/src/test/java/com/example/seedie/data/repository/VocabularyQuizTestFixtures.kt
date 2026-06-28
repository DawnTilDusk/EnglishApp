package com.example.seedie.data.repository

import com.example.seedie.data.local.entity.VocabularyWordEntity

internal object VocabularyQuizTestFixtures {
    fun wordBank24(): List<VocabularyWordEntity> {
        return buildList {
            for (index in 1..8) {
                add(word("w$index", "easy$index", "简单$index", "easy", index))
            }
            for (index in 9..16) {
                add(word("w$index", "medium$index", "中等$index", "medium", index))
            }
            for (index in 17..24) {
                add(word("w$index", "hard$index", "困难$index", "hard", index))
            }
        }
    }

    private fun word(
        wordId: String,
        english: String,
        translation: String,
        difficultyLevel: String,
        sortOrder: Int
    ): VocabularyWordEntity {
        return VocabularyWordEntity(
            wordId = wordId,
            bookId = "seedie-default-book",
            english = english,
            phonetic = "/test/",
            partOfSpeech = "n.",
            translation = translation,
            exampleSentence = "Example $english",
            difficultyLevel = difficultyLevel,
            rewardToken = 3,
            estimatedDurationSec = 8,
            sortOrder = sortOrder
        )
    }
}
