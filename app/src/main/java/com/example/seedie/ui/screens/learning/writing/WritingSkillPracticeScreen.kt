package com.example.seedie.ui.screens.learning.writing

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.seedie.ui.components.TabSectionSurface
import com.example.seedie.ui.theme.gardenShadow

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

@Composable
fun WritingConnectorDrillRoute(
    onNavigateBack: () -> Unit
) {
    var currentIndex by rememberSaveable { mutableIntStateOf(0) }
    var selectedIndex by rememberSaveable { mutableIntStateOf(-1) }
    var submitted by rememberSaveable { mutableStateOf(false) }
    var correctCount by rememberSaveable { mutableIntStateOf(0) }
    var completed by rememberSaveable { mutableStateOf(false) }

    if (completed) {
        PracticeSummaryScreen(
            title = "衔接词训练",
            headline = "本轮答对 $correctCount / ${connectorExercises.size} 题",
            subline = "回到写作训练继续下一项练习，或再来一轮巩固熟练度。",
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
    val progress = (currentIndex + if (submitted) 1 else 0) / connectorExercises.size.toFloat()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        PracticeHeader(
            title = "衔接词训练",
            progressLabel = "第 ${currentIndex + 1} / ${connectorExercises.size} 题",
            progress = progress,
            onNavigateBack = onNavigateBack
        )

        TabSectionSurface(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "选出最合适的连接词",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                PromptBubble(text = exercise.sentence)

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    exercise.options.forEachIndexed { index, option ->
                        WritingChoiceOption(
                            label = option,
                            index = index,
                            isSelected = selectedIndex == index,
                            isCorrect = index == exercise.correctIndex,
                            submitted = submitted,
                            enabled = !submitted,
                            onSelect = { selectedIndex = index }
                        )
                    }
                }

                if (submitted) {
                    FeedbackPanel(
                        isPositive = isCorrect,
                        title = if (isCorrect) "回答正确" else "再看一下逻辑关系",
                        message = exercise.explanation
                    )
                }
            }
        }

        PracticeActionBar {
            if (!submitted) {
                OutlinedButton(
                    onClick = {
                        submitted = true
                    },
                    modifier = Modifier.weight(1f),
                    enabled = selectedIndex >= 0
                ) {
                    Text("先看看答案")
                }
                Button(
                    onClick = {
                        submitted = true
                        if (isCorrect) {
                            correctCount += 1
                        }
                    },
                    modifier = Modifier.weight(2f),
                    enabled = selectedIndex >= 0
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
                    Text(if (currentIndex == connectorExercises.lastIndex) "查看结果" else "下一题")
                }
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
    var currentIndex by rememberSaveable { mutableIntStateOf(0) }
    var answer by rememberSaveable { mutableStateOf("") }
    var showReference by rememberSaveable { mutableStateOf(false) }
    var completed by rememberSaveable { mutableStateOf(false) }

    if (completed) {
        PracticeSummaryScreen(
            title = title,
            headline = "本轮已完成 $items 题",
            subline = "多练几遍就能形成语感，继续下一项或再来一轮吧。",
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

    val progress = (currentIndex + if (showReference) 1 else 0) / items.toFloat()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        PracticeHeader(
            title = title,
            progressLabel = "第 ${currentIndex + 1} / $items 题",
            progress = progress,
            onNavigateBack = onNavigateBack
        )

        TabSectionSurface(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = intro,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary
                )

                content(currentIndex, showReference)

                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                ) {
                    OutlinedTextField(
                        value = answer,
                        onValueChange = { answer = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        label = { Text("先写下你的答案") }
                    )
                }
            }
        }

        PracticeActionBar {
            if (!showReference) {
                Button(
                    onClick = { showReference = true },
                    enabled = answer.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("查看参考答案")
                }
            } else {
                OutlinedButton(
                    onClick = { showReference = false },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("再改一改")
                }
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
                    modifier = Modifier.weight(2f)
                ) {
                    Text(if (currentIndex == items - 1) "查看结果" else "下一题")
                }
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
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = promptTitle,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
        PromptBubble(text = prompt)
        Text(
            text = supportText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary
        )

        if (showReference) {
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = referenceTitle,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = reference,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun PracticeHeader(
    title: String,
    progressLabel: String,
    progress: Float,
    onNavigateBack: () -> Unit
) {
    TabSectionSurface(
        modifier = Modifier.fillMaxWidth()
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
                    modifier = Modifier.clickable(onClick = onNavigateBack)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    shape = CircleShape
                ) {
                    Text(
                        text = progressLabel,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun PromptBubble(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
        shape = MaterialTheme.shapes.medium
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 72.dp)
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun WritingChoiceOption(
    label: String,
    index: Int,
    isSelected: Boolean,
    isCorrect: Boolean,
    submitted: Boolean,
    enabled: Boolean,
    onSelect: () -> Unit
) {
    val containerColor = when {
        submitted && isCorrect -> MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
        submitted && isSelected && !isCorrect -> MaterialTheme.colorScheme.error.copy(alpha = 0.14f)
        isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
        else -> MaterialTheme.colorScheme.surface
    }
    val borderColor = when {
        submitted && isCorrect -> MaterialTheme.colorScheme.primary
        submitted && isSelected && !isCorrect -> MaterialTheme.colorScheme.error
        isSelected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    }
    val letter = ('A' + index).toString()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onSelect),
        shape = MaterialTheme.shapes.medium,
        color = containerColor,
        tonalElevation = if (isSelected) 2.dp else 0.dp,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                shape = CircleShape
            ) {
                Box(
                    modifier = Modifier.size(30.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = letter,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Text(
                modifier = Modifier.weight(1f),
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (submitted) {
                when {
                    isCorrect -> Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "正确",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    isSelected -> Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = "错误",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun FeedbackPanel(
    isPositive: Boolean,
    title: String,
    message: String
) {
    val bgColor = if (isPositive) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
    } else {
        MaterialTheme.colorScheme.error.copy(alpha = 0.10f)
    }
    val accentColor = if (isPositive) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.error
    }
    Surface(
        color = bgColor,
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun PracticeActionBar(
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit
) {
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
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

@Composable
private fun PracticeSummaryScreen(
    title: String,
    headline: String,
    subline: String,
    onNavigateBack: () -> Unit,
    onRestart: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
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
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(56.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = headline,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = subline,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("返回写作训练")
                    }
                    Button(
                        onClick = onRestart,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("再练一轮")
                    }
                }
            }
        }
    }
}
