package com.programmersbox.uiviews.presentation.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.favoritesdatabase.ListDao
import com.programmersbox.favoritesdatabase.NotificationItem
import com.programmersbox.models.ItemModel
import com.programmersbox.sharedutils.AppLogo
import com.programmersbox.uiviews.R
import com.programmersbox.uiviews.presentation.Screen
import com.programmersbox.uiviews.presentation.components.imageloaders.ImageLoaderChoice
import com.programmersbox.uiviews.presentation.lists.ListChoiceScreen
import com.programmersbox.uiviews.presentation.navigateToDetails
import com.programmersbox.uiviews.utils.ComposableUtils
import com.programmersbox.uiviews.utils.LoadingDialog
import com.programmersbox.uiviews.utils.LocalNavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun optionsSheet(
    scope: CoroutineScope = rememberCoroutineScope(),
    navController: NavController = LocalNavController.current,
    sheetState: SheetState = rememberModalBottomSheetState(true),
    moreContent: @Composable OptionsSheetScope.(ItemModelOptionsSheet) -> Unit = {},
): MutableState<ItemModel?> {
    val itemInfo = remember { mutableStateOf<ItemModel?>(null) }

    itemInfo
        .value
        ?.let { ItemModelOptionsSheet(itemModel = it) }
        ?.let {
            OptionsSheet(
                sheet = sheetState,
                scope = scope,
                optionsSheetValues = it,
                onOpen = { navController.navigateToDetails(it.itemModel) },
                onGlobalSearch = { navController.navigate(Screen.GlobalSearchScreen(it)) },
                onDismiss = { itemInfo.value = null },
                moreContent = moreContent
            )
        }

    return itemInfo
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun optionsSheetList(
    scope: CoroutineScope = rememberCoroutineScope(),
    navController: NavController = LocalNavController.current,
    sheetState: SheetState = rememberModalBottomSheetState(true),
    moreContent: @Composable OptionsSheetScope.(ItemModelOptionsSheet) -> Unit = {},
): MutableState<List<ItemModel>?> {
    val itemInfo = remember { mutableStateOf<List<ItemModel>?>(null) }

    itemInfo
        .value
        ?.map { ItemModelOptionsSheet(it) }
        ?.let {
            OptionsSheet(
                sheet = sheetState,
                scope = scope,
                optionsSheetValuesList = it,
                onOpen = { navController.navigateToDetails(it.itemModel) },
                onGlobalSearch = { navController.navigate(Screen.GlobalSearchScreen(it)) },
                onDismiss = { itemInfo.value = null },
                moreContent = moreContent
            )
        }

    return itemInfo
}

class ItemModelOptionsSheet(
    val itemModel: ItemModel,
    override val imageUrl: String = itemModel.imageUrl,
    override val title: String = itemModel.title,
    override val description: String = itemModel.description,
    override val serviceName: String = itemModel.source.serviceName,
    override val url: String = itemModel.url,
) : OptionsSheetValues

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T : OptionsSheetValues> optionsSheetList(
    scope: CoroutineScope = rememberCoroutineScope(),
    navController: NavController = LocalNavController.current,
    sheetState: SheetState = rememberModalBottomSheetState(true),
    onOpen: (T) -> Unit,
    moreContent: @Composable OptionsSheetScope.(T) -> Unit = {},
): MutableState<List<T>?> {
    val itemInfo = remember { mutableStateOf<List<T>?>(null) }

    itemInfo.value?.let {
        OptionsSheet(
            sheet = sheetState,
            scope = scope,
            optionsSheetValuesList = it,
            onOpen = { onOpen(it) },
            onGlobalSearch = { navController.navigate(Screen.GlobalSearchScreen(it)) },
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
    navController: NavController = LocalNavController.current,
    sheetState: SheetState = rememberModalBottomSheetState(true),
    onOpen: (T) -> Unit,
    moreContent: @Composable OptionsSheetScope.(T) -> Unit = {},
): MutableState<T?> {
    val itemInfo = remember { mutableStateOf<T?>(null) }

    itemInfo.value?.let {
        OptionsSheet(
            sheet = sheetState,
            scope = scope,
            optionsSheetValues = it,
            onOpen = { onOpen(it) },
            onGlobalSearch = { navController.navigate(Screen.GlobalSearchScreen(it)) },
            onDismiss = { itemInfo.value = null },
            moreContent = moreContent
        )
    }

    return itemInfo
}

interface OptionsSheetValues {
    val imageUrl: String
    val title: String
    val description: String
    val serviceName: String
    val url: String
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T : OptionsSheetValues> OptionsSheet(
    sheet: SheetState = rememberModalBottomSheetState(true),
    scope: CoroutineScope,
    optionsSheetValues: T,
    onOpen: () -> Unit,
    onGlobalSearch: (String) -> Unit,
    onDismiss: () -> Unit,
    dao: ItemDao = koinInject(),
    moreContent: @Composable OptionsSheetScope.(T) -> Unit = {},
) {
    val optionsSheetScope = remember {
        object : OptionsSheetScope {
            override fun dismiss() {
                scope.launch {
                    sheet.hide()
                }.invokeOnCompletion { onDismiss() }
            }
        }
    }
    var showLoadingDialog by remember { mutableStateOf(false) }

    LoadingDialog(
        showLoadingDialog = showLoadingDialog,
        onDismissRequest = { showLoadingDialog = false }
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheet
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
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
    sheet: SheetState = rememberModalBottomSheetState(true),
    scope: CoroutineScope,
    optionsSheetValuesList: List<T>,
    onOpen: (T) -> Unit,
    onGlobalSearch: (String) -> Unit,
    onDismiss: () -> Unit,
    dao: ItemDao = koinInject(),
    moreContent: @Composable OptionsSheetScope.(T) -> Unit = {},
) {
    val optionsSheetScope = remember {
        object : OptionsSheetScope {
            override fun dismiss() {
                scope.launch {
                    sheet.hide()
                }.invokeOnCompletion { onDismiss() }
            }
        }
    }
    var showLoadingDialog by remember { mutableStateOf(false) }

    LoadingDialog(
        showLoadingDialog = showLoadingDialog,
        onDismissRequest = { showLoadingDialog = false }
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheet
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
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
    scope: CoroutineScope = rememberCoroutineScope(),
    moreContent: @Composable OptionsSheetScope.(T) -> Unit = {},
) {
    val imageUrl = optionsSheetValues.imageUrl
    val title = optionsSheetValues.title
    val description = optionsSheetValues.description
    val serviceName = optionsSheetValues.serviceName
    val url = optionsSheetValues.url

    val isSaved by dao
        .doesNotificationExistFlow(url)
        .collectAsStateWithLifecycle(false)

    ListItem(
        leadingContent = {
            val logo = koinInject<AppLogo>().logo
            ImageLoaderChoice(
                imageUrl = imageUrl,
                name = title,
                placeHolder = rememberDrawablePainter(logo),
                modifier = Modifier
                    .size(ComposableUtils.IMAGE_WIDTH, ComposableUtils.IMAGE_HEIGHT)
                    .clip(MaterialTheme.shapes.small)
            )
        },
        overlineContent = { Text(serviceName) },
        headlineContent = { Text(title) },
        supportingContent = { Text(description) },
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent
        )
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
                    onOpen()
                }
            }
        )

        //TODO: Add favorite/unfavorite
        // might have to pass in a class with an action (like details does)
        // that includes if we can show favorites or not

        //TODO: Maybe do some ocr and allow all languages in order to be able to translate?
        // https://github.com/VrajVyas11/AI_Manga_Reader

        OptionsItem(
            title = stringResource(R.string.global_search_by_name),
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
            title = stringResource(id = R.string.add_to_list),
            onClick = { showLists = true }
        )

        Crossfade(isSaved) { target ->
            if (!target) {
                OptionsItem(
                    title = stringResource(id = R.string.save_for_later),
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
                    title = stringResource(R.string.removeNotification),
                    onClick = {
                        scope.launch {
                            dao.getNotificationItemFlow(url)
                                .firstOrNull()
                                ?.let { dao.deleteNotification(it) }
                        }
                    }
                )
            }
        }

        moreContent(optionsSheetValues)
    }
}

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

/*
@Composable
private fun ColumnScope.OptionsItem(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
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
}*/
