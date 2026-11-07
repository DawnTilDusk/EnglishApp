package com.example.seedie.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = PrimaryGreen,
    onPrimary = SurfaceCream,
    primaryContainer = Color(0xFFE3EFE4),
    onPrimaryContainer = Color(0xFF254D3A),

    secondary = SecondaryBrown,
    onSecondary = SurfaceCream,
    secondaryContainer = Color(0xFFEDF1E9),
    onSecondaryContainer = SecondaryBrown,

    tertiary = AccentOrange,
    onTertiary = SurfaceCream,
    tertiaryContainer = Color(0xFFF6EBD7),
    onTertiaryContainer = Color(0xFF78501F),

    background = BackgroundMint,
    onBackground = Color(0xFF22372B),

    surface = SurfaceCream,
    onSurface = Color(0xFF22372B),
    surfaceVariant = Color(0xFFEDF2EA),
    onSurfaceVariant = Color(0xFF647267),

    outline = Color(0xFF95A695),
    outlineVariant = Color(0xFFDDE5D9),
    surfaceContainerLowest = Color(0xFFFFFEFB),
    surfaceContainerLow = Color(0xFFF8FAF5),
    surfaceContainer = Color(0xFFF0F4EB),
    surfaceContainerHigh = Color(0xFFE8EEE2),
    surfaceContainerHighest = Color(0xFFE1E8DC),
    error = Color(0xFFAF433E),
    errorContainer = Color(0xFFFCEBE7),
    onErrorContainer = Color(0xFF782C28)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFA1D3AD),
    onPrimary = Green900,
    primaryContainer = Green700,
    onPrimaryContainer = Green100,

    secondary = SecondaryBrown,
    onSecondary = SurfaceCream,
    secondaryContainer = DeepCocoa,
    onSecondaryContainer = Green100,

    tertiary = AccentOrange,
    onTertiary = DeepCocoa,
    tertiaryContainer = ForestDeep,
    onTertiaryContainer = KhakiGold,

    background = Green900,
    onBackground = Green50,

    surface = Green800,
    onSurface = Green50,
    surfaceVariant = ForestDeep,
    onSurfaceVariant = Green200,

    outline = Green600,
    outlineVariant = Green700
)

@Composable
fun SeedieTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
