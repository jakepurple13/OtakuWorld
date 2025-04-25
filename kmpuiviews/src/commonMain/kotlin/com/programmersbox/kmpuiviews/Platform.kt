package com.programmersbox.kmpuiviews

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.UriHandler
import androidx.navigation.NavHostController

expect fun platform(): String

@Composable
expect fun createColorScheme(
    darkTheme: Boolean,
    isExpressive: Boolean,
): ColorScheme

expect fun customUriHandler(navController: NavHostController): UriHandler