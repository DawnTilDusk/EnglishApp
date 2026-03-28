package com.example.seedie.ui.screens.garden

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.seedie.data.local.entity.GardenPlotEntity
import com.example.seedie.ui.theme.AccentOrange
import com.example.seedie.ui.theme.PrimaryGreen
import com.example.seedie.ui.theme.SecondaryBrown
import com.example.seedie.ui.theme.gardenShadow

@Composable
fun GardenPlotSection(
    modifier: Modifier = Modifier,
    plots: List<GardenPlotEntity> = List(16) { GardenPlotEntity(it) }, // Default empty 4x4
    onPlotClick: (GardenPlotEntity) -> Unit = {}
) {
    Surface(
        modifier = modifier
            .fillMaxSize()
            .gardenShadow(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "我的花园",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize(0.9f) // Keep it somewhat centered
                ) {
                    items(plots) { plot ->
                        PlantSlot(plot = plot, onClick = { onPlotClick(plot) })
                    }
                }
            }
        }
    }
}

@Composable
fun PlantSlot(
    plot: GardenPlotEntity,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(SecondaryBrown.copy(alpha = 0.1f)) // Soil color
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        when (plot.plantType) {
            "empty" -> {
                // Render nothing or a small dot for empty soil
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(SecondaryBrown.copy(alpha = 0.3f))
                )
            }
            "grass" -> {
                Icon(
                    imageVector = Icons.Default.Grass,
                    contentDescription = "Grass",
                    tint = PrimaryGreen,
                    modifier = Modifier.fillMaxSize(0.6f + (plot.level * 0.1f))
                )
            }
            "flower" -> {
                val icon = when (plot.level) {
                    0 -> Icons.Default.Eco // Seed
                    1 -> Icons.Default.LocalFlorist // Sprout/Small Flower
                    else -> Icons.Default.LocalFlorist // Blooming
                }
                val color = if (plot.level >= 2) AccentOrange else PrimaryGreen
                
                Icon(
                    imageVector = icon,
                    contentDescription = "Flower",
                    tint = color,
                    modifier = Modifier.fillMaxSize(0.5f + (plot.level * 0.15f))
                )
            }
        }
    }
}