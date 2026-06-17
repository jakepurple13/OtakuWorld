package com.programmersbox.supabaseintegration.backup

import com.programmersbox.supabaseintegration.auth.AuthManager
import com.programmersbox.supabaseintegration.auth.AuthState
import com.programmersbox.supabaseintegration.client.SupabaseClientProvider
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.time.Clock

class BackupManagerImpl(
    private val clientProvider: SupabaseClientProvider,
    private val authManager: AuthManager,
) : BackupManager {
    private val _progress = MutableStateFlow(0f)
    override fun getUploadProgress(): Flow<Float> = _progress

    override suspend fun uploadBackup(filePath: String): Result<String> = runCatching {
        val uid = (authManager.authState.value as? AuthState.Authenticated)?.user?.id
            ?: error("Not authenticated")
        val client = clientProvider.getOrCreate() ?: error("Client not initialized")
        val timestamp = Clock.System.now().toEpochMilliseconds()
        val remotePath = "backups/$uid/backup_$timestamp.db"
        client.storage["otakuworld-backups"].upload(remotePath, readFileBytes(filePath)) {
            upsert = false
        }
        remotePath
    }
}
