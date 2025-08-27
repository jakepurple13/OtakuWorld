package com.programmersbox.kmpuiviews.utils

import com.programmersbox.favoritesdatabase.ExceptionDao
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.serialization.Serializable

class Backup(
    private val exceptionDao: ExceptionDao,
    private val zipper: Zipper,
) {
    suspend fun createBackup(document: PlatformFile) {
        runCatching { zipper.zipFile(document) }
            .logFailureToDatabase()
    }

    suspend fun restoreBackup(document: PlatformFile) {
        runCatching { zipper.readZip(document) }
            .logFailureToDatabase()
    }

    private suspend fun <T> Result<T>.logFailureToDatabase() = onFailure {
        it.printStackTrace()
        exceptionDao.insertException(it)
    }
}

@Serializable
data class BackupSettings(
    val stringSettings: Map<String, String>,
    val intSettings: Map<String, Int>,
    val longSettings: Map<String, Long>,
    val booleanSettings: Map<String, Boolean>,
    val doubleSettings: Map<String, Double>,
    val byteArraySettings: Map<String, ByteArray>,
)