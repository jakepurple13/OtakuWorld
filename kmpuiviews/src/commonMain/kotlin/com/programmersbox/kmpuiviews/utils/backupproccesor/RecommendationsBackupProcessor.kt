package com.programmersbox.kmpuiviews.utils.backupproccesor

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ThumbUp
import com.programmersbox.favoritesdatabase.Recommendation
import com.programmersbox.favoritesdatabase.RecommendationDao
import com.programmersbox.sharedcomponents.backup.BackupDataSummary
import com.programmersbox.sharedcomponents.backup.BackupUiInfo
import com.programmersbox.sharedtools.BackupProcessor
import com.programmersbox.sharedtools.ProcessorResult
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

    override suspend fun backup(sink: BufferedSink): ProcessorResult {
        val recommendations = recommendationDao.getAllRecommendationsSync()
        recommendations.toJson().let { sink.writeUtf8(it) }
        return ProcessorResult(successCount = recommendations.size)
    }

    override suspend fun restore(json: String, bufferedSource: BufferedSource): ProcessorResult =
        json.fromJson<List<Recommendation>>().restoreEachCatching(idOf = { it.title }) {
            recommendationDao.insertRecommendation(it)
        }

    override suspend fun currentSummary() = BackupDataSummary(itemCount = recommendationDao.getAllRecommendationsSync().size)

    override suspend fun parseSummary(json: String?, rawBytes: ByteArray?) = BackupDataSummary(
        itemCount = json?.let { runCatching { it.fromJson<List<Recommendation>>().size }.getOrNull() },
        sizeBytes = rawBytes?.size?.toLong(),
    )
}
