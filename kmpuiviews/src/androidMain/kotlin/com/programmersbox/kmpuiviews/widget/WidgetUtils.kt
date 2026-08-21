package com.programmersbox.kmpuiviews.widget

import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.expressiveLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Color
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.glance.LocalContext
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.ktx.animateColorScheme
import com.materialkolor.rememberDynamicColorScheme
import com.programmersbox.datastore.Settings
import com.programmersbox.datastore.ThemeColor
import com.programmersbox.datastore.otakuDataStore
import com.programmersbox.kmpuiviews.utils.seedColor
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch

//TODO: Widget ideas
// Maybe favorite count with a small chart?
// Maybe read time count?

@Composable
fun <T, R> rememberPreferenceForWidget(
    key: Preferences.Key<T>,
    mapToType: (T) -> R?,
    mapToKey: (R) -> T,
    defaultValue: R,
): MutableState<R> {
    val coroutineScope = rememberCoroutineScope()
    val state by remember {
        otakuDataStore
            .data
            .mapNotNull { it[key]?.let(mapToType) ?: defaultValue }
            .distinctUntilChanged()
    }.collectAsState(defaultValue)

    return remember(state) {
        object : MutableState<R> {
            override var value: R
                get() = state
                set(value) {
                    coroutineScope.launch {
                        otakuDataStore.edit { it[key] = value.let(mapToKey) }
                    }
                }

            override fun component1() = value
            override fun component2(): (R) -> Unit = { value = it }
        }
    }
}

@Composable
private fun createColorScheme(
    darkTheme: Boolean,
    isExpressive: Boolean,
): ColorScheme {
    val context = LocalContext.current
    return remember(context, darkTheme, isExpressive) {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && darkTheme -> dynamicDarkColorScheme(context)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !darkTheme -> dynamicLightColorScheme(context)
            darkTheme -> darkColorScheme(
                primary = Color(0xff90CAF9),
                secondary = Color(0xff90CAF9)
            )

            isExpressive -> expressiveLightColorScheme()

            else -> lightColorScheme()
        }
    }
}

@Composable
fun createWidgetScheme(
    darkTheme: Boolean,
    settings: Settings,
    swatchStyle: PaletteStyle,
): ColorScheme {
    val isAmoledMode = settings.amoledMode
    val isExpressive = settings.showExpressiveness
    val themeColor = settings.themeColor

    val colorScheme = if (themeColor == ThemeColor.Dynamic) {
        createColorScheme(darkTheme, isExpressive).let {
            if (isAmoledMode && darkTheme) {
                it.copy(
                    surface = Color.Black,
                    onSurface = Color.White,
                    background = Color.Black,
                    onBackground = Color.White,
                )
            } else {
                it
            }
        }
    } else {
        rememberDynamicColorScheme(
            seedColor = themeColor.seedColor,
            isAmoled = isAmoledMode,
            isDark = darkTheme,
            style = swatchStyle,
            specVersion = ColorSpec.SpecVersion.SPEC_2025
        )
    }

    return animateColorScheme(remember(isAmoledMode, isExpressive, themeColor, colorScheme) { colorScheme })
}