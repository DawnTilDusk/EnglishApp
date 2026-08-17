package com.example.seedie.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import com.example.seedie.ui.components.TabSectionSurface

@Composable
fun AssetGallerySection(
    modifier: Modifier = Modifier,
    totalTokens: Int = 120,
    totalDews: Int = 0,
    badges: List<BadgeConfig> = emptyList(),
    onOpenShop: () -> Unit = {}
) {
    TabSectionSurface(
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Top: Currency balances
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AssetBalanceCard(
                    modifier = Modifier.weight(1f),
                    label = "我的代币",
                    amount = totalTokens,
                    icon = Icons.Default.Star,
                    iconDescription = "代币",
                    accent = MaterialTheme.colorScheme.tertiary,
                    container = MaterialTheme.colorScheme.tertiaryContainer
                )
                AssetBalanceCard(
                    modifier = Modifier.weight(1f),
                    label = "我的露水",
                    amount = totalDews,
                    icon = Icons.Default.Eco,
                    iconDescription = "露水",
                    accent = MaterialTheme.colorScheme.primary,
                    container = MaterialTheme.colorScheme.primaryContainer
                )
            }

            Button(
                onClick = onOpenShop,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Text(text = "教师商城")
            }

            // Bottom: Achievement Gallery
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "成就勋章",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(badges) { badge ->
                        AchievementBadge(badge = badge)
                    }
                }
            }
        }
    }
}

@Composable
private fun AssetBalanceCard(
    modifier: Modifier,
    label: String,
    amount: Int,
    icon: ImageVector,
    iconDescription: String,
    accent: Color,
    container: Color
) {
    Row(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(container)
            .padding(20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = amount.toString(),
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = accent
            )
        }
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = iconDescription,
                modifier = Modifier.size(30.dp),
                tint = accent
            )
        }
    }
}
