package com.programmersbox.kmpuiviews.di

import com.programmersbox.kmpuiviews.workers.AppCleanupWorker
import com.programmersbox.kmpuiviews.workers.CloudToLocalSyncWorker
import com.programmersbox.kmpuiviews.workers.LocalToCloudSyncWorker
import com.programmersbox.kmpuiviews.workers.NotifySingleWorker
import com.programmersbox.kmpuiviews.workers.UpdateFlowWorker
import org.koin.androidx.workmanager.dsl.workerOf
import org.koin.core.module.Module

fun Module.kmpWorkers() {
    workerOf(::AppCleanupWorker)
    workerOf(::NotifySingleWorker)
    workerOf(::LocalToCloudSyncWorker)
    workerOf(::CloudToLocalSyncWorker)
    workerOf(::UpdateFlowWorker)
}