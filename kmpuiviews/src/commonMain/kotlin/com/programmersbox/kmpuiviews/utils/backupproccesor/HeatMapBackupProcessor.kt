package com.programmersbox.kmpuiviews.utils.backupproccesor

import com.programmersbox.favoritesdatabase.HeatMapDao
import com.programmersbox.favoritesdatabase.HeatMapItem
import okio.BufferedSink
import okio.BufferedSource

class HeatMapBackupProcessor(
    private val heatMapDao: HeatMapDao,
) : BackupProcessor() {
    override val fileName: String
        get() = "heat_map.json"

    override suspend fun backup(sink: BufferedSink) {
        heatMapDao
            .getAllHeatMapsSync()
            .toJson()
            .let { sink.writeUtf8(it) }
    }

    override suspend fun restore(json: String, bufferedSource: BufferedSource) {
        json.fromJson<List<HeatMapItem>>().forEach { heatMapDao.insertHeatMap(it) }
    }
}