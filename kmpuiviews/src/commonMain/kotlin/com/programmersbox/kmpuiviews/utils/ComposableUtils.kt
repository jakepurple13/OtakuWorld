package com.programmersbox.kmpuiviews.utils

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import com.programmersbox.datastore.GridChoice
import com.programmersbox.datastore.ThemeColor
import com.programmersbox.favoritesdatabase.ActivityDao
import com.programmersbox.supabaseintegration.repository.ActivityRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

object ComposableUtils {
    const val IMAGE_WIDTH_PX = 360
    const val IMAGE_HEIGHT_PX = 480
    val IMAGE_WIDTH @Composable get() = with(LocalDensity.current) { IMAGE_WIDTH_PX.toDp() }
    val IMAGE_HEIGHT @Composable get() = with(LocalDensity.current) { IMAGE_HEIGHT_PX.toDp() }
}

val ThemeColor.seedColor
    get() = when (this) {
        ThemeColor.Dynamic -> Color.Transparent
        ThemeColor.Blue -> Color.Blue
        ThemeColor.Red -> Color.Red
        ThemeColor.Green -> Color.Green
        ThemeColor.Yellow -> Color.Yellow
        ThemeColor.Cyan -> Color.Cyan
        ThemeColor.Magenta -> Color.Magenta
        else -> Color.Transparent
    }

enum class ComponentState { Pressed, Released }

val LocalWindowSizeClass = staticCompositionLocalOf<WindowSizeClass> {
    error("No WindowSizeClass available")
}

val gridColumns: Int
    @Composable get() = when (LocalWindowSizeClass.current.widthSizeClass) {
        WindowWidthSizeClass.Compact -> 3
        WindowWidthSizeClass.Medium -> 5
        else -> 6
    }

@Composable
fun adaptiveGridCell(width: Dp = ComposableUtils.IMAGE_WIDTH): GridCells {
    val gridChoice by LocalSettingsHandling.current.rememberGridChoice()
    return when (gridChoice) {
        GridChoice.FullAdaptive -> remember(width) { CustomAdaptive(width) }
        GridChoice.Adaptive -> remember(width) { GridCells.Adaptive(width) }
        GridChoice.Fixed -> GridCells.Fixed(gridColumns)
        else -> GridCells.Fixed(gridColumns)
    }
}

class CustomAdaptive(private val minSize: Dp) : GridCells {
    init {
        require(minSize > 0.dp)
    }

    override fun Density.calculateCrossAxisCellSizes(
        availableSize: Int,
        spacing: Int,
    ): List<Int> {
        val count = maxOf((availableSize + spacing) / (minSize.roundToPx() + spacing), 1) + 1
        return calculateCellsCrossAxisSizeImpl(availableSize, count, spacing)
    }

    override fun hashCode(): Int {
        return minSize.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return other is CustomAdaptive && minSize == other.minSize
    }
}

private fun calculateCellsCrossAxisSizeImpl(
    gridSize: Int,
    slotCount: Int,
    spacing: Int,
): List<Int> {
    val gridSizeWithoutSpacing = gridSize - spacing * (slotCount - 1)
    val slotSize = gridSizeWithoutSpacing / slotCount
    val remainingPixels = gridSizeWithoutSpacing % slotCount
    return List(slotCount) {
        slotSize + if (it < remainingPixels) 1 else 0
    }
}

fun Int.toComposeColor() = Color(this)

@Composable
fun Color.animate(label: String = "") = animateColorAsState(this, label = label)

@Composable
fun Color?.animate(default: Color, label: String = "") =
    animateColorAsState(this ?: default, label = label)

@Composable
fun LazyListState.isScrollingUp(): Boolean {
    var previousIndex by remember(this) { mutableIntStateOf(firstVisibleItemIndex) }
    var previousScrollOffset by remember(this) { mutableIntStateOf(firstVisibleItemScrollOffset) }
    return remember(this) {
        derivedStateOf {
            if (previousIndex != firstVisibleItemIndex) {
                previousIndex > firstVisibleItemIndex
            } else {
                previousScrollOffset >= firstVisibleItemScrollOffset
            }.also {
                previousIndex = firstVisibleItemIndex
                previousScrollOffset = firstVisibleItemScrollOffset
            }
        }
    }.value
}

private const val TIME_SPENT_FLUSH_SECONDS = 30

@Composable
fun RecordTimeSpentDoing() {
    val activityRepository = koinInject<ActivityRepository>()
    val activityDao = koinInject<ActivityDao>()
    val scope = rememberCoroutineScope()

    // Counts only while started and keeps the seconds in memory, writing them every
    // TIME_SPENT_FLUSH_SECONDS and on stop, so Room isn't written (and every observer
    // invalidated) once a second.
    LifecycleStartEffect(Unit) {
        var pending = 0L
        fun flush(markDirty: Boolean = false) {
            val seconds = pending
            pending = 0
            if (seconds > 0) GlobalScope.launch(Dispatchers.IO) {
                activityDao.incrementSeconds(seconds)
                // Mark dirty after the final write so sync picks up these seconds
                if (markDirty) activityRepository.onActivityStop()
            }
        }

        val job = scope.launch {
            while (true) {
                delay(1000)
                pending++
                if (pending >= TIME_SPENT_FLUSH_SECONDS) flush()
            }
        }
        onStopOrDispose {
            job.cancel()
            flush(markDirty = true)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            // rememberCoroutineScope() is cancelled as part of this same composable's
            // teardown, so a fire-and-forget network call (triggerSync) launched on it
            // here would be cancelled before it can complete. Use a scope that outlives
            // the disposal, matching the equivalent app-level trigger in KmpOtakuApp.kt.
            GlobalScope.launch(Dispatchers.IO) { activityRepository.onActivityStop() }
        }
    }
}