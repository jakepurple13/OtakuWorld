package com.programmersbox.kmpuiviews

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RememberMe
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.expressiveLightColorScheme
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
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.navigation.NavHostController
import ca.gosyer.appdirs.AppDirs
import com.mikepenz.aboutlibraries.Libs
import com.programmersbox.datastore.DataStoreHandler
import com.programmersbox.favoritesdatabase.DatabaseBuilder
import com.programmersbox.kmpmodels.KmpSourceInformation
import com.programmersbox.kmpuiviews.domain.KmpCustomRemoteModel
import com.programmersbox.kmpuiviews.domain.TranslationHandler
import com.programmersbox.kmpuiviews.domain.TranslationModelHandler
import io.github.vinceglb.filekit.PlatformFile
import io.kamel.core.ExperimentalKamelApi
import io.kamel.core.config.KamelConfig
import io.kamel.core.config.takeFrom
import io.kamel.image.config.Default
import io.kamel.image.config.animatedImageDecoder
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import java.io.File
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Locale


actual fun platform(): String = "Desktop"

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
actual fun createColorScheme(darkTheme: Boolean, isExpressive: Boolean): ColorScheme {
    return remember(darkTheme, isExpressive) {
        when {
            darkTheme -> darkColorScheme(
                primary = Color(0xff90CAF9),
                secondary = Color(0xff90CAF9)
            )

            isExpressive -> expressiveLightColorScheme()

            else -> lightColorScheme()
        }
    }
}

actual class CustomUriHandler : UriHandler {
    actual override fun openUri(uri: String) {

    }
}

actual fun customUriHandler(navController: NavHostController): UriHandler = object : UriHandler {
    override fun openUri(uri: String) {
        error("No Jvm implementation")
    }
}

actual val databaseBuilder: Module = module {
    singleOf(::DatabaseBuilder)
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
            val pattern = df.toPattern()
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
actual fun appVersion(): String = BuildKonfig.VERSION_NAME_KMP

@Composable
actual fun versionCode(): String = BuildKonfig.VERSION_CODE_KMP

@Composable
actual fun Modifier.zoomOverlay(): Modifier = this

@Composable
actual fun painterLogo(): Painter = rememberVectorPainter(Icons.Default.RememberMe)
actual class AboutLibraryBuilder {
    @Composable
    actual fun buildLibs(): State<Libs?> = mutableStateOf(null)
}

@Composable
actual fun ScrollBar(lazyListState: LazyListState) {
    VerticalScrollbar(
        adapter = rememberScrollbarAdapter(lazyListState),
    )
}

@Composable
actual fun rememberCustomUriHandler(): CustomUriHandler {
    return remember { CustomUriHandler() }
}

@Composable
actual fun SourceIcon(iconLoader: IconLoader, sourceInfo: KmpSourceInformation) {
}

@Composable
actual fun InitialSetup() {
}

@Composable
actual fun HideScreen(shouldHide: Boolean) {
}

actual fun analyticsScreen(screenName: String) {
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

class TranslationItemHandler : TranslationHandler {
    override fun translateDescription(
        textToTranslate: String,
        progress: (Boolean) -> Unit,
        translatedText: (String) -> Unit,
    ) {
        translatedText(textToTranslate)
    }

    override suspend fun translate(textToTranslate: String): String = textToTranslate

    override fun clear() {
    }
}

class TranslationModelHandlerImpl : TranslationModelHandler {
    override fun getModels(onSuccess: (List<KmpCustomRemoteModel>) -> Unit) {
        onSuccess(emptyList())
    }

    override suspend fun deleteModel(model: KmpCustomRemoteModel) {

    }

    override suspend fun modelList(): List<KmpCustomRemoteModel> = emptyList()

    override suspend fun delete(model: KmpCustomRemoteModel) {

    }
}

class MangaDesktopSettings(
    appDirs: AppDirs,
) {
    val extensionDirectory = DataStoreHandler(
        key = stringPreferencesKey("extensionDirectory"),
        defaultValue = File(appDirs.getUserDataDir(), "extensions").absolutePath
    )

    val useWebViewForReader = DataStoreHandler(
        key = booleanPreferencesKey("useWebViewForReader"),
        defaultValue = false
    )
}