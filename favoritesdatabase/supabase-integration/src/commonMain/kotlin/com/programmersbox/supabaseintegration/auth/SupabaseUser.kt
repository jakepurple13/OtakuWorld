package com.programmersbox.supabaseintegration.auth

data class SupabaseUser(
    val id: String,
    val email: String?,
    val phone: String?,
    val displayName: String?,
)
