@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.programmersbox.uiviews.utils.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
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
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec()
    ) { -it },
    bannerExit: ExitTransition = slideOutVertically(
        animationSpec = MaterialTheme.motionScheme.fastEffectsSpec()
    ) { -it } + fadeOut(),
    content: @Composable BoxScope.() -> Unit,
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
    content: @Composable BoxScope.() -> Unit,
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