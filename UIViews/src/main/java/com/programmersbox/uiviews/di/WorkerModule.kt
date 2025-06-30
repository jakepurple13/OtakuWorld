package com.programmersbox.uiviews.di

import com.programmersbox.uiviews.checkers.DownloadAndInstallWorker
import com.programmersbox.uiviews.checkers.DownloadWorker
import com.programmersbox.uiviews.checkers.InstallWorker
import org.koin.androidx.workmanager.dsl.workerOf
import org.koin.core.module.Module

fun Module.workers() {
    workerOf(::DownloadAndInstallWorker)
    workerOf(::DownloadWorker)
    workerOf(::InstallWorker)
}