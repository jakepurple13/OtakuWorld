package com.programmersbox.kmpuiviews.utils

import com.programmersbox.sharedtools.BackupProcessor
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.absolutePath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.buffer
import okio.sink
import okio.source
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.time.measureTime

actual class Zipper(
    private val backupProcessors: List<BackupProcessor>,
) {
    init {
        val processors = backupProcessors.map { it.fileName }
        println("Backup processors: $processors")
    }

    actual suspend fun zipFile(platformFile: PlatformFile) {
        val f = platformFile.absolutePath()
        withContext(Dispatchers.IO) {
            ZipOutputStream(FileOutputStream(f)).use { zip ->
                backupProcessors.forEach { processor ->
                    println("Zipping ${processor.fileName}")
                    val duration = measureTime {
                        zip.putNextEntry(ZipEntry(processor.fileName))
                        runCatching { processor.backup(zip.sink().buffer()) }
                    }
                    println("Zipped ${processor.fileName} in $duration")
                }
            }
        }
    }

    actual suspend fun readZip(platformFile: PlatformFile) {
        withContext(Dispatchers.IO) {
            FileInputStream(platformFile.absolutePath()).use { inStream ->
                ZipInputStream(inStream).use { zipIs ->
                    var entry: ZipEntry?
                    while (true) {
                        entry = zipIs.nextEntry
                        if (entry == null) break
                        val duration = measureTime {
                            runCatching {
                                backupProcessors
                                    .find { it.fileName == entry.name }
                                    .also { println("Unzipping ${it?.fileName}") }
                                    ?.restore(
                                        json = zipIs.bufferedReader().readText(),
                                        bufferedSource = zipIs.source().buffer()
                                    )
                            }
                        }
                        println("Unzipped ${entry.name} in $duration")
                    }
                }
            }
        }
    }
}