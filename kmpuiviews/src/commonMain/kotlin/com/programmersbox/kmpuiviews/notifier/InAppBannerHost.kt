package com.programmersbox.kmpuiviews.notifier

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.programmersbox.kmpuiviews.utils.LocalWindowSizeClass
import com.programmersbox.sharedcomponents.notifier.BannerId
import com.programmersbox.sharedcomponents.notifier.InAppNotifier
import com.programmersbox.sharedcomponents.notifier.PauseReason
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.banner_clear_all

private val Spec = spring<Float>(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)

/**
 * App-wide floating banner stack.
 *
 * @param topEnd Place the stack at the top end (wide windows) instead of top center.
 * Defaults to reading [LocalWindowSizeClass]; pass `false` where that local is not provided.
 */
@Composable
fun InAppBannerHost(
    modifier: Modifier = Modifier,
    topEnd: Boolean = LocalWindowSizeClass.current.widthSizeClass == WindowWidthSizeClass.Expanded,
) {
    val notifier = koinInject<InAppNotifier>()
    val banners by notifier.banners.collectAsState()
    var expanded by rememberSaveable { mutableStateOf(false) }
    val isExpanded = expanded && banners.size >= 2

    LaunchedEffect(banners.size) { if (banners.size < 2) expanded = false }
    LaunchedEffect(isExpanded) { notifier.setPaused(PauseReason.Expanded, isExpanded) }
    LifecycleResumeEffect(Unit) {
        notifier.setPaused(PauseReason.Background, false)
        onPauseOrDispose { notifier.setPaused(PauseReason.Background, true) }
    }
    val accessibility = LocalAccessibilityManager.current
    LaunchedEffect(accessibility) {
        notifier.setTimeoutAdjuster { ms, hasAction ->
            accessibility?.calculateRecommendedTimeoutMillis(
                ms,
                containsIcons = true,
                containsText = true,
                containsControls = hasAction,
            ) ?: ms
        }
    }

    if (banners.isEmpty()) return

    val density = LocalDensity.current
    val peekPx = with(density) { 9.dp.toPx() }
    val gapPx = with(density) { 8.dp.toPx() }
    val chipPx = with(density) { 48.dp.toPx() }
    val fallbackHeight = with(density) { 72.dp.toPx() }
    val heights = remember { mutableStateMapOf<BannerId, Float>() }
    val heightList = banners.map { heights[it.id] ?: fallbackHeight }
    val slots = bannerSlots(heightList, isExpanded, peekPx, gapPx)

    // Hit testing ignores children outside the parent's bounds, so size the deck to what is drawn.
    val deckHeightPx = slots.lastOrNull()?.let { last ->
        val bottom = last.translationY + heightList.last() * last.scale
        if (isExpanded) bottom + chipPx else bottom
    } ?: 0f
    val animatedDeckHeight by animateFloatAsState(deckHeightPx, Spec)

    Box(modifier.fillMaxSize()) {
        if (isExpanded) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.32f))
                    .clickable { expanded = false },
            )
        }
        Box(
            Modifier
                .align(if (topEnd) Alignment.TopEnd else Alignment.TopCenter)
                .windowInsetsPadding(
                    WindowInsets.statusBars.union(WindowInsets.displayCutout)
                        .only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
                )
                .padding(if (topEnd) 16.dp else 12.dp)
                .then(if (topEnd) Modifier.width(360.dp) else Modifier.fillMaxWidth().widthIn(max = 480.dp))
                .height(with(density) { animatedDeckHeight.toDp() }),
        ) {
            banners.forEachIndexed { index, banner ->
                key(banner.id) {
                    val slot = slots[index]
                    val height = heightList[index]
                    var entered by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) { entered = true }
                    val ty by animateFloatAsState(if (entered) slot.translationY else -height, Spec)
                    val scale by animateFloatAsState(slot.scale, Spec)
                    val alpha by animateFloatAsState(if (entered) slot.alpha else 0f, Spec)
                    val contentAlpha by animateFloatAsState(slot.contentAlpha, Spec)

                    SwipeToDismissBanner(
                        enabled = slot.interactive,
                        onDismiss = { notifier.dismiss(banner.id) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(align = Alignment.Top, unbounded = true)
                            .zIndex(slot.zIndex)
                            .onSizeChanged { heights[banner.id] = it.height.toFloat() }
                            .graphicsLayer {
                                translationY = ty
                                scaleX = scale
                                scaleY = scale
                                this.alpha = alpha
                                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0f)
                            },
                    ) {
                        BannerCard(
                            banner = banner,
                            contentAlpha = contentAlpha,
                            onDismiss = { notifier.dismiss(banner.id) },
                            onAction = { notifier.performAction(banner.id) },
                            modifier = if (index == 0 && !isExpanded && banners.size >= 2) {
                                Modifier.clickable { expanded = true }
                            } else {
                                Modifier
                            },
                        )
                    }
                }
            }
            if (isExpanded) {
                AssistChip(
                    onClick = { notifier.dismissAll() },
                    label = { Text(stringResource(Res.string.banner_clear_all)) },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .zIndex(0f)
                        .graphicsLayer { translationY = deckHeightPx - chipPx + gapPx },
                )
            }
        }
    }
}
