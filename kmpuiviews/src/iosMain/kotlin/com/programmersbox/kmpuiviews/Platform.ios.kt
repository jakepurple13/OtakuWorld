package com.programmersbox.kmpuiviews

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.UriHandler
import androidx.navigation.NavHostController

actual fun platform() = "iOS"

@Composable
actual fun createColorScheme(darkTheme: Boolean, isExpressive: Boolean): ColorScheme {
    return when {
        darkTheme -> darkColorScheme(
            primary = Color(0xff90CAF9),
            secondary = Color(0xff90CAF9)
        )

        isExpressive -> lightColorScheme()//expressiveLightColorScheme()

        else -> lightColorScheme()
    }
}

actual fun customUriHandler(navController: NavHostController): UriHandler = object : UriHandler {
    override fun openUri(uri: String) {
        error("No iOS implementation")
    }
}