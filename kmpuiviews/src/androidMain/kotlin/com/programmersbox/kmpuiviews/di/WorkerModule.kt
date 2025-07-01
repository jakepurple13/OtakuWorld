package com.programmersbox.kmpuiviews.di

import com.programmersbox.kmpuiviews.workers.AppCheckWorker
import com.programmersbox.kmpuiviews.workers.AppCleanupWorker
import com.programmersbox.kmpuiviews.workers.CloudToLocalSyncWorker
import com.programmersbox.kmpuiviews.workers.DownloadAndInstallWorker
import com.programmersbox.kmpuiviews.workers.DownloadWorker
import com.programmersbox.kmpuiviews.workers.InstallWorker
import com.programmersbox.kmpuiviews.workers.LocalToCloudSyncWorker
import com.programmersbox.kmpuiviews.workers.NotifySingleWorker
import com.programmersbox.kmpuiviews.workers.SourceUpdateChecker
import com.programmersbox.kmpuiviews.workers.UpdateFlowWorker
import org.koin.androidx.workmanager.dsl.workerOf
import org.koin.core.module.Module

fun Module.kmpWorkers() {
    workerOf(::AppCleanupWorker)
    workerOf(::NotifySingleWorker)
    workerOf(::LocalToCloudSyncWorker)
    workerOf(::CloudToLocalSyncWorker)
    workerOf(::UpdateFlowWorker)
    workerOf(::SourceUpdateChecker)
    workerOf(::AppCheckWorker)
    workerOf(::DownloadAndInstallWorker)
    workerOf(::DownloadWorker)
    workerOf(::InstallWorker)
}