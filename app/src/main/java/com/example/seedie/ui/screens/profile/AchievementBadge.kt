package com.example.seedie.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class BadgeConfig(
    val id: String,
    val name: String,
    val description: String,
    val isUnlocked: Boolean
)

@Composable
fun AchievementBadge(
    badge: BadgeConfig,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(
                    if (badge.isUnlocked) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.EmojiEvents,
                contentDescription = badge.name,
                modifier = Modifier.size(40.dp),
                tint = if (badge.isUnlocked) MaterialTheme.colorScheme.tertiary
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = badge.name,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = if (badge.isUnlocked) MaterialTheme.colorScheme.onSurface 
                   else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
        
        Text(
            text = badge.description,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary.copy(alpha = if (badge.isUnlocked) 1f else 0.5f)
        )
    }
}