package com.programmersbox.kmpuiviews.presentation.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.TargetedFlingBehavior
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.layout.MutableIntervalList
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

interface OnboardingScope {
    fun item(content: @Composable () -> Unit)

    val size: Int

    operator fun get(index: Int): @Composable () -> Unit
}

internal class OnboardingScopeImpl(
    content: OnboardingScope.() -> Unit = {},
) : OnboardingScope {
    val intervals: MutableIntervalList<@Composable () -> Unit> = MutableIntervalList()

    override val size: Int get() = intervals.size

    override fun get(index: Int): @Composable (() -> Unit) = intervals[index].value

    init {
        apply(content)
    }

    override fun item(content: @Composable () -> Unit) {
        intervals.addInterval(1, content)
    }
}

@Composable
fun rememberOnboardingScope(
    content: OnboardingScope.() -> Unit = {},
): OnboardingScope = remember { OnboardingScopeImpl(content) }

@Composable
fun Onboarding(
    onboardingScope: OnboardingScope,
    state: PagerState = rememberPagerState { onboardingScope.size },
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    pageSize: PageSize = PageSize.Fill,
    beyondViewportPageCount: Int = PagerDefaults.BeyondViewportPageCount,
    pageSpacing: Dp = 0.dp,
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    flingBehavior: TargetedFlingBehavior = PagerDefaults.flingBehavior(state = state),
    userScrollEnabled: Boolean = true,
    reverseLayout: Boolean = false,
    key: ((index: Int) -> Any)? = null,
    pageNestedScrollConnection: NestedScrollConnection = PagerDefaults.pageNestedScrollConnection(state, Orientation.Horizontal),
    snapPosition: SnapPosition = SnapPosition.Start,
    overscrollEffect: OverscrollEffect? = rememberOverscrollEffect(),
) {
    val stateHolder = rememberSaveableStateHolder()
    HorizontalPager(
        state = state,
        modifier = modifier,
        contentPadding = contentPadding,
        pageSize = pageSize,
        beyondViewportPageCount = beyondViewportPageCount,
        pageNestedScrollConnection = pageNestedScrollConnection,
        flingBehavior = flingBehavior,
        userScrollEnabled = userScrollEnabled,
        reverseLayout = reverseLayout,
        key = key,
        snapPosition = snapPosition,
        overscrollEffect = overscrollEffect,
        pageSpacing = pageSpacing,
        verticalAlignment = verticalAlignment,
        pageContent = { page -> stateHolder.SaveableStateProvider(page) { onboardingScope[page]() } }
    )
}

@Composable
fun OnboardingIndicator(
    pagerState: PagerState,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = modifier
            .wrapContentHeight()
            .fillMaxWidth()
    ) {
        repeat(pagerState.pageCount) { iteration ->
            val color by animateColorAsState(
                when {
                    pagerState.currentPage == iteration -> MaterialTheme.colorScheme.primary
                    pagerState.currentPage > iteration -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.secondary
                }
            )

            Box(
                modifier = Modifier
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(color)
                    .size(16.dp)
            )
        }
    }
}