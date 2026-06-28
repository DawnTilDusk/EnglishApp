package com.example.seedie.ui.screens.learning.quiz

import android.speech.tts.TextToSpeech
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.LinearProgressIndicator
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
import com.example.seedie.domain.model.StudyResult
import com.example.seedie.domain.quiz.VocabularyQuizConstants
import com.example.seedie.ui.components.PracticeOptionCard
import com.example.seedie.ui.screens.learning.practice.AnswerStatus
import com.example.seedie.ui.theme.gardenShadow
import java.util.Locale

@Composable
fun VocabularyQuizRoute(
    onFinishSession: (StudyResult) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: VocabularyQuizViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var speaker by remember { mutableStateOf<TextToSpeech?>(null) }
    var speakerReady by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.initialize()
    }

    DisposableEffect(Unit) {
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

    VocabularyQuizScreen(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onBackClick = viewModel::onBackClick,
        onConfirmExit = viewModel::onConfirmExit,
        onDismissExitDialog = viewModel::onDismissExitDialog,
        onOptionSelected = viewModel::onOptionSelected,
        onSubmitAnswer = viewModel::onSubmitAnswer,
        onNextQuestion = viewModel::onNextQuestion,
        onRetryLoad = viewModel::onRetryLoad,
        onFinishSession = viewModel::onFinishSession,
        onPlayPronunciation = { english ->
            if (speakerReady) {
                speaker?.speak(english, TextToSpeech.QUEUE_FLUSH, null, english)
            }
        }
    )
}

@Composable
private fun VocabularyQuizScreen(
    uiState: VocabularyQuizUiState,
    onNavigateBack: () -> Unit,
    onBackClick: () -> Unit,
    onConfirmExit: () -> Unit,
    onDismissExitDialog: () -> Unit,
    onOptionSelected: (String) -> Unit,
    onSubmitAnswer: () -> Unit,
    onNextQuestion: () -> Unit,
    onRetryLoad: () -> Unit,
    onFinishSession: () -> Unit,
    onPlayPronunciation: (String) -> Unit
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
                    Text("继续测验")
                }
            },
            title = { Text("退出后将结束本次词汇测验") },
            text = { Text("已完成的作答会被记录，但你将离开当前测验。") }
        )
    }

    when (uiState.stage) {
        VocabularyQuizStage.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        VocabularyQuizStage.Error -> {
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

        VocabularyQuizStage.Completed -> {
            val totalTokens = uiState.earnedTokens + VocabularyQuizConstants.COMPLETION_BONUS
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("词汇量估算", style = MaterialTheme.typography.headlineMedium)
                Text(
                    text = "约 ${uiState.estimatedVocabulary ?: 0} 词",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 12.dp)
                )
                Text(
                    text = VocabularyQuizConstants.ESTIMATE_DISCLAIMER,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
                )
                Text(
                    text = "正确 ${uiState.correctCount} / ${uiState.totalCount} · 获得 $totalTokens 代币",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                if (uiState.wrongWords.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "错词回顾",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        uiState.wrongWords.forEach { wrongWord ->
                            Text(
                                text = "${wrongWord.english} — ${wrongWord.translation}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                Button(onClick = onFinishSession) {
                    Text("返回学习中心")
                }
            }
        }

        VocabularyQuizStage.Ready,
        VocabularyQuizStage.AnswerEvaluated -> {
            val question = uiState.currentQuestion ?: return
            val progress = if (uiState.totalCount == 0) {
                0f
            } else {
                (uiState.currentIndex + 1).toFloat() / uiState.totalCount
            }
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
                        verticalArrangement = Arrangement.spacedBy(16.dp)
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
                                    contentDescription = "返回"
                                )
                                Text(
                                    text = "词汇测验",
                                    modifier = Modifier.padding(start = 8.dp),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("第 ${uiState.currentIndex + 1} / ${uiState.totalCount} 题")
                                Text("用时 ${uiState.elapsedSeconds}s")
                            }
                        }

                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "选择正确的中文释义",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = question.english,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${question.phonetic}  ${question.partOfSpeech}",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            IconButton(
                                onClick = { onPlayPronunciation(question.english) },
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                        shape = CircleShape
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "播放单词发音",
                                    modifier = Modifier.size(32.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            if (uiState.stage == VocabularyQuizStage.AnswerEvaluated) {
                                Text(
                                    text = uiState.feedbackMessage,
                                    color = if (uiState.answerStatus == AnswerStatus.Correct) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.error
                                    }
                                )
                            }
                        }

                        question.options.forEach { option ->
                            PracticeOptionCard(
                                option = option,
                                selectedOptionId = uiState.selectedOptionId,
                                answerStatus = uiState.answerStatus,
                                interactionEnabled = uiState.stage == VocabularyQuizStage.Ready,
                                feedbackVisible = uiState.stage == VocabularyQuizStage.AnswerEvaluated,
                                onOptionSelected = onOptionSelected
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            when (uiState.stage) {
                                VocabularyQuizStage.Ready -> {
                                    Button(
                                        onClick = onSubmitAnswer,
                                        enabled = uiState.canSubmitAnswer
                                    ) {
                                        Text("提交")
                                    }
                                }

                                VocabularyQuizStage.AnswerEvaluated -> {
                                    Button(onClick = onNextQuestion) {
                                        Text(
                                            if (uiState.currentIndex + 1 >= uiState.totalCount) {
                                                "查看结果"
                                            } else {
                                                "下一题"
                                            }
                                        )
                                    }
                                }

                                else -> Unit
                            }
                        }
                    }
                }
            }
        }
    }
}
