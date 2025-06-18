package com.programmersbox.kmpuiviews.presentation.settings.utils

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.programmersbox.datastore.DataStoreHandling
import com.programmersbox.kmpmodels.KmpSourceInformation
import com.programmersbox.kmpuiviews.presentation.components.ListBottomScreen
import com.programmersbox.kmpuiviews.presentation.components.ListBottomSheetItemModel
import com.programmersbox.kmpuiviews.utils.LocalItemDao
import com.programmersbox.kmpuiviews.utils.LocalNavActions
import com.programmersbox.kmpuiviews.utils.LocalSourcesRepository
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.chooseASource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun showSourceChooser(): MutableState<Boolean> {
    val showSourceChooser = remember { mutableStateOf(false) }
    val state = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    if (showSourceChooser.value) {
        ModalBottomSheet(
            onDismissRequest = { showSourceChooser.value = false },
            sheetState = state,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            SourceChooserScreen(
                onChosen = {
                    scope.launch { state.hide() }
                        .invokeOnCompletion { showSourceChooser.value = false }
                }
            )
        }
    }

    return showSourceChooser
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourceChooserScreen(
    onChosen: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val dataStoreHandling = koinInject<DataStoreHandling>()
    val sourceRepository = LocalSourcesRepository.current
    val itemDao = LocalItemDao.current

    val currentService by dataStoreHandling.currentService
        .asFlow()
        .collectAsStateWithLifecycle(null)

    var showChooser by remember { mutableStateOf<List<KmpSourceInformation>?>(null) }

    val languageCode = remember { Locale.current.language }

    showChooser?.let { list ->
        ModalBottomSheet(
            onDismissRequest = { showChooser = null },
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            ListBottomScreen(
                includeInsetPadding = false,
                title = stringResource(Res.string.chooseASource),
                list = list,
                navigationIcon = {
                    IconButton(
                        onClick = onChosen
                    ) { Icon(Icons.Default.Close, null) }
                },
                onClick = { service ->
                    onChosen()
                    scope.launch { dataStoreHandling.currentService.set(service.apiService.serviceName) }
                },
                lazyListContent = {
                    list
                        .find { it.apiService.serviceName == "${it.name}$languageCode" }
                        ?.let {
                            ListBottomSheetItemModel(
                                primaryText = it.apiService.serviceName,
                                icon = if (it.apiService.serviceName == currentService) Icons.Default.Check else null
                            )
                        }
                        ?.let { c ->
                            item {
                                ListItem(
                                    modifier = Modifier.clickable {
                                        onChosen()
                                        scope.launch { dataStoreHandling.currentService.set(c.primaryText) }
                                    },
                                    leadingContent = c.icon?.let { i -> { Icon(i, null) } },
                                    headlineContent = { Text("Current Language Source") },
                                    supportingContent = { Text(c.primaryText) }
                                )
                                HorizontalDivider()
                            }
                        }
                }
            ) {
                ListBottomSheetItemModel(
                    primaryText = it.apiService.serviceName,
                    icon = if (it.apiService.serviceName == currentService) Icons.Default.Check else null
                )
            }
        }
    }

    GroupBottomScreen(
        includeInsetPadding = false,
        title = stringResource(Res.string.chooseASource),
        list = remember {
            combine(
                sourceRepository.sources,
                itemDao.getSourceOrder()
            ) { list, order ->
                list
                    .filterNot { it.apiService.notWorking }
                    .sortedBy { order.find { o -> o.source == it.packageName }?.order ?: 0 }
                    .groupBy { it.packageName }
            }
        }
            .collectAsStateWithLifecycle(emptyMap())
            .value,
        onClick = { service ->
            if (service.size == 1) {
                onChosen()
                service.firstOrNull()?.apiService?.serviceName?.let {
                    scope.launch { dataStoreHandling.currentService.set(it) }
                }
            } else {
                showChooser = service
            }
        }
    ) {
        ListBottomSheetItemModel(
            primaryText = "${it.firstOrNull()?.name ?: "Nothing"} (${it.size})",
            icon = if (it.firstOrNull()?.apiService?.serviceName == currentService) Icons.Default.Check else null,
            overlineText = it.firstOrNull()?.packageName
        )
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun <T> GroupBottomScreen(
    title: String,
    list: Map<String, T>,
    onClick: (T) -> Unit,
    includeInsetPadding: Boolean = true,
    navigationIcon: @Composable () -> Unit = {
        val navController = LocalNavActions.current
        IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.Close, null) }
    },
    lazyListContent: LazyListScope.() -> Unit = {},
    itemContent: (T) -> ListBottomSheetItemModel,
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.navigationBarsPadding()
    ) {
        stickyHeader {
            TopAppBar(
                windowInsets = if (includeInsetPadding) WindowInsets.statusBars else WindowInsets(0.dp),
                title = { Text(title) },
                navigationIcon = navigationIcon,
                actions = { if (list.isNotEmpty()) Text("(${list.size})") }
            )
            HorizontalDivider()
        }
        lazyListContent()
        itemsIndexed(list.entries.toList()) { index, it ->
            val c = itemContent(it.value)
            ListItem(
                modifier = Modifier.clickable { onClick(it.value) },
                leadingContent = c.icon?.let { i -> { Icon(i, null) } },
                headlineContent = { Text(c.primaryText) },
                supportingContent = c.secondaryText?.let { i -> { Text(i) } },
                overlineContent = c.overlineText?.let { i -> { Text(i) } },
                trailingContent = c.trailingText?.let { i -> { Text(i) } }
            )
            if (index < list.size - 1) HorizontalDivider()
        }
    }
}