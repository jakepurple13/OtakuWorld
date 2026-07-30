package com.programmersbox.supabaseintegration.credentials

import kotlinx.coroutines.flow.Flow

interface CredentialManager {
    fun hasCredentials(): Flow<Boolean>
    suspend fun saveCredentials(credentials: SupabaseCredentials)
    suspend fun getCredentials(): SupabaseCredentials?
    suspend fun clearCredentials()
}
