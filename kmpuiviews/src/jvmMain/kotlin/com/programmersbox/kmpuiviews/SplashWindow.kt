package com.programmersbox.kmpuiviews

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import com.programmersbox.kmpuiviews.theme.OtakuMaterialTheme
import org.koin.compose.koinInject

/**
 * Shown while the main window is being built. Opening it also closes the native JVM splash
 * (`-splash:`), since AWT hides that as soon as the first window becomes visible.
 */
@Composable
fun SplashWindow(
    title: String,
    onCloseRequest: () -> Unit,
    onShown: () -> Unit,
) {
    Window(
        onCloseRequest = onCloseRequest,
        title = title,
        state = rememberWindowState(
            size = DpSize(360.dp, 300.dp),
            position = WindowPosition(Alignment.Center),
        ),
        undecorated = true,
        transparent = true,
        resizable = false,
        icon = painterLogo(),
    ) {
        LaunchedEffect(Unit) {
            // Wait for the splash to draw before the main window starts composing.
            withFrameNanos { }
            onShown()
        }

        OtakuMaterialTheme(
            settingsHandling = koinInject(),
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant
                )
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                ) {
                    Image(
                        painter = painterLogo(),
                        contentDescription = null,
                        modifier = Modifier
                            .size(112.dp)
                            .clip(MaterialTheme.shapes.large),
                    )
                    Text(
                        title,
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}
