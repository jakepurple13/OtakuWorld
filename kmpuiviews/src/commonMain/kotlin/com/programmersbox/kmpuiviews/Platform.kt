package com.programmersbox.kmpuiviews

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.UriHandler
import androidx.navigation.NavHostController
import com.mikepenz.aboutlibraries.Libs
import com.programmersbox.kmpmodels.KmpSourceInformation
import dev.jordond.connectivity.Connectivity
import io.github.vinceglb.filekit.PlatformFile
import io.kamel.core.config.KamelConfig
import org.koin.core.module.Module

expect fun platform(): String

@Composable
expect fun createColorScheme(
    darkTheme: Boolean,
    isExpressive: Boolean,
): ColorScheme

expect fun customUriHandler(navController: NavHostController): UriHandler

@Composable
expect fun rememberCustomUriHandler(): CustomUriHandler

expect class CustomUriHandler : UriHandler {
    override fun openUri(uri: String)
}

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

expect fun readPlatformFile(uri: String): PlatformFile

@Composable
expect fun versionCode(): String

@Composable
expect fun appVersion(): String

@Composable
expect fun painterLogo(): Painter

expect class AboutLibraryBuilder {
    @Composable
    fun buildLibs(): State<Libs?>
}

@Composable
expect fun Modifier.zoomOverlay(): Modifier

@Composable
expect fun HideScreen(shouldHide: Boolean)

@Composable
expect fun InitialSetup()

@Composable
expect fun SourceIcon(iconLoader: IconLoader, sourceInfo: KmpSourceInformation)