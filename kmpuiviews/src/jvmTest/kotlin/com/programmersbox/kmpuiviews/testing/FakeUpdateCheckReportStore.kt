package com.programmersbox.kmpuiviews.testing

import com.programmersbox.kmpuiviews.domain.updatecheck.UpdateCheckReport
import com.programmersbox.kmpuiviews.domain.updatecheck.UpdateCheckReportStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeUpdateCheckReportStore(initial: UpdateCheckReport? = null) : UpdateCheckReportStore {
    private val state = MutableStateFlow(initial)

    override val report: Flow<UpdateCheckReport?> = state

    override suspend fun current(): UpdateCheckReport? = state.value

    override suspend fun save(report: UpdateCheckReport) {
        state.value = report
    }
}
