package com.example.seedie.data.repository

import com.example.seedie.data.local.entity.VocabularyWordEntity
import com.example.seedie.data.local.entity.senseDisplayLabel
import com.example.seedie.ui.screens.learning.practice.VocabularyPracticeOption
import com.example.seedie.ui.screens.learning.practice.VocabularyPracticeWord
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class VocabularyOptionBuilder @Inject constructor() {

    fun toPracticeWord(
        entity: VocabularyWordEntity,
        allEntries: List<VocabularyWordEntity>,
        random: Random
    ): VocabularyPracticeWord {
        val distractorPool = buildDistractorPool(entity, allEntries, random)
        val translationOptions = buildTranslationOptionsFromPool(entity, distractorPool, random)
        val englishOptions = buildEnglishOptionsFromPool(entity, distractorPool, random)
        val contextOptions = buildContextOptions(entity, distractorPool, random)
        val label = entity.senseDisplayLabel()

        return VocabularyPracticeWord(
            wordId = entity.wordId,
            bookId = entity.bookId,
            english = entity.english,
            phonetic = entity.phonetic,
            partOfSpeech = entity.partOfSpeech,
            translation = entity.translation,
            senseDisplayLabel = label,
            exampleSentence = entity.exampleSentence,
            difficultyLevel = entity.difficultyLevel,
            rewardToken = entity.rewardToken,
            estimatedDurationSec = entity.estimatedDurationSec,
            sortOrder = entity.sortOrder,
            translationOptions = translationOptions,
            englishOptions = englishOptions,
            contextOptions = contextOptions,
            contextSentence = buildContextSentence(
                exampleSentence = entity.exampleSentence,
                answer = entity.english
            )
        )
    }

    fun buildEnglishOptions(
        entity: VocabularyWordEntity,
        allEntries: List<VocabularyWordEntity>,
        random: Random
    ): List<VocabularyPracticeOption> {
        val distractorPool = buildDistractorPool(entity, allEntries, random)
        return buildEnglishOptionsFromPool(entity, distractorPool, random)
    }

    fun buildTranslationOptions(
        entity: VocabularyWordEntity,
        allEntries: List<VocabularyWordEntity>,
        random: Random
    ): List<VocabularyPracticeOption> {
        val distractorPool = buildDistractorPool(entity, allEntries, random)
        return buildTranslationOptionsFromPool(entity, distractorPool, random)
    }

    private fun buildDistractorPool(
        entity: VocabularyWordEntity,
        allEntries: List<VocabularyWordEntity>,
        random: Random
    ): List<VocabularyWordEntity> {
        val entityLabel = entity.senseDisplayLabel()
        return allEntries
            .asSequence()
            .filter {
                it.wordId != entity.wordId && it.senseDisplayLabel() != entityLabel
            }
            .sortedByDescending { candidate -> candidate.partOfSpeech == entity.partOfSpeech }
            .distinctBy { candidate -> candidate.english }
            .toList()
            .shuffled(random)
            .take(3)
    }

    private fun buildTranslationOptionsFromPool(
        entity: VocabularyWordEntity,
        distractorPool: List<VocabularyWordEntity>,
        random: Random
    ): List<VocabularyPracticeOption> {
        val options = (distractorPool + entity)
            .distinctBy { entry -> entry.senseDisplayLabel() }
            .shuffled(random)
            .mapIndexed { optionIndex, entry ->
                VocabularyPracticeOption(
                    optionId = "translation_${entity.wordId}_${optionIndex + 1}",
                    label = entry.senseDisplayLabel(),
                    isCorrect = entry.wordId == entity.wordId,
                    englishHint = entry.english
                )
            }
        require(options.size == 4) {
            "Translation options must contain exactly 4 items for ${entity.wordId}, got ${options.size}"
        }
        return options
    }

    private fun buildEnglishOptionsFromPool(
        entity: VocabularyWordEntity,
        distractorPool: List<VocabularyWordEntity>,
        random: Random
    ): List<VocabularyPracticeOption> {
        val options = (distractorPool + entity)
            .distinctBy { entry -> entry.english }
            .shuffled(random)
            .mapIndexed { optionIndex, entry ->
                VocabularyPracticeOption(
                    optionId = "english_${entity.wordId}_${optionIndex + 1}",
                    label = entry.english,
                    isCorrect = entry.wordId == entity.wordId,
                    englishHint = entry.senseDisplayLabel()
                )
            }
        require(options.size == 4) {
            "English options must contain exactly 4 items for ${entity.wordId}, got ${options.size}"
        }
        return options
    }

    private fun buildContextOptions(
        entity: VocabularyWordEntity,
        distractorPool: List<VocabularyWordEntity>,
        random: Random
    ): List<VocabularyPracticeOption> {
        return (distractorPool + entity)
            .distinctBy { entry -> entry.english }
            .shuffled(random)
            .mapIndexed { optionIndex, entry ->
                VocabularyPracticeOption(
                    optionId = "context_${entity.wordId}_${optionIndex + 1}",
                    label = entry.english,
                    isCorrect = entry.wordId == entity.wordId,
                    englishHint = entry.senseDisplayLabel()
                )
            }
    }

    private fun buildContextSentence(exampleSentence: String, answer: String): String {
        val answerPattern = Regex(
            pattern = "\\b${Regex.escape(answer)}(s|es|ed|ing)?\\b",
            option = RegexOption.IGNORE_CASE
        )
        return if (answerPattern.containsMatchIn(exampleSentence)) {
            exampleSentence.replaceFirst(answerPattern, "_____")
        } else {
            "_____  $exampleSentence"
        }
    }
}
