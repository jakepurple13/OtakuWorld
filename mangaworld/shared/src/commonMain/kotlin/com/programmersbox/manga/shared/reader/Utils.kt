package com.programmersbox.manga.shared.reader

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp

@ExperimentalAnimationApi
@Composable
internal fun PageIndicator(
    currentPage: Int,
    pageCount: Int,
    modifier: Modifier = Modifier,
) {
    Text(
        "$currentPage/$pageCount",
        style = MaterialTheme.typography.bodyLarge,
        modifier = modifier
    )
}

@Composable
internal fun dpToPx(dp: Int): Int = (dp * LocalDensity.current.density).toInt()

internal fun LazyListState.isScrolledToTheEnd() = layoutInfo.visibleItemsInfo.lastOrNull()?.index == layoutInfo.totalItemsCount - 1
internal fun LazyListState.isScrolledToTheBeginning() = layoutInfo.visibleItemsInfo.firstOrNull()?.index == 0

@Composable
internal fun PaddingValues.animate() = PaddingValues(
    start = calculateStartPadding(LocalLayoutDirection.current).animate().value,
    end = calculateEndPadding(LocalLayoutDirection.current).animate().value,
    top = calculateTopPadding().animate().value,
    bottom = calculateBottomPadding().animate().value,
)

@Composable
internal fun Dp.animate() = animateDpAsState(targetValue = this, label = "")