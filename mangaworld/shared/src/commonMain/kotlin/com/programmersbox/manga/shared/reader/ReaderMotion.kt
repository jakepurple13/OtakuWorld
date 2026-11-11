package com.programmersbox.manga.shared.reader

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
