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
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VocabularyPracticeRepositoryImpl @Inject constructor() : VocabularyPracticeRepository {
    private val questionRecords = linkedMapOf<String, MutableList<VocabularyQuestionRecord>>()
    private val completedResults = linkedMapOf<String, StudyResult>()

    override suspend fun getPracticeSession(args: VocabularyPracticeArgs): VocabularyPracticeSession {
        val sessionId = args.sessionId ?: UUID.randomUUID().toString()
        val questions = buildQuestionBank()
            .take(args.wordCountTarget.coerceAtLeast(1))

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

    private fun buildQuestionBank(): List<VocabularyPracticeQuestion> {
        return listOf(
            VocabularyPracticeQuestion(
                questionId = "q1",
                wordId = "w1",
                questionType = VocabularyQuestionType.MULTIPLE_CHOICE_TRANSLATION,
                english = "apple",
                phonetic = "/ˈae.pəl/",
                partOfSpeech = "n.",
                translationCorrect = "苹果",
                optionList = listOf(
                    VocabularyPracticeOption("q1_a", "苹果", true),
                    VocabularyPracticeOption("q1_b", "香蕉", false),
                    VocabularyPracticeOption("q1_c", "橙子", false),
                    VocabularyPracticeOption("q1_d", "西瓜", false)
                ),
                exampleSentence = "An apple a day keeps the doctor away.",
                difficultyLevel = "easy",
                rewardToken = 3,
                estimatedDurationSec = 8
            ),
            VocabularyPracticeQuestion(
                questionId = "q2",
                wordId = "w2",
                questionType = VocabularyQuestionType.MULTIPLE_CHOICE_TRANSLATION,
                english = "bridge",
                phonetic = "/brɪdʒ/",
                partOfSpeech = "n.",
                translationCorrect = "桥",
                optionList = listOf(
                    VocabularyPracticeOption("q2_a", "山", false),
                    VocabularyPracticeOption("q2_b", "桥", true),
                    VocabularyPracticeOption("q2_c", "森林", false),
                    VocabularyPracticeOption("q2_d", "河流", false)
                ),
                exampleSentence = "We walked across the bridge together.",
                difficultyLevel = "easy",
                rewardToken = 3,
                estimatedDurationSec = 8
            ),
            VocabularyPracticeQuestion(
                questionId = "q3",
                wordId = "w3",
                questionType = VocabularyQuestionType.MULTIPLE_CHOICE_TRANSLATION,
                english = "careful",
                phonetic = "/ˈkeə.fəl/",
                partOfSpeech = "adj.",
                translationCorrect = "小心的",
                optionList = listOf(
                    VocabularyPracticeOption("q3_a", "勇敢的", false),
                    VocabularyPracticeOption("q3_b", "安静的", false),
                    VocabularyPracticeOption("q3_c", "小心的", true),
                    VocabularyPracticeOption("q3_d", "整洁的", false)
                ),
                exampleSentence = "Please be careful with the glass bottle.",
                difficultyLevel = "medium",
                rewardToken = 4,
                estimatedDurationSec = 10
            ),
            VocabularyPracticeQuestion(
                questionId = "q4",
                wordId = "w4",
                questionType = VocabularyQuestionType.MULTIPLE_CHOICE_TRANSLATION,
                english = "discover",
                phonetic = "/dɪˈskʌv.ər/",
                partOfSpeech = "v.",
                translationCorrect = "发现",
                optionList = listOf(
                    VocabularyPracticeOption("q4_a", "发现", true),
                    VocabularyPracticeOption("q4_b", "修理", false),
                    VocabularyPracticeOption("q4_c", "追赶", false),
                    VocabularyPracticeOption("q4_d", "选择", false)
                ),
                exampleSentence = "The children discover a tiny seed in the soil.",
                difficultyLevel = "medium",
                rewardToken = 4,
                estimatedDurationSec = 10
            ),
            VocabularyPracticeQuestion(
                questionId = "q5",
                wordId = "w5",
                questionType = VocabularyQuestionType.MULTIPLE_CHOICE_TRANSLATION,
                english = "protect",
                phonetic = "/prəˈtekt/",
                partOfSpeech = "v.",
                translationCorrect = "保护",
                optionList = listOf(
                    VocabularyPracticeOption("q5_a", "推动", false),
                    VocabularyPracticeOption("q5_b", "种植", false),
                    VocabularyPracticeOption("q5_c", "保护", true),
                    VocabularyPracticeOption("q5_d", "清洗", false)
                ),
                exampleSentence = "Trees protect the soil from strong wind.",
                difficultyLevel = "medium",
                rewardToken = 4,
                estimatedDurationSec = 10
            )
        )
    }
}
