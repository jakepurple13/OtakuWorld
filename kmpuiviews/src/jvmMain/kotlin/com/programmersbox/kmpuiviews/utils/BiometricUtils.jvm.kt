package com.programmersbox.kmpuiviews.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

actual class BiometricPrompting {
    actual fun authenticate(
        onAuthenticationSucceeded: () -> Unit,
        onAuthenticationFailed: () -> Unit,
        title: String,
        subtitle: String,
        negativeButtonText: String,
    ) {

    }

    actual fun authenticate(promptInfo: PromptCallback) {
        promptInfo.onAuthenticationSucceeded()
    }
}

@Composable
actual fun rememberBiometricPrompting(): BiometricPrompting = remember { BiometricPrompting() }