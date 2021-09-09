package com.programmersbox.mangaworld

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.*
import androidx.compose.runtime.rxjava2.subscribeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.anggrayudi.storage.file.DocumentFileCompat
import com.anggrayudi.storage.file.DocumentFileType
import com.anggrayudi.storage.file.deleteRecursively
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.android.material.composethemeadapter.MdcTheme
import com.programmersbox.dragswipe.*
import com.programmersbox.mangaworld.databinding.FragmentDownloadViewerBinding
import com.programmersbox.uiviews.BaseMainActivity
import com.programmersbox.uiviews.utils.*
import de.helmbold.rxfilewatcher.PathObservables
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.launch
import java.io.File
import kotlin.io.path.Path


class DownloadViewerFragment(private val pathname: File? = null) : BaseBottomSheetDialogFragment() {

    private lateinit var binding: FragmentDownloadViewerBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentDownloadViewerBinding.inflate(inflater, container, false)
        return binding.root
    }

    private val disposable = CompositeDisposable()

    private val defaultPathname get() = File(DOWNLOAD_FILE_PATH)

    @ExperimentalPermissionsApi
    @ExperimentalAnimationApi
    @ExperimentalMaterialApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadInformation()
    }

    @ExperimentalPermissionsApi
    @ExperimentalAnimationApi
    @ExperimentalMaterialApi
    private fun loadInformation() {
        binding.composeDownloadView.setContent {
            MdcTheme {
                PermissionRequest(listOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)) { DownloadViewer() }
            }
        }
    }

    @ExperimentalAnimationApi
    @ExperimentalMaterialApi
    @Composable
    fun DownloadViewer() {
        val context = LocalContext.current
        val files by PathObservables
            .watchNonRecursive(Path((pathname ?: defaultPathname).path))
            .concatMapSingle {
                Single.create<List<File>> {
                    (pathname ?: defaultPathname)
                        .listFiles()
                        .also { f -> println(f?.joinToString("\n") { n -> n.name }) }
                        .orEmpty()
                        .toList()
                        .let(it::onSuccess)
                }
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.io())
            }
            .startWith(
                Single.create<List<File>> {
                    (pathname ?: defaultPathname)
                        .listFiles()
                        .also { f -> println(f?.joinToString("\n") { n -> n.name }) }
                        .orEmpty()
                        .toList()
                        .let(it::onSuccess)
                }
                    .toObservable()
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.io())
            )
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeAsState(emptyList())

        val state = rememberBottomSheetScaffoldState()
        val scope = rememberCoroutineScope()

        BottomSheetDeleteScaffold(
            listOfItems = files,
            state = state,
            multipleTitle = stringResource(id = R.string.delete),
            onRemove = { file ->
                Single.create<Boolean> {
                    it.onSuccess(
                        DocumentFileCompat.fromFullPath(context, file.path, DocumentFileType.FOLDER)
                            ?.deleteRecursively(context, false) ?: false
                    )
                }
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeBy { Toast.makeText(context, R.string.finished_deleting, Toast.LENGTH_SHORT).show() }
                    .addTo(disposable)
            },
            onMultipleRemove = { list ->
                Completable.create {
                    list.forEach { f ->
                        DocumentFileCompat.fromFullPath(context, f.path, DocumentFileType.FOLDER)
                            ?.deleteRecursively(context, false)
                    }
                    it.onComplete()
                }
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeBy {
                        Toast.makeText(requireContext(), R.string.finished_deleting, Toast.LENGTH_SHORT).show()
                        list.clear()
                    }
                    .addTo(disposable)
            },
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.downloaded_chapters)) },
                    navigationIcon = if (pathname != defaultPathname && pathname?.parentFile != null) {
                        { IconButton(onClick = { dismiss() }) { Icon(Icons.Default.ArrowBack, null) } }
                    } else null,
                    actions = {
                        IconButton(
                            onClick = {
                                parentFragmentManager.fragments
                                    .filterIsInstance<DownloadViewerFragment>()
                                    .forEach(DownloadViewerFragment::dismiss)
                            }
                        ) { Icon(Icons.Default.Close, null) }

                        IconButton(onClick = { scope.launch { state.bottomSheetState.expand() } }) { Icon(Icons.Default.Delete, null) }
                    }
                )
            },
            itemUi = { file ->
                ListItem(modifier = Modifier.padding(5.dp)) {
                    Text(
                        file.name,
                        style = MaterialTheme.typography.h5,
                        modifier = Modifier.padding(5.dp)
                    )
                }
            }
        ) {
            Scaffold(modifier = Modifier.padding(it)) { p ->
                val f by updateAnimatedItemsState(newList = files)

                if (files.isEmpty()) EmptyState()
                else LazyColumn(contentPadding = p) {
                    animatedItems(
                        f,
                        enterTransition = slideInHorizontally(),
                        exitTransition = slideOutHorizontally()
                    ) { file -> ChapterItem(file) }
                }
            }
        }
    }

    @Composable
    private fun EmptyState() {

        Box(modifier = Modifier.fillMaxSize()) {

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(5.dp),
                elevation = 5.dp,
                shape = RoundedCornerShape(5.dp)
            ) {

                Column(modifier = Modifier) {

                    Text(
                        text = stringResource(id = R.string.get_started),
                        style = MaterialTheme.typography.h4,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )

                    Text(
                        text = stringResource(id = R.string.download_a_manga),
                        style = MaterialTheme.typography.body1,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )

                    Button(
                        onClick = {
                            dismiss()
                            (activity as? BaseMainActivity)?.goToScreen(BaseMainActivity.Screen.RECENT)
                        },
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(bottom = 5.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.go_download),
                            style = MaterialTheme.typography.button
                        )
                    }

                }

            }
        }

    }

    @ExperimentalMaterialApi
    @Composable
    private fun ChapterItem(file: File) {
        val context = LocalContext.current

        var showPopup by remember { mutableStateOf(false) }

        if (showPopup) {
            val onDismiss = { showPopup = false }

            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(stringResource(R.string.delete_title, file.name)) },
                confirmButton = {
                    Button(
                        onClick = {
                            Single.create<Boolean> {
                                it.onSuccess(
                                    DocumentFileCompat.fromFullPath(context, file.path, DocumentFileType.FOLDER)
                                        ?.deleteRecursively(context, false) ?: false
                                )
                            }
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribeBy { Toast.makeText(context, R.string.finished_deleting, Toast.LENGTH_SHORT).show() }
                                .addTo(disposable)
                            onDismiss()
                        }
                    ) { Text(stringResource(R.string.yes), style = MaterialTheme.typography.button) }
                },
                dismissButton = { Button(onClick = onDismiss) { Text(stringResource(R.string.no), style = MaterialTheme.typography.button) } }
            )
        }

        val clickAction: () -> Unit = {
            if (file.listFiles()?.all(File::isFile) == true)
                activity?.startActivity(
                    Intent(context, ReadActivity::class.java).apply {
                        putExtra("downloaded", true)
                        putExtra("filePath", file)
                    }
                )
            else DownloadViewerFragment(file).show(parentFragmentManager, null)
        }

        val dismissState = rememberDismissState(
            confirmStateChange = {
                if (it == DismissValue.DismissedToEnd) {
                    //read
                    clickAction()
                } else if (it == DismissValue.DismissedToStart) {
                    //delete
                    showPopup = true
                }
                false
            }
        )

        SwipeToDismiss(
            state = dismissState,
            directions = setOf(DismissDirection.StartToEnd, DismissDirection.EndToStart),
            dismissThresholds = { FractionalThreshold(0.5f) },
            background = {
                val direction = dismissState.dismissDirection ?: return@SwipeToDismiss
                val color by animateColorAsState(
                    when (dismissState.targetValue) {
                        DismissValue.Default -> Color.Transparent
                        DismissValue.DismissedToEnd -> Color.Green
                        DismissValue.DismissedToStart -> Color.Red
                    }
                )
                val alignment = when (direction) {
                    DismissDirection.StartToEnd -> Alignment.CenterStart
                    DismissDirection.EndToStart -> Alignment.CenterEnd
                }
                val icon = when (direction) {
                    DismissDirection.StartToEnd -> Icons.Default.Book
                    DismissDirection.EndToStart -> Icons.Default.Delete
                }
                val scale by animateFloatAsState(if (dismissState.targetValue == DismissValue.Default) 0.75f else 1f)

                Box(
                    Modifier
                        .fillMaxSize()
                        .background(color)
                        .padding(horizontal = 20.dp),
                    contentAlignment = alignment
                ) {
                    Icon(
                        icon,
                        contentDescription = "Localized description",
                        modifier = Modifier.scale(scale)
                    )
                }
            }
        ) {
            Card(
                interactionSource = MutableInteractionSource(),
                indication = rememberRipple(),
                onClick = clickAction,
                modifier = Modifier
                    .padding(5.dp)
                    .fillMaxWidth()
            ) {
                ListItem(
                    modifier = Modifier.padding(5.dp),
                    text = { Text(file.name) },
                    secondaryText = {
                        Text(
                            stringResource(
                                if (file.listFiles()?.all(File::isFile) == true) R.string.page_count else R.string.chapter_count,
                                file.listFiles()?.size ?: 0
                            )
                        )
                    },
                    trailing = { IconButton(onClick = clickAction) { Icon(Icons.Default.ChevronRight, null) } }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        disposable.dispose()
    }

}