package com.example.seedie.ui.screens.learning.listening.immersion

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private const val IMMERSION_TITLE = "The Quiet Power of Morning Light"

private const val IMMERSION_SOURCE = "Placeholder Weekly · Science"

private val IMMERSION_PARAGRAPHS = listOf(
    "Long before the city wakes, the first light of the day slips over the rooftops and settles on the streets below. Researchers who study human sleep say this early light is far more than scenery: it is a signal.",
    "Cells in the back of the eye detect the blue tones in dawn light and pass a message to a small cluster of neurons that keeps the body on schedule. Twenty minutes outdoors in the morning can shift that clock earlier, making it easier to fall asleep at night.",
    "The effect is surprisingly strong. In one study, office workers who took a short morning walk reported falling asleep almost half an hour earlier within two weeks. None of them changed anything else about their routine.",
    "Evening light works in the opposite direction. Bright screens after sunset tell the same neurons that the day is still going, which is why many sleep specialists suggest dimming lamps and putting phones away an hour before bed."
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListeningImmersionRoute(
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("磨耳朵") },
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
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = IMMERSION_TITLE,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = IMMERSION_SOURCE,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                tonalElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "听读音频",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "音频还在准备，先读一读文章吧",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    FilledTonalButton(onClick = {}, enabled = false) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Text("播放")
                    }
                }
            }
            IMMERSION_PARAGRAPHS.forEach { paragraph ->
                Text(
                    text = paragraph,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}
