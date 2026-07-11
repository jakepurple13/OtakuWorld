package com.programmersbox.sharedcomponents.backup

import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.Serializable
import kotlin.time.Instant

interface BackupUiInfo {
    val key: String
    val displayName: String
    val description: String?
    val icon: ImageVector?
    suspend fun currentSummary(): BackupDataSummary
    suspend fun parseSummary(json: String?, rawBytes: ByteArray?): BackupDataSummary
}

data class BackupDataSummary(
    val itemCount: Int? = null,
    val sizeBytes: Long? = null,
    val lastModified: Instant? = null,
    val details: List<Pair<String, String>> = emptyList(),
)

@Serializable
data class ItemResult(
    val key: String,
    val success: Boolean,
    val error: String? = null,
)
