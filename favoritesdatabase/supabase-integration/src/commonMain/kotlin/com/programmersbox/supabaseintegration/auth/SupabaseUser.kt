package com.programmersbox.supabaseintegration.auth

import androidx.compose.runtime.Stable

@Stable
data class SupabaseUser(
    val id: String,
    val email: String?,
    val phone: String?,
    val displayName: String?,
)
