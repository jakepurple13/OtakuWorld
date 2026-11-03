package com.programmersbox.kmpuiviews.presentation.settings.qrcode

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kmpalette.generatePalette
import com.programmersbox.datastore.ColorBlindnessType
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.datastore.rememberSwatchType
import com.programmersbox.datastore.rememberUseCoverColorsInQrCode
import com.programmersbox.datastore.rememberUseLogoInQrCode
import com.programmersbox.kmpuiviews.theme.generateColorScheme
import com.programmersbox.kmpuiviews.theme.rememberSeedColorScheme
import com.programmersbox.kmpuiviews.utils.AppConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.favoritesdatabase.NotificationItem
import com.programmersbox.kmpuiviews.painterLogo
import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.kmpuiviews.presentation.components.LoadingDialog
import com.programmersbox.kmpuiviews.presentation.components.colorFilterBlind
import com.programmersbox.kmpuiviews.utils.LocalNavActions
import com.programmersbox.kmpuiviews.utils.LocalSourcesRepository
import com.programmersbox.kmpuiviews.utils.composables.imageloaders.ImageLoaderChoice
import com.programmersbox.kmpuiviews.utils.dispatchIo
import com.programmersbox.sharedcomponents.qrcode.ScanQrCode
import com.programmersbox.sharedcomponents.qrcode.ShareViaQrCode
import com.programmersbox.sharedcomponents.qrcode.qrDockTransform
import io.github.alexzhirkevich.qrose.options.QrLogoPadding
import io.github.alexzhirkevich.qrose.options.QrLogoShape
import io.github.alexzhirkevich.qrose.options.circle
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject

@Serializable
data class QrCodeInfo(
    override val title: String,
    override val url: String,
    val imageUrl: String,
    val apiService: String,
) : com.programmersbox.sharedcomponents.qrcode.QrCodeInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareViaQrCode(
    title: String,
    url: String,
    imageUrl: String,
    apiService: String,
    onClose: () -> Unit,
) {
    var includeLogo by rememberUseLogoInQrCode()
    var useCoverColors by rememberUseCoverColorsInQrCode()
    val swatchType by rememberSwatchType()
    val appName = koinInject<AppConfig>().appName
    val colorBlindness: ColorBlindnessType by koinInject<NewSettingsHandling>().rememberColorBlindType()
    val colorFilter by remember { derivedStateOf { colorFilterBlind(colorBlindness) } }
    val logoPainter = painterLogo()
    // apiService is the lookup key (e.g. "mihon:6084…"), so show the source's readable name instead
    val sourceName = LocalSourcesRepository.current
        .toSourceByApiServiceName(apiService)
        ?.apiService
        ?.displayName
        ?: apiService

    var cover by remember(imageUrl) { mutableStateOf<ImageBitmap?>(null) }
    // Same palette and swatch Details uses, so from Details the scheme comes straight from SeedSchemeCache
    val seedColor by produceState<Int?>(null, cover, swatchType) {
        val bitmap = cover ?: return@produceState
        value = withContext(Dispatchers.Default) {
            runCatching { swatchType.swatch(bitmap.generatePalette())?.rgb }.getOrNull()
        }
    }

    val content: @Composable () -> Unit = {
        ShareViaQrCode<QrCodeInfo>(
            qrCodeInfo = QrCodeInfo(
                title = title,
                url = url,
                imageUrl = imageUrl,
                apiService = apiService,
            ),
            onClose = onClose,
            captureReady = cover != null,
            hint = "Scan in $appName › Settings › Scan QR Code",
            header = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ImageLoaderChoice(
                        imageUrl = imageUrl,
                        name = title,
                        placeHolder = { logoPainter },
                        contentScale = ContentScale.Crop,
                        colorFilter = colorFilter,
                        onImageSet = { cover = it },
                        modifier = Modifier
                            .size(44.dp, 62.dp)
                            .clip(MaterialTheme.shapes.small)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            title,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            "$sourceName · $appName",
                            style = MaterialTheme.typography.labelMedium,
                            color = LocalContentColor.current.copy(alpha = .8f),
                        )
                    }
                }
            },
            customUi = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = includeLogo,
                        onClick = { includeLogo = !includeLogo },
                        label = { Text("Logo") },
                        leadingIcon = if (includeLogo) {
                            { Icon(Icons.Default.Check, null, Modifier.size(FilterChipDefaults.IconSize)) }
                        } else null,
                    )
                    FilterChip(
                        selected = useCoverColors,
                        onClick = { useCoverColors = !useCoverColors },
                        label = { Text("Cover colors") },
                        leadingIcon = if (useCoverColors) {
                            { Icon(Icons.Default.Check, null, Modifier.size(FilterChipDefaults.IconSize)) }
                        } else null,
                    )
                }
            },
            painterCustomize = {
                if (includeLogo) {
                    logo {
                        painter = logoPainter
                        padding = QrLogoPadding.Natural(.1f)
                        shape = QrLogoShape.circle()
                    }
                }
            },
        )
    }

    // From Details the surrounding theme is already cover-seeded, so it is the right placeholder until
    // our own scheme is ready. With cover colors off, Details' theming is undone with the app theme.
    val appScheme = generateColorScheme(koinInject<NewSettingsHandling>())
    MaterialTheme(
        colorScheme = rememberSeedColorScheme(
            seedColor = seedColor.takeIf { useCoverColors },
            fallback = if (useCoverColors) MaterialTheme.colorScheme else appScheme,
        ),
        content = content
    )
}

private enum class QrDockAction { None, Open, SaveForLater }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ScanQrCode() {
    val navController = LocalNavActions.current
    val dao: ItemDao = koinInject()
    val info = LocalSourcesRepository.current
    val colorBlindness: ColorBlindnessType by koinInject<NewSettingsHandling>().rememberColorBlindType()
    val colorFilter by remember { derivedStateOf { colorFilterBlind(colorBlindness) } }
    val scope = rememberCoroutineScope()
    var showLoadingDialog by remember { mutableStateOf(false) }

    LoadingDialog(
        showLoadingDialog = showLoadingDialog,
        onDismissRequest = { showLoadingDialog = false }
    )

    ScanQrCode<QrCodeInfo>(
        onRemove = { navController.remove(Screen.ScanQrCodeScreen) },
        hint = "Point at a shared QR code",
        invalidCodeText = "Not an OtakuWorld QR code. Scan one from a Details share.",
        dockContent = { qrCodeInfo, scanAgain ->
            val sourceInstalled = qrCodeInfo
                ?.apiService
                ?.let { info.toSourceByApiServiceName(it) } != null

            AnimatedContent(
                targetState = qrCodeInfo,
                transitionSpec = { qrDockTransform() },
                contentKey = { it?.url },
            ) { target ->
                QrScanResultRow(
                    info = target,
                    sourceInstalled = target == null || sourceInstalled,
                    colorFilter = colorFilter,
                )
            }

            AnimatedContent(
                targetState = when {
                    qrCodeInfo == null -> QrDockAction.None
                    sourceInstalled -> QrDockAction.Open
                    else -> QrDockAction.SaveForLater
                },
                transitionSpec = { qrDockTransform() },
            ) { action ->
                if (action == QrDockAction.None) {
                    Button(
                        onClick = {},
                        enabled = false,
                        shapes = ButtonDefaults.shapes(),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Open") }
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = scanAgain,
                            shapes = ButtonDefaults.shapes(),
                            modifier = Modifier.weight(1f)
                        ) { Text("Scan again") }

                        if (action == QrDockAction.Open) {
                            Button(
                                onClick = {
                                    val qr = qrCodeInfo ?: return@Button
                                    scope.launch {
                                        info.toSourceByApiServiceName(qr.apiService)
                                            ?.apiService
                                            ?.getSourceByUrlFlow(qr.url)
                                            ?.dispatchIo()
                                            ?.onStart { showLoadingDialog = true }
                                            ?.catch { showLoadingDialog = false }
                                            ?.onEach { m ->
                                                showLoadingDialog = false
                                                navController.remove(Screen.ScanQrCodeScreen)
                                                navController.details(m)
                                            }
                                            ?.collect()
                                    }
                                },
                                shapes = ButtonDefaults.shapes(),
                                modifier = Modifier.weight(1f)
                            ) { Text("Open") }
                        } else {
                            Button(
                                onClick = {
                                    val qr = qrCodeInfo ?: return@Button
                                    scope.launch {
                                        dao.insertNotification(
                                            NotificationItem(
                                                id = qr.toString().hashCode(),
                                                url = qr.url,
                                                summaryText = "Waiting for source",
                                                notiTitle = qr.title,
                                                imageUrl = qr.imageUrl,
                                                source = qr.apiService,
                                                contentTitle = qr.title
                                            )
                                        )
                                    }.invokeOnCompletion { navController.remove(Screen.ScanQrCodeScreen) }
                                },
                                shapes = ButtonDefaults.shapes(),
                                modifier = Modifier.weight(1f)
                            ) { Text("Save for later") }
                        }
                    }
                }
            }
        }
    )
}

@Composable
private fun QrScanResultRow(
    info: QrCodeInfo?,
    sourceInstalled: Boolean,
    colorFilter: ColorFilter?,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        val coverModifier = Modifier
            .size(44.dp, 66.dp)
            .clip(MaterialTheme.shapes.small)

        if (info == null) {
            Box(coverModifier.background(MaterialTheme.colorScheme.surfaceContainer))
            Column {
                Text(
                    "Nothing scanned yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "The title shows up here",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            ImageLoaderChoice(
                imageUrl = info.imageUrl,
                name = info.title,
                placeHolder = { painterLogo() },
                colorFilter = colorFilter,
                modifier = coverModifier
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Surface(
                    color = if (sourceInstalled) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = if (sourceInstalled) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onTertiaryContainer,
                    shape = MaterialTheme.shapes.extraSmall,
                ) {
                    Text(
                        if (sourceInstalled) info.apiService else "${info.apiService} · not installed",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
                Text(
                    info.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    info.url,
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
