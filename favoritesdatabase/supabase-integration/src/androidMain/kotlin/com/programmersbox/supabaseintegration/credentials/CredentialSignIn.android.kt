package com.programmersbox.supabaseintegration.credentials

import android.content.Context
import androidx.credentials.CreatePasswordRequest
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPasswordOption
import androidx.credentials.PasswordCredential
import androidx.credentials.exceptions.CreateCredentialCancellationException
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.credentials.CredentialManager as AndroidxCredentialManager

/*
 * Supabase dashboard setup for passkeys (registration only — sign-in is not yet supported by
 * supabase-kt, see docs/superpowers/specs/2026-07-24-android-credential-manager-design.md):
 *
 * 1. Dashboard -> Authentication -> Passkeys.
 * 2. Turn on "Enable Passkey authentication".
 * 3. Set Relying Party ID: bare domain, no scheme/port/path (e.g. "example.com").
 * 4. Set Relying Party Display Name: human-readable app name shown in the OS passkey prompt.
 * 5. Set Relying Party Origins: comma-separated allowed origins (up to 5). HTTPS required except
 *    for "localhost" / "127.0.0.1" / "[::1]".
 * 6. Passkeys are cryptographically bound to the Relying Party ID — changing it invalidates every
 *    previously registered passkey.
 */
class AndroidCredentialSignIn(private val context: Context) : CredentialSignIn {

    override val isSupported: Boolean = true
    private val manager = AndroidxCredentialManager.create(context)

    override suspend fun savePassword(email: String, password: String, context: Any?) {
        val activityContext = context as? Context ?: this.context
        val credential = CreatePasswordRequest(id = email, password = password)
        runCatching {
            val d = manager.createCredential(activityContext, credential)
            println(d)
        }.onFailure { it.printStackTrace() }
    }

    override suspend fun signInWithSavedPassword(context: Any?): CredentialSignInResult {
        val activityContext = context as? Context ?: this.context
        val request = GetCredentialRequest(listOf(GetPasswordOption()))
        return try {
            val response = manager.getCredential(activityContext, request)
            val credential = response.credential as? PasswordCredential
            if (credential != null) {
                CredentialSignInResult.Success(email = credential.id, password = credential.password)
            } else {
                CredentialSignInResult.Error("Unexpected credential type returned")
            }
        } catch (e: NoCredentialException) {
            CredentialSignInResult.NoCredentials
        } catch (e: GetCredentialCancellationException) {
            CredentialSignInResult.Cancelled
        } catch (e: GetCredentialException) {
            CredentialSignInResult.Error(e.message ?: "Credential Manager sign-in failed")
        }
    }

    override suspend fun registerPasskey(
        challengeId: String,
        creationOptionsJson: String,
        context: Any?,
    ): PasskeyRegistrationResult {
        val activityContext = context as? Context ?: this.context
        val request = CreatePublicKeyCredentialRequest(requestJson = creationOptionsJson)
        return try {
            val response = manager.createCredential(activityContext, request) as CreatePublicKeyCredentialResponse
            PasskeyRegistrationResult.Success(credentialJson = response.registrationResponseJson)
        } catch (e: CreateCredentialCancellationException) {
            PasskeyRegistrationResult.Cancelled
        } catch (e: CreateCredentialException) {
            PasskeyRegistrationResult.Error(e.message ?: "Passkey registration failed")
        }
    }
}

actual fun createCredentialSignIn(): CredentialSignIn = error(
    "AndroidCredentialSignIn requires a Context — use the Koin-provided single<CredentialSignIn> binding instead of calling this factory directly on Android."
)
