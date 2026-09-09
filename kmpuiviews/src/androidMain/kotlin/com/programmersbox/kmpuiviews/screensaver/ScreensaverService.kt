package com.programmersbox.kmpuiviews.screensaver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.service.dreams.DreamService
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableLongState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.programmersbox.kmpuiviews.theme.OtakuMaterialTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.koin.compose.koinInject
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class ScreensaverService : DreamService(),
    LifecycleOwner,
    SavedStateRegistryOwner,
    ViewModelStoreOwner {

    // 2. Add the ViewModelStore
    private val store = ViewModelStore()
    override val viewModelStore: ViewModelStore
        get() = store

    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle
        field = LifecycleRegistry(this)
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        isInteractive = false
        isFullscreen = true

        // 2. Mark the lifecycle as active
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        // 3. Manually create the ViewModel using a factory to bind it to the store

        // 3. Create the ComposeView
        val composeView = ComposeView(this).apply {
            // Ensure Compose cleans up when the screensaver stops
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                OtakuMaterialTheme(
                    settingsHandling = koinInject()
                ) {
                    ScreensaverScreen()
                }
            }
        }

        // 4. Bind the registries to the View tree so Compose can find them
        composeView.setViewTreeLifecycleOwner(this)
        composeView.setViewTreeSavedStateRegistryOwner(this)
        composeView.setViewTreeViewModelStoreOwner(this)
        //ViewTreeLifecycleOwner.set(composeView, this)
        //ViewTreeSavedStateRegistryOwner.set(composeView, this)

        setContentView(composeView)
    }

    override fun onDetachedFromWindow() {
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        super.onDetachedFromWindow()
    }

    override fun onDestroy() {
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)

        // 6. Clear the store to cancel active coroutines inside the ViewModel
        store.clear()

        super.onDestroy()
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