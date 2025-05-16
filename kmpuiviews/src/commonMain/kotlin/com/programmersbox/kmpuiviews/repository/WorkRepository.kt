package com.programmersbox.kmpuiviews.repository

import com.programmersbox.kmpuiviews.presentation.settings.workerinfo.WorkerInfoModel
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDateTime

interface WorkRepository {
    val manualCheck: Flow<List<WorkInfoKmp>>
    val allWorkCheck: Flow<List<WorkInfoKmp>>
    fun pruneWork()
    fun checkManually()
}

data class WorkInfoKmp(
    val state: String,
    val source: String,
    val progress: Int?,
    val max: Int?,
    val nextScheduleTimeMillis: LocalDateTime,
)

interface BackgroundWorkHandler {
    fun localToCloudListener(): Flow<List<WorkInfoKmp>>
    fun cloudToLocalListener(): Flow<List<WorkInfoKmp>>
    fun syncLocalToCloud()
    fun syncCloudToLocal()
    fun setupPeriodicCheckers()
    fun workerInfoFlow(): Flow<List<WorkerInfoModel>>
}