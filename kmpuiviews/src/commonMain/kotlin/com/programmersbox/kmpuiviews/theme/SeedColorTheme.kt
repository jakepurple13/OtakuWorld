package com.programmersbox.kmpuiviews.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.Color
import com.materialkolor.PaletteStyle
import com.materialkolor.material3.dynamicColorScheme
import com.programmersbox.datastore.SystemThemeMode
import com.programmersbox.datastore.rememberSwatchStyle
import com.programmersbox.kmpuiviews.utils.LocalSettingsHandling
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** What a cover-seeded color scheme is built from */
internal data class SchemeInputs(
    val seedColor: Color,
    val isDark: Boolean,
    val isAmoled: Boolean,
    val style: PaletteStyle,
)

/**
 * Holds the last cover-seeded scheme Details built, so a reader opened from Details can show it on
 * its first frame instead of rebuilding it (hundreds of ms on low-end devices)
 */
internal object SeedSchemeCache {
    private var last: Pair<SchemeInputs, ColorScheme>? = null

    fun get(inputs: SchemeInputs): ColorScheme? = last?.takeIf { it.first == inputs }?.second

    fun put(inputs: SchemeInputs, scheme: ColorScheme) {
        last = inputs to scheme
    }
}

/**
 * Themes [content] from the cover color handed over by Details, using the same settings Details does.
 * With no [seedColor] the app theme is used unchanged.
 */
@Composable
fun SeedColorTheme(
    seedColor: Int?,
    content: @Composable () -> Unit,
) {
    if (seedColor == null) {
        content()
        return
    }

    val handling = LocalSettingsHandling.current
    val themeSetting by handling.rememberSystemThemeMode()
    val paletteStyle by rememberSwatchStyle()
    val isDark = when (themeSetting) {
        SystemThemeMode.FollowSystem -> isSystemInDarkTheme()
        SystemThemeMode.Day -> false
        SystemThemeMode.Night -> true
    }
    // Details turns AMOLED off whenever a cover palette is in use, so a seeded scheme never uses it
    val inputs = SchemeInputs(Color(seedColor), isDark, isAmoled = false, style = paletteStyle)

    val cached = SeedSchemeCache.get(inputs)
    val colorScheme by produceState(cached ?: MaterialTheme.colorScheme, inputs) {
        if (cached != null) {
            value = cached
            return@produceState
        }
        value = withContext(Dispatchers.Default) {
            dynamicColorScheme(
                seedColor = inputs.seedColor,
                isDark = inputs.isDark,
                isAmoled = inputs.isAmoled,
                style = inputs.style
            )
        }
        SeedSchemeCache.put(inputs, value)
    }

    MaterialTheme(colorScheme = colorScheme, content = content)
}
