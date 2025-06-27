package com.programmersbox.uiviews.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.ActivityResult
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.BottomAppBarScrollBehavior
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.core.app.ActivityOptionsCompat

@OptIn(ExperimentalMaterial3Api::class)
val LocalBottomAppBarScrollBehavior = staticCompositionLocalOf<BottomAppBarScrollBehavior> { error("") }

/**
 * Registers a broadcast receiver and unregisters at the end of the composable lifecycle
 *
 * @param defaultValue the default value that this starts as
 * @param intentFilter the filter for intents
 * @see IntentFilter
 * @param tick the callback from the broadcast receiver
 */
@Composable
fun <T : Any> broadcastReceiver(
    defaultValue: T,
    intentFilter: IntentFilter,
    flags: Int,
    tick: (context: Context, intent: Intent) -> T,
): State<T> {
    val item: MutableState<T> = remember { mutableStateOf(defaultValue) }
    val context = LocalContext.current

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                item.value = tick(context, intent)
            }
        }
        context.registerReceiver(receiver, intentFilter, flags)
        onDispose { context.unregisterReceiver(receiver) }
    }
    return item
}

/**
 * Registers a broadcast receiver and unregisters at the end of the composable lifecycle
 *
 * @param defaultValue the default value that this starts as
 * @param intentFilter the filter for intents.
 * @see IntentFilter
 * @param tick the callback from the broadcast receiver
 */
@Composable
fun <T : Any> broadcastReceiverNullable(
    defaultValue: T?,
    intentFilter: IntentFilter,
    flags: Int,
    tick: (context: Context, intent: Intent) -> T?,
): State<T?> {
    val item: MutableState<T?> = remember { mutableStateOf(defaultValue) }
    val context = LocalContext.current

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                item.value = tick(context, intent)
            }
        }
        context.registerReceiver(receiver, intentFilter, flags)
        onDispose { context.unregisterReceiver(receiver) }
    }
    return item
}

fun ManagedActivityResultLauncher<Intent, ActivityResult>.launchCatching(
    intent: Intent,
    options: ActivityOptionsCompat? = null,
) = runCatching {
    launch(
        input = intent,
        options = options
    )
}

@Composable
fun hapticInteractionSource(
    hapticFeedbackType: HapticFeedbackType = HapticFeedbackType.LongPress,
    enabled: Boolean = true,
): MutableInteractionSource {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    LaunchedEffect(isPressed) {
        if (isPressed && enabled) {
            haptic.performHapticFeedback(hapticFeedbackType)
        }
    }

    return interactionSource
}

/*
// Need `implementation("com.mutualmobile:composesensors:1.1.2")` to make this work
@Composable
fun HingeDetection(
    locationAlignment: Alignment = Alignment.TopCenter,
    timeoutDurationMillis: Long = 2000
) {
    val hingeState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        rememberHingeAngleSensorState()
    } else {
        null
    }

    hingeState?.let { hinge ->
        val alphaHinge = remember { Animatable(0f) }
        var firstCount by remember { mutableIntStateOf(0) }

        LaunchedEffect(hinge.angle) {
            alphaHinge.animateTo(1f)
            delay(timeoutDurationMillis)
            alphaHinge.animateTo(0f)
        }

        Box(
            contentAlignment = locationAlignment,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = alphaHinge.value }
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(WindowInsets.systemBars.asPaddingValues())
                    .clip(MaterialTheme.shapes.medium)
                    .background(Color.Black.copy(alpha = .75f))
                    .padding(16.dp)
            ) {
                Icon(
                    Icons.Default.DevicesFold,
                    null,
                    tint = Color.White,
                    modifier = Modifier.weight(1f, false)
                )

                Text(
                    "${hinge.angle}°",
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )

                Canvas(
                    Modifier
                        .size(24.dp)
                        .weight(1f, false)
                ) {
                    drawArc(
                        Color.White,
                        270f,
                        hinge.angle,
                        true
                    )
                    drawCircle(
                        color = Color.White,
                        style = Stroke()
                    )
                }
            }
        }
    }
}
*/
