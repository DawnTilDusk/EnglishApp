package com.example.seedie.domain.quiz

object VocabularyEstimateCalculator {
    data class Input(
        val correctCount: Int,
        val totalCount: Int,
        val correctDifficulties: List<String>
    )

    fun estimate(input: Input): Int {
        if (input.totalCount <= 0) return 600
        val accuracy = input.correctCount.toFloat() / input.totalCount
        val difficultyScore = input.correctDifficulties.sumOf { level ->
            when (level) {
                "easy" -> 80
                "medium" -> 120
                "hard" -> 160
                else -> 100
            }
        }
        val raw = 600 + (difficultyScore * accuracy * 2.2).toInt()
        return (raw / 50) * 50
    }
}
