package com.programmersbox.supabaseintegration.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.supabaseintegration.auth.AuthManager
import com.programmersbox.supabaseintegration.auth.AuthState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel(private val authManager: AuthManager) : ViewModel() {
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
    fun signOut() { viewModelScope.launch { authManager.signOut() } }
}
