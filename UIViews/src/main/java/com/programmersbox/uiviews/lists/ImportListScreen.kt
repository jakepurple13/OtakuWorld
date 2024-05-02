package com.programmersbox.uiviews.lists

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bumptech.glide.integration.compose.CrossFade
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.dokar.sonner.ToastType
import com.dokar.sonner.rememberToasterState
import com.programmersbox.favoritesdatabase.CustomListInfo
import com.programmersbox.favoritesdatabase.ListDao
import com.programmersbox.sharedutils.AppLogo
import com.programmersbox.uiviews.R
import com.programmersbox.uiviews.utils.BackButton
import com.programmersbox.uiviews.utils.ComposableUtils
import com.programmersbox.uiviews.utils.InsetSmallTopAppBar
import com.programmersbox.uiviews.utils.LightAndDarkPreviews
import com.programmersbox.uiviews.utils.LocalCustomListDao
import com.programmersbox.uiviews.utils.LocalNavController
import com.programmersbox.uiviews.utils.LocalNavHostPadding
import com.programmersbox.uiviews.utils.PreviewTheme
import com.programmersbox.uiviews.utils.Screen
import com.programmersbox.uiviews.utils.ToasterSetup
import com.programmersbox.uiviews.utils.ToasterUtils
import com.programmersbox.uiviews.utils.components.NormalOtakuScaffold
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import java.util.UUID
import kotlin.time.Duration

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ImportListScreen(
    importList: Screen.ImportListScreen,
    listDao: ListDao = LocalCustomListDao.current,
    context: Context = LocalContext.current,
    vm: ImportListViewModel = viewModel { ImportListViewModel(listDao, importList, context) },
) {
    val toaster = rememberToasterState()
    val scope = rememberCoroutineScope()
    val navController = LocalNavController.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    val logoDrawable = koinInject<AppLogo>().logo

    when (val status = vm.importStatus) {
        ImportListStatus.Loading -> {
            LaunchedEffect(Unit) {
                toaster.show(
                    "Importing...",
                    id = ToasterUtils.LOADING_TOAST_ID,
                    icon = ToasterUtils.LOADING_TOAST_ID,
                    duration = Duration.INFINITE,
                )
            }
            Box(Modifier.fillMaxSize()) {
                CircularProgressIndicator()
            }
        }

        is ImportListStatus.Error -> {
            LaunchedEffect(Unit) {
                toaster.dismiss(ToasterUtils.LOADING_TOAST_ID)
                toaster.show("Error", type = ToastType.Error)
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
                toaster.dismiss(ToasterUtils.LOADING_TOAST_ID)
                toaster.show("Completed!", type = ToastType.Success)
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

    ToasterSetup(toaster = toaster)
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
            GlideImage(
                model = item.imageUrl,
                transition = CrossFade,
                contentScale = ContentScale.Crop,
                contentDescription = item.title,
                loading = placeholder(logoDrawable),
                failure = placeholder(logoDrawable),
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
        val vm: ImportListViewModel = viewModel { ImportListViewModel(listDao, Screen.ImportListScreen(""), context) }
        ImportListScreen(
            importList = Screen.ImportListScreen(""),
            listDao = listDao,
            context = context,
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
                uuid = UUID.randomUUID(),
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