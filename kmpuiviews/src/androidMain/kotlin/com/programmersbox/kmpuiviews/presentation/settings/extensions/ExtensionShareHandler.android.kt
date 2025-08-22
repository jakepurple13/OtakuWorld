package com.programmersbox.kmpuiviews.presentation.settings.extensions

import android.content.Context
import com.programmersbox.kmpmodels.KmpSourceInformation
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.uri
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

actual class ExtensionShareHandler(
    private val context: Context,
) {
    actual suspend fun shareExtensions(platformFile: PlatformFile, extensions: List<KmpSourceInformation>) {
        runCatching {
            val f = platformFile.uri
            val pfd = context.contentResolver.openFileDescriptor(f, "w")!!
            ZipOutputStream(
                FileOutputStream(pfd.fileDescriptor)
            ).use { zip ->
                extensions.forEach { source ->
                    runCatching {
                        val info = context
                            .packageManager
                            .getApplicationInfo(source.packageName, 0)
                        val path = info.publicSourceDir
                        val file = File(path)
                        zip.putNextEntry(ZipEntry("${source.packageName}${source.name}.apk"))
                        file.inputStream().copyTo(zip)
                    }
                        .onFailure { it.printStackTrace() }
                        .getOrNull()
                }
            }
        }.onFailure { it.printStackTrace() }
    }
}