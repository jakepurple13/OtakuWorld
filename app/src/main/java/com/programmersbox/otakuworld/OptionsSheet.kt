package com.programmersbox.otakuworld

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.otakuworld.info.ComposableUtils
import com.programmersbox.otakuworld.textflow.TextFlow
import com.skydoves.landscapist.glide.GlideImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

interface OptionsSheetScope {
    fun dismiss()

    @Composable
    fun OptionsItem(
        title: String,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        trailingContent: (@Composable () -> Unit)? = null,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Card(
                onClick = onClick,
                colors = CardDefaults.cardColors(
                    containerColor = Color.Transparent
                ),
                modifier = modifier
            ) {
                ListItem(
                    headlineContent = { Text(title) },
                    trailingContent = trailingContent,
                    colors = ListItemDefaults.colors(
                        containerColor = Color.Transparent
                    )
                )
            }

            HorizontalDivider()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun optionsSheet(
    scope: CoroutineScope = rememberCoroutineScope(),
    sheet: SheetState = rememberModalBottomSheetState(true),
    moreContent: @Composable OptionsSheetScope.() -> Unit = {},
): MutableState<Boolean> {
    val show = remember { mutableStateOf(false) }
    val optionsSheetScope = remember {
        object : OptionsSheetScope {
            override fun dismiss() {
                scope.launch { sheet.hide() }
                    .invokeOnCompletion { show.value = false }
            }
        }
    }

    if (show.value) {
        ModalBottomSheet(
            onDismissRequest = { show.value = false },
            sheetState = sheet,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .animateContentSize(),
            ) {
                with(optionsSheetScope) {
                    moreContent()
                }
            }
        }
    }

    return show
}

interface OptionsSheetValues {
    val imageUrl: String
    val title: String
    val description: String
    val serviceName: String
    val url: String
}

class KmpItemModelOptionsSheet(
    val itemModel: DbModel,
    override val imageUrl: String = itemModel.imageUrl,
    override val title: String = itemModel.title,
    override val description: String = itemModel.description,
    override val serviceName: String = itemModel.source,
    override val url: String = itemModel.url,
) : OptionsSheetValues

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun optionsKmpSheet(
    scope: CoroutineScope = rememberCoroutineScope(),
    sheetState: SheetState = rememberModalBottomSheetState(true),
    moreContent: @Composable OptionsSheetScope.(KmpItemModelOptionsSheet) -> Unit = {},
): MutableState<DbModel?> {
    val itemInfo = remember { mutableStateOf<DbModel?>(null) }

    itemInfo
        .value
        ?.let { KmpItemModelOptionsSheet(itemModel = it) }
        ?.let { item ->
            OptionsSheet(
                scope = scope,
                sheet = sheetState,
                optionsSheetValues = item,
                onDismiss = { itemInfo.value = null },
                moreContent = moreContent
            )
        }

    return itemInfo
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T : OptionsSheetValues> OptionsSheet(
    optionsSheetValues: T,
    onDismiss: () -> Unit,
    scope: CoroutineScope = rememberCoroutineScope(),
    sheet: SheetState = rememberModalBottomSheetState(true),
    moreContent: @Composable OptionsSheetScope.(T) -> Unit = {},
) {
    val optionsSheetScope = remember(onDismiss) {
        object : OptionsSheetScope {
            override fun dismiss() {
                scope.launch { sheet.hide() }
                    .invokeOnCompletion { onDismiss() }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheet,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .animateContentSize(),
        ) {
            with(optionsSheetScope) {
                OptionsItems(
                    optionsSheetValues = optionsSheetValues,
                    moreContent = moreContent
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T : OptionsSheetValues> OptionsSheet(
    optionsSheetValuesList: List<T>,
    onDismiss: () -> Unit,
    scope: CoroutineScope = rememberCoroutineScope(),
    sheet: SheetState = rememberModalBottomSheetState(true),
    moreContent: @Composable OptionsSheetScope.(T) -> Unit = {},
) {
    val optionsSheetScope = remember(onDismiss) {
        object : OptionsSheetScope {
            override fun dismiss() {
                scope.launch { sheet.hide() }
                    .invokeOnCompletion { onDismiss() }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheet,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .animateContentSize(),
        ) {
            optionsSheetValuesList.forEach {
                var showInfo by remember { mutableStateOf(optionsSheetValuesList.size == 1) }
                OutlinedCard(
                    modifier = Modifier.animateContentSize()
                ) {
                    OutlinedCard(
                        onClick = { showInfo = !showInfo },
                    ) {
                        ListItem(
                            headlineContent = { Text(it.title) },
                            overlineContent = { Text(it.serviceName) },
                            trailingContent = {
                                Icon(
                                    if (showInfo) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                    null
                                )
                            },
                            colors = ListItemDefaults.colors(
                                containerColor = Color.Transparent
                            )
                        )
                    }
                    if (showInfo) {
                        with(optionsSheetScope) {
                            OptionsItems(
                                optionsSheetValues = it,
                                moreContent = moreContent
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T : OptionsSheetValues> OptionsSheetScope.OptionsItems(
    optionsSheetValues: T,
    moreContent: @Composable OptionsSheetScope.(T) -> Unit = {},
) {
    val imageUrl = optionsSheetValues.imageUrl
    val title = optionsSheetValues.title
    val description = optionsSheetValues.description
    val serviceName = optionsSheetValues.serviceName
    val url = optionsSheetValues.url

    val listItemColors = ListItemDefaults.colors()

    TextFlow(
        text = buildAnnotatedString {
            withStyle(
                MaterialTheme.typography.labelSmall
                    .copy(color = listItemColors.overlineColor)
                    .toSpanStyle()
            ) { appendLine(serviceName) }

            withStyle(
                MaterialTheme.typography.bodyLarge
                    .copy(color = listItemColors.headlineColor)
                    .toSpanStyle()
            ) { appendLine(title) }

            withStyle(
                MaterialTheme.typography.bodySmall
                    .copy(color = listItemColors.supportingTextColor)
                    .toSpanStyle()
            ) { appendLine(description.trimIndent()) }
        },
        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight,
        obstacleContent = {
            GlideImage(
                imageModel = { imageUrl },
                modifier = Modifier
                    .padding(end = 16.dp)
                    .size(ComposableUtils.IMAGE_WIDTH, ComposableUtils.IMAGE_HEIGHT)
                    .clip(MaterialTheme.shapes.small)
            )
        },
        modifier = Modifier.padding(16.dp)
    )

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        HorizontalDivider()

        /*var showQr by remember { mutableStateOf(false) }
        if (showQr) {
            ShareViaQrCode(
                url = url,
                title = title,
                imageUrl = imageUrl,
                apiService = serviceName,
                onClose = { showQr = false }
            )
        }

        OptionsItem(
            title = "Share via QR Code",
            onClick = { showQr = true }
        )*/

        moreContent(optionsSheetValues)

        /*if (remember { platformRepository.hasBiometric() }) {
            Crossfade(isIncognito) { target ->
                if (target == null) {
                    OptionsItem(
                        title = "Add to Incognito",
                        onClick = {
                            biometric.authenticate(
                                onAuthenticationSucceeded = {
                                    scope.launch {
                                        dao.insertIncognitoSource(
                                            IncognitoSource(
                                                source = url,
                                                name = title,
                                                isIncognito = true
                                            )
                                        )
                                    }.invokeOnCompletion { dismiss() }
                                },
                                title = "Authentication required",
                                subtitle = "In order to add ${title}, please authenticate",
                                negativeButtonText = "Never Mind"
                            )
                        }
                    )
                } else {
                    OptionsItem(
                        title = "Remove from Incognito",
                        onClick = {
                            biometric.authenticate(
                                onAuthenticationSucceeded = {
                                    scope.launch { dao.deleteIncognitoSource(url) }
                                        .invokeOnCompletion { dismiss() }
                                },
                                title = "Authentication required",
                                subtitle = "In order to remove ${title}, please authenticate",
                                negativeButtonText = "Never Mind"
                            )
                        }
                    )
                }
            }
        } else {
            OptionsItem(
                title = "Biometrics/Security not set",
                onClick = {}
            )
        }*/
    }
}