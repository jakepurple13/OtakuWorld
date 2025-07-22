package com.programmersbox.kmpuiviews.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.runtime.remember

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
        promptInfo.onAuthenticationFailed()
    }
}

@Composable
actual fun rememberBiometricPrompting(): BiometricPrompting = remember { BiometricPrompting() }