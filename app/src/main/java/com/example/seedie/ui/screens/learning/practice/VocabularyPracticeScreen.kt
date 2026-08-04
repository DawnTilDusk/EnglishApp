package com.example.seedie.ui.screens.learning.practice

import android.speech.tts.TextToSpeech
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.seedie.domain.model.StudyResult
import com.example.seedie.ui.components.PracticeOptionCard
import com.example.seedie.ui.theme.gardenShadow
import java.util.Locale

@Composable
fun VocabularyPracticeRoute(
    args: VocabularyPracticeArgs = VocabularyPracticeArgs(),
    onFinishSession: (StudyResult) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: VocabularyPracticeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var speaker by remember { mutableStateOf<TextToSpeech?>(null) }
    var speakerReady by remember { mutableStateOf(false) }

    LaunchedEffect(args) {
        viewModel.initialize(args)
    }

    DisposableEffect(context) {
        var textToSpeech: TextToSpeech? = null
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                speakerReady = true
                textToSpeech?.language = Locale.US
            }
        }
        speaker = textToSpeech
        onDispose {
            textToSpeech?.stop()
            textToSpeech?.shutdown()
            speaker = null
            speakerReady = false
        }
    }

    LaunchedEffect(Unit) {
        viewModel.studyResults.collect { result ->
            onFinishSession(result)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.pronunciationEvents.collect { english ->
            if (speakerReady) {
                speaker?.speak(english, TextToSpeech.QUEUE_FLUSH, null, english)
            }
        }
    }

    VocabularyPracticeScreen(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onBackClick = viewModel::onBackClick,
        onConfirmExit = viewModel::onConfirmExit,
        onDismissExitDialog = viewModel::onDismissExitDialog,
        onOptionSelected = viewModel::onOptionSelected,
        onSpellingInputChanged = viewModel::onSpellingInputChanged,
        onSubmitAnswer = viewModel::onSubmitAnswer,
        onRevealAnswer = viewModel::onRevealAnswer,
        onRevealPhoneticHint = viewModel::onRevealPhoneticHint,
        onNextQuestion = viewModel::onNextQuestion,
        onReplayPronunciation = viewModel::onReplayPronunciation,
        onRetryLoad = viewModel::onRetryLoad,
        onFinishSession = viewModel::onFinishSession,
        onStartImmediateReview = viewModel::onStartImmediateReview,
        onDeferReview = viewModel::onDeferReview
    )
}

@Composable
private fun VocabularyPracticeScreen(
    uiState: VocabularyPracticeUiState,
    onNavigateBack: () -> Unit,
    onBackClick: () -> Unit,
    onConfirmExit: () -> Unit,
    onDismissExitDialog: () -> Unit,
    onOptionSelected: (String) -> Unit,
    onSpellingInputChanged: (String) -> Unit,
    onSubmitAnswer: () -> Unit,
    onRevealAnswer: () -> Unit,
    onRevealPhoneticHint: () -> Unit,
    onNextQuestion: () -> Unit,
    onReplayPronunciation: () -> Unit,
    onRetryLoad: () -> Unit,
    onFinishSession: () -> Unit,
    onStartImmediateReview: () -> Unit,
    onDeferReview: () -> Unit
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
            text = { Text("已完成的作答会被记录，但你将离开当前练习。") }
        )
    }

    when (uiState.stage) {
        VocabularyPracticeStage.Loading -> LoadingState()
        VocabularyPracticeStage.Empty -> EmptyState(onBackClick = onNavigateBack)
        VocabularyPracticeStage.Error -> ErrorState(
            message = uiState.errorMessage ?: "词包加载失败",
            onRetryLoad = onRetryLoad,
            onBackClick = onBackClick
        )
        VocabularyPracticeStage.Completed -> CompletedState(
            uiState = uiState,
            onBackClick = onBackClick,
            onFinishSession = onFinishSession,
            onStartImmediateReview = onStartImmediateReview,
            onDeferReview = onDeferReview
        )
        VocabularyPracticeStage.Ready,
        VocabularyPracticeStage.AnswerEvaluated -> PracticeContent(
            uiState = uiState,
            onBackClick = onBackClick,
            onOptionSelected = onOptionSelected,
            onSpellingInputChanged = onSpellingInputChanged,
            onSubmitAnswer = onSubmitAnswer,
            onRevealAnswer = onRevealAnswer,
            onRevealPhoneticHint = onRevealPhoneticHint,
            onNextQuestion = onNextQuestion,
            onReplayPronunciation = onReplayPronunciation
        )
    }
}

@Composable
private fun PracticeContent(
    uiState: VocabularyPracticeUiState,
    onBackClick: () -> Unit,
    onOptionSelected: (String) -> Unit,
    onSpellingInputChanged: (String) -> Unit,
    onSubmitAnswer: () -> Unit,
    onRevealAnswer: () -> Unit,
    onRevealPhoneticHint: () -> Unit,
    onNextQuestion: () -> Unit,
    onReplayPronunciation: () -> Unit
) {
    val currentPrompt = uiState.currentPrompt ?: return
    val totalStudy = uiState.session?.studyWords?.size?.coerceAtLeast(1) ?: 1
    val totalReview = uiState.session?.reviewWords?.size?.coerceAtLeast(1) ?: 1
    val progress = when (uiState.currentSection) {
        VocabularyPracticeMode.Study -> uiState.masteredStudyCount / totalStudy.toFloat()
        VocabularyPracticeMode.Review -> uiState.completedReviewCount / totalReview.toFloat()
    }
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
                    text = progressText(uiState = uiState, totalStudy = totalStudy, totalReview = totalReview),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
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
                    verticalArrangement = Arrangement.spacedBy(
                        if (currentPrompt.questionType == VocabularyQuestionType.StudyContextChoice) 18.dp else 14.dp
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = headlineText(currentPrompt),
                                    modifier = if (
                                        currentPrompt.questionType == VocabularyQuestionType.StudyContextChoice &&
                                        uiState.stage != VocabularyPracticeStage.AnswerEvaluated
                                    ) {
                                        Modifier.blur(12.dp)
                                    } else {
                                        Modifier
                                    },
                                    style = if (currentPrompt.questionType == VocabularyQuestionType.StudyContextChoice) {
                                        MaterialTheme.typography.displayMedium
                                    } else {
                                        MaterialTheme.typography.displaySmall
                                    },
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                val stageChip = stageChipText(currentPrompt)
                                if (stageChip != null) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                        shape = CircleShape
                                    ) {
                                        Text(
                                            text = stageChip,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                            if (currentPrompt.questionType == VocabularyQuestionType.StudyChineseToEnglish) {
                                ChineseToEnglishHintRow(
                                    uiState = uiState,
                                    currentPrompt = currentPrompt,
                                    onRevealPhoneticHint = onRevealPhoneticHint
                                )
                            } else if (currentPrompt.helperText.isNotBlank()) {
                                Text(
                                    text = currentPrompt.helperText,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
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
                                    text = queueStatusText(uiState),
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

                    PromptCard(
                        uiState = uiState,
                        currentPrompt = currentPrompt,
                        isAuxPanelExpanded = isAuxPanelExpanded
                    )

                    when (currentPrompt.questionType) {
                        VocabularyQuestionType.ReviewSpelling -> {
                            ReviewInputCard(
                                uiState = uiState,
                                currentPrompt = currentPrompt,
                                onValueChange = onSpellingInputChanged
                            )
                        }

                        VocabularyQuestionType.StudyEnglishToChinese,
                        VocabularyQuestionType.StudyChineseToEnglish,
                        VocabularyQuestionType.StudyContextChoice -> {
                            Column(
                                modifier = Modifier.padding(top = if (currentPrompt.questionType == VocabularyQuestionType.StudyContextChoice) 6.dp else 0.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                currentPrompt.optionList.forEach { option ->
                                    PracticeOptionCard(
                                        option = option,
                                        selectedOptionId = uiState.selectedOptionId,
                                        answerStatus = uiState.answerStatus,
                                        interactionEnabled = uiState.stage == VocabularyPracticeStage.Ready,
                                        feedbackVisible = uiState.stage == VocabularyPracticeStage.AnswerEvaluated,
                                        onOptionSelected = onOptionSelected
                                    )
                                }
                            }
                        }
                    }

                    if (
                        uiState.stage == VocabularyPracticeStage.AnswerEvaluated &&
                        currentPrompt.questionType != VocabularyQuestionType.StudyContextChoice
                    ) {
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
                                    text = currentPrompt.correctAnswerText,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = uiState.feedbackMessage,
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
                                        text = "练习状态",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "阶段一使用滚动学习队列：学会一个，顺序补入一个新词。",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    SummaryRow("活跃队列", "${uiState.studyQueueSize} 个")
                                    SummaryRow("已引入", "${uiState.introducedStudyCount} / ${uiState.studyTargetCount}")
                                    SummaryRow("已掌握", "${uiState.masteredStudyCount} 个")
                                    SummaryRow("复习队列", "${uiState.reviewQueueSize} 个")
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
                if (uiState.stage == VocabularyPracticeStage.AnswerEvaluated) {
                    OutlinedButton(
                        onClick = onReplayPronunciation,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("再听一遍")
                    }
                    Button(
                        onClick = onNextQuestion,
                        modifier = Modifier.weight(2f)
                    ) {
                        Text(nextButtonLabel(uiState))
                    }
                } else {
                    OutlinedButton(
                        onClick = if (currentPrompt.section == VocabularyPracticeMode.Study) {
                            onRevealAnswer
                        } else {
                            onReplayPronunciation
                        },
                        modifier = Modifier.weight(1f),
                        enabled = uiState.stage == VocabularyPracticeStage.Ready
                    ) {
                        Text(
                            if (currentPrompt.section == VocabularyPracticeMode.Study) {
                                "没见过，直接看答案"
                            } else {
                                "播放发音"
                            }
                        )
                    }
                    Button(
                        onClick = onSubmitAnswer,
                        modifier = Modifier.weight(2f),
                        enabled = uiState.canSubmitAnswer
                    ) {
                        Text(if (currentPrompt.questionType == VocabularyQuestionType.ReviewSpelling) "提交拼写" else "提交答案")
                    }
                }
            }
        }
    }
}

@Composable
private fun PromptCard(
    uiState: VocabularyPracticeUiState,
    currentPrompt: VocabularyPracticePrompt,
    isAuxPanelExpanded: Boolean
) {
    val isContextPrompt = currentPrompt.questionType == VocabularyQuestionType.StudyContextChoice
    val shouldBlur = uiState.stage != VocabularyPracticeStage.AnswerEvaluated &&
        currentPrompt.questionType == VocabularyQuestionType.StudyChineseToEnglish
    val promptText = when (currentPrompt.questionType) {
        VocabularyQuestionType.StudyEnglishToChinese,
        VocabularyQuestionType.StudyChineseToEnglish -> currentPrompt.word.exampleSentence
        VocabularyQuestionType.StudyContextChoice -> currentPrompt.promptTitle
        VocabularyQuestionType.ReviewSpelling -> currentPrompt.promptBody
    }
    val overlayText = when (currentPrompt.questionType) {
        VocabularyQuestionType.StudyEnglishToChinese -> ""
        VocabularyQuestionType.StudyChineseToEnglish -> "作答之后展示例句"
        VocabularyQuestionType.StudyContextChoice -> ""
        VocabularyQuestionType.ReviewSpelling -> "5 秒无操作将提示首字母"
    }

    Surface(
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
        shape = MaterialTheme.shapes.medium
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = if (isContextPrompt) 72.dp else 0.dp)
                .padding(horizontal = 16.dp, vertical = if (isContextPrompt) 10.dp else 12.dp)
        ) {
            Text(
                text = promptText,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (shouldBlur) Modifier.blur(10.dp) else Modifier),
                style = if (isContextPrompt) {
                    MaterialTheme.typography.titleLarge
                } else {
                    MaterialTheme.typography.bodyLarge
                },
                color = if (isContextPrompt) Color(0xFF7A5230) else MaterialTheme.colorScheme.onSurface,
                maxLines = if (isContextPrompt) {
                    if (isAuxPanelExpanded) 3 else 2
                } else {
                    if (isAuxPanelExpanded) 4 else 2
                },
                overflow = TextOverflow.Ellipsis
            )
            if (overlayText.isNotBlank() && uiState.stage != VocabularyPracticeStage.AnswerEvaluated) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = overlayText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.secondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun ChineseToEnglishHintRow(
    uiState: VocabularyPracticeUiState,
    currentPrompt: VocabularyPracticePrompt,
    onRevealPhoneticHint: () -> Unit
) {
    val shouldRevealPhonetic = uiState.stage == VocabularyPracticeStage.AnswerEvaluated || uiState.showPhoneticHint
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        IconButton(
            onClick = onRevealPhoneticHint,
            enabled = uiState.stage == VocabularyPracticeStage.Ready && !uiState.showPhoneticHint
        ) {
            Icon(
                imageVector = Icons.Default.Lightbulb,
                contentDescription = "Reveal phonetic hint",
                tint = if (uiState.stage == VocabularyPracticeStage.Ready && !uiState.showPhoneticHint) {
                    MaterialTheme.colorScheme.tertiary
                } else {
                    MaterialTheme.colorScheme.secondary
                }
            )
        }
        Text(
            text = currentPrompt.word.phonetic,
            modifier = if (shouldRevealPhonetic) Modifier else Modifier.blur(10.dp),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}

@Composable
private fun ReviewInputCard(
    uiState: VocabularyPracticeUiState,
    currentPrompt: VocabularyPracticePrompt,
    onValueChange: (String) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = uiState.spellingInput,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.stage == VocabularyPracticeStage.Ready,
                singleLine = true,
                label = { Text("请输入英文单词") },
                placeholder = { Text("例如：${currentPrompt.firstLetterHint ?: "_"}...") },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    keyboardType = KeyboardType.Text
                )
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "提示倒计时 ${uiState.reviewHintCountdownSec}s",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                if (uiState.showFirstLetterHint && !uiState.firstLetterHint.isNullOrBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        shape = CircleShape
                    ) {
                        Text(
                            text = "首字母 ${uiState.firstLetterHint}",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
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
            modifier = Modifier.size(34.dp),
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
    onFinishSession: () -> Unit,
    onStartImmediateReview: () -> Unit,
    onDeferReview: () -> Unit
) {
    val totalAnswered = uiState.correctCount + uiState.wrongCount + uiState.skippedCount
    val accuracy = if (totalAnswered == 0) 0 else (uiState.correctCount * 100 / totalAnswered)
    val isStudyCompletion = uiState.canStartImmediateReview

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
                if (isStudyCompletion) {
                    Text(
                        text = "本轮已有 ${uiState.pendingReviewWordCount} 个词进入待复习，可立即开始拼写复习。",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                SummaryRow("已掌握学习词", uiState.masteredStudyCount.toString())
                SummaryRow("本轮已引入", "${uiState.introducedStudyCount} / ${uiState.studyTargetCount}")
                SummaryRow("完成题数", totalAnswered.toString())
                SummaryRow("正确题数", uiState.correctCount.toString())
                SummaryRow("错误题数", uiState.wrongCount.toString())
                SummaryRow("跳过题数", uiState.skippedCount.toString())
                SummaryRow("学习时长", "${uiState.elapsedSeconds}s")
                SummaryRow("词汇增量", "+${uiState.masteredStudyCount}")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = if (isStudyCompletion) onDeferReview else onBackClick,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isStudyCompletion) "稍后再说" else "返回学习中心")
                    }
                    Button(
                        onClick = if (isStudyCompletion) onStartImmediateReview else onFinishSession,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isStudyCompletion) "开始拼写复习" else "完成并结算")
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

@Composable
private fun feedbackColor(answerStatus: AnswerStatus) = when (answerStatus) {
    AnswerStatus.Correct -> MaterialTheme.colorScheme.primary
    AnswerStatus.Wrong,
    AnswerStatus.Skipped,
    AnswerStatus.Revealed,
    AnswerStatus.TimedOut -> MaterialTheme.colorScheme.error
    AnswerStatus.Unanswered -> MaterialTheme.colorScheme.onSurface
}

private fun progressText(
    uiState: VocabularyPracticeUiState,
    totalStudy: Int,
    totalReview: Int
): String {
    return when (uiState.currentSection) {
        VocabularyPracticeMode.Study -> "已掌握 ${uiState.masteredStudyCount} / $totalStudy"
        VocabularyPracticeMode.Review -> "已完成复习 ${uiState.completedReviewCount} / $totalReview"
    }
}

private fun queueStatusText(uiState: VocabularyPracticeUiState): String {
    return if (uiState.currentSection == VocabularyPracticeMode.Study) {
        "活跃队列 ${uiState.studyQueueSize} 个 · 已引入 ${uiState.introducedStudyCount}/${uiState.studyTargetCount}"
    } else {
        "复习队列 ${uiState.reviewQueueSize} 个"
    }
}

private fun nextButtonLabel(uiState: VocabularyPracticeUiState): String {
    return if (uiState.studyQueueSize == 0 && uiState.reviewQueueSize == 0) {
        "查看结果"
    } else {
        "下一题"
    }
}

private fun stageChipText(prompt: VocabularyPracticePrompt): String? {
    return when (prompt.questionType) {
        VocabularyQuestionType.StudyEnglishToChinese,
        VocabularyQuestionType.StudyChineseToEnglish,
        VocabularyQuestionType.StudyContextChoice -> prompt.stageTitle
        VocabularyQuestionType.ReviewSpelling -> null
    }
}

private fun headlineText(prompt: VocabularyPracticePrompt): String {
    return if (prompt.questionType == VocabularyQuestionType.StudyContextChoice) {
        prompt.correctAnswerText
    } else {
        prompt.promptTitle
    }
}
