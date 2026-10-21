@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.programmersbox.kmpuiviews.presentation.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.HideSource
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.programmersbox.datastore.ColorBlindnessType
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.favoritesdatabase.NotificationItem
import com.programmersbox.favoritesdatabase.toDbModel
import com.programmersbox.kmpmodels.KmpItemModel
import com.programmersbox.kmpuiviews.painterLogo
import com.programmersbox.kmpuiviews.presentation.components.textflow.TextFlow
import com.programmersbox.kmpuiviews.presentation.navactions.NavigationActions
import com.programmersbox.kmpuiviews.presentation.settings.lists.addtolist.ListChoiceScreen
import com.programmersbox.kmpuiviews.presentation.settings.qrcode.ShareViaQrCode
import com.programmersbox.kmpuiviews.repository.FavoritesRepository
import com.programmersbox.kmpuiviews.repository.IncognitoRepository
import com.programmersbox.kmpuiviews.repository.ListRepository
import com.programmersbox.kmpuiviews.repository.NotificationRepository
import com.programmersbox.kmpuiviews.repository.PlatformRepository
import com.programmersbox.kmpuiviews.utils.ComposableUtils
import com.programmersbox.kmpuiviews.utils.LocalNavActions
import com.programmersbox.kmpuiviews.utils.LocalSourcesRepository
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
        trailingContent: (@Composable () -> Unit)? = null,
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
    sheet: SheetState = rememberBottomSheetState(SheetValue.Expanded),
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
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
                verticalArrangement = verticalArrangement,
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
    val itemModel: KmpItemModel? get() = null
}

class KmpItemModelOptionsSheet(
    override val itemModel: KmpItemModel,
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
    sheetState: SheetState = rememberBottomSheetState(SheetValue.Expanded),
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
    sheetState: SheetState = rememberBottomSheetState(SheetValue.Expanded),
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
    sheetState: SheetState = rememberBottomSheetState(SheetValue.Expanded),
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
    sheet: SheetState = rememberBottomSheetState(SheetValue.Expanded),
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
                NewOptionsItems(
                    optionsSheetValues = optionsSheetValues,
                    onOpen = onOpen,
                    onGlobalSearch = onGlobalSearch,
                    onDismiss = onDismiss,
                    sheet = sheet,
                    dao = dao,
                    scope = scope,
                    //moreContent = moreContent
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
    sheet: SheetState = rememberBottomSheetState(SheetValue.Expanded),
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
                            overlineContent = { Text(LocalSourcesRepository.current.displayNameFor(it.serviceName)) },
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
    incognitoRepository: IncognitoRepository = koinInject(),
    listDao: ListRepository = koinInject(),
    platformRepository: PlatformRepository = koinInject(),
    notificationRepository: NotificationRepository = koinInject(),
    favoritesRepository: FavoritesRepository = koinInject(),
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

    val dbModel by dao
        .getDbModel(url)
        .collectAsStateWithLifecycle(null)

    val shouldCheck by koinInject<NewSettingsHandling>()
        .mediaCheckerSettings
        .rememberPreference()

    val colorBlindness: ColorBlindnessType by koinInject<NewSettingsHandling>().rememberColorBlindType()
    val colorFilter by remember { derivedStateOf { colorFilterBlind(colorBlindness) } }

    val listItemColors = ListItemDefaults.colors()

    TextFlow(
        text = buildAnnotatedString {
            withStyle(
                MaterialTheme.typography.labelSmall
                    .copy(color = listItemColors.overlineContentColor)
                    .toSpanStyle()
            ) { appendLine(LocalSourcesRepository.current.displayNameFor(serviceName)) }

            withStyle(
                MaterialTheme.typography.bodyLarge
                    .copy(color = listItemColors.contentColor)
                    .toSpanStyle()
            ) { appendLine(title) }

            withStyle(
                MaterialTheme.typography.bodySmall
                    .copy(color = listItemColors.supportingContentColor)
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
                scope.launch { sheet.hide() }
                    .invokeOnCompletion {
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

        val updateParams by remember {
            derivedStateOf {
                CheckForUpdateParams(
                    isFavorite = dbModel != null,
                    deviceCheckForUpdate = shouldCheck.shouldRun,
                    shouldCheckForUpdate = dbModel?.shouldCheckForUpdate == true
                )
            }
        }

        Crossfade(updateParams) { target ->
            if (target.isFavorite && target.deviceCheckForUpdate) {
                OptionsItem(
                    title = if (target.shouldCheckForUpdate) "Check for updates" else "Do not check for updates",
                    onClick = {
                        scope.launch {
                            dbModel
                                ?.let { it.copy(shouldCheckForUpdate = !it.shouldCheckForUpdate) }
                                ?.let { favoritesRepository.toggleNotify(it) }
                        }
                    }
                )
            } else {
                OptionsItem(
                    title = "Cannot change update status without being a favorite",
                    onClick = {},
                    modifier = Modifier.alpha(.5f)
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
        val listState = rememberModalBottomSheetState(true)

        if (showLists) {
            ModalBottomSheet(
                onDismissRequest = { showLists = false },
                containerColor = MaterialTheme.colorScheme.surface,
                sheetState = listState
            ) {
                ListChoiceScreen(
                    url = url,
                    onClick = { item ->
                        scope.launch {
                            listDao.addToList(
                                item.item.uuid,
                                title,
                                description,
                                url,
                                imageUrl,
                                serviceName
                            )
                            listState.hide()
                        }.invokeOnCompletion { showLists = false }
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
                                        incognitoRepository.addIncognito(
                                            url = url,
                                            title = title,
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
                                    scope.launch { incognitoRepository.removeIncognito(url) }
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
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T : OptionsSheetValues> OptionsSheetScope.NewOptionsItems(
    optionsSheetValues: T,
    onOpen: () -> Unit,
    onGlobalSearch: (String) -> Unit,
    onDismiss: () -> Unit,
    sheet: SheetState,
    dao: ItemDao = koinInject(),
    incognitoRepository: IncognitoRepository = koinInject(),
    listDao: ListRepository = koinInject(),
    platformRepository: PlatformRepository = koinInject(),
    notificationRepository: NotificationRepository = koinInject(),
    favoritesRepository: FavoritesRepository = koinInject(),
    scope: CoroutineScope = rememberCoroutineScope(),
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

    val dbModel by dao
        .getDbModel(url)
        .collectAsStateWithLifecycle(null)

    val shouldCheck by koinInject<NewSettingsHandling>()
        .mediaCheckerSettings
        .rememberPreference()

    val colorBlindness: ColorBlindnessType by koinInject<NewSettingsHandling>().rememberColorBlindType()
    val colorFilter by remember { derivedStateOf { colorFilterBlind(colorBlindness) } }

    val listItemColors = ListItemDefaults.colors()

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

    var showLists by remember { mutableStateOf(false) }
    val listState = rememberBottomSheetState(SheetValue.Expanded)

    val updateParams by remember {
        derivedStateOf {
            CheckForUpdateParams(
                isFavorite = dbModel != null,
                deviceCheckForUpdate = shouldCheck.shouldRun,
                shouldCheckForUpdate = dbModel?.shouldCheckForUpdate == true
            )
        }
    }

    if (showLists) {
        ModalBottomSheet(
            onDismissRequest = { showLists = false },
            containerColor = MaterialTheme.colorScheme.surface,
            sheetState = listState
        ) {
            ListChoiceScreen(
                url = url,
                onClick = { item ->
                    scope.launch {
                        listDao.addToList(
                            item.item.uuid,
                            title,
                            description,
                            url,
                            imageUrl,
                            serviceName
                        )
                        listState.hide()
                    }.invokeOnCompletion { showLists = false }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { showLists = false }
                    ) { Icon(Icons.Default.Close, null) }
                },
            )
        }
    }

    TextFlow(
        text = buildAnnotatedString {
            withStyle(
                MaterialTheme.typography.labelSmall
                    .copy(color = listItemColors.overlineContentColor)
                    .toSpanStyle()
            ) { appendLine(LocalSourcesRepository.current.displayNameFor(serviceName)) }

            withStyle(
                MaterialTheme.typography.bodyLarge
                    .copy(color = listItemColors.contentColor)
                    .toSpanStyle()
            ) { appendLine(title) }

            withStyle(
                MaterialTheme.typography.bodySmall
                    .copy(color = listItemColors.supportingContentColor)
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
                    .size(
                        ComposableUtils.IMAGE_WIDTH,
                        ComposableUtils.IMAGE_HEIGHT
                    )
                    .clip(MaterialTheme.shapes.small)
            )
        },
        modifier = Modifier.padding(16.dp)
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(16.dp)
    ) {
        HorizontalDivider()

        Spacer(modifier = Modifier.height(8.dp))

        FilledTonalButton(
            onClick = {
                scope.launch { sheet.hide() }
                    .invokeOnCompletion {
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
            },
            shapes = ButtonDefaults.shapes(),
            modifier = Modifier
                .fillMaxWidth(.75f)
                .align(Alignment.CenterHorizontally)
        ) { Text("Open") }

        Text("Quick Actions")

        Row( // Changed from FlowRow to standard Row
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp), // Added spacing
            verticalAlignment = Alignment.CenterVertically
        ) {
            Crossfade(
                dbModel,
                modifier = Modifier.weight(1f)
            ) { target ->
                if (target != null) {
                    QuickActionCard(
                        onClick = { scope.launch { favoritesRepository.removeFavorite(target) } },
                        icon = Icons.Default.Favorite,
                        title = "Favorite",
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    QuickActionCard(
                        onClick = {
                            scope.launch {
                                optionsSheetValues.itemModel?.let { favoritesRepository.addFavorite(it.toDbModel()) }

                            }
                        },
                        icon = Icons.Default.FavoriteBorder,
                        title = "Favorite",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Crossfade(
                isSaved,
                modifier = Modifier.weight(1f)
            ) { target ->
                if (target) {
                    QuickActionCard(
                        onClick = {
                            scope.launch {
                                dao.getNotificationItemFlow(url)
                                    .firstOrNull()
                                    ?.let {
                                        notificationRepository.cancelNotification(it)
                                        dao.deleteNotification(it)
                                    }
                            }
                        },
                        icon = Icons.Default.Bookmark,
                        title = "Save",
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    QuickActionCard(
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
                        },
                        icon = Icons.Default.BookmarkBorder,
                        title = "Save",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            QuickActionCard(
                onClick = { showQr = true },
                icon = Icons.Default.QrCode,
                title = "Share QR",
                modifier = Modifier.weight(1f)
            )

            QuickActionCard(
                onClick = { showLists = true },
                icon = Icons.AutoMirrored.Filled.PlaylistAdd,
                title = "Lists",
                modifier = Modifier.weight(1f)
            )
        }

        Text("Utilities")

        Column {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                UtilityCard(
                    onClick = {
                        scope.launch { sheet.hide() }
                            .invokeOnCompletion { onGlobalSearch(title) }
                    },
                    icon = Icons.Default.Search,
                    title = "Global Search",
                    modifier = Modifier.weight(1f)
                )

                UtilityCard(
                    onClick = {
                        scope.launch {
                            dbModel
                                ?.let { it.copy(shouldCheckForUpdate = !it.shouldCheckForUpdate) }
                                ?.let { favoritesRepository.toggleNotify(it) }
                        }
                    },
                    enabled = updateParams.isFavorite && updateParams.deviceCheckForUpdate,
                    additionalIcon = {
                        if (!(updateParams.isFavorite && updateParams.deviceCheckForUpdate)) {
                            Icon(
                                Icons.Default.Lock,
                                null,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    icon = Icons.Default.Refresh,
                    title = if (updateParams.shouldCheckForUpdate) "Will check for updates" else "Will not check for updates",
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (remember { platformRepository.hasBiometric() }) {
                    Crossfade(isIncognito) { target ->
                        if (target == null) {
                            UtilityCard(
                                title = "Add to Incognito",
                                onClick = {
                                    biometric.authenticate(
                                        onAuthenticationSucceeded = {
                                            scope.launch {
                                                incognitoRepository.addIncognito(
                                                    url = url,
                                                    title = title,
                                                )
                                            }.invokeOnCompletion { dismiss() }
                                        },
                                        title = "Authentication required",
                                        subtitle = "In order to add ${title}, please authenticate",
                                        negativeButtonText = "Never Mind"
                                    )
                                },
                                icon = Icons.Default.HideSource,
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            UtilityCard(
                                title = "Remove from Incognito",
                                onClick = {
                                    biometric.authenticate(
                                        onAuthenticationSucceeded = {
                                            scope.launch { incognitoRepository.removeIncognito(url) }
                                                .invokeOnCompletion { dismiss() }
                                        },
                                        title = "Authentication required",
                                        subtitle = "In order to remove ${title}, please authenticate",
                                        negativeButtonText = "Never Mind"
                                    )
                                },
                                icon = Icons.Default.HideSource,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                } else {
                    // If biometric isn't available, we still need a weight(1f) spacer here
                    // to maintain alignment, just in case.
                    Spacer(modifier = Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun QuickActionCard(
    onClick: () -> Unit,
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier // Receives weight(1f) from parent
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Icon(
                icon,
                null,
                modifier = Modifier.size(28.dp) // Adjusted slightly for 4-item row scaling
            )

            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall, // Use a smaller typography
                maxLines = 1, // Prevent it from stretching height
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun UtilityCard(
    onClick: () -> Unit,
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    additionalIcon: @Composable () -> Unit = {},
    enabled: Boolean = true,
) {
    Card(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(100.dp) // CRITICAL FIX: Fixed height so they all match perfectly
    ) {
        Column(
            verticalArrangement = Arrangement.SpaceBetween, // Pushes the top row and bottom text apart
            modifier = Modifier
                .padding(12.dp)
                .fillMaxSize() // Fill the new 100.dp height
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    icon,
                    null,
                    modifier = Modifier.size(28.dp)
                )

                additionalIcon()
            }

            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Stable
private data class CheckForUpdateParams(
    val isFavorite: Boolean,
    val deviceCheckForUpdate: Boolean,
    val shouldCheckForUpdate: Boolean,
)