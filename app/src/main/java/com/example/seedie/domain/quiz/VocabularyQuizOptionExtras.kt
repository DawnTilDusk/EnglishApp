package com.example.seedie.domain.quiz

import com.example.seedie.ui.screens.learning.practice.VocabularyPracticeOption
import kotlin.random.Random

/**
 * With [VocabularyQuizConstants.NONE_OF_ABOVE_PROBABILITY], replace the 4th option
 * label with「以上都不对」, keeping [VocabularyPracticeOption.isCorrect].
 */
fun maybeReplaceFourthWithNoneOfAbove(
    options: List<VocabularyPracticeOption>,
    random: Random,
    probability: Float = VocabularyQuizConstants.NONE_OF_ABOVE_PROBABILITY,
    label: String = VocabularyQuizConstants.NONE_OF_ABOVE_LABEL
): List<VocabularyPracticeOption> {
    if (options.size < 4) return options
    if (random.nextFloat() >= probability) return options
    return options.mapIndexed { index, option ->
        if (index != 3) {
            option
        } else {
            option.copy(
                label = label,
                showFeedbackHint = false
            )
        }
    }
}
