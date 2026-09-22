package com.example.seedie.ui.screens.learning.listening

import android.speech.tts.TextToSpeech
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.foundation.BorderStroke
import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.seedie.BuildConfig
import com.example.seedie.data.audio.WordAudioPlayer
import com.example.seedie.domain.model.StudyResult
import com.example.seedie.ui.components.PracticeOptionCard
import com.example.seedie.ui.screens.garden.GardenExitConfirmDialog
import com.example.seedie.ui.screens.garden.PlantSessionGate
import com.example.seedie.ui.screens.learning.assignments.PracticeAssignmentArgs
import com.example.seedie.ui.screens.learning.catalog.FreePracticeArgs
import com.example.seedie.ui.screens.learning.practice.AnswerStatus
import java.util.Locale

@Composable
fun ListeningPracticeRoute(
    assignmentArgs: PracticeAssignmentArgs?,
    freeArgs: FreePracticeArgs?,
    onFinishSession: (StudyResult) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: ListeningPracticeViewModel = hiltViewModel()
) {
    PlantSessionGate { speciesId ->
        ListeningPracticeRouteBody(
            assignmentArgs = assignmentArgs,
            freeArgs = freeArgs,
            speciesId = speciesId,
            onFinishSession = onFinishSession,
            onNavigateBack = onNavigateBack,
            viewModel = viewModel
        )
    }
}

@Composable
private fun ListeningPracticeRouteBody(
    assignmentArgs: PracticeAssignmentArgs?,
    freeArgs: FreePracticeArgs?,
    speciesId: String,
    onFinishSession: (StudyResult) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: ListeningPracticeViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val audioPlayer = remember { WordAudioPlayer(context.applicationContext) }
    var speaker by remember { mutableStateOf<TextToSpeech?>(null) }
    var speakerReady by remember { mutableStateOf(false) }
    var pendingSpeech by remember { mutableStateOf<String?>(null) }
    var audioFeedbackMessage by remember { mutableStateOf<String?>(null) }

    fun logAudioDebug(message: String) {
        if (BuildConfig.DEBUG) {
            Log.d(AUDIO_LOG_TAG, message)
        }
    }

    fun speakFallback(text: String) {
        if (speakerReady) {
            audioPlayer.stop()
            logAudioDebug("TTS speak: $text")
            speaker?.speak(
                text,
                TextToSpeech.QUEUE_FLUSH,
                null,
                text
            )
            pendingSpeech = null
        } else {
            logAudioDebug("TTS not ready, queue pending: $text")
            pendingSpeech = text
        }
    }

    fun playWordAudio(audioUrl: String?, fallbackText: String) {
        speaker?.stop()
        val resolvedUrl = audioUrl?.takeIf { it.isNotBlank() }
        if (resolvedUrl != null) {
            logAudioDebug("playWordAudio ExoPlayer url=$resolvedUrl")
            audioFeedbackMessage = null
            audioPlayer.playUrl(resolvedUrl) {
                logAudioDebug("playWordAudio remote audio failed, fallback to TTS")
                audioFeedbackMessage = "远端音频播放失败，已改用文本播报，请稍后重试。"
                speakFallback(fallbackText)
            }
        } else {
            logAudioDebug("playWordAudio no remote audio, TTS: $fallbackText")
            audioFeedbackMessage = "远端音频当前不可用，已改用文本播报。"
            speakFallback(fallbackText)
        }
    }

    LaunchedEffect(assignmentArgs?.submissionId, assignmentArgs?.mode, freeArgs?.itemRef, speciesId) {
        viewModel.setSelectedSpeciesId(speciesId)
        when {
            assignmentArgs != null -> viewModel.initializeAssignment(assignmentArgs)
            freeArgs != null -> viewModel.initializeFree(freeArgs)
        }
    }

    DisposableEffect(Unit) {
        var textToSpeech: TextToSpeech? = null
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                speakerReady = true
                textToSpeech?.language = Locale.US
            } else {
                logAudioDebug("TextToSpeech init failed status=$status")
            }
        }
        speaker = textToSpeech
        onDispose {
            audioPlayer.release()
            textToSpeech?.stop()
            textToSpeech?.shutdown()
            speaker = null
            speakerReady = false
            pendingSpeech = null
        }
    }

    LaunchedEffect(speakerReady, pendingSpeech) {
        val queued = pendingSpeech
        if (speakerReady && queued != null) {
            logAudioDebug("Playing pending TTS: $queued")
            speakFallback(queued)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.studyResults.collect { result ->
            onFinishSession(result)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.playAudioEvents.collect { event ->
            playWordAudio(event.audioUrl, event.fallbackText)
        }
    }

    ListeningPracticeScreen(
        uiState = uiState,
        audioFeedbackMessage = audioFeedbackMessage,
        onNavigateBack = onNavigateBack,
        onBackClick = viewModel::onBackClick,
        onConfirmExit = viewModel::onConfirmExit,
        onDismissExitDialog = viewModel::onDismissExitDialog,
        onOptionSelected = viewModel::onOptionSelected,
        onSubmitAnswer = viewModel::onSubmitAnswer,
        onNextQuestion = viewModel::onNextQuestion,
        onQuestionPageSelected = viewModel::onQuestionPageSelected,
        onSubmitFreePractice = viewModel::onSubmitFreePractice,
        onReviewFreePracticeAnswers = viewModel::onReviewFreePracticeAnswers,
        onReplayAudio = viewModel::onReplayAudio,
        onRetryLoad = viewModel::onRetryLoad,
        onFinishSession = viewModel::onFinishSession
    )
}

@Composable
private fun ListeningPracticeScreen(
    uiState: ListeningPracticeUiState,
    audioFeedbackMessage: String?,
    onNavigateBack: () -> Unit,
    onBackClick: () -> Unit,
    onConfirmExit: () -> Unit,
    onDismissExitDialog: () -> Unit,
    onOptionSelected: (String) -> Unit,
    onSubmitAnswer: () -> Unit,
    onNextQuestion: () -> Unit,
    onQuestionPageSelected: (Int) -> Unit,
    onSubmitFreePractice: () -> Unit,
    onReviewFreePracticeAnswers: () -> Unit,
    onReplayAudio: () -> Unit,
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
                    OutlinedButton(onClick = onDismissExitDialog) { Text("继续练习") }
                },
                title = { Text("退出回顾？") },
                text = { Text("进度不会改变。") }
            )
        } else {
            GardenExitConfirmDialog(
                answeredQuestionCount = uiState.correctCount + uiState.wrongCount,
                withinAbandonGrace = com.example.seedie.domain.usecase.GardenForestRules
                    .isWithinAbandonGrace(uiState.sessionOpenedAtMillis),
                onConfirmExit = onConfirmExit,
                onContinue = onDismissExitDialog
            )
        }
    }

    when (uiState.stage) {
        ListeningPracticeStage.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        ListeningPracticeStage.Error -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = uiState.errorMessage ?: "加载失败",
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onRetryLoad) { Text("重试") }
                    Button(onClick = onNavigateBack) { Text("返回") }
                }
            }
        }

        ListeningPracticeStage.Completed -> {
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
                Text(
                    text = "共完成 ${uiState.totalQuestionCount} 题，正确 ${uiState.correctCount}，错误 ${uiState.wrongCount}",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 12.dp)
                )
                if (!uiState.isReviewMode) {
                    Text(
                        text = "本局将种入你的花园 · 用时 ${uiState.elapsedSeconds}s",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
                    )
                }
                Row(
                    modifier = Modifier.padding(top = if (uiState.isReviewMode) 24.dp else 0.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (uiState.isFreePractice) {
                        OutlinedButton(onClick = onReviewFreePracticeAnswers) {
                            Text("查看作答")
                        }
                    }
                    Button(onClick = onFinishSession) {
                        Text("完成")
                    }
                }
            }
        }

        ListeningPracticeStage.Ready,
        ListeningPracticeStage.AnswerEvaluated -> {
            ListeningQuestionContent(
                uiState = uiState,
                audioFeedbackMessage = audioFeedbackMessage,
                onBackClick = onBackClick,
                onOptionSelected = onOptionSelected,
                onSubmitAnswer = onSubmitAnswer,
                onNextQuestion = onNextQuestion,
                onQuestionPageSelected = onQuestionPageSelected,
                onSubmitFreePractice = onSubmitFreePractice,
                onReplayAudio = onReplayAudio,
                onFinishSession = onFinishSession
            )
        }
    }
}

@Composable
private fun ListeningQuestionContent(
    uiState: ListeningPracticeUiState,
    audioFeedbackMessage: String?,
    onBackClick: () -> Unit,
    onOptionSelected: (String) -> Unit,
    onSubmitAnswer: () -> Unit,
    onNextQuestion: () -> Unit,
    onQuestionPageSelected: (Int) -> Unit,
    onSubmitFreePractice: () -> Unit,
    onReplayAudio: () -> Unit,
    onFinishSession: () -> Unit
) {
    val material = uiState.currentMaterial ?: return
    val pages = remember(uiState.materials) { uiState.materials.flatMap { it.questions } }
    if (pages.isEmpty()) return
    val evaluated = uiState.stage == ListeningPracticeStage.AnswerEvaluated
    var explanationExpanded by rememberSaveable(uiState.sessionId, uiState.isReviewMode) {
        mutableStateOf(true)
    }
    // Assignment questions unlock after submission; free practice and review allow all pages.
    val pageCount = if (uiState.isFreePractice || uiState.isReviewMode) pages.size else {
        val firstUnanswered = pages.indexOfFirst { it.questionId !in uiState.evaluatedQuestionIds }
        if (firstUnanswered < 0) pages.size else firstUnanswered + 1
    }
    val pagerState = rememberPagerState(
        initialPage = (uiState.currentQuestionOrdinal - 1).coerceAtLeast(0),
        pageCount = { pageCount }
    )
    val selectPage by rememberUpdatedState(onQuestionPageSelected)
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.distinctUntilChanged().collect { selectPage(it) }
    }
    val isLast = uiState.currentQuestionOrdinal >= uiState.totalQuestionCount
    val pageSettled = !pagerState.isScrollInProgress &&
        pagerState.settledPage == uiState.currentQuestionOrdinal - 1

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
            }
            Text("听力训练", modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("${uiState.currentQuestionOrdinal} / ${uiState.totalQuestionCount}",
                style = MaterialTheme.typography.titleMedium)
            if (evaluated) {
                IconButton(onClick = { explanationExpanded = !explanationExpanded },
                    modifier = Modifier.semantics {
                        contentDescription = if (explanationExpanded) "收起解析" else "展开解析"
                    }) {
                    Text(if (explanationExpanded) ">" else "<", color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(material.titleZh ?: material.title, style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.secondary)
                    material.promptText?.takeIf { it.isNotBlank() }?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall)
                    }
                    Text("用时 ${uiState.elapsedSeconds}s", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onReplayAudio, enabled = pageSettled,
                    modifier = Modifier.size(56.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape)) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "播放听力材料",
                        modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
        if (!audioFeedbackMessage.isNullOrBlank()) {
            Text(audioFeedbackMessage, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error)
        }
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            pageSpacing = 16.dp,
            key = { pages[it].questionId },
            verticalAlignment = Alignment.Top
        ) { page ->
            val question = pages[page]
            val selected = uiState.selectedAnswers[question.questionId]
            val feedbackVisible = uiState.isReviewMode || question.questionId in uiState.evaluatedQuestionIds
            val answerStatus = when {
                !feedbackVisible || selected == null -> AnswerStatus.Unanswered
                selected == question.correctOptionId -> AnswerStatus.Correct
                else -> AnswerStatus.Wrong
            }
            Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    modifier = Modifier.weight(if (feedbackVisible && explanationExpanded) 2f else 1f)
                        .fillMaxHeight(),
                    shape = MaterialTheme.shapes.large,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("第 ${page + 1} 题", style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary)
                        Text(question.stem, style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold)
                        question.options.forEach { option ->
                            PracticeOptionCard(
                                option = option,
                                selectedOptionId = selected,
                                answerStatus = answerStatus,
                                interactionEnabled = pageSettled && page == pagerState.settledPage && !feedbackVisible,
                                feedbackVisible = feedbackVisible,
                                onOptionSelected = onOptionSelected,
                                modifier = Modifier.heightIn(min = 64.dp)
                            )
                        }
                    }
                }
                if (feedbackVisible && explanationExpanded) {
                    Surface(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("答案解析", style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text("你的答案：${selected ?: "未作答"}", style = MaterialTheme.typography.bodyMedium)
                            Text("正确答案：${question.correctOptionId}", style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary)
                            Text(when (answerStatus) {
                                AnswerStatus.Correct -> "回答正确"
                                AnswerStatus.Wrong -> "回答错误"
                                else -> "未作答"
                            }, style = MaterialTheme.typography.bodyMedium,
                                color = if (answerStatus == AnswerStatus.Wrong) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.primary)
                            Text(question.explanation?.takeIf { it.isNotBlank() } ?: "暂无解析",
                                style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
        if (uiState.isFreePractice && !uiState.isReviewMode) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("已作答 ${uiState.answeredQuestionCount} / ${uiState.totalQuestionCount}",
                    modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                Button(onClick = onSubmitFreePractice,
                    enabled = pageSettled && uiState.answeredQuestionCount == uiState.totalQuestionCount) { Text("提交") }
            }
        } else if (!uiState.isFreePractice && !evaluated) {
            Button(onClick = onSubmitAnswer, enabled = pageSettled && uiState.canSubmitAnswer,
                modifier = Modifier.align(Alignment.End)) { Text("提交本题") }
        } else if (isLast) {
            Button(onClick = if (uiState.isFreePractice) onFinishSession else onNextQuestion,
                enabled = pageSettled, modifier = Modifier.align(Alignment.End)) {
                Text(if (uiState.isReviewMode) "完成回顾" else "提交作业")
            }
        }
    }
}

private const val AUDIO_LOG_TAG = "SeedieAudio"
