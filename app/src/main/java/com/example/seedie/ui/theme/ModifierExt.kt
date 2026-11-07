package com.example.seedie.ui.theme

import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// 低透明度的绿色软阴影，保持卡片层次轻盈。
private val GardenShadowTint = Color(0x102C4932)

/**
 * 自定义软阴影扩展，使用 Garden 主题的淡绿色阴影
 */
fun Modifier.gardenShadow(
    elevation: Dp = 3.dp,
    shape: Shape = RoundedCornerShape(24.dp),
    clip: Boolean = false
): Modifier = this.shadow(
    elevation = elevation,
    shape = shape,
    clip = clip,
    ambientColor = GardenShadowTint,
    spotColor = GardenShadowTint
)

fun Modifier.gardenPressable(
    shape: Shape,
    onClick: () -> Unit
): Modifier = this
    .clip(shape)
    .clickable(onClick = onClick)
