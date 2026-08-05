package com.example.seedie.ui.screens.learning.writing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

enum class WritingFeatureDestination {
    PracticalWriting,
    SentenceTranslation,
    ConnectorDrill,
    Paraphrase
}

data class WritingFeatureEntry(
    val destination: WritingFeatureDestination,
    val title: String,
    val subtitle: String
)

private val writingFeatureEntries = listOf(
    WritingFeatureEntry(
        destination = WritingFeatureDestination.PracticalWriting,
        title = "写作实战",
        subtitle = "进入当前作文作业与提交页面"
    ),
    WritingFeatureEntry(
        destination = WritingFeatureDestination.SentenceTranslation,
        title = "句子翻译",
        subtitle = "中英互译，练表达落地"
    ),
    WritingFeatureEntry(
        destination = WritingFeatureDestination.ConnectorDrill,
        title = "衔接词训练",
        subtitle = "选择恰当连接词，让句子更顺"
    ),
    WritingFeatureEntry(
        destination = WritingFeatureDestination.Paraphrase,
        title = "同义改写",
        subtitle = "换一种更自然的写法表达同样意思"
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WritingHubScreen(
    onNavigateBack: () -> Unit,
    onOpenFeature: (WritingFeatureDestination) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("写作训练") },
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
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "先选一个训练方向",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "从实战提交到句子打磨，先把最常用的写作能力练起来。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            writingFeatureEntries.forEach { item ->
                Button(
                    onClick = { onOpenFeature(item.destination) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = item.subtitle,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}
