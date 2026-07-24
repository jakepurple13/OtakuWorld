package com.programmersbox.supabaseintegration.ui.viewmodel

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.supabaseintegration.auth.AuthManager
import com.programmersbox.supabaseintegration.auth.AuthState
import com.programmersbox.supabaseintegration.credentials.CredentialManager
import com.programmersbox.supabaseintegration.credentials.CredentialSignIn
import com.programmersbox.supabaseintegration.credentials.CredentialSignInResult
import com.programmersbox.supabaseintegration.credentials.PasskeyRegistrationResult
import com.programmersbox.supabaseintegration.database.DatabaseRepository
import com.programmersbox.supabaseintegration.database.ManagedTable
import com.programmersbox.supabaseintegration.database.SupportedTableAction
import io.github.jan.supabase.auth.providers.Apple
import io.github.jan.supabase.auth.providers.Azure
import io.github.jan.supabase.auth.providers.Bitbucket
import io.github.jan.supabase.auth.providers.Discord
import io.github.jan.supabase.auth.providers.Facebook
import io.github.jan.supabase.auth.providers.Figma
import io.github.jan.supabase.auth.providers.Github
import io.github.jan.supabase.auth.providers.Gitlab
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.Kakao
import io.github.jan.supabase.auth.providers.Keycloak
import io.github.jan.supabase.auth.providers.LinkedIn
import io.github.jan.supabase.auth.providers.Notion
import io.github.jan.supabase.auth.providers.OAuthProvider
import io.github.jan.supabase.auth.providers.Slack
import io.github.jan.supabase.auth.providers.Spotify
import io.github.jan.supabase.auth.providers.Twitch
import io.github.jan.supabase.auth.providers.Twitter
import io.github.jan.supabase.auth.providers.WorkOS
import io.github.jan.supabase.auth.providers.Zoom
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Stable
data class ManagedTableSelection(
    val table: ManagedTable,
    val selectedAction: SupportedTableAction,
)

@Stable
data class LogoutUiState(
    val manageDatabasesEnabled: Boolean = false,
    val tableSelections: List<ManagedTableSelection> = emptyList(),
    val isLoggingOut: Boolean = false,
)

@Stable
sealed interface PasskeyRegistrationUiState {
    data object Idle : PasskeyRegistrationUiState
    data object Loading : PasskeyRegistrationUiState
    data object Success : PasskeyRegistrationUiState
    data class Error(val message: String) : PasskeyRegistrationUiState
}

class AuthViewModel(
    private val authManager: AuthManager,
    private val credentialManager: CredentialManager,
    private val credentialSignIn: CredentialSignIn,
    private val databaseRepository: DatabaseRepository,
) : ViewModel() {
    val authState: StateFlow<AuthState> = authManager.authState

    val credentialManagerSupported: Boolean = credentialSignIn.isSupported

    private val _passkeyRegistrationState = MutableStateFlow<PasskeyRegistrationUiState>(PasskeyRegistrationUiState.Idle)
    val passkeyRegistrationState: StateFlow<PasskeyRegistrationUiState> = _passkeyRegistrationState.asStateFlow()

    private val _logoutUiState = MutableStateFlow(
        LogoutUiState(
            tableSelections = databaseRepository.managedTables.map {
                ManagedTableSelection(table = it, selectedAction = it.defaultAction)
            }
        )
    )
    val logoutUiState: StateFlow<LogoutUiState> = _logoutUiState.asStateFlow()

    fun signInWithEmail(email: String, password: String) {
        viewModelScope.launch { authManager.signInWithEmail(email, password) }
    }

    fun signInWithCredentialManager(context: Any?) {
        viewModelScope.launch {
            when (val result = credentialSignIn.signInWithSavedPassword(context)) {
                is CredentialSignInResult.Success -> signInWithEmail(result.email, result.password)
                CredentialSignInResult.Cancelled, CredentialSignInResult.NoCredentials -> Unit
                is CredentialSignInResult.Error -> authManager.reportError(result.message)
            }
        }
    }

    fun signUpWithEmail(email: String, password: String) {
        viewModelScope.launch { authManager.signUpWithEmail(email, password) }
    }

    fun signInWithMagicLink(email: String) {
        viewModelScope.launch { authManager.signInWithMagicLink(email) }
    }

    fun registerPasskey(context: Any?) {
        viewModelScope.launch {
            _passkeyRegistrationState.value = PasskeyRegistrationUiState.Loading
            try {
                val registration = authManager.startPasskeyRegistration()
                when (val result = credentialSignIn.registerPasskey(context, registration.challengeId, registration.options.toString())) {
                    is PasskeyRegistrationResult.Success -> {
                        authManager.verifyPasskeyRegistration(registration.challengeId, result.credentialJson)
                        _passkeyRegistrationState.value = PasskeyRegistrationUiState.Success
                    }
                    PasskeyRegistrationResult.Cancelled -> {
                        _passkeyRegistrationState.value = PasskeyRegistrationUiState.Idle
                    }
                    is PasskeyRegistrationResult.Error -> {
                        _passkeyRegistrationState.value = PasskeyRegistrationUiState.Error(result.message)
                    }
                }
            } catch (e: Exception) {
                _passkeyRegistrationState.value = PasskeyRegistrationUiState.Error(e.message ?: "Passkey registration failed")
            }
        }
    }

    fun dismissPasskeyRegistrationResult() {
        _passkeyRegistrationState.value = PasskeyRegistrationUiState.Idle
    }

    fun signInWithProvider(provider: OAuthProvider) {
        viewModelScope.launch { authManager.signInWithOAuth(provider) }
    }

    fun setManageDatabasesEnabled(enabled: Boolean) {
        _logoutUiState.update { state ->
            state.copy(
                manageDatabasesEnabled = enabled,
                tableSelections = if (enabled) {
                    state.tableSelections
                } else {
                    state.tableSelections.map { it.copy(selectedAction = it.table.defaultAction) }
                }
            )
        }
    }

    fun setTableAction(table: ManagedTable, action: SupportedTableAction) {
        _logoutUiState.update { state ->
            state.copy(
                tableSelections = state.tableSelections.map {
                    if (it.table == table) it.copy(selectedAction = action) else it
                }
            )
        }
    }

    fun confirmLogout() {
        viewModelScope.launch {
            _logoutUiState.update { it.copy(isLoggingOut = true) }
            try {
                authManager.signOut()
                val selections = _logoutUiState.value
                    .tableSelections
                    .associate { it.table to it.selectedAction }
                databaseRepository.executeActions(selections)
            } finally {
                _logoutUiState.update { it.copy(isLoggingOut = false) }
            }
        }
    }

    init {
        viewModelScope.launch {
            val credentials = credentialManager.getCredentials()
            val supabaseUrl = credentials?.projectUrl
            val anonKey = credentials?.anonKey
            if (supabaseUrl != null && anonKey != null) {
                val enabledProviders = fetchEnabledProviders(supabaseUrl, anonKey)
                    .mapNotNull { getOAuthProviderObject(it) }

                println(enabledProviders)
            }
        }
    }

    private fun getOAuthProviderObject(providerName: String): OAuthProvider? {
        return when (providerName.lowercase()) {
            "apple" -> Apple
            "azure" -> Azure
            "facebook" -> Facebook
            "github" -> Github
            "gitlab" -> Gitlab
            "google" -> Google
            "linkedin" -> LinkedIn
            "twitter" -> Twitter
            "bitbucket" -> Bitbucket
            "discord" -> Discord
            "figma" -> Figma
            "kakao" -> Kakao
            "keycloak" -> Keycloak
            "notion" -> Notion
            "slack" -> Slack
            "spotify" -> Spotify
            "twitch" -> Twitch
            "workos" -> WorkOS
            "zoom" -> Zoom
            else -> null
        }
    }

    private suspend fun fetchEnabledProviders(supabaseUrl: String, anonKey: String): List<String> {
        val client = HttpClient()
        return try {
            val response = client.get("$supabaseUrl/auth/v1/settings") {
                header("apikey", anonKey)
            }
            val json = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            val externalProviders = json["external"]?.jsonObject ?: return emptyList()

            externalProviders.entries
                .filter { it.value.jsonPrimitive.booleanOrNull == true }
                .map { it.key }
        } catch (_: Exception) {
            emptyList()
        } finally {
            client.close()
        }
    }
}
