package com.programmersbox.manga.shared.reader

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

@Composable
actual fun insetsController(defaultValue: Boolean): MutableState<Boolean> {
    val state = remember(defaultValue) { mutableStateOf(defaultValue) }

    val activity = LocalActivity.current

    val insetsController = remember {
        activity?.let {
            WindowCompat.getInsetsController(it.window, it.window.decorView)
        }
    }
    DisposableEffect(state.value) {
        insetsController?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (state.value) {
            insetsController?.show(WindowInsetsCompat.Type.systemBars())
        } else {
            insetsController?.hide(WindowInsetsCompat.Type.systemBars())
        }

        onDispose { insetsController?.show(WindowInsetsCompat.Type.systemBars()) }
    }

    return state
}
