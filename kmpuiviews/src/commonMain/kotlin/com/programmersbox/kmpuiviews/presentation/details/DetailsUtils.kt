package com.programmersbox.kmpuiviews.presentation.details

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BookmarkRemove
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WatchLater
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.BottomAppBarScrollBehavior
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.material3.ToggleFloatingActionButtonDefaults.animateIcon
import androidx.compose.material3.animateFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.unit.dp
import com.programmersbox.datastore.DataStoreHandling
import com.programmersbox.datastore.asState
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.favoritesdatabase.ListDao
import com.programmersbox.favoritesdatabase.NotificationItem
import com.programmersbox.kmpmodels.KmpInfoModel
import com.programmersbox.kmpuiviews.KmpGenericInfo
import com.programmersbox.kmpuiviews.presentation.components.ToolTipWrapper
import com.programmersbox.kmpuiviews.presentation.navactions.NavigationActions
import com.programmersbox.kmpuiviews.presentation.settings.lists.addtolist.ListChoiceScreen
import com.programmersbox.kmpuiviews.presentation.settings.qrcode.ShareViaQrCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.addToFavorites
import otakuworld.kmpuiviews.generated.resources.add_to_for_later_list
import otakuworld.kmpuiviews.generated.resources.add_to_list
import otakuworld.kmpuiviews.generated.resources.added_to_list
import otakuworld.kmpuiviews.generated.resources.already_in_list
import otakuworld.kmpuiviews.generated.resources.global_search_by_name
import otakuworld.kmpuiviews.generated.resources.hadAnUpdate
import otakuworld.kmpuiviews.generated.resources.markAs
import otakuworld.kmpuiviews.generated.resources.removeFromFavorites
import otakuworld.kmpuiviews.generated.resources.removeNotification
import otakuworld.kmpuiviews.generated.resources.reverseOrder
import otakuworld.kmpuiviews.generated.resources.save_for_later

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
internal fun AddToList(
    showLists: Boolean,
    showListsChange: (Boolean) -> Unit,
    info: KmpInfoModel,
    listDao: ListDao,
    hostState: SnackbarHostState?,
    scope: CoroutineScope,
) {
    if (showLists) {
        BackHandler { showListsChange(false) }

        ModalBottomSheet(
            onDismissRequest = { showListsChange(false) },
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            ListChoiceScreen(
                url = info.url,
                onClick = { item ->
                    scope.launch {
                        showListsChange(false)
                        val result = listDao.addToList(
                            item.item.uuid,
                            info.title,
                            info.description,
                            info.url,
                            info.imageUrl,
                            info.source.serviceName
                        )
                        hostState?.showSnackbar(
                            getString(
                                if (result) {
                                    Res.string.added_to_list
                                } else {
                                    Res.string.already_in_list
                                },
                                item.item.name
                            ),
                            withDismissAction = true
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { showListsChange(false) }
                    ) { Icon(Icons.Default.Close, null) }
                },
            )
        }
    }
}

@Composable
internal fun DetailActions(
    genericInfo: KmpGenericInfo,
    scaffoldState: DrawerState,
    navController: NavigationActions,
    scope: CoroutineScope,
    info: KmpInfoModel,
    isSaved: Boolean,
    dao: ItemDao,
    isFavorite: Boolean,
    canNotify: Boolean,
    notifyAction: () -> Unit,
    onReverseChaptersClick: () -> Unit,
    onShowLists: () -> Unit,
    addToForLater: () -> Unit,
    customActions: @Composable () -> Unit = {},
) {
    val uriHandler = LocalUriHandler.current

    var showDropDown by remember { mutableStateOf(false) }

    val dropDownDismiss = { showDropDown = false }

    val shouldCheck by koinInject<DataStoreHandling>().shouldCheck.asState()

    DropdownMenu(
        expanded = showDropDown,
        onDismissRequest = dropDownDismiss,
    ) {

        DropdownMenuItem(
            onClick = {
                dropDownDismiss()
                scope.launch { scaffoldState.open() }
            },
            text = { Text(stringResource(Res.string.markAs)) },
            leadingIcon = { Icon(Icons.Default.Check, null) }
        )

        DropdownMenuItem(
            onClick = {
                dropDownDismiss()
                uriHandler.openUri(info.url)
            },
            text = { Text("Open in Browser") },
            leadingIcon = { Icon(Icons.Default.OpenInBrowser, null) }
        )

        DropdownMenuItem(
            onClick = {
                dropDownDismiss()
                addToForLater()
            },
            text = { Text(stringResource(Res.string.add_to_for_later_list)) },
            leadingIcon = { Icon(Icons.Default.WatchLater, null) }
        )

        DropdownMenuItem(
            onClick = {
                dropDownDismiss()
                onShowLists()
            },
            text = { Text(stringResource(Res.string.add_to_list)) },
            leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, null) }
        )

        if (!isSaved) {
            DropdownMenuItem(
                onClick = {
                    dropDownDismiss()
                    scope.launch(Dispatchers.IO) {
                        dao.insertNotification(
                            NotificationItem(
                                id = info.hashCode(),
                                url = info.url,
                                summaryText = getString(
                                    Res.string.hadAnUpdate,
                                    info.title,
                                    info.chapters.firstOrNull()?.name.orEmpty()
                                ),
                                notiTitle = info.title,
                                imageUrl = info.imageUrl,
                                source = info.source.serviceName,
                                contentTitle = info.title
                            )
                        )
                    }
                },
                text = { Text(stringResource(Res.string.save_for_later)) },
                leadingIcon = { Icon(Icons.Default.Save, null) }
            )
        } else {
            DropdownMenuItem(
                onClick = {
                    dropDownDismiss()
                    scope.launch(Dispatchers.IO) {
                        dao.getNotificationItemFlow(info.url)
                            .firstOrNull()
                            ?.let { dao.deleteNotification(it) }
                    }
                },
                text = { Text(stringResource(Res.string.removeNotification)) },
                leadingIcon = { Icon(Icons.Default.Delete, null) }
            )
        }

        if (isFavorite && shouldCheck) {
            DropdownMenuItem(
                onClick = {
                    dropDownDismiss()
                    notifyAction()
                },
                text = { Text(if (canNotify) "Check for updates" else "Do not check for updates") },
                leadingIcon = {
                    Icon(
                        if (canNotify) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                        null
                    )
                }
            )
        }

        DropdownMenuItem(
            onClick = {
                dropDownDismiss()
                navController.globalSearch(info.title)
            },
            text = { Text(stringResource(Res.string.global_search_by_name)) },
            leadingIcon = { Icon(Icons.Default.Search, null) }
        )

        DropdownMenuItem(
            onClick = {
                dropDownDismiss()
                onReverseChaptersClick()
            },
            text = { Text(stringResource(Res.string.reverseOrder)) },
            leadingIcon = { Icon(Icons.AutoMirrored.Filled.Sort, null) }
        )
    }

    customActions()

    ShareButton(info = info)

    genericInfo.DetailActions(infoModel = info, tint = LocalContentColor.current)

    IconButton(onClick = { showDropDown = true }) {
        Icon(Icons.Default.MoreVert, null)
    }
}

@Composable
internal fun ShareButton(
    info: KmpInfoModel,
) {
    var showQr by remember { mutableStateOf(false) }
    if (showQr) {
        ShareViaQrCode(
            url = info.url,
            title = info.title,
            imageUrl = info.imageUrl,
            apiService = info.source.serviceName,
            onClose = { showQr = false }
        )
    }

    IconButton(
        onClick = { showQr = true }
    ) { Icon(Icons.Default.Share, null) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailBottomBar(
    navController: NavigationActions,
    onShowLists: () -> Unit,
    info: KmpInfoModel,
    customActions: @Composable () -> Unit,
    removeFromSaved: () -> Unit,
    isSaved: Boolean,
    canNotify: Boolean,
    notifyAction: () -> Unit,
    modifier: Modifier = Modifier,
    isFavorite: Boolean,
    onFavoriteClick: (Boolean) -> Unit,
    bottomAppBarScrollBehavior: BottomAppBarScrollBehavior? = null,
    windowInsets: WindowInsets = WindowInsets(0.dp),
) {
    BottomAppBar(
        actions = {
            val shouldCheck by koinInject<DataStoreHandling>().shouldCheck.asState()

            ToolTipWrapper(
                info = { Text("Add to List") }
            ) {
                IconButton(
                    onClick = onShowLists,
                ) { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, null) }
            }

            ToolTipWrapper(
                info = { Text("Global Search by Name") }
            ) {
                IconButton(
                    onClick = { navController.globalSearch(info.title) },
                ) { Icon(Icons.Default.Search, null) }
            }

            ToolTipWrapper(
                info = { Text(stringResource(if (isFavorite) Res.string.removeFromFavorites else Res.string.addToFavorites)) }
            ) {
                IconButton(onClick = { onFavoriteClick(isFavorite) }) {
                    Icon(
                        if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = null,
                    )
                }
            }

            AnimatedVisibility(
                visible = isFavorite && shouldCheck,
                enter = fadeIn() + slideInHorizontally(),
                exit = slideOutHorizontally() + fadeOut()
            ) {
                ToolTipWrapper(info = { Text(if (canNotify) "Check for updates" else "Do not check for updates") }) {
                    IconButton(
                        onClick = notifyAction
                    ) {
                        Icon(
                            if (canNotify) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                            null
                        )
                    }
                }
            }

            customActions()
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = isSaved,
                enter = slideInHorizontally { it },
                exit = slideOutHorizontally { it },
                label = "",
            ) {
                ExtendedFloatingActionButton(
                    onClick = removeFromSaved,
                    text = { Text("Remove from Saved") },
                    icon = { Icon(Icons.Default.BookmarkRemove, null) },
                )
            }
        },
        scrollBehavior = bottomAppBarScrollBehavior,
        windowInsets = windowInsets,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalComposeUiApi::class)
@Composable
fun DetailFloatingActionButtonMenu(
    navController: NavigationActions,
    fabMenuExpanded: Boolean,
    onFabMenuExpandedChange: (Boolean) -> Unit,
    isVisible: Boolean,
    onShowLists: () -> Unit,
    info: KmpInfoModel,
    removeFromSaved: () -> Unit,
    addToSaved: () -> Unit,
    isSaved: Boolean,
    canNotify: Boolean,
    notifyAction: () -> Unit,
    modifier: Modifier = Modifier,
    isFavorite: Boolean,
    onFavoriteClick: (Boolean) -> Unit,
) {
    BackHandler(fabMenuExpanded) { onFabMenuExpandedChange(false) }

    FloatingActionButtonMenu(
        expanded = fabMenuExpanded,
        button = {
            ToggleFloatingActionButton(
                modifier = Modifier
                    .semantics {
                        traversalIndex = -1f
                        stateDescription = if (fabMenuExpanded) "Expanded" else "Collapsed"
                        contentDescription = "Toggle menu"
                    }
                    .animateFloatingActionButton(
                        visible = isVisible || fabMenuExpanded,
                        alignment = Alignment.BottomEnd,
                        scaleAnimationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
                        alphaAnimationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
                    ),
                checked = fabMenuExpanded,
                onCheckedChange = { onFabMenuExpandedChange(!fabMenuExpanded) }
            ) {
                val imageVector by remember {
                    derivedStateOf {
                        if (checkedProgress > 0.5f) Icons.Filled.Close else Icons.Filled.Add
                    }
                }
                Icon(
                    painter = rememberVectorPainter(imageVector),
                    contentDescription = null,
                    modifier = Modifier.animateIcon({ checkedProgress })
                )
            }
        },
        modifier = modifier
    ) {
        if (isSaved) {
            FloatingActionButtonMenuItem(
                onClick = {
                    onFabMenuExpandedChange(false)
                    removeFromSaved()
                },
                icon = { Icon(Icons.Default.BookmarkRemove, contentDescription = null) },
                text = { Text(text = "Remove from Saved") },
            )
        } else {
            FloatingActionButtonMenuItem(
                onClick = {
                    onFabMenuExpandedChange(false)
                    addToSaved()
                },
                icon = { Icon(Icons.Default.Save, null) },
                text = { Text(stringResource(Res.string.save_for_later)) },
            )
        }

        FloatingActionButtonMenuItem(
            onClick = {
                onFabMenuExpandedChange(false)
                onShowLists()
            },
            icon = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null) },
            text = { Text(text = "Add to List") },
        )

        FloatingActionButtonMenuItem(
            onClick = {
                onFabMenuExpandedChange(false)
                navController.globalSearch(info.title)
            },
            icon = { Icon(Icons.Default.Search, contentDescription = null) },
            text = { Text(text = "Global Search by Name") },
        )

        FloatingActionButtonMenuItem(
            onClick = { onFavoriteClick(isFavorite) },
            icon = {
                Icon(
                    if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = null,
                )
            },
            text = { Text(stringResource(if (isFavorite) Res.string.removeFromFavorites else Res.string.addToFavorites)) },
        )

        val shouldCheck by koinInject<DataStoreHandling>().shouldCheck.asState()

        if (isFavorite && shouldCheck) {
            FloatingActionButtonMenuItem(
                onClick = notifyAction,
                icon = {
                    Icon(
                        if (canNotify) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                        null
                    )
                },
                text = { Text(if (canNotify) "Check for updates" else "Do not check for updates") },
            )
        }
    }
}