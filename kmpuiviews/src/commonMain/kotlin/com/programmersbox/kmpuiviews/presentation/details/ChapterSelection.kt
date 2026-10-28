package com.programmersbox.kmpuiviews.presentation.details

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkRemove
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RemoveDone
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.programmersbox.favoritesdatabase.ChapterWatched
import com.programmersbox.kmpmodels.KmpChapterModel
import com.programmersbox.kmpuiviews.ChapterDownloadUiState
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.clear_selection
import otakuworld.kmpuiviews.generated.resources.selected_count

enum class SelectionDownloadMode { Hidden, Download, Delete }

data class ChapterSelectionUiState(
    val count: Int,
    val showSingleActions: Boolean,
    val allRead: Boolean,
    val allBookmarked: Boolean,
    val downloadMode: SelectionDownloadMode,
    val chaptersToDownload: List<KmpChapterModel>,
    val selectAllEnabled: Boolean,
)

fun chapterSelectionUiState(
    selected: List<KmpChapterModel>,
    totalChapters: Int,
    readUrls: Set<String>,
    bookmarkedUrls: Set<String>,
    downloadStates: Map<String, ChapterDownloadUiState>,
    canDownload: Boolean,
    supportsBatchDownload: Boolean,
): ChapterSelectionUiState {
    val count = selected.size
    val toDownload = selected.filter { (downloadStates[it.url] ?: ChapterDownloadUiState.None) == ChapterDownloadUiState.None }
    val downloadMode = when {
        !canDownload || count == 0 -> SelectionDownloadMode.Hidden
        count >= 2 && !supportsBatchDownload -> SelectionDownloadMode.Hidden
        selected.all { downloadStates[it.url] == ChapterDownloadUiState.Downloaded } -> SelectionDownloadMode.Delete
        else -> SelectionDownloadMode.Download
    }
    return ChapterSelectionUiState(
        count = count,
        showSingleActions = count == 1,
        allRead = count > 0 && selected.all { it.url in readUrls },
        allBookmarked = count > 0 && selected.all { it.url in bookmarkedUrls },
        downloadMode = downloadMode,
        chaptersToDownload = toDownload,
        selectAllEnabled = count < totalChapters,
    )
}

data class ChapterSelectionActions(
    val selectedUrls: Set<String> = emptySet(),
    val onToggle: (String) -> Unit = {},
    val onSelectAll: () -> Unit = {},
    val onInvert: () -> Unit = {},
    val onClear: () -> Unit = {},
    val onMarkAs: (List<KmpChapterModel>, Boolean) -> Unit = { _, _ -> },
    val onRestoreRead: (List<KmpChapterModel>, Set<String>) -> Unit = { _, _ -> },
    val onSetBookmarked: (List<KmpChapterModel>, Boolean) -> Unit = { _, _ -> },
    val onRestoreBookmarks: (List<KmpChapterModel>, Set<String>) -> Unit = { _, _ -> },
    val onBatchDownload: (List<KmpChapterModel>) -> Unit = {},
    val supportsBatchDownload: Boolean = false,
)

// Space below the chapter list so the last row can scroll clear of the floating toolbar.
internal val SelectionToolbarListPadding = 88.dp

private fun chapterCount(n: Int) = if (n == 1) "1 chapter" else "$n chapters"

// Holds the last non-empty value so exit animations don't show an empty selection.
@Composable
private fun <T> rememberLastNonEmpty(value: T, isEmpty: Boolean): T {
    val holder = remember { arrayOfNulls<Any?>(1) }
    if (!isEmpty) holder[0] = value
    @Suppress("UNCHECKED_CAST")
    return (holder[0] ?: value) as T
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun ChapterSelectionTopBar(
    count: Int,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    val shownCount = rememberLastNonEmpty(count, count == 0)
    TopAppBar(
        scrollBehavior = scrollBehavior,
        title = {
            AnimatedContent(
                targetState = shownCount,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInVertically { it } + fadeIn()) togetherWith (slideOutVertically { -it } + fadeOut())
                    } else {
                        (slideInVertically { -it } + fadeIn()) togetherWith (slideOutVertically { it } + fadeOut())
                    }
                }
            ) { Text(stringResource(Res.string.selected_count, it)) }
        },
        navigationIcon = {
            IconButton(onClick = onClear) {
                Icon(Icons.Default.Close, stringResource(Res.string.clear_selection))
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        modifier = modifier
    )
}

@Composable
internal fun ChapterSelectionIndicator(
    isSelected: Boolean,
    modifier: Modifier = Modifier,
) {
    val container by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        label = "selectionContainer"
    )
    val border by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "selectionBorder"
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(24.dp)
            .border(2.dp, border, CircleShape)
            .background(container, CircleShape)
    ) {
        AnimatedVisibility(
            visible = isSelected,
            enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
            exit = scaleOut() + fadeOut()
        ) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
internal fun ChapterUnreadDot(
    transition: Transition<Boolean>,
    size: Dp,
) {
    val animatedSize by animateDpAsState(size, label = "unreadDotSize")
    transition.AnimatedVisibility(
        { !it },
        enter = fadeIn() + expandHorizontally(),
        exit = fadeOut() + shrinkHorizontally()
    ) {
        Box(
            modifier = Modifier
                .size(animatedSize)
                .background(MaterialTheme.colorScheme.primary, CircleShape)
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ChapterSelectionToolbarHost(
    visible: Boolean,
    chapters: List<KmpChapterModel>,
    read: List<ChapterWatched>,
    detailsActions: DetailsActions,
    downloadStates: Map<String, ChapterDownloadUiState>,
    canDownload: Boolean,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val selection = detailsActions.selection
    val selected = chapters.filter { it.url in selection.selectedUrls }
    val readUrls = remember(read) { read.map { it.url }.toSet() }
    val bookmarkedUrls = detailsActions.bookmarkedChapterUrls
    val liveState = chapterSelectionUiState(
        selected = selected,
        totalChapters = chapters.size,
        readUrls = readUrls,
        bookmarkedUrls = bookmarkedUrls,
        downloadStates = downloadStates,
        canDownload = canDownload,
        supportsBatchDownload = selection.supportsBatchDownload,
    )
    val state = rememberLastNonEmpty(liveState, selected.isEmpty())

    fun showUndo(message: String, undo: () -> Unit) {
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = "Undo",
                duration = SnackbarDuration.Long
            )
            if (result == SnackbarResult.ActionPerformed) undo()
        }
    }

    fun showMessage(message: String) {
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically { it * 2 } + fadeIn() + scaleIn(),
        exit = slideOutVertically { it * 2 } + fadeOut() + scaleOut(),
        modifier = modifier
            .offset(y = -FloatingToolbarDefaults.ScreenOffset)
            .padding(horizontal = 16.dp)
    ) {
        HorizontalFloatingToolbar(
            expanded = true,
            // Border keeps the toolbar distinct from the chapter rows under any cover-derived palette.
            modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
        ) {
            // Scrolls sideways when every action doesn't fit (e.g. single selection on narrow phones).
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                AnimatedVisibility(
                    visible = state.showSingleActions,
                    enter = expandHorizontally() + fadeIn(),
                    exit = shrinkHorizontally() + fadeOut()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Button(
                            onClick = {
                                val chapter = selected.singleOrNull() ?: return@Button
                                selection.onClear()
                                detailsActions.onClick(chapter)
                            }
                        ) {
                            Icon(Icons.Default.PlayArrow, null)
                            Spacer(Modifier.width(4.dp))
                            Text("Read")
                        }
                        IconButton(
                            onClick = {
                                val chapter = selected.singleOrNull() ?: return@IconButton
                                selection.onClear()
                                detailsActions.shareChapter(chapter)
                            }
                        ) { Icon(Icons.Default.Share, "Share") }
                        VerticalDivider(Modifier.height(24.dp).padding(horizontal = 4.dp))
                    }
                }

                IconButton(
                    onClick = {
                        if (selected.isEmpty()) return@IconButton
                        val targetRead = !state.allRead
                        val previouslyRead = selected.filter { it.url in readUrls }.map { it.url }.toSet()
                        val affected = selected
                        selection.onMarkAs(affected, targetRead)
                        selection.onClear()
                        showUndo("Marked ${chapterCount(affected.size)} ${if (targetRead) "read" else "unread"}") {
                            selection.onRestoreRead(affected, previouslyRead)
                        }
                    }
                ) {
                    AnimatedContent(state.allRead) { allRead ->
                        if (allRead) Icon(Icons.Default.RemoveDone, "Mark unread")
                        else Icon(Icons.Default.DoneAll, "Mark read")
                    }
                }

                IconButton(
                    onClick = {
                        if (selected.isEmpty()) return@IconButton
                        val targetBookmarked = !state.allBookmarked
                        val previouslyBookmarked = selected.filter { it.url in bookmarkedUrls }.map { it.url }.toSet()
                        val affected = selected
                        selection.onSetBookmarked(affected, targetBookmarked)
                        selection.onClear()
                        showUndo(
                            if (targetBookmarked) "Bookmarked ${chapterCount(affected.size)}"
                            else "Removed bookmark from ${chapterCount(affected.size)}"
                        ) { selection.onRestoreBookmarks(affected, previouslyBookmarked) }
                    }
                ) {
                    AnimatedContent(state.allBookmarked) { allBookmarked ->
                        if (allBookmarked) Icon(Icons.Default.BookmarkRemove, "Remove bookmark")
                        else Icon(Icons.Outlined.BookmarkBorder, "Bookmark")
                    }
                }

                AnimatedVisibility(
                    visible = state.downloadMode != SelectionDownloadMode.Hidden,
                    enter = expandHorizontally() + fadeIn(),
                    exit = shrinkHorizontally() + fadeOut()
                ) {
                    IconButton(
                        onClick = {
                            if (selected.isEmpty()) return@IconButton
                            when (state.downloadMode) {
                                SelectionDownloadMode.Delete -> {
                                    selected.forEach(detailsActions.onDeleteDownload)
                                    showMessage("Deleted ${chapterCount(selected.size)}")
                                }

                                SelectionDownloadMode.Download -> {
                                    val toDownload = state.chaptersToDownload
                                    if (toDownload.isNotEmpty()) {
                                        if (selected.size == 1) detailsActions.onDownload(toDownload.first())
                                        else selection.onBatchDownload(toDownload)
                                        showMessage("Downloading ${chapterCount(toDownload.size)}")
                                    }
                                }

                                SelectionDownloadMode.Hidden -> Unit
                            }
                            selection.onClear()
                        }
                    ) {
                        AnimatedContent(state.downloadMode == SelectionDownloadMode.Delete) { delete ->
                            if (delete) Icon(Icons.Default.Delete, "Delete download")
                            else Icon(Icons.Default.Download, "Download")
                        }
                    }
                }

                VerticalDivider(Modifier.height(24.dp).padding(horizontal = 4.dp))

                IconButton(
                    onClick = selection.onSelectAll,
                    enabled = state.selectAllEnabled
                ) { Icon(Icons.Default.SelectAll, "Select all") }

                IconButton(onClick = selection.onInvert) {
                    Icon(Icons.Default.Flip, "Invert selection")
                }
            }
        }
    }
}
