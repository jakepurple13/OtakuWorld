package com.programmersbox.uiviews.presentation.lists.imports

import android.content.Context
import android.graphics.drawable.Drawable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.programmersbox.favoritesdatabase.CustomListInfo
import com.programmersbox.favoritesdatabase.ListDao
import com.programmersbox.kmpuiviews.presentation.components.BackButton
import com.programmersbox.kmpuiviews.presentation.components.NormalOtakuScaffold
import com.programmersbox.kmpuiviews.presentation.settings.lists.imports.ImportListStatus
import com.programmersbox.kmpuiviews.presentation.settings.lists.imports.ImportListViewModel
import com.programmersbox.kmpuiviews.utils.ComposableUtils
import com.programmersbox.kmpuiviews.utils.HideNavBarWhileOnScreen
import com.programmersbox.kmpuiviews.utils.LocalCustomListDao
import com.programmersbox.kmpuiviews.utils.LocalNavController
import com.programmersbox.kmpuiviews.utils.LocalNavHostPadding
import com.programmersbox.sharedutils.AppLogo
import com.programmersbox.uiviews.R
import com.programmersbox.uiviews.presentation.components.imageloaders.ImageLoaderChoice
import com.programmersbox.uiviews.utils.InsetSmallTopAppBar
import com.programmersbox.uiviews.utils.LightAndDarkPreviews
import com.programmersbox.uiviews.utils.PreviewTheme
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import java.util.UUID

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
    val navController = LocalNavController.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    val logoDrawable = koinInject<AppLogo>().logo

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
                        InsetSmallTopAppBar(
                            title = { Text(stringResource(R.string.importing_import_list)) },
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
                        Text(stringResource(id = R.string.something_went_wrong), style = MaterialTheme.typography.titleLarge)
                        Text(status.throwable.localizedMessage.orEmpty())
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
                            InsetSmallTopAppBar(
                                title = { Text(stringResource(R.string.importing_import_list)) },
                                navigationIcon = { BackButton() },
                                actions = { Text("(${status.customList?.list.orEmpty().size})") },
                                scrollBehavior = scrollBehavior
                            )

                            Surface {
                                OutlinedTextField(
                                    value = name,
                                    onValueChange = { name = it },
                                    label = { Text(stringResource(R.string.list_name)) },
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
                            ) { Text(stringResource(R.string.import_import_list)) }
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
                                logoDrawable = logoDrawable,
                                modifier = Modifier.animateItem()
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun CustomItem(
    item: CustomListInfo,
    logoDrawable: Drawable?,
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
                placeHolder = rememberDrawablePainter(logoDrawable),
                error = rememberDrawablePainter(logoDrawable),
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

@LightAndDarkPreviews
@Composable
private fun ImportScreenPreview() {
    PreviewTheme {
        val listDao: ListDao = LocalCustomListDao.current
        val context: Context = LocalContext.current
        val vm: ImportListViewModel = viewModel { ImportListViewModel(listDao, createSavedStateHandle()) }
        ImportListScreen(
            listDao = listDao,
            vm = vm
        )
    }
}

@LightAndDarkPreviews
@Composable
private fun ImportItemPreview() {
    PreviewTheme {
        CustomItem(
            item = CustomListInfo(
                uuid = UUID.randomUUID().toString(),
                title = "Title",
                description = "description",
                url = "",
                imageUrl = "",
                source = "MANGA_READ"
            ),
            logoDrawable = null
        )
    }
}