package com.programmersbox.kmpuiviews.utils

import androidx.compose.runtime.Composable

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
    }
}

@Composable
actual fun rememberBiometricPrompting(): BiometricPrompting {
    TODO("Not yet implemented")
}