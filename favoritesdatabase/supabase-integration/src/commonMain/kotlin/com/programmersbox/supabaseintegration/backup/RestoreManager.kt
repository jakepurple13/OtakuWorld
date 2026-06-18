package com.programmersbox.supabaseintegration.backup

import kotlinx.coroutines.flow.Flow

data class BackupEntry(
    val remotePath: String,
    val name: String,
    val createdAt: Long,
    val sizeBytes: Long,
)

interface RestoreManager {
    suspend fun listBackups(): List<BackupEntry>
    suspend fun downloadBackup(entry: BackupEntry, localPath: String): Result<String>
    fun getDownloadProgress(): Flow<Float>
}
