package com.example.seedie.data.repository

import com.example.seedie.data.local.dao.VocabularyWordDao
import com.example.seedie.data.local.dao.WordBookDao
import com.example.seedie.data.local.entity.VocabularyWordEntity
import com.example.seedie.domain.model.StudyResult
import com.example.seedie.domain.repository.VocabularyPracticeRepository
import com.example.seedie.ui.screens.learning.practice.VocabularyPracticeArgs
import com.example.seedie.ui.screens.learning.practice.VocabularyPracticeOption
import com.example.seedie.ui.screens.learning.practice.VocabularyPracticeSession
import com.example.seedie.ui.screens.learning.practice.VocabularyPracticeWord
import com.example.seedie.ui.screens.learning.practice.VocabularyQuestionRecord
import com.example.seedie.ui.screens.learning.practice.VocabularySessionMeta
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class VocabularyPracticeRepositoryImpl @Inject constructor(
    private val wordBookDao: WordBookDao,
    private val vocabularyWordDao: VocabularyWordDao
) : VocabularyPracticeRepository {
    private companion object {
        const val STUDY_GROUP_SIZE = 10
        const val DEFAULT_REVIEW_COUNT = 5
    }

    private val questionRecords = linkedMapOf<String, MutableList<VocabularyQuestionRecord>>()
    private val completedResults = linkedMapOf<String, StudyResult>()

    override suspend fun getPracticeSession(args: VocabularyPracticeArgs): VocabularyPracticeSession {
        val sessionId = args.sessionId ?: UUID.randomUUID().toString()
        val wordBank = loadWordBank()
        val requestedDifficulty = args.difficulty.trim().lowercase()
        val filteredPool = when (requestedDifficulty) {
            "", "mixed", "all" -> wordBank
            else -> wordBank.filter { it.difficultyLevel == requestedDifficulty }
        }.ifEmpty { wordBank }

        val random = Random(sessionId.hashCode())
        val shuffledPool = filteredPool.shuffled(random)
        val studyEntries = shuffledPool.take(STUDY_GROUP_SIZE.coerceAtMost(shuffledPool.size))
        val reviewTarget = args.wordCountTarget.coerceAtLeast(DEFAULT_REVIEW_COUNT)
        val reviewEntries = shuffledPool
            .drop(studyEntries.size)
            .take(reviewTarget.coerceAtMost((shuffledPool.size - studyEntries.size).coerceAtLeast(0)))

        return VocabularyPracticeSession(
            sessionMeta = VocabularySessionMeta(
                sessionId = sessionId,
                moduleId = args.sourceModuleId,
                startedAt = System.currentTimeMillis(),
                targetWordCount = studyEntries.size,
                difficulty = args.difficulty,
                source = args.planId ?: "learning_hub",
                resumeSupported = false
            ),
            studyWords = studyEntries.map { entry ->
                entry.toPracticeWord(allEntries = wordBank, random = random)
            },
            reviewWords = reviewEntries.map { entry ->
                entry.toPracticeWord(allEntries = wordBank, random = random)
            }
        )
    }

    override suspend fun submitQuestionRecord(record: VocabularyQuestionRecord) {
        val sessionRecords = questionRecords.getOrPut(record.sessionId) { mutableListOf() }
        sessionRecords.removeAll { it.promptId == record.promptId }
        sessionRecords += record
    }

    override suspend fun finishPracticeSession(result: StudyResult) {
        completedResults[result.sessionId] = result
    }

    override suspend fun getWrongWords(sessionId: String): List<String> {
        return questionRecords[sessionId]
            ?.filterNot { it.isCorrect }
            ?.map { it.wordId }
            .orEmpty()
    }

    private suspend fun loadWordBank(): List<VocabularyWordEntity> {
        ensureSeededWordBook()
        val activeBook = wordBookDao.getActiveBook() ?: wordBookDao.getLatestBook()
            ?: error("未找到可用词书")
        return vocabularyWordDao.getWordsByBook(activeBook.bookId)
            .ifEmpty { error("当前词书没有任何单词") }
    }

    private suspend fun ensureSeededWordBook() {
        if (wordBookDao.getBookCount() > 0) return
        wordBookDao.insertBook(VocabularyStaticWordPack.defaultBookEntity())
        vocabularyWordDao.insertWords(VocabularyStaticWordPack.defaultWordEntities())
    }

    private fun VocabularyWordEntity.toPracticeWord(
        allEntries: List<VocabularyWordEntity>,
        random: Random
    ): VocabularyPracticeWord {
        val distractorPool = buildDistractorPool(allEntries = allEntries, random = random)
        val translationOptions = buildTranslationOptions(distractorPool = distractorPool, random = random)
        val englishOptions = buildEnglishOptions(distractorPool = distractorPool, random = random)
        val contextOptions = buildContextOptions(distractorPool = distractorPool, random = random)

        return VocabularyPracticeWord(
            wordId = wordId,
            english = english,
            phonetic = phonetic,
            partOfSpeech = partOfSpeech,
            translation = translation,
            exampleSentence = exampleSentence,
            difficultyLevel = difficultyLevel,
            rewardToken = rewardToken,
            estimatedDurationSec = estimatedDurationSec,
            translationOptions = translationOptions,
            englishOptions = englishOptions,
            contextOptions = contextOptions,
            contextSentence = buildContextSentence(exampleSentence = exampleSentence, answer = english)
        )
    }

    private fun VocabularyWordEntity.buildDistractorPool(
        allEntries: List<VocabularyWordEntity>,
        random: Random
    ): List<VocabularyWordEntity> {
        val distractorPool = allEntries
            .asSequence()
            .filter { it.wordId != wordId && it.translation != translation }
            .sortedByDescending { candidate -> candidate.partOfSpeech == partOfSpeech }
            .distinctBy { candidate -> candidate.translation }
            .toList()
            .shuffled(random)
            .take(3)
        return distractorPool
    }

    private fun VocabularyWordEntity.buildTranslationOptions(
        distractorPool: List<VocabularyWordEntity>,
        random: Random
    ): List<VocabularyPracticeOption> {
        return (distractorPool + this)
            .distinctBy { entry -> entry.translation }
            .shuffled(random)
            .mapIndexed { optionIndex, entry ->
                VocabularyPracticeOption(
                    optionId = "translation_${wordId}_${optionIndex + 1}",
                    label = entry.translation,
                    isCorrect = entry.wordId == wordId,
                    englishHint = entry.english
                )
            }
    }

    private fun VocabularyWordEntity.buildEnglishOptions(
        distractorPool: List<VocabularyWordEntity>,
        random: Random
    ): List<VocabularyPracticeOption> {
        return (distractorPool + this)
            .distinctBy { entry -> entry.english }
            .shuffled(random)
            .mapIndexed { optionIndex, entry ->
                VocabularyPracticeOption(
                    optionId = "english_${wordId}_${optionIndex + 1}",
                    label = entry.english,
                    isCorrect = entry.wordId == wordId,
                    englishHint = entry.translation
                )
            }
    }

    private fun VocabularyWordEntity.buildContextOptions(
        distractorPool: List<VocabularyWordEntity>,
        random: Random
    ): List<VocabularyPracticeOption> {
        return (distractorPool + this)
            .distinctBy { entry -> entry.english }
            .shuffled(random)
            .mapIndexed { optionIndex, entry ->
                VocabularyPracticeOption(
                    optionId = "context_${wordId}_${optionIndex + 1}",
                    label = entry.english,
                    isCorrect = entry.wordId == wordId,
                    englishHint = entry.translation
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
            "_____  ${exampleSentence}"
        }
    }
}
