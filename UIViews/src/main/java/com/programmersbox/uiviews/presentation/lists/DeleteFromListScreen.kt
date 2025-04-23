package com.programmersbox.uiviews.presentation.lists

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.programmersbox.favoritesdatabase.CustomListInfo
import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.sharedutils.AppLogo
import com.programmersbox.uiviews.R
import com.programmersbox.uiviews.presentation.components.M3CoverCard
import com.programmersbox.uiviews.theme.LocalCustomListDao
import com.programmersbox.uiviews.utils.LocalNavController
import com.programmersbox.uiviews.utils.adaptiveGridCell
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeleteFromListScreen(
    deleteFromList: Screen.CustomListScreen.DeleteFromList,
) {
    val logoDrawable = koinInject<AppLogo>()
    val navController = LocalNavController.current
    val dao = LocalCustomListDao.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()
    val onDismiss: () -> Unit = {
        scope.launch { sheetState.hide() }
        navController.popBackStack()
    }

    val customList by dao
        .getCustomListItemFlow(deleteFromList.uuid)
        .collectAsStateWithLifecycle(null)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        sheetState = sheetState
    ) {
        val itemsToDelete = remember { mutableStateListOf<CustomListInfo>() }
        var showPopup by remember { mutableStateOf(false) }
        var removing by remember { mutableStateOf(false) }

        if (showPopup) {
            val onPopupDismiss = { showPopup = false }

            AlertDialog(
                onDismissRequest = if (removing) {
                    {}
                } else onPopupDismiss,
                title = { Text("Delete") },
                text = {
                    Text(
                        context.resources.getQuantityString(
                            R.plurals.areYouSureRemove,
                            itemsToDelete.size,
                            itemsToDelete.size
                        )
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            removing = true
                            scope.launch {
                                runCatching {
                                    itemsToDelete.forEach { item -> dao.removeItem(item) }
                                    customList?.item?.let { dao.updateFullList(it) }
                                }.onSuccess {
                                    removing = false
                                    itemsToDelete.clear()
                                    onPopupDismiss()
                                    onDismiss()
                                }
                            }
                        },
                        enabled = !removing
                    ) { Text(stringResource(R.string.yes)) }
                },
                dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.no)) } },
            )
        }

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(stringResource(R.string.delete_multiple)) },
                    windowInsets = WindowInsets(0.dp),
                )
            },
            bottomBar = {
                BottomAppBar(
                    contentPadding = PaddingValues(0.dp),
                    windowInsets = WindowInsets(0.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp)
                    ) { Text(stringResource(id = R.string.cancel)) }

                    Button(
                        onClick = { showPopup = true },
                        enabled = itemsToDelete.isNotEmpty(),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp)
                    ) { Text(stringResource(id = R.string.remove)) }
                }
            }
        ) { padding ->
            LazyVerticalGrid(
                columns = adaptiveGridCell(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = padding,
                modifier = Modifier.padding(4.dp),
            ) {
                customList
                    ?.list
                    ?.groupBy { it.source }
                    ?.forEach { (t, u) ->
                        item(
                            span = { GridItemSpan(maxLineSpan) }
                        ) {
                            Column {
                                HorizontalDivider()
                                CenterAlignedTopAppBar(
                                    title = { Text(t) },
                                    windowInsets = WindowInsets(0.dp),
                                )
                            }
                        }
                        items(u) { item ->
                            val transition = updateTransition(targetState = item in itemsToDelete, label = "")
                            val outlineColor = MaterialTheme.colorScheme.outline
                            M3CoverCard(
                                imageUrl = item.imageUrl,
                                name = item.title,
                                placeHolder = logoDrawable.logo,
                                onClick = {
                                    if (item in itemsToDelete) itemsToDelete.remove(item) else itemsToDelete.add(item)
                                },
                                modifier = Modifier
                                    .animateItem()
                                    .border(
                                        border = BorderStroke(
                                            transition.animateDp(label = "border_width") { target -> if (target) 4.dp else 1.dp }.value,
                                            transition.animateColor(label = "border_color") { target -> if (target) Color(0xfff44336) else outlineColor }.value
                                        ),
                                        shape = MaterialTheme.shapes.medium
                                    )
                            )
                        }
                    }
            }
        }
    }
}