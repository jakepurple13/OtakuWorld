package com.programmersbox.supabaseintegration.credentials

import kotlinx.coroutines.flow.Flow

interface CredentialManager {
    fun hasCredentials(): Flow<Boolean>
    suspend fun saveCredentials(credentials: SupabaseCredentials)
    fun getCredentials(): SupabaseCredentials?
    suspend fun clearCredentials()
}

expect fun createCredentialManager(context: Any?): CredentialManager
