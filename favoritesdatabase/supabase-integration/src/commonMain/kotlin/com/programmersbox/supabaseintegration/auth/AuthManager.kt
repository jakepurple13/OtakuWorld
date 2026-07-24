package com.programmersbox.supabaseintegration.auth

import io.github.jan.supabase.auth.providers.OAuthProvider
import kotlinx.coroutines.flow.StateFlow

interface AuthManager {
    val authState: StateFlow<AuthState>
    fun isLoggedIn(): Boolean
    suspend fun signInWithEmail(email: String, password: String)
    suspend fun signUpWithEmail(email: String, password: String)
    suspend fun signInWithOAuth(provider: OAuthProvider)
    suspend fun signInWithMagicLink(email: String)
    suspend fun signInWithPhone(phone: String, otp: String)
    suspend fun signInAnonymously()
    suspend fun signOut()
    suspend fun deleteAccount()
    suspend fun refreshSession()

    //suspend fun startPasskeyRegistration(): PasskeyRegistrationResponse
    //suspend fun verifyPasskeyRegistration(challengeId: String, credentialJson: String): PasskeyRegistrationVerifyResponse
    fun reportError(message: String)
}
