package com.programmersbox.kmpuiviews.utils

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.programmersbox.datastore.PlatformDataStoreHandling
import com.programmersbox.datastore.asState
import org.koin.compose.koinInject

actual class BiometricPrompting(
    private val context: Context,
    private val useStrongSecurity: Boolean,
    private val useDeviceCredentials: Boolean,
) {
    actual fun authenticate(
        onAuthenticationSucceeded: () -> Unit,
        onAuthenticationFailed: () -> Unit,
        title: String,
        subtitle: String,
        negativeButtonText: String,
    ) = authenticate(
        PromptCallback(
            onAuthenticationSucceeded = onAuthenticationSucceeded,
            onAuthenticationFailed = onAuthenticationFailed,
            title = title,
            subtitle = subtitle,
            negativeButtonText = negativeButtonText
        )
    )

    actual fun authenticate(promptInfo: PromptCallback) {
        var biometricStrength = if (useStrongSecurity)
            BiometricManager.Authenticators.BIOMETRIC_STRONG
        else
            BiometricManager.Authenticators.BIOMETRIC_WEAK

        if (useDeviceCredentials) biometricStrength = biometricStrength or BiometricManager.Authenticators.DEVICE_CREDENTIAL

        BiometricPrompt(
            context.findActivity(),
            context.mainExecutor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    promptInfo.onAuthenticationFailed()
                }

                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult,
                ) {
                    super.onAuthenticationSucceeded(result)
                    printLogs { "Authentication succeeded $result" }
                    promptInfo.onAuthenticationSucceeded()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    printLogs { "Authentication failed" }
                    promptInfo.onAuthenticationFailed()
                }
            }
        ).authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle(promptInfo.title)
                .setSubtitle(promptInfo.subtitle)
                .also {
                    if (!useDeviceCredentials) {
                        it.setNegativeButtonText(promptInfo.negativeButtonText)
                    }
                }
                .setAllowedAuthenticators(biometricStrength)
                .build()
        )
    }
}

@Composable
actual fun rememberBiometricPrompting(): BiometricPrompting {
    val context = LocalContext.current
    val platformDataStoreHandling = koinInject<PlatformDataStoreHandling>()
    val useStrongSecurity by platformDataStoreHandling.useStrongSecurity.asState()
    val useDeviceCredentials by platformDataStoreHandling.useDeviceCredentials.asState()

    val biometricPrompt = remember(
        context,
        useStrongSecurity,
        useDeviceCredentials
    ) {
        BiometricPrompting(
            context = context,
            useStrongSecurity = useStrongSecurity,
            useDeviceCredentials = useDeviceCredentials
        )
    }

    return biometricPrompt
}
