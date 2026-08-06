package com.programmersbox.showcase

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.expressiveLightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.materialkolor.dynamicColorScheme
import com.materialkolor.ktx.animateColorScheme
import com.programmersbox.kmpuiviews.CustomTitleBar
import com.programmersbox.kmpuiviews.utils.LocalNavHostPadding

fun main() = application {
    val windowState = rememberWindowState()

    Window(
        onCloseRequest = ::exitApplication,
        title = "Component Showcase",
        state = windowState,
        undecorated = true,
        transparent = true,
    ) {
        val isDarkMode = isSystemInDarkTheme()
        var themeMode by remember { mutableStateOf(isDarkMode) }
        val colorScheme by remember(themeMode) {
            derivedStateOf {
                if (themeMode) dynamicColorScheme(Color.Cyan, isDark = true)
                else expressiveLightColorScheme()
            }
        }
        MaterialTheme(
            colorScheme = animateColorScheme(colorScheme),
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant
                )
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    CustomTitleBar(
                        title = "Component Showcase",
                        onMinimizeClick = { windowState.isMinimized = true },
                        onMaximizeToggle = {
                            windowState.placement = if (windowState.placement == WindowPlacement.Maximized) {
                                WindowPlacement.Floating
                            } else {
                                WindowPlacement.Maximized
                            }
                        },
                        onCloseClick = ::exitApplication
                    )
                    HorizontalDivider()
                    CompositionLocalProvider(
                        LocalNavHostPadding provides PaddingValues()
                    ) {
                        App(
                            themeMode = themeMode,
                            onThemeModeChange = { themeMode = it },
                        )
                    }
                }
            }
        }
    }
}
