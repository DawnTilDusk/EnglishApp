package com.example.seedie.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.example.seedie.ui.theme.gardenPressable
import com.example.seedie.ui.theme.gardenShadow

@Composable
fun TabSectionSurface(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    Surface(
        modifier = modifier.gardenShadow(shape = shape),
        shape = shape,
        // 卡片底色：奶油白 surface，保持整站卡片同一层清爽底
        color = MaterialTheme.colorScheme.surface
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (onClick != null) {
                        Modifier.gardenPressable(shape = shape, onClick = onClick)
                    } else {
                        Modifier
                    }
                )
                // 顶部叠加 Green50 晨露绿薄雾（surfaceVariant），中段淡入 accent 色，
                // 底部回到纯 surface；三层过渡制造花园光影感，同时不喧宾夺主
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            accentColor.copy(alpha = 0.10f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                // 描边：主题 outlineVariant（Green100 新芽白绿）与 accent 混合，
                // 形成一条极淡的绿色线，替代原来几乎不可见的 10% 透明描边
                .border(
                    width = 1.dp,
                    color = accentColor.copy(alpha = 0.16f),
                    shape = shape
                )
        ) {
            content()
        }
    }
}
