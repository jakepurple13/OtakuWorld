package com.programmersbox.kmpuiviews.utils

import androidx.compose.ui.graphics.Color
import com.programmersbox.datastore.ThemeColor

val ThemeColor.seedColor
    get() = when (this) {
        ThemeColor.Dynamic -> Color.Transparent
        ThemeColor.Blue -> Color.Blue
        ThemeColor.Red -> Color.Red
        ThemeColor.Green -> Color.Green
        ThemeColor.Yellow -> Color.Yellow
        ThemeColor.Cyan -> Color.Cyan
        ThemeColor.Magenta -> Color.Magenta
        ThemeColor.Custom -> Color.Transparent
    }

enum class ComponentState { Pressed, Released }
