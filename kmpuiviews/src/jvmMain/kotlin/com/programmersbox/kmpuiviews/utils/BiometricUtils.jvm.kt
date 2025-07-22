package com.programmersbox.kmpuiviews.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.runtime.remember
import com.tecknobit.biometrik.engines.BiometrikHandling
import com.tecknobit.biometrik.enums.AuthenticationResult

actual class BiometricPrompting {
    private val state = BiometrikHandling()

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
        when (state.requestAuth()) {
            AuthenticationResult.AUTHENTICATION_SUCCESS -> promptInfo.onAuthenticationSucceeded()
            else -> promptInfo.onAuthenticationFailed()
        }
    }
}

@Composable
actual fun rememberBiometricPrompting(): BiometricPrompting = remember { BiometricPrompting() }