package com.programmersbox.otakuworld

import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity

class BiometricPrompting(
    private val context: Context,
    private val useStrongSecurity: Boolean = true,
    private val useDeviceCredentials: Boolean = true,
) {
    fun authenticate(
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

    fun authenticate(promptInfo: PromptCallback) {
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
                    promptInfo.onAuthenticationSucceeded()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
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
fun rememberBiometricPrompting(): BiometricPrompting {
    val context = LocalContext.current

    val biometricPrompt = remember(
        context
    ) {
        BiometricPrompting(
            context = context,
        )
    }

    return biometricPrompt
}

data class PromptCallback(
    val onAuthenticationSucceeded: () -> Unit,
    val onAuthenticationFailed: () -> Unit = {},
    val title: String = "Authentication required",
    val subtitle: String = "Please Authenticate",
    val negativeButtonText: String = "Never Mind",
)

tailrec fun Context.findActivity(): FragmentActivity = when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> this.baseContext.findActivity()
    else -> error("Could not find activity in Context chain.")
}