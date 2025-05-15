package com.programmersbox.uiviews.di

import com.programmersbox.uiviews.checkers.AppCheckWorker
import com.programmersbox.uiviews.checkers.AppCleanupWorker
import com.programmersbox.uiviews.checkers.CloudToLocalSyncWorker
import com.programmersbox.uiviews.checkers.DownloadAndInstallWorker
import com.programmersbox.uiviews.checkers.DownloadWorker
import com.programmersbox.uiviews.checkers.InstallWorker
import com.programmersbox.uiviews.checkers.LocalToCloudSyncWorker
import com.programmersbox.uiviews.checkers.NotifySingleWorker
import com.programmersbox.uiviews.checkers.SourceUpdateChecker
import com.programmersbox.uiviews.checkers.UpdateFlowWorker
import org.koin.androidx.workmanager.dsl.workerOf
import org.koin.core.module.Module

fun Module.workers() {
    workerOf(::UpdateFlowWorker)
    workerOf(::AppCheckWorker)
    workerOf(::SourceUpdateChecker)
    workerOf(::NotifySingleWorker)
    workerOf(::DownloadAndInstallWorker)
    workerOf(::DownloadWorker)
    workerOf(::InstallWorker)
    workerOf(::AppCleanupWorker)
    workerOf(::LocalToCloudSyncWorker)
    workerOf(::CloudToLocalSyncWorker)
}