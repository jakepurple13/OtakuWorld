package com.programmersbox.telemetry.events

import com.programmersbox.telemetry.AnalyticsEvent

// Every event the apps can send. TELEMETRY.md documents each one; update it with any change here.
// Counts and durations are passed in already bucketed (see Buckets). Source names go through TelemetrySourceNamer.

/** Where the user came from when opening a screen. */
object OpenedFrom {
    const val DETAILS = "details"
    const val SEARCH = "search"
    const val RECENT = "recent"
    const val ALL = "all"
    const val FAVORITES = "favorites"
    const val HISTORY = "history"
    const val NOTIFICATION = "notification"
    /** The system notification for new chapters, as opposed to the in-app Notifications screen. */
    const val UPDATE_NOTIFICATION = "update_notification"
    const val DEEP_LINK = "deep_link"
    const val QUICK_ACTION = "quick_action"
    const val DOWNLOADS = "downloads"
    const val LONG_PRESS = "long_press"
    const val LISTS = "lists"
    const val OTHER = "other"
}

/** Every event name, in catalog order. */
val telemetryEventNames: List<String> = listOf(
    "onboarding_finished", "onboarding_step", "source_selected", "search", "describe_search",
    "item_viewed", "favorite_added", "favorite_removed", "list_item_added", "list_created",
    "list_imported", "item_shared", "note_saved", "mood_tag_added", "update_check_toggled",
    "chapter_opened", "reading_session", "reader_setting_changed", "chapter_bookmarked", "dictionary_lookup",
    "episode_play_requested", "playback_started", "playback_error", "download_started", "download_finished",
    "favorites_filtered", "favorites_searched", "history_cleared", "update_check_finished",
    "update_notification_opened", "catch_up_used", "notification_dismissed", "extension_installed",
    "extension_install_failed", "extension_uninstalled", "extension_updated", "source_update_skipped",
    "source_error", "backup_created", "restore_finished", "sync_sign_in", "sync_failed", "ai_task",
    "ai_model_installed", "translation_used", "quick_action_used", "daily_overview_opened",
)

// Onboarding

data class OnboardingFinished(val skipped: Boolean, val stepsSeen: Int) :
    AnalyticsEvent("onboarding_finished", mapOf("skipped" to skipped, "steps_seen" to stepsSeen))

data class OnboardingStep(val step: Int) : AnalyticsEvent("onboarding_step", mapOf("step" to step))

// Browse & search

data class SourceSelected(val source: String, val mediaType: String) :
    AnalyticsEvent("source_selected", mapOf("source" to source, "media_type" to mediaType))

data class Search(val scope: String, val sourceCount: Int, val resultCount: String) :
    AnalyticsEvent("search", mapOf("scope" to scope, "source_count" to sourceCount, "result_count" to resultCount)) {
    companion object {
        const val GLOBAL = "global"
        const val SOURCE = "source"
    }
}

data class DescribeSearch(val outcome: String, val resultCount: String) :
    AnalyticsEvent("describe_search", mapOf("outcome" to outcome, "result_count" to resultCount)) {
    companion object {
        const val SUCCESS = "success"
        const val EMPTY = "empty"
        const val ERROR = "error"
    }
}

// Details

data class ItemViewed(val source: String, val mediaType: String, val from: String) :
    AnalyticsEvent("item_viewed", mapOf("source" to source, "media_type" to mediaType, "from" to from))

data class FavoriteAdded(val source: String, val from: String) :
    AnalyticsEvent("favorite_added", mapOf("source" to source, "from" to from))

data class FavoriteRemoved(val source: String) : AnalyticsEvent("favorite_removed", mapOf("source" to source))

data class ListItemAdded(val listCount: Int) : AnalyticsEvent("list_item_added", mapOf("list_count" to listCount))

data object ListCreated : AnalyticsEvent("list_created")

data class ListImported(val itemCount: String, val kind: String) :
    AnalyticsEvent("list_imported", mapOf("item_count" to itemCount, "kind" to kind)) {
    companion object {
        const val LIST = "list"
        const val FULL = "full"
    }
}

data class ItemShared(val method: String) : AnalyticsEvent("item_shared", mapOf("method" to method)) {
    companion object {
        const val LINK = "link"
        const val CARD = "card"
        const val QR = "qr"
        const val SAVE = "save"
    }
}

data class NoteSaved(val isNew: Boolean) : AnalyticsEvent("note_saved", mapOf("is_new" to isNew))

data class MoodTagAdded(val kind: String) : AnalyticsEvent("mood_tag_added", mapOf("kind" to kind)) {
    companion object {
        const val PRESET = "preset"
        const val CUSTOM = "custom"
        const val AI = "ai"
    }
}

data class UpdateCheckToggled(val enabled: Boolean) :
    AnalyticsEvent("update_check_toggled", mapOf("enabled" to enabled))

// Reading & watching

data class ChapterOpened(val source: String, val mediaType: String, val from: String) :
    AnalyticsEvent("chapter_opened", mapOf("source" to source, "media_type" to mediaType, "from" to from))

data class ReadingSession(val mediaType: String, val pagesRead: String, val duration: String, val reachedEnd: Boolean) :
    AnalyticsEvent(
        "reading_session",
        mapOf("media_type" to mediaType, "pages_read" to pagesRead, "duration" to duration, "reached_end" to reachedEnd),
    )

data class ReaderSettingChanged(val key: String, val value: String) :
    AnalyticsEvent("reader_setting_changed", mapOf("key" to key, "value" to value))

data class ChapterBookmarked(val source: String) : AnalyticsEvent("chapter_bookmarked", mapOf("source" to source))

data class DictionaryLookup(val action: String) : AnalyticsEvent("dictionary_lookup", mapOf("action" to action)) {
    companion object {
        const val LOOKUP = "lookup"
        const val ADD = "add"
    }
}

data class EpisodePlayRequested(val source: String, val linkCount: Int) :
    AnalyticsEvent("episode_play_requested", mapOf("source" to source, "link_count" to linkCount))

data class PlaybackStarted(val playerStyle: String, val casting: Boolean, val ttff: String) :
    AnalyticsEvent("playback_started", mapOf("player_style" to playerStyle, "casting" to casting, "ttff" to ttff))

data class PlaybackError(val source: String, val errorType: String) :
    AnalyticsEvent("playback_error", mapOf("source" to source, "error_type" to errorType))

data class DownloadStarted(val source: String, val mediaType: String) :
    AnalyticsEvent("download_started", mapOf("source" to source, "media_type" to mediaType))

data class DownloadFinished(val mediaType: String, val outcome: String, val duration: String) :
    AnalyticsEvent("download_finished", mapOf("media_type" to mediaType, "outcome" to outcome, "duration" to duration)) {
    companion object {
        const val SUCCESS = "success"
        const val FAIL = "fail"
        const val CANCEL = "cancel"
    }
}

// Library & history

data class FavoritesFiltered(val filters: Int, val sort: String) :
    AnalyticsEvent("favorites_filtered", mapOf("filters" to filters, "sort" to sort))

data class FavoritesSearched(val resultCount: String) :
    AnalyticsEvent("favorites_searched", mapOf("result_count" to resultCount))

data class HistoryCleared(val scope: String) : AnalyticsEvent("history_cleared", mapOf("scope" to scope)) {
    companion object {
        const val ALL = "all"
        const val ITEM = "item"
    }
}

// Updates & notifications

data class UpdateCheckFinished(
    val trigger: String,
    val itemsChecked: String,
    val updatesFound: String,
    val sourcesFailed: Int,
    val duration: String,
) : AnalyticsEvent(
    "update_check_finished",
    mapOf(
        "trigger" to trigger,
        "items_checked" to itemsChecked,
        "updates_found" to updatesFound,
        "sources_failed" to sourcesFailed,
        "duration" to duration,
    ),
) {
    companion object {
        const val PERIODIC = "periodic"
        const val MANUAL = "manual"
        const val RETRY = "retry"
    }
}

data class UpdateNotificationOpened(val count: Int) :
    AnalyticsEvent("update_notification_opened", mapOf("count" to count))

data class CatchUpUsed(val action: String) : AnalyticsEvent("catch_up_used", mapOf("action" to action)) {
    companion object {
        const val READ = "read"
        const val SKIP = "skip"
        const val DELETE = "delete"
        const val REMIND = "remind"
    }
}

data object NotificationDismissed : AnalyticsEvent("notification_dismissed")

// Extensions & sources

data class ExtensionInstalled(val packageName: String, val catalog: String) :
    AnalyticsEvent("extension_installed", mapOf("package" to packageName, "catalog" to catalog))

data class ExtensionInstallFailed(val catalog: String, val reason: String) :
    AnalyticsEvent("extension_install_failed", mapOf("catalog" to catalog, "reason" to reason))

data class ExtensionUninstalled(val packageName: String) :
    AnalyticsEvent("extension_uninstalled", mapOf("package" to packageName))

data class ExtensionUpdated(val packageName: String) :
    AnalyticsEvent("extension_updated", mapOf("package" to packageName))

data class SourceUpdateSkipped(val packageName: String) :
    AnalyticsEvent("source_update_skipped", mapOf("package" to packageName))

data class SourceError(val source: String, val operation: String, val errorClass: String) :
    AnalyticsEvent("source_error", mapOf("source" to source, "operation" to operation, "error_class" to errorClass)) {
    companion object {
        const val RECENT = "recent"
        const val ALL = "all"
        const val SEARCH = "search"
        const val INFO = "info"
        const val CHAPTERS = "chapters"
        const val PAGES = "pages"
    }
}

// Backup & sync

data class BackupCreated(val destination: String, val success: Boolean) :
    AnalyticsEvent("backup_created", mapOf("destination" to destination, "success" to success)) {
    companion object {
        const val FILE = "file"
        const val CLOUD = "cloud"
    }
}

data class RestoreFinished(val success: Boolean, val categories: Int) :
    AnalyticsEvent("restore_finished", mapOf("success" to success, "categories" to categories))

data class SyncSignIn(val provider: String) : AnalyticsEvent("sync_sign_in", mapOf("provider" to provider))

data class SyncFailed(val provider: String, val errorClass: String) :
    AnalyticsEvent("sync_failed", mapOf("provider" to provider, "error_class" to errorClass))

// On-device AI

data class AiTaskRun(val feature: String, val engine: String, val outcome: String, val latency: String) :
    AnalyticsEvent("ai_task", mapOf("feature" to feature, "engine" to engine, "outcome" to outcome, "latency" to latency)) {
    companion object {
        const val SUCCESS = "success"
        const val ERROR = "error"
        const val UNAVAILABLE = "unavailable"
    }
}

data class AiModelInstalled(val engine: String, val via: String) :
    AnalyticsEvent("ai_model_installed", mapOf("engine" to engine, "via" to via)) {
    companion object {
        const val DOWNLOAD = "download"
        const val IMPORT = "import"
    }
}

data class TranslationUsed(val kind: String) : AnalyticsEvent("translation_used", mapOf("kind" to kind)) {
    companion object {
        const val DESCRIPTION = "description"
        const val TLDR = "tldr"
    }
}

// Quick actions & overview

data class QuickActionUsed(val action: String, val surface: String) :
    AnalyticsEvent("quick_action_used", mapOf("action" to action, "surface" to surface))

data class DailyOverviewOpened(val from: String) : AnalyticsEvent("daily_overview_opened", mapOf("from" to from))
