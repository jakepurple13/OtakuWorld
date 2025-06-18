package com.programmersbox.kmpuiviews.repository

import android.content.Context
import android.content.Intent
import android.content.Intent.createChooser
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import org.jetbrains.compose.resources.getString
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.share_item
import java.io.File
import kotlin.coroutines.resume

actual class QrCodeRepository(
    private val context: Context,
) {
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
                    getString(Res.string.share_item, title)
                ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            )
        }
    }

    actual suspend fun shareImage(
        bitmap: ImageBitmap,
        title: String,
    ) {
        runCatching { bitmap.asAndroidBitmap().saveToDisk(title) }
            .onSuccess { shareBitmap(context, it, title) }
    }

    actual suspend fun saveImage(bitmap: ImageBitmap, title: String) {
        runCatching { bitmap.asAndroidBitmap().saveToDisk(title) }
            .onSuccess { Toast.makeText(context, "Qr Code Saved!", Toast.LENGTH_LONG).show() }
    }

    //Copied from https://github.com/android/snippets/blob/latest/compose/snippets/src/main/java/com/example/compose/snippets/graphics/AdvancedGraphicsSnippets.kt#L123
    private suspend fun Bitmap.saveToDisk(title: String): Uri {
        val file = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            "$title-${System.currentTimeMillis()}.png"
        )

        file.writeBitmap(this, Bitmap.CompressFormat.PNG, 100)

        return scanFilePath(context, file.path) ?: throw Exception("File could not be saved")
    }

    private suspend fun scanFilePath(context: Context, filePath: String): Uri? {
        return suspendCancellableCoroutine { continuation ->
            MediaScannerConnection.scanFile(
                context,
                arrayOf(filePath),
                arrayOf("image/png")
            ) { _, scannedUri ->
                if (scannedUri == null) {
                    continuation.cancel(Exception("File $filePath could not be scanned"))
                } else {
                    continuation.resume(scannedUri)
                }
            }
        }
    }

    private fun File.writeBitmap(bitmap: Bitmap, format: Bitmap.CompressFormat, quality: Int) {
        outputStream().use { out ->
            bitmap.compress(format, quality, out)
            out.flush()
        }
    }

    private fun shareBitmap(context: Context, uri: Uri, title: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TITLE, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            createChooser(intent, "Share your image")
                .apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) },
            null
        )
    }
}
