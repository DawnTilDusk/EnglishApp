package com.example.seedie.data.repository

import com.example.seedie.data.local.entity.VocabularyWordEntity
import com.example.seedie.domain.quiz.VocabularyQuizConstants
import com.example.seedie.domain.quiz.VocabularyQuizWordSelector
import com.example.seedie.ui.screens.learning.quiz.VocabularyQuizQuestion
import com.example.seedie.ui.screens.learning.quiz.VocabularyQuizSession
import kotlin.random.Random

internal object VocabularyQuizSessionFactory {
    fun create(
        sessionId: String,
        wordBank: List<VocabularyWordEntity>,
        optionBuilder: VocabularyOptionBuilder,
        questionCount: Int = VocabularyQuizConstants.QUESTION_COUNT,
        difficulty: String = "mixed"
    ): VocabularyQuizSession {
        require(wordBank.size >= 4) { "词书词量不足，至少需要 4 个单词" }

        val random = Random(sessionId.hashCode())
        val normalizedDifficulty = difficulty.trim().lowercase()
        val selected = when (normalizedDifficulty) {
            "", "mixed", "all" -> {
                val balanced = VocabularyQuizWordSelector.selectBalanced(
                    wordBank = wordBank,
                    random = random
                )
                require(balanced.size == questionCount) {
                    "分层抽样不足 $questionCount 题，当前仅有 ${balanced.size} 题"
                }
                balanced
            }
            else -> VocabularyQuizWordSelector.selectRandom(
                wordBank = wordBank,
                random = random,
                questionCount = questionCount
            )
        }

        val ordered = selected.shuffled(random)
        val questions = ordered.mapIndexed { index, word ->
            VocabularyQuizQuestion(
                questionId = "${sessionId}_q_${index + 1}",
                wordId = word.wordId,
                english = word.english,
                phonetic = word.phonetic,
                partOfSpeech = word.partOfSpeech,
                translation = word.translation,
                difficultyLevel = word.difficultyLevel,
                rewardToken = word.rewardToken,
                options = optionBuilder.buildTranslationOptions(
                    entity = word,
                    allEntries = wordBank,
                    random = random
                )
            )
        }

        return VocabularyQuizSession(
            sessionId = sessionId,
            questions = questions
        )
    }
}
