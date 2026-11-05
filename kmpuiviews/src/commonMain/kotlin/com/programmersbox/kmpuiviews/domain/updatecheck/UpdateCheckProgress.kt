package com.programmersbox.kmpuiviews.domain.updatecheck

import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.kmpmodels.KmpChapterModel
import com.programmersbox.kmpmodels.KmpInfoModel

enum class SourceState { Pending, Running, Done, Failed }

/** Live state of one source during an update check. [name] is the user-facing display name. */
data class SourceProgress(
    val name: String,
    val state: SourceState,
    val favorites: Int,
    val toCheck: Int,
    val checked: Int,
    val updates: Int,
    val failure: FailureInfo?,
)

/** Snapshot of a running update check, emitted after every change. */
data class UpdateCheckProgress(
    val favoriteCount: Int,
    val loadingRecents: Boolean,
    val total: Int,
    val checked: Int,
    val current: String?,
    val sources: List<SourceProgress>,
    val updatesFound: Int,
    val itemFailures: Int,
)

enum class UpdateCheckMode { Full, RetryFailed }

data class UpdatedItem(
    val info: KmpInfoModel,
    val dbModel: DbModel,
    val newChapters: List<KmpChapterModel>,
)

/** [report] is this run's own report (before any retry merge); null when nothing ran. */
data class UpdateCheckResult(
    val updates: List<UpdatedItem>,
    val report: UpdateCheckReport?,
)
