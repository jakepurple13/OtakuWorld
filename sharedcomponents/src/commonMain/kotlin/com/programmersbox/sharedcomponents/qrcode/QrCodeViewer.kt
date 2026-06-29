package com.programmersbox.sharedcomponents.qrcode

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.alexzhirkevich.qrose.options.dsl.QrOptionsBuilderScope
import io.github.alexzhirkevich.qrose.rememberQrCodePainter
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.koin.compose.koinInject

interface QrCodeInfo {
    val title: String
    val url: String
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
inline fun <reified T : QrCodeInfo> ShareViaQrCode(
    qrCodeInfo: T,
    crossinline onClose: () -> Unit,
    crossinline customUi: @Composable () -> Unit = {},
    includeShareImage: Boolean = true,
    includeShareUrl: Boolean = true,
    includeSaveImage: Boolean = true,
    crossinline painterCustomize: QrOptionsBuilderScope.() -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberBottomSheetState(initialValue = SheetValue.Expanded)
    val onDismiss: () -> Unit = {
        scope.launch { sheetState.hide() }
        onClose()
    }

    val qrCodeRepository = koinInject<QrCodeRepository>()

    val painter = rememberQrCodePainter(
        remember { Json.encodeToString(qrCodeInfo) }
    ) {
        painterCustomize()
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

                customUi()

                if (includeShareImage) {
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
                }

                if (includeSaveImage) {
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
                }

                if (includeShareUrl) {
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
}