package com.programmersbox.kmpuiviews

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.text.format.DateFormat
import android.view.WindowManager
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.expressiveLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.navigation.NavHostController
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.analytics.logEvent
import com.google.firebase.crashlytics.crashlytics
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.util.withContext
import com.programmersbox.favoritesdatabase.DatabaseBuilder
import com.programmersbox.kmpmodels.KmpSourceInformation
import com.programmersbox.kmpuiviews.utils.AppLogo
import com.programmersbox.kmpuiviews.utils.navigateChromeCustomTabs
import com.programmersbox.kmpuiviews.utils.openInCustomChromeBrowser
import com.programmersbox.kmpuiviews.utils.printLogs
import io.github.vinceglb.filekit.PlatformFile
import io.kamel.core.ExperimentalKamelApi
import io.kamel.core.config.KamelConfig
import io.kamel.core.config.takeFrom
import io.kamel.image.config.Default
import io.kamel.image.config.animatedImageDecoder
import io.kamel.image.config.imageBitmapResizingDecoder
import io.kamel.image.config.resourcesFetcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.saket.telephoto.zoomable.rememberZoomablePeekOverlayState
import me.saket.telephoto.zoomable.zoomablePeekOverlay
import my.nanihadesuka.compose.InternalLazyColumnScrollbar
import my.nanihadesuka.compose.ScrollbarSettings
import org.koin.compose.koinInject
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platform() = "Android"

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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

        isExpressive -> expressiveLightColorScheme()

        else -> lightColorScheme()
    }
}

actual class CustomUriHandler(
    private val context: Context,
) : UriHandler {
    actual override fun openUri(uri: String) {
        context.openInCustomChromeBrowser(uri.toUri())
    }
}

@Composable
actual fun rememberCustomUriHandler(): CustomUriHandler {
    val context = LocalContext.current
    return remember(context) { CustomUriHandler(context) }
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
    runCatching { Firebase.crashlytics.recordException(throwable) }
}

actual fun logFirebaseMessage(message: String) {
    runCatching {
        printLogs { message }
        Firebase.crashlytics.log(message)
    }.onFailure { printLogs { message } }
}

actual fun analyticsScreen(screenName: String) {
    runCatching {
        Firebase.analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
            param(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
        }
    }
}

actual fun readPlatformFile(uri: String): PlatformFile = PlatformFile(uri.toUri())

@Composable
actual fun painterLogo(): Painter = rememberDrawablePainter(drawable = koinInject<AppLogo>().logo)

val Context.appVersion: String
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.getPackageInfo(
            packageName,
            PackageManager.PackageInfoFlags.of(0L)
        ).versionName
    } else {
        packageManager.getPackageInfo(packageName, 0)?.versionName
    }.orEmpty()

val Context.versionCode: String
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.getPackageInfo(
            packageName,
            PackageManager.PackageInfoFlags.of(0L)
        ).longVersionCode
    } else {
        packageManager.getPackageInfo(packageName, 0)?.longVersionCode
    }
        ?.toString()
        .orEmpty()

@Composable
actual fun appVersion(): String {
    return if (LocalInspectionMode.current) {
        "1.0.0"
    } else {
        val context = LocalContext.current
        remember(context) { context.appVersion }
    }
}

@Composable
actual fun versionCode(): String {
    return if (LocalInspectionMode.current) {
        "1"
    } else {
        val context = LocalContext.current
        remember(context) { context.versionCode }
    }
}

actual class AboutLibraryBuilder {

    @Composable
    actual fun buildLibs(): State<Libs?> = libraryList()

    @Composable
    fun libraryList(librariesBlock: (Context) -> Libs = { context -> Libs.Builder().withContext(context).build() }): State<Libs?> {
        val context = LocalContext.current
        return produceState<Libs?>(null) {
            value = withContext(Dispatchers.IO) {
                librariesBlock(context)
            }
        }
    }
}

@Composable
actual fun Modifier.zoomOverlay(): Modifier = zoomablePeekOverlay(state = rememberZoomablePeekOverlayState())

@Composable
actual fun HideScreen(shouldHide: Boolean) {
    val window = LocalActivity.current

    DisposableEffect(shouldHide) {
        if (shouldHide) {
            window?.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
        onDispose { window?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
    }
}

@Composable
actual fun InitialSetup() {
    AskForNotificationPermissions()
}

@Composable
fun AskForNotificationPermissions() {
    if (Build.VERSION.SDK_INT >= 33) {
        val permissionRequest = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
        LaunchedEffect(Unit) { permissionRequest.launch(android.Manifest.permission.POST_NOTIFICATIONS) }
    }
}

@Composable
actual fun SourceIcon(iconLoader: IconLoader, sourceInfo: KmpSourceInformation) {
    (iconLoader.load(sourceInfo.packageName) as? Drawable)?.let {
        Image(
            rememberDrawablePainter(drawable = it),
            contentDescription = null,
            modifier = Modifier
                .clip(CircleShape)
                .size(64.dp)
        )
    }
}

@Composable
actual fun ScrollBar(lazyListState: LazyListState) {
    InternalLazyColumnScrollbar(
        state = lazyListState,
        settings = ScrollbarSettings.Default.copy(
            thumbThickness = 8.dp,
            scrollbarPadding = 2.dp,
            thumbUnselectedColor = MaterialTheme.colorScheme.primary,
            thumbSelectedColor = MaterialTheme.colorScheme.primary.copy(alpha = .6f),
        ),
    )
}