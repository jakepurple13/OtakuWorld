package com.programmersbox.supabaseintegration.credentials

private class UnsupportedCredentialSignIn : CredentialSignIn {
    override val isSupported: Boolean = false

    override suspend fun signInWithSavedPassword(): CredentialSignInResult =
        CredentialSignInResult.Error("Credential Manager sign-in is not supported on this platform")

    override suspend fun registerPasskey(
        challengeId: String,
        creationOptionsJson: String,
    ): PasskeyRegistrationResult =
        PasskeyRegistrationResult.Error("Passkey registration is not supported on this platform")
}

actual fun createCredentialSignIn(): CredentialSignIn = UnsupportedCredentialSignIn()
