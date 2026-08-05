package com.programmersbox.showcase.annotations

import androidx.compose.runtime.Composable

data class ShowcaseEntry(
    val name: String,
    val description: String,
    val group: String,
    val content: @Composable () -> Unit,
)
