package com.programmersbox.kmpuiviews.utils

import android.content.Context
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

actual class BiometricPrompting(
    private val context: Context,
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
                    println("Authentication succeeded $result")
                    promptInfo.onAuthenticationSucceeded()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    println("Authentication failed")
                    promptInfo.onAuthenticationFailed()
                }
            }
        ).authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle(promptInfo.title)
                .setSubtitle(promptInfo.subtitle)
                .setNegativeButtonText(promptInfo.negativeButtonText)
                .build()
        )
    }
}

@Composable
actual fun rememberBiometricPrompting(): BiometricPrompting {
    val context = LocalContext.current
    val biometricPrompt = remember(context) {
        BiometricPrompting(context)
    }
    return biometricPrompt
}
