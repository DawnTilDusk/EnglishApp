package com.example.seedie.domain.reading

import com.example.seedie.ui.screens.learning.reading.ReadingSetItem

data class ReadingSetScore(
    val correctCount: Int,
    val wrongCount: Int,
    val earnedTokens: Int
)

object ReadingPracticeScorer {
    fun scoreSet(set: ReadingSetItem, answers: Map<String, String>): ReadingSetScore {
        var correct = 0
        var wrong = 0
        var tokens = 0
        set.questions.forEach { question ->
            val selected = answers[question.questionId]
            if (selected == question.correctOptionId) {
                correct += 1
                tokens += question.rewardToken
            } else {
                wrong += 1
            }
        }
        return ReadingSetScore(
            correctCount = correct,
            wrongCount = wrong,
            earnedTokens = tokens
        )
    }
}
