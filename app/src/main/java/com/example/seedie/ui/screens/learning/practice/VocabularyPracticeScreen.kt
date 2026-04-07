package com.example.seedie.ui.screens.learning.practice

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.seedie.domain.model.StudyResult
import com.example.seedie.ui.theme.gardenShadow

@Composable
fun VocabularyPracticeRoute(
    args: VocabularyPracticeArgs = VocabularyPracticeArgs(),
    onFinishSession: (StudyResult) -> Unit,
    viewModel: VocabularyPracticeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(args) {
        viewModel.initialize(args)
    }

    LaunchedEffect(Unit) {
        viewModel.studyResults.collect { result ->
            onFinishSession(result)
        }
    }

    VocabularyPracticeScreen(
        uiState = uiState,
        onBackClick = viewModel::onBackClick,
        onConfirmExit = viewModel::onConfirmExit,
        onDismissExitDialog = viewModel::onDismissExitDialog,
        onOptionSelected = viewModel::onOptionSelected,
        onSubmitAnswer = viewModel::onSubmitAnswer,
        onNextQuestion = viewModel::onNextQuestion,
        onSkipQuestion = viewModel::onSkipQuestion,
        onRetryLoad = viewModel::onRetryLoad,
        onFinishSession = viewModel::onFinishSession
    )
}

@Composable
private fun VocabularyPracticeScreen(
    uiState: VocabularyPracticeUiState,
    onBackClick: () -> Unit,
    onConfirmExit: () -> Unit,
    onDismissExitDialog: () -> Unit,
    onOptionSelected: (String) -> Unit,
    onSubmitAnswer: () -> Unit,
    onNextQuestion: () -> Unit,
    onSkipQuestion: () -> Unit,
    onRetryLoad: () -> Unit,
    onFinishSession: () -> Unit
) {
    if (uiState.showExitConfirmDialog) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {
                Button(onClick = onConfirmExit) {
                    Text("确认退出")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = onDismissExitDialog) {
                    Text("继续学习")
                }
            },
            title = { Text("退出后将结束本次学习") },
            text = { Text("已完成的题目会被记录，但你将离开当前练习。") }
        )
    }

    when (uiState.stage) {
        VocabularyPracticeStage.Loading -> LoadingState()
        VocabularyPracticeStage.Empty -> EmptyState(onBackClick = onBackClick)
        VocabularyPracticeStage.Error -> ErrorState(
            message = uiState.errorMessage ?: "词包加载失败",
            onRetryLoad = onRetryLoad,
            onBackClick = onBackClick
        )
        VocabularyPracticeStage.Completed -> CompletedState(
            uiState = uiState,
            onBackClick = onBackClick,
            onFinishSession = onFinishSession
        )
        VocabularyPracticeStage.Ready,
        VocabularyPracticeStage.AnswerEvaluated -> PracticeContent(
            uiState = uiState,
            onBackClick = onBackClick,
            onOptionSelected = onOptionSelected,
            onSubmitAnswer = onSubmitAnswer,
            onNextQuestion = onNextQuestion,
            onSkipQuestion = onSkipQuestion
        )
    }
}

@Composable
private fun PracticeContent(
    uiState: VocabularyPracticeUiState,
    onBackClick: () -> Unit,
    onOptionSelected: (String) -> Unit,
    onSubmitAnswer: () -> Unit,
    onNextQuestion: () -> Unit,
    onSkipQuestion: () -> Unit
) {
    val currentQuestion = uiState.currentQuestion ?: return
    val totalQuestions = uiState.questions.size.coerceAtLeast(1)
    val progress = (uiState.currentQuestionIndex + 1) / totalQuestions.toFloat()
    var isAuxPanelExpanded by rememberSaveable { mutableStateOf(false) }
    val mainPanelWeight by animateFloatAsState(
        targetValue = if (isAuxPanelExpanded) 0.64f else 1f,
        animationSpec = tween(durationMillis = 260),
        label = "mainPanelWeight"
    )
    val auxPanelWeight by animateFloatAsState(
        targetValue = if (isAuxPanelExpanded) 0.36f else 0f,
        animationSpec = tween(durationMillis = 260),
        label = "auxPanelWeight"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .gardenShadow(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable(onClick = onBackClick)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = "背单词练习",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    TokenBadge(tokens = uiState.earnedTokens)
                }

                Text(
                    text = "第 ${uiState.currentQuestionIndex + 1} / $totalQuestions 题",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary
                )

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .gardenShadow()
                .animateContentSize(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(mainPanelWeight)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = currentQuestion.english,
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${currentQuestion.phonetic}  ${currentQuestion.partOfSpeech}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "用时 ${uiState.elapsedSeconds}s",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "剩余 ${uiState.remainingCount} 题",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                            AuxPanelToggle(
                                expanded = isAuxPanelExpanded,
                                onClick = { isAuxPanelExpanded = !isAuxPanelExpanded }
                            )
                        }
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = currentQuestion.exampleSentence,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .then(
                                        if (uiState.stage == VocabularyPracticeStage.AnswerEvaluated) {
                                            Modifier
                                        } else {
                                            Modifier.blur(10.dp)
                                        }
                                    ),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = if (isAuxPanelExpanded) 2 else 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (uiState.stage != VocabularyPracticeStage.AnswerEvaluated) {
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.35f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "作答之后展示例句",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.secondary,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        currentQuestion.optionList.forEach { option ->
                            OptionCard(
                                option = option,
                                selectedOptionId = uiState.selectedOptionId,
                                answerStatus = uiState.answerStatus,
                                stage = uiState.stage,
                                onOptionSelected = onOptionSelected
                            )
                        }
                    }

                    if (uiState.stage == VocabularyPracticeStage.AnswerEvaluated) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = currentQuestion.translationCorrect,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = feedbackText(uiState.answerStatus, currentQuestion.translationCorrect),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = feedbackColor(uiState.answerStatus),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                if (isAuxPanelExpanded || auxPanelWeight > 0.01f) {
                    Box(
                        modifier = Modifier
                            .weight(auxPanelWeight.coerceAtLeast(0.001f))
                            .fillMaxHeight()
                    ) {
                        androidx.compose.animation.AnimatedVisibility(
                            visible = isAuxPanelExpanded,
                            modifier = Modifier.fillMaxSize(),
                            enter = fadeIn(animationSpec = tween(180)) +
                                expandHorizontally(
                                    animationSpec = tween(260),
                                    expandFrom = Alignment.Start
                                ),
                            exit = fadeOut(animationSpec = tween(140)) +
                                shrinkHorizontally(
                                    animationSpec = tween(220),
                                    shrinkTowards = Alignment.Start
                                )
                        ) {
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                shape = MaterialTheme.shapes.large,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "拓展栏",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "后续可放注记、错题提示或 AI 助手。",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = MaterialTheme.shapes.medium,
                                        color = MaterialTheme.colorScheme.surface
                                    ) {
                                        Text(
                                            text = "当前版本仅预留区域，不承载实际功能。",
                                            modifier = Modifier.padding(14.dp),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .gardenShadow(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onSkipQuestion,
                    modifier = Modifier.weight(1f),
                    enabled = uiState.stage == VocabularyPracticeStage.Ready
                ) {
                    Text("跳过")
                }
                if (uiState.stage == VocabularyPracticeStage.AnswerEvaluated) {
                    Button(
                        onClick = onNextQuestion,
                        modifier = Modifier.weight(2f)
                    ) {
                        Text(if (uiState.remainingCount == 0) "查看结果" else "下一题")
                    }
                } else {
                    Button(
                        onClick = onSubmitAnswer,
                        modifier = Modifier.weight(2f),
                        enabled = uiState.canSubmitAnswer
                    ) {
                        Text("提交答案")
                    }
                }
            }
        }
    }
}

@Composable
private fun OptionCard(
    option: VocabularyPracticeOption,
    selectedOptionId: String?,
    answerStatus: AnswerStatus,
    stage: VocabularyPracticeStage,
    onOptionSelected: (String) -> Unit
) {
    val isSelected = selectedOptionId == option.optionId
    val containerColor = when {
        stage == VocabularyPracticeStage.AnswerEvaluated && option.isCorrect ->
            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
        stage == VocabularyPracticeStage.AnswerEvaluated && isSelected && !option.isCorrect ->
            MaterialTheme.colorScheme.error.copy(alpha = 0.14f)
        isSelected ->
            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        else -> MaterialTheme.colorScheme.surface
    }
    val borderColor = when {
        stage == VocabularyPracticeStage.AnswerEvaluated && option.isCorrect -> MaterialTheme.colorScheme.primary
        stage == VocabularyPracticeStage.AnswerEvaluated && isSelected && !option.isCorrect -> MaterialTheme.colorScheme.error
        isSelected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = stage == VocabularyPracticeStage.Ready) {
                onOptionSelected(option.optionId)
            },
        shape = MaterialTheme.shapes.medium,
        color = containerColor,
        tonalElevation = if (isSelected) 2.dp else 0.dp,
        shadowElevation = 0.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
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
                    if (stage == VocabularyPracticeStage.AnswerEvaluated && !option.englishHint.isNullOrBlank()) {
                        append("  ")
                        append(option.englishHint)
                    }
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (stage == VocabularyPracticeStage.AnswerEvaluated) {
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

@Composable
private fun AuxPanelToggle(
    expanded: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (expanded) ">" else "<",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun CompletedState(
    uiState: VocabularyPracticeUiState,
    onBackClick: () -> Unit,
    onFinishSession: () -> Unit
) {
    val totalAnswered = uiState.correctCount + uiState.wrongCount + uiState.skippedCount
    val accuracy = if (totalAnswered == 0) 0 else (uiState.correctCount * 100 / totalAnswered)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .gardenShadow(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocalFlorist,
                    contentDescription = "Done",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp)
                )
                Text(
                    text = "本轮背单词完成",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "正确 $accuracy% · 获得 ${uiState.earnedTokens} 枚代币",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.secondary
                )
                SummaryRow("完成题数", totalAnswered.toString())
                SummaryRow("正确题数", uiState.correctCount.toString())
                SummaryRow("错误题数", uiState.wrongCount.toString())
                SummaryRow("跳过题数", uiState.skippedCount.toString())
                SummaryRow("学习时长", "${uiState.elapsedSeconds}s")
                SummaryRow("词汇增量", "+${uiState.correctCount}")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onBackClick,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("返回学习中心")
                    }
                    Button(
                        onClick = onFinishSession,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("完成并结算")
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text("正在整理今日词包...")
        }
    }
}

@Composable
private fun EmptyState(onBackClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "今日词包已完成",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "先去看看其他学习模块，或者明天再来打卡。",
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(onClick = onBackClick) {
                Text("返回学习中心")
            }
        }
    }
}

@Composable
private fun ErrorState(
    message: String,
    onRetryLoad: () -> Unit,
    onBackClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = "Error",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onBackClick) {
                    Text("返回")
                }
                Button(onClick = onRetryLoad) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("重试")
                }
            }
        }
    }
}

@Composable
private fun TokenBadge(tokens: Int) {
    Surface(
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
        shape = CircleShape
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(MaterialTheme.colorScheme.tertiary, CircleShape)
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = "+$tokens 代币",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun feedbackText(answerStatus: AnswerStatus, correctAnswer: String): String {
    return when (answerStatus) {
        AnswerStatus.Correct -> "回答正确，继续保持。"
        AnswerStatus.Wrong -> "回答错误，正确答案是：$correctAnswer"
        AnswerStatus.Skipped -> "本题已跳过，正确答案是：$correctAnswer"
        AnswerStatus.Unanswered -> ""
    }
}

@Composable
private fun feedbackColor(answerStatus: AnswerStatus) = when (answerStatus) {
    AnswerStatus.Correct -> MaterialTheme.colorScheme.primary
    AnswerStatus.Wrong,
    AnswerStatus.Skipped -> MaterialTheme.colorScheme.error
    AnswerStatus.Unanswered -> MaterialTheme.colorScheme.onSurface
}
