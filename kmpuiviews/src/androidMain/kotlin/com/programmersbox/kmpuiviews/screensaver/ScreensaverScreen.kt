package com.programmersbox.kmpuiviews.screensaver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.BatteryManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.programmersbox.kmpuiviews.DateTimeFormatHandler
import com.programmersbox.kmpuiviews.utils.DateTimeFormatScreensaverItem
import com.programmersbox.kmpuiviews.utils.toLocalDateTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.koin.compose.koinInject
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@Composable
fun ScreensaverScreen() {
    val resources = LocalConfiguration.current.orientation

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
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        val orientation by rememberUpdatedState(resources)

        AnimatedContent(orientation) { target ->
            Scaffold { padding ->
                Surface(
                    modifier = Modifier.padding(padding)
                ) {
                    when (target) {
                        Configuration.ORIENTATION_LANDSCAPE -> {
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

                        Configuration.ORIENTATION_PORTRAIT -> {
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
                    }
                }
            }
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