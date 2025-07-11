package com.programmersbox.kmpuiviews.presentation.settings.security

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.programmersbox.kmpuiviews.presentation.settings.SettingsScaffold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityScreen() {
    SettingsScaffold(
        title = "Security",
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        BiometricSettings()
    }
}

@Composable
expect fun ColumnScope.BiometricSettings()