package com.programmersbox.supabaseintegration.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.supabaseintegration.auth.AuthManager
import com.programmersbox.supabaseintegration.auth.AuthState
import com.programmersbox.supabaseintegration.credentials.CredentialManager
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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class AuthViewModel(
    private val authManager: AuthManager,
    private val credentialManager: CredentialManager,
) : ViewModel() {
    val authState: StateFlow<AuthState> = authManager.authState

    fun signInWithEmail(email: String, password: String) {
        viewModelScope.launch { authManager.signInWithEmail(email, password) }
    }

    fun signUpWithEmail(email: String, password: String) {
        viewModelScope.launch { authManager.signUpWithEmail(email, password) }
    }

    fun signInWithMagicLink(email: String) {
        viewModelScope.launch { authManager.signInWithMagicLink(email) }
    }

    fun signInWithProvider(provider: OAuthProvider) {
        viewModelScope.launch { authManager.signInWithOAuth(provider) }
    }

    fun signOut() {
        viewModelScope.launch { authManager.signOut() }
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
            // Public GoTrue endpoint containing external provider statuses
            val response = client.get("$supabaseUrl/auth/v1/settings") {
                header("apikey", anonKey)
            }
            val json = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            val externalProviders = json["external"]?.jsonObject ?: return emptyList()

            // Filter and return only the providers explicitly set to true
            externalProviders.entries
                .filter { it.value.jsonPrimitive.booleanOrNull == true }
                .map { it.key } // Returns names like "google", "github", "apple"
        } catch (_: Exception) {
            emptyList() // Handle network or parsing errors appropriately
        } finally {
            client.close()
        }
    }
}
