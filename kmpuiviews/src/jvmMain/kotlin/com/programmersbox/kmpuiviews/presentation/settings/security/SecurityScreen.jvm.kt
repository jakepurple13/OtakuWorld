package com.programmersbox.kmpuiviews.presentation.settings.security

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Text
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.state.ToggleableState
import com.programmersbox.kmpuiviews.presentation.components.settings.CategoryGroup
import com.programmersbox.kmpuiviews.presentation.components.settings.PreferenceSetting
import com.programmersbox.kmpuiviews.utils.rememberBiometricPrompting

@Composable
actual fun ColumnScope.BiometricSettings() {
    val biometricPrompting = rememberBiometricPrompting()

    CategoryGroup {
        item {
            var biometricTestingState by remember { mutableStateOf(BiometricTestingState.Idle) }

            PreferenceSetting(
                settingTitle = { Text("Test Biometrics") },
                endIcon = {
                    TriStateCheckbox(
                        state = when (biometricTestingState) {
                            BiometricTestingState.Idle -> ToggleableState.Indeterminate
                            BiometricTestingState.Failed -> ToggleableState.Off
                            BiometricTestingState.Success -> ToggleableState.On
                        },
                        onClick = {}
                    )
                },
                onClick = {
                    biometricPrompting.authenticate(
                        onAuthenticationSucceeded = { biometricTestingState = BiometricTestingState.Success },
                        onAuthenticationFailed = { biometricTestingState = BiometricTestingState.Failed },
                        title = "Testing",
                        subtitle = "Biometrics",
                        negativeButtonText = "Cancel"
                    )
                }
            )
        }
    }
}

private enum class BiometricTestingState {
    Idle, Failed, Success
}