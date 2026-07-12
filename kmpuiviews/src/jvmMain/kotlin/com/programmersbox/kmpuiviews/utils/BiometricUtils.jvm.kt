package com.programmersbox.kmpuiviews.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.runtime.remember
import eu.anifantakis.lib.ksafe.biometrics.KSafeBiometrics

actual class BiometricPrompting {

    actual fun authenticate(
        onAuthenticationSucceeded: () -> Unit,
        onAuthenticationFailed: () -> Unit,
        title: String,
        subtitle: String,
        negativeButtonText: String,
    ) {
        authenticate(
            PromptCallback(
                onAuthenticationSucceeded = onAuthenticationSucceeded,
                onAuthenticationFailed = onAuthenticationFailed,
                title = title,
                subtitle = subtitle,
                negativeButtonText = negativeButtonText,
            )
        )
    }

    @OptIn(ExperimentalComposeApi::class)
    actual fun authenticate(promptInfo: PromptCallback) {
        KSafeBiometrics.verifyBiometricDirect(
            reason = "${promptInfo.title}\n${promptInfo.subtitle}",
        ) {
            if (it)
                promptInfo.onAuthenticationSucceeded()
            else
                promptInfo.onAuthenticationFailed()
        }
    }
}

@Composable
actual fun rememberBiometricPrompting(): BiometricPrompting = remember { BiometricPrompting() }