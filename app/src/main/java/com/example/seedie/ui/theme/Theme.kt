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

// Light 主题：主色调完全保留 Seedie IP 的生机绿套装，补充色只负责容器/描边/层次
private val LightColorScheme = lightColorScheme(
    // 主色：生机绿 PrimaryGreen，Seedie 小芽的品牌主情绪色，不做替换
    primary = PrimaryGreen,
    onPrimary = SurfaceCream,
    // 主容器：Green100 新芽白绿，用作选中/高亮的浅底，属于补充层次
    primaryContainer = Green100,
    onPrimaryContainer = Green800,

    // 次色：橡木棕 SecondaryBrown，保留原有中性木质感
    secondary = SecondaryBrown,
    onSecondary = SurfaceCream,
    // 次容器：Green50 晨露绿，用作弱层级信息底，仍维持整体绿意
    secondaryContainer = Green50,
    onSecondaryContainer = SecondaryBrown,

    // 三级色：暖阳橙 AccentOrange，奖励/代币/强调点，保留品牌 IP
    tertiary = AccentOrange,
    onTertiary = SurfaceCream,
    // 三级容器：KhakiGold 卡其金淡化版，用于成就点缀的底色
    tertiaryContainer = KhakiGold.copy(alpha = 0.24f),
    onTertiaryContainer = SecondaryBrown,

    // 背景：淡薄荷绿保留（IP 底色）
    background = BackgroundMint,
    onBackground = Green900,

    // 表面：奶油白保留卡片清爽感
    surface = SurfaceCream,
    onSurface = Green900,
    // 表面变体：Green50 晨露绿作为分隔/次级容器底，比灰色更贴合花园气质
    surfaceVariant = Green50,
    onSurfaceVariant = Green700,

    // 描边：Green300 春叶绿常规描边、Green100 用于极淡分隔线
    outline = Green300,
    outlineVariant = Green100
)

// Dark 主题：保持生机绿的品牌情绪，反转色阶用于深色环境
private val DarkColorScheme = darkColorScheme(
    // 深色态下依然以生机绿为主色，让品牌情绪跨主题延续
    primary = PrimaryGreen,
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

    // 深色背景：Green900 墨林绿，避免纯黑显得冰冷
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
    // Dynamic color is available on Android 12+
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
