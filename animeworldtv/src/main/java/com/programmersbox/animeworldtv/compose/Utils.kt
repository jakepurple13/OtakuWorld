package com.programmersbox.animeworldtv.compose

import android.content.Context
import android.content.ContextWrapper
import android.view.KeyEvent
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavHostController
import androidx.tv.material3.ColorScheme
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme
import androidx.tv.material3.lightColorScheme

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun OtakuMaterialTheme(
    navController: NavHostController,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val darkTheme = AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES ||
            (isSystemInDarkTheme() && AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
    MaterialTheme(currentColorScheme) {
        CompositionLocalProvider(
            LocalActivity provides remember { context.findActivity() },
            LocalNavController provides navController,
            LocalCurrentSource provides remember { CurrentSourceRepository() }
        ) { content() }
    }
}

val LocalNavController = staticCompositionLocalOf<NavHostController> { error("No NavController Found!") }
val LocalCurrentSource = staticCompositionLocalOf<CurrentSourceRepository> { CurrentSourceRepository() }
val LocalActivity = staticCompositionLocalOf<FragmentActivity> { error("Context is not an Activity.") }

fun Context.findActivity(): FragmentActivity {
    var currentContext = this
    while (currentContext is ContextWrapper) {
        if (currentContext is FragmentActivity) {
            return currentContext
        }
        currentContext = currentContext.baseContext
    }
    error("Context is not an Activity.")
}

@OptIn(ExperimentalTvMaterial3Api::class)
val currentColorScheme: ColorScheme
    @Composable
    get() {
        val darkTheme = AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES ||
                (isSystemInDarkTheme() && AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        return when {
            darkTheme -> darkColorScheme(
                primary = Color(0xff90CAF9),
                secondary = Color(0xff90CAF9)
            )

            else -> lightColorScheme(
                primary = Color(0xff2196F3),
                secondary = Color(0xff90CAF9)
            )
        }
    }

private val DPadEventsKeyCodes = listOf(
    KeyEvent.KEYCODE_DPAD_LEFT,
    KeyEvent.KEYCODE_DPAD_RIGHT,
    KeyEvent.KEYCODE_DPAD_CENTER,
    KeyEvent.KEYCODE_ENTER,
    KeyEvent.KEYCODE_NUMPAD_ENTER
)

/**
 * Handles horizontal (Left & Right) D-Pad Keys and consumes the event(s) so that the focus doesn't
 * accidentally move to another element.
 * */
fun Modifier.handleDPadKeyEvents(
    onLeft: (() -> Unit)? = null,
    onRight: (() -> Unit)? = null,
    onEnter: (() -> Unit)? = null
) = onPreviewKeyEvent {
    fun onActionUp(block: () -> Unit) {
        if (it.nativeKeyEvent.action == KeyEvent.ACTION_UP) block()
    }

    if (DPadEventsKeyCodes.contains(it.nativeKeyEvent.keyCode)) {
        when (it.nativeKeyEvent.keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_SYSTEM_NAVIGATION_LEFT -> {
                onLeft?.apply {
                    onActionUp(::invoke)
                    return@onPreviewKeyEvent true
                }
            }

            KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_SYSTEM_NAVIGATION_RIGHT -> {
                onRight?.apply {
                    onActionUp(::invoke)
                    return@onPreviewKeyEvent true
                }
            }

            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                onEnter?.apply {
                    onActionUp(::invoke)
                    return@onPreviewKeyEvent true
                }
            }
        }
    }
    false
}

/**
 * Fills max available size and only utilizes the content size for the composable. Useful for
 * cases when you need to quickly center the item on the available area.
 * */
fun Modifier.occupyScreenSize() = this
    .fillMaxSize()
    .wrapContentSize()

@Immutable
data class Padding(
    val start: Dp,
    val top: Dp,
    val end: Dp,
    val bottom: Dp,
)

val ParentPadding = PaddingValues(vertical = 16.dp, horizontal = 58.dp)

@Composable
fun rememberChildPadding(direction: LayoutDirection = LocalLayoutDirection.current): Padding {
    return remember {
        Padding(
            start = ParentPadding.calculateStartPadding(direction) + 8.dp,
            top = ParentPadding.calculateTopPadding(),
            end = ParentPadding.calculateEndPadding(direction) + 8.dp,
            bottom = ParentPadding.calculateBottomPadding()
        )
    }
}