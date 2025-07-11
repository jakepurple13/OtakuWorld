package com.programmersbox.kmpuiviews.presentation.settings.security

import android.annotation.SuppressLint
import android.app.KeyguardManager
import androidx.biometric.BiometricManager
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.state.ToggleableState
import androidx.core.content.getSystemService
import com.programmersbox.datastore.PlatformDataStoreHandling
import com.programmersbox.datastore.asState
import com.programmersbox.kmpuiviews.presentation.components.settings.CategoryGroup
import com.programmersbox.kmpuiviews.presentation.components.settings.PreferenceSetting
import com.programmersbox.kmpuiviews.presentation.components.settings.SwitchSetting
import com.programmersbox.kmpuiviews.presentation.components.settings.categorySetting
import com.programmersbox.kmpuiviews.utils.rememberBiometricPrompting
import org.koin.compose.koinInject

@SuppressLint("MissingPermission")
@Composable
actual fun ColumnScope.BiometricSettings() {
    val platformDataStoreHandling: PlatformDataStoreHandling = koinInject()
    var useStrongSecurity by platformDataStoreHandling.useStrongSecurity.asState()
    var useDeviceCredentials by platformDataStoreHandling.useDeviceCredentials.asState()

    val context = LocalContext.current
    val biometricManager = remember(context) { BiometricManager.from(context) }
    val keyguardManager = remember(context) { context.getSystemService<KeyguardManager>() }
    val isSecure = remember(context) { keyguardManager?.isDeviceSecure == true }
    val biometricPrompting = rememberBiometricPrompting()

    CategoryGroup {
        categorySetting { Text("Legend") }

        item {
            PreferenceSetting(
                settingTitle = { Text("This level of security is not set up") },
                settingIcon = { Icon(Icons.Default.Warning, null) }
            )
        }
    }

    CategoryGroup {
        item {
            val strongSecurityStrings = remember {
                biometricManager.getStrings(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            }

            SwitchSetting(
                value = useStrongSecurity,
                updateValue = { useStrongSecurity = it },
                settingIcon = {
                    if (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED) {
                        Icon(Icons.Default.Warning, null)
                    }
                },
                settingTitle = {
                    Text(
                        strongSecurityStrings
                            ?.settingName
                            ?.toString()
                            .orEmpty()
                    )
                },
                summaryValue = {
                    Text(
                        strongSecurityStrings
                            ?.promptMessage
                            ?.toString()
                            .orEmpty()
                    )
                }
            )
        }

        item {
            val credentialStrings = remember {
                biometricManager.getStrings(BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            }
            SwitchSetting(
                value = useDeviceCredentials,
                updateValue = { useDeviceCredentials = it },
                settingIcon = {
                    if (!isSecure) {
                        Icon(Icons.Default.Warning, null)
                    }
                },
                settingTitle = {
                    Text(
                        credentialStrings
                            ?.settingName
                            ?.toString()
                            .orEmpty()
                    )
                },
                summaryValue = {
                    Text(
                        credentialStrings
                            ?.promptMessage
                            ?.toString()
                            .orEmpty()
                    )
                }
            )
        }
    }

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