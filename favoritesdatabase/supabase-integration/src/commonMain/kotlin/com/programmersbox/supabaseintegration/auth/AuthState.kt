package com.programmersbox.supabaseintegration.auth

sealed class AuthState {
    object Unauthenticated : AuthState()
    object Loading : AuthState()
    data class Authenticated(val user: SupabaseUser) : AuthState()
    data class Error(val message: String) : AuthState()
}
