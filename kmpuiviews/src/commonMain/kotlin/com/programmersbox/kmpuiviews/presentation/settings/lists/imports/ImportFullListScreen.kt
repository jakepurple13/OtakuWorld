package com.programmersbox.kmpuiviews.presentation.settings.lists.imports

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.programmersbox.favoritesdatabase.CustomList
import com.programmersbox.favoritesdatabase.CustomListInfo
import com.programmersbox.kmpuiviews.painterLogo
import com.programmersbox.kmpuiviews.presentation.components.BackButton
import com.programmersbox.kmpuiviews.presentation.components.M3CoverCard
import com.programmersbox.kmpuiviews.presentation.components.NormalOtakuScaffold
import com.programmersbox.kmpuiviews.utils.ComposableUtils
import com.programmersbox.kmpuiviews.utils.HideNavBarWhileOnScreen
import com.programmersbox.kmpuiviews.utils.LocalNavActions
import com.programmersbox.kmpuiviews.utils.LocalNavHostPadding
import com.programmersbox.kmpuiviews.utils.adaptiveGridCell
import com.programmersbox.kmpuiviews.utils.composables.imageloaders.ImageLoaderChoice
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.areYouSureRemove
import otakuworld.kmpuiviews.generated.resources.cancel
import otakuworld.kmpuiviews.generated.resources.confirm
import otakuworld.kmpuiviews.generated.resources.delete
import otakuworld.kmpuiviews.generated.resources.delete_multiple
import otakuworld.kmpuiviews.generated.resources.import_import_list
import otakuworld.kmpuiviews.generated.resources.importing_import_list
import otakuworld.kmpuiviews.generated.resources.no
import otakuworld.kmpuiviews.generated.resources.remove
import otakuworld.kmpuiviews.generated.resources.remove_items
import otakuworld.kmpuiviews.generated.resources.something_went_wrong
import otakuworld.kmpuiviews.generated.resources.update_list_name_title
import otakuworld.kmpuiviews.generated.resources.yes

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ImportFullListScreen(
    vm: ImportFullListViewModel = koinViewModel(),
) {
    HideNavBarWhileOnScreen()

    val scope = rememberCoroutineScope()
    val navController = LocalNavActions.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    val snackbarHostState = remember { SnackbarHostState() }

    when (val status = vm.importStatus) {
        ImportFullListStatus.Loading -> {
            LaunchedEffect(Unit) {
                snackbarHostState.showSnackbar("Importing...")
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) { CircularWavyProgressIndicator() }
        }

        is ImportFullListStatus.Error -> {
            LaunchedEffect(Unit) {
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar("Error")
            }
            NormalOtakuScaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(stringResource(Res.string.importing_import_list)) },
                        navigationIcon = { BackButton() },
                        scrollBehavior = scrollBehavior
                    )
                },
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                contentWindowInsets = ScaffoldDefaults.contentWindowInsets,
            ) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Image(
                        Icons.Default.Warning,
                        null,
                        modifier = Modifier.size(50.dp),
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground)
                    )
                    Text(stringResource(Res.string.something_went_wrong), style = MaterialTheme.typography.titleLarge)
                    Text(status.throwable.message.orEmpty())
                }
            }
        }

        is ImportFullListStatus.Success -> {
            LaunchedEffect(Unit) {
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar("Completed!")
            }
            NormalOtakuScaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(stringResource(Res.string.importing_import_list)) },
                        navigationIcon = { BackButton() },
                        actions = { Text("(${vm.importingList.size})") },
                        scrollBehavior = scrollBehavior
                    )
                },
                bottomBar = {
                    BottomAppBar(
                        windowInsets = WindowInsets(0.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    vm.importList()
                                    navController.popBackStack()
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(stringResource(Res.string.import_import_list)) }
                    }
                },
                contentWindowInsets = ScaffoldDefaults.contentWindowInsets,
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                modifier = Modifier
                    .padding(LocalNavHostPadding.current)
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
            ) { padding ->
                LazyColumn(
                    contentPadding = padding,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(vertical = 4.dp),
                ) {
                    itemsIndexed(vm.importingList) { index, it ->

                        var showRemoveSheet by remember { mutableStateOf(false) }

                        if (showRemoveSheet) {
                            RemoveFromList(
                                customList = it,
                                onDismiss = { showRemoveSheet = false },
                                sheetState = rememberModalBottomSheetState(),
                                onRemove = { list ->
                                    vm.importingList[index] =
                                        it.copy(list = it.list.filter { item -> item !in list })
                                },
                            )
                        }

                        var showInfoSheet by remember { mutableStateOf(false) }

                        if (showInfoSheet) {
                            InfoSheet(
                                customItem = it,
                                sheetState = rememberModalBottomSheetState(),
                                rename = { newName ->
                                    vm.importingList[index] = it.copy(item = it.item.copy(name = newName))
                                },
                                onDismiss = { showInfoSheet = false },
                                onDeleteListAction = { vm.importingList.remove(it) },
                                onRemoveItemsAction = { showRemoveSheet = true },
                                onUseBiometricAction = { change ->
                                    vm.importingList[index] = it.copy(item = it.item.copy(useBiometric = change))
                                },
                            )
                        }

                        OutlinedCard(
                            onClick = { showInfoSheet = true },
                            modifier = Modifier.animateItem()
                        ) {
                            ListItem(
                                headlineContent = { Text(it.item.name) },
                                trailingContent = { Text(it.list.size.toString()) },
                                supportingContent = {
                                    Column {
                                        it
                                            .list
                                            .take(3)
                                            .forEach { item -> Text(item.title) }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun InfoSheet(
    customItem: CustomList,
    sheetState: SheetState,
    rename: (String) -> Unit,
    onDismiss: () -> Unit,
    onDeleteListAction: () -> Unit,
    onRemoveItemsAction: () -> Unit,
    onUseBiometricAction: (Boolean) -> Unit,
) {
    val scope = rememberCoroutineScope()

    var currentName by remember { mutableStateOf(customItem.item.name) }

    var showAdd by remember { mutableStateOf(false) }

    if (showAdd) {
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text(stringResource(Res.string.update_list_name_title)) },
            text = { Text("Are you sure you want to change the name?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        rename(currentName)
                        showAdd = false
                    }
                ) { Text(stringResource(Res.string.confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showAdd = false }) { Text(stringResource(Res.string.cancel)) }
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            OutlinedTextField(
                currentName,
                onValueChange = { currentName = it },
                shape = MaterialTheme.shapes.large,
                trailingIcon = {
                    IconButton(
                        onClick = { showAdd = true },
                        enabled = currentName != customItem.item.name
                    ) { Icon(Icons.Default.Check, null) }
                },
                modifier = Modifier.fillMaxWidth()
            )
            ListItem(
                headlineContent = {},
                leadingContent = {
                    ImageLoaderChoice(
                        imageUrl = customItem.list.firstOrNull()?.imageUrl.orEmpty(),
                        name = customItem.item.name,
                        placeHolder = { painterLogo() },
                        modifier = Modifier
                            .size(ComposableUtils.IMAGE_WIDTH, ComposableUtils.IMAGE_HEIGHT)
                            .clip(MaterialTheme.shapes.small)
                    )
                },
                supportingContent = {
                    Column {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Require Biometrics?")
                            Checkbox(
                                checked = customItem.item.useBiometric,
                                onCheckedChange = onUseBiometricAction
                            )
                        }
                    }
                },
                colors = ListItemDefaults.colors(
                    containerColor = Color.Transparent,
                )
            )

            HorizontalDivider()

            Text("List Count: ${customItem.list.size}")

            HorizontalDivider()

            FlowRow(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth()
            ) {
                ActionItem(
                    onClick = onRemoveItemsAction,
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.error,
                    )
                ) {
                    Icon(Icons.Default.RemoveCircle, null)
                    Text(stringResource(Res.string.remove_items))
                }

                ActionItem(
                    onClick = {
                        scope.launch { sheetState.hide() }
                            .invokeOnCompletion {
                                onDismiss()
                                onDeleteListAction()
                            }
                    },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    ),
                ) {
                    Icon(Icons.Default.Delete, null)
                    Text(stringResource(Res.string.delete))
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .height(400.dp)
                    .fillMaxWidth()
            ) {
                customItem
                    .list
                    .groupBy { it.source }
                    .forEach { items ->
                        item(
                            span = { GridItemSpan(maxLineSpan) }
                        ) {
                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                tonalElevation = 4.dp,
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .animateItem()
                            ) {
                                ListItem(
                                    modifier = Modifier.padding(4.dp),
                                    headlineContent = { Text(items.key) },
                                    leadingContent = { Text(items.value.size.toString()) },
                                    colors = ListItemDefaults.colors(
                                        containerColor = Color.Transparent,
                                    )
                                )
                            }
                        }

                        items(items.value) {
                            M3CoverCard(
                                imageUrl = it.imageUrl,
                                name = it.title,
                                placeHolder = { painterLogo() }
                            )
                        }
                    }
            }
        }
    }
}

@Composable
private fun ActionItem(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    colors: CardColors = CardDefaults.cardColors(),
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        onClick = onClick,
        colors = colors,
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(8.dp),
            content = content
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RemoveFromList(
    customList: CustomList,
    onDismiss: () -> Unit,
    sheetState: SheetState,
    onRemove: (List<CustomListInfo>) -> Unit,
) {
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
                        pluralStringResource(
                            Res.plurals.areYouSureRemove,
                            itemsToDelete.size,
                            itemsToDelete.size
                        )
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onRemove(itemsToDelete)
                            onPopupDismiss()
                            onDismiss()
                        },
                    ) { Text(stringResource(Res.string.yes)) }
                },
                dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.no)) } },
            )
        }

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(stringResource(Res.string.delete_multiple)) },
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
                    ) { Text(stringResource(Res.string.cancel)) }

                    Button(
                        onClick = { showPopup = true },
                        enabled = itemsToDelete.isNotEmpty(),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp)
                    ) { Text(stringResource(Res.string.remove)) }
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
                    .list
                    .groupBy { it.source }
                    .forEach { (t, u) ->
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
                                placeHolder = { painterLogo() },
                                onClick = {
                                    if (item in itemsToDelete) itemsToDelete.remove(item) else itemsToDelete.add(item)
                                },
                                modifier = Modifier
                                    .animateItem()
                                    .border(
                                        border = BorderStroke(
                                            transition.animateDp(label = "border_width") { target ->
                                                if (target) 4.dp else 1.dp
                                            }.value,
                                            transition.animateColor(label = "border_color") { target ->
                                                if (target) Color(0xfff44336) else outlineColor
                                            }.value
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