package com.programmersbox.mangaworld.downloads

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.programmersbox.mangaworld.ChaptersGet
import com.programmersbox.mangaworld.DOWNLOAD_FILE_PATH
import com.programmersbox.mangaworld.R
import com.programmersbox.mangaworld.reader.ReadActivity
import com.programmersbox.mangaworld.reader.ReadViewModel
import com.programmersbox.mangaworld.useNewReaderFlow
import com.programmersbox.uiviews.BaseMainActivity
import com.programmersbox.uiviews.utils.LocalActivity
import com.programmersbox.uiviews.utils.LocalNavController
import com.programmersbox.uiviews.utils.components.PermissionRequest
import com.programmersbox.uiviews.utils.components.animatedItems
import com.programmersbox.uiviews.utils.components.updateAnimatedItemsState
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.io.File
import androidx.compose.material3.MaterialTheme as M3MaterialTheme

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalPermissionsApi::class,
    ExperimentalFoundationApi::class,
    ExperimentalAnimationApi::class,
    ExperimentalMaterialApi::class
)
@Composable
fun DownloadScreen() {

    val navController = LocalNavController.current

    val defaultPathname = remember { File(DOWNLOAD_FILE_PATH) }

    val topAppBarScrollState = rememberTopAppBarScrollState()
    val scrollBehavior = remember { TopAppBarDefaults.pinnedScrollBehavior(topAppBarScrollState) }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            SmallTopAppBar(
                scrollBehavior = scrollBehavior,
                title = { Text(stringResource(R.string.downloaded_chapters)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, null) }
                }
            )
        }
    ) { p1 ->
        PermissionRequest(listOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)) {
            val context = LocalContext.current
            /*val viewModel: DownloadViewModel = viewModel(
                factory = factoryCreate { DownloadViewModel(context, File(runBlocking { context.folderLocationFlow.first() })) }
            )*/
            val viewModel: DownloadViewModel = viewModel {
                DownloadViewModel(
                    context,
                    defaultPathname
                )
            }
            DownloadViewer(viewModel, p1)
        }
    }
}

@ExperimentalMaterial3Api
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@ExperimentalMaterialApi
@Composable
private fun DownloadViewer(viewModel: DownloadViewModel, p1: PaddingValues) {
    val fileList = viewModel.fileList

    val f by updateAnimatedItemsState(newList = fileList.entries.toList())

    if (fileList.isEmpty()) EmptyState(p1)
    else LazyColumn(
        contentPadding = p1,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(horizontal = 5.dp, vertical = 4.dp)
    ) {
        animatedItems(
            f,
            enterTransition = fadeIn(),
            exitTransition = fadeOut()
        ) { file -> ChapterItem(file, viewModel) }
    }
}

@Composable
private fun EmptyState(p1: PaddingValues) {
    val navController = LocalNavController.current
    val activity = LocalActivity.current
    Box(
        modifier = Modifier
            .padding(p1)
            .fillMaxSize()
    ) {
        androidx.compose.material3.Surface(
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 4.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(5.dp)
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
                    onClick = {
                        navController.popBackStack()
                        (activity as? BaseMainActivity)?.goToScreen(BaseMainActivity.Screen.RECENT)
                    },
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 5.dp)
                ) { Text(text = stringResource(id = R.string.go_download)) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@ExperimentalMaterialApi
@Composable
private fun ChapterItem(file: Map.Entry<String, Map<String, List<ChaptersGet.Chapters>>>, viewModel: DownloadViewModel) {
    val context = LocalContext.current

    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        androidx.compose.material3.Surface(
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 4.dp,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    indication = rememberRipple(),
                    interactionSource = remember { MutableInteractionSource() }
                ) { expanded = !expanded }
        ) {
            ListItem(
                modifier = Modifier.padding(5.dp),
                headlineText = { Text(file.value.values.randomOrNull()?.randomOrNull()?.folderName.orEmpty()) },
                supportingText = { Text(stringResource(R.string.chapter_count, file.value.size)) },
                trailingContent = {
                    Icon(
                        Icons.Default.ArrowDropDown,
                        null,
                        modifier = Modifier.rotate(animateFloatAsState(if (expanded) 180f else 0f).value)
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

                    AlertDialog(
                        onDismissRequest = onDismiss,
                        title = { Text(stringResource(R.string.delete_title, c?.chapterName.orEmpty())) },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    Single.create<Boolean> {
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
                                            it.onSuccess(true)
                                        } catch (e: Exception) {
                                            it.onSuccess(false)
                                        }
                                    }
                                        .subscribeOn(Schedulers.io())
                                        .observeOn(AndroidSchedulers.mainThread())
                                        .subscribeBy { Toast.makeText(context, R.string.finished_deleting, Toast.LENGTH_SHORT).show() }
                                        .addTo(viewModel.disposable)
                                    onDismiss()
                                }
                            ) { Text(stringResource(R.string.yes)) }
                        },
                        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.no)) } }
                    )
                }

                val dismissState = rememberDismissState(
                    confirmStateChange = {
                        if (it == DismissValue.DismissedToStart) {
                            //delete
                            showPopup = true
                        }
                        false
                    }
                )

                SwipeToDismiss(
                    modifier = Modifier.padding(start = 32.dp),
                    state = dismissState,
                    directions = setOf(DismissDirection.EndToStart),
                    background = {
                        val color by animateColorAsState(
                            when (dismissState.targetValue) {
                                DismissValue.Default -> Color.Transparent
                                DismissValue.DismissedToEnd -> Color.Transparent
                                DismissValue.DismissedToStart -> Color.Red
                            }
                        )

                        val scale by animateFloatAsState(if (dismissState.targetValue == DismissValue.Default) 0.75f else 1f)

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
                    }
                ) {
                    val navController = LocalNavController.current
                    androidx.compose.material3.Surface(
                        shape = MaterialTheme.shapes.medium,
                        tonalElevation = 4.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                indication = rememberRipple(),
                                interactionSource = remember { MutableInteractionSource() }
                            ) {
                                if (runBlocking { context.useNewReaderFlow.first() }) {
                                    ReadViewModel.navigateToMangaReader(
                                        navController,
                                        filePath = c?.chapterFolder,
                                        downloaded = true
                                    )
                                    /*findNavController()
                                        .navigate(
                                            ReadActivityComposeFragment::class.java.hashCode(),
                                            Bundle().apply {
                                                putBoolean("downloaded", true)
                                                putSerializable("filePath", c?.chapterFolder?.let { f -> File(f) })
                                            },
                                            SettingsDsl.customAnimationOptions
                                        )*/
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
                            modifier = Modifier.padding(5.dp),
                            headlineText = { Text(c?.chapterName.orEmpty()) },
                            supportingText = { Text(stringResource(R.string.page_count, chapter.size)) },
                            trailingContent = { Icon(Icons.Default.ChevronRight, null) }
                        )
                    }
                }
            }
        }
    }
}