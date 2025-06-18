package com.programmersbox.uiviews.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.WindowManager
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.ActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomAppBarScrollBehavior
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.app.ActivityOptionsCompat
import com.programmersbox.uiviews.R

@OptIn(ExperimentalMaterial3Api::class)
val LocalBottomAppBarScrollBehavior = staticCompositionLocalOf<BottomAppBarScrollBehavior> { error("") }

@Composable
fun <T : Any> rememberMutableStateListOf(vararg elements: T): SnapshotStateList<T> = rememberSaveable(
    saver = listSaver(
        save = { it.toList() },
        restore = { it.toMutableStateList() }
    )
) { elements.toList().toMutableStateList() }

/**
 * Registers a broadcast receiver and unregisters at the end of the composable lifecycle
 *
 * @param defaultValue the default value that this starts as
 * @param intentFilter the filter for intents
 * @see IntentFilter
 * @param tick the callback from the broadcast receiver
 */
@Composable
fun <T : Any> broadcastReceiver(defaultValue: T, intentFilter: IntentFilter, tick: (context: Context, intent: Intent) -> T): State<T> {
    val item: MutableState<T> = remember { mutableStateOf(defaultValue) }
    val context = LocalContext.current

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                item.value = tick(context, intent)
            }
        }
        context.registerReceiver(receiver, intentFilter)
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
fun <T : Any> broadcastReceiverNullable(defaultValue: T?, intentFilter: IntentFilter, tick: (context: Context, intent: Intent) -> T?): State<T?> {
    val item: MutableState<T?> = remember { mutableStateOf(defaultValue) }
    val context = LocalContext.current

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                item.value = tick(context, intent)
            }
        }
        context.registerReceiver(receiver, intentFilter)
        onDispose { context.unregisterReceiver(receiver) }
    }
    return item
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LoadingDialog(
    showLoadingDialog: Boolean,
    onDismissRequest: () -> Unit,
) {
    if (showLoadingDialog) {
        Dialog(
            onDismissRequest = onDismissRequest,
            DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(100.dp)
                    .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(28.0.dp))
            ) {
                Column {
                    ContainedLoadingIndicator(
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Text(text = stringResource(id = R.string.loading), Modifier.align(Alignment.CenterHorizontally))
                }
            }
        }

    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun loadingDialog(): MutableState<Boolean> {
    val showLoadingDialog = remember { mutableStateOf(false) }

    if (showLoadingDialog.value) {
        Dialog(
            onDismissRequest = { showLoadingDialog.value = false },
            DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(100.dp)
                    .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(28.0.dp))
            ) {
                Column {
                    ContainedLoadingIndicator(
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Text(text = stringResource(id = R.string.loading), Modifier.align(Alignment.CenterHorizontally))
                }
            }
        }
    }

    return showLoadingDialog
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

@Composable
fun KeepScreenOn() {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val window = context.findActivity().window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
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
