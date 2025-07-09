package com.programmersbox.kmpuiviews.repository

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.core.content.getSystemService

actual class PlatformRepository(
    context: Context,
) {
    private val biometricManager by lazy { BiometricManager.from(context) }
    private val keyguardManager by lazy { context.getSystemService<KeyguardManager>() }

    actual fun hasBiometric(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            when (biometricManager.canAuthenticate(BiometricManager.Authenticators.DEVICE_CREDENTIAL or BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
                BiometricManager.BIOMETRIC_SUCCESS -> true
                BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> false
                BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> false
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> false
                BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> true
                BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> true
                BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> false
                else -> false
            }
        } else {
            when (biometricManager.canAuthenticate()) {
                BiometricManager.BIOMETRIC_SUCCESS -> true
                BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> false
                BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> false
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> false
                else -> false
            }
        } || (keyguardManager?.isKeyguardSecure == true)
    }
}