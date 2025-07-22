package com.programmersbox.manga.shared.reader

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

@Composable
actual fun insetsController(defaultValue: Boolean): MutableState<Boolean> = remember { mutableStateOf(defaultValue) }