package com.programmersbox.sharedcomponents.qrcode

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.programmersbox.showcase.annotations.ShowcaseComponent
import io.github.alexzhirkevich.qrose.options.dsl.QrOptionsBuilderScope
import io.github.alexzhirkevich.qrose.rememberQrCodePainter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import com.programmersbox.sharedcomponents.notifier.InAppNotifier
import org.koin.compose.koinInject

interface QrCodeInfo {
    val title: String
    val url: String
}

/** Colors of the share card, which is the part of the sheet captured into the shared image */
@Immutable
data class QrShareCardColors(
    val container: Color,
    val content: Color,
)

object QrShareDefaults {
    @Composable
    fun cardColors() = QrShareCardColors(
        container = MaterialTheme.colorScheme.surfaceContainerHighest,
        content = MaterialTheme.colorScheme.onSurface,
    )

    /** How long Share and Save wait for [ShareViaQrCode]'s `captureReady` before capturing anyway */
    const val CAPTURE_WAIT_MS = 3_000L
}

/**
 * Bottom sheet that shows [qrCodeInfo] as a QR code on a share card. Share and Save export only the
 * card, so everything in [header], [cardDetails] and [hint] ends up in the image, while [customUi]
 * (options) and [notice] stay on screen only.
 *
 * @param captureReady false while something on the card (such as a cover) is still loading. Share and
 * Save wait for it, up to [QrShareDefaults.CAPTURE_WAIT_MS].
 */
@Composable
inline fun <reified T : QrCodeInfo> ShareViaQrCode(
    qrCodeInfo: T,
    noinline onClose: () -> Unit,
    noinline header: @Composable () -> Unit = { QrShareTitle(qrCodeInfo.title) },
    noinline cardDetails: @Composable () -> Unit = {},
    hint: String? = null,
    noinline customUi: @Composable () -> Unit = {},
    noinline notice: @Composable () -> Unit = {},
    cardColors: QrShareCardColors = QrShareDefaults.cardColors(),
    captureReady: Boolean = true,
    includeShareImage: Boolean = true,
    includeSaveImage: Boolean = true,
    includeCopyUrl: Boolean = true,
    includeShareUrl: Boolean = true,
    crossinline painterCustomize: QrOptionsBuilderScope.() -> Unit = {},
) {
    val painter = rememberQrCodePainter(
        remember(qrCodeInfo) { Json.encodeToString(qrCodeInfo) }
    ) {
        painterCustomize()
    }

    QrShareSheet(
        qrPainter = painter,
        title = qrCodeInfo.title,
        url = qrCodeInfo.url,
        onClose = onClose,
        header = header,
        cardDetails = cardDetails,
        hint = hint,
        options = customUi,
        notice = notice,
        cardColors = cardColors,
        captureReady = captureReady,
        includeShareImage = includeShareImage,
        includeSaveImage = includeSaveImage,
        includeCopyUrl = includeCopyUrl,
        includeShareUrl = includeShareUrl,
    )
}

@Composable
fun QrShareTitle(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        textAlign = TextAlign.Center,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.fillMaxWidth()
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrShareSheet(
    qrPainter: Painter,
    title: String,
    url: String,
    onClose: () -> Unit,
    header: @Composable () -> Unit,
    cardDetails: @Composable () -> Unit,
    hint: String?,
    options: @Composable () -> Unit,
    notice: @Composable () -> Unit,
    cardColors: QrShareCardColors,
    captureReady: Boolean,
    includeShareImage: Boolean,
    includeSaveImage: Boolean,
    includeCopyUrl: Boolean,
    includeShareUrl: Boolean,
) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val qrCodeRepository = koinInject<QrCodeRepository>()

    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current
    val notifier = koinInject<InAppNotifier>()

    var waitedOut by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(QrShareDefaults.CAPTURE_WAIT_MS)
        waitedOut = true
    }
    val canCapture = captureReady || waitedOut

    val graphicsLayer = rememberGraphicsLayer()

    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState,
    ) {
        Box {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp)
            ) {
                Surface(
                    color = cardColors.container,
                    contentColor = cardColors.content,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .widthIn(max = 360.dp)
                        .fillMaxWidth()
                        .drawWithContent {
                            // Record the card so Share and Save export exactly what is on screen
                            graphicsLayer.record { this@drawWithContent.drawContent() }
                            drawLayer(graphicsLayer)
                        }
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(14.dp)
                    ) {
                        header()
                        // Always dark modules on white, whatever the theme, so every scanner reads it
                        Image(
                            painter = qrPainter,
                            contentDescription = "QR code",
                            modifier = Modifier
                                .background(Color.White, RoundedCornerShape(16.dp))
                                .padding(12.dp)
                                .size(200.dp)
                        )
                        cardDetails()
                        hint?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.labelSmall,
                                textAlign = TextAlign.Center,
                                color = cardColors.content.copy(alpha = .8f),
                            )
                        }
                    }
                }

                options()
                notice()

                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (includeShareImage) {
                        QrShareAction(
                            label = "Share",
                            icon = Icons.Default.Share,
                            primary = true,
                            busy = !canCapture,
                            onClick = {
                                scope.launch {
                                    qrCodeRepository.shareImage(graphicsLayer.toImageBitmap(), title)
                                }
                            }
                        )
                    }
                    if (includeSaveImage) {
                        QrShareAction(
                            label = "Save",
                            icon = Icons.Default.Download,
                            busy = !canCapture,
                            onClick = {
                                scope.launch {
                                    qrCodeRepository.saveImage(graphicsLayer.toImageBitmap(), title)
                                }
                            }
                        )
                    }
                    if (includeCopyUrl) {
                        QrShareAction(
                            label = "Copy link",
                            icon = Icons.Default.ContentCopy,
                            onClick = {
                                clipboard.setText(AnnotatedString(url))
                                if (!systemConfirmsCopy) notifier.success("Link copied")
                            }
                        )
                    }
                    if (includeShareUrl) {
                        QrShareAction(
                            label = "Share link",
                            icon = Icons.Default.Link,
                            onClick = { scope.launch { qrCodeRepository.shareUrl(url, title) } }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QrShareAction(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    primary: Boolean = false,
    busy: Boolean = false,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        val content: @Composable () -> Unit = {
            if (busy) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Icon(icon, contentDescription = null)
            }
        }
        val buttonModifier = Modifier.size(56.dp)
        if (primary) {
            FilledIconButton(
                onClick = onClick,
                enabled = !busy,
                modifier = buttonModifier,
                shapes = IconButtonDefaults.shapes(),
                content = content
            )
        } else {
            FilledTonalIconButton(
                onClick = onClick,
                enabled = !busy,
                modifier = buttonModifier,
                content = content,
                shapes = IconButtonDefaults.shapes(),
            )
        }
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

@ShowcaseComponent(
    name = "View QR code",
    description = "View QR code.",
    group = "BottomSheet"
)
@Composable
fun QrCodeViewerSample() {
    var showShareDialog by remember { mutableStateOf(false) }

    @Serializable
    data class QrCodeInfoSample(
        override val title: String,
        override val url: String,
    ) : QrCodeInfo

    if (showShareDialog) {
        ShareViaQrCode(
            qrCodeInfo = QrCodeInfoSample("Hello", "World"),
            onClose = { showShareDialog = false },
            hint = "Scan in OtakuWorld › Scan QR code",
        )
    }

    Button(
        onClick = { showShareDialog = true },
        modifier = Modifier.fillMaxWidth(.75f)
    ) { Text("View QR code") }
}
