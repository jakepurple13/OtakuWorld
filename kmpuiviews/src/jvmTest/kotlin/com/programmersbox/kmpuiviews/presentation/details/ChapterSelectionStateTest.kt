package com.programmersbox.kmpuiviews.presentation.details

import com.programmersbox.kmpmodels.ExampleService
import com.programmersbox.kmpmodels.KmpChapterModel
import com.programmersbox.kmpuiviews.ChapterDownloadUiState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChapterSelectionStateTest {

    private val service = ExampleService()

    private val chapters = List(10) {
        KmpChapterModel(
            name = "Chapter $it",
            url = "https://example.com/$it",
            uploaded = "",
            sourceUrl = "https://example.com/",
            source = service,
        )
    }

    private fun state(
        selected: List<KmpChapterModel>,
        readUrls: Set<String> = emptySet(),
        bookmarkedUrls: Set<String> = emptySet(),
        downloadStates: Map<String, ChapterDownloadUiState> = emptyMap(),
        canDownload: Boolean = true,
        supportsBatchDownload: Boolean = true,
    ) = chapterSelectionUiState(
        selected = selected,
        totalChapters = chapters.size,
        readUrls = readUrls,
        bookmarkedUrls = bookmarkedUrls,
        downloadStates = downloadStates,
        canDownload = canDownload,
        supportsBatchDownload = supportsBatchDownload,
    )

    @Test
    fun `single selection shows single actions`() {
        val s = state(chapters.take(1))
        assertEquals(1, s.count)
        assertTrue(s.showSingleActions)
        assertTrue(s.selectAllEnabled)
        assertFalse(state(chapters.take(2)).showSingleActions)
    }

    @Test
    fun `all read flips to unread only when every selected is read`() {
        val picked = chapters.take(2)
        assertFalse(state(picked, readUrls = setOf(picked[0].url)).allRead)
        assertTrue(state(picked, readUrls = picked.map { it.url }.toSet()).allRead)
    }

    @Test
    fun `mixed bookmarks is not allBookmarked`() {
        val picked = chapters.take(2)
        assertFalse(state(picked, bookmarkedUrls = setOf(picked[0].url)).allBookmarked)
        assertTrue(state(picked, bookmarkedUrls = picked.map { it.url }.toSet()).allBookmarked)
    }

    @Test
    fun `download hidden when source cannot download`() {
        assertEquals(SelectionDownloadMode.Hidden, state(chapters.take(1), canDownload = false).downloadMode)
    }

    @Test
    fun `batch unsupported hides download for two or more`() {
        assertEquals(
            SelectionDownloadMode.Download,
            state(chapters.take(1), supportsBatchDownload = false).downloadMode
        )
        assertEquals(
            SelectionDownloadMode.Hidden,
            state(chapters.take(2), supportsBatchDownload = false).downloadMode
        )
        val picked = chapters.take(2)
        assertEquals(
            SelectionDownloadMode.Hidden,
            state(
                picked,
                downloadStates = picked.associate { it.url to ChapterDownloadUiState.Downloaded },
                supportsBatchDownload = false,
            ).downloadMode
        )
    }

    @Test
    fun `all downloaded shows delete`() {
        val picked = chapters.take(2)
        assertEquals(
            SelectionDownloadMode.Delete,
            state(picked, downloadStates = picked.associate { it.url to ChapterDownloadUiState.Downloaded }).downloadMode
        )
        assertEquals(
            SelectionDownloadMode.Delete,
            state(
                picked.take(1),
                downloadStates = mapOf(picked[0].url to ChapterDownloadUiState.Downloaded),
                supportsBatchDownload = false,
            ).downloadMode
        )
    }

    @Test
    fun `chaptersToDownload skips downloaded and queued`() {
        val (a, b, c, d) = chapters.take(4)
        val s = state(
            listOf(a, b, c, d),
            downloadStates = mapOf(
                a.url to ChapterDownloadUiState.Downloaded,
                b.url to ChapterDownloadUiState.Queued,
                c.url to ChapterDownloadUiState.None,
            ),
        )
        assertEquals(SelectionDownloadMode.Download, s.downloadMode)
        assertEquals(listOf(c, d), s.chaptersToDownload)
    }

    @Test
    fun `selectAll disabled when everything selected`() {
        assertFalse(state(chapters).selectAllEnabled)
    }
}
