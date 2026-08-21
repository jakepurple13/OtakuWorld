package com.programmersbox.kmpuiviews.widget

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceComposable
import androidx.glance.GlanceTheme
import androidx.glance.material3.ColorProviders
import com.materialkolor.PaletteStyle
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.datastore.Settings
import org.koin.compose.koinInject

@Composable
fun WidgetTheme(
    settingsHandling: NewSettingsHandling = koinInject(),
    content: @GlanceComposable @Composable () -> Unit,
) {
    val settings by settingsHandling.preferences.data.collectAsState(Settings())
    val swatchStyle by rememberPreferenceForWidget(
        key = stringPreferencesKey("swatchStyle"),
        mapToType = { runCatching { PaletteStyle.valueOf(it) }.getOrDefault(PaletteStyle.TonalSpot) },
        mapToKey = { it.name },
        defaultValue = PaletteStyle.TonalSpot
    )

    val (light, dark) = createWidgetScheme(
        darkTheme = false,
        settings = settings,
        swatchStyle = swatchStyle
    ) to createWidgetScheme(
        darkTheme = true,
        settings = settings,
        swatchStyle = swatchStyle
    )

    GlanceTheme(
        ColorProviders(
            light = light,
            dark = dark
        ),
        content = content
    )
}