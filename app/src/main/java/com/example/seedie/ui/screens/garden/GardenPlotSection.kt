package com.example.seedie.ui.screens.garden

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.seedie.data.local.entity.GardenPlotEntity
import com.example.seedie.ui.theme.AccentOrange
import com.example.seedie.ui.theme.PrimaryGreen
import com.example.seedie.ui.theme.SecondaryBrown
import com.example.seedie.ui.theme.gardenShadow

@Composable
fun GardenPlotSection(
    modifier: Modifier = Modifier,
    plots: List<GardenPlotEntity> = List(16) { index -> GardenPlotEntity(userId = "", plotIndex = index) },
    onPlotClick: (GardenPlotEntity) -> Unit = {}
) {
    var selectedPlotIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    val selectedPlot = plots.firstOrNull { it.plotIndex == selectedPlotIndex }
    val occupiedPlots = plots.count { it.plantType != "empty" }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .gardenShadow(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "我的花园",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "点击地块即可延续原有播种/浇灌逻辑，并在右侧获得即时状态反馈。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = PrimaryGreen.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "已激活 $occupiedPlots/${plots.size}",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = PrimaryGreen
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GardenLegendChip(
                    label = "空地",
                    color = SecondaryBrown,
                    icon = {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(SecondaryBrown.copy(alpha = 0.45f))
                        )
                    }
                )
                GardenLegendChip(
                    label = "生长中",
                    color = PrimaryGreen,
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Grass,
                            contentDescription = null,
                            tint = PrimaryGreen,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
                GardenLegendChip(
                    label = "开花期",
                    color = AccentOrange,
                    icon = {
                        Icon(
                            imageVector = Icons.Default.LocalFlorist,
                            contentDescription = null,
                            tint = AccentOrange,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }

            SelectedPlotSummaryCard(
                modifier = Modifier.padding(top = 18.dp),
                plot = selectedPlot
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize(0.9f)
                ) {
                    items(
                        items = plots,
                        key = { it.plotIndex }
                    ) { plot ->
                        PlantSlot(
                            plot = plot,
                            isSelected = plot.plotIndex == selectedPlotIndex,
                            onClick = {
                                selectedPlotIndex = plot.plotIndex
                                onPlotClick(plot)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PlantSlot(
    plot: GardenPlotEntity,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val presentation = remember(plot.plantType, plot.level) { plot.toPresentation() }
    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.95f
            isSelected -> 1.03f
            else -> 1f
        },
        label = "plantSlotScale"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) {
            presentation.accent.copy(alpha = 0.78f)
        } else {
            presentation.accent.copy(alpha = 0.18f)
        },
        label = "plantSlotBorder"
    )
    val badgeColor by animateColorAsState(
        targetValue = if (isSelected) presentation.accent else MaterialTheme.colorScheme.surface,
        label = "plantSlotBadge"
    )
    val badgeTextColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "plantSlotBadgeText"
    )
    val shadowElevation by animateDpAsState(
        targetValue = if (isSelected) 12.dp else 5.dp,
        label = "plantSlotShadow"
    )

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .gardenShadow(
                elevation = shadowElevation,
                shape = RoundedCornerShape(20.dp)
            )
            .clip(RoundedCornerShape(20.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        presentation.container.copy(alpha = if (isSelected) 0.32f else 0.22f),
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(10.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = badgeColor,
            modifier = Modifier.align(Alignment.TopStart)
        ) {
            Text(
                text = "${plot.plotIndex + 1}",
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = badgeTextColor
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(10.dp)
                .clip(CircleShape)
                .background(presentation.accent.copy(alpha = if (isSelected) 0.95f else 0.6f))
        )

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.size(4.dp))

            when (plot.plantType) {
                "empty" -> {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(SecondaryBrown.copy(alpha = 0.32f))
                    )
                }
                "grass" -> {
                    Icon(
                        imageVector = Icons.Default.Grass,
                        contentDescription = "Grass",
                        tint = PrimaryGreen,
                        modifier = Modifier.fillMaxSize(0.52f + (plot.level * 0.08f))
                    )
                }
                "flower" -> {
                    val icon = when (plot.level) {
                        0 -> Icons.Default.Eco
                        1 -> Icons.Default.LocalFlorist
                        else -> Icons.Default.LocalFlorist
                    }

                    Icon(
                        imageVector = icon,
                        contentDescription = "Flower",
                        tint = presentation.accent,
                        modifier = Modifier.fillMaxSize(0.48f + (plot.level * 0.12f))
                    )
                }
            }

            Text(
                text = presentation.shortLabel,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun GardenLegendChip(
    label: String,
    color: Color,
    icon: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon()
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun SelectedPlotSummaryCard(
    modifier: Modifier = Modifier,
    plot: GardenPlotEntity?
) {
    val presentation = remember(plot?.plantType, plot?.level) { plot?.toPresentation() }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            (presentation?.container ?: SecondaryBrown).copy(alpha = 0.18f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = plot?.let { "当前选中 ${it.plotIndex + 1} 号地块" } ?: "选择一块地开始查看状态",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = plot?.let {
                    "${presentation?.label} · ${presentation?.description}"
                } ?: "点击任意地块即可查看成长阶段，同时保留原有播种或浇灌点击行为。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private data class PlotPresentation(
    val shortLabel: String,
    val label: String,
    val description: String,
    val container: Color,
    val accent: Color
)

private fun GardenPlotEntity.toPresentation(): PlotPresentation {
    return when (plantType) {
        "grass" -> PlotPresentation(
            shortLabel = if (level >= 2) "茂盛" else "幼苗",
            label = if (level >= 2) "草木繁盛" else "生长中",
            description = if (level >= 2) {
                "这块地已经积累了不错的成长值，继续互动会向更成熟状态推进。"
            } else {
                "幼苗正在扎根，适合继续浇灌，让学习进度稳定增长。"
            },
            container = PrimaryGreen,
            accent = PrimaryGreen
        )
        "flower" -> PlotPresentation(
            shortLabel = when {
                level <= 0 -> "种子"
                level == 1 -> "含苞"
                else -> "盛放"
            },
            label = when {
                level <= 0 -> "待发芽"
                level == 1 -> "含苞待放"
                else -> "开花期"
            },
            description = when {
                level <= 0 -> "花种已经埋下，下一次点击会继续推动它发芽成长。"
                level == 1 -> "花朵正在积累能量，已经接近完整绽放。"
                else -> "花朵已经进入高亮阶段，代表这块地的学习表现较成熟。"
            },
            container = AccentOrange,
            accent = if (level >= 2) AccentOrange else PrimaryGreen
        )
        else -> PlotPresentation(
            shortLabel = "空地",
            label = "待播种",
            description = "这里还是一块空地，点击即可触发原有播种逻辑，种下新的成长记录。",
            container = SecondaryBrown,
            accent = SecondaryBrown
        )
    }
}
