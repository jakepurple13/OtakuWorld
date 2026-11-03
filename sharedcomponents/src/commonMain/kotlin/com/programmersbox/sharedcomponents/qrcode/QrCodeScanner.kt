package com.programmersbox.sharedcomponents.qrcode

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.programmersbox.showcase.annotations.ShowcaseComponent
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.dialogs.compose.util.toImageBitmap
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.koin.compose.viewmodel.koinViewModel

/**
 * Bottom sheet that scans a QR code holding a JSON-encoded [T].
 *
 * The camera fills the top of the sheet. [dockContent] renders in the dock pinned to the bottom,
 * and receives the scanned value (`null` until something is read) plus a `scanAgain` callback
 * that clears it and resumes scanning.
 */
@Composable
inline fun <reified T> ScanQrCode(
    noinline onRemove: () -> Unit,
    noinline dockContent: @Composable ColumnScope.(info: T?, scanAgain: () -> Unit) -> Unit,
    hint: String = "Point at a QR code",
    invalidCodeText: String = "This QR code can't be read here",
    viewModel: QrCodeViewModel = koinViewModel(),
) {
    ScanQrCodeSheet(
        decode = { scan -> runCatching { Json.decodeFromString<T>(scan) } },
        decodeImage = { bitmap -> viewModel.scanQrCodeFromImage<T>(bitmap) },
        onRemove = onRemove,
        hint = hint,
        invalidCodeText = invalidCodeText,
        dockContent = dockContent,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> ScanQrCodeSheet(
    decode: (String) -> Result<T>,
    decodeImage: suspend (ImageBitmap) -> Result<T>,
    onRemove: () -> Unit,
    hint: String,
    invalidCodeText: String,
    dockContent: @Composable ColumnScope.(info: T?, scanAgain: () -> Unit) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberBottomSheetState(initialValue = SheetValue.Expanded)
    val onDismiss: () -> Unit = {
        scope.launch { sheetState.hide() }
            .invokeOnCompletion { onRemove() }
    }

    var qrCodeInfo by remember { mutableStateOf<T?>(null) }
    var showInvalid by remember { mutableStateOf(false) }
    var torchState by remember { mutableStateOf(false) }
    var dropHovering by remember { mutableStateOf(false) }
    val locked = qrCodeInfo != null

    LaunchedEffect(showInvalid) {
        if (showInvalid) {
            delay(3000)
            showInvalid = false
        }
    }

    val onResult: (Result<T>) -> Unit = { result ->
        result
            .onSuccess {
                showInvalid = false
                qrCodeInfo = it
            }
            .onFailure { showInvalid = true }
    }
    val onImage: (ImageBitmap) -> Unit = { bitmap -> scope.launch { onResult(decodeImage(bitmap)) } }

    val filePicker = rememberFilePickerLauncher(type = FileKitType.Image) { file ->
        if (file == null) return@rememberFilePickerLauncher
        scope.launch {
            runCatching { file.toImageBitmap() }
                .onSuccess(onImage)
                .onFailure { showInvalid = true }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        sheetState = sheetState,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 820.dp)
                .fillMaxHeight(.9f)
        ) {
            Text(
                "Scan QR code",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Black)
                    .qrImageDropTarget(
                        onImage = onImage,
                        onHoverChange = { dropHovering = it },
                    )
            ) {
                CameraView(
                    onScan = { scan -> if (qrCodeInfo == null) onResult(decode(scan)) },
                    torchState = torchState,
                    unavailableContent = {
                        QrImageDropZone(
                            hovering = dropHovering,
                            onChooseImage = { filePicker.launch() },
                        )
                    },
                    overlay = { hasTorch ->
                        QrViewfinderOverlay(
                            locked = locked,
                            hint = hint,
                            hasTorch = hasTorch,
                            torchOn = torchState,
                            onTorchToggle = { torchState = !torchState },
                            onChooseImage = { filePicker.launch() },
                        )
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .animateContentSize()
                        .navigationBarsPadding()
                        .padding(16.dp)
                ) {
                    AnimatedVisibility(
                        visible = showInvalid,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically(),
                    ) { QrInvalidNotice(invalidCodeText) }

                    dockContent(qrCodeInfo) {
                        qrCodeInfo = null
                        showInvalid = false
                    }
                }
            }
        }
    }
}

/**
 * Transition for content that swaps inside the scanner dock: new content fades in and rises,
 * old content fades out.
 */
fun <S> AnimatedContentTransitionScope<S>.qrDockTransform(): ContentTransform =
    (fadeIn(tween(300, delayMillis = 60)) + slideInVertically(tween(300, delayMillis = 60)) { it / 4 })
        .togetherWith(fadeOut(tween(150)))
        .using(SizeTransform(clip = false))

@Composable
private fun QrInvalidNotice(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Icon(Icons.Default.Error, null, modifier = Modifier.size(18.dp))
            Text(text, style = MaterialTheme.typography.bodySmall)
        }
    }
}

private val LockedColor = Color(0xFF8EF0B0)
private val FabContainer = Color.Black.copy(alpha = .55f)

@Composable
private fun BoxScope.QrViewfinderOverlay(
    locked: Boolean,
    hint: String,
    hasTorch: Boolean,
    torchOn: Boolean,
    onTorchToggle: () -> Unit,
    onChooseImage: () -> Unit,
) {
    val bracketColor by animateColorAsState(if (locked) LockedColor else Color.White)
    val pulse = remember { Animatable(1f) }
    LaunchedEffect(locked) {
        if (locked) {
            pulse.animateTo(.9f, tween(160))
            pulse.animateTo(1f, tween(260))
        }
    }

    val transition = rememberInfiniteTransition()
    val scanProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Reverse)
    )

    Canvas(
        Modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = pulse.value
                scaleY = pulse.value
            }
    ) {
        val side = size.minDimension * .68f
        val topLeft = Offset((size.width - side) / 2, (size.height - side) / 2)
        val arm = 28.dp.toPx()
        val radius = 14.dp.toPx()
        val stroke = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
        val left = topLeft.x
        val top = topLeft.y
        val right = left + side
        val bottom = top + side
        // (corner x, corner y, direction along x, direction along y)
        listOf(
            listOf(left, top, 1f, 1f),
            listOf(right, top, -1f, 1f),
            listOf(left, bottom, 1f, -1f),
            listOf(right, bottom, -1f, -1f),
        ).forEach { (cx, cy, dx, dy) ->
            val path = Path().apply {
                moveTo(cx, cy + dy * arm)
                lineTo(cx, cy + dy * radius)
                quadraticTo(cx, cy, cx + dx * radius, cy)
                lineTo(cx + dx * arm, cy)
            }
            drawPath(path, color = bracketColor, style = stroke)
        }

        if (!locked) {
            val y = topLeft.y + side * (.06f + .88f * scanProgress)
            drawLine(
                brush = Brush.horizontalGradient(
                    listOf(Color.Transparent, Color(0xFFC9CDFF), Color.Transparent),
                    startX = topLeft.x,
                    endX = topLeft.x + side,
                ),
                start = Offset(topLeft.x, y),
                end = Offset(topLeft.x + side, y),
                strokeWidth = 2.dp.toPx(),
            )
        }
    }

    AnimatedVisibility(
        visible = !locked,
        enter = fadeIn() + scaleIn(initialScale = .6f),
        exit = fadeOut() + scaleOut(targetScale = .6f),
        modifier = Modifier
            .align(Alignment.TopStart)
            .padding(12.dp)
    ) {
        IconButton(
            onClick = onChooseImage,
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = FabContainer,
                contentColor = Color.White,
            )
        ) { Icon(Icons.Default.Image, contentDescription = "Scan from image") }
    }

    if (hasTorch) {
        AnimatedVisibility(
            visible = !locked,
            enter = fadeIn() + scaleIn(initialScale = .6f),
            exit = fadeOut() + scaleOut(targetScale = .6f),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
        ) {
            IconButton(
                onClick = onTorchToggle,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = if (torchOn) Color.White else FabContainer,
                    contentColor = if (torchOn) Color.Black else Color.White,
                )
            ) {
                Icon(
                    if (torchOn) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                    contentDescription = if (torchOn) "Turn off flashlight" else "Turn on flashlight"
                )
            }
        }
    }

    AnimatedContent(
        targetState = if (locked) "Code read" else hint,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(12.dp)
    ) { text ->
        Text(
            text,
            color = Color.White,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier
                .background(Color.Black.copy(alpha = .55f), RoundedCornerShape(50))
                .padding(horizontal = 12.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun QrImageDropZone(
    hovering: Boolean,
    onChooseImage: () -> Unit,
) {
    val borderColor by animateColorAsState(
        if (hovering) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    )
    val fill by animateColorAsState(
        if (hovering) MaterialTheme.colorScheme.primary.copy(alpha = .08f) else MaterialTheme.colorScheme.surface
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(fill)
            .border(
                width = 1.5.dp,
                color = borderColor,
                shape = RoundedCornerShape(24.dp),
            )
            .padding(20.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                Icons.Default.Image,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
            Text(
                "Drop an image with a QR code",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
            )
            Text(
                "No camera found",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            FilledTonalButton(
                onClick = onChooseImage,
                modifier = Modifier.padding(top = 6.dp)
            ) { Text("Choose image") }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun QrCodeLoadingDialog(
    showLoadingDialog: Boolean,
    onDismissRequest: () -> Unit,
) {
    if (showLoadingDialog) {
        Dialog(
            onDismissRequest = onDismissRequest,
            DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(100.dp)
                    .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(28.0.dp))
            ) {
                Column {
                    CircularWavyProgressIndicator(
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Text(text = "Loading...", Modifier.align(Alignment.CenterHorizontally))
                }
            }
        }
    }
}

/**
 * Camera preview that reports decoded QR strings through [onScan].
 *
 * [unavailableContent] replaces the preview when there is no usable camera (none attached, or
 * permission denied). [overlay] draws over a live preview and is told whether a torch exists.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
expect fun CameraView(
    onScan: (String) -> Unit,
    torchState: Boolean,
    unavailableContent: @Composable () -> Unit,
    overlay: @Composable BoxScope.(hasTorch: Boolean) -> Unit,
    modifier: Modifier = Modifier,
)

/** Accepts image files dropped onto this element. Only does anything on desktop. */
expect fun Modifier.qrImageDropTarget(
    onImage: (ImageBitmap) -> Unit,
    onHoverChange: (Boolean) -> Unit,
): Modifier

@ShowcaseComponent(
    name = "Scan QR code",
    description = "Scan QR code.",
    group = "BottomSheet"
)
@Composable
fun QrCodeScannerSample() {
    var showShareDialog by remember { mutableStateOf(false) }
    if (showShareDialog) {
        ScanQrCode<String>(
            onRemove = { showShareDialog = false },
            dockContent = { info, scanAgain ->
                Text(info ?: "Nothing scanned yet")
                Button(
                    onClick = scanAgain,
                    enabled = info != null,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Scan again") }
            }
        )
    }

    Button(
        onClick = { showShareDialog = true },
        modifier = Modifier.fillMaxWidth(.75f)
    ) { Text("Scan QR code") }
}
