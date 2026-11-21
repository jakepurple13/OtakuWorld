package com.programmersbox.kmpuiviews.presentation.onboarding

import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.TargetedFlingBehavior
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.layout.MutableIntervalList
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.programmersbox.kmpuiviews.presentation.onboarding.motion.onboardingReducedMotion
import com.programmersbox.kmpuiviews.utils.composables.modifiers.pagerStagger

interface OnboardingScope {
    fun step(spec: OnboardingStepSpec, content: @Composable () -> Unit)

    val size: Int

    operator fun get(index: Int): OnboardingStepEntry
}

private class OnboardingScopeImpl(
    content: OnboardingScope.() -> Unit = {},
) : OnboardingScope {
    val intervals: MutableIntervalList<OnboardingStepEntry> = MutableIntervalList()

    override val size: Int get() = intervals.size

    override fun get(index: Int): OnboardingStepEntry = intervals[index].value

    init {
        apply(content)
    }

    override fun step(spec: OnboardingStepSpec, content: @Composable () -> Unit) {
        intervals.addInterval(1, OnboardingStepEntry(spec, content))
    }
}

internal fun buildOnboardingScope(content: OnboardingScope.() -> Unit = {}): OnboardingScope =
    OnboardingScopeImpl(content)

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
    val reduced = onboardingReducedMotion()
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
        pageContent = { page ->
            Box(if (reduced) Modifier else Modifier.pagerStagger(state, page, index = 0)) {
                stateHolder.SaveableStateProvider(page) { onboardingScope[page].content() }
            }
        }
    )
}
