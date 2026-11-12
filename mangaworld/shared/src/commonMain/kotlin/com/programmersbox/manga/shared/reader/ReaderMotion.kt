package com.programmersbox.manga.shared.reader

import com.programmersbox.datastore.mangasettings.ReaderType
import io.kamel.core.DataSource

/** Scale a freshly loaded page starts at before settling to 1f. */
internal const val REVEAL_START_SCALE = 1.04f

internal data class RevealStart(val alpha: Float, val scale: Float)

/** Memory-cache hits (scrolling back, prefetched pages) show instantly; only fresh loads fade and settle in. */
internal fun pageRevealStart(source: DataSource): RevealStart =
    if (source == DataSource.Memory) RevealStart(alpha = 1f, scale = 1f)
    else RevealStart(alpha = 0f, scale = REVEAL_START_SCALE)

/** 1-based position of a chapter counting from the oldest; list index 0 is the newest chapter. */
internal fun chapterPosition(listIndex: Int, chapterCount: Int): Int = chapterCount - listIndex

/** How far through the series a chapter sits, in 0..1. */
internal fun seriesProgress(listIndex: Int, chapterCount: Int): Float =
    if (chapterCount <= 0) 0f
    else (chapterPosition(listIndex, chapterCount).toFloat() / chapterCount).coerceIn(0f, 1f)

/** Fraction of the current chapter read, in 0..1; [currentPage] is 0-based. */
internal fun chapterProgress(currentPage: Int, pages: Int): Float =
    if (pages <= 0) 0f else ((currentPage + 1f) / pages).coerceIn(0f, 1f)

/** Only List <-> Pager share page elements; switches involving Flip or Curl just fade. */
internal fun morphsPages(from: ReaderType, to: ReaderType): Boolean =
    (from == ReaderType.List && to == ReaderType.Pager) || (from == ReaderType.Pager && to == ReaderType.List)

/** Page chip text; the end-of-chapter card sits one past the last page, so the number is capped. */
internal fun pageLabel(currentPage: Int, pages: Int): String =
    "p. ${(currentPage + 1).coerceAtMost(pages)} / $pages"

/** Banner suffix for a chapter's page count, or null while unknown or when nothing loaded. */
internal fun bannerPageText(pageCount: Int?): String? = when {
    pageCount == null || pageCount <= 0 -> null
    pageCount == 1 -> " · 1 page"
    else -> " · $pageCount pages"
}

/** 1-based page number shown by the page indicators, capped so the end-of-chapter card reads as the last page. */
internal fun displayPage(currentPage: Int, pages: Int): Int = (currentPage + 1).coerceIn(1, pages.coerceAtLeast(1))

/** Past this many pages the edge rail gives each dot a range of pages. */
internal const val MAX_RAIL_DOTS = 40

internal fun railDotCount(pages: Int): Int = pages.coerceIn(1, MAX_RAIL_DOTS)

/** Rail dot that holds [page] (0-based). */
internal fun railDotFor(page: Int, pages: Int): Int {
    val dots = railDotCount(pages)
    val clamped = page.coerceIn(0, (pages - 1).coerceAtLeast(0))
    return (clamped * dots / pages.coerceAtLeast(1)).coerceIn(0, dots - 1)
}

/** First page (0-based) in a rail dot's range. */
internal fun railFirstPage(dot: Int, pages: Int): Int {
    val dots = railDotCount(pages)
    return (dot * pages + dots - 1) / dots
}

/** Past this many pages the segmented line would be too thin, so it falls back to the plain line. */
internal const val MAX_SEGMENTS = 60

internal fun usesSegments(pages: Int): Boolean = pages in 1..MAX_SEGMENTS

/** The folio sits in the outer corner like a printed book: start on even pages, end on odd. */
internal fun folioOnStart(page: Int): Boolean = page % 2 == 0
