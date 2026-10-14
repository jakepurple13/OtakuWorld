package com.programmersbox.kmpuiviews.presentation.settings.lists

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.programmersbox.favoritesdatabase.CustomList
import com.programmersbox.kmpuiviews.painterLogo
import com.programmersbox.kmpuiviews.utils.composables.imageloaders.ImageLoaderChoice
import com.programmersbox.kmpuiviews.utils.rememberBiometricPrompting
import org.jetbrains.compose.resources.stringResource
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.cancel
import otakuworld.kmpuiviews.generated.resources.confirm

@Composable
private fun sourceColors() = listOf(
    MaterialTheme.colorScheme.primary,
    MaterialTheme.colorScheme.tertiary,
    MaterialTheme.colorScheme.secondary,
    MaterialTheme.colorScheme.primaryContainer,
    MaterialTheme.colorScheme.tertiaryContainer,
    MaterialTheme.colorScheme.secondaryContainer,
)

@Composable
internal fun ListHeroHeader(
    customList: CustomList,
    coverUrl: String,
    sources: List<Pair<String, Int>>,
    colorFilter: ColorFilter?,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxWidth()) {
        ImageLoaderChoice(
            imageUrl = coverUrl,
            name = "",
            placeHolder = { painterLogo() },
            contentScale = ContentScale.Crop,
            colorFilter = colorFilter,
            modifier = Modifier
                .matchParentSize()
                .blur(32.dp)
                .alpha(0.5f)
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        0.3f to Color.Transparent,
                        1f to MaterialTheme.colorScheme.surface,
                    )
                )
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .statusBarsPadding()
                .padding(start = 4.dp, end = 4.dp, bottom = 8.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "Edit list") }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.padding(horizontal = 12.dp)
            ) {
                ImageLoaderChoice(
                    imageUrl = coverUrl,
                    name = customList.item.name,
                    placeHolder = { painterLogo() },
                    contentScale = ContentScale.Crop,
                    colorFilter = colorFilter,
                    modifier = Modifier
                        .width(96.dp)
                        .aspectRatio(2f / 3f)
                        .clip(MaterialTheme.shapes.medium)
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        customList.item.name,
                        style = MaterialTheme.typography.headlineMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (customList.item.description.isNotBlank()) {
                        Text(
                            customList.item.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        "${customList.list.size} titles · ${sources.size} sources",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (customList.item.useBiometric) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Default.Lock,
                                null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                "Locked with biometrics",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            if (sources.size > 1) {
                SourceMixBar(
                    sources = sources,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun SourceMixBar(
    sources: List<Pair<String, Int>>,
    modifier: Modifier = Modifier,
) {
    val colors = sourceColors()
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(MaterialTheme.shapes.small)
    ) {
        sources.forEachIndexed { index, (_, count) ->
            Box(
                modifier = Modifier
                    .weight(count.toFloat())
                    .fillMaxHeight()
                    .background(colors[index % colors.size])
            )
        }
    }
}

@Composable
internal fun SourceChipsRow(
    sources: List<Pair<String, Int>>,
    total: Int,
    filtered: List<String>,
    allSourcesShown: Boolean,
    onSelectSource: (String) -> Unit,
    onShowAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        item {
            FilterChip(
                selected = allSourcesShown,
                onClick = onShowAll,
                label = { Text("All") },
                trailingIcon = { Text(total.toString()) },
            )
        }
        items(sources, key = { it.first }) { (source, count) ->
            FilterChip(
                selected = !allSourcesShown && source in filtered,
                onClick = { onSelectSource(source) },
                label = { Text(source) },
                trailingIcon = { Text(count.toString()) },
            )
        }
    }
}

@Composable
internal fun CollapsedListBar(
    customList: CustomList,
    coverUrl: String,
    countLabel: String,
    colorFilter: ColorFilter?,
    onBack: () -> Unit,
    containerColor: Color,
    modifier: Modifier = Modifier,
    chips: @Composable () -> Unit,
) {
    Surface(
        color = containerColor,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .statusBarsPadding()
                .padding(bottom = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
            ) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                ImageLoaderChoice(
                    imageUrl = coverUrl,
                    name = customList.item.name,
                    placeHolder = { painterLogo() },
                    contentScale = ContentScale.Crop,
                    colorFilter = colorFilter,
                    modifier = Modifier
                        .width(28.dp)
                        .aspectRatio(2f / 3f)
                        .clip(MaterialTheme.shapes.extraSmall)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        customList.item.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        countLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (customList.item.useBiometric) {
                    Icon(
                        Icons.Default.Lock,
                        null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(18.dp)
                    )
                }
            }
            chips()
        }
    }
}

internal enum class ListToolbarMode { Browse, Search, Select }

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun ListFloatingToolbar(
    mode: ListToolbarMode,
    searchQuery: TextFieldState,
    searchPlaceholder: String,
    selectedCount: Int,
    canDeleteList: Boolean,
    onSearch: () -> Unit,
    onCloseSearch: () -> Unit,
    onSort: () -> Unit,
    onShare: () -> Unit,
    onEdit: () -> Unit,
    onExport: () -> Unit,
    onDeleteList: () -> Unit,
    onRandom: () -> Unit,
    onClearSelection: () -> Unit,
    onSelectAll: () -> Unit,
    onAddToList: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (mode) {
        ListToolbarMode.Browse -> HorizontalFloatingToolbar(
            expanded = true,
            floatingActionButton = {
                FloatingToolbarDefaults.VibrantFloatingActionButton(onClick = onRandom) {
                    Icon(Icons.Default.Shuffle, "Random title")
                }
            },
            modifier = modifier,
        ) {
            IconButton(onClick = onSearch) { Icon(Icons.Default.Search, "Search") }
            IconButton(onClick = onSort) { Icon(Icons.AutoMirrored.Filled.Sort, "Sort and view") }
            IconButton(onClick = onShare) { Icon(Icons.Default.Share, "Share") }
            Box {
                var showMenu by remember { mutableStateOf(false) }
                IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, "More") }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit list") },
                        leadingIcon = { Icon(Icons.Default.Edit, null) },
                        onClick = {
                            showMenu = false
                            onEdit()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Export") },
                        leadingIcon = { Icon(Icons.Default.ImportExport, null) },
                        onClick = {
                            showMenu = false
                            onExport()
                        }
                    )
                    if (canDeleteList) {
                        DropdownMenuItem(
                            text = { Text("Delete list", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showMenu = false
                                onDeleteList()
                            }
                        )
                    }
                }
            }
        }

        ListToolbarMode.Search -> HorizontalFloatingToolbar(
            expanded = true,
            modifier = modifier,
        ) {
            val focusRequester = remember { FocusRequester() }
            LaunchedEffect(Unit) { focusRequester.requestFocus() }
            IconButton(onClick = onCloseSearch) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Close search") }
            TextField(
                state = searchQuery,
                placeholder = { Text(searchPlaceholder, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                lineLimits = TextFieldLineLimits.SingleLine,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                modifier = Modifier
                    .widthIn(max = 240.dp)
                    .focusRequester(focusRequester)
            )
            IconButton(
                onClick = { searchQuery.clearText() },
                enabled = searchQuery.text.isNotEmpty(),
            ) { Icon(Icons.Default.Close, "Clear search") }
        }

        ListToolbarMode.Select -> HorizontalFloatingToolbar(
            expanded = true,
            floatingActionButton = {
                FloatingToolbarDefaults.VibrantFloatingActionButton(
                    onClick = onRemove,
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ) { Icon(Icons.Default.Delete, "Remove from list") }
            },
            modifier = modifier,
        ) {
            IconButton(onClick = onClearSelection) { Icon(Icons.Default.Close, "Stop selecting") }
            Text(
                "$selectedCount selected",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            IconButton(onClick = onSelectAll) { Icon(Icons.Default.SelectAll, "Select all") }
            IconButton(onClick = onAddToList) { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, "Add to another list") }
        }
    }
}

private val ListSort.label
    get() = when (this) {
        ListSort.RecentlyAdded -> "Recently added"
        ListSort.Title -> "Title A–Z"
        ListSort.Source -> "Source"
    }

private val ListViewMode.label
    get() = when (this) {
        ListViewMode.Grid -> "Grid"
        ListViewMode.BySource -> "Grouped by source"
    }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun SortAndViewSheet(
    sort: ListSort,
    viewMode: ListViewMode,
    onSort: (ListSort) -> Unit,
    onViewMode: (ListViewMode) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
        ) {
            Text("Sort", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ListSort.entries.forEach {
                    FilterChip(
                        selected = sort == it,
                        onClick = { onSort(it) },
                        label = { Text(it.label) },
                    )
                }
            }
            Text("View", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ListViewMode.entries.forEach {
                    FilterChip(
                        selected = viewMode == it,
                        onClick = { onViewMode(it) },
                        label = { Text(it.label) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditListSheet(
    customList: CustomList,
    coverUrl: String,
    colorFilter: ColorFilter?,
    onChangeCover: () -> Unit,
    onSave: (name: String, description: String) -> Unit,
    addSecurityItem: (String) -> Unit,
    removeSecurityItem: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val biometric = rememberBiometricPrompting()
    var showSecurityDialog by remember { mutableStateOf(false) }
    var showRemoveSecurityDialog by remember { mutableStateOf(false) }

    if (showSecurityDialog) {
        AlertDialog(
            onDismissRequest = { showSecurityDialog = false },
            title = { Text("Require Authentication to View this list?") },
            text = { Text("This will require phone authentication to view this list") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSecurityDialog = false
                        biometric.authenticate(
                            title = "Add Authentication for ${customList.item.name}",
                            subtitle = "Enter Authentication to add",
                            negativeButtonText = "Never Mind",
                            onAuthenticationSucceeded = { addSecurityItem(customList.item.uuid) },
                            onAuthenticationFailed = { showSecurityDialog = false }
                        )
                    }
                ) { Text(stringResource(Res.string.confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showSecurityDialog = false }) { Text(stringResource(Res.string.cancel)) }
            }
        )
    }

    if (showRemoveSecurityDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveSecurityDialog = false },
            title = { Text("Remove Authentication?") },
            text = { Text("This will remove the phone authentication to view this list") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRemoveSecurityDialog = false
                        biometric.authenticate(
                            title = "Remove Authentication for ${customList.item.name}",
                            subtitle = "Enter Authentication to remove",
                            negativeButtonText = "Never Mind",
                            onAuthenticationSucceeded = { removeSecurityItem(customList.item.uuid) },
                            onAuthenticationFailed = { showRemoveSecurityDialog = false }
                        )
                    }
                ) { Text(stringResource(Res.string.confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveSecurityDialog = false }) { Text(stringResource(Res.string.cancel)) }
            }
        )
    }

    var name by remember { mutableStateOf(customList.item.name) }
    var description by remember { mutableStateOf(customList.item.description) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("Edit list", style = MaterialTheme.typography.titleLarge)

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Card(onClick = onChangeCover) {
                    ImageLoaderChoice(
                        imageUrl = coverUrl,
                        name = customList.item.name,
                        placeHolder = { painterLogo() },
                        contentScale = ContentScale.Crop,
                        colorFilter = colorFilter,
                        modifier = Modifier
                            .width(64.dp)
                            .aspectRatio(2f / 3f)
                    )
                }
                TextButton(onClick = onChangeCover) { Text("Change cover") }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Require biometrics", modifier = Modifier.weight(1f))
                Switch(
                    checked = customList.item.useBiometric,
                    onCheckedChange = {
                        if (it) showSecurityDialog = true
                        else showRemoveSecurityDialog = true
                    }
                )
            }

            Button(
                onClick = { onSave(name.trim(), description.trim()) },
                enabled = name.isNotBlank() &&
                        (name.trim() != customList.item.name || description.trim() != customList.item.description),
                modifier = Modifier.align(Alignment.End)
            ) { Text("Save") }
        }
    }
}
