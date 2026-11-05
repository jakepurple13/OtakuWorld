package com.programmersbox.kmpuiviews.domain.updatecheck

import com.programmersbox.datastore.DataStoreHandling
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

/** Holds only the most recent update check report. */
interface UpdateCheckReportStore {
    val report: Flow<UpdateCheckReport?>
    suspend fun current(): UpdateCheckReport?
    suspend fun save(report: UpdateCheckReport)
}

class DataStoreUpdateCheckReportStore(
    private val dataStoreHandling: DataStoreHandling,
) : UpdateCheckReportStore {
    override val report: Flow<UpdateCheckReport?> = dataStoreHandling.lastUpdateCheckReport
        .asFlow()
        .map(::decodeUpdateCheckReport)

    override suspend fun current(): UpdateCheckReport? =
        decodeUpdateCheckReport(dataStoreHandling.lastUpdateCheckReport.get())

    override suspend fun save(report: UpdateCheckReport) {
        dataStoreHandling.lastUpdateCheckReport.set(report.encode())
    }
}

private val reportJson = Json { ignoreUnknownKeys = true }

fun UpdateCheckReport.encode(): String = reportJson.encodeToString(UpdateCheckReport.serializer(), this)

/** Returns null for a blank or undecodable (corrupt, older schema) value. */
fun decodeUpdateCheckReport(raw: String): UpdateCheckReport? {
    if (raw.isBlank()) return null
    return runCatching { reportJson.decodeFromString(UpdateCheckReport.serializer(), raw) }.getOrNull()
}
