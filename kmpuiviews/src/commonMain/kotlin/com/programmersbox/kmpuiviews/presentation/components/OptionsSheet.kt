package com.programmersbox.kmpuiviews.presentation.components

import androidx.compose.animation.Crossfade
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.derivedStateOf
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.programmersbox.datastore.ColorBlindnessType
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.favoritesdatabase.IncognitoSource
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.favoritesdatabase.ListDao
import com.programmersbox.favoritesdatabase.NotificationItem
import com.programmersbox.kmpmodels.KmpItemModel
import com.programmersbox.kmpuiviews.painterLogo
import com.programmersbox.kmpuiviews.presentation.components.textflow.TextFlow
import com.programmersbox.kmpuiviews.presentation.navactions.NavigationActions
import com.programmersbox.kmpuiviews.presentation.settings.lists.addtolist.ListChoiceScreen
import com.programmersbox.kmpuiviews.presentation.settings.qrcode.ShareViaQrCode
import com.programmersbox.kmpuiviews.repository.NotificationRepository
import com.programmersbox.kmpuiviews.repository.PlatformRepository
import com.programmersbox.kmpuiviews.utils.ComposableUtils
import com.programmersbox.kmpuiviews.utils.LocalNavActions
import com.programmersbox.kmpuiviews.utils.composables.imageloaders.ImageLoaderChoice
import com.programmersbox.kmpuiviews.utils.rememberBiometricPrompting
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.add_to_list
import otakuworld.kmpuiviews.generated.resources.global_search_by_name
import otakuworld.kmpuiviews.generated.resources.save_for_later

interface OptionsSheetScope {
    fun dismiss()

    @Composable
    fun OptionsItem(
        title: String,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
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
                    colors = ListItemDefaults.colors(
                        containerColor = Color.Transparent
                    )
                )
            }

            HorizontalDivider()
        }
    }
}

interface OptionsSheetValues {
    val imageUrl: String
    val title: String
    val description: String
    val serviceName: String
    val url: String
}

class KmpItemModelOptionsSheet(
    val itemModel: KmpItemModel,
    override val imageUrl: String = itemModel.imageUrl,
    override val title: String = itemModel.title,
    override val description: String = itemModel.description,
    override val serviceName: String = itemModel.source.serviceName,
    override val url: String = itemModel.url,
) : OptionsSheetValues

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun optionsKmpSheet(
    scope: CoroutineScope = rememberCoroutineScope(),
    navController: NavigationActions = LocalNavActions.current,
    sheetState: SheetState = rememberModalBottomSheetState(true),
    moreContent: @Composable OptionsSheetScope.(KmpItemModelOptionsSheet) -> Unit = {},
): MutableState<KmpItemModel?> {
    val itemInfo = remember { mutableStateOf<KmpItemModel?>(null) }

    itemInfo
        .value
        ?.let { KmpItemModelOptionsSheet(itemModel = it) }
        ?.let { item ->
            OptionsSheet(
                sheet = sheetState,
                scope = scope,
                optionsSheetValues = item,
                onOpen = { navController.details(item.itemModel) },
                onGlobalSearch = { navController.globalSearch(it) },
                onDismiss = { itemInfo.value = null },
                moreContent = moreContent
            )
        }

    return itemInfo
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun optionsKmpSheetList(
    scope: CoroutineScope = rememberCoroutineScope(),
    navController: NavigationActions = LocalNavActions.current,
    sheetState: SheetState = rememberModalBottomSheetState(true),
    moreContent: @Composable OptionsSheetScope.(KmpItemModelOptionsSheet) -> Unit = {},
): MutableState<List<KmpItemModel>?> {
    val itemInfo = remember { mutableStateOf<List<KmpItemModel>?>(null) }

    itemInfo
        .value
        ?.map { KmpItemModelOptionsSheet(it) }
        ?.let { item ->
            OptionsSheet(
                sheet = sheetState,
                scope = scope,
                optionsSheetValuesList = item,
                onOpen = { navController.details(it.itemModel) },
                onGlobalSearch = { navController.globalSearch(it) },
                onDismiss = { itemInfo.value = null },
                moreContent = moreContent
            )
        }

    return itemInfo
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T : OptionsSheetValues> optionsSheetList(
    onOpen: (T) -> Unit,
    scope: CoroutineScope = rememberCoroutineScope(),
    navController: NavigationActions = LocalNavActions.current,
    sheetState: SheetState = rememberModalBottomSheetState(true),
    moreContent: @Composable OptionsSheetScope.(T) -> Unit = {},
): MutableState<List<T>?> {
    val itemInfo = remember { mutableStateOf<List<T>?>(null) }

    itemInfo.value?.let { items ->
        OptionsSheet(
            sheet = sheetState,
            scope = scope,
            optionsSheetValuesList = items,
            onOpen = { onOpen(it) },
            onGlobalSearch = { navController.globalSearch(it) },
            onDismiss = { itemInfo.value = null },
            moreContent = moreContent
        )
    }

    return itemInfo
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T : OptionsSheetValues> optionsSheet(
    scope: CoroutineScope = rememberCoroutineScope(),
    navController: NavigationActions = LocalNavActions.current,
    sheetState: SheetState = rememberModalBottomSheetState(true),
    onOpen: (T) -> Unit,
    moreContent: @Composable OptionsSheetScope.(T) -> Unit = {},
): MutableState<T?> {
    val itemInfo = remember { mutableStateOf<T?>(null) }

    itemInfo.value?.let { item ->
        OptionsSheet(
            sheet = sheetState,
            scope = scope,
            optionsSheetValues = item,
            onOpen = { onOpen(item) },
            onGlobalSearch = { navController.globalSearch(it) },
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
    onOpen: () -> Unit,
    onGlobalSearch: (String) -> Unit,
    onDismiss: () -> Unit,
    scope: CoroutineScope = rememberCoroutineScope(),
    sheet: SheetState = rememberModalBottomSheetState(true),
    dao: ItemDao = koinInject(),
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
                    onOpen = onOpen,
                    onGlobalSearch = onGlobalSearch,
                    onDismiss = onDismiss,
                    sheet = sheet,
                    dao = dao,
                    scope = scope,
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
    onOpen: (T) -> Unit,
    onGlobalSearch: (String) -> Unit,
    onDismiss: () -> Unit,
    scope: CoroutineScope = rememberCoroutineScope(),
    sheet: SheetState = rememberModalBottomSheetState(true),
    dao: ItemDao = koinInject(),
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
                                onOpen = { onOpen(it) },
                                onGlobalSearch = onGlobalSearch,
                                onDismiss = onDismiss,
                                sheet = sheet,
                                dao = dao,
                                scope = scope,
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
    onOpen: () -> Unit,
    onGlobalSearch: (String) -> Unit,
    onDismiss: () -> Unit,
    sheet: SheetState,
    dao: ItemDao = koinInject(),
    listDao: ListDao = koinInject(),
    platformRepository: PlatformRepository = koinInject(),
    notificationRepository: NotificationRepository = koinInject(),
    scope: CoroutineScope = rememberCoroutineScope(),
    moreContent: @Composable OptionsSheetScope.(T) -> Unit = {},
) {
    val imageUrl = optionsSheetValues.imageUrl
    val title = optionsSheetValues.title
    val description = optionsSheetValues.description
    val serviceName = optionsSheetValues.serviceName
    val url = optionsSheetValues.url

    val biometric = rememberBiometricPrompting()
    val isIncognito by dao
        .getIncognitoSource(url)
        .collectAsStateWithLifecycle(null)

    val isSaved by dao
        .doesNotificationExistFlow(url)
        .collectAsStateWithLifecycle(false)

    val colorBlindness: ColorBlindnessType by koinInject<NewSettingsHandling>().rememberColorBlindType()
    val colorFilter by remember { derivedStateOf { colorFilterBlind(colorBlindness) } }

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
            ImageLoaderChoice(
                imageUrl = imageUrl,
                name = title,
                placeHolder = { painterLogo() },
                colorFilter = colorFilter,
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

        OptionsItem(
            title = "Open",
            onClick = {
                scope.launch {
                    sheet.hide()
                }.invokeOnCompletion {
                    onDismiss()
                    if (isIncognito != null) {
                        biometric.authenticate(
                            onAuthenticationSucceeded = { onOpen() },
                            title = "Authentication required",
                            subtitle = "In order to open ${title}, please authenticate",
                            negativeButtonText = "Never Mind"
                        )
                    } else {
                        onOpen()
                    }
                }
            }
        )

        Crossfade(isSaved) { target ->
            if (!target) {
                OptionsItem(
                    title = stringResource(Res.string.save_for_later),
                    onClick = {
                        scope.launch {
                            dao.insertNotification(
                                NotificationItem(
                                    id = "$title$url$imageUrl$serviceName$description".hashCode(),
                                    url = url,
                                    summaryText = "$title had an update",
                                    notiTitle = title,
                                    imageUrl = imageUrl,
                                    source = serviceName,
                                    contentTitle = title
                                )
                            )
                        }
                    }
                )
            } else {
                OptionsItem(
                    title = "Remove from Saved",
                    onClick = {
                        scope.launch {
                            dao.getNotificationItemFlow(url)
                                .firstOrNull()
                                ?.let {
                                    notificationRepository.cancelNotification(it)
                                    dao.deleteNotification(it)
                                }
                        }
                    }
                )
            }
        }

        OptionsItem(
            title = stringResource(Res.string.global_search_by_name),
            onClick = {
                scope.launch { sheet.hide() }
                    .invokeOnCompletion { onGlobalSearch(title) }
            }
        )

        var showLists by remember { mutableStateOf(false) }

        if (showLists) {
            ModalBottomSheet(
                onDismissRequest = { showLists = false },
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
                ListChoiceScreen(
                    url = url,
                    onClick = { item ->
                        scope.launch {
                            showLists = false
                            listDao.addToList(
                                item.item.uuid,
                                title,
                                description,
                                url,
                                imageUrl,
                                serviceName
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { showLists = false }
                        ) { Icon(Icons.Default.Close, null) }
                    },
                )
            }
        }

        OptionsItem(
            title = stringResource(Res.string.add_to_list),
            onClick = { showLists = true }
        )

        var showQr by remember { mutableStateOf(false) }
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
        )

        moreContent(optionsSheetValues)

        if (remember { platformRepository.hasBiometric() }) {
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
        }
    }
}