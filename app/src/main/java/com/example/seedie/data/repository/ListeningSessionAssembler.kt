package com.example.seedie.data.repository

import com.example.seedie.data.remote.SupabaseListeningMaterial
import com.example.seedie.data.remote.SupabaseListeningOption
import com.example.seedie.data.remote.SupabaseListeningQuestion
import com.example.seedie.ui.screens.learning.listening.ListeningMaterialItem
import com.example.seedie.ui.screens.learning.listening.ListeningPracticeSession
import com.example.seedie.ui.screens.learning.listening.ListeningQuestionItem
import com.example.seedie.ui.screens.learning.practice.VocabularyPracticeOption

object ListeningSessionAssembler {
    fun assemble(
        sessionId: String,
        materials: List<SupabaseListeningMaterial>,
        questions: List<SupabaseListeningQuestion>,
        options: List<SupabaseListeningOption>
    ): ListeningPracticeSession {
        if (materials.isEmpty()) {
            error("听力题库为空")
        }

        val questionsByMaterial = questions.groupBy { it.material_id }
        val optionsByQuestion = options.groupBy { it.question_id }

        val assembledMaterials = materials.sortedBy { it.sort_order }.map { material ->
            val materialQuestions = questionsByMaterial[material.material_id]
                ?.sortedBy { it.sort_order }
                .orEmpty()
            if (materialQuestions.isEmpty()) {
                error("听力材料 ${material.material_id} 没有题目")
            }

            ListeningMaterialItem(
                materialId = material.material_id,
                title = material.title?.takeIf { it.isNotBlank() } ?: "Listening Material",
                titleZh = material.title_zh,
                materialType = material.material_type,
                promptText = material.prompt_text,
                transcript = material.transcript,
                audioUrl = material.audio_url,
                estimatedSeconds = material.estimated_seconds,
                questions = materialQuestions.map { question ->
                    val questionOptions = optionsByQuestion[question.question_id]
                        ?.sortedBy { it.sort_order }
                        .orEmpty()
                    if (questionOptions.isEmpty()) {
                        error("听力题目 ${question.question_id} 没有选项")
                    }

                    val correctOptionId = question.correct_option_id
                        ?.takeIf { it.isNotBlank() }
                        ?: error("听力题目 ${question.question_id} 缺少正确答案")
                    if (questionOptions.none { it.option_id == correctOptionId }) {
                        error("听力题目 ${question.question_id} 缺少正确答案选项")
                    }

                    ListeningQuestionItem(
                        questionId = question.question_id,
                        questionType = question.question_type,
                        stem = question.stem.orEmpty(),
                        options = questionOptions.map { option ->
                            val optionText = option.option_text.orEmpty()
                            VocabularyPracticeOption(
                                optionId = option.option_id,
                                label = "${option.option_id}. $optionText",
                                isCorrect = option.option_id == correctOptionId
                            )
                        },
                        correctOptionId = correctOptionId,
                        explanation = question.explanation,
                        rewardToken = question.reward_token
                    )
                }
            )
        }

        return ListeningPracticeSession(
            sessionId = sessionId,
            materials = assembledMaterials
        )
    }
}
