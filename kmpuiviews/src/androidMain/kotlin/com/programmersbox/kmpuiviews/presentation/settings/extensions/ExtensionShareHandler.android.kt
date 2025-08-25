package com.programmersbox.kmpuiviews.presentation.settings.extensions

import android.content.Context
import android.content.Intent
import android.content.Intent.createChooser
import com.programmersbox.favoritesdatabase.ExceptionDao
import com.programmersbox.kmpmodels.KmpSourceInformation
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

actual class ExtensionShareHandler(
    private val context: Context,
    private val exceptionDao: ExceptionDao,
) {
    actual suspend fun shareExtensions(platformFile: PlatformFile, extensions: List<KmpSourceInformation>) {
        //TODO: Maybe make a desktop tool that looks for these apps
        // it could handle exporting everything and installing and setting everything?
        // Maybe everything pulls into a single zip file?
        // Desktop tool can also list all of the extensions for each app?
        runCatching {
            val f = platformFile.uri
            withContext(Dispatchers.IO) {
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
                            .onFailure {
                                it.printStackTrace()
                                exceptionDao.insertException(it)
                            }
                            .getOrNull()
                    }
                }
            }

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/zip"
                putExtra(Intent.EXTRA_STREAM, f)
                putExtra(Intent.EXTRA_TITLE, "Sharing Extensions")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(
                createChooser(intent, "Share your extensions")
                    .apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) },
                null
            )
        }.onFailure {
            it.printStackTrace()
            exceptionDao.insertException(it)
        }
    }
}