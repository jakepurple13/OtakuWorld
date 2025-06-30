package com.programmersbox.uiviews.checkers

import androidx.work.ListenableWorker

fun <T> Result<T>.workerReturn() = fold(
    onSuccess = { ListenableWorker.Result.success() },
    onFailure = { ListenableWorker.Result.failure() }
)