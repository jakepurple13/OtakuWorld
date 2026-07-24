package com.programmersbox.supabaseintegration.credentials

/**
 * Result of attempting to sign in with a saved password via the platform credential store.
 */
sealed interface CredentialSignInResult {
    data class Success(val email: String, val password: String) : CredentialSignInResult
    data object NoCredentials : CredentialSignInResult
    data object Cancelled : CredentialSignInResult
    data class Error(val message: String) : CredentialSignInResult
}

/**
 * Result of registering a new passkey via the platform credential store.
 */
sealed interface PasskeyRegistrationResult {
    data class Success(val credentialJson: String) : PasskeyRegistrationResult
    data object Cancelled : PasskeyRegistrationResult
    data class Error(val message: String) : PasskeyRegistrationResult
}

/**
 * Platform credential store integration (Android Credential Manager / androidx.credentials).
 * Distinct from [CredentialManager] in this same package, which persists the Supabase project
 * URL and anon key — unrelated to platform credential storage.
 *
 * [isSupported] is false on every platform except Android; the sign-in/register methods are
 * unreachable on unsupported platforms because the UI that calls them is not rendered there.
 *
 * [context] is an opaque per-call Android Activity context (passed as `Any?` to keep this
 * interface multiplatform); androidx.credentials requires an Activity context to display its
 * picker UI. Ignored on platforms where [isSupported] is false.
 */
interface CredentialSignIn {
    val isSupported: Boolean
    suspend fun signInWithSavedPassword(): CredentialSignInResult
    suspend fun registerPasskey(challengeId: String, creationOptionsJson: String): PasskeyRegistrationResult
}

expect fun createCredentialSignIn(): CredentialSignIn
