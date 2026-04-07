package com.example.seedie.data.repository

import com.example.seedie.domain.model.StudyResult
import com.example.seedie.domain.repository.VocabularyPracticeRepository
import com.example.seedie.ui.screens.learning.practice.VocabularyPracticeArgs
import com.example.seedie.ui.screens.learning.practice.VocabularyPracticeOption
import com.example.seedie.ui.screens.learning.practice.VocabularyPracticeQuestion
import com.example.seedie.ui.screens.learning.practice.VocabularyPracticeSession
import com.example.seedie.ui.screens.learning.practice.VocabularyQuestionType
import com.example.seedie.ui.screens.learning.practice.VocabularyQuestionRecord
import com.example.seedie.ui.screens.learning.practice.VocabularySessionMeta
import kotlin.random.Random
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VocabularyPracticeRepositoryImpl @Inject constructor() : VocabularyPracticeRepository {
    private val questionRecords = linkedMapOf<String, MutableList<VocabularyQuestionRecord>>()
    private val completedResults = linkedMapOf<String, StudyResult>()
    private val wordBank = VocabularyStaticWordPack.entries

    override suspend fun getPracticeSession(args: VocabularyPracticeArgs): VocabularyPracticeSession {
        val sessionId = args.sessionId ?: UUID.randomUUID().toString()
        val questions = buildQuestionBank(args = args, sessionId = sessionId)

        return VocabularyPracticeSession(
            sessionMeta = VocabularySessionMeta(
                sessionId = sessionId,
                moduleId = args.sourceModuleId,
                startedAt = System.currentTimeMillis(),
                targetWordCount = questions.size,
                difficulty = args.difficulty,
                source = args.planId ?: "learning_hub",
                resumeSupported = false
            ),
            questions = questions
        )
    }

    override suspend fun submitQuestionRecord(record: VocabularyQuestionRecord) {
        val sessionRecords = questionRecords.getOrPut(record.sessionId) { mutableListOf() }
        sessionRecords.removeAll { it.questionId == record.questionId }
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

    private fun buildQuestionBank(
        args: VocabularyPracticeArgs,
        sessionId: String
    ): List<VocabularyPracticeQuestion> {
        val random = Random(sessionId.hashCode())
        val requestedCount = args.wordCountTarget.coerceIn(1, wordBank.size)
        val requestedDifficulty = args.difficulty.trim().lowercase()

        val difficultyPool = when (requestedDifficulty) {
            "", "mixed", "all" -> wordBank
            else -> wordBank.filter { it.difficultyLevel == requestedDifficulty }
        }

        val basePool = if (difficultyPool.size >= requestedCount) difficultyPool else wordBank
        val selectedEntries = basePool
            .shuffled(random)
            .take(requestedCount)

        return selectedEntries.mapIndexed { index, entry ->
            entry.toQuestion(
                questionNumber = index + 1,
                allEntries = wordBank,
                random = random
            )
        }
    }

    private fun StaticWordEntry.toQuestion(
        questionNumber: Int,
        allEntries: List<StaticWordEntry>,
        random: Random
    ): VocabularyPracticeQuestion {
        val distractorPool = allEntries
            .asSequence()
            .filter { it.wordId != wordId && it.translation != translation }
            .sortedByDescending { candidate -> candidate.partOfSpeech == partOfSpeech }
            .distinctBy { candidate -> candidate.translation }
            .toList()
            .shuffled(random)
            .take(3)

        val optionEntries = (distractorPool + this)
            .distinctBy { entry -> entry.translation }
            .shuffled(random)
            .mapIndexed { optionIndex, entry ->
                VocabularyPracticeOption(
                    optionId = "q${questionNumber}_o${optionIndex + 1}",
                    label = entry.translation,
                    isCorrect = entry.wordId == wordId,
                    englishHint = entry.english
                )
            }

        return VocabularyPracticeQuestion(
            questionId = "q_$wordId",
            wordId = wordId,
            questionType = VocabularyQuestionType.MULTIPLE_CHOICE_TRANSLATION,
            english = english,
            phonetic = phonetic,
            partOfSpeech = partOfSpeech,
            translationCorrect = translation,
            optionList = optionEntries,
            exampleSentence = exampleSentence,
            difficultyLevel = difficultyLevel,
            rewardToken = rewardToken,
            estimatedDurationSec = estimatedDurationSec
        )
    }
}
