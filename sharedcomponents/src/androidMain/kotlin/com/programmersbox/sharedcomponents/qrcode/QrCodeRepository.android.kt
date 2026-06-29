package com.programmersbox.sharedcomponents.qrcode

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.Intent.createChooser
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.core.content.FileProvider
import androidx.core.content.getSystemService
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.OutputStream

actual class QrCodeRepository(
    private val context: Context,
) {
    private val clipboardManager by lazy {
        context.getSystemService<ClipboardManager>()
    }
    val scanner = BarcodeScanning.getClient()
    actual suspend fun getInfoFromQRCode(
        bitmap: ImageBitmap,
    ): Result<List<String>> = runCatching { InputImage.fromBitmap(bitmap.asAndroidBitmap(), 0) }
        .mapCatching { scanner.process(it).await() }
        .mapCatching { barcodes -> barcodes.mapNotNull { it.displayValue } }

    actual suspend fun shareUrl(url: String, title: String) {
        runCatching {
            context.startActivity(
                createChooser(
                    Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, url)
                        putExtra(Intent.EXTRA_TITLE, title)
                    },
                    "Share"
                ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            )
        }
    }

    actual suspend fun shareImage(
        bitmap: ImageBitmap,
        title: String,
    ) {
        // Save into the app's internal storage (not visible to gallery apps) then share
        runCatching { bitmap.asAndroidBitmap().saveToInternal(title) }
            .onSuccess { shareBitmap(context, it, title) }
    }

    actual suspend fun saveImage(bitmap: ImageBitmap, title: String) {
        runCatching { bitmap.asAndroidBitmap().saveToDisk(title) }
            .onSuccess { Toast.makeText(context, "Qr Code Saved!", Toast.LENGTH_LONG).show() }
    }

    private suspend fun Bitmap.saveToDisk(title: String): Uri {
        return suspendCancellableCoroutine {
            it.resumeWith(
                runCatching {
                    saveImageModernWay(
                        contentResolver = context.contentResolver,
                        bitmap = this,
                        filename = title
                    )!!
                }
            )
        }
    }

    fun saveImageModernWay(
        contentResolver: ContentResolver,
        bitmap: Bitmap,
        filename: String,
    ): Uri? {
        val imageCollection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.DESCRIPTION, "Qr Code")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        // Insert a new item into the MediaStore
        val imageUri: Uri? = contentResolver.insert(imageCollection, contentValues)

        try {
            imageUri?.let { uri ->
                // Open an output stream and write the bitmap data
                val outputStream: OutputStream? = contentResolver.openOutputStream(uri)
                outputStream?.use { stream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                }

                // If on Android Q+, clear the IS_PENDING flag to make the image visible
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    contentResolver.update(uri, contentValues, null, null)
                }
                return uri
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Clean up the entry if an error occurs during writing
            if (imageUri != null) {
                contentResolver.delete(imageUri, null, null)
            }
            return null
        }

        return null
    }

    // Save bitmap into app-internal storage and return a FileProvider content URI suitable for sharing
    private suspend fun Bitmap.saveToInternal(title: String): Uri {
        val file = File(context.filesDir, "$title-${System.currentTimeMillis()}.png")
        if (!file.exists()) withContext(Dispatchers.IO) { file.createNewFile() }
        file.writeBitmap(this, Bitmap.CompressFormat.PNG, 100)

        // Use FileProvider so external apps can read the file via a content:// URI without exposing the path
        return FileProvider.getUriForFile(
            context,
            context.applicationContext.packageName + ".fileprovider",
            file
        )
    }

    private fun File.writeBitmap(bitmap: Bitmap, format: Bitmap.CompressFormat, quality: Int) {
        outputStream().use { out ->
            bitmap.compress(format, quality, out)
            out.flush()
        }
    }

    private fun shareBitmap(context: Context, uri: Uri, title: String) {
        clipboardManager?.setPrimaryClip(ClipData.newUri(context.contentResolver, title, uri))
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2)
            Toast.makeText(context, "Qr Code Copied!", Toast.LENGTH_LONG).show()
    }
}