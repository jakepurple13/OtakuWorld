package com.programmersbox.kmpuiviews.presentation.settings.lists.imports

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.programmersbox.favoritesdatabase.CustomListInfo
import com.programmersbox.favoritesdatabase.ListDao
import com.programmersbox.kmpuiviews.painterLogo
import com.programmersbox.kmpuiviews.presentation.components.BackButton
import com.programmersbox.kmpuiviews.presentation.components.NormalOtakuScaffold
import com.programmersbox.kmpuiviews.utils.ComposableUtils
import com.programmersbox.kmpuiviews.utils.HideNavBarWhileOnScreen
import com.programmersbox.kmpuiviews.utils.LocalCustomListDao
import com.programmersbox.kmpuiviews.utils.LocalNavActions
import com.programmersbox.kmpuiviews.utils.LocalNavHostPadding
import com.programmersbox.kmpuiviews.utils.composables.imageloaders.ImageLoaderChoice
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.import_import_list
import otakuworld.kmpuiviews.generated.resources.importing_import_list
import otakuworld.kmpuiviews.generated.resources.list_name
import otakuworld.kmpuiviews.generated.resources.something_went_wrong

//TODO: Might be removing this
// Need to keep this for legacy
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ImportListScreen(
    listDao: ListDao = LocalCustomListDao.current,
    vm: ImportListViewModel = koinViewModel(),
) {
    HideNavBarWhileOnScreen()

    val scope = rememberCoroutineScope()
    val navController = LocalNavActions.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(LocalNavHostPadding.current)
            )
        }
    ) { _ ->
        when (val status = vm.importStatus) {
            ImportListStatus.Loading -> {
                LaunchedEffect(Unit) {
                    snackbarHostState.showSnackbar("Importing...")
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) { CircularWavyProgressIndicator() }
            }

            is ImportListStatus.Error -> {
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

            is ImportListStatus.Success -> {
                LaunchedEffect(Unit) {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    snackbarHostState.showSnackbar("Completed!")
                }
                val lists by listDao.getAllLists().collectAsStateWithLifecycle(emptyList())
                var name by remember(status.customList?.item) { mutableStateOf(status.customList?.item?.name.orEmpty()) }
                NormalOtakuScaffold(
                    topBar = {
                        Column {
                            TopAppBar(
                                title = { Text(stringResource(Res.string.importing_import_list)) },
                                navigationIcon = { BackButton() },
                                actions = { Text("(${status.customList?.list.orEmpty().size})") },
                                scrollBehavior = scrollBehavior
                            )

                            Surface {
                                OutlinedTextField(
                                    value = name,
                                    onValueChange = { name = it },
                                    label = { Text(stringResource(Res.string.list_name)) },
                                    placeholder = { Text(status.customList?.item?.name.orEmpty()) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                    isError = lists.any { it.item.name == name },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    },
                    bottomBar = {
                        BottomAppBar(
                            windowInsets = WindowInsets(0.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        vm.importList(name)
                                        navController.popBackStack()
                                    }
                                },
                                enabled = lists.none { it.item.name == name },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text(stringResource(Res.string.import_import_list)) }
                        }
                    },
                    contentWindowInsets = ScaffoldDefaults.contentWindowInsets,
                    modifier = Modifier
                        .padding(LocalNavHostPadding.current)
                        .nestedScroll(scrollBehavior.nestedScrollConnection)
                ) { padding ->
                    LazyColumn(
                        contentPadding = padding,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(vertical = 4.dp),
                    ) {
                        items(status.customList?.list.orEmpty()) { item ->
                            CustomItem(
                                item = item,
                                modifier = Modifier.animateItem()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomItem(
    item: CustomListInfo,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier = modifier
            .height(ComposableUtils.IMAGE_HEIGHT)
            .padding(horizontal = 4.dp)
    ) {
        Row {
            ImageLoaderChoice(
                imageUrl = item.imageUrl,
                placeHolder = { painterLogo() },
                error = { painterLogo() },
                contentScale = ContentScale.Crop,
                name = item.title,
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .size(ComposableUtils.IMAGE_WIDTH, ComposableUtils.IMAGE_HEIGHT)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp, top = 4.dp)
            ) {
                Text(item.source, style = MaterialTheme.typography.labelMedium)
                Text(item.title, style = MaterialTheme.typography.titleSmall)
                Text(item.description, style = MaterialTheme.typography.bodyMedium, maxLines = 3)
            }
        }
    }
}