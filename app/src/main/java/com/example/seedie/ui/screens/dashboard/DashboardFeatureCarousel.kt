package com.example.seedie.ui.screens.dashboard

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import kotlin.math.absoluteValue

private const val DashboardFeatureCardCount = 2
private const val DashboardFeatureVirtualPageCount = 10_000
private const val DashboardFeatureInitialPage =
    DashboardFeatureVirtualPageCount / 2 - (DashboardFeatureVirtualPageCount / 2) % DashboardFeatureCardCount

@Composable
fun DashboardFeatureCarousel(modifier: Modifier = Modifier) {
    val pagerState = rememberPagerState(
        initialPage = DashboardFeatureInitialPage,
        pageCount = { DashboardFeatureVirtualPageCount }
    )
    val dragTranslationDistance = with(LocalDensity.current) { 72.dp.toPx() }
    val stackLiftDistance = with(LocalDensity.current) { 14.dp.toPx() }

    HorizontalPager(
        state = pagerState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 4.dp, end = 44.dp),
        pageSpacing = 4.dp,
        beyondViewportPageCount = 1
    ) { page ->
        val actualPage = page.mod(DashboardFeatureCardCount)
        val rawOffset = (page - pagerState.currentPage) + pagerState.currentPageOffsetFraction
        val pageOffset = rawOffset.absoluteValue.coerceIn(0f, 1f)
        val targetScale = lerp(start = 0.86f, stop = 1f, fraction = 1f - pageOffset)
        val targetAlpha = lerp(start = 0.52f, stop = 1f, fraction = 1f - pageOffset)
        val targetTranslationX = -rawOffset * dragTranslationDistance
        val targetTranslationY = pageOffset * stackLiftDistance
        val scale by animateFloatAsState(
            targetValue = targetScale,
            animationSpec = spring(
                dampingRatio = 0.82f,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "dashboardFeatureScale"
        )
        val alpha by animateFloatAsState(
            targetValue = targetAlpha,
            animationSpec = spring(
                dampingRatio = 0.90f,
                stiffness = Spring.StiffnessMedium
            ),
            label = "dashboardFeatureAlpha"
        )
        val animatedTranslationX by animateFloatAsState(
            targetValue = targetTranslationX,
            animationSpec = spring(
                dampingRatio = 0.78f,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "dashboardFeatureTranslationX"
        )
        val animatedTranslationY by animateFloatAsState(
            targetValue = targetTranslationY,
            animationSpec = spring(
                dampingRatio = 0.86f,
                stiffness = Spring.StiffnessMedium
            ),
            label = "dashboardFeatureTranslationY"
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(2f - pageOffset)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    this.alpha = alpha
                    translationX = animatedTranslationX
                    translationY = animatedTranslationY
                }
        ) {
            when (actualPage) {
                0 -> HeatmapSection(modifier = Modifier.fillMaxSize())
                else -> DailyQuoteSection(modifier = Modifier.fillMaxSize())
            }
        }
    }
}
