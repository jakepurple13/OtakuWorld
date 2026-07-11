package com.programmersbox.kmpuiviews.utils.backupproccesor

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Whatshot
import com.programmersbox.favoritesdatabase.HeatMapDao
import com.programmersbox.favoritesdatabase.HeatMapItem
import com.programmersbox.sharedcomponents.backup.BackupDataSummary
import com.programmersbox.sharedcomponents.backup.BackupUiInfo
import com.programmersbox.sharedtools.BackupProcessor
import okio.BufferedSink
import okio.BufferedSource

class HeatMapBackupProcessor(
    private val heatMapDao: HeatMapDao,
) : BackupProcessor(), BackupUiInfo {
    override val fileName: String
        get() = "heat_map.json"

    override val key: String get() = fileName
    override val displayName: String get() = "Activity Heat Map"
    override val description: String? get() = "Daily usage activity records"
    override val icon get() = Icons.Default.Whatshot

    override suspend fun backup(sink: BufferedSink) {
        heatMapDao
            .getAllHeatMapsSync()
            .toJson()
            .let { sink.writeUtf8(it) }
    }

    override suspend fun restore(json: String, bufferedSource: BufferedSource) {
        json.fromJson<List<HeatMapItem>>().forEach { heatMapDao.insertHeatMap(it) }
    }

    override suspend fun currentSummary() = BackupDataSummary(itemCount = heatMapDao.getAllHeatMapsSync().size)

    override suspend fun parseSummary(json: String?, rawBytes: ByteArray?) = BackupDataSummary(
        itemCount = json?.let { runCatching { it.fromJson<List<HeatMapItem>>().size }.getOrNull() },
        sizeBytes = rawBytes?.size?.toLong(),
    )
}
