package com.programmersbox.animeworld

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SmallTopAppBar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastMap
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.core.net.toUri
import androidx.navigation.fragment.findNavController
import com.programmersbox.helpfulutils.notificationManager
import com.programmersbox.uiviews.BaseMainActivity
import com.programmersbox.uiviews.utils.BaseBottomSheetDialogFragment
import com.programmersbox.uiviews.utils.BottomSheetDeleteScaffold
import com.programmersbox.uiviews.utils.currentColorScheme
import com.programmersbox.uiviews.utils.currentScreen
import com.tonyodev.fetch2.*
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import androidx.compose.material3.MaterialTheme as M3MaterialTheme

class DownloadViewerFragment : BaseBottomSheetDialogFragment(), ActionListener {

    companion object {
        private const val UNKNOWN_REMAINING_TIME: Long = -1
        private const val UNKNOWN_DOWNLOADED_BYTES_PER_SECOND: Long = 0
    }

    private val fetch: Fetch = Fetch.getDefaultInstance()
    private val downloadState = mutableStateListOf<DownloadData>()

    @OptIn(
        ExperimentalMaterial3Api::class,
        ExperimentalMaterialApi::class,
        ExperimentalAnimationApi::class,
    )
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner))
        setContent { M3MaterialTheme(currentColorScheme) { ScaffoldUi() } }
    }

    override fun onResume() {
        super.onResume()
        fetch.getDownloads { downloads ->
            downloadState.clear()
            val list = ArrayList(downloads)
            list.sortWith { first, second -> first.created.compareTo(second.created) }
            downloadState.addAll(list.fastMap { DownloadData(it) })
        }.addListener(fetchListener)
    }

    override fun onPause() {
        super.onPause()
        fetch.removeListener(fetchListener)
    }

    private fun updateUI(
        download: Download,
        etaTime: Long = UNKNOWN_REMAINING_TIME,
        downloadPs: Long = UNKNOWN_DOWNLOADED_BYTES_PER_SECOND,
        addOrRemove: Boolean = true
    ) {
        val d = downloadState.withIndex().find { it.value.id == download.id }

        if (addOrRemove) {
            val item = DownloadData(download).apply {
                eta = etaTime
                downloadedBytesPerSecond = downloadPs
            }

            if (d == null) {
                downloadState.add(item)
            } else {
                downloadState.remove(d.value)
                downloadState.add(d.index, item)
            }
        } else {
            if (download.status == Status.REMOVED || download.status == Status.DELETED) {
                downloadState.removeAt(d!!.index)
            }
        }
    }

    private val fetchListener = object : AbstractFetchListener() {
        override fun onQueued(download: Download, waitingOnNetwork: Boolean) {
            updateUI(download)
        }

        override fun onCompleted(download: Download) {
            updateUI(download)
            context?.notificationManager?.cancel(download.id)
        }

        override fun onError(download: Download, error: Error, throwable: Throwable?) {
            super.onError(download, error, throwable)
            updateUI(download)
        }

        override fun onProgress(download: Download, etaInMilliSeconds: Long, downloadedBytesPerSecond: Long) {
            updateUI(download, etaInMilliSeconds, downloadedBytesPerSecond)
        }

        override fun onPaused(download: Download) {
            updateUI(download)
        }

        override fun onResumed(download: Download) {
            updateUI(download)
        }

        override fun onCancelled(download: Download) {
            updateUI(download)
            context?.notificationManager?.cancel(download.id)
        }

        override fun onRemoved(download: Download) {
            downloadState.removeAll { it.id == download.id }
        }

        override fun onDeleted(download: Download) {
            downloadState.removeAll { it.id == download.id }
            context?.notificationManager?.cancel(download.id)
        }
    }

    override fun onPauseDownload(id: Int) {
        fetch.pause(id)
    }

    override fun onResumeDownload(id: Int) {
        fetch.resume(id)
    }

    override fun onRemoveDownload(id: Int) {
        fetch.remove(id)
    }

    override fun onRetryDownload(id: Int) {
        fetch.retry(id)
    }

    @ExperimentalMaterial3Api
    @ExperimentalAnimationApi
    @ExperimentalMaterialApi
    @Composable
    private fun ScaffoldUi() {

        val state = rememberBottomSheetScaffoldState()
        val scope = rememberCoroutineScope()

        LaunchedEffect(state.bottomSheetState.isExpanded) {
            if (state.bottomSheetState.isExpanded) {
                fetch.pauseAll()
            } else {
                fetch.resumeAll()
            }
        }

        BackHandler(state.bottomSheetState.isExpanded && currentScreen.value == R.id.setting_nav) {
            scope.launch { state.bottomSheetState.collapse() }
        }

        var itemToDelete by remember { mutableStateOf<DownloadData?>(null) }
        val showDialog = remember { mutableStateOf(false) }

        itemToDelete?.download?.let { SlideToDeleteDialog(showDialog = showDialog, download = it) }

        val scrollBehavior = remember { TopAppBarDefaults.pinnedScrollBehavior() }

        BottomSheetDeleteScaffold(
            bottomScrollBehavior = scrollBehavior,
            state = state,
            listOfItems = downloadState,
            multipleTitle = stringResource(id = R.string.delete),
            onRemove = { download ->
                itemToDelete = download
                showDialog.value = true
            },
            customSingleRemoveDialog = { download ->
                itemToDelete = download
                showDialog.value = true
                false
            },
            onMultipleRemove = { downloadItems -> fetch.delete(downloadItems.fastMap { it.id }) },
            topBar = {
                SmallTopAppBar(
                    scrollBehavior = scrollBehavior,
                    navigationIcon = { IconButton(onClick = { findNavController().popBackStack() }) { Icon(Icons.Default.ArrowBack, null) } },
                    actions = { IconButton(onClick = { scope.launch { state.bottomSheetState.expand() } }) { Icon(Icons.Default.Delete, null) } },
                    title = {
                        Text(
                            stringResource(id = R.string.in_progress_downloads),
                            style = M3MaterialTheme.typography.titleLarge
                        )
                    }
                )
            },
            itemUi = { download ->
                ListItem(
                    modifier = Modifier.padding(5.dp),
                    text = { Text(download.download.url.toUri().lastPathSegment.orEmpty()) },
                    overlineText = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val progress = download.download.progress.coerceAtLeast(0)
                            Text(stringResource(R.string.percent_progress, progress))
                            androidx.compose.material3.LinearProgressIndicator(
                                progress = animateFloatAsState(targetValue = progress.toFloat() / 100f).value,
                                modifier = Modifier
                                    .padding(horizontal = 8.dp)
                                    .fillMaxWidth()
                            )
                        }
                    }
                )
            }
        ) { p, items ->
            if (items.isEmpty()) {
                EmptyState()
            } else {
                LazyColumn(
                    modifier = Modifier.padding(top = 5.dp),
                    contentPadding = p,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) { items(items) { d -> DownloadItem(d, this@DownloadViewerFragment) } }
            }
        }
    }

    @Composable
    private fun EmptyState() {
        Box(modifier = Modifier.fillMaxSize()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(5.dp),
                tonalElevation = 5.dp,
                shape = RoundedCornerShape(5.dp)
            ) {
                Column(modifier = Modifier) {
                    Text(
                        text = stringResource(id = R.string.get_started),
                        style = M3MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )

                    Text(
                        text = stringResource(id = R.string.download_a_video),
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

    @ExperimentalAnimationApi
    @ExperimentalMaterialApi
    @Composable
    private fun DownloadItem(download: DownloadData, actionListener: ActionListener) {
        val showDialog = remember { mutableStateOf(false) }

        SlideToDeleteDialog(showDialog = showDialog, download = download.download)

        val dismissState = rememberDismissState(
            confirmStateChange = {
                if (it == DismissValue.DismissedToEnd || it == DismissValue.DismissedToStart) {
                    showDialog.value = true
                }
                false
            }
        )

        SwipeToDismiss(
            state = dismissState,
            background = {
                val direction = dismissState.dismissDirection ?: return@SwipeToDismiss
                val color by animateColorAsState(
                    when (dismissState.targetValue) {
                        DismissValue.Default -> Color.Transparent
                        DismissValue.DismissedToEnd -> Color.Red
                        DismissValue.DismissedToStart -> Color.Red
                    }
                )
                val alignment = when (direction) {
                    DismissDirection.StartToEnd -> Alignment.CenterStart
                    DismissDirection.EndToStart -> Alignment.CenterEnd
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
                        Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.scale(scale),
                        tint = M3MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 5.dp),
                tonalElevation = 5.dp,
                shape = MaterialTheme.shapes.medium
            ) {

                ConstraintLayout {

                    val (
                        title, progress,
                        action, progressText,
                        speed, remaining,
                        status
                    ) = createRefs()

                    Text(
                        download.download.url.toUri().lastPathSegment.orEmpty(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        textAlign = TextAlign.Start,
                        modifier = Modifier
                            .constrainAs(title) {
                                start.linkTo(parent.start)
                                top.linkTo(parent.top)
                            }
                            .padding(horizontal = 8.dp)
                            .padding(top = 8.dp)
                    )

                    val prog = download.download.progress.coerceAtLeast(0)

                    androidx.compose.material3.LinearProgressIndicator(
                        progress = animateFloatAsState(targetValue = prog.toFloat() / 100f).value,
                        modifier = Modifier
                            .constrainAs(progress) {
                                start.linkTo(parent.start)
                                bottom.linkTo(action.bottom)
                                end.linkTo(action.start)
                                top.linkTo(action.top)
                            }
                            .padding(8.dp)
                    )

                    OutlinedButton(
                        onClick = {
                            when (download.download.status) {
                                Status.FAILED -> actionListener.onRetryDownload(download.download.id)
                                Status.PAUSED -> actionListener.onResumeDownload(download.download.id)
                                Status.DOWNLOADING, Status.QUEUED -> actionListener.onPauseDownload(download.download.id)
                                Status.ADDED -> actionListener.onResumeDownload(download.download.id)
                                else -> {
                                }
                            }
                        },
                        modifier = Modifier
                            .constrainAs(action) {
                                end.linkTo(parent.end)
                                top.linkTo(title.bottom)
                            }
                            .padding(top = 8.dp, end = 8.dp)
                    ) {
                        Text(
                            stringResource(
                                id = when (download.download.status) {
                                    Status.COMPLETED -> R.string.view
                                    Status.FAILED -> R.string.retry
                                    Status.PAUSED -> R.string.resume
                                    Status.DOWNLOADING, Status.QUEUED -> R.string.pause
                                    Status.ADDED -> R.string.download
                                    else -> R.string.error_text
                                }
                            )
                        )
                    }

                    Text(
                        stringResource(R.string.percent_progress, prog),
                        modifier = Modifier
                            .constrainAs(progressText) {
                                top.linkTo(progress.bottom)
                                start.linkTo(progress.start)
                            }
                            .padding(horizontal = 8.dp)
                    )

                    Text(
                        if (download.downloadedBytesPerSecond == 0L) "" else getDownloadSpeedString(download.downloadedBytesPerSecond),
                        modifier = Modifier
                            .constrainAs(speed) {
                                top.linkTo(progress.bottom)
                                end.linkTo(progress.end)
                            }
                            .padding(horizontal = 8.dp)
                            .padding(bottom = 8.dp)
                    )

                    Text(
                        if (download.eta == -1L) "" else getETAString(download.eta, true),
                        modifier = Modifier
                            .constrainAs(remaining) {
                                bottom.linkTo(parent.bottom)
                                baseline.linkTo(parent.baseline)
                                top.linkTo(progressText.bottom)
                                start.linkTo(parent.start)
                            }
                            .padding(8.dp)
                    )

                    Text(
                        stringResource(id = getStatusString(download.download.status)),
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .constrainAs(status) {
                                bottom.linkTo(parent.bottom)
                                baseline.linkTo(parent.baseline)
                                top.linkTo(remaining.top)
                                start.linkTo(remaining.end)
                                end.linkTo(parent.end)
                            }
                            .padding(8.dp)
                    )

                }

            }
        }
    }

    private fun getStatusString(status: Status): Int = when (status) {
        Status.COMPLETED -> R.string.done
        Status.DOWNLOADING -> R.string.downloading_no_dots
        Status.FAILED -> R.string.error_text
        Status.PAUSED -> R.string.paused_text
        Status.QUEUED -> R.string.waiting_in_queue
        Status.REMOVED -> R.string.removed_text
        Status.NONE -> R.string.not_queued
        else -> R.string.unknown
    }

    private fun getDownloadSpeedString(downloadedBytesPerSecond: Long): String {
        if (downloadedBytesPerSecond < 0) {
            return ""
        }
        val kb = downloadedBytesPerSecond.toDouble() / 1000.toDouble()
        val mb = kb / 1000.toDouble()
        val gb = mb / 1000
        val tb = gb / 1000
        val decimalFormat = DecimalFormat(".##")
        return when {
            tb >= 1 -> "${decimalFormat.format(tb)} tb/s"
            gb >= 1 -> "${decimalFormat.format(gb)} gb/s"
            mb >= 1 -> "${decimalFormat.format(mb)} mb/s"
            kb >= 1 -> "${decimalFormat.format(kb)} kb/s"
            else -> "$downloadedBytesPerSecond b/s"
        }
    }

    private fun getETAString(etaInMilliSeconds: Long, needLeft: Boolean = true): String {
        if (etaInMilliSeconds < 0) {
            return ""
        }
        var seconds = (etaInMilliSeconds / 1000).toInt()
        val hours = (seconds / 3600).toLong()
        seconds -= (hours * 3600).toInt()
        val minutes = (seconds / 60).toLong()
        seconds -= (minutes * 60).toInt()
        return when {
            hours > 0 -> String.format("%02d:%02d:%02d hours", hours, minutes, seconds)
            minutes > 0 -> String.format("%02d:%02d mins", minutes, seconds)
            else -> "$seconds secs"
        } + (if (needLeft) " left" else "")
    }

}

interface ActionListener {
    fun onPauseDownload(id: Int)
    fun onResumeDownload(id: Int)
    fun onRemoveDownload(id: Int)
    fun onRetryDownload(id: Int)
}

class DownloadData(val download: Download, val id: Int = download.id) {
    var eta: Long = -1
    var downloadedBytesPerSecond: Long = 0
    override fun hashCode(): Int = id
    override fun toString(): String = download.toString()
    override fun equals(other: Any?): Boolean = other === this || other is DownloadData && other.id == id
}