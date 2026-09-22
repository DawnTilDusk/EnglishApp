package com.example.seedie.ui.screens.learning.listening

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp

@Composable
internal fun ListeningTranscriptReview(
    materials: List<ListeningMaterialItem>,
    onQuestionClick: (String) -> Unit,
    onPlayMaterial: (ListeningMaterialItem) -> Unit,
    audioPlaying: Boolean,
    audioPaused: Boolean,
    onToggleAudio: () -> Unit
) {
    val questions = remember(materials) { materials.flatMap { it.questions } }
    var chooseQuestions by remember { mutableStateOf<List<String>>(emptyList()) }
    if (chooseQuestions.isNotEmpty()) {
        AlertDialog(onDismissRequest = { chooseQuestions = emptyList() },
            title = { Text("这句话对应多道题") },
            text = {
                Column {
                    chooseQuestions.forEach { id ->
                        val index = questions.indexOfFirst { it.questionId == id }
                        if (index >= 0) TextButton(onClick = { chooseQuestions = emptyList(); onQuestionClick(id) }) {
                            Text("第 ${index + 1} 题 · ${questions[index].stem}")
                        }
                    }
                }
            }, confirmButton = { TextButton(onClick = { chooseQuestions = emptyList() }) { Text("取消") } })
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("听力原文对照", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            if (audioPlaying || audioPaused) TextButton(onClick = onToggleAudio) {
                Text(if (audioPlaying) "暂停播放" else "继续播放")
            }
        }
        Text("高亮内容为答题关键句，点击可回到对应题目。", style = MaterialTheme.typography.bodySmall)
        materials.forEach { material ->
            Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(material.titleZh ?: material.title, style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = { onPlayMaterial(material) }) { Text("播放本篇") }
                    val segments = remember(material) {
                        listeningTranscriptSegments(material.transcript.orEmpty(), material.questions.map { it.questionId })
                    }
                    val highlightStyle = TextLinkStyles(style = SpanStyle(
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        background = MaterialTheme.colorScheme.primaryContainer,
                        fontWeight = FontWeight.SemiBold
                    ))
                    val annotated = buildAnnotatedString {
                        segments.forEach { segment ->
                            if (segment.questionIds.isEmpty()) append(segment.text)
                            else withLink(LinkAnnotation.Clickable(
                                tag = segment.questionIds.joinToString(","),
                                styles = highlightStyle,
                                linkInteractionListener = {
                                    if (segment.questionIds.size == 1) onQuestionClick(segment.questionIds.single())
                                    else chooseQuestions = segment.questionIds
                                }
                            )) { append(segment.text) }
                        }
                    }
                    if (material.transcript.isNullOrBlank()) Text("暂无原文")
                    else Text(annotated, style = MaterialTheme.typography.bodyLarge)
                    if (segments.none { it.questionIds.isNotEmpty() }) {
                        Text("这篇材料暂未标注关键句。", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
