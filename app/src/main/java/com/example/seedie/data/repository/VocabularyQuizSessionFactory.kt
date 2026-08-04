package com.example.seedie.data.repository

import com.example.seedie.data.local.entity.VocabularyWordEntity
import com.example.seedie.data.local.entity.senseDisplayLabel
import com.example.seedie.domain.quiz.VocabularyQuizConstants
import com.example.seedie.domain.quiz.VocabularyQuizWordSelector
import com.example.seedie.domain.quiz.maybeReplaceFourthWithNoneOfAbove
import com.example.seedie.ui.screens.learning.quiz.VocabularyQuizQuestion
import com.example.seedie.ui.screens.learning.quiz.VocabularyQuizSession
import kotlin.random.Random

internal object VocabularyQuizSessionFactory {
    fun createBandQuestions(
        sessionId: String,
        bandIndex: Int,
        bookWords: List<VocabularyWordEntity>,
        distractorPool: List<VocabularyWordEntity>,
        optionBuilder: VocabularyOptionBuilder,
        questionCount: Int = VocabularyQuizConstants.WORDS_PER_BAND
    ): List<VocabularyQuizQuestion> {
        val random = Random(sessionId.hashCode() * 31 + bandIndex)
        val selected = VocabularyQuizWordSelector.selectFromBook(
            wordBank = bookWords,
            random = random,
            count = questionCount
        )
        val optionsSource = distractorPool.ifEmpty { bookWords }
        return selected.mapIndexed { index, word ->
            val baseOptions = optionBuilder.buildTranslationOptions(
                entity = word,
                allEntries = optionsSource,
                random = random
            )
            val optionRandom = Random(sessionId.hashCode() * 31 + bandIndex * 17 + index)
            VocabularyQuizQuestion(
                questionId = "${sessionId}_b${bandIndex}_q${index + 1}",
                wordId = word.wordId,
                english = word.english,
                phonetic = word.phonetic,
                partOfSpeech = word.partOfSpeech,
                translation = word.senseDisplayLabel(),
                difficultyLevel = word.difficultyLevel,
                rewardToken = word.rewardToken,
                options = maybeReplaceFourthWithNoneOfAbove(
                    options = baseOptions,
                    random = optionRandom
                )
            )
        }
    }

    @Deprecated("Use createBandQuestions")
    fun create(
        sessionId: String,
        wordBank: List<VocabularyWordEntity>,
        optionBuilder: VocabularyOptionBuilder,
        questionCount: Int = VocabularyQuizConstants.WORDS_PER_BAND,
        difficulty: String = "mixed"
    ): VocabularyQuizSession {
        val questions = createBandQuestions(
            sessionId = sessionId,
            bandIndex = 0,
            bookWords = wordBank,
            distractorPool = wordBank,
            optionBuilder = optionBuilder,
            questionCount = questionCount.coerceAtMost(wordBank.size.coerceAtLeast(1))
        )
        return VocabularyQuizSession(
            sessionId = sessionId,
            questions = questions
        )
    }
}
