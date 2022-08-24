package com.programmersbox.uiviews.utils.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.zIndex

enum class FadingAction {
    Fade, None
}

fun Modifier.fadingQuickAction(
    key: FadingAction,
    onAnimationEnd: () -> Unit,
    content: @Composable () -> Unit
) = composed {
    val alphaValue = remember { Animatable(0f) }

    LaunchedEffect(key) {
        when (key) {
            FadingAction.Fade -> alphaValue
            else -> null
        }?.let { animatable ->
            animatable.animateTo(1f)
            animatable.animateTo(0f)
            onAnimationEnd()
        }
    }

    Row(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .weight(1f)
                .zIndex(10f)
                .alpha(alphaValue.value)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) { content() }
    }

    this
}