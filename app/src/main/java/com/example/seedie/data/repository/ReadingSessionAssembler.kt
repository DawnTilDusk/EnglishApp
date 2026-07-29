package com.example.seedie.data.repository

import com.example.seedie.data.remote.SupabaseReadingOption
import com.example.seedie.data.remote.SupabaseReadingQuestion
import com.example.seedie.data.remote.SupabaseReadingSet
import com.example.seedie.ui.screens.learning.practice.VocabularyPracticeOption
import com.example.seedie.ui.screens.learning.reading.ReadingPracticeSession
import com.example.seedie.ui.screens.learning.reading.ReadingQuestionItem
import com.example.seedie.ui.screens.learning.reading.ReadingSetItem

object ReadingSessionAssembler {
    fun assemble(
        sessionId: String,
        sets: List<SupabaseReadingSet>,
        questions: List<SupabaseReadingQuestion>,
        options: List<SupabaseReadingOption>
    ): ReadingPracticeSession {
        if (sets.isEmpty()) {
            error("阅读题库为空")
        }
        val questionsBySet = questions.groupBy { it.set_id }
        val optionsByQuestion = options.groupBy { it.question_id }

        val assembledSets = sets.sortedBy { it.sort_order }.map { set ->
            val setQuestions = questionsBySet[set.set_id]
                ?.sortedBy { it.sort_order }
                .orEmpty()
            if (setQuestions.isEmpty()) {
                error("阅读套卷 ${set.set_id} 没有题目")
            }
            ReadingSetItem(
                setId = set.set_id,
                title = set.title,
                titleZh = set.title_zh,
                passage = set.passage,
                questions = setQuestions.map { question ->
                    val opts = optionsByQuestion[question.question_id]
                        ?.sortedBy { it.sort_order }
                        .orEmpty()
                    if (opts.isEmpty()) {
                        error("题目 ${question.question_id} 没有选项")
                    }
                    if (opts.none { it.option_id == question.correct_option_id }) {
                        error("题目 ${question.question_id} 缺少正确答案选项")
                    }
                    ReadingQuestionItem(
                        questionId = question.question_id,
                        questionType = question.question_type,
                        stem = question.stem,
                        options = opts.map { option ->
                            VocabularyPracticeOption(
                                optionId = option.option_id,
                                label = "${option.option_id}. ${option.option_text}",
                                isCorrect = option.option_id == question.correct_option_id
                            )
                        },
                        correctOptionId = question.correct_option_id,
                        explanation = question.explanation,
                        highlightWord = question.highlight_word,
                        rewardToken = question.reward_token
                    )
                }
            )
        }

        return ReadingPracticeSession(
            sessionId = sessionId,
            sets = assembledSets
        )
    }
}
