package com.programmersbox.kmpuiviews.presentation.settings.qrcode

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.programmersbox.datastore.ColorBlindnessType
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.datastore.rememberUseLogoInQrCode
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.favoritesdatabase.NotificationItem
import com.programmersbox.kmpuiviews.painterLogo
import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.kmpuiviews.presentation.components.LoadingDialog
import com.programmersbox.kmpuiviews.presentation.components.colorFilterBlind
import com.programmersbox.kmpuiviews.utils.ComposableUtils
import com.programmersbox.kmpuiviews.utils.LocalNavActions
import com.programmersbox.kmpuiviews.utils.LocalSourcesRepository
import com.programmersbox.kmpuiviews.utils.composables.imageloaders.ImageLoaderChoice
import com.programmersbox.kmpuiviews.utils.dispatchIo
import com.programmersbox.sharedcomponents.qrcode.ScanQrCode
import com.programmersbox.sharedcomponents.qrcode.ShareViaQrCode
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
    val logoPainter = painterLogo()
    ShareViaQrCode<QrCodeInfo>(
        qrCodeInfo = QrCodeInfo(
            title = title,
            url = url,
            imageUrl = imageUrl,
            apiService = apiService,
        ),
        onClose = onClose,
        painterCustomize = {
            if (includeLogo) {
                logo {
                    painter = logoPainter
                    padding = QrLogoPadding.Natural(.1f)
                    shape = QrLogoShape.circle()
                }
            }
        },
        customUi = {
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
        }
    )
}

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
        onOpen = { qrCodeInfo ->
            scope.launch {
                info.toSourceByApiServiceName(qrCodeInfo.apiService)
                    ?.apiService
                    ?.getSourceByUrlFlow(qrCodeInfo.url)
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
        onRemove = { navController.remove(Screen.ScanQrCodeScreen) },
        customUi = { qrCodeInfo ->
            Crossfade(qrCodeInfo) { target ->
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(.75f)
                ) {
                    ListItem(
                        headlineContent = { Text(target?.title ?: "Waiting for QR code") },
                        overlineContent = { Text(target?.apiService ?: "") },
                        leadingContent = {
                            ImageLoaderChoice(
                                imageUrl = target?.imageUrl ?: "",
                                name = target?.title ?: "Waiting for QR code",
                                placeHolder = { painterLogo() },
                                colorFilter = colorFilter,
                                modifier = Modifier
                                    .size(ComposableUtils.IMAGE_WIDTH, ComposableUtils.IMAGE_HEIGHT)
                                    .clip(MaterialTheme.shapes.medium)
                            )
                        },
                        colors = ListItemDefaults.colors(
                            containerColor = Color.Transparent,
                        )
                    )
                }
            }

            val source = qrCodeInfo
                ?.apiService
                ?.let { info.toSourceByApiServiceName(it) }

            if (source == null && qrCodeInfo != null) {
                Text("Source not found. Please install the source.")

                ElevatedButton(
                    onClick = {
                        scope.launch {
                            qrCodeInfo.let {
                                dao.insertNotification(
                                    NotificationItem(
                                        id = it.toString().hashCode(),
                                        url = it.url,
                                        summaryText = "Waiting for source",
                                        notiTitle = it.title,
                                        imageUrl = it.imageUrl,
                                        source = it.apiService,
                                        contentTitle = it.title
                                    )
                                )
                            }
                        }.invokeOnCompletion { navController.remove(Screen.ScanQrCodeScreen) }
                    },
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.fillMaxWidth(.75f)
                ) { Text("Save for later") }
            }
        }
    )
}