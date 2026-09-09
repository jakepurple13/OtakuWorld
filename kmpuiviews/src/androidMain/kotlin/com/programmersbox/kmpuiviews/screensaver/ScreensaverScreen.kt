package com.programmersbox.kmpuiviews.screensaver

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun ScreensaverScreen() {
    Scaffold { padding ->
        Text(
            "Hello",
            modifier = Modifier.padding(padding)
        )
    }
}