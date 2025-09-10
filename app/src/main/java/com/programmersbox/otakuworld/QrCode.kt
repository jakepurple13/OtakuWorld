package com.programmersbox.otakuworld

import android.content.Context
import android.content.Intent
import android.content.Intent.createChooser
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.alexzhirkevich.qrose.rememberQrCodePainter
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.koin.compose.koinInject
import java.io.File
import kotlin.coroutines.resume

@Serializable
data class QrCodeInfo(
    val title: String,
    val url: String,
    val imageUrl: String,
    val apiService: String,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareViaQrCode(
    title: String,
    url: String,
    imageUrl: String,
    apiService: String,
    onClose: () -> Unit,
) {
    ShareViaQrCode(
        qrCodeInfo = QrCodeInfo(
            title = title,
            url = url,
            imageUrl = imageUrl,
            apiService = apiService,
        ),
        onClose = onClose
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ShareViaQrCode(
    qrCodeInfo: QrCodeInfo,
    onClose: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()
    val onDismiss: () -> Unit = {
        scope.launch { sheetState.hide() }
        onClose()
    }

    //var includeLogo by rememberUseLogoInQrCode()
    var includeLogo = true
    val qrCodeRepository = koinInject<QrCodeRepository>()
    //val logoPainter = painterLogo()
    val painter = rememberQrCodePainter(
        remember { Json.encodeToString(qrCodeInfo) }
    ) {
        /*if (includeLogo) {
            logo {
                painter = logoPainter
                padding = QrLogoPadding.Natural(.1f)
                shape = QrLogoShape.circle()
            }
        }*/
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        sheetState = sheetState
    ) {
        Scaffold { padding ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .padding(padding)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                val graphicsLayer = rememberGraphicsLayer()
                SelectionContainer {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.drawWithContent {
                            // call record to capture the content in the graphics layer
                            graphicsLayer.record {
                                // draw the contents of the composable into the graphics layer
                                this@drawWithContent.drawContent()
                            }
                            // draw the graphics layer on the visible canvas
                            drawLayer(graphicsLayer)
                        }
                    ) {
                        Text(
                            qrCodeInfo.title,
                            style = MaterialTheme.typography.titleLarge,
                            textAlign = TextAlign.Center
                        )
                        Image(
                            painter = painter,
                            contentDescription = "QR code",
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.onSurface, MaterialTheme.shapes.medium)
                                .padding(16.dp)
                                .animateContentSize()
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("Include Logo")

                    Switch(
                        checked = includeLogo,
                        onCheckedChange = { includeLogo = it }
                    )
                }

                FilledTonalButton(
                    onClick = {
                        scope.launch {
                            //TODO: In an update, change to copy to clipboard
                            qrCodeRepository.shareImage(
                                bitmap = graphicsLayer.toImageBitmap(),
                                title = qrCodeInfo.title
                            )
                        }
                    },
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.fillMaxWidth(.75f)
                ) { Text("Share") }

                ElevatedButton(
                    onClick = {
                        scope.launch {
                            qrCodeRepository.saveImage(
                                bitmap = graphicsLayer.toImageBitmap(),
                                title = qrCodeInfo.title
                            )
                        }
                    },
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.fillMaxWidth(.75f)
                ) { Text("Save") }

                OutlinedButton(
                    onClick = {
                        scope.launch {
                            qrCodeRepository.shareUrl(qrCodeInfo.url, qrCodeInfo.title)
                        }
                    },
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.fillMaxWidth(.75f)
                ) { Text("Share Url") }
            }
        }
    }
}

class QrCodeRepository(
    private val context: Context,
) {
    suspend fun shareUrl(url: String, title: String) {
        runCatching {
            context.startActivity(
                createChooser(
                    Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, url)
                        putExtra(Intent.EXTRA_TITLE, title)
                    },
                    "Share $title"
                ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            )
        }
    }

    suspend fun shareImage(
        bitmap: ImageBitmap,
        title: String,
    ) {
        runCatching { bitmap.asAndroidBitmap().saveToDisk(title) }
            .onSuccess { shareBitmap(context, it, title) }
    }

    suspend fun saveImage(bitmap: ImageBitmap, title: String) {
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
