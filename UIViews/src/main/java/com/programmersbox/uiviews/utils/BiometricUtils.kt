package com.programmersbox.uiviews.utils

import android.content.Context
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

fun biometricPrompting(
    context: Context,
    authenticationCallback: BiometricPrompt.AuthenticationCallback,
) = BiometricPrompt(
    context.findActivity(),
    context.mainExecutor,
    authenticationCallback
)

@Composable
fun rememberBiometricPrompt(
    onAuthenticationSucceeded: () -> Unit,
    onAuthenticationFailed: () -> Unit = {},
): BiometricPrompt.AuthenticationCallback {
    val succeed by rememberUpdatedState(onAuthenticationSucceeded)
    val failed by rememberUpdatedState(onAuthenticationFailed)
    return remember(succeed, failed) {
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                failed()
            }

            override fun onAuthenticationSucceeded(
                result: BiometricPrompt.AuthenticationResult,
            ) {
                super.onAuthenticationSucceeded(result)
                println("Authentication succeeded $result")
                succeed()
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                println("Authentication failed")
                failed()
            }
        }
    }
}