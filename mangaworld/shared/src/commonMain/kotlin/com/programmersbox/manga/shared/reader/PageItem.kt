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

/** A chapter that reaching a [PageItem.ChapterTransition] should load. */
internal sealed interface TransitionLoad {
    val chapterListIndex: Int

    /** Newer chapter added below the loaded ones. */
    data class Append(override val chapterListIndex: Int) : TransitionLoad

    /** Older chapter added above the loaded ones. */
    data class Prepend(override val chapterListIndex: Int) : TransitionLoad
}

/**
 * Every boundary goes from an older chapter (higher index) to a newer one. The newer side is
 * appended whenever it isn't loaded; the older side only when the reader is scrolling back, so
 * opening a chapter at its top boundary doesn't load the previous one.
 */
internal fun PageItem.ChapterTransition.loadFor(
    loadedChapters: Collection<Int>,
    scrolledBackward: Boolean,
): TransitionLoad? = when {
    toChapterListIndex !in loadedChapters -> TransitionLoad.Append(toChapterListIndex)
    scrolledBackward && fromChapterListIndex !in loadedChapters -> TransitionLoad.Prepend(fromChapterListIndex)
    else -> null
}

/**
 * Drops [chapterListIndex]'s pages and the boundary on its outer side. [fromTop] means the chapter
 * sits above the kept ones. The boundary facing the kept chapters stays, so reaching it loads the
 * chapter again.
 */
internal fun MutableList<PageItem>.evictChapter(chapterListIndex: Int, fromTop: Boolean) {
    removeAll { item ->
        when (item) {
            is PageItem.Page -> item.chapterListIndex == chapterListIndex
            is PageItem.ChapterTransition -> if (fromTop) {
                item.toChapterListIndex == chapterListIndex
            } else {
                item.fromChapterListIndex == chapterListIndex
            }
        }
    }
}
