package com.programmersbox.uiviews.lists

import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.programmersbox.favoritesdatabase.ListDao
import com.programmersbox.favoritesdatabase.toDbModel
import com.programmersbox.favoritesdatabase.toItemModel
import com.programmersbox.sharedutils.MainLogo
import com.programmersbox.uiviews.utils.BackButton
import com.programmersbox.uiviews.utils.Cached
import com.programmersbox.uiviews.utils.ComposableUtils
import com.programmersbox.uiviews.utils.InsetSmallTopAppBar
import com.programmersbox.uiviews.utils.LoadingDialog
import com.programmersbox.uiviews.utils.LocalCustomListDao
import com.programmersbox.uiviews.utils.LocalGenericInfo
import com.programmersbox.uiviews.utils.LocalNavController
import com.programmersbox.uiviews.utils.OtakuScaffold
import com.programmersbox.uiviews.utils.dispatchIo
import com.programmersbox.uiviews.utils.navigateToDetails
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtakuCustomListScreen(
    logo: MainLogo,
    listDao: ListDao = LocalCustomListDao.current,
    vm: OtakuCustomListViewModel = viewModel { OtakuCustomListViewModel(listDao, createSavedStateHandle()) }
) {
    val genericInfo = LocalGenericInfo.current
    val navController = LocalNavController.current
    val context = LocalContext.current

    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val customItem = vm.customItem
    var showAdd by remember { mutableStateOf(false) }

    if (showAdd) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("Create New List") },
            text = {
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("List Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            customItem?.item?.copy(name = name)?.let { listDao.updateList(it) }
                            showAdd = false
                        }
                    },
                    enabled = name.isNotEmpty()
                ) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = { showAdd = false }) { Text("Cancel") }
            }
        )
    }

    OtakuScaffold(
        topBar = {
            InsetSmallTopAppBar(
                title = { Text(customItem?.item?.name.orEmpty()) },
                navigationIcon = { BackButton() },
                actions = {
                    IconButton(onClick = { showAdd = true }) {
                        Icon(Icons.Default.Edit, null)
                    }
                    Text("(${customItem?.list.orEmpty().size})")
                },
                scrollBehavior = scrollBehavior
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
    ) { padding ->
        var showLoadingDialog by remember { mutableStateOf(false) }

        LoadingDialog(
            showLoadingDialog = showLoadingDialog,
            onDismissRequest = { showLoadingDialog = false }
        )
        if (customItem != null) {
            LazyColumn(
                contentPadding = padding,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(customItem.list) { item ->
                    ElevatedCard(
                        onClick = {
                            genericInfo
                                .toSource(item.source)
                                ?.let { source ->
                                    Cached.cache[item.url]?.let {
                                        flow {
                                            emit(
                                                it
                                                    .toDbModel()
                                                    .toItemModel(source)
                                            )
                                        }
                                    } ?: source.getSourceByUrlFlow(item.url)
                                }
                                ?.dispatchIo()
                                ?.onStart { showLoadingDialog = true }
                                ?.onEach {
                                    showLoadingDialog = false
                                    navController.navigateToDetails(it)
                                }
                                ?.onCompletion { showLoadingDialog = false }
                                ?.launchIn(scope)
                        },
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Row {
                            val logoDrawable = remember { AppCompatResources.getDrawable(context, logo.logoId) }
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(item.imageUrl)
                                    .lifecycle(LocalLifecycleOwner.current)
                                    .crossfade(true)
                                    .build(),
                                placeholder = rememberDrawablePainter(logoDrawable),
                                error = rememberDrawablePainter(logoDrawable),
                                contentScale = ContentScale.Crop,
                                contentDescription = item.title,
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

                            Box(
                                modifier = Modifier
                                    .align(Alignment.Top)
                                    .padding(horizontal = 2.dp)
                            ) {

                            }
                        }
                    }
                }
            }
        }
    }
}