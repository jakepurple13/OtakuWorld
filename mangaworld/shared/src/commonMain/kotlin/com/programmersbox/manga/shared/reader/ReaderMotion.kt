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
