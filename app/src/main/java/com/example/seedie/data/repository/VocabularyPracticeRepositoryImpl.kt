package com.example.seedie.data.repository

import androidx.room.withTransaction
import com.example.seedie.data.local.SeedieDatabase
import com.example.seedie.data.local.dao.VocabularyBookProgressDao
import com.example.seedie.data.local.dao.VocabularyStudyRoundDao
import com.example.seedie.data.local.dao.VocabularyStudyRoundWordDao
import com.example.seedie.data.local.dao.VocabularyWordDao
import com.example.seedie.data.local.dao.VocabularyWordLearningProgressDao
import com.example.seedie.data.local.dao.WordBookDao
import com.example.seedie.data.remote.AuthService
import com.example.seedie.data.sync.SyncManager
import com.example.seedie.data.sync.SyncScope
import com.example.seedie.data.local.entity.VocabularyBookProgressEntity
import com.example.seedie.data.local.entity.VocabularyStudyRoundEntity
import com.example.seedie.data.local.entity.VocabularyStudyRoundWordEntity
import com.example.seedie.data.local.entity.VocabularyWordEntity
import com.example.seedie.data.local.entity.VocabularyWordLearningProgressEntity
import com.example.seedie.data.local.entity.WordBookEntity
import com.example.seedie.domain.model.StudyResult
import com.example.seedie.domain.repository.ReviewWordUpdateResult
import com.example.seedie.domain.repository.VocabularyPracticeRepository
import com.example.seedie.ui.screens.learning.practice.PendingReviewEntry
import com.example.seedie.ui.screens.learning.practice.VocabularyPracticeArgs
import com.example.seedie.ui.screens.learning.practice.VocabularyPracticeResumeSnapshot
import com.example.seedie.ui.screens.learning.practice.VocabularyPracticeSession
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
    private val vocabularyStudyRoundWordDao: VocabularyStudyRoundWordDao,
    private val authService: AuthService,
    private val syncManager: SyncManager,
    private val wordBookSeeder: WordBookSeeder,
    private val vocabularyOptionBuilder: VocabularyOptionBuilder
) : VocabularyPracticeRepository {

    private fun currentUserId() = authService.currentSession.value?.userId ?: ""
    private companion object {
        const val STAGE_STUDY_TARGET_COUNT = 10
        const val STAGE_ACTIVE_QUEUE_SIZE = 4
        const val WORD_STATUS_NEW = "NEW"
        const val WORD_STATUS_LEARNING = "LEARNING"
        const val WORD_STATUS_LEARNED = "LEARNED"
        const val WORD_STATUS_REVIEW_PENDING = "REVIEW_PENDING"
        const val WORD_STATUS_MASTERED = "MASTERED"
        const val ROUND_STATUS_ACTIVE = "ACTIVE"
        const val ROUND_STATUS_REVIEW_PENDING = "REVIEW_PENDING"
        const val ROUND_STATUS_REVIEW_COMPLETED = "REVIEW_COMPLETED"
        const val CURSOR_END_SENTINEL = Int.MAX_VALUE
    }

    private val questionRecords = linkedMapOf<String, MutableList<VocabularyQuestionRecord>>()
    private val completedResults = linkedMapOf<String, StudyResult>()

    override suspend fun getPracticeSession(args: VocabularyPracticeArgs): VocabularyPracticeSession {
        val sessionId = args.sessionId ?: UUID.randomUUID().toString()
        val activeBook = loadActiveBook()
        val wordBank = vocabularyWordDao.getWordsByBook(activeBook.bookId)
            .ifEmpty { error("当前词书没有任何单词") }
        val filteredPool = wordBookSeeder.filterByDifficulty(
            wordBank = wordBank,
            requestedDifficulty = args.difficulty
        )
        val random = Random(sessionId.hashCode())
        val now = System.currentTimeMillis()
        val initialCursor = filteredPool.firstOrNull()?.sortOrder
            ?: wordBank.firstOrNull()?.sortOrder
            ?: error("当前词书没有任何单词")

        if (args.entryMode == com.example.seedie.ui.screens.learning.practice.VocabularyPracticeMode.Review) {
            val reviewRoundId = args.targetRoundId
                ?: getPendingReviewEntry()?.roundId
                ?: error("当前没有待复习轮次")
            return buildPendingReviewSession(
                sessionId = sessionId,
                args = args,
                activeBook = activeBook,
                wordBank = wordBank,
                random = random,
                roundId = reviewRoundId
            )
        }

        val bookProgress = vocabularyBookProgressDao.getProgressByBookId(currentUserId(), activeBook.bookId)
            ?: VocabularyBookProgressEntity(
                userId = currentUserId(),
                bookId = activeBook.bookId,
                nextWordSortOrderCursor = initialCursor,
                activeRoundId = null,
                learnedWordCount = 0,
                updatedAt = now
            ).also { vocabularyBookProgressDao.insertOrReplace(it) }

        var currentBookProgress = bookProgress
        val activeRoundId = currentBookProgress.activeRoundId
        if (activeRoundId != null) {
            val activeRound = vocabularyStudyRoundDao.getRoundById(activeRoundId)
            if (activeRound != null && activeRound.status == ROUND_STATUS_ACTIVE) {
                val roundWords = vocabularyStudyRoundWordDao.getActiveRoundWords(activeRoundId)
                if (roundWords.isNotEmpty()) {
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
                markStudyRoundReviewPending(activeRoundId)
                currentBookProgress = vocabularyBookProgressDao.getProgressByBookId(currentUserId(), activeBook.bookId)
                    ?: currentBookProgress.copy(
                        activeRoundId = null,
                        updatedAt = now
                    )
            } else {
                currentBookProgress = currentBookProgress.copy(
                    activeRoundId = null,
                    updatedAt = now
                )
                vocabularyBookProgressDao.insertOrReplace(currentBookProgress)
            }
        }

        return createNewStudySession(
            sessionId = sessionId,
            args = args,
            activeBook = activeBook,
            wordBank = wordBank,
            filteredPool = filteredPool,
            random = random,
            bookProgress = currentBookProgress
        )
    }

    override suspend fun saveStudyRoundSnapshot(snapshot: VocabularyPracticeResumeSnapshot) {
        val now = System.currentTimeMillis()
        database.withTransaction {
            val round = vocabularyStudyRoundDao.getRoundById(snapshot.roundId) ?: return@withTransaction
            val existingRoundWords = vocabularyStudyRoundWordDao.getRoundWords(snapshot.roundId)
            val activeWordIds = snapshot.activeWordIds.toSet()
            val previouslyMasteredRows = existingRoundWords.filter { it.isMasteredInRound }
            val newlyMasteredRows = existingRoundWords
                .filterNot { it.isMasteredInRound }
                .filterNot { it.wordId in activeWordIds }
                .mapIndexed { masteredIndex, roundWord ->
                    roundWord.copy(
                        queueOrder = previouslyMasteredRows.size + masteredIndex,
                        isMasteredInRound = true,
                        updatedAt = now
                    )
                }
            val removedWordIds = newlyMasteredRows.map { it.wordId }

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
            val activeRows = snapshot.activeWordIds.mapIndexedNotNull { queueOrder, wordId ->
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
            vocabularyStudyRoundWordDao.deleteByRoundId(snapshot.roundId)
            vocabularyStudyRoundWordDao.insertOrReplace(previouslyMasteredRows + newlyMasteredRows + activeRows)

            val activeProgressRows = snapshot.activeWordIds.map {
                VocabularyWordLearningProgressEntity(
                    userId = currentUserId(),
                    bookId = snapshot.bookId,
                    wordId = it,
                    status = WORD_STATUS_LEARNING,
                    lastStudiedAt = now,
                    learnedAt = null
                )
            }
            val removedProgressRows = removedWordIds.map {
                VocabularyWordLearningProgressEntity(
                    userId = currentUserId(),
                    bookId = snapshot.bookId,
                    wordId = it,
                    status = WORD_STATUS_LEARNED,
                    lastStudiedAt = now,
                    learnedAt = now
                )
            }
            vocabularyWordLearningProgressDao.insertOrReplace(activeProgressRows + removedProgressRows)

            val currentBookProgress = vocabularyBookProgressDao.getProgressByBookId(currentUserId(), snapshot.bookId)
                ?: VocabularyBookProgressEntity(
                    userId = currentUserId(),
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
        syncManager.syncNow(SyncScope.VOCABULARY_PROGRESS)
    }

    override suspend fun markStudyRoundReviewPending(roundId: String) {
        val now = System.currentTimeMillis()
        database.withTransaction {
            val round = vocabularyStudyRoundDao.getRoundById(roundId) ?: return@withTransaction
            if (round.status == ROUND_STATUS_REVIEW_PENDING || round.status == ROUND_STATUS_REVIEW_COMPLETED) {
                return@withTransaction
            }
            val roundWordIds = vocabularyStudyRoundWordDao.getRoundWords(roundId).map { it.wordId }
            val roundEntries = if (roundWordIds.isEmpty()) {
                emptyList()
            } else {
                vocabularyWordDao.getWordsByIds(round.bookId, roundWordIds)
            }
            val nextBatchCursor = roundEntries
                .maxByOrNull { it.sortOrder }
                ?.sortOrder
                ?.let { maxSortOrder ->
                    vocabularyWordDao.getWordsByBook(round.bookId)
                        .firstOrNull { it.sortOrder > maxSortOrder }
                        ?.sortOrder
                }
                ?: CURSOR_END_SENTINEL
            val masteredWordIds = vocabularyStudyRoundWordDao.getMasteredRoundWords(roundId).map { it.wordId }
            vocabularyStudyRoundDao.insertOrReplace(
                round.copy(
                    status = ROUND_STATUS_REVIEW_PENDING,
                    nextWordSortOrderCursor = nextBatchCursor,
                    updatedAt = now
                )
            )

            val currentBookProgress = vocabularyBookProgressDao.getProgressByBookId(currentUserId(), round.bookId)
                ?: VocabularyBookProgressEntity(
                    userId = currentUserId(),
                    bookId = round.bookId,
                    nextWordSortOrderCursor = round.nextWordSortOrderCursor,
                    activeRoundId = null,
                    learnedWordCount = 0,
                    updatedAt = now
                )
            vocabularyBookProgressDao.insertOrReplace(
                currentBookProgress.copy(
                    nextWordSortOrderCursor = nextBatchCursor,
                    activeRoundId = null,
                    learnedWordCount = currentBookProgress.learnedWordCount + round.masteredWordCount,
                    updatedAt = now
                )
            )
            if (masteredWordIds.isNotEmpty()) {
                vocabularyWordLearningProgressDao.insertOrReplace(
                    masteredWordIds.map { wordId ->
                        VocabularyWordLearningProgressEntity(
                            userId = currentUserId(),
                            bookId = round.bookId,
                            wordId = wordId,
                            status = WORD_STATUS_REVIEW_PENDING,
                            lastStudiedAt = now,
                            learnedAt = now
                        )
                    }
                )
            }
        }
        syncManager.syncNow(SyncScope.VOCABULARY_PROGRESS)
    }

    override suspend fun markReviewWordMastered(
        roundId: String,
        wordId: String
    ): ReviewWordUpdateResult {
        val now = System.currentTimeMillis()
        val result = database.withTransaction {
            updateReviewRoundWord(
                roundId = roundId,
                wordId = wordId,
                now = now
            ) { round ->
                VocabularyWordLearningProgressEntity(
                    userId = currentUserId(),
                    bookId = round.bookId,
                    wordId = wordId,
                    status = WORD_STATUS_MASTERED,
                    lastStudiedAt = now,
                    learnedAt = now
                )
            }
        }
        syncManager.syncNow(SyncScope.VOCABULARY_PROGRESS)
        return result
    }

    override suspend fun markReviewWordSentBackToLearning(
        roundId: String,
        wordId: String
    ): ReviewWordUpdateResult {
        val now = System.currentTimeMillis()
        val result = database.withTransaction {
            updateReviewRoundWord(
                roundId = roundId,
                wordId = wordId,
                now = now
            ) { round ->
                VocabularyWordLearningProgressEntity(
                    userId = currentUserId(),
                    bookId = round.bookId,
                    wordId = wordId,
                    status = WORD_STATUS_LEARNING,
                    lastStudiedAt = now,
                    learnedAt = null
                )
            }
        }
        syncManager.syncNow(SyncScope.VOCABULARY_PROGRESS)
        return result
    }

    override suspend fun markReviewCompleted(roundId: String) {
        val now = System.currentTimeMillis()
        database.withTransaction {
            val round = vocabularyStudyRoundDao.getRoundById(roundId) ?: return@withTransaction
            if (round.status == ROUND_STATUS_REVIEW_COMPLETED) return@withTransaction
            val remainingWordIds = vocabularyStudyRoundWordDao.getMasteredRoundWords(roundId).map { it.wordId }
            vocabularyStudyRoundDao.insertOrReplace(
                round.copy(
                    status = ROUND_STATUS_REVIEW_COMPLETED,
                    updatedAt = now
                )
            )
            if (remainingWordIds.isNotEmpty()) {
                vocabularyWordLearningProgressDao.insertOrReplace(
                    remainingWordIds.map { wordId ->
                        VocabularyWordLearningProgressEntity(
                            userId = currentUserId(),
                            bookId = round.bookId,
                            wordId = wordId,
                            status = WORD_STATUS_MASTERED,
                            lastStudiedAt = now,
                            learnedAt = now
                        )
                    }
                )
            }
            vocabularyStudyRoundWordDao.deleteByRoundId(roundId)
        }
        syncManager.syncNow(SyncScope.VOCABULARY_PROGRESS)
    }

    override suspend fun getPendingReviewEntry(): PendingReviewEntry? {
        val activeBook = loadActiveBook()
        val pendingRound = vocabularyStudyRoundDao.getLatestRoundByStatus(
            userId = currentUserId(),
            bookId = activeBook.bookId,
            status = ROUND_STATUS_REVIEW_PENDING
        ) ?: return null
        val pendingWordCount = vocabularyStudyRoundWordDao.getMasteredRoundWords(pendingRound.roundId).size
        if (pendingWordCount == 0) return null
        return PendingReviewEntry(
            roundId = pendingRound.roundId,
            bookId = pendingRound.bookId,
            pendingWordCount = pendingWordCount
        )
    }

    private suspend fun buildPendingReviewSession(
        sessionId: String,
        args: VocabularyPracticeArgs,
        activeBook: WordBookEntity,
        wordBank: List<VocabularyWordEntity>,
        random: Random,
        roundId: String
    ): VocabularyPracticeSession {
        val round = vocabularyStudyRoundDao.getRoundById(roundId)
            ?.takeIf { it.status == ROUND_STATUS_REVIEW_PENDING }
            ?: error("待复习轮次不存在或已完成")
        val masteredRoundWords = vocabularyStudyRoundWordDao.getMasteredRoundWords(roundId)
        val reviewWordIds = masteredRoundWords.map { it.wordId }
        val reviewEntryMap = vocabularyWordDao.getWordsByIds(activeBook.bookId, reviewWordIds)
            .associateBy { it.wordId }
        val reviewEntries = reviewWordIds.mapNotNull { reviewEntryMap[it] }
        return VocabularyPracticeSession(
            sessionMeta = VocabularySessionMeta(
                sessionId = sessionId,
                moduleId = args.sourceModuleId,
                startedAt = System.currentTimeMillis(),
                targetWordCount = reviewEntries.size,
                difficulty = args.difficulty,
                source = args.planId ?: "learning_hub",
                resumeSupported = false
            ),
            studyWords = emptyList(),
            reviewWords = reviewEntries.map { entry ->
                vocabularyOptionBuilder.toPracticeWord(entity = entry, allEntries = wordBank, random = random)
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
                vocabularyOptionBuilder.toPracticeWord(entity = entry, allEntries = wordBank, random = random)
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
        if (bookProgress.nextWordSortOrderCursor == CURSOR_END_SENTINEL) {
            return VocabularyPracticeSession(
                sessionMeta = VocabularySessionMeta(
                    sessionId = sessionId,
                    moduleId = args.sourceModuleId,
                    startedAt = System.currentTimeMillis(),
                    targetWordCount = 0,
                    difficulty = args.difficulty,
                    source = args.planId ?: "learning_hub",
                    resumeSupported = true
                ),
                studyWords = emptyList(),
                reviewWords = emptyList()
            )
        }
        val startSortOrder = filteredPool.firstOrNull { it.sortOrder >= bookProgress.nextWordSortOrderCursor }
            ?.sortOrder
            ?: return VocabularyPracticeSession(
                sessionMeta = VocabularySessionMeta(
                    sessionId = sessionId,
                    moduleId = args.sourceModuleId,
                    startedAt = System.currentTimeMillis(),
                    targetWordCount = 0,
                    difficulty = args.difficulty,
                    source = args.planId ?: "learning_hub",
                    resumeSupported = true
                ),
                studyWords = emptyList(),
                reviewWords = emptyList()
            )
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
                    userId = currentUserId(),
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
                        userId = currentUserId(),
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
                vocabularyOptionBuilder.toPracticeWord(entity = entry, allEntries = wordBank, random = random)
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

    private suspend fun updateReviewRoundWord(
        roundId: String,
        wordId: String,
        now: Long,
        progressBuilder: (VocabularyStudyRoundEntity) -> VocabularyWordLearningProgressEntity
    ): ReviewWordUpdateResult {
        val round = vocabularyStudyRoundDao.getRoundById(roundId)
            ?: return ReviewWordUpdateResult(
                remainingPendingCount = 0,
                isRoundCompleted = true
            )
        if (round.status != ROUND_STATUS_REVIEW_PENDING) {
            return ReviewWordUpdateResult(
                remainingPendingCount = vocabularyStudyRoundWordDao.getMasteredRoundWords(roundId).size,
                isRoundCompleted = round.status == ROUND_STATUS_REVIEW_COMPLETED
            )
        }
        vocabularyStudyRoundWordDao.deleteByRoundIdAndWordId(roundId, wordId)
        vocabularyWordLearningProgressDao.insertOrReplace(progressBuilder(round))
        val remainingPendingCount = vocabularyStudyRoundWordDao.getMasteredRoundWords(roundId).size
        val isRoundCompleted = remainingPendingCount == 0
        if (isRoundCompleted) {
            vocabularyStudyRoundDao.insertOrReplace(
                round.copy(
                    status = ROUND_STATUS_REVIEW_COMPLETED,
                    updatedAt = now
                )
            )
        }
        return ReviewWordUpdateResult(
            remainingPendingCount = remainingPendingCount,
            isRoundCompleted = isRoundCompleted
        )
    }

    private suspend fun loadActiveBook(): WordBookEntity = wordBookSeeder.loadActiveBook()

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

}
