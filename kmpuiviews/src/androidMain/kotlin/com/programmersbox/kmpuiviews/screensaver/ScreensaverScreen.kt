package com.programmersbox.kmpuiviews.screensaver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.view.OrientationEventListener
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableLongState
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.programmersbox.favoritesdatabase.NotificationItem
import com.programmersbox.kmpuiviews.DateTimeFormatHandler
import com.programmersbox.kmpuiviews.presentation.components.M3CoverCard2
import com.programmersbox.kmpuiviews.utils.DateTimeFormatScreensaverItem
import com.programmersbox.kmpuiviews.utils.adaptiveGridCell
import com.programmersbox.kmpuiviews.utils.toLocalDateTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@Composable
fun ScreensaverScreen(
    viewModel: ScreensaverViewModel = koinViewModel(),
) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val physicalOrientation by rememberPhysicalDeviceOrientation()
    Scaffold { _ ->
        SensorRotatedLayout(physicalOrientation = physicalOrientation) {
            SharedTransitionLayout {
                val boxes = remember {
                    movableContentOf { modifier: Modifier, animatedVisibilityScope: AnimatedVisibilityScope ->
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = modifier
                                .sharedBounds(
                                    sharedContentState = rememberSharedContentState(
                                        key = "other",
                                    ),
                                    animatedVisibilityScope = animatedVisibilityScope
                                )
                                .animateContentSize()
                        ) {
                            DateBatteryCard(
                                modifier = Modifier.weight(.7f)
                            )

                            InfoCard(
                                activity = viewModel.activity,
                                favoriteCount = viewModel.favoritesCount,
                                modifier = Modifier.weight(.3f)
                            )
                        }

                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = modifier
                                .sharedBounds(
                                    sharedContentState = rememberSharedContentState(
                                        key = "list",
                                    ),
                                    animatedVisibilityScope = animatedVisibilityScope
                                )
                                .animateContentSize()
                        ) {
                            ItemsCard(
                                list = items,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                AnimatedContent(physicalOrientation) { target ->
                    when (target) {
                        PhysicalOrientation.PORTRAIT,
                        PhysicalOrientation.REVERSE_PORTRAIT,
                        PhysicalOrientation.UNKNOWN,
                            -> {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp)
                            ) {
                                boxes(
                                    Modifier.weight(1f, false),
                                    this@AnimatedContent
                                )
                            }
                        }

                        PhysicalOrientation.LANDSCAPE_LEFT,
                        PhysicalOrientation.LANDSCAPE_RIGHT,
                            -> {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp)
                            ) {
                                boxes(
                                    Modifier.weight(1f, false),
                                    this@AnimatedContent
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ItemsCard(
    modifier: Modifier = Modifier,
    list: List<NotificationItem>,
) {
    val listState = rememberLazyGridState()

    SlowScroll(
        listState = listState,
        animateScrollToItem = { listState.animateScrollToItem(it) }
    )

    Card(
        shape = MaterialTheme.shapes.extraLarge,
        modifier = modifier.animateContentSize()
    ) {
        LazyVerticalGrid(
            columns = adaptiveGridCell(),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clip(MaterialTheme.shapes.extraLarge)
                .animateContentSize()
        ) {
            item(
                span = { GridItemSpan(maxLineSpan) },
            ) {
                Text(
                    "Saved For Later",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateItem()
                )
            }

            items(list) {
                M3CoverCard2(
                    imageUrl = it.imageUrl.orEmpty(),
                    name = it.notiTitle,
                    placeHolder = { rememberVectorPainter(Icons.Default.Settings) },
                    modifier = Modifier.animateItem()
                )
            }
        }
    }
}

@Composable
private fun InfoCard(
    activity: String,
    favoriteCount: Int,
    modifier: Modifier = Modifier,
) {
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        modifier = modifier.animateContentSize()
    ) {
        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .animateContentSize()
        ) {
            Text("Time Spent Doing: $activity")
            Text("Favorites: $favoriteCount")
        }
    }
}

@Composable
private fun DateBatteryCard(
    modifier: Modifier = Modifier,
) {
    val timeRemaining by rememberBatteryInfo()

    val animatedProgress by animateFloatAsState(
        targetValue = timeRemaining.percentage / 100f,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
    )

    val dateTimeFormatHandler: DateTimeFormatHandler = koinInject()

    val dateFormat = DateTimeFormatScreensaverItem(
        dateTimeFormatHandler.is24Time()
    )

    val currentTimeMs by rememberCurrentTime()

    val timeString by remember {
        derivedStateOf {
            dateFormat.format(currentTimeMs.toLocalDateTime())
        }
    }

    Card(
        shape = MaterialTheme.shapes.extraLarge,
        modifier = modifier.animateContentSize()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .animateContentSize()
        ) {
            Column {
                Text(
                    timeString,
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text("Battery:")
                Text("${timeRemaining.percentage}%")
                Text("Time Remaining Until Full:")
                Text("${timeRemaining.timeRemainingMs.milliseconds}")
            }

            CircularWavyProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.size(64.dp)
            )
        }
    }
}

data class BatteryInfo(
    val timeRemainingMs: Long,
    val percentage: Int,
)

@Composable
fun rememberBatteryInfo(): State<BatteryInfo> {
    val context = LocalContext.current
    val batteryInfo = remember { mutableStateOf(BatteryInfo(-1L, -1)) }

    // Helper function to fetch the latest battery states
    val updateBatteryInfo = {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager

        val timeRemaining = batteryManager.computeChargeTimeRemaining()

        // Returns the current battery level as an integer from 0 to 100
        val percentage = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)

        batteryInfo.value = BatteryInfo(
            timeRemainingMs = timeRemaining,
            percentage = percentage
        )
    }

    // 1. Listen for system battery events
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                updateBatteryInfo()
            }
        }

        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        context.registerReceiver(receiver, filter)

        // Fetch immediately upon subscription to avoid waiting for the first tick
        updateBatteryInfo()

        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    // 2. Poll every 1 minute as a fallback
    LaunchedEffect(Unit) {
        while (coroutineContext.isActive) {
            updateBatteryInfo()
            val delayDuration = if (batteryInfo.value.timeRemainingMs == 0L) {
                1.hours
            } else if (batteryInfo.value.timeRemainingMs.milliseconds < 5.minutes) {
                5.seconds
            } else {
                1.minutes
            }
            delay(delayDuration) // Wait 1 minute
        }
    }

    return batteryInfo
}

@Composable
fun rememberCurrentTime(): MutableLongState {
    val context = LocalContext.current
    val currentTime = remember { mutableLongStateOf(System.currentTimeMillis()) }

    // 1. Listen for system time events
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                currentTime.longValue = System.currentTimeMillis()
            }
        }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)        // Fires exactly at the top of every minute
            addAction(Intent.ACTION_TIME_CHANGED)     // Fires if the user manually changes the time
            addAction(Intent.ACTION_TIMEZONE_CHANGED) // Fires if the time zone changes
        }

        context.registerReceiver(receiver, filter)

        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    return currentTime
}

@Composable
fun SlowScroll(
    listState: ScrollableState,
    animateScrollToItem: suspend (Int) -> Unit,
    // Speed in pixels per second
    speed: Float = 40f,
    bottomPause: Duration = remember { 1.5.seconds },
    pauseAfterInteraction: Duration = remember { 2.seconds },
    pauseBeforeStartingAgain: Duration = remember { 1.seconds },
) {
    LaunchedEffect(
        speed,
        bottomPause,
        animateScrollToItem
    ) {
        while (isActive) {
            if (listState.canScrollForward) {
                try {
                    // Open a scroll block. This gives us access to ScrollScope.scrollBy()
                    listState.scroll {
                        var lastFrameTime = withFrameNanos { it }

                        while (listState.canScrollForward) {
                            val currentFrameTime = withFrameNanos { it }
                            val deltaSeconds = (currentFrameTime - lastFrameTime) / 1_000_000_000f
                            lastFrameTime = currentFrameTime

                            val pixelsToScroll = speed * deltaSeconds
                            val consumed = scrollBy(pixelsToScroll)

                            // Failsafe: if we requested a scroll but didn't move, we hit the end
                            if (pixelsToScroll > 0f && consumed == 0f) {
                                break
                            }
                        }
                    }
                } catch (e: CancellationException) {
                    // This triggers if the user manually drags the grid.
                    // We re-throw if the entire Composable is actually being destroyed.
                    if (!isActive) throw e

                    // Wait a moment before resuming the auto-scroll after the user lets go
                    delay(pauseAfterInteraction)
                }
            } else {
                // Reached the bottom of the list
                delay(bottomPause) // Pause briefly at the bottom
                animateScrollToItem(0) // Smoothly scroll back to the top
                delay(pauseBeforeStartingAgain) // Pause briefly before starting the downward scroll again
            }
        }
    }
}

enum class PhysicalOrientation {
    PORTRAIT,
    LANDSCAPE_LEFT,    // Left side of the device is pointing up
    REVERSE_PORTRAIT,  // Upside down
    LANDSCAPE_RIGHT,   // Right side of the device is pointing up
    UNKNOWN
}

@Composable
fun rememberPhysicalDeviceOrientation(): State<PhysicalOrientation> {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val orientation = remember { mutableStateOf(PhysicalOrientation.UNKNOWN) }

    DisposableEffect(context, lifecycleOwner) {
        val listener = object : OrientationEventListener(context) {
            override fun onOrientationChanged(degrees: Int) {
                if (degrees == ORIENTATION_UNKNOWN) return // Device is flat

                // Map 360 degrees to 4 basic orientations
                orientation.value = when (degrees) {
                    in 45..134 -> PhysicalOrientation.LANDSCAPE_LEFT
                    in 135..224 -> PhysicalOrientation.REVERSE_PORTRAIT
                    in 225..314 -> PhysicalOrientation.LANDSCAPE_RIGHT
                    else -> PhysicalOrientation.PORTRAIT // 0..44 and 315..359
                }
            }
        }

        // Only listen when the activity is active to save battery
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> if (listener.canDetectOrientation()) listener.enable()
                Lifecycle.Event.ON_STOP -> listener.disable()
                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            listener.disable()
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    return orientation
}

@Composable
fun SensorRotatedLayout(
    physicalOrientation: PhysicalOrientation,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val targetRotation = when (physicalOrientation) {
        PhysicalOrientation.PORTRAIT, PhysicalOrientation.UNKNOWN -> 0f
        PhysicalOrientation.LANDSCAPE_LEFT -> -90f
        PhysicalOrientation.REVERSE_PORTRAIT -> 180f
        PhysicalOrientation.LANDSCAPE_RIGHT -> 90f
    }

    val animatedRotation by animateFloatAsState(
        targetValue = targetRotation,
        animationSpec = tween(durationMillis = 500),
        label = "rotation"
    )

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val isLandscape = targetRotation % 180 != 0f

        // Swap width and height based on orientation
        val targetWidth = if (isLandscape) maxHeight else maxWidth
        val targetHeight = if (isLandscape) maxWidth else maxHeight

        // Animate the size change so SharedTransitionLayout doesn't jump
        val animatedWidth by animateDpAsState(
            targetValue = targetWidth,
            animationSpec = tween(durationMillis = 500),
            label = "width"
        )
        val animatedHeight by animateDpAsState(
            targetValue = targetHeight,
            animationSpec = tween(durationMillis = 500),
            label = "height"
        )

        Box(
            modifier = Modifier
                .requiredSize(width = animatedWidth, height = animatedHeight)
                .graphicsLayer {
                    rotationZ = animatedRotation
                },
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}