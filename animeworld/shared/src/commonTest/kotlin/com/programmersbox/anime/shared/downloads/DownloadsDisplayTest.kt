package com.programmersbox.anime.shared.downloads

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DownloadsDisplayTest {
    @Test fun splitsShowAndEpisode() = assertEquals(EpisodeTitle("Dragon Ball Daima", "Episode 20 · Maximum"), splitEpisodeTitle("Dragon Ball Daima - Episode 20 - Maximum"))
    @Test fun splitsAtEpisodeWhenShowHasDash() = assertEquals(EpisodeTitle("Re - Zero", "Episode 3 · The Whale"), splitEpisodeTitle("Re - Zero - Episode 3 - The Whale"))
    @Test fun splitsOnFirstDashWithoutEpisodeWord() = assertEquals(EpisodeTitle("Show", "Special 1"), splitEpisodeTitle("Show - Special 1"))
    @Test fun keepsCollisionSuffix() = assertEquals(EpisodeTitle("Show", "Episode 1 · Pilot (2)"), splitEpisodeTitle("Show - Episode 1 - Pilot (2)"))
    @Test fun plainNameIsShowOnly() = assertEquals(EpisodeTitle("VID_20261015_102348", null), splitEpisodeTitle("VID_20261015_102348"))
    @Test fun episodeWithoutName() = assertEquals(EpisodeTitle("Show", "Episode 4"), splitEpisodeTitle("Show - Episode 4"))

    @Test fun inFolder() = assertTrue(isInFolder("/storage/emulated/0/Movies/AnimeWorld/a.ts", "/storage/emulated/0/Movies/AnimeWorld/"))
    @Test fun inFolderWithoutTrailingSlash() = assertTrue(isInFolder("/storage/emulated/0/Movies/AnimeWorld/a.ts", "/storage/emulated/0/Movies/AnimeWorld"))
    @Test fun siblingFolderIsNotInFolder() = assertFalse(isInFolder("/storage/emulated/0/Movies/AnimeWorld2/a.ts", "/storage/emulated/0/Movies/AnimeWorld/"))
    @Test fun nullPathIsNotInFolder() = assertFalse(isInFolder(null, "/storage/emulated/0/Movies/AnimeWorld/"))
    @Test fun subfolderIsInFolder() = assertTrue(isInFolder("/storage/emulated/0/Movies/AnimeWorld/Show/a.ts", "/storage/emulated/0/Movies/AnimeWorld/"))

    @Test fun bytes() {
        assertEquals("0 B", formatBytes(0)); assertEquals("900 KB", formatBytes(900_000))
        assertEquals("92 MB", formatBytes(92_892_116)); assertEquals("1.2 GB", formatBytes(1_200_000_000))
    }

    @Test fun runningKnownTotal() = assertEquals("42% · 39 of 92 MB", QueuedDownload("k", "t", QueuedDownloadState.Running, 0.42f, 39_000_000, 92_000_000).statusLabel())
    @Test fun runningUnknownTotal() = assertEquals("42% · 39 MB", QueuedDownload("k", "t", QueuedDownloadState.Running, 0.42f, 39_000_000, null).statusLabel())
    @Test fun runningUnknownLength() = assertEquals("39 MB", QueuedDownload("k", "t", QueuedDownloadState.Running, null, 39_000_000, null).statusLabel())
    @Test fun runningNoBytesYet() = assertEquals("0%", QueuedDownload("k", "t", QueuedDownloadState.Running, 0f, null, null).statusLabel())
    @Test fun waiting() = assertEquals("Waiting", QueuedDownload("k", "t", QueuedDownloadState.Waiting).statusLabel())
    @Test fun failed() = assertEquals("Failed: All files access is needed to save downloads", QueuedDownload("k", "t", QueuedDownloadState.Failed, error = "All files access is needed to save downloads").statusLabel())
    @Test fun failedNoReason() = assertEquals("Failed", QueuedDownload("k", "t", QueuedDownloadState.Failed).statusLabel())
}
