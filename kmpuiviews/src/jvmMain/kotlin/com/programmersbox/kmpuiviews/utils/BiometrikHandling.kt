package com.tecknobit.biometrik.engines

import com.tecknobit.biometrik.enums.AuthenticationResult.Companion.toAuthenticationResult

//Hoping this works
class BiometrikHandling {
    private val nativeEngine = NativeEngine.getInstance()

    fun requestAuth() = nativeEngine.requestAuth("want it").toAuthenticationResult()
}
