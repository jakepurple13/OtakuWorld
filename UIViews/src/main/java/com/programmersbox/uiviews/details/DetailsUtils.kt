package com.programmersbox.uiviews.details

import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SaveAs
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.favoritesdatabase.ListDao
import com.programmersbox.favoritesdatabase.NotificationItem
import com.programmersbox.models.InfoModel
import com.programmersbox.uiviews.GenericInfo
import com.programmersbox.uiviews.R
import com.programmersbox.uiviews.lists.ListChoiceScreen
import com.programmersbox.uiviews.utils.Screen
import com.programmersbox.uiviews.utils.launchCatching
import com.programmersbox.uiviews.utils.showErrorToast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlin.math.ln


data class SwatchInfo(val rgb: Int?, val titleColor: Int?, val bodyColor: Int?)

internal data class SwatchInfoColors(val colors: SwatchInfo? = null)

internal val LocalSwatchInfo = compositionLocalOf { SwatchInfoColors() }
internal val LocalSwatchChange = staticCompositionLocalOf<(SwatchInfo?) -> Unit> { {} }

/**
 * Returns the new background [Color] to use, representing the original background [color] with an
 * overlay corresponding to [elevation] applied. The overlay will only be applied to
 * [ColorScheme.surface].
 */
internal fun ColorScheme.applyTonalElevation(backgroundColor: Color, elevation: Dp): Color {
    return if (backgroundColor == surface) {
        surfaceColorAtElevation(elevation)
    } else {
        backgroundColor
    }
}

/**
 * Returns the [ColorScheme.surface] color with an alpha of the [ColorScheme.primary] color overlaid
 * on top of it.
 * Computes the surface tonal color at different elevation levels e.g. surface1 through surface5.
 *
 * @param elevation Elevation value used to compute alpha of the color overlay layer.
 */
internal fun ColorScheme.surfaceColorAtElevation(
    elevation: Dp,
): Color {
    if (elevation == 0.dp) return surface
    val alpha = ((4.5f * ln(elevation.value + 1)) + 2f) / 100f
    return primary.copy(alpha = alpha).compositeOver(surface)
}

internal fun Color.surfaceColorAtElevation(
    elevation: Dp,
    surface: Color,
): Color {
    if (elevation == 0.dp) return surface
    val alpha = ((4.5f * ln(elevation.value + 1)) + 2f) / 100f
    return copy(alpha = alpha).compositeOver(surface)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DetailActions(
    genericInfo: GenericInfo,
    scaffoldState: DrawerState,
    navController: NavHostController,
    scope: CoroutineScope,
    context: Context,
    info: InfoModel,
    listDao: ListDao,
    hostState: SnackbarHostState,
    topBarColor: Color,
    isSaved: Boolean,
    dao: ItemDao,
    onReverseChaptersClick: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    var showLists by remember { mutableStateOf(false) }

    if (showLists) {
        BackHandler { showLists = false }

        ModalBottomSheet(
            onDismissRequest = { showLists = false },
            windowInsets = WindowInsets.systemBars.only(WindowInsetsSides.Top)
        ) {
            ListChoiceScreen(
                url = info.url,
                onClick = { item ->
                    scope.launch {
                        showLists = false
                        val result = listDao.addToList(
                            item.item.uuid,
                            info.title,
                            info.description,
                            info.url,
                            info.imageUrl,
                            info.source.serviceName
                        )
                        hostState.showSnackbar(
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
                    IconButton(onClick = { showLists = false }) { Icon(Icons.Default.Close, null) }
                },
            )
        }
    }

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
                showLists = true
            },
            text = { Text(stringResource(R.string.add_to_list)) },
            leadingIcon = { Icon(Icons.Default.SaveAs, null) }
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

        DropdownMenuItem(
            onClick = {
                dropDownDismiss()
                Screen.GlobalSearchScreen.navigate(navController, info.title)
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
    ) { Icon(Icons.Default.Share, null, tint = topBarColor) }

    genericInfo.DetailActions(infoModel = info, tint = topBarColor)

    IconButton(onClick = { showDropDown = true }) {
        Icon(Icons.Default.MoreVert, null, tint = topBarColor)
    }
}