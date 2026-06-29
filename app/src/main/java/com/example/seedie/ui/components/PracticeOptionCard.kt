package com.example.seedie.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.seedie.ui.screens.learning.practice.AnswerStatus
import com.example.seedie.ui.screens.learning.practice.VocabularyPracticeOption

@Composable
fun PracticeOptionCard(
    option: VocabularyPracticeOption,
    selectedOptionId: String?,
    answerStatus: AnswerStatus,
    interactionEnabled: Boolean,
    feedbackVisible: Boolean,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isSelected = selectedOptionId == option.optionId
    val containerColor = when {
        feedbackVisible && option.isCorrect ->
            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
        feedbackVisible && isSelected && !option.isCorrect ->
            MaterialTheme.colorScheme.error.copy(alpha = 0.14f)
        isSelected ->
            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        else -> MaterialTheme.colorScheme.surface
    }
    val borderColor = when {
        feedbackVisible && option.isCorrect -> MaterialTheme.colorScheme.primary
        feedbackVisible && isSelected && !option.isCorrect -> MaterialTheme.colorScheme.error
        isSelected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = interactionEnabled) {
                onOptionSelected(option.optionId)
            },
        shape = MaterialTheme.shapes.medium,
        color = containerColor,
        tonalElevation = if (isSelected) 2.dp else 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = buildString {
                    append(option.label)
                    if (feedbackVisible && option.showFeedbackHint && !option.englishHint.isNullOrBlank()) {
                        append("  ")
                        append(option.englishHint)
                    }
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (feedbackVisible) {
                when {
                    option.isCorrect -> Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Correct",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    isSelected && answerStatus == AnswerStatus.Wrong -> Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = "Wrong",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
