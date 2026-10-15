package com.programmersbox.kmpuiviews.presentation.settings.extensions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.programmersbox.kmpuiviews.presentation.components.BackButton
import com.programmersbox.sharedcomponents.components.HideNavBarWhileOnScreen
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtensionCatalogScreen(
    catalogName: String,
    viewModel: ExtensionListViewModel = koinViewModel(),
) {
    HideNavBarWhileOnScreen()
    val ui = viewModel.catalogs[catalogName]
    val all = (ui as? CatalogUi.Loaded)?.items.orEmpty()
    var query by rememberSaveable { mutableStateOf("") }
    var lang by rememberSaveable { mutableStateOf<String?>(null) }
    var hideNsfw by rememberSaveable { mutableStateOf(false) }
    var sheetKey by rememberSaveable { mutableStateOf<String?>(null) }
    RemoteItemSheet(viewModel, sheetKey, onDismiss = { sheetKey = null })

    val languages = all.languages()
    val hasNsfw = all.any { it.remote.nsfw }
    val shown = all.filterCatalog(lang, hideNsfw).search(query)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (ui is CatalogUi.Loaded) "$catalogName · ${all.size}" else catalogName) },
                navigationIcon = { BackButton() },
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            ExtensionSearchField(query, onQueryChange = { query = it }, placeholder = "Search $catalogName")
            if (languages.size > 1 || hasNsfw) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                ) {
                    item { FilterChip(selected = lang == null, onClick = { lang = null }, label = { Text("All") }) }
                    items(languages) { l ->
                        FilterChip(selected = lang == l, onClick = { lang = l }, label = { Text(displayLang(l)) })
                    }
                    if (hasNsfw) {
                        item { FilterChip(selected = hideNsfw, onClick = { hideNsfw = !hideNsfw }, label = { Text("Hide 18+") }) }
                    }
                }
            }
            when (ui) {
                null, CatalogUi.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularWavyProgressIndicator()
                }

                is CatalogUi.Error -> CatalogError(ui.message, onRetry = { viewModel.retry(catalogName) })
                is CatalogUi.Loaded -> LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(shown, key = { it.key }, contentType = { "tile" }) { item ->
                        ExtensionTile(item, showCatalog = false, onClick = { sheetKey = item.key })
                    }
                }
            }
        }
    }
}
