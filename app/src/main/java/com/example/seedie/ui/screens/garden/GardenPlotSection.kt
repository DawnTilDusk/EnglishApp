package com.example.seedie.ui.screens.garden

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.zIndex
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
    val cardShape = RoundedCornerShape(28.dp)
    var detailsExpanded by rememberSaveable { mutableStateOf(false) }
    var selectedPlotIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    val selectedPlot = plots.firstOrNull { it.plotIndex == selectedPlotIndex }
    val occupiedPlots = plots.count { it.plantType != "empty" }
    val gridTopInset by animateDpAsState(
        targetValue = if (selectedPlot != null) 92.dp else 0.dp,
        label = "gardenGridTopInset"
    )

    Surface(
        modifier = modifier
            .fillMaxSize()
            .gardenShadow(shape = cardShape),
        shape = cardShape,
        color = MaterialTheme.colorScheme.surface
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.30f),
                            PrimaryGreen.copy(alpha = 0.06f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    color = PrimaryGreen.copy(alpha = 0.10f),
                    shape = cardShape
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = "我的花园",
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { detailsExpanded = !detailsExpanded }
                            .padding(vertical = 4.dp),
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )

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

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = selectedPlot != null,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .zIndex(1f),
                        enter = fadeIn(animationSpec = tween(220)) +
                            slideInVertically(
                                animationSpec = tween(240),
                                initialOffsetY = { -it / 2 }
                            ),
                        exit = fadeOut(animationSpec = tween(180)) +
                            slideOutVertically(
                                animationSpec = tween(220),
                                targetOffsetY = { -it / 3 }
                            )
                    ) {
                        selectedPlot?.let { plot ->
                            GardenPlotActionBar(
                                plot = plot,
                                modifier = Modifier.padding(horizontal = 8.dp),
                                onActionClick = { onPlotClick(plot) }
                            )
                        }
                    }

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxSize(0.92f)
                            .padding(top = gridTopInset)
                    ) {
                        items(
                            items = plots,
                            key = { it.plotIndex }
                        ) { plot ->
                            PlantSlot(
                                plot = plot,
                                isSelected = plot.plotIndex == selectedPlotIndex,
                                onClick = { selectedPlotIndex = plot.plotIndex }
                            )
                        }
                    }
                }
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = detailsExpanded,
                modifier = Modifier.fillMaxSize(),
                enter = fadeIn(animationSpec = tween(220)) +
                    slideInVertically(
                        animationSpec = tween(260),
                        initialOffsetY = { -it / 2 }
                    ),
                exit = fadeOut(animationSpec = tween(180)) +
                    slideOutVertically(
                        animationSpec = tween(220),
                        targetOffsetY = { -it / 3 }
                    )
            ) {
                GardenPlotDetailsOverlay(
                    cardShape = cardShape,
                    occupiedPlots = occupiedPlots,
                    totalPlots = plots.size,
                    onDismiss = { detailsExpanded = false }
                )
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

            PlotHudChip(
                text = presentation.hudLabel,
                color = presentation.accent
            )
        }
    }
}

@Composable
private fun GardenPlotActionBar(
    plot: GardenPlotEntity,
    modifier: Modifier = Modifier,
    onActionClick: () -> Unit
) {
    val presentation = remember(plot.plantType, plot.level) { plot.toPresentation() }
    val actionLabel = when {
        plot.plantType == "empty" -> "播种 -20"
        plot.level < 2 -> "升级 -10"
        else -> "已满级"
    }
    val actionHint = when {
        plot.plantType == "empty" -> "将沿用原有播种逻辑，为这块空地种下新的成长记录。"
        plot.level < 2 -> "点击按钮后才会执行原有浇灌升级，不再因点中地块误触。"
        else -> "这块地已经达到当前最高阶段，先保留状态展示。"
    }
    val actionEnabled = plot.plantType == "empty" || plot.level < 2

    Surface(
        modifier = modifier.gardenShadow(shape = RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
        tonalElevation = 3.dp,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            presentation.container.copy(alpha = 0.20f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = presentation.accent.copy(alpha = 0.14f)
            ) {
                Text(
                    text = "${plot.plotIndex + 1}号",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = presentation.accent
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = presentation.label,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = actionHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Button(
                onClick = onActionClick,
                enabled = actionEnabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = presentation.accent,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(text = actionLabel)
            }
        }
    }
}

@Composable
private fun GardenPlotDetailsOverlay(
    cardShape: RoundedCornerShape,
    occupiedPlots: Int,
    totalPlots: Int,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .clip(cardShape),
        shape = cardShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
        tonalElevation = 3.dp,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = "我的花园",
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onDismiss() }
                        .padding(vertical = 4.dp),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )

                Surface(
                    shape = CircleShape,
                    color = PrimaryGreen.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "已激活 $occupiedPlots/$totalPlots",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = PrimaryGreen
                    )
                }
            }

            Text(
                text = "这里集中收纳花园玩法说明、状态提示和操作代价。默认收起时只保留植物栏主体，避免说明区长期占用视线。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
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

            GardenInfoCard(
                title = "操作引导",
                body = "点击地块只负责选中并唤起悬浮操作条，真正的播种或升级要在按钮里确认，避免直接点中地块就触发操作。"
            )

            GardenInfoCard(
                title = "代价说明",
                body = "空地播种会沿用原有 20 代币消耗，已种植地块继续升级会沿用原有 10 代币消耗；满级地块仅保留状态展示。"
            )

            GardenInfoCard(
                title = "HUD 说明",
                body = "每个地块内部仍保留小型状态 HUD，只负责提示当前阶段；更完整的说明和操作集中在点击后的悬浮条中。"
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
private fun PlotHudChip(
    text: String,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = color.copy(alpha = 0.14f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun GardenInfoCard(
    title: String,
    body: String
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private data class PlotPresentation(
    val hudLabel: String,
    val label: String,
    val description: String,
    val container: Color,
    val accent: Color
)

private fun GardenPlotEntity.toPresentation(): PlotPresentation {
    return when (plantType) {
        "grass" -> PlotPresentation(
            hudLabel = if (level >= 2) "茂盛" else "幼苗",
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
            hudLabel = when {
                level <= 0 -> "待发芽"
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
            hudLabel = "待播种",
            label = "空地待播种",
            description = "这里还是一块空地，点击后可在悬浮操作条中触发原有播种逻辑。",
            container = SecondaryBrown,
            accent = SecondaryBrown
        )
    }
}
