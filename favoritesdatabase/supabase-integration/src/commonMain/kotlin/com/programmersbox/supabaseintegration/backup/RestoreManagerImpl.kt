package com.programmersbox.supabaseintegration.backup

import com.programmersbox.supabaseintegration.auth.AuthManager
import com.programmersbox.supabaseintegration.auth.AuthState
import com.programmersbox.supabaseintegration.client.SupabaseClientProvider
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

class RestoreManagerImpl(
    private val clientProvider: SupabaseClientProvider,
    private val authManager: AuthManager,
) : RestoreManager {
    private val _progress = MutableStateFlow(0f)
    override fun getDownloadProgress(): Flow<Float> = _progress

    override suspend fun listBackups(): List<BackupEntry> {
        val uid = (authManager.authState.value as? AuthState.Authenticated)?.user?.id
            ?: return emptyList()
        val client = clientProvider.getOrCreate() ?: return emptyList()
        return client.storage["otakuworld-backups"]
            .list("backups/$uid")
            .map { obj ->
                BackupEntry(
                    remotePath = "backups/$uid/${obj.name}",
                    name = obj.name,
                    createdAt = obj.createdAt?.toEpochMilliseconds() ?: 0L,
                    sizeBytes = obj.metadata?.get("size")?.jsonPrimitive?.longOrNull ?: 0L,
                )
            }
            .sortedByDescending { it.createdAt }
    }

    override suspend fun downloadBackup(entry: BackupEntry, localPath: String): Result<String> = runCatching {
        val client = clientProvider.getOrCreate() ?: error("Client not initialized")
        val bytes = client.storage["otakuworld-backups"].downloadAuthenticated(entry.remotePath)
        writeFileBytes(localPath, bytes)
        localPath
    }
}
