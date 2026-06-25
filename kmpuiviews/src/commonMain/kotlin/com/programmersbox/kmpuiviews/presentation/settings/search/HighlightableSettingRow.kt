package com.programmersbox.kmpuiviews.presentation.settings.search

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HighlightableSettingRow(
    activeHighlight: MutableState<String?>,
    itemKey: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val isHighlighted = activeHighlight.value == itemKey

    val highlightColor by animateColorAsState(
        targetValue = if (isHighlighted) MaterialTheme.colorScheme.primaryContainer
                      else Color.Transparent,
        animationSpec = tween(durationMillis = 1500),
        finishedListener = { if (it == Color.Transparent) activeHighlight.value = null },
        label = "settingHighlight",
    )

    LaunchedEffect(isHighlighted) {
        if (isHighlighted) bringIntoViewRequester.bringIntoView()
    }

    Box(
        modifier = modifier
            .bringIntoViewRequester(bringIntoViewRequester)
            .background(highlightColor),
    ) {
        content()
    }
}

@Composable
fun rememberActiveHighlight(highlightState: SettingsHighlightState): MutableState<String?> =
    remember { mutableStateOf(highlightState.pendingHighlightKey.also { highlightState.pendingHighlightKey = null }) }
