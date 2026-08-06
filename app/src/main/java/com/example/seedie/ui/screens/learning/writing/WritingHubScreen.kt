package com.example.seedie.ui.screens.learning.writing

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.seedie.ui.components.TabSectionSurface

enum class WritingFeatureDestination {
    PracticalWriting,
    SentenceTranslation,
    ConnectorDrill,
    Paraphrase
}

private data class WritingFeatureEntry(
    val destination: WritingFeatureDestination,
    val title: String,
    val subtitle: String,
    val icon: ImageVector
)

private val writingFeatureEntries = listOf(
    WritingFeatureEntry(
        destination = WritingFeatureDestination.PracticalWriting,
        title = "写作实战",
        subtitle = "进入当前作文作业与提交页面",
        icon = Icons.Default.Create
    ),
    WritingFeatureEntry(
        destination = WritingFeatureDestination.SentenceTranslation,
        title = "句子翻译",
        subtitle = "中英互译，练表达落地",
        icon = Icons.Default.Translate
    ),
    WritingFeatureEntry(
        destination = WritingFeatureDestination.ConnectorDrill,
        title = "衔接词训练",
        subtitle = "选择恰当连接词，让句子更顺",
        icon = Icons.Default.AutoAwesome
    ),
    WritingFeatureEntry(
        destination = WritingFeatureDestination.Paraphrase,
        title = "同义改写",
        subtitle = "换一种更自然的写法表达同样意思",
        icon = Icons.Default.SwapHoriz
    )
)

@Composable
fun WritingHubScreen(
    onNavigateBack: () -> Unit,
    onOpenFeature: (WritingFeatureDestination) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        WritingHubHeader(onNavigateBack = onNavigateBack)

        writingFeatureEntries.forEach { entry ->
            WritingFeatureCard(
                entry = entry,
                onClick = { onOpenFeature(entry.destination) }
            )
        }
    }
}

@Composable
private fun WritingHubHeader(onNavigateBack: () -> Unit) {
    TabSectionSurface(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                    text = "写作训练",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "从实战提交到句子打磨，先把最常用的写作能力练起来。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
private fun WritingFeatureCard(
    entry: WritingFeatureEntry,
    onClick: () -> Unit
) {
    TabSectionSurface(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = entry.icon,
                    contentDescription = entry.title,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.size(16.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = entry.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = entry.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(6.dp)
                        .size(20.dp)
                )
            }
        }
    }
}
