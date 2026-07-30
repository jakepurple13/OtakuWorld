package com.programmersbox.supabaseintegration.auth

import com.programmersbox.favoritesdatabase.ExceptionDao
import com.programmersbox.supabaseintegration.client.SupabaseClientProvider
import com.programmersbox.supabaseintegration.credentials.CredentialSignIn
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.OAuthProvider
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.builtin.OTP
import io.github.jan.supabase.auth.status.RefreshFailureCause
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

class AuthManagerImpl(
    private val clientProvider: SupabaseClientProvider,
    private val exceptionDao: ExceptionDao,
    private val credentialSignIn: CredentialSignIn,
) : AuthManager {
    private val scope = CoroutineScope(Dispatchers.Default)
    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()
    override fun isLoggedIn(): Boolean = _authState.value is AuthState.Authenticated

    init {
        scope.launch {
            clientProvider.clientState.collectLatest { client ->
                if (client == null) {
                    _authState.value = AuthState.Unauthenticated
                    return@collectLatest
                }
                client.auth.sessionStatus.collect { status ->
                    _authState.value = when (status) {
                        is SessionStatus.Authenticated -> {
                            val user = status.session.user
                            AuthState.Authenticated(
                                SupabaseUser(
                                    id = user?.id ?: "",
                                    email = user?.email,
                                    phone = user?.phone,
                                    displayName = user?.userMetadata
                                        ?.get("full_name")
                                        ?.jsonPrimitive
                                        ?.contentOrNull
                                )
                            )
                        }

                        is SessionStatus.NotAuthenticated -> AuthState.Unauthenticated
                        is SessionStatus.Initializing -> AuthState.Loading
                        is SessionStatus.RefreshFailure -> {
                            val message = when (val cause = status.cause) {
                                is RefreshFailureCause.NetworkError ->
                                    cause.exception.message ?: "Network error during session refresh"

                                is RefreshFailureCause.InternalServerError ->
                                    cause.exception.message ?: "Server error during session refresh"

                                else -> "Session refresh failed"
                            }
                            AuthState.Error(message)
                        }
                    }
                }
            }
        }
    }

    private suspend fun auth() = clientProvider.getOrCreate()?.auth
            ?: error("Supabase client not initialized — save credentials first")

    override suspend fun signInWithEmail(email: String, password: String, context: Any?) {
        _authState.value = AuthState.Loading
        runCatching {
            auth().signInWith(Email) {
                this.email = email
                this.password = password
            }
        }
            .mapCatching { credentialSignIn.savePassword(email, password, context) }
            .onFailure {
                it.printStackTrace()
                exceptionDao.insertException(it)
                _authState.value = AuthState.Error(it.message ?: "Sign in failed")
            }
    }

    override suspend fun signUpWithEmail(email: String, password: String, context: Any?) {
        _authState.value = AuthState.Loading
        runCatching {
            auth().signUpWith(Email) {
                this.email = email
                this.password = password
            }
        }
            .mapCatching { credentialSignIn.savePassword(email, password, context) }
            .onFailure {
                it.printStackTrace()
                exceptionDao.insertException(it)
                _authState.value = AuthState.Error(it.message ?: "Sign up failed")
            }
    }

    override suspend fun signInWithOAuth(provider: OAuthProvider) {
        _authState.value = AuthState.Loading
        runCatching {
            auth().signInWith(provider)
        }.onFailure {
            it.printStackTrace()
            exceptionDao.insertException(it)
            _authState.value = AuthState.Error(it.message ?: "OAuth failed")
        }
    }

    override suspend fun signInWithMagicLink(email: String) {
        runCatching {
            auth().signInWith(OTP) { this.email = email }
        }.onFailure {
            it.printStackTrace()
            exceptionDao.insertException(it)
            _authState.value = AuthState.Error(it.message ?: "Magic link failed")
        }
    }

    override suspend fun signInWithPhone(phone: String, otp: String) {
        runCatching {
            auth().verifyPhoneOtp(type = io.github.jan.supabase.auth.OtpType.Phone.SMS, phone = phone, token = otp)
        }.onFailure {
            it.printStackTrace()
            exceptionDao.insertException(it)
            _authState.value = AuthState.Error(it.message ?: "Phone auth failed")
        }
    }

    override suspend fun signInAnonymously() {
        runCatching {
            auth().signInAnonymously()
        }.onFailure {
            it.printStackTrace()
            exceptionDao.insertException(it)
            _authState.value = AuthState.Error(it.message ?: "Anonymous auth failed")
        }
    }

    override suspend fun signOut() {
        runCatching { auth().signOut() }
    }

    override suspend fun deleteAccount() {
        // deleteCurrentUser is not available in the Auth client API in supabase-kt v3.
        // Account deletion requires a server-side admin call or an Edge Function.
        throw UnsupportedOperationException(
            "Account deletion requires a server-side call. " +
                    "Use an Edge Function or Supabase Admin API with the service role key."
        )
    }

    override suspend fun refreshSession() {
        runCatching { auth().refreshCurrentSession() }
    }

    /*@OptIn(SupabaseExperimental::class)
    override suspend fun startPasskeyRegistration(): PasskeyRegistrationResponse {
        return auth.passkeys.startRegistration()
    }

    @OptIn(SupabaseExperimental::class)
    override suspend fun verifyPasskeyRegistration(
        challengeId: String,
        credentialJson: String,
    ): PasskeyRegistrationVerifyResponse {
        return auth.passkeys.verifyRegistration(challengeId, credentialJson)
    }*/

    override fun reportError(message: String) {
        _authState.value = AuthState.Error(message)
    }
}
