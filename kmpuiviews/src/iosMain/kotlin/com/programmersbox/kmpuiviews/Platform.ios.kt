package com.programmersbox.kmpuiviews

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.expressiveLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.UriHandler
import androidx.navigation.NavHostController
import com.programmersbox.favoritesdatabase.DatabaseBuilder
import io.github.vinceglb.filekit.PlatformFile
import io.kamel.core.ExperimentalKamelApi
import io.kamel.core.config.KamelConfig
import io.kamel.core.config.takeFrom
import io.kamel.image.config.Default
import io.kamel.image.config.animatedImageDecoder
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.UIKit.UIDevice

actual fun platform() = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
actual fun createColorScheme(darkTheme: Boolean, isExpressive: Boolean): ColorScheme {
    return when {
        darkTheme -> darkColorScheme(
            primary = Color(0xff90CAF9),
            secondary = Color(0xff90CAF9)
        )

        isExpressive -> expressiveLightColorScheme()

        else -> lightColorScheme()
    }
}

actual class CustomUriHandler : UriHandler {
    actual override fun openUri(uri: String) {
        TODO("Not yet implemented")
    }
}

actual fun customUriHandler(navController: NavHostController): UriHandler = object : UriHandler {
    override fun openUri(uri: String) {
        error("No iOS implementation")
    }
}

actual val databaseBuilder: Module = module {
    single { DatabaseBuilder() }
}

@OptIn(ExperimentalKamelApi::class)
@Composable
actual fun customKamelConfig(): KamelConfig {
    return KamelConfig {
        takeFrom(KamelConfig.Default)
        animatedImageDecoder()
    }
}

actual class IconLoader {
    actual fun load(packageName: String): Any {
        return ""
    }
}

actual class DateTimeFormatHandler {
    actual fun is24HourTime() = true

    @Composable
    actual fun is24Time(): Boolean {
        return true
    }
}

actual fun recordFirebaseException(throwable: Throwable) {
    throwable.printStackTrace()
}

actual fun logFirebaseMessage(message: String) {
    println(message)
}

actual fun readPlatformFile(uri: String): PlatformFile = PlatformFile(uri)
