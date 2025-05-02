package com.programmersbox.kmpuiviews

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.UriHandler
import androidx.navigation.NavHostController
import dev.jordond.connectivity.Connectivity
import io.kamel.core.config.KamelConfig
import org.koin.core.module.Module

expect fun platform(): String

@Composable
expect fun createColorScheme(
    darkTheme: Boolean,
    isExpressive: Boolean,
): ColorScheme

expect fun customUriHandler(navController: NavHostController): UriHandler

expect val databaseBuilder: Module

@Composable
expect fun customKamelConfig(): KamelConfig

expect class IconLoader {
    fun load(packageName: String): Any
}

expect class DateTimeFormatHandler {
    fun is24HourTime(): Boolean

    @Composable
    fun is24Time(): Boolean
}

expect fun createConnectivity(): Connectivity

expect fun recordFirebaseException(throwable: Throwable)

expect fun logFirebaseMessage(message: String)