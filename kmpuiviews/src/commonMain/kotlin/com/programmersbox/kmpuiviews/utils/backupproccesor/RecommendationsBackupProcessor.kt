package com.programmersbox.kmpuiviews.utils.backupproccesor

import com.programmersbox.favoritesdatabase.Recommendation
import com.programmersbox.favoritesdatabase.RecommendationDao
import com.programmersbox.sharedtools.BackupProcessor
import okio.BufferedSink
import okio.BufferedSource

class RecommendationsBackupProcessor(
    private val recommendationDao: RecommendationDao,
) : BackupProcessor() {
    override val fileName: String
        get() = "recommendations.json"

    override suspend fun backup(sink: BufferedSink) {
        recommendationDao
            .getAllRecommendationsSync()
            .toJson()
            .let { sink.writeUtf8(it) }
    }

    override suspend fun restore(json: String, bufferedSource: BufferedSource) {
        json
            .fromJson<List<Recommendation>>()
            .forEach { recommendationDao.insertRecommendation(it) }
    }
}