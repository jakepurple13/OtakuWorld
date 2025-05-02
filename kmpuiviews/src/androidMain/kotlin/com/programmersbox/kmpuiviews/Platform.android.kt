package com.programmersbox.kmpuiviews

import android.content.Context
import android.os.Build
import android.text.format.DateFormat
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.UriHandler
import androidx.navigation.NavHostController
import com.programmersbox.favoritesdatabase.DatabaseBuilder
import com.programmersbox.kmpuiviews.utils.navigateChromeCustomTabs
import io.kamel.core.ExperimentalKamelApi
import io.kamel.core.config.KamelConfig
import io.kamel.core.config.takeFrom
import io.kamel.image.config.Default
import io.kamel.image.config.animatedImageDecoder
import io.kamel.image.config.imageBitmapResizingDecoder
import io.kamel.image.config.resourcesFetcher
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platform() = "Android"

@Composable
actual fun createColorScheme(
    darkTheme: Boolean,
    isExpressive: Boolean,
): ColorScheme {
    return when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && darkTheme -> dynamicDarkColorScheme(LocalContext.current)
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !darkTheme -> dynamicLightColorScheme(LocalContext.current)
        darkTheme -> darkColorScheme(
            primary = Color(0xff90CAF9),
            secondary = Color(0xff90CAF9)
        )

        //TODO: On next cmp update, add expressive color scheme
        isExpressive -> lightColorScheme()//expressiveLightColorScheme()

        else -> lightColorScheme()
    }
}

actual fun customUriHandler(navController: NavHostController): UriHandler = object : UriHandler {
    override fun openUri(uri: String) {
        navController.navigateChromeCustomTabs(
            url = uri,
            builder = {
                anim {
                    enter = R.anim.slide_in_right
                    popEnter = R.anim.slide_in_right
                    exit = R.anim.slide_out_left
                    popExit = R.anim.slide_out_left
                }
            }
        )
    }
}

actual val databaseBuilder: Module = module {
    single { DatabaseBuilder(get()) }
}

@OptIn(ExperimentalKamelApi::class)
@Composable
actual fun customKamelConfig(): KamelConfig {
    val context = LocalContext.current
    return KamelConfig {
        takeFrom(KamelConfig.Default)
        imageBitmapResizingDecoder()
        animatedImageDecoder()
        resourcesFetcher(context)
    }
}

actual class IconLoader(
    context: Context,
) {
    private val packageManager by lazy { context.packageManager }

    actual fun load(packageName: String): Any {
        return packageManager.getApplicationIcon(packageName)
    }
}

actual class DateTimeFormatHandler(private val context: Context) {
    actual fun is24HourTime() = DateFormat.is24HourFormat(context)

    @Composable
    actual fun is24Time(): Boolean {
        LocalConfiguration.current
        return DateFormat.is24HourFormat(LocalContext.current)
    }
}

actual fun recordFirebaseException(throwable: Throwable) {
    runCatching {
        Firebase.crashlytics.recordException(throwable)
    }
}

actual fun logFirebaseMessage(message: String) {
    runCatching {
        println(message)
        Firebase.crashlytics.log(message)
    }.onFailure { println(message) }
}