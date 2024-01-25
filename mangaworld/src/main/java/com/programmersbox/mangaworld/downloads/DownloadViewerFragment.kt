package com.programmersbox.mangaworld.downloads

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.ripple
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.programmersbox.mangaworld.ChaptersGet
import com.programmersbox.mangaworld.DOWNLOAD_FILE_PATH
import com.programmersbox.mangaworld.MangaSettingsHandling
import com.programmersbox.mangaworld.R
import com.programmersbox.mangaworld.reader.ReadActivity
import com.programmersbox.mangaworld.reader.ReadViewModel
import com.programmersbox.uiviews.utils.BackButton
import com.programmersbox.uiviews.utils.InsetSmallTopAppBar
import com.programmersbox.uiviews.utils.LocalNavController
import com.programmersbox.uiviews.utils.Screen
import com.programmersbox.uiviews.utils.components.OtakuScaffold
import com.programmersbox.uiviews.utils.components.PermissionRequest
import com.programmersbox.uiviews.utils.components.animatedItems
import com.programmersbox.uiviews.utils.components.updateAnimatedItemsState
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import java.io.File
import androidx.compose.material3.MaterialTheme as M3MaterialTheme

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalPermissionsApi::class,
    ExperimentalFoundationApi::class,
    ExperimentalAnimationApi::class,
)
@Composable
fun DownloadScreen() {

    val defaultPathname = remember { File(DOWNLOAD_FILE_PATH) }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    OtakuScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            InsetSmallTopAppBar(
                scrollBehavior = scrollBehavior,
                title = { Text(stringResource(R.string.downloaded_chapters)) },
                navigationIcon = { BackButton() }
            )
        }
    ) { p1 ->
        PermissionRequest(
            if (Build.VERSION.SDK_INT >= 33)
                listOf(Manifest.permission.READ_MEDIA_VIDEO)
            else listOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
            )
        ) {
            val context = LocalContext.current
            val viewModel: DownloadViewModel = viewModel { DownloadViewModel(context, defaultPathname) }
            DownloadViewer(viewModel, p1)
        }
    }
}

@ExperimentalMaterial3Api
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@Composable
private fun DownloadViewer(
    viewModel: DownloadViewModel,
    p1: PaddingValues,
    mangaSettingsHandling: MangaSettingsHandling = koinInject(),
) {
    val useNewReader by mangaSettingsHandling.useNewReader
        .flow
        .collectAsStateWithLifecycle(initialValue = true)

    val fileList = viewModel.fileList

    val f by updateAnimatedItemsState(newList = fileList.entries.toList())

    if (fileList.isEmpty()) EmptyState(p1)
    else LazyColumn(
        contentPadding = p1,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(4.dp)
    ) {
        animatedItems(
            f,
            enterTransition = fadeIn(),
            exitTransition = fadeOut()
        ) { file -> ChapterItem(file, useNewReader) }
    }
}

@Composable
private fun EmptyState(p1: PaddingValues) {
    val navController = LocalNavController.current
    Box(
        modifier = Modifier
            .padding(p1)
            .fillMaxSize()
    ) {
        Surface(
            shape = M3MaterialTheme.shapes.medium,
            tonalElevation = 4.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
        ) {
            Column(modifier = Modifier) {

                Text(
                    text = stringResource(id = R.string.get_started),
                    style = M3MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Text(
                    text = stringResource(id = R.string.download_a_manga),
                    style = M3MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Button(
                    onClick = { navController.popBackStack(Screen.RecentScreen.route, false) },
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 4.dp)
                ) { Text(text = stringResource(id = R.string.go_download)) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChapterItem(
    file: Map.Entry<String, Map<String, List<ChaptersGet.Chapters>>>,
    useNewReader: Boolean = true,
) {
    val context = LocalContext.current

    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Surface(
            shape = M3MaterialTheme.shapes.medium,
            tonalElevation = 4.dp,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    indication = ripple(),
                    interactionSource = null
                ) { expanded = !expanded }
        ) {
            ListItem(
                modifier = Modifier.padding(4.dp),
                headlineContent = { Text(file.value.values.randomOrNull()?.randomOrNull()?.folderName.orEmpty()) },
                supportingContent = { Text(stringResource(R.string.chapter_count, file.value.size)) },
                trailingContent = {
                    Icon(
                        Icons.Default.ArrowDropDown,
                        null,
                        modifier = Modifier.rotate(animateFloatAsState(if (expanded) 180f else 0f, label = "").value)
                    )
                }
            )
        }

        if (expanded) {
            file.value.values.toList().fastForEach { chapter ->
                val c = chapter.randomOrNull()

                var showPopup by remember { mutableStateOf(false) }

                if (showPopup) {
                    val onDismiss = { showPopup = false }

                    val scope = rememberCoroutineScope()

                    AlertDialog(
                        onDismissRequest = onDismiss,
                        title = { Text(stringResource(R.string.delete_title, c?.chapterName.orEmpty())) },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    scope.launch {
                                        flow {
                                            try {
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                                    chapter.fastForEach { f ->
                                                        context.contentResolver.delete(
                                                            f.assetFileStringUri.toUri(),
                                                            "${MediaStore.Images.Media._ID} = ?",
                                                            arrayOf(f.id)
                                                        )
                                                    }
                                                } else {
                                                    File(c?.chapterFolder!!).delete()
                                                }
                                                emit(true)
                                            } catch (e: Exception) {
                                                emit(false)
                                            }
                                        }
                                            .onEach { Toast.makeText(context, R.string.finished_deleting, Toast.LENGTH_SHORT).show() }
                                            .collect()
                                    }
                                    onDismiss()
                                }
                            ) { Text(stringResource(R.string.yes)) }
                        },
                        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.no)) } }
                    )
                }

                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = {
                        if (it == SwipeToDismissBoxValue.EndToStart) {
                            //delete
                            showPopup = true
                        }
                        false
                    }
                )

                SwipeToDismissBox(
                    modifier = Modifier.padding(start = 32.dp),
                    state = dismissState,
                    enableDismissFromStartToEnd = false,
                    backgroundContent = {
                        val color by animateColorAsState(
                            when (dismissState.targetValue) {
                                SwipeToDismissBoxValue.EndToStart -> Color.Red
                                else -> Color.Transparent
                            }, label = ""
                        )

                        val scale by animateFloatAsState(if (dismissState.targetValue == SwipeToDismissBoxValue.Settled) 0.75f else 1f, label = "")

                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(color)
                                .padding(horizontal = 20.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.scale(scale)
                            )
                        }
                    },
                    content = {
                        val navController = LocalNavController.current
                        Surface(
                            shape = M3MaterialTheme.shapes.medium,
                            tonalElevation = 4.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    indication = ripple(),
                                    interactionSource = null
                                ) {
                                    if (useNewReader) {
                                        ReadViewModel.navigateToMangaReader(
                                            navController,
                                            filePath = c?.chapterFolder,
                                            downloaded = true
                                        )
                                    } else {
                                        context.startActivity(
                                            Intent(context, ReadActivity::class.java).apply {
                                                putExtra("downloaded", true)
                                                putExtra("filePath", c?.chapterFolder?.let { f -> File(f) })
                                            }
                                        )
                                    }
                                }
                        ) {
                            ListItem(
                                modifier = Modifier.padding(4.dp),
                                headlineContent = { Text(c?.chapterName.orEmpty()) },
                                supportingContent = { Text(stringResource(R.string.page_count, chapter.size)) },
                                trailingContent = { Icon(Icons.Default.ChevronRight, null) }
                            )
                        }
                    }
                )
            }
        }
    }
}