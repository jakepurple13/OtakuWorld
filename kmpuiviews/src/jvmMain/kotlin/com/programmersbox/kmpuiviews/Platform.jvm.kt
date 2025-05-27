package com.programmersbox.kmpuiviews

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RememberMe
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.UriHandler
import androidx.navigation.NavHostController
import com.mikepenz.aboutlibraries.Libs
import com.programmersbox.favoritesdatabase.DatabaseBuilder
import io.github.vinceglb.filekit.PlatformFile
import io.kamel.core.ExperimentalKamelApi
import io.kamel.core.config.KamelConfig
import io.kamel.core.config.takeFrom
import io.kamel.image.config.Default
import io.kamel.image.config.animatedImageDecoder
import org.koin.core.module.Module
import org.koin.dsl.module
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Locale


actual fun platform(): String = "Desktop"

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

actual class CustomUriHandler : UriHandler {
    actual override fun openUri(uri: String) {
        TODO("Not yet implemented")
    }
}

actual fun customUriHandler(navController: NavHostController): UriHandler = object : UriHandler {
    override fun openUri(uri: String) {
        error("No Jvm implementation")
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
    actual fun is24HourTime(): Boolean {
        val df = SimpleDateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault())
        return if (df is SimpleDateFormat) {
            val sdf = df
            val pattern = sdf.toPattern()
            !pattern.contains("a")
        } else {
            true
        }
    }

    @Composable
    actual fun is24Time(): Boolean {
        return remember { is24HourTime() }
    }
}

actual fun recordFirebaseException(throwable: Throwable) {
    throwable.printStackTrace()
}

actual fun logFirebaseMessage(message: String) {
    println(message)
}

actual fun readPlatformFile(uri: String): PlatformFile = PlatformFile(uri)

@Composable
actual fun appVersion(): String = "1.0.0"

@Composable
actual fun versionCode(): String = "1"

@Composable
actual fun Modifier.zoomOverlay(): Modifier = this

@Composable
actual fun painterLogo(): Painter = rememberVectorPainter(Icons.Default.RememberMe)
actual class AboutLibraryBuilder {
    @Composable
    actual fun buildLibs(): State<Libs?> = mutableStateOf(null)
}