package com.programmersbox.kmpuiviews.utils

import com.programmersbox.sharedcomponents.backup.BackupDataSummary
import com.programmersbox.sharedcomponents.backup.BackupUiInfo
import com.programmersbox.sharedcomponents.backup.ItemResult
import com.programmersbox.sharedtools.BackupProcessor
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.absolutePath
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
import kotlin.time.measureTimedValue

actual class Zipper(
    private val backupProcessors: List<BackupProcessor>,
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
        ZipOutputStream(FileOutputStream(platformFile.absolutePath())).use { zip ->
            backupProcessors.filter { it.fileName in selectedKeys }.forEach { processor ->
                println("Zipping ${processor.fileName}")
                val duration = measureTime {
                    zip.putNextEntry(ZipEntry(processor.fileName))
                    val result = runCatching {
                        measureTimedValue {
                            val sink = zip.sink().buffer()
                            val processorResult = processor.backup(sink)
                            sink.flush()
                            processorResult
                        }
                    }
                        .fold(
                            onSuccess = { timedValue ->
                                val processorResult = timedValue.value
                                ItemResult(
                                    processor.fileName,
                                    timeTaken = timedValue.duration.toString(),
                                    success = processorResult.successCount > 0,
                                    error = processorResult.failed.takeIf { it.isNotEmpty() }
                                        ?.let { "${it.size} failed: ${it.joinToString()}" },
                                )
                            },
                            onFailure = { e ->
                                ItemResult(
                                    processor.fileName,
                                    timeTaken = e.message ?: "Unknown Error",
                                    success = false,
                                    error = e.message
                                )
                            },
                        )
                    results += result
                    onItemComplete(result)
                }
                println("Zipped ${processor.fileName} in $duration")
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
        FileInputStream(platformFile.absolutePath()).use { inStream ->
            ZipInputStream(inStream).use { zipIs ->
                var entry: ZipEntry? = zipIs.nextEntry
                while (entry != null) {
                    val name = entry.name
                    val processor = backupProcessors.find { it.fileName == name }
                    if (name in selectedKeys && processor != null) {
                        val duration = measureTime {
                            val result = runCatching {
                                measureTimedValue {
                                    val bytes = zipIs.readBytes()
                                    processor.restore(
                                        json = bytes.decodeToString(),
                                        bufferedSource = Buffer().apply { write(bytes) },
                                    )
                                }
                            }
                                .fold(
                                    onSuccess = { timedValue ->
                                        val processorResult = timedValue.value
                                        ItemResult(
                                            name,
                                            timeTaken = timedValue.duration.toString(),
                                            success = processorResult.successCount > 0,
                                            error = processorResult.failed.takeIf { it.isNotEmpty() }
                                                ?.let { "${it.size} failed: ${it.joinToString()}" },
                                        )
                                    },
                                    onFailure = { e ->
                                        ItemResult(
                                            name,
                                            timeTaken = e.message ?: "Unknown Error",
                                            success = false,
                                            error = e.message
                                        )
                                    },
                                )
                            results += result
                            onItemComplete(result)
                        }
                        println("Unzipped $name in $duration")
                    }
                    entry = zipIs.nextEntry
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
        FileInputStream(platformFile.absolutePath()).use { inStream ->
            ZipInputStream(inStream).use { zipIs ->
                var entry: ZipEntry? = zipIs.nextEntry
                while (entry != null) {
                    val name = entry.name
                    val uiInfo = uiInfos.find { it.key == name }
                    if (uiInfo != null) {
                        runCatching {
                            val bytes = zipIs.readBytes()
                            uiInfo.parseSummary(json = bytes.decodeToString(), rawBytes = bytes)
                        }
                            .onSuccess { summaries[name] = it }
                    }
                    entry = zipIs.nextEntry
                }
            }
        }
        summaries
    }
}
