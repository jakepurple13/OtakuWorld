package com.programmersbox.supabaseintegration.credentials

import kotlinx.serialization.Serializable

@Serializable
data class SupabaseCredentials(
    val projectUrl: String,
    val anonKey: String,
)
