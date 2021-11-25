package com.programmersbox.otakuworld

import android.app.Activity
import androidx.annotation.VisibleForTesting
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.toComposeRect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.window.layout.WindowMetricsCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.merge

/**
 * Opinionated set of viewport breakpoints
 *     - Compact: Most phones in portrait mode
 *     - Medium: Most foldables and tablets in portrait mode
 *     - Expanded: Most tablets in landscape mode
 *
 * More info: https://material.io/archive/guidelines/layout/responsive-ui.html
 */
enum class WindowSize { Compact, Medium, Expanded }

/**
 * Remembers the [WindowSize] class for the window corresponding to the current window metrics.
 */
@Composable
fun Activity.rememberWindowSizeClass(): WindowSize {
    // Get the size (in pixels) of the window
    val windowSize = rememberWindowSize()

    // Convert the window size to [Dp]
    val windowDpSize = with(LocalDensity.current) { windowSize.toDpSize() }

    // Calculate the window size class
    return getWindowSizeClass(windowDpSize)
}

/**
 * Remembers the [Size] in pixels of the window corresponding to the current window metrics.
 */
@Composable
private fun Activity.rememberWindowSize(): Size {
    val configuration = LocalConfiguration.current
    // WindowMetricsCalculator implicitly depends on the configuration through the activity,
    // so re-calculate it upon changes.
    val windowMetrics = remember(configuration) {
        WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(this)
    }
    return windowMetrics.bounds.toComposeRect().size
}

/**
 * Partitions a [DpSize] into a enumerated [WindowSize] class.
 */
@VisibleForTesting
fun getWindowSizeClass(windowDpSize: DpSize): WindowSize = when {
    windowDpSize.width < 0.dp -> throw IllegalArgumentException("Dp value cannot be negative")
    windowDpSize.width < 600.dp -> WindowSize.Compact
    windowDpSize.width < 840.dp -> WindowSize.Medium
    else -> WindowSize.Expanded
}

var currentScheme: ColorScheme by mutableStateOf(darkColorScheme())

abstract class FlowViewModel<T>(protected val flow: Flow<T>) : ViewModel() {
    protected abstract val manualFlow: MutableSharedFlow<T>

    @Composable
    fun collectAsState(initial: T): State<T> = merge(flow, manualFlow).collectAsState(initial = initial)

    suspend fun emit(item: T) = manualFlow.emit(item)
    fun tryEmit(item: T) = manualFlow.tryEmit(item)
}

class FlowStateViewModel<T>(flow: Flow<T>, initialValue: T) : FlowViewModel<T>(flow) {
    override val manualFlow = MutableStateFlow(initialValue)
}

class FlowSharedViewModel<T>(flow: Flow<T>) : FlowViewModel<T>(flow) {
    override val manualFlow = MutableSharedFlow<T>()
}