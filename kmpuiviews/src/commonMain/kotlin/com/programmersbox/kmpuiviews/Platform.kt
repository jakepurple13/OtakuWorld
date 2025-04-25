package com.programmersbox.kmpuiviews

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.UriHandler
import androidx.navigation.NavHostController
import org.koin.core.module.Module

expect fun platform(): String

@Composable
expect fun createColorScheme(
    darkTheme: Boolean,
    isExpressive: Boolean,
): ColorScheme

expect fun customUriHandler(navController: NavHostController): UriHandler

expect val databaseBuilder: Module

/*
val format = LocalDateTime.Format {
    monthName(MonthNames.ENGLISH_FULL)
    char(' ')
    dayOfMonth()
    char(' ')
    year()
    chars(", ")
    if (isUsing24HourTime) {
        hour()
        char(':')
        minute()
    } else {
        amPmHour()
        char(':')
        minute()
        char(' ')
        amPmMarker("AM", "PM")
    }
}*/
