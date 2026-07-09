package com.programmersbox.kmpuiviews.utils

import android.content.Context
import com.programmersbox.favoritesdatabase.ExceptionDao
import com.programmersbox.kmpuiviews.logFirebaseMessage
import com.programmersbox.sharedcomponents.backup.BackupDataSummary
import com.programmersbox.sharedcomponents.backup.BackupUiInfo
import com.programmersbox.sharedcomponents.backup.ItemResult
import com.programmersbox.sharedtools.BackupProcessor
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.toAndroidUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.Buffer
import okio.buffer
import okio.sink
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.time.measureTime

actual open class Zipper(
    private val context: Context,
    private val backupProcessors: List<BackupProcessor>,
    protected val exceptionDao: ExceptionDao,
) {

    init {
        val processors = backupProcessors.map { it.fileName }
        println("Backup processors: $processors")
    }

    actual suspend fun zipFile(
        platformFile: PlatformFile,
        selectedKeys: Set<String>,
        onItemComplete: suspend (ItemResult) -> Unit,
    ): List<ItemResult> = withContext(Dispatchers.IO) {
        val results = mutableListOf<ItemResult>()
        val pfd = context.contentResolver.openFileDescriptor(platformFile.toAndroidUri(""), "w")!!
        ZipOutputStream(FileOutputStream(pfd.fileDescriptor)).use { zip ->
            backupProcessors.filter { it.fileName in selectedKeys }.forEach { backup ->
                logFirebaseMessage("Zipping ${backup.fileName}")
                val duration = measureTime {
                    zip.putNextEntry(ZipEntry(backup.fileName))
                    val result = runCatching {
                        val sink = zip.sink().buffer()
                        backup.backup(sink)
                        sink.flush()
                    }
                        .onFailure { it.printStackTrace(); exceptionDao.insertException(it) }
                        .fold(
                            onSuccess = { ItemResult(backup.fileName, success = true) },
                            onFailure = { e -> ItemResult(backup.fileName, success = false, error = e.message) },
                        )
                    results += result
                    onItemComplete(result)
                }
                logFirebaseMessage("Zipped ${backup.fileName} in $duration")
            }
        }
        results
    }

    actual suspend fun readZip(
        platformFile: PlatformFile,
        selectedKeys: Set<String>,
        onItemComplete: suspend (ItemResult) -> Unit,
    ): List<ItemResult> = withContext(Dispatchers.IO) {
        val results = mutableListOf<ItemResult>()
        context.contentResolver.openFileDescriptor(platformFile.toAndroidUri(""), "r")!!.use { pfd ->
            FileInputStream(pfd.fileDescriptor).use { inStream ->
                ZipInputStream(inStream).use { zipIs ->
                    var entry: ZipEntry? = zipIs.nextEntry
                    while (entry != null) {
                        val name = entry.name
                        val processor = backupProcessors.find { it.fileName == name }
                        if (name in selectedKeys && processor != null) {
                            val duration = measureTime {
                                val bytes = zipIs.readBytes()
                                val result = runCatching {
                                    processor.restore(
                                        json = bytes.decodeToString(),
                                        bufferedSource = Buffer().apply { write(bytes) },
                                    )
                                }
                                    .fold(
                                        onSuccess = { ItemResult(name, success = true) },
                                        onFailure = { e -> ItemResult(name, success = false, error = e.message) },
                                    )
                                results += result
                                onItemComplete(result)
                            }
                            logFirebaseMessage("Unzipped $name in $duration")
                        }
                        entry = zipIs.nextEntry
                    }
                }
            }
        }
        results
    }

    actual suspend fun peekZip(
        platformFile: PlatformFile,
        uiInfos: List<BackupUiInfo>,
    ): Map<String, BackupDataSummary> = withContext(Dispatchers.IO) {
        val summaries = mutableMapOf<String, BackupDataSummary>()
        context.contentResolver.openFileDescriptor(platformFile.toAndroidUri(""), "r")!!.use { pfd ->
            FileInputStream(pfd.fileDescriptor).use { inStream ->
                ZipInputStream(inStream).use { zipIs ->
                    var entry: ZipEntry? = zipIs.nextEntry
                    while (entry != null) {
                        val name = entry.name
                        val uiInfo = uiInfos.find { it.key == name }
                        if (uiInfo != null) {
                            val bytes = zipIs.readBytes()
                            runCatching { uiInfo.parseSummary(json = bytes.decodeToString(), rawBytes = bytes) }
                                .onSuccess { summaries[name] = it }
                                .onFailure { it.printStackTrace(); exceptionDao.insertException(it) }
                        }
                        entry = zipIs.nextEntry
                    }
                }
            }
        }
        summaries
    }
}
