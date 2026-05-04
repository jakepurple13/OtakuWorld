package com.programmersbox.kmpuiviews.repository

import com.programmersbox.kmpuiviews.presentation.settings.workerinfo.WorkerInfoModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class BackgroundWorkHandlerImpl : BackgroundWorkHandler {
    override fun localToCloudListener(): Flow<List<WorkInfoKmp>> = flowOf(emptyList())

    override fun cloudToLocalListener(): Flow<List<WorkInfoKmp>> = flowOf(emptyList())

    override fun syncLocalToCloud() {

    }

    override fun syncCloudToLocal() {

    }

    override fun setupPeriodicCheckers() {

    }

    override fun workerInfoFlow(): Flow<List<WorkerInfoModel>> = flowOf(emptyList())

    override fun sourceUpdate() {

    }

    override fun cancel(uuid: String) {

    }
}