package com.programmersbox.kmpuiviews.utils.backupproccesor

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.ui.graphics.vector.ImageVector
import com.programmersbox.favoritesdatabase.ActivityDao
import com.programmersbox.favoritesdatabase.ActivityTable
import com.programmersbox.sharedcomponents.backup.BackupDataSummary
import com.programmersbox.sharedcomponents.backup.BackupUiInfo
import com.programmersbox.sharedtools.BackupProcessor
import okio.BufferedSink
import okio.BufferedSource
import kotlin.time.Instant

class ActivityBackupProcessor(
    private val activityDao: ActivityDao,
) : BackupProcessor(), BackupUiInfo {
    override val fileName: String
        get() = "activity.json"

    override suspend fun backup(sink: BufferedSink) {
        activityDao
            .getActivity()
            ?.toJson()
            ?.let { sink.writeUtf8(it) }
    }

    override suspend fun restore(json: String, bufferedSource: BufferedSource) {
        json
            .fromJson<ActivityTable>()
            .let { activityDao.upsertSynced(it.cumulativeSeconds, it.updatedAt) }
    }

    override val key: String
        get() = "activity"
    override val displayName: String
        get() = "Time Spent Doing"
    override val description: String
        get() = "Time spent doing things"
    override val icon: ImageVector
        get() = Icons.Default.AccessTime

    override suspend fun currentSummary(): BackupDataSummary {
        val item = activityDao.getActivity()
        return BackupDataSummary(
            itemCount = item?.cumulativeSeconds?.toInt(),
            lastModified = item?.updatedAt?.let {
                Instant.fromEpochSeconds(it)
            }
        )
    }

    override suspend fun parseSummary(
        json: String?,
        rawBytes: ByteArray?,
    ): BackupDataSummary {
        return BackupDataSummary(
            itemCount = json?.fromJson<ActivityTable>()?.cumulativeSeconds?.toInt(),
            sizeBytes = rawBytes?.size?.toLong()
        )
    }
}