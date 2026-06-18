package com.programmersbox.supabaseintegration.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.supabaseintegration.client.SupabaseClientProvider
import com.programmersbox.supabaseintegration.credentials.CredentialManager
import com.programmersbox.supabaseintegration.credentials.SupabaseCredentials
import com.programmersbox.supabaseintegration.sync.SyncConfig
import com.programmersbox.supabaseintegration.sync.SyncConfigRepository
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SupabaseConfigViewModel(
    private val credentialManager: CredentialManager,
    private val clientProvider: SupabaseClientProvider,
    private val syncConfigRepository: SyncConfigRepository,
) : ViewModel() {
    val projectUrl = MutableStateFlow("")
    val anonKey = MutableStateFlow("")
    val connectionResult = MutableStateFlow<String?>(null)
    val hasCredentials: StateFlow<Boolean> = credentialManager
        .hasCredentials()
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val syncConfig = syncConfigRepository
        .listenForChanges()
        .stateIn(viewModelScope, SharingStarted.Eagerly, SyncConfig())

    val pollIntervalMinutes = MutableStateFlow("")
    val maxRetries = MutableStateFlow("")
    val initialBackoffSeconds = MutableStateFlow("")
    val maxBackoffSeconds = MutableStateFlow("")
    val syncConfigSaved = MutableStateFlow(false)

    init {
        credentialManager.getCredentials()?.let {
            projectUrl.value = it.projectUrl
            anonKey.value = it.anonKey
        }
        syncConfig.value.let { c ->
            pollIntervalMinutes.value = (c.pollIntervalMs / 60_000).toString()
            maxRetries.value = c.maxRetries.toString()
            initialBackoffSeconds.value = (c.initialBackoffMs / 1_000).toString()
            maxBackoffSeconds.value = (c.maxBackoffMs / 1_000).toString()
        }
    }

    fun onPollIntervalChange(v: String) { pollIntervalMinutes.value = v; syncConfigSaved.value = false }
    fun onMaxRetriesChange(v: String) { maxRetries.value = v; syncConfigSaved.value = false }
    fun onInitialBackoffChange(v: String) { initialBackoffSeconds.value = v; syncConfigSaved.value = false }
    fun onMaxBackoffChange(v: String) { maxBackoffSeconds.value = v; syncConfigSaved.value = false }

    fun saveSyncConfig() {
        viewModelScope.launch {
            pollIntervalMinutes.value.toLongOrNull()?.takeIf { it > 0 }?.let {
                syncConfigRepository.updatePollIntervalMs(it * 60_000)
            }
            maxRetries.value.toIntOrNull()?.takeIf { it > 0 }?.let {
                syncConfigRepository.updateMaxRetries(it)
            }
            initialBackoffSeconds.value.toLongOrNull()?.takeIf { it > 0 }?.let {
                syncConfigRepository.updateInitialBackoffMs(it * 1_000)
            }
            maxBackoffSeconds.value.toLongOrNull()?.takeIf { it > 0 }?.let {
                syncConfigRepository.updateMaxBackoffMs(it * 1_000)
            }
            syncConfigSaved.value = true
        }
    }

    fun onProjectUrlChange(value: String) { projectUrl.value = value }
    fun onAnonKeyChange(value: String) { anonKey.value = value }

    fun testConnection() {
        viewModelScope.launch {
            connectionResult.value = null
            val url = projectUrl.value.trim()
            val key = anonKey.value.trim()
            val tempClient = createSupabaseClient(url, key) {
                install(Postgrest)
            }
            runCatching {
                // Perform a real network call with limit(0) — succeeds only if URL/key are valid
                tempClient.from("favorite_items").select { limit(0) }
            }.onSuccess {
                // Credentials are valid — persist and rebuild the shared client
                credentialManager.saveCredentials(SupabaseCredentials(url, key))
                clientProvider.recreate()
                connectionResult.value = "✓ Connection successful"
            }.onFailure {
                // Do NOT persist credentials on failure
                connectionResult.value = "✗ ${it.message}"
            }
            tempClient.close()
        }
    }

    fun save() {
        viewModelScope.launch {
            credentialManager.saveCredentials(SupabaseCredentials(projectUrl.value.trim(), anonKey.value.trim()))
            clientProvider.recreate()
        }
    }

    fun clear() {
        viewModelScope.launch {
            credentialManager.clearCredentials()
            clientProvider.close()
            projectUrl.value = ""
            anonKey.value = ""
        }
    }
}
