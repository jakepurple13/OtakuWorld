package com.programmersbox.uiviews.utils.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * This code was taken from https://al-e-shevelev.medium.com/collapsible-panels-on-jetpack-compose-its-a-piece-of-cake-9a2e76bb70c9
 * It does a good job explaining things for a better feel
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollapsingScrollScaffold(
    topBarFixed: @Composable (Float) -> Unit,
    topBarCollapsing: @Composable (Float) -> Unit,
    bottomBar: @Composable () -> Unit = {},
    modifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit
) {
    val toolbarHeightPx = remember { mutableStateOf(0f) }
    val topPanelOffset = remember { mutableStateOf(0f) }

    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        topBar = {
            TopBarFixed(
                maxOffset = toolbarHeightPx.value,
                currentOffset = topPanelOffset.value,
                topBar = topBarFixed
            )
        },
        bottomBar = bottomBar
    ) { padding ->
        CoordinatedScroll(
            collapsingAreaHeightPx = toolbarHeightPx
        ) { offset, nestedScroll ->
            topPanelOffset.value = offset

            Box(
                Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .nestedScroll(nestedScroll)
            ) {
                TopBarCollapsing(
                    currentOffset = offset,
                    maxOffset = toolbarHeightPx.value,
                    modifier = Modifier.fillMaxWidth(),
                    onHeightCalculated = { toolbarHeightPx.value = it },
                    topBar = topBarCollapsing
                )

                val contentPadding = with(LocalDensity.current) {
                    toolbarHeightPx.value.toInt().toDp() - abs(offset).toDp()
                }

                Box(
                    modifier = Modifier.fillMaxSize()
                ) { content(PaddingValues(top = contentPadding, bottom = padding.calculateBottomPadding())) }
            }
        }
    }
}

@Composable
internal fun CoordinatedScroll(
    collapsingAreaHeightPx: MutableState<Float> = remember { mutableStateOf(0f) },
    content: @Composable (Float, NestedScrollConnection) -> Unit
) {
    val currentOffsetPx = remember { mutableStateOf(0f) }
    val currentAbsoluteOffsetPx = remember { mutableStateOf(0f) }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y

                var absoluteOffset = currentAbsoluteOffsetPx.value + delta
                if (absoluteOffset > 0f) {
                    absoluteOffset = 0f
                }
                currentAbsoluteOffsetPx.value = absoluteOffset

                if (absoluteOffset >= -collapsingAreaHeightPx.value) {
                    currentOffsetPx.value = absoluteOffset
                } else {
                    currentOffsetPx.value = -collapsingAreaHeightPx.value
                }

                return when {
                    // The panel is completely collapsed - an internal scroll must be turned on
                    abs(currentOffsetPx.value) == collapsingAreaHeightPx.value -> Offset.Zero

                    // The panel is completely expanded - we must turn on an internal scroll
                    // to complete content scrolling
                    abs(currentOffsetPx.value) == 0f -> Offset.Zero

                    // Intermediate state - the scroll is blocked
                    else -> available
                }
            }
        }
    }

    content(
        currentOffsetPx.value,
        nestedScrollConnection
    )
}

@Composable
internal fun TopBarCollapsing(
    currentOffset: Float,
    maxOffset: Float,
    modifier: Modifier = Modifier,
    onHeightCalculated: (Float) -> Unit,
    topBar: @Composable (Float) -> Unit
) {
    val alpha = (abs(currentOffset) / maxOffset).pow(0.75f)

    Column(
        modifier = modifier
            .offset { IntOffset(x = 0, y = currentOffset.roundToInt()) }
            .onGloballyPositioned { onHeightCalculated(it.size.height.toFloat()) }
    ) { topBar(alpha) }
}

@Composable
internal fun TopBarFixed(
    currentOffset: Float,
    maxOffset: Float,
    topBar: @Composable (Float) -> Unit
) {
    topBar((abs(currentOffset) / maxOffset).pow(2))
}
