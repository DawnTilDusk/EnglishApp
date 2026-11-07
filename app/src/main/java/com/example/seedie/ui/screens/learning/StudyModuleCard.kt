package com.example.seedie.ui.screens.learning

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun StudyModuleCard(module: ModuleConfig, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val tone = when (module.id) {
        "listening" -> Color(0xFF466E8B)
        "reading" -> Color(0xFF917242)
        "writing" -> Color(0xFF74628B)
        else -> MaterialTheme.colorScheme.primary
    }
    Surface(onClick = onClick, modifier = modifier.fillMaxWidth().heightIn(min = 142.dp),
        shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Row(Modifier.padding(22.dp), horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(52.dp).clip(MaterialTheme.shapes.medium).background(tone.copy(alpha = .09f)),
                contentAlignment = Alignment.Center) {
                Icon(module.icon, contentDescription = null, tint = tone, modifier = Modifier.size(26.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(module.title, style = MaterialTheme.typography.titleLarge)
                Text(module.subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (module.hasNewContent) Text("待你继续", style = MaterialTheme.typography.labelSmall, color = tone)
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .6f), modifier = Modifier.size(19.dp))
        }
    }
}
