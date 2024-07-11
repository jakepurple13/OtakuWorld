package com.programmersbox.uiviews.details

import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.Sort
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
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.BottomAppBarScrollBehavior
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.kmpalette.palette.graphics.Palette
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.favoritesdatabase.ListDao
import com.programmersbox.favoritesdatabase.NotificationItem
import com.programmersbox.models.InfoModel
import com.programmersbox.uiviews.GenericInfo
import com.programmersbox.uiviews.R
import com.programmersbox.uiviews.lists.ListChoiceScreen
import com.programmersbox.uiviews.utils.Screen
import com.programmersbox.uiviews.utils.components.ToolTipWrapper
import com.programmersbox.uiviews.utils.launchCatching
import com.programmersbox.uiviews.utils.shouldCheckFlow
import com.programmersbox.uiviews.utils.showErrorToast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

enum class PaletteSwatchType(val swatch: (Palette) -> Palette.Swatch?) {
    Vibrant(Palette::vibrantSwatch),
    Muted(Palette::mutedSwatch),
    Dominant(Palette::dominantSwatch),
    LightVibrant(Palette::lightVibrantSwatch),
    DarkVibrant(Palette::darkVibrantSwatch),
    LightMuted(Palette::lightMutedSwatch),
    DarkMuted(Palette::darkMutedSwatch),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddToList(
    showLists: Boolean,
    showListsChange: (Boolean) -> Unit,
    info: InfoModel,
    listDao: ListDao,
    hostState: SnackbarHostState?,
    scope: CoroutineScope,
    context: Context,
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
                            context.getString(
                                if (result) {
                                    R.string.added_to_list
                                } else {
                                    R.string.already_in_list
                                },
                                item.item.name
                            ),
                            withDismissAction = true
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { showListsChange(false) }) { Icon(Icons.Default.Close, null) }
                },
            )
        }
    }
}

@Composable
internal fun DetailActions(
    genericInfo: GenericInfo,
    scaffoldState: DrawerState,
    navController: NavHostController,
    scope: CoroutineScope,
    context: Context,
    info: InfoModel,
    topBarColor: Color,
    isSaved: Boolean,
    dao: ItemDao,
    isFavorite: Boolean,
    canNotify: Boolean,
    notifyAction: () -> Unit,
    onReverseChaptersClick: () -> Unit,
    onShowLists: () -> Unit,
    customActions: @Composable () -> Unit = {},
) {
    val uriHandler = LocalUriHandler.current

    var showDropDown by remember { mutableStateOf(false) }

    val dropDownDismiss = { showDropDown = false }

    DropdownMenu(
        expanded = showDropDown,
        onDismissRequest = dropDownDismiss,
    ) {

        DropdownMenuItem(
            onClick = {
                dropDownDismiss()
                scope.launch { scaffoldState.open() }
            },
            text = { Text(stringResource(id = R.string.markAs)) },
            leadingIcon = { Icon(Icons.Default.Check, null) }
        )

        DropdownMenuItem(
            onClick = {
                dropDownDismiss()
                uriHandler.openUri(info.url)
            },
            text = { Text(stringResource(id = R.string.fallback_menu_item_open_in_browser)) },
            leadingIcon = { Icon(Icons.Default.OpenInBrowser, null) }
        )

        DropdownMenuItem(
            onClick = {
                dropDownDismiss()
                onShowLists()
            },
            text = { Text(stringResource(R.string.add_to_list)) },
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
                                summaryText = context
                                    .getString(
                                        R.string.hadAnUpdate,
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
                text = { Text(stringResource(id = R.string.save_for_later)) },
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
                text = { Text(stringResource(R.string.removeNotification)) },
                leadingIcon = { Icon(Icons.Default.Delete, null) }
            )
        }

        if (isFavorite && LocalContext.current.shouldCheckFlow.collectAsStateWithLifecycle(initialValue = true).value) {
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
                navController.navigate(Screen.GlobalSearchScreen(info.title))
            },
            text = { Text(stringResource(id = R.string.global_search_by_name)) },
            leadingIcon = { Icon(Icons.Default.Search, null) }
        )

        DropdownMenuItem(
            onClick = {
                dropDownDismiss()
                onReverseChaptersClick()
            },
            text = { Text(stringResource(id = R.string.reverseOrder)) },
            leadingIcon = { Icon(Icons.AutoMirrored.Filled.Sort, null) }
        )
    }

    customActions()

    ShareButton(info = info)

    genericInfo.DetailActions(infoModel = info, tint = topBarColor)

    IconButton(onClick = { showDropDown = true }) {
        Icon(Icons.Default.MoreVert, null)
    }
}

@Composable
internal fun ShareButton(
    info: InfoModel,
) {
    val context = LocalContext.current
    val shareItem = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {}

    IconButton(
        onClick = {
            shareItem.launchCatching(
                Intent.createChooser(
                    Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, info.url)
                        putExtra(Intent.EXTRA_TITLE, info.title)
                    },
                    context.getString(R.string.share_item, info.title)
                )
            ).onFailure { context.showErrorToast() }
        }
    ) { Icon(Icons.Default.Share, null) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailBottomBar(
    navController: NavController,
    onShowLists: () -> Unit,
    info: InfoModel,
    customActions: @Composable () -> Unit,
    removeFromSaved: () -> Unit,
    isSaved: Boolean,
    canNotify: Boolean,
    notifyAction: () -> Unit,
    topBarColor: Color,
    modifier: Modifier = Modifier,
    containerColor: Color = Color.Transparent,
    isFavorite: Boolean,
    onFavoriteClick: (Boolean) -> Unit,
    bottomAppBarScrollBehavior: BottomAppBarScrollBehavior? = null,
    windowInsets: WindowInsets = WindowInsets(0.dp),
) {
    BottomAppBar(
        actions = {
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
                    onClick = { navController.navigate(Screen.GlobalSearchScreen(info.title)) },
                ) { Icon(Icons.Default.Search, null) }
            }

            ToolTipWrapper(info = { Text(stringResource(if (isFavorite) R.string.removeFromFavorites else R.string.addToFavorites)) }) {
                IconButton(onClick = { onFavoriteClick(isFavorite) }) {
                    Icon(
                        if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = null,
                    )
                }
            }

            AnimatedVisibility(
                visible = isFavorite && LocalContext.current.shouldCheckFlow.collectAsStateWithLifecycle(initialValue = true).value,
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
        containerColor = containerColor,
        contentColor = topBarColor,
        scrollBehavior = bottomAppBarScrollBehavior,
        windowInsets = windowInsets,
        modifier = modifier
    )
}