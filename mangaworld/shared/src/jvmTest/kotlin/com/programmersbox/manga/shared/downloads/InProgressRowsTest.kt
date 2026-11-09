package com.programmersbox.manga.shared.downloads

import kotlin.test.Test
import kotlin.test.assertEquals

class InProgressRowsTest {

    private fun progress(url: String, state: DownloadState) =
        ChapterDownloadProgress(chapterUrl = url, chapterName = url, mangaTitle = "Title", state = state)

    // Re-queuing a running chapter yields a RUNNING and a BLOCKED WorkInfo with the same url;
    // the Downloads list keys rows by url, so a duplicate would crash it.
    @Test fun sameChapterTwiceShowsOneRow() {
        val rows = listOf(
            progress("c1", DownloadState.Downloading(2, 10)),
            progress("c1", DownloadState.Queued),
            progress("c2", DownloadState.Queued),
        ).inProgressRows()

        assertEquals(listOf("c1", "c2"), rows.map { it.chapterUrl })
    }

    @Test fun finishedDownloadsAreLeftOut() {
        val rows = listOf(
            progress("c1", DownloadState.Completed),
            progress("c2", DownloadState.Failed("x")),
            progress("c3", DownloadState.Cancelled),
            progress("c4", DownloadState.Queued),
        ).inProgressRows()

        assertEquals(listOf("c4"), rows.map { it.chapterUrl })
    }
}
