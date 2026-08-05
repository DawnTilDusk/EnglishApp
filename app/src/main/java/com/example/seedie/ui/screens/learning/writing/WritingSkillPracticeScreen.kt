package com.example.seedie.ui.screens.learning.writing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private data class TranslationExercise(
    val prompt: String,
    val hint: String,
    val reference: String
)

private data class ConnectorExercise(
    val sentence: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)

private data class ParaphraseExercise(
    val original: String,
    val targetExpression: String,
    val reference: String
)

private val translationExercises = listOf(
    TranslationExercise(
        prompt = "如果你坚持练习，你的写作会越来越自然。",
        hint = "试着用 keep practicing 和 more and more natural。",
        reference = "If you keep practicing, your writing will become more and more natural."
    ),
    TranslationExercise(
        prompt = "我认为每天记录一件小事是提升表达的好方法。",
        hint = "可以用 I think ... is a good way to ...",
        reference = "I think writing down one small thing every day is a good way to improve expression."
    ),
    TranslationExercise(
        prompt = "虽然任务不难，但按时完成仍然很重要。",
        hint = "注意 although 和 it is still important。",
        reference = "Although the task is not difficult, it is still important to finish it on time."
    )
)

private val connectorExercises = listOf(
    ConnectorExercise(
        sentence = "Many students know a lot of words. _____, they still struggle to organize a clear paragraph.",
        options = listOf("However", "For example", "As a result", "In addition"),
        correctIndex = 0,
        explanation = "前后是转折关系，用 However 最自然。"
    ),
    ConnectorExercise(
        sentence = "Reading model essays is useful. _____, it helps you notice how topic sentences work.",
        options = listOf("Instead", "For example", "Meanwhile", "Otherwise"),
        correctIndex = 1,
        explanation = "后半句是在举具体作用，所以用 For example。"
    ),
    ConnectorExercise(
        sentence = "First, list your ideas clearly. _____, turn them into a short paragraph.",
        options = listOf("Besides", "Finally", "Similarly", "After all"),
        correctIndex = 1,
        explanation = "这里是步骤推进，前面 First，后面接 Finally 更顺。"
    )
)

private val paraphraseExercises = listOf(
    ParaphraseExercise(
        original = "The city changed a lot in a short time.",
        targetExpression = "underwent significant changes",
        reference = "The city underwent significant changes in a short time."
    ),
    ParaphraseExercise(
        original = "She was very happy about the result.",
        targetExpression = "delighted",
        reference = "She was delighted with the result."
    ),
    ParaphraseExercise(
        original = "We should use simple words to explain the idea.",
        targetExpression = "convey",
        reference = "We should use simple words to convey the idea."
    )
)

@Composable
fun WritingSentenceTranslationRoute(
    onNavigateBack: () -> Unit
) {
    WritingOpenAnswerPracticeScreen(
        title = "句子翻译",
        intro = "先自己写，再对照参考答案做微调。",
        items = translationExercises.size,
        onNavigateBack = onNavigateBack
    ) { index, showReference ->
        val exercise = translationExercises[index]
        OpenAnswerExerciseContent(
            promptTitle = "中文句子",
            prompt = exercise.prompt,
            supportText = exercise.hint,
            referenceTitle = "参考译文",
            reference = exercise.reference,
            showReference = showReference
        )
    }
}

@Composable
fun WritingParaphraseRoute(
    onNavigateBack: () -> Unit
) {
    WritingOpenAnswerPracticeScreen(
        title = "同义改写",
        intro = "尝试把句子换一种更自然的表达方式。",
        items = paraphraseExercises.size,
        onNavigateBack = onNavigateBack
    ) { index, showReference ->
        val exercise = paraphraseExercises[index]
        OpenAnswerExerciseContent(
            promptTitle = "原句",
            prompt = exercise.original,
            supportText = "请在改写中用上：${exercise.targetExpression}",
            referenceTitle = "参考改写",
            reference = exercise.reference,
            showReference = showReference
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WritingConnectorDrillRoute(
    onNavigateBack: () -> Unit
) {
    var currentIndex by rememberSaveable { mutableStateOf(0) }
    var selectedIndex by rememberSaveable { mutableStateOf(-1) }
    var submitted by rememberSaveable { mutableStateOf(false) }
    var correctCount by rememberSaveable { mutableStateOf(0) }
    var completed by rememberSaveable { mutableStateOf(false) }

    if (completed) {
        PracticeSummaryScreen(
            title = "衔接词训练",
            summary = "本轮答对 $correctCount / ${connectorExercises.size} 题",
            onNavigateBack = onNavigateBack,
            onRestart = {
                currentIndex = 0
                selectedIndex = -1
                submitted = false
                correctCount = 0
                completed = false
            }
        )
        return
    }

    val exercise = connectorExercises[currentIndex]
    val isCorrect = selectedIndex == exercise.correctIndex

    PracticeScaffold(
        title = "衔接词训练",
        onNavigateBack = onNavigateBack
    ) {
        Text(
            text = "第 ${currentIndex + 1} / ${connectorExercises.size} 题",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "选出最合适的连接词",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Text(
                text = exercise.sentence,
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyLarge
            )
        }

        exercise.options.forEachIndexed { index, option ->
            Card(
                onClick = {
                    if (!submitted) {
                        selectedIndex = index
                    }
                },
                colors = CardDefaults.cardColors(
                    containerColor = optionBackground(
                        submitted = submitted,
                        isSelected = selectedIndex == index,
                        isCorrect = index == exercise.correctIndex
                    )
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    RadioButton(
                        selected = selectedIndex == index,
                        onClick = {
                            if (!submitted) {
                                selectedIndex = index
                            }
                        }
                    )
                    Text(
                        text = option,
                        modifier = Modifier.padding(top = 12.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }

        if (submitted) {
            Text(
                text = if (isCorrect) "回答正确" else "再看一下逻辑关系",
                color = if (isCorrect) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                },
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = exercise.explanation,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        if (!submitted) {
            Button(
                onClick = {
                    submitted = true
                    if (isCorrect) {
                        correctCount += 1
                    }
                },
                enabled = selectedIndex >= 0,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("提交答案")
            }
        } else {
            Button(
                onClick = {
                    if (currentIndex == connectorExercises.lastIndex) {
                        completed = true
                    } else {
                        currentIndex += 1
                        selectedIndex = -1
                        submitted = false
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (currentIndex == connectorExercises.lastIndex) "完成本轮" else "下一题")
            }
        }
    }
}

@Composable
private fun WritingOpenAnswerPracticeScreen(
    title: String,
    intro: String,
    items: Int,
    onNavigateBack: () -> Unit,
    content: @Composable (index: Int, showReference: Boolean) -> Unit
) {
    var currentIndex by rememberSaveable { mutableStateOf(0) }
    var answer by rememberSaveable { mutableStateOf("") }
    var showReference by rememberSaveable { mutableStateOf(false) }
    var completed by rememberSaveable { mutableStateOf(false) }

    if (completed) {
        PracticeSummaryScreen(
            title = title,
            summary = "这一轮已完成，共练习 $items 题",
            onNavigateBack = onNavigateBack,
            onRestart = {
                currentIndex = 0
                answer = ""
                showReference = false
                completed = false
            }
        )
        return
    }

    PracticeScaffold(
        title = title,
        onNavigateBack = onNavigateBack
    ) {
        Text(
            text = "第 ${currentIndex + 1} / $items 题",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = intro,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        content(currentIndex, showReference)

        OutlinedTextField(
            value = answer,
            onValueChange = { answer = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
            label = { Text("先写下你的答案") }
        )

        if (!showReference) {
            Button(
                onClick = { showReference = true },
                enabled = answer.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("查看参考答案")
            }
        } else {
            Button(
                onClick = {
                    if (currentIndex == items - 1) {
                        completed = true
                    } else {
                        currentIndex += 1
                        answer = ""
                        showReference = false
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (currentIndex == items - 1) "完成本轮" else "下一题")
            }
        }
    }
}

@Composable
private fun OpenAnswerExerciseContent(
    promptTitle: String,
    prompt: String,
    supportText: String,
    referenceTitle: String,
    reference: String,
    showReference: Boolean
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = promptTitle,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = prompt,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = supportText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (showReference) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = referenceTitle,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = reference,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PracticeScaffold(
    title: String,
    onNavigateBack: () -> Unit,
    content: @Composable () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun PracticeSummaryScreen(
    title: String,
    summary: String,
    onNavigateBack: () -> Unit,
    onRestart: () -> Unit
) {
    PracticeScaffold(
        title = title,
        onNavigateBack = onNavigateBack
    ) {
        Text(
            text = "练习完成",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = summary,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(
            onClick = onRestart,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("再练一轮")
        }
        OutlinedButton(
            onClick = onNavigateBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("返回写作训练")
        }
    }
}

@Composable
private fun optionBackground(
    submitted: Boolean,
    isSelected: Boolean,
    isCorrect: Boolean
): Color {
    return when {
        submitted && isCorrect -> MaterialTheme.colorScheme.primaryContainer
        submitted && isSelected -> MaterialTheme.colorScheme.errorContainer
        isSelected -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.surface
    }
}
