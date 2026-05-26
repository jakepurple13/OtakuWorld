package com.programmersbox.manga.shared.reader

import androidx.compose.runtime.Stable

@Stable
sealed class PageItem {
    /**
     * A single manga page. [chapterListIndex] is a raw index into ReadViewModel.list
     * (0 = newest chapter). [pageIndex] is the 0-based position within that chapter.
     */
    @Stable
    data class Page(
        val url: String,
        val chapterListIndex: Int,
        val pageIndex: Int,
        val isDownloaded: Boolean = false,
    ) : PageItem()

    /**
     * A boundary marker between two chapters. Indices are raw positions into
     * ReadViewModel.list (0 = newest chapter).
     */
    @Stable
    data class ChapterTransition(
        val fromChapterListIndex: Int,
        val toChapterListIndex: Int,
    ) : PageItem()
}
