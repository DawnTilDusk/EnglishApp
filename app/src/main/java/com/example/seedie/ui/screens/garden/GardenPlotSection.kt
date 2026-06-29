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
                            text = "已点亮 $occupiedPlots/${plots.size}",
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
        plot.plantType == "empty" -> "播下种子 -20"
        plot.level < 2 -> "继续培育 -10"
        else -> "已盛放"
    }
    val actionHint = when {
        plot.plantType == "empty" -> "这块地还安静地空着，等你放下一颗种子，让今天的成长从这里发芽。"
        plot.level < 2 -> "它已经在慢慢长大，再照料一次，也许很快就会迎来新的颜色。"
        else -> "它已经开得很好了，今天不必催促，就让这份盛放多停一会儿。"
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
                        text = "已点亮 $occupiedPlots/$totalPlots",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = PrimaryGreen
                    )
                }
            }

            Text(
                text = "这里是你的学习花园。每一块地都会随着点滴积累慢慢发芽、生长、盛放，轻轻点开看看，就能知道它们正在经历怎样的变化。",
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
                    label = "抽芽中",
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
                    label = "盛放时",
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
                title = "照料方式",
                body = "轻点一块地，先看看它此刻的模样；如果你想播种，或想继续陪它长大，再按下按钮，让成长慢慢发生。"
            )

            GardenInfoCard(
                title = "成长代价",
                body = "播下一颗新种子需要 20 代币，继续培育正在生长的地块需要 10 代币；已经盛放的花，可以先静静欣赏。"
            )

            GardenInfoCard(
                title = "地块低语",
                body = "每块地都会用一小句提示告诉你它正走到哪一步。若想知道更多，只要轻点它，花园会把答案慢慢说给你听。"
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
            label = if (level >= 2) "枝叶渐盛" else "抽芽中",
            description = if (level >= 2) {
                "这一块绿意已经长得很有精神，像你最近一点点攒下来的稳定进步。"
            } else {
                "它才刚冒出嫩芽，再多照看一次，就会比现在更有生气。"
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
                level <= 0 -> "静待发芽"
                level == 1 -> "含苞待放"
                else -> "花开正盛"
            },
            description = when {
                level <= 0 -> "种子已经安稳睡下，再陪它一会儿，很快就会看见新的动静。"
                level == 1 -> "它正在悄悄攒着力气，再多一点照料，就会把颜色慢慢打开。"
                else -> "这朵花已经开得很好，像你这段时间静静累积下来的光亮。"
            },
            container = AccentOrange,
            accent = if (level >= 2) AccentOrange else PrimaryGreen
        )
        else -> PlotPresentation(
            hudLabel = "待播种",
            label = "静待播种",
            description = "这块地还空着，等你种下今天的一点成长，让它从安静里慢慢发芽。",
            container = SecondaryBrown,
            accent = SecondaryBrown
        )
    }
}
