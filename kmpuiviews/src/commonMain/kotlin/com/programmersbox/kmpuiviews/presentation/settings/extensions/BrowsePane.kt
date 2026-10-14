package com.programmersbox.kmpuiviews.presentation.settings.extensions

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.kmpuiviews.utils.LocalNavActions

private const val SHELF_SIZE = 6

@Composable
internal fun BrowsePane(
    viewModel: ExtensionListViewModel,
    modifier: Modifier = Modifier,
) {
    val navActions = LocalNavActions.current
    var query by rememberSaveable { mutableStateOf("") }
    var sheetKey by rememberSaveable { mutableStateOf<String?>(null) }
    RemoteItemSheet(viewModel, sheetKey, onDismiss = { sheetKey = null })

    val results by remember {
        derivedStateOf {
            viewModel.catalogs.values
                .filterIsInstance<CatalogUi.Loaded>()
                .flatMap { it.items }
                .search(query)
                .shelfOrder()
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        ExtensionSearchField(query, onQueryChange = { query = it }, placeholder = "Search all catalogs")

        if (query.isNotBlank()) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(results, key = { it.key }, contentType = { "tile" }) { item ->
                    ExtensionTile(item, showCatalog = true, onClick = { sheetKey = item.key })
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 24.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                viewModel.catalogs.forEach { (name, ui) ->
                    item(key = "header:$name") {
                        ShelfHeader(
                            name = name,
                            count = (ui as? CatalogUi.Loaded)?.items?.size,
                            onSeeAll = { navActions.navigate(Screen.ExtensionListScreen.Catalog(name)) },
                        )
                    }
                    item(key = "body:$name") {
                        when (ui) {
                            CatalogUi.Loading -> Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(96.dp)
                            ) { CircularProgressIndicator() }

                            is CatalogUi.Error -> CatalogError(ui.message, onRetry = { viewModel.retry(name) })
                            is CatalogUi.Loaded -> TileRows(
                                ui.items.take(SHELF_SIZE),
                                showCatalog = false,
                                onClick = { sheetKey = it.key }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun ExtensionSearchField(query: String, onQueryChange: (String) -> Unit, placeholder: String) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(placeholder) },
        leadingIcon = { Icon(Icons.Default.Search, null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) { Icon(Icons.Default.Clear, "Clear") }
            }
        },
        singleLine = true,
        shape = MaterialTheme.shapes.extraLarge,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    )
}

@Composable
private fun ShelfHeader(name: String, count: Int?, onSeeAll: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 4.dp, top = 12.dp)
    ) {
        Text(name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        if (count != null && count > SHELF_SIZE) {
            TextButton(onClick = onSeeAll) { Text("See all $count") }
        }
    }
}

@Composable
internal fun CatalogError(message: String?, onRetry: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        ListItem(
            headlineContent = { Text("Couldn't load this catalog") },
            supportingContent = message?.let { { Text(it) } },
            leadingContent = { Icon(Icons.Default.CloudOff, null) },
            trailingContent = { TextButton(onClick = onRetry) { Text("Retry") } },
            colors = ListItemDefaults.colors(
                containerColor = Color.Transparent,
                headlineColor = MaterialTheme.colorScheme.onErrorContainer,
                supportingColor = MaterialTheme.colorScheme.onErrorContainer,
                leadingIconColor = MaterialTheme.colorScheme.onErrorContainer,
            )
        )
    }
}
