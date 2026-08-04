package com.programmersbox.kmpuiviews

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.expressiveLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.UriHandler
import androidx.navigation.NavHostController
import com.mikepenz.aboutlibraries.Libs
import com.programmersbox.favoritesdatabase.DatabaseBuilder
import com.programmersbox.kmpmodels.KmpSourceInformation
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

@Composable
actual fun ReportDrawnWhen(predicate: () -> Boolean) {
    // No-op on iOS — TTFD tracking is Android-only
}

/*
fun provideBiometricAuthenticator(): BiometricAuthenticator {
    val osName = System.getProperty("os.name").toLowerCase()
    return when {
        osName.contains("windows") -> WindowsBiometricAuthenticator() // Assuming you'll create this
        osName.contains("mac os x") || osName.contains("darwin") -> MacOSBiometricAuthenticator()
        osName.contains("linux") -> LinuxBiometricAuthenticator() // Assuming you'll create this
        else -> object : BiometricAuthenticator {
            override val isBiometricAvailable: Boolean = false
            override suspend fun authenticate(reason: String): Boolean = false
        }
    }
}

 class MacOSBiometricAuthenticator {
    val isBiometricAvailable: Boolean
        get() {
            val context = LAContext()
            var error: NSError? = null
            val canEvaluate = context.canEvaluatePolicy(
                LAPolicyDeviceOwnerAuthenticationWithBiometrics,
                error = error.ptr
            )
            error?.let { println("Error checking biometric availability: $it") }
            return canEvaluate
        }

    suspend fun authenticate(reason: String): Boolean = suspendCoroutine { continuation ->
        val context = LAContext()
        val nsReason = NSString.create(string = reason)

        context.evaluatePolicy(
            policy = LAPolicyDeviceOwnerAuthenticationWithBiometrics,
            localizedReason = nsReason,
            reply = object : (Boolean, NSError?) -> Unit {
                override fun invoke(success: Boolean, error: NSError?) {
                    if (success) {
                        continuation.resume(true)
                    } else {
                        error?.let {
                            println("Biometric authentication failed: $it")
                            continuation.resume(false)
                        } ?: run {
                            println("Biometric authentication failed for an unknown reason.")
                            continuation.resume(false)
                        }
                    }
                }
            }
        )
    }
}*/
@Composable
actual fun rememberCustomUriHandler(): CustomUriHandler = CustomUriHandler()
actual fun analyticsScreen(screenName: String) {
}

@Composable
actual fun versionCode(): String = BuildKonfig.VERSION_CODE_KMP

@Composable
actual fun appVersion(): String = BuildKonfig.VERSION_NAME_KMP

@Composable
actual fun painterLogo(): Painter = rememberVectorPainter(Icons.Default.AcUnit)
actual class AboutLibraryBuilder {
    @Composable
    actual fun buildLibs(): State<Libs?> = mutableStateOf(null)
}

@Composable
actual fun Modifier.zoomOverlay(): Modifier = Modifier

@Composable
actual fun HideScreen(shouldHide: Boolean) {
}

@Composable
actual fun InitialSetup() {
}

@Composable
actual fun SourceIcon(iconLoader: IconLoader, sourceInfo: KmpSourceInformation) {
}

@Composable
actual fun ScrollBar(lazyListState: LazyListState) {
}

actual class SystemAlerter {
    actual fun alertFavoritesChange() {
    }

    actual fun alertChapterChange() {
    }

    actual fun alertListChange() {
    }

    actual fun alertListItemChange() {
    }

    actual fun alertIncognitoChange() {
    }
}