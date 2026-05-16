package com.example.seedie.data.repository

import androidx.room.withTransaction
import com.example.seedie.data.local.SeedieDatabase
import com.example.seedie.data.local.dao.VocabularyBookProgressDao
import com.example.seedie.data.local.dao.VocabularyStudyRoundDao
import com.example.seedie.data.local.dao.VocabularyStudyRoundWordDao
import com.example.seedie.data.local.dao.VocabularyWordDao
import com.example.seedie.data.local.dao.VocabularyWordLearningProgressDao
import com.example.seedie.data.local.dao.WordBookDao
import com.example.seedie.data.local.entity.VocabularyBookProgressEntity
import com.example.seedie.data.local.entity.VocabularyStudyRoundEntity
import com.example.seedie.data.local.entity.VocabularyStudyRoundWordEntity
import com.example.seedie.data.local.entity.VocabularyWordEntity
import com.example.seedie.data.local.entity.VocabularyWordLearningProgressEntity
import com.example.seedie.data.local.entity.WordBookEntity
import com.example.seedie.domain.model.StudyResult
import com.example.seedie.domain.repository.VocabularyPracticeRepository
import com.example.seedie.ui.screens.learning.practice.VocabularyPracticeArgs
import com.example.seedie.ui.screens.learning.practice.VocabularyPracticeOption
import com.example.seedie.ui.screens.learning.practice.VocabularyPracticeResumeSnapshot
import com.example.seedie.ui.screens.learning.practice.VocabularyPracticeSession
import com.example.seedie.ui.screens.learning.practice.VocabularyPracticeWord
import com.example.seedie.ui.screens.learning.practice.VocabularyQuestionRecord
import com.example.seedie.ui.screens.learning.practice.VocabularyQuestionType
import com.example.seedie.ui.screens.learning.practice.VocabularyResumeWordProgress
import com.example.seedie.ui.screens.learning.practice.VocabularySessionMeta
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class VocabularyPracticeRepositoryImpl @Inject constructor(
    private val database: SeedieDatabase,
    private val wordBookDao: WordBookDao,
    private val vocabularyWordDao: VocabularyWordDao,
    private val vocabularyBookProgressDao: VocabularyBookProgressDao,
    private val vocabularyWordLearningProgressDao: VocabularyWordLearningProgressDao,
    private val vocabularyStudyRoundDao: VocabularyStudyRoundDao,
    private val vocabularyStudyRoundWordDao: VocabularyStudyRoundWordDao
) : VocabularyPracticeRepository {
    private companion object {
        const val STAGE_STUDY_TARGET_COUNT = 10
        const val STAGE_ACTIVE_QUEUE_SIZE = 4
        const val WORD_STATUS_NEW = "NEW"
        const val WORD_STATUS_LEARNING = "LEARNING"
        const val WORD_STATUS_LEARNED = "LEARNED"
        const val ROUND_STATUS_ACTIVE = "ACTIVE"
        const val ROUND_STATUS_COMPLETED = "COMPLETED"
        const val CURSOR_END_SENTINEL = Int.MAX_VALUE
    }

    private val questionRecords = linkedMapOf<String, MutableList<VocabularyQuestionRecord>>()
    private val completedResults = linkedMapOf<String, StudyResult>()

    override suspend fun getPracticeSession(args: VocabularyPracticeArgs): VocabularyPracticeSession {
        val sessionId = args.sessionId ?: UUID.randomUUID().toString()
        val activeBook = loadActiveBook()
        val wordBank = vocabularyWordDao.getWordsByBook(activeBook.bookId)
            .ifEmpty { error("当前词书没有任何单词") }
        val filteredPool = filterByDifficulty(
            wordBank = wordBank,
            requestedDifficulty = args.difficulty
        )
        val random = Random(sessionId.hashCode())
        val now = System.currentTimeMillis()
        val initialCursor = filteredPool.firstOrNull()?.sortOrder
            ?: wordBank.firstOrNull()?.sortOrder
            ?: error("当前词书没有任何单词")
        val bookProgress = vocabularyBookProgressDao.getProgressByBookId(activeBook.bookId)
            ?: VocabularyBookProgressEntity(
                bookId = activeBook.bookId,
                nextWordSortOrderCursor = initialCursor,
                activeRoundId = null,
                learnedWordCount = 0,
                updatedAt = now
            ).also { vocabularyBookProgressDao.insertOrReplace(it) }

        val activeRoundId = bookProgress.activeRoundId
        if (activeRoundId != null) {
            val activeRound = vocabularyStudyRoundDao.getRoundById(activeRoundId)
            if (activeRound != null && activeRound.status == ROUND_STATUS_ACTIVE) {
                val roundWords = vocabularyStudyRoundWordDao.getRoundWords(activeRoundId)
                return buildResumedSession(
                    sessionId = sessionId,
                    args = args,
                    activeBook = activeBook,
                    wordBank = wordBank,
                    filteredPool = filteredPool,
                    random = random,
                    activeRound = activeRound,
                    roundWords = roundWords
                )
            }

            vocabularyBookProgressDao.insertOrReplace(
                bookProgress.copy(
                    activeRoundId = null,
                    updatedAt = now
                )
            )
        }

        return createNewStudySession(
            sessionId = sessionId,
            args = args,
            activeBook = activeBook,
            wordBank = wordBank,
            filteredPool = filteredPool,
            random = random,
            bookProgress = bookProgress
        )
    }

    override suspend fun saveStudyRoundSnapshot(snapshot: VocabularyPracticeResumeSnapshot) {
        val now = System.currentTimeMillis()
        database.withTransaction {
            val round = vocabularyStudyRoundDao.getRoundById(snapshot.roundId) ?: return@withTransaction
            val existingRoundWords = vocabularyStudyRoundWordDao.getRoundWords(snapshot.roundId)
            val activeWordIds = snapshot.activeWordIds.toSet()
            val removedWordIds = existingRoundWords
                .map { it.wordId }
                .filterNot { it in activeWordIds }

            vocabularyStudyRoundDao.insertOrReplace(
                round.copy(
                    introducedWordCount = snapshot.introducedStudyCount,
                    masteredWordCount = snapshot.masteredStudyCount,
                    targetWordCount = snapshot.studyTargetCount,
                    nextWordSortOrderCursor = snapshot.nextWordSortOrderCursor,
                    updatedAt = now
                )
            )

            val progressByWordId = snapshot.wordProgressList.associateBy { it.wordId }
            vocabularyStudyRoundWordDao.deleteByRoundId(snapshot.roundId)
            vocabularyStudyRoundWordDao.insertOrReplace(
                snapshot.activeWordIds.mapIndexedNotNull { queueOrder, wordId ->
                    val progress = progressByWordId[wordId] ?: return@mapIndexedNotNull null
                    VocabularyStudyRoundWordEntity(
                        roundId = snapshot.roundId,
                        wordId = wordId,
                        queueOrder = queueOrder,
                        passedStages = serializePassedStages(progress.passedStudyQuestionTypes),
                        hasSeenStudyWord = progress.hasSeenStudyWord,
                        totalWrongCount = progress.totalWrongCount,
                        revealCount = progress.revealCount,
                        isMasteredInRound = false,
                        updatedAt = now
                    )
                }
            )

            val activeProgressRows = snapshot.activeWordIds.map {
                VocabularyWordLearningProgressEntity(
                    bookId = snapshot.bookId,
                    wordId = it,
                    status = WORD_STATUS_LEARNING,
                    lastStudiedAt = now,
                    learnedAt = null
                )
            }
            val removedProgressRows = removedWordIds.map {
                VocabularyWordLearningProgressEntity(
                    bookId = snapshot.bookId,
                    wordId = it,
                    status = WORD_STATUS_LEARNED,
                    lastStudiedAt = now,
                    learnedAt = now
                )
            }
            vocabularyWordLearningProgressDao.insertOrReplace(activeProgressRows + removedProgressRows)

            val currentBookProgress = vocabularyBookProgressDao.getProgressByBookId(snapshot.bookId)
                ?: VocabularyBookProgressEntity(
                    bookId = snapshot.bookId,
                    nextWordSortOrderCursor = snapshot.nextWordSortOrderCursor,
                    activeRoundId = snapshot.roundId,
                    learnedWordCount = 0,
                    updatedAt = now
                )
            vocabularyBookProgressDao.insertOrReplace(
                currentBookProgress.copy(
                    activeRoundId = snapshot.roundId,
                    updatedAt = now
                )
            )
        }
    }

    override suspend fun completeStudyRound(roundId: String) {
        val now = System.currentTimeMillis()
        database.withTransaction {
            val round = vocabularyStudyRoundDao.getRoundById(roundId) ?: return@withTransaction
            vocabularyStudyRoundDao.insertOrReplace(
                round.copy(
                    status = ROUND_STATUS_COMPLETED,
                    updatedAt = now
                )
            )
            vocabularyStudyRoundWordDao.deleteByRoundId(roundId)

            val currentBookProgress = vocabularyBookProgressDao.getProgressByBookId(round.bookId)
                ?: VocabularyBookProgressEntity(
                    bookId = round.bookId,
                    nextWordSortOrderCursor = round.nextWordSortOrderCursor,
                    activeRoundId = null,
                    learnedWordCount = 0,
                    updatedAt = now
                )
            vocabularyBookProgressDao.insertOrReplace(
                currentBookProgress.copy(
                    nextWordSortOrderCursor = round.nextWordSortOrderCursor,
                    activeRoundId = null,
                    learnedWordCount = currentBookProgress.learnedWordCount + round.masteredWordCount,
                    updatedAt = now
                )
            )
        }
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

    private suspend fun buildResumedSession(
        sessionId: String,
        args: VocabularyPracticeArgs,
        activeBook: WordBookEntity,
        wordBank: List<VocabularyWordEntity>,
        filteredPool: List<VocabularyWordEntity>,
        random: Random,
        activeRound: VocabularyStudyRoundEntity,
        roundWords: List<VocabularyStudyRoundWordEntity>
    ): VocabularyPracticeSession {
        val activeWordIds = roundWords.map { it.wordId }
        val activeWordEntryMap = vocabularyWordDao.getWordsByIds(activeBook.bookId, activeWordIds)
            .associateBy { it.wordId }
        val activeEntries = activeWordIds.mapNotNull { activeWordEntryMap[it] }
        val remainingUnintroducedCount =
            (activeRound.targetWordCount - activeRound.introducedWordCount).coerceAtLeast(0)
        val upcomingEntries = if (
            activeRound.nextWordSortOrderCursor == CURSOR_END_SENTINEL || remainingUnintroducedCount == 0
        ) {
            emptyList()
        } else {
            filteredPool
                .filter { it.sortOrder >= activeRound.nextWordSortOrderCursor }
                .take(remainingUnintroducedCount)
        }

        return VocabularyPracticeSession(
            sessionMeta = VocabularySessionMeta(
                sessionId = sessionId,
                moduleId = args.sourceModuleId,
                startedAt = System.currentTimeMillis(),
                targetWordCount = activeRound.targetWordCount,
                difficulty = args.difficulty,
                source = args.planId ?: "learning_hub",
                resumeSupported = true
            ),
            studyWords = (activeEntries + upcomingEntries).map { entry ->
                entry.toPracticeWord(allEntries = wordBank, random = random)
            },
            reviewWords = emptyList(),
            resumeSnapshot = VocabularyPracticeResumeSnapshot(
                roundId = activeRound.roundId,
                bookId = activeBook.bookId,
                activeWordIds = activeWordIds,
                introducedStudyCount = activeRound.introducedWordCount,
                studyTargetCount = activeRound.targetWordCount,
                nextWordSortOrderCursor = activeRound.nextWordSortOrderCursor,
                masteredStudyCount = activeRound.masteredWordCount,
                wordProgressList = roundWords.map { roundWord ->
                    VocabularyResumeWordProgress(
                        wordId = roundWord.wordId,
                        passedStudyQuestionTypes = deserializePassedStages(roundWord.passedStages),
                        hasSeenStudyWord = roundWord.hasSeenStudyWord,
                        totalWrongCount = roundWord.totalWrongCount,
                        revealCount = roundWord.revealCount
                    )
                }
            )
        )
    }

    private suspend fun createNewStudySession(
        sessionId: String,
        args: VocabularyPracticeArgs,
        activeBook: WordBookEntity,
        wordBank: List<VocabularyWordEntity>,
        filteredPool: List<VocabularyWordEntity>,
        random: Random,
        bookProgress: VocabularyBookProgressEntity
    ): VocabularyPracticeSession {
        val startSortOrder = filteredPool.firstOrNull { it.sortOrder >= bookProgress.nextWordSortOrderCursor }
            ?.sortOrder
            ?: filteredPool.firstOrNull()?.sortOrder
            ?: error("当前词书没有任何单词")
        val studyEntries = filteredPool
            .filter { it.sortOrder >= startSortOrder }
            .take(STAGE_STUDY_TARGET_COUNT)
            .ifEmpty { filteredPool.take(STAGE_STUDY_TARGET_COUNT) }
        val roundId = UUID.randomUUID().toString()
        val introducedEntries = studyEntries.take(STAGE_ACTIVE_QUEUE_SIZE.coerceAtMost(studyEntries.size))
        val nextCursor = studyEntries.getOrNull(introducedEntries.size)?.sortOrder ?: CURSOR_END_SENTINEL
        val now = System.currentTimeMillis()

        database.withTransaction {
            vocabularyStudyRoundDao.insertOrReplace(
                VocabularyStudyRoundEntity(
                    roundId = roundId,
                    bookId = activeBook.bookId,
                    status = ROUND_STATUS_ACTIVE,
                    targetWordCount = studyEntries.size,
                    introducedWordCount = introducedEntries.size,
                    masteredWordCount = 0,
                    activeQueueSize = STAGE_ACTIVE_QUEUE_SIZE,
                    nextWordSortOrderCursor = nextCursor,
                    createdAt = now,
                    updatedAt = now
                )
            )
            vocabularyStudyRoundWordDao.deleteByRoundId(roundId)
            vocabularyStudyRoundWordDao.insertOrReplace(
                introducedEntries.mapIndexed { queueOrder, entry ->
                    VocabularyStudyRoundWordEntity(
                        roundId = roundId,
                        wordId = entry.wordId,
                        queueOrder = queueOrder,
                        passedStages = "",
                        hasSeenStudyWord = false,
                        totalWrongCount = 0,
                        revealCount = 0,
                        isMasteredInRound = false,
                        updatedAt = now
                    )
                }
            )
            vocabularyBookProgressDao.insertOrReplace(
                bookProgress.copy(
                    activeRoundId = roundId,
                    updatedAt = now
                )
            )
            vocabularyWordLearningProgressDao.insertOrReplace(
                introducedEntries.map { entry ->
                    VocabularyWordLearningProgressEntity(
                        bookId = activeBook.bookId,
                        wordId = entry.wordId,
                        status = WORD_STATUS_LEARNING,
                        lastStudiedAt = now,
                        learnedAt = null
                    )
                }
            )
        }

        return VocabularyPracticeSession(
            sessionMeta = VocabularySessionMeta(
                sessionId = sessionId,
                moduleId = args.sourceModuleId,
                startedAt = now,
                targetWordCount = studyEntries.size,
                difficulty = args.difficulty,
                source = args.planId ?: "learning_hub",
                resumeSupported = true
            ),
            studyWords = studyEntries.map { entry ->
                entry.toPracticeWord(allEntries = wordBank, random = random)
            },
            reviewWords = emptyList(),
            resumeSnapshot = VocabularyPracticeResumeSnapshot(
                roundId = roundId,
                bookId = activeBook.bookId,
                activeWordIds = introducedEntries.map { it.wordId },
                introducedStudyCount = introducedEntries.size,
                studyTargetCount = studyEntries.size,
                nextWordSortOrderCursor = nextCursor,
                masteredStudyCount = 0,
                wordProgressList = introducedEntries.map { entry ->
                    VocabularyResumeWordProgress(wordId = entry.wordId)
                }
            )
        )
    }

    private fun filterByDifficulty(
        wordBank: List<VocabularyWordEntity>,
        requestedDifficulty: String
    ): List<VocabularyWordEntity> {
        return when (requestedDifficulty.trim().lowercase()) {
            "", "mixed", "all" -> wordBank
            else -> wordBank.filter { it.difficultyLevel == requestedDifficulty.trim().lowercase() }
        }.ifEmpty { wordBank }
    }

    private suspend fun loadActiveBook(): WordBookEntity {
        ensureSeededWordBook()
        return wordBookDao.getActiveBook() ?: wordBookDao.getLatestBook()
        ?: error("未找到可用词书")
    }

    private suspend fun ensureSeededWordBook() {
        if (wordBookDao.getBookCount() > 0) return
        wordBookDao.insertBook(VocabularyStaticWordPack.defaultBookEntity())
        vocabularyWordDao.insertWords(VocabularyStaticWordPack.defaultWordEntities())
    }

    private fun serializePassedStages(questionTypes: Set<VocabularyQuestionType>): String {
        return questionTypes.joinToString(",") { it.name }
    }

    private fun deserializePassedStages(serialized: String): Set<VocabularyQuestionType> {
        if (serialized.isBlank()) return emptySet()
        return serialized.split(",")
            .mapNotNull { typeName ->
                runCatching { VocabularyQuestionType.valueOf(typeName) }.getOrNull()
            }
            .toSet()
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
            bookId = bookId,
            english = english,
            phonetic = phonetic,
            partOfSpeech = partOfSpeech,
            translation = translation,
            exampleSentence = exampleSentence,
            difficultyLevel = difficultyLevel,
            rewardToken = rewardToken,
            estimatedDurationSec = estimatedDurationSec,
            sortOrder = sortOrder,
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
        return allEntries
            .asSequence()
            .filter { it.wordId != wordId && it.translation != translation }
            .sortedByDescending { candidate -> candidate.partOfSpeech == partOfSpeech }
            .distinctBy { candidate -> candidate.translation }
            .toList()
            .shuffled(random)
            .take(3)
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
