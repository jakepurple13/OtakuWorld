package com.programmersbox.kmpuiviews.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

class WorkerRepositoryImpl : WorkRepository {
    override val manualCheck: Flow<List<WorkInfoKmp>>
        get() = emptyFlow()
    override val allWorkCheck: Flow<List<WorkInfoKmp>>
        get() = emptyFlow()

    override fun pruneWork() {

    }

    override fun checkManually() {

    }
}