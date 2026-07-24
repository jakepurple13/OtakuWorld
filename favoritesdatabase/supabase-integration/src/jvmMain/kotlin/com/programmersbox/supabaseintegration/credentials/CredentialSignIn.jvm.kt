package com.programmersbox.supabaseintegration.credentials

private class UnsupportedCredentialSignIn : CredentialSignIn {
    override val isSupported: Boolean = false

    override suspend fun signInWithSavedPassword(context: Any?): CredentialSignInResult =
        CredentialSignInResult.Error("Credential Manager sign-in is not supported on this platform")

    override suspend fun registerPasskey(
        context: Any?,
        challengeId: String,
        creationOptionsJson: String,
    ): PasskeyRegistrationResult =
        PasskeyRegistrationResult.Error("Passkey registration is not supported on this platform")
}

actual fun createCredentialSignIn(): CredentialSignIn = UnsupportedCredentialSignIn()
