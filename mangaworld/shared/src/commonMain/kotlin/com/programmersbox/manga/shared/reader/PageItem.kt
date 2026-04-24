package com.programmersbox.manga.shared.reader

sealed class PageItem {
    /**
     * A single manga page. [chapterListIndex] is a raw index into ReadViewModel.list
     * (0 = newest chapter). [pageIndex] is the 0-based position within that chapter.
     */
    data class Page(
        val url: String,
        val chapterListIndex: Int,
        val pageIndex: Int,
    ) : PageItem()

    /**
     * A boundary marker between two chapters. Indices are raw positions into
     * ReadViewModel.list (0 = newest chapter).
     */
    data class ChapterTransition(
        val fromChapterListIndex: Int,
        val toChapterListIndex: Int,
    ) : PageItem()
}
