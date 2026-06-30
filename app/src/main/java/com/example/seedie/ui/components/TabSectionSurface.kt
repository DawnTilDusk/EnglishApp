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
import com.example.seedie.ui.theme.gardenShadow

@Composable
fun TabSectionSurface(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    content: @Composable BoxScope.() -> Unit
) {
    Surface(
        modifier = modifier.gardenShadow(shape = shape),
        shape = shape,
        color = MaterialTheme.colorScheme.surface
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f),
                            accentColor.copy(alpha = 0.08f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    color = accentColor.copy(alpha = 0.10f),
                    shape = shape
                )
        ) {
            content()
        }
    }
}
