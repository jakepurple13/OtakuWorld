package com.programmersbox.kmpuiviews.presentation.details

import com.programmersbox.kmpmodels.KmpChapterModel
import com.programmersbox.kmpuiviews.ChapterDownloadUiState

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
