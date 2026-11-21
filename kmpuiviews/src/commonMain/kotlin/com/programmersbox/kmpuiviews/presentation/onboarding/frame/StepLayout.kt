package com.programmersbox.kmpuiviews.presentation.onboarding.frame

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import com.programmersbox.kmpuiviews.presentation.onboarding.HeroShape
import com.programmersbox.kmpuiviews.presentation.onboarding.OnboardingStepSpec
import com.programmersbox.kmpuiviews.presentation.onboarding.OnboardingTags
import com.programmersbox.kmpuiviews.utils.LocalWindowSizeClass

/** Hero size shared by [StoryStepsScaffold] (which draws it) and [StepLayout] (which reserves its space). */
internal fun heroSizeFor(expanded: Boolean): Dp = if (expanded) 104.dp else 64.dp

/**
 * Lets the scaffold read each compact step's list scroll (keyed by step id) so the hero can leave with
 * the space [StepLayout] reserves for it instead of covering the content that scrolls up.
 */
internal class HeroScroll {
    val states = mutableStateMapOf<String, LazyListState>()

    /** Pixels the page [id] has scrolled, or [Int.MAX_VALUE] once the reserved space is off screen. */
    fun offsetOf(id: String): Int {
        val state = states[id] ?: return 0
        return if (state.firstVisibleItemIndex > 0) Int.MAX_VALUE else state.firstVisibleItemScrollOffset
    }
}

internal val LocalHeroScroll = staticCompositionLocalOf<HeroScroll?> { null }

/** Space [StepLayout] keeps free at the top-left for the hero the scaffold overlays there. */
internal fun heroReserveFor(expanded: Boolean): Dp = heroSizeFor(expanded) + heroInsetFor(expanded) * 2 + 12.dp

/**
 * How far the hero is inset from the top-left of the pager area so its largest scale
 * ([HeroShape.Done]) still fits inside the area's clip: overflow per side = size * (maxScale - 1) / 2.
 */
internal fun heroInsetFor(expanded: Boolean): Dp = heroSizeFor(expanded) * (HeroShape.Done.scale - 1f) / 2f

@Composable
fun isExpandedWidth(): Boolean = LocalWindowSizeClass.current.widthSizeClass == WindowWidthSizeClass.Expanded

/**
 * One step's page. The scaffold draws the hero over the top-left of this area; this layout reserves
 * that space ([heroReserveFor]) and renders the step title and body.
 *
 * Compact: a single `LazyColumn` (hero space, title, body, sticky [preview], [controls]).
 * Expanded: left 45% title + body under the hero, right 55% an independent `LazyColumn`
 * ([preview] sticky, then [controls]).
 */
@Composable
fun StepLayout(
    spec: OnboardingStepSpec,
    expanded: Boolean = isExpandedWidth(),
    preview: (@Composable () -> Unit)? = null,
    controls: @Composable ColumnScope.() -> Unit,
) {
    val reserve = if (spec.shape == HeroShape.Hidden) 0.dp else heroReserveFor(expanded)
    if (expanded) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(32.dp),
            modifier = Modifier.fillMaxSize().testTag(OnboardingTags.TwoPane),
        ) {
            Column(Modifier.weight(0.45f).fillMaxSize()) {
                Spacer(Modifier.height(reserve))
                StepHeading(spec, expanded = true)
            }
            LazyColumn(
                state = rememberLazyListState(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(0.55f).fillMaxSize(),
            ) {
                stepBody(preview, controls)
            }
        }
    } else {
        val listState = rememberLazyListState()
        val heroScroll = LocalHeroScroll.current
        if (heroScroll != null) {
            DisposableEffect(heroScroll, spec.id, listState) {
                heroScroll.states[spec.id] = listState
                onDispose { if (heroScroll.states[spec.id] === listState) heroScroll.states.remove(spec.id) }
            }
        }
        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize().testTag(OnboardingTags.SinglePane),
        ) {
            item(key = "hero_space") { Spacer(Modifier.height(reserve).animateItem()) }
            item(key = "heading") { StepHeading(spec, expanded = false, modifier = Modifier.animateItem()) }
            stepBody(preview, controls)
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.stepBody(
    preview: (@Composable () -> Unit)?,
    controls: @Composable ColumnScope.() -> Unit,
) {
    if (preview != null) {
        stickyHeader(key = "preview") {
            Box(Modifier.fillMaxWidth().animateItem()) { preview() }
        }
    }
    item(key = "controls") {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth().animateItem(),
            content = controls,
        )
    }
}

@Composable
private fun StepHeading(spec: OnboardingStepSpec, expanded: Boolean, modifier: Modifier = Modifier) {
    Column(modifier.padding(bottom = 4.dp)) {
        Text(
            spec.title,
            style = MaterialTheme.typography.headlineSmall,
            fontSize = if (expanded) 38.sp else 25.sp,
            lineHeight = if (expanded) 44.sp else 30.sp,
        )
        if (spec.body.isNotBlank()) {
            Text(
                spec.body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}
