package com.programmersbox.kmpuiviews.utils.backupproccesor

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ThumbUp
import com.programmersbox.favoritesdatabase.Recommendation
import com.programmersbox.favoritesdatabase.RecommendationDao
import com.programmersbox.sharedcomponents.backup.BackupDataSummary
import com.programmersbox.sharedcomponents.backup.BackupUiInfo
import com.programmersbox.sharedtools.BackupProcessor
import okio.BufferedSink
import okio.BufferedSource

class RecommendationsBackupProcessor(
    private val recommendationDao: RecommendationDao,
) : BackupProcessor(), BackupUiInfo {
    override val fileName: String
        get() = "recommendations.json"

    override val key: String get() = fileName
    override val displayName: String get() = "Recommendations"
    override val description: String? get() = "AI/recommendation cache"
    override val icon get() = Icons.Default.ThumbUp

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

    override suspend fun currentSummary() = BackupDataSummary(itemCount = recommendationDao.getAllRecommendationsSync().size)

    override suspend fun parseSummary(json: String?, rawBytes: ByteArray?) = BackupDataSummary(
        itemCount = json?.let { runCatching { it.fromJson<List<Recommendation>>().size }.getOrNull() },
        sizeBytes = rawBytes?.size?.toLong(),
    )
}
