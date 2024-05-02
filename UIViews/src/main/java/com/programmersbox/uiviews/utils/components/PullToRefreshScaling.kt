package com.programmersbox.uiviews.utils.components

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtakuPullToRefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    state: PullToRefreshState = rememberPullToRefreshState(),
    indicator: @Composable BoxScope.() -> Unit = {
        OtakuPullToRefreshDefaults.ScalingIndicator(isRefreshing, state, Modifier.align(Alignment.TopCenter))
    },
    enabled: () -> Boolean = { true },
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier.pullToRefresh(
            state = state,
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            enabled = enabled
        )
    ) {
        content()
        indicator()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtakuPullToRefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    paddingValues: PaddingValues,
    modifier: Modifier = Modifier,
    state: PullToRefreshState = rememberPullToRefreshState(),
    indicator: @Composable BoxScope.() -> Unit = {
        OtakuPullToRefreshDefaults.ScalingIndicator(
            isRefreshing = isRefreshing,
            state = state,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(paddingValues)
        )
    },
    enabled: () -> Boolean = { true },
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier.pullToRefresh(
            state = state,
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            enabled = enabled
        )
    ) {
        content()
        indicator()
    }
}

object OtakuPullToRefreshDefaults {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ScalingIndicator(
        isRefreshing: Boolean,
        state: PullToRefreshState,
        modifier: Modifier = Modifier,
    ) {
        val scaleFraction =
            { if (isRefreshing) 1f else LinearOutSlowInEasing.transform(state.distanceFraction).coerceIn(0f, 1f) }
        PullToRefreshDefaults.Indicator(
            modifier = modifier.graphicsLayer {
                scaleX = scaleFraction()
                scaleY = scaleFraction()
            },
            isRefreshing = isRefreshing,
            state = state
        )
    }
}