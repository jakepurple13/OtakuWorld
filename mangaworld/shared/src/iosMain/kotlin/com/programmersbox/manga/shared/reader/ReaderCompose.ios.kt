package com.programmersbox.manga.shared.reader

import androidx.compose.runtime.mutableStateOf

@androidx.compose.runtime.Composable
actual fun insetsController(defaultValue: Boolean): androidx.compose.runtime.MutableState<Boolean> = mutableStateOf(defaultValue)