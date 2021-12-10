package com.programmersbox.mangaworld

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallTopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.programmersbox.uiviews.BaseMainActivity
import com.programmersbox.uiviews.utils.*
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import java.io.File
import androidx.compose.material3.MaterialTheme as M3MaterialTheme

class DownloadViewerFragment : BaseBottomSheetDialogFragment() {

    private val disposable = CompositeDisposable()

    private val defaultPathname get() = File(DOWNLOAD_FILE_PATH)

    @OptIn(
        ExperimentalMaterial3Api::class,
        ExperimentalMaterialApi::class,
        ExperimentalPermissionsApi::class,
        ExperimentalAnimationApi::class,
        ExperimentalFoundationApi::class
    )
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner))
        setContent {
            M3MaterialTheme(currentColorScheme) {
                PermissionRequest(listOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    LaunchedEffect(Unit) {
                        val c = ChaptersGet.getInstance(requireContext())
                        c?.loadChapters(lifecycleScope, defaultPathname.absolutePath)
                    }
                    DownloadViewer()
                }
            }
        }
    }

    @ExperimentalMaterial3Api
    @ExperimentalFoundationApi
    @ExperimentalAnimationApi
    @ExperimentalMaterialApi
    @Composable
    private fun DownloadViewer() {

        val c = remember { ChaptersGet.getInstance(requireContext()) }

        val fileList by c!!.chapters2
            .map { f ->
                f
                    .groupBy { it.folder }
                    .entries
                    .toList()
                    .fastMap { it.key to it.value.groupBy { c -> c.chapterFolder } }
                    .toMap()
            }
            .collectAsState(initial = emptyMap())

        val scrollBehavior = remember { TopAppBarDefaults.pinnedScrollBehavior() }

        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                SmallTopAppBar(
                    scrollBehavior = scrollBehavior,
                    title = { Text(stringResource(R.string.downloaded_chapters)) },
                    navigationIcon = {
                        IconButton(onClick = { findNavController().popBackStack() }) { Icon(Icons.Default.ArrowBack, null) }
                    }
                )
            }
        ) { p1 ->
            val f by updateAnimatedItemsState(newList = fileList.entries.toList())

            if (fileList.isEmpty()) EmptyState()
            else LazyColumn(
                contentPadding = p1,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 4.dp)
            ) {
                animatedItems(
                    f,
                    enterTransition = fadeIn(),
                    exitTransition = fadeOut()
                ) { file -> ChapterItem(file) }
            }
        }
    }

    @Composable
    private fun EmptyState() {
        Box(modifier = Modifier.fillMaxSize()) {
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
                            findNavController().popBackStack()
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

    @ExperimentalMaterialApi
    @Composable
    private fun ChapterItem(file: Map.Entry<String, Map<String, List<ChaptersGet.Chapters>>>) {
        val context = LocalContext.current

        var expanded by remember { mutableStateOf(false) }

        Column(
            modifier = Modifier.animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            androidx.compose.material3.Surface(
                indication = rememberRipple(),
                shape = MaterialTheme.shapes.medium,
                tonalElevation = 4.dp,
                onClick = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                ListItem(
                    modifier = Modifier.padding(5.dp),
                    text = { Text(file.value.values.randomOrNull()?.randomOrNull()?.folderName.orEmpty()) },
                    secondaryText = { Text(stringResource(R.string.chapter_count, file.value.size)) },
                    trailing = {
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
                                            .addTo(disposable)
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
                        androidx.compose.material3.Surface(
                            shape = MaterialTheme.shapes.medium,
                            tonalElevation = 4.dp,
                            indication = rememberRipple(),
                            onClick = {
                                activity?.startActivity(
                                    Intent(
                                        context,
                                        if (runBlocking { context.useNewReaderFlow.first() }) ReadActivityCompose::class.java else ReadActivity::class.java
                                    ).apply {
                                        putExtra("downloaded", true)
                                        putExtra("filePath", c?.chapterFolder?.let { f -> File(f) })
                                    }
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ListItem(
                                modifier = Modifier.padding(5.dp),
                                text = { Text(c?.chapterName.orEmpty()) },
                                secondaryText = { Text(stringResource(R.string.page_count, chapter.size)) },
                                trailing = { Icon(Icons.Default.ChevronRight, null) }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        disposable.dispose()
        ChaptersGet.getInstance(requireContext())?.unregister()
    }

}