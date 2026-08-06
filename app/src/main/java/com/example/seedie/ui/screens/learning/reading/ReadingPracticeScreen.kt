package com.example.seedie.ui.screens.learning.reading

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.seedie.domain.model.StudyResult
import com.example.seedie.ui.components.PracticeOptionCard
import com.example.seedie.ui.screens.garden.GardenExitConfirmDialog
import com.example.seedie.ui.screens.garden.PlantSessionGate
import com.example.seedie.ui.screens.learning.assignments.PracticeAssignmentArgs
import com.example.seedie.ui.screens.learning.catalog.FreePracticeArgs
import com.example.seedie.ui.theme.gardenShadow

@Composable
fun ReadingPracticeRoute(
    assignmentArgs: PracticeAssignmentArgs?,
    freeArgs: FreePracticeArgs?,
    onFinishSession: (StudyResult) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: ReadingPracticeViewModel = hiltViewModel()
) {
    PlantSessionGate { speciesId ->
        val uiState by viewModel.uiState.collectAsState()

        LaunchedEffect(assignmentArgs?.submissionId, assignmentArgs?.mode, freeArgs?.itemRef, speciesId) {
            viewModel.setSelectedSpeciesId(speciesId)
            when {
                assignmentArgs != null -> viewModel.initializeAssignment(assignmentArgs)
                freeArgs != null -> viewModel.initializeFree(freeArgs)
            }
        }

        LaunchedEffect(Unit) {
            viewModel.studyResults.collect { result ->
                onFinishSession(result)
            }
        }

        ReadingPracticeScreen(
            uiState = uiState,
            onNavigateBack = onNavigateBack,
            onBackClick = viewModel::onBackClick,
            onConfirmExit = viewModel::onConfirmExit,
            onDismissExitDialog = viewModel::onDismissExitDialog,
            onOptionSelected = viewModel::onOptionSelected,
            onSubmitSet = viewModel::onSubmitSet,
            onNextSet = viewModel::onNextSet,
            onRetryLoad = viewModel::onRetryLoad,
            onFinishSession = viewModel::onFinishSession
        )
    }
}

@Composable
private fun ReadingPracticeScreen(
    uiState: ReadingPracticeUiState,
    onNavigateBack: () -> Unit,
    onBackClick: () -> Unit,
    onConfirmExit: () -> Unit,
    onDismissExitDialog: () -> Unit,
    onOptionSelected: (String, String) -> Unit,
    onSubmitSet: () -> Unit,
    onNextSet: () -> Unit,
    onRetryLoad: () -> Unit,
    onFinishSession: () -> Unit
) {
    if (uiState.showExitConfirmDialog) {
        if (uiState.isReviewMode) {
            AlertDialog(
                onDismissRequest = {},
                confirmButton = {
                    Button(onClick = onConfirmExit) { Text("确认退出") }
                },
                dismissButton = {
                    OutlinedButton(onClick = onDismissExitDialog) { Text("继续作答") }
                },
                title = { Text("退出回顾？") },
                text = { Text("进度不会改变。") }
            )
        } else {
            GardenExitConfirmDialog(
                answeredQuestionCount = uiState.correctCount + uiState.wrongCount,
                onConfirmExit = onConfirmExit,
                onContinue = onDismissExitDialog
            )
        }
    }

    when (uiState.stage) {
        ReadingPracticeStage.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        ReadingPracticeStage.Error -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = uiState.errorMessage ?: "加载失败",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onNavigateBack) { Text("返回") }
                    Button(onClick = onRetryLoad) { Text("重试") }
                }
            }
        }

        ReadingPracticeStage.Completed -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    if (uiState.isReviewMode) "作业回顾完成" else "作业已提交",
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text("正确 ${uiState.correctCount} / 错误 ${uiState.wrongCount}")
                if (!uiState.isReviewMode) {
                    Text("本局将种入你的花园 · 用时 ${uiState.elapsedSeconds}s")
                }
                Spacer(modifier = Modifier.height(20.dp))
                Button(onClick = onFinishSession) { Text("完成") }
            }
        }

        ReadingPracticeStage.Answering,
        ReadingPracticeStage.Reviewing -> {
            val set = uiState.currentSet ?: return
            val reviewing = uiState.stage == ReadingPracticeStage.Reviewing
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "阅读训练 · 第 ${uiState.currentSetIndex + 1} / ${uiState.totalSetCount} 篇",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = listOfNotNull(set.title, set.titleZh).joinToString(" · "),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "${uiState.elapsedSeconds}s",
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .gardenShadow(),
                        shape = MaterialTheme.shapes.large,
                        tonalElevation = 1.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = "Reading Passage",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            PassageText(
                                passage = set.passage,
                                highlightWords = if (reviewing) {
                                    set.questions.mapNotNull { it.highlightWord }
                                } else {
                                    emptyList()
                                }
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .gardenShadow(),
                        shape = MaterialTheme.shapes.large,
                        tonalElevation = 1.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                set.questions.forEachIndexed { index, question ->
                                    Text(
                                        text = "Q${index + 1}. ${question.stem}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    question.options.forEach { option ->
                                        PracticeOptionCard(
                                            option = option,
                                            selectedOptionId = uiState.answers[question.questionId],
                                            answerStatus = uiState.answerStatusFor(question),
                                            interactionEnabled = !reviewing,
                                            feedbackVisible = reviewing,
                                            onOptionSelected = { optionId ->
                                                onOptionSelected(question.questionId, optionId)
                                            }
                                        )
                                    }
                                    if (reviewing) {
                                        Text(
                                            text = question.explanation,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            if (!reviewing) {
                                if (uiState.submitHint.isNotBlank()) {
                                    Text(
                                        text = uiState.submitHint,
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                }
                                Button(
                                    onClick = onSubmitSet,
                                    enabled = uiState.canSubmitSet,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("提交")
                                }
                            } else {
                                val isLast = uiState.currentSetIndex + 1 >= uiState.totalSetCount
                                Button(
                                    onClick = onNextSet,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        when {
                                            isLast && uiState.isReviewMode -> "完成回顾"
                                            isLast -> "提交作业"
                                            else -> "下一篇"
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PassageText(
    passage: String,
    highlightWords: List<String>,
    modifier: Modifier = Modifier
) {
    val highlightSet = highlightWords.map { it.lowercase() }.toSet()
    val annotated = buildAnnotatedString {
        var index = 0
        while (index < passage.length) {
            if (passage.startsWith("<<", index)) {
                val end = passage.indexOf(">>", index + 2)
                if (end > index) {
                    val word = passage.substring(index + 2, end)
                    val shouldHighlight = highlightSet.isEmpty() || word.lowercase() in highlightSet
                    if (shouldHighlight && highlightSet.isNotEmpty()) {
                        withStyle(
                            SpanStyle(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                                textDecoration = TextDecoration.Underline,
                                background = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            )
                        ) {
                            append(word)
                        }
                    } else {
                        append(word)
                    }
                    index = end + 2
                    continue
                }
            }
            append(passage[index])
            index += 1
        }
    }
    Text(
        text = annotated,
        style = MaterialTheme.typography.bodyLarge,
        modifier = modifier
    )
}
