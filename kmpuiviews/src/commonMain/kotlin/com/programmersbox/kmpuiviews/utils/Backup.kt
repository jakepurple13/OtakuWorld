package com.programmersbox.kmpuiviews.utils

import com.programmersbox.favoritesdatabase.CustomList
import com.programmersbox.favoritesdatabase.ExceptionDao
import com.programmersbox.sharedcomponents.backup.BackupDataSummary
import com.programmersbox.sharedcomponents.backup.BackupUiInfo
import com.programmersbox.sharedcomponents.backup.ItemResult
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.serialization.Serializable
import kotlin.time.measureTime

class Backup(
    private val exceptionDao: ExceptionDao,
    private val zipper: Zipper,
) {
    suspend fun createBackup(
        document: PlatformFile,
        selectedKeys: Set<String>,
        selectedListIds: Set<String>? = null,
        onItemComplete: suspend (ItemResult) -> Unit,
    ): List<ItemResult> {
        var output: List<ItemResult> = emptyList()
        val time = measureTime {
            output = runCatching { zipper.zipFile(document, selectedKeys, selectedListIds, onItemComplete) }
                .logFailureToDatabase()
                .getOrThrow()
        }
        println("Took $time to zip file")
        return output
    }

    suspend fun restoreBackup(
        document: PlatformFile,
        selectedKeys: Set<String>,
        selectedListIds: Set<String>? = null,
        onItemComplete: suspend (ItemResult) -> Unit,
    ): List<ItemResult> =
        runCatching { zipper.readZip(document, selectedKeys, selectedListIds, onItemComplete) }
            .logFailureToDatabase()
            .getOrThrow()

    suspend fun peekBackup(document: PlatformFile, uiInfos: List<BackupUiInfo>): Map<String, BackupDataSummary> =
        runCatching { zipper.peekZip(document, uiInfos) }
            .logFailureToDatabase()
            .getOrThrow()

    suspend fun peekListContents(document: PlatformFile): List<CustomList> =
        runCatching { zipper.peekListContents(document) }
            .logFailureToDatabase()
            .getOrElse { emptyList() }

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
