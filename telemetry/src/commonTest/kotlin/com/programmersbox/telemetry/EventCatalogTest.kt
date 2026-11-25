package com.programmersbox.telemetry

import com.programmersbox.telemetry.events.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EventCatalogTest {
    private val all: List<AnalyticsEvent> = listOf(
        OnboardingFinished(skipped = false, stepsSeen = 4),
        OnboardingStep(step = 2),
        SourceSelected(source = "MangaDex", mediaType = "manga"),
        Search(scope = Search.GLOBAL, sourceCount = 3, resultCount = "6-20"),
        DescribeSearch(outcome = DescribeSearch.SUCCESS, resultCount = "1-5"),
        ItemViewed(source = "MangaDex", mediaType = "manga", from = OpenedFrom.RECENT),
        FavoriteAdded(source = "MangaDex", from = OpenedFrom.DETAILS),
        FavoriteRemoved(source = "MangaDex"),
        ListItemAdded(listCount = 2),
        ListCreated,
        ListImported(itemCount = "6-20", kind = ListImported.LIST),
        ItemShared(method = ItemShared.LINK),
        NoteSaved(isNew = true),
        MoodTagAdded(kind = MoodTagAdded.PRESET),
        UpdateCheckToggled(enabled = true),
        ChapterOpened(source = "MangaDex", mediaType = "manga", from = OpenedFrom.DETAILS),
        ReadingSession(mediaType = "manga", pagesRead = "6-20", duration = "30s-2m", reachedEnd = true),
        ReaderSettingChanged(key = "reader_mode", value = "list"),
        ChapterBookmarked(source = "MangaDex"),
        DictionaryLookup(action = DictionaryLookup.ADD),
        EpisodePlayRequested(source = "Gogo", linkCount = 3),
        PlaybackStarted(playerStyle = "classic", casting = false, ttff = "1-5s"),
        PlaybackError(source = "Gogo", errorType = "ERROR_CODE_IO_NETWORK_CONNECTION_FAILED"),
        DownloadStarted(source = "MangaDex", mediaType = "manga"),
        DownloadFinished(mediaType = "manga", outcome = DownloadFinished.SUCCESS, duration = "5-30s"),
        FavoritesFiltered(filters = 2, sort = "title"),
        FavoritesSearched(resultCount = "1-5"),
        HistoryCleared(scope = HistoryCleared.ALL),
        UpdateCheckFinished(
            trigger = UpdateCheckFinished.PERIODIC,
            itemsChecked = "21+",
            updatesFound = "1-5",
            sourcesFailed = 1,
            duration = "30s-2m",
        ),
        UpdateNotificationOpened(count = 1),
        CatchUpUsed(action = CatchUpUsed.READ),
        NotificationDismissed,
        ExtensionInstalled(packageName = "eu.kanade.tachiyomi.extension.en.mangadex", catalog = "mihon"),
        ExtensionInstallFailed(catalog = "mihon", reason = "Conflict"),
        ExtensionUninstalled(packageName = "eu.kanade.tachiyomi.extension.en.mangadex"),
        ExtensionUpdated(packageName = "eu.kanade.tachiyomi.extension.en.mangadex"),
        SourceUpdateSkipped(packageName = "eu.kanade.tachiyomi.extension.en.mangadex"),
        SourceError(source = "MangaDex", operation = SourceError.INFO, errorClass = "SocketTimeoutException"),
        BackupCreated(destination = BackupCreated.FILE, success = true),
        RestoreFinished(success = true, categories = 4),
        SyncSignIn(provider = "supabase"),
        SyncFailed(provider = "supabase", errorClass = "HttpRequestException"),
        AiTaskRun(feature = "tldr", engine = "nano", outcome = AiTaskRun.SUCCESS, latency = "1-5s"),
        AiModelInstalled(engine = "gemma", via = AiModelInstalled.IMPORT),
        TranslationUsed(kind = TranslationUsed.DESCRIPTION),
        QuickActionUsed(action = "random", surface = "bolt"),
        DailyOverviewOpened(from = OpenedFrom.NOTIFICATION),
    )

    private val forbidden = setOf("title", "url", "link", "query", "text", "note", "email", "path", "name")

    @Test
    fun catalogHas47UniqueEvents() = assertEquals(47, all.map { it.name }.toSet().size)

    @Test
    fun samplesMatchNameList() = assertEquals(telemetryEventNames, all.map { it.name })

    @Test
    fun namesAreSnakeCaseUnder40() = all.forEach {
        assertTrue(Regex("^[a-z][a-z0-9_]{0,39}$").matches(it.name), it.name)
    }

    @Test
    fun paramRules() = all.forEach { e ->
        assertTrue(e.params.size <= 25, e.name)
        e.params.forEach { (k, v) ->
            assertTrue(Regex("^[a-z][a-z0-9_]*$").matches(k), "${e.name}.$k")
            assertTrue(k !in forbidden, "${e.name}.$k is forbidden")
            assertTrue(v is String || v is Int || v is Long || v is Double || v is Boolean, "${e.name}.$k type")
            if (v is String) assertTrue(v.length <= 100, "${e.name}.$k length")
        }
    }

    @Test
    fun dataClassesCompareByValue() = assertEquals(FavoriteRemoved("A"), FavoriteRemoved("A"))
}
