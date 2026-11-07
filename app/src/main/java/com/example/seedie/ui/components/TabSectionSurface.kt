package com.example.seedie.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
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
                .border(
                    width = 1.dp,
                    color = lerp(MaterialTheme.colorScheme.outlineVariant, accentColor, 0.08f),
                    shape = shape
                )
        ) {
            content()
        }
    }
}
