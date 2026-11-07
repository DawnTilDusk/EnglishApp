package com.example.seedie.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private fun gardenType(size: Int, line: Int, weight: FontWeight = FontWeight.Normal, spacing: Float = 0f) = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontSize = size.sp,
    lineHeight = line.sp,
    fontWeight = weight,
    letterSpacing = spacing.sp
)

val Typography = Typography(
    displayLarge = gardenType(48, 58, FontWeight.SemiBold, -1f),
    displayMedium = gardenType(40, 50, FontWeight.SemiBold, -0.7f),
    displaySmall = gardenType(34, 44, FontWeight.SemiBold, -0.5f),
    headlineLarge = gardenType(30, 40, FontWeight.SemiBold, -0.4f),
    headlineMedium = gardenType(26, 36, FontWeight.SemiBold, -0.3f),
    headlineSmall = gardenType(23, 32, FontWeight.SemiBold, -0.2f),
    titleLarge = gardenType(21, 30, FontWeight.SemiBold),
    titleMedium = gardenType(17, 26, FontWeight.SemiBold),
    titleSmall = gardenType(15, 23, FontWeight.SemiBold),
    bodyLarge = gardenType(17, 29),
    bodyMedium = gardenType(15, 24),
    bodySmall = gardenType(13, 21),
    labelLarge = gardenType(14, 20, FontWeight.SemiBold),
    labelMedium = gardenType(12, 18, FontWeight.Medium, 0.2f),
    labelSmall = gardenType(11, 16, FontWeight.Medium, 0.2f)
)
