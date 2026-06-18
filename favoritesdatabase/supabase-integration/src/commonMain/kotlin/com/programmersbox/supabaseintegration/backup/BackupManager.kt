package com.programmersbox.supabaseintegration.backup

import kotlinx.coroutines.flow.Flow

interface BackupManager {
    suspend fun uploadBackup(filePath: String): Result<String>
    fun getUploadProgress(): Flow<Float>
}
