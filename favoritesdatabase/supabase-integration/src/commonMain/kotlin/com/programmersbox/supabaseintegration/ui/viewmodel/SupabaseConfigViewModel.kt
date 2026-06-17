package com.programmersbox.supabaseintegration.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.supabaseintegration.client.SupabaseClientProvider
import com.programmersbox.supabaseintegration.credentials.CredentialManager
import com.programmersbox.supabaseintegration.credentials.SupabaseCredentials
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SupabaseConfigViewModel(
    private val credentialManager: CredentialManager,
    private val clientProvider: SupabaseClientProvider,
) : ViewModel() {
    val projectUrl = MutableStateFlow("")
    val anonKey = MutableStateFlow("")
    val connectionResult = MutableStateFlow<String?>(null)
    val hasCredentials: StateFlow<Boolean> = credentialManager.hasCredentials()
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    init {
        credentialManager.getCredentials()?.let {
            projectUrl.value = it.projectUrl
            anonKey.value = it.anonKey
        }
    }

    fun onProjectUrlChange(value: String) { projectUrl.value = value }
    fun onAnonKeyChange(value: String) { anonKey.value = value }

    fun testConnection() {
        viewModelScope.launch {
            connectionResult.value = null
            runCatching {
                val testClient = SupabaseCredentials(
                    projectUrl.value.trim(), anonKey.value.trim()
                )
                credentialManager.saveCredentials(testClient)
                clientProvider.recreate()
                connectionResult.value = "✓ Connection successful"
            }.onFailure {
                connectionResult.value = "✗ ${it.message}"
            }
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
