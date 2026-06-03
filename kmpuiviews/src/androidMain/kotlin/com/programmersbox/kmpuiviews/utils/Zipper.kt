package com.programmersbox.kmpuiviews.utils

import android.content.Context
import com.programmersbox.favoritesdatabase.ExceptionDao
import com.programmersbox.kmpuiviews.logFirebaseMessage
import com.programmersbox.kmpuiviews.utils.backupproccesor.BackupProcessor
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.toAndroidUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okio.buffer
import okio.sink
import okio.source
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStream
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

    actual suspend fun zipFile(platformFile: PlatformFile) {
        val f = platformFile.toAndroidUri("")
        withContext(Dispatchers.IO) {
            val pfd = context
                .contentResolver
                .openFileDescriptor(f, "w")!!
            ZipOutputStream(FileOutputStream(pfd.fileDescriptor)).use { zip ->
                backupProcessors.forEach { backup ->
                    val duration = measureTime {
                        zip.putNextEntry(ZipEntry(backup.fileName))
                        runCatching {
                            measureTime {
                                val sink = zip.sink().buffer()
                                backup.backup(sink)
                                sink.flush()
                            }
                        }
                            .onSuccess { println("Wrote ${backup.fileName} in $it") }
                            .logFailureToDatabase()
                    }

                    logFirebaseMessage("Zipped ${backup.fileName} in $duration")
                }
            }
        }
    }

    actual suspend fun readZip(platformFile: PlatformFile) {
        withContext(Dispatchers.IO) {
            val pfd = context
                .contentResolver
                .openFileDescriptor(platformFile.toAndroidUri(""), "r")!!
            pfd.use {
                FileInputStream(it.fileDescriptor).use { inStream ->
                    ZipInputStream(inStream).use { zipIs ->
                        var entry: ZipEntry?
                        while (true) {
                            entry = zipIs.nextEntry
                            if (entry == null) break
                            val duration = measureTime {
                                runCatching {
                                    backupProcessors
                                        .find { it.fileName == entry.name }
                                        ?.restore(
                                            json = zipIs.bufferedReader().readText(),
                                            bufferedSource = zipIs.source().buffer()
                                        )
                                }.logFailureToDatabase()
                            }
                            logFirebaseMessage("Unzipped ${entry.name} in $duration")
                        }
                    }
                }
            }
        }
    }

    protected suspend fun <T> Result<T>.logFailureToDatabase() = onFailure {
        it.printStackTrace()
        exceptionDao.insertException(it)
    }

    protected inline fun <reified T> dataToOutputStream(data: T, outputStream: OutputStream) {
        Json.encodeToString(data)
            .byteInputStream()
            .copyTo(outputStream)
    }
}