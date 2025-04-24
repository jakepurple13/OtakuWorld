package com.programmersbox.kmpuiviews.theme

import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import com.materialkolor.ktx.animateColorScheme
import com.materialkolor.rememberDynamicColorScheme
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.datastore.SystemThemeMode
import com.programmersbox.datastore.ThemeColor
import com.programmersbox.datastore.rememberSwatchStyle
import com.programmersbox.kmpuiviews.createColorScheme
import com.programmersbox.kmpuiviews.utils.seedColor

@Composable
fun generateColorScheme(
    settingsHandling: NewSettingsHandling,
): ColorScheme {
    val isAmoledMode by settingsHandling.rememberIsAmoledMode()
    val isExpressive by settingsHandling.rememberShowExpressiveness()
    val themeSetting by settingsHandling.rememberSystemThemeMode()
    val themeColor by settingsHandling.rememberThemeColor()
    val darkTheme = when (themeSetting) {
        SystemThemeMode.FollowSystem -> isSystemInDarkTheme()
        SystemThemeMode.Day -> false
        SystemThemeMode.Night -> true
    }

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
        val swatchStyle by rememberSwatchStyle()

        rememberDynamicColorScheme(
            seedColor = themeColor.seedColor,
            isAmoled = isAmoledMode,
            isDark = darkTheme,
            style = swatchStyle,
        )
    }

    return animateColorScheme(colorScheme, tween())
}