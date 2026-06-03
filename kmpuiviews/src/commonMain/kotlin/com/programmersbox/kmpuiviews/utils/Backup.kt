package com.programmersbox.kmpuiviews.utils

import com.programmersbox.favoritesdatabase.ExceptionDao
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.serialization.Serializable
import kotlin.time.measureTime

class Backup(
    private val exceptionDao: ExceptionDao,
    private val zipper: Zipper,
) {
    suspend fun createBackup(document: PlatformFile) {
        val time = measureTime {
            runCatching { zipper.zipFile(document) }
                .logFailureToDatabase()
                .getOrThrow()
        }

        println("Took $time to zip file")
    }

    suspend fun restoreBackup(document: PlatformFile) {
        runCatching { zipper.readZip(document) }
            .logFailureToDatabase()
            .getOrThrow()
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