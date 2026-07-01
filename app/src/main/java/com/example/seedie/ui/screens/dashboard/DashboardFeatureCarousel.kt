package com.example.seedie.ui.screens.dashboard

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
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
    val translationDistance = with(LocalDensity.current) { 28.dp.toPx() }

    HorizontalPager(
        state = pagerState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp),
        pageSpacing = 12.dp
    ) { page ->
        val actualPage = page.mod(DashboardFeatureCardCount)
        val rawOffset = (page - pagerState.currentPage) + pagerState.currentPageOffsetFraction
        val pageOffset = rawOffset.absoluteValue.coerceIn(0f, 1f)
        val scale = lerpFloat(start = 0.9f, stop = 1f, fraction = 1f - pageOffset)
        val alpha = lerpFloat(start = 0.72f, stop = 1f, fraction = 1f - pageOffset)

        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(1f - pageOffset)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    this.alpha = alpha
                    translationX = -rawOffset * translationDistance
                }
        ) {
            when (actualPage) {
                0 -> HeatmapSection(modifier = Modifier.fillMaxSize())
                else -> DailyQuoteSection(modifier = Modifier.fillMaxSize())
            }
        }
    }
}

private fun lerpFloat(start: Float, stop: Float, fraction: Float): Float {
    return start + (stop - start) * fraction
}
