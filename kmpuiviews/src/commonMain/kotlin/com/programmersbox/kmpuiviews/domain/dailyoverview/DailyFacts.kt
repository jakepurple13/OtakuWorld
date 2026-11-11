package com.programmersbox.kmpuiviews.domain.dailyoverview

import com.programmersbox.kmpuiviews.domain.updatecheck.UpdateUnit
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class TitleRead(
    val title: String,
    val url: String,
    val imageUrl: String,
    val source: String,
    val firstChapter: String,
    val lastChapter: String,
    val count: Int,
    val caughtUp: Boolean,
)

@Serializable
data class UpdateItem(
    val title: String,
    val url: String,
    val imageUrl: String?,
    val source: String,
    val summary: String,
)

@Serializable
data class DailyFacts(
    val date: LocalDate,
    val unit: UpdateUnit,
    val readCount: Int,
    val titles: List<TitleRead>,
    /** Always 24 entries, index is the local hour of day. */
    val readsByHour: List<Int>,
    val updates: List<UpdateItem>,
    val failedSources: List<String>,
) {
    // titles too: the heat-map count can be 0 while watched-chapter rows exist.
    val isQuiet: Boolean get() = readCount == 0 && titles.isEmpty() && updates.isEmpty()
}
