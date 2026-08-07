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

// 花园主题软阴影：取品牌主色 PrimaryGreen 生机绿 8% 透明度，
// 让卡片投影仍然带着小芽的绿意，视觉上和主题主色保持同一情绪
private val GardenShadowTint = Color(0x1466BB6A) // 8% 透明度的 PrimaryGreen 生机绿

/**
 * 自定义软阴影扩展，符合 Garden 主题的 8% 透明度绿色阴影
 */
fun Modifier.gardenShadow(
    elevation: Dp = 8.dp,
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
