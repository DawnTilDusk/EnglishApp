package com.example.seedie.ui.theme

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 自定义扩展 Modifier: 产生 8% 透明度的绿色软阴影
 * 贴合 Garden 主题的视觉规范
 */
fun Modifier.gardenShadow(
    elevation: Dp = 8.dp,
    shape: androidx.compose.ui.graphics.Shape = GardenShapes.large,
    clip: Boolean = false
): Modifier = this.then(
    Modifier.shadow(
        elevation = elevation,
        shape = shape,
        clip = clip,
        ambientColor = SoftGreenShadow,
        spotColor = SoftGreenShadow
    )
)