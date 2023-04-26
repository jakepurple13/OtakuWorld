package com.programmersbox.uiviews.utils.components

import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt

@Composable
fun BannerBox(
    banner: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier,
    showBanner: Boolean = false,
    bannerEnter: EnterTransition = slideInVertically(
        animationSpec = tween(
            durationMillis = 150,
            easing = LinearOutSlowInEasing
        )
    ) { -it },
    bannerExit: ExitTransition = slideOutVertically(
        animationSpec = tween(
            durationMillis = 150,
            easing = LinearOutSlowInEasing
        )
    ) { -it },
    content: @Composable BoxScope.() -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        content()
        AnimatedVisibility(
            visible = showBanner,
            enter = bannerEnter,
            exit = bannerExit,
            modifier = modifier
        ) { banner() }
    }
}

@Composable
fun BannerBox2(
    bannerSize: Dp,
    banner: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier,
    showBanner: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        Modifier
            .fillMaxSize()
            .then(modifier)
    ) {
        content()
        val topBarHeightPx = with(LocalDensity.current) { bannerSize.roundToPx().toFloat() }
        val aniOffset = remember { Animatable(-topBarHeightPx * 2f) }
        LaunchedEffect(key1 = showBanner) { aniOffset.animateTo(if (showBanner) 0f else (-topBarHeightPx * 2f)) }
        Box(modifier = Modifier.offset { IntOffset(x = 0, y = aniOffset.value.roundToInt()) }) { banner() }
    }
}