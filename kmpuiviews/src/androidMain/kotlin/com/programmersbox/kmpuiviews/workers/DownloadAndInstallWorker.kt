package com.programmersbox.kmpuiviews.workers

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.PermissionChecker
import androidx.core.net.toUri
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.programmersbox.kmpuiviews.logFirebaseMessage
import com.programmersbox.kmpuiviews.repository.DownloadAndInstallState
import com.programmersbox.kmpuiviews.utils.ConfirmationType
import com.programmersbox.kmpuiviews.utils.DownloadAndInstallStatus
import com.programmersbox.kmpuiviews.utils.DownloadAndInstaller
import com.programmersbox.kmpuiviews.utils.InstallErrorReason
import com.programmersbox.kmpuiviews.utils.NotificationChannels
import com.programmersbox.kmpuiviews.utils.NotificationLogo
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import java.io.File
import java.util.concurrent.TimeUnit

class DownloadAndInstallWorker(
    context: Context,
    workerParams: WorkerParameters,
    private val notificationLogo: NotificationLogo,
    private val downloadAndInstaller: DownloadAndInstaller,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val url = inputData.getString("url") ?: return Result.failure()

        logFirebaseMessage("Downloading and installing")

        //TODO: Need to fix so it doesn't start up again when the app starts up after an update

        var lastStatus: DownloadAndInstallStatus? = null
        val result = runCatching {
            downloadAndInstaller.downloadAndInstall(
                url = url,
                confirmationType = ConfirmationType.IMMEDIATE
            )
                .onEach {
                    lastStatus = it
                    notify(notificationLogo, url) {
                        when (it) {
                            is DownloadAndInstallStatus.Error -> {
                                setContentText("Error: ${it.message}")
                                    .setProgress(0, 0, false)
                            }

                            is DownloadAndInstallStatus.Downloading -> {
                                setContentText("Downloading... ${(it.progress * 100).toInt()}%")
                                    .setProgress(100, (it.progress * 100).toInt(), false)
                            }

                            DownloadAndInstallStatus.Downloaded -> {
                                setContentText("Downloaded")
                                    .setProgress(0, 0, true)
                            }

                            DownloadAndInstallStatus.Installing -> {
                                setContentText("Installing...")
                                    .setProgress(100, 100, true)
                            }

                            DownloadAndInstallStatus.Installed -> {
                                setContentText("Download and install completed.")
                                    .setProgress(0, 0, false)
                                    .setTimeoutAfter(5000)
                            }

                            DownloadAndInstallStatus.PendingUserAction -> {
                                setContentText("Waiting for install confirmation...")
                                    .setProgress(0, 0, false)
                            }

                            DownloadAndInstallStatus.PermissionRequired -> {
                                setContentText("Enable install permission in Settings, then retry.")
                                    .setProgress(0, 0, false)
                            }

                            DownloadAndInstallStatus.Cancelled -> {
                                setContentText("Install cancelled.")
                                    .setProgress(0, 0, false)
                            }
                        }
                    }

                    //TODO: Replace this with the repository
                    setProgress(
                        workDataOf(
                            "url" to url,
                            "progress" to it.progressKey(),
                            "progressAmount" to when (it) {
                                is DownloadAndInstallStatus.Downloading -> it.progress
                                else -> 0
                            },
                            "error" to when (it) {
                                is DownloadAndInstallStatus.Error -> it.message
                                else -> null
                            }
                        )
                    )
                }
                .collect()
        }

        val notificationManager = NotificationManagerCompat.from(applicationContext)
        result.exceptionOrNull()?.let {
            if (it is CancellationException) {
                notificationManager.cancel(NOTIFICATION_ID + url.hashCode())
                throw it
            }
        }

        result.onSuccess { delay(5000) }

        notificationManager.cancel(NOTIFICATION_ID + url.hashCode())

        return if (result.isSuccess && lastStatus !is DownloadAndInstallStatus.Error) Result.success() else Result.failure()
    }

    @SuppressLint("MissingPermission")
    private fun notify(
        logo: NotificationLogo,
        url: String,
        buildMore: NotificationCompat.Builder.() -> NotificationCompat.Builder,
    ) {
        if (
            PermissionChecker.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PermissionChecker.PERMISSION_GRANTED
        ) return

        // Create a notification builder
        val builder = NotificationCompat.Builder(
            applicationContext,
            NotificationChannels.Download.id
        )
            .setSmallIcon(logo.notificationId) // Set a small icon (replace with your actual icon)
            .setContentTitle("Downloading and installing...")
            .setPriority(NotificationCompat.PRIORITY_HIGH) // Set the priority
            .setOnlyAlertOnce(true) // Show the notification only once (update it)
            .setAutoCancel(true)
            .addAction(
                android.R.drawable.ic_delete,
                "Cancel",
                WorkManager
                    .getInstance(applicationContext)
                    .createCancelPendingIntent(id)
            )
            .buildMore()

        // Notify
        NotificationManagerCompat
            .from(applicationContext)
            .notify(NOTIFICATION_ID + url.hashCode(), builder.build())
    }

    companion object {
        private const val NOTIFICATION_ID = 40234

        fun downloadAndInstall(context: Context, url: String) {
            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    "downloadAndInstall",
                    ExistingWorkPolicy.APPEND_OR_REPLACE,
                    OneTimeWorkRequestBuilder<DownloadAndInstallWorker>()
                        .setInputData(workDataOf("url" to url))
                        .addTag("downloadAndInstall")
                        .keepResultsForAtLeast(10, TimeUnit.MINUTES)
                        .setConstraints(
                            Constraints.Builder()
                                .setRequiredNetworkType(NetworkType.CONNECTED)
                                .build()
                        )
                        .build()
                )
        }

        fun listToDownloads(context: Context) = WorkManager
            .getInstance(context)
            .getWorkInfosByTagFlow("downloadAndInstall")
            .map { list ->
                list.map {
                    DownloadAndInstallState(
                        url = it.progress.getString("url") ?: "",
                        name = it.progress.getString("url")?.toUri()?.lastPathSegment ?: "",
                        id = it.id.toString(),
                        status = if (it.state == WorkInfo.State.CANCELLED) {
                            DownloadAndInstallStatus.Error(InstallErrorReason.UNKNOWN, it.progress.getString("error") ?: "Unknown error")
                        } else {
                            decodeProgress(it.progress)
                        }
                    )
                }
            }
    }
}

//TODO: Try out and then clean up

class DownloadWorker(
    context: Context,
    workerParams: WorkerParameters,
    private val notificationLogo: NotificationLogo,
    private val downloadAndInstaller: DownloadAndInstaller,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val url = inputData.getString("url") ?: return Result.failure()

        logFirebaseMessage("Downloading and installing")

        //TODO: Need to fix so it doesn't start up again when the app starts up after an update

        return runCatching {
            downloadAndInstaller.download(
                url = url,
            )
                .onEach {
                    notify(notificationLogo, url) {
                        when (it) {
                            is DownloadAndInstallStatus.Error -> {
                                setContentText("Error during download and install.")
                                    .setProgress(0, 0, false)
                            }

                            is DownloadAndInstallStatus.Downloading -> {
                                setContentText("Downloading... ${(it.progress * 100).toInt()}%")
                                    .setProgress(100, (it.progress * 100).toInt(), false)
                            }

                            DownloadAndInstallStatus.Downloaded -> {
                                setContentText("Downloaded")
                                    .setProgress(0, 0, true)
                            }

                            DownloadAndInstallStatus.Installing -> {
                                setContentText("Installing...")
                                    .setProgress(100, 100, true)
                            }

                            DownloadAndInstallStatus.Installed -> {
                                setContentText("Download and install completed.")
                                    .setProgress(0, 0, false)
                                    .setTimeoutAfter(5000)
                            }

                            else -> {
                                setContentText("Status: $it")
                                    .setProgress(0, 0, false)
                            }
                        }
                    }

                    setProgress(
                        workDataOf(
                            "url" to url,
                            "progress" to it.progressKey(),
                            "progressAmount" to when (it) {
                                is DownloadAndInstallStatus.Downloading -> it.progress
                                else -> 0
                            },
                            "error" to when (it) {
                                is DownloadAndInstallStatus.Error -> it.message
                                else -> null
                            }
                        )
                    )
                }
                .collect()
        }
            .onSuccess {
                delay(5000)
            }
            .onFailure {
                NotificationManagerCompat
                    .from(applicationContext)
                    .cancel(NOTIFICATION_ID + url.hashCode())
            }
            .fold(
                onSuccess = {
                    Result.success(
                        workDataOf(
                            "url" to url,
                            "notification_id" to NOTIFICATION_ID + url.hashCode()
                        )
                    )
                },
                onFailure = { Result.failure() }
            )
    }

    @SuppressLint("MissingPermission")
    private fun notify(
        logo: NotificationLogo,
        url: String,
        buildMore: NotificationCompat.Builder.() -> NotificationCompat.Builder,
    ) {
        if (
            PermissionChecker.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PermissionChecker.PERMISSION_GRANTED
        ) return

        // Create a notification builder
        val builder = NotificationCompat.Builder(
            applicationContext,
            NotificationChannels.Download.id
        )
            .setSmallIcon(logo.notificationId) // Set a small icon (replace with your actual icon)
            .setContentTitle("Downloading and installing...")
            .setPriority(NotificationCompat.PRIORITY_HIGH) // Set the priority
            .setOnlyAlertOnce(true) // Show the notification only once (update it)
            .setAutoCancel(true)
            .addAction(
                android.R.drawable.ic_delete,
                "Cancel",
                WorkManager
                    .getInstance(applicationContext)
                    .createCancelPendingIntent(id)
            )
            .buildMore()

        // Notify
        NotificationManagerCompat
            .from(applicationContext)
            .notify(NOTIFICATION_ID + url.hashCode(), builder.build())
    }

    companion object {
        private const val NOTIFICATION_ID = 40234

        fun downloadThenInstall(context: Context, url: String) {
            WorkManager.getInstance(context)
                .beginWith(
                    //TODO: Work on this
                    OneTimeWorkRequestBuilder<DownloadAndInstallWorker>()
                        .setInputData(workDataOf("url" to url))
                        .addTag("downloadAndInstall")
                        .keepResultsForAtLeast(10, TimeUnit.MINUTES)
                        .setConstraints(
                            Constraints.Builder()
                                .setRequiredNetworkType(NetworkType.CONNECTED)
                                .build()
                        )
                        .build()
                )
                .then(
                    OneTimeWorkRequestBuilder<InstallWorker>()
                        .setInputData(workDataOf("url" to url))
                        .addTag("downloadAndInstall")
                        .build()
                )
                .enqueue()
        }

        fun listToDownloads(context: Context) = WorkManager
            .getInstance(context)
            .getWorkInfosByTagFlow("downloadAndInstall")
            .map { list ->
                list.map {
                    DownloadAndInstallState(
                        url = it.progress.getString("url") ?: "",
                        name = it.progress.getString("url")?.toUri()?.lastPathSegment ?: "",
                        id = it.id.toString(),
                        status = if (it.state == WorkInfo.State.CANCELLED) {
                            DownloadAndInstallStatus.Error(InstallErrorReason.UNKNOWN, it.progress.getString("error") ?: "Unknown error")
                        } else {
                            decodeProgress(it.progress)
                        }
                    )
                }
            }
    }
}

class InstallWorker(
    context: Context,
    workerParams: WorkerParameters,
    private val notificationLogo: NotificationLogo,
    private val downloadAndInstaller: DownloadAndInstaller,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val urlString = inputData.getString("url") ?: return Result.failure()
        val url = File(applicationContext.cacheDir, "${urlString.toUri().lastPathSegment}.apk")

        val notificationId = inputData.getInt("notification_id", NOTIFICATION_ID + urlString.hashCode())

        logFirebaseMessage("Downloading and installing")

        //TODO: Need to fix so it doesn't start up again when the app starts up after an update

        var lastStatus: DownloadAndInstallStatus? = null
        val result = runCatching {
            downloadAndInstaller.install(
                file = PlatformFile(url),
                confirmationType = ConfirmationType.DEFERRED
            )
                .onEach {
                    lastStatus = it
                    notify(notificationLogo, notificationId) {
                        when (it) {
                            is DownloadAndInstallStatus.Error -> {
                                setContentText("Error during download and install.")
                                    .setProgress(0, 0, false)
                            }

                            is DownloadAndInstallStatus.Downloading -> {
                                setContentText("Downloading... ${(it.progress * 100).toInt()}%")
                                    .setProgress(100, (it.progress * 100).toInt(), false)
                            }

                            DownloadAndInstallStatus.Downloaded -> {
                                setContentText("Downloaded")
                                    .setProgress(0, 0, true)
                            }

                            DownloadAndInstallStatus.Installing -> {
                                setContentText("Installing...")
                                    .setProgress(100, 100, true)
                            }

                            DownloadAndInstallStatus.Installed -> {
                                setContentText("Download and install completed.")
                                    .setProgress(0, 0, false)
                                    .setTimeoutAfter(5000)
                            }

                            else -> {
                                setContentText("Status: $it")
                                    .setProgress(0, 0, false)
                            }
                        }
                    }

                    setProgress(
                        workDataOf(
                            "url" to urlString,
                            "progress" to it.progressKey(),
                            "progressAmount" to when (it) {
                                is DownloadAndInstallStatus.Downloading -> it.progress
                                else -> 0
                            },
                            "error" to when (it) {
                                is DownloadAndInstallStatus.Error -> it.message
                                else -> null
                            }
                        )
                    )
                }
                .collect()
        }

        val notificationManager = NotificationManagerCompat.from(applicationContext)
        result.exceptionOrNull()?.let {
            if (it is CancellationException) {
                notificationManager.cancel(notificationId)
                throw it
            }
        }

        result.onSuccess { delay(5000) }

        notificationManager.cancel(notificationId)

        return if (result.isSuccess && lastStatus !is DownloadAndInstallStatus.Error) Result.success() else Result.failure()
    }

    @SuppressLint("MissingPermission")
    private fun notify(
        logo: NotificationLogo,
        url: Int,
        buildMore: NotificationCompat.Builder.() -> NotificationCompat.Builder,
    ) {
        if (
            PermissionChecker.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PermissionChecker.PERMISSION_GRANTED
        ) return

        // Create a notification builder
        val builder = NotificationCompat.Builder(
            applicationContext,
            NotificationChannels.Download.id
        )
            .setSmallIcon(logo.notificationId) // Set a small icon (replace with your actual icon)
            .setContentTitle("Downloading and installing...")
            .setPriority(NotificationCompat.PRIORITY_HIGH) // Set the priority
            .setOnlyAlertOnce(true) // Show the notification only once (update it)
            .setAutoCancel(true)
            .addAction(
                android.R.drawable.ic_delete,
                "Cancel",
                WorkManager
                    .getInstance(applicationContext)
                    .createCancelPendingIntent(id)
            )
            .buildMore()

        // Notify
        NotificationManagerCompat
            .from(applicationContext)
            .notify(url, builder.build())
    }

    companion object {
        private const val NOTIFICATION_ID = 40234
    }
}

// Stable keys so progress survives R8 renaming the status classes.
private fun DownloadAndInstallStatus.progressKey(): String = when (this) {
    is DownloadAndInstallStatus.Downloading -> "downloading"
    DownloadAndInstallStatus.Downloaded -> "downloaded"
    DownloadAndInstallStatus.Installing -> "installing"
    DownloadAndInstallStatus.PendingUserAction -> "pendingUserAction"
    DownloadAndInstallStatus.PermissionRequired -> "permissionRequired"
    DownloadAndInstallStatus.Installed -> "installed"
    DownloadAndInstallStatus.Cancelled -> "cancelled"
    is DownloadAndInstallStatus.Error -> "error"
}

private fun decodeProgress(progress: Data): DownloadAndInstallStatus = when (progress.getString("progress")) {
    "downloading" -> DownloadAndInstallStatus.Downloading(progress.getFloat("progressAmount", 0f))
    "downloaded" -> DownloadAndInstallStatus.Downloaded
    "installing" -> DownloadAndInstallStatus.Installing
    "pendingUserAction" -> DownloadAndInstallStatus.PendingUserAction
    "permissionRequired" -> DownloadAndInstallStatus.PermissionRequired
    "installed" -> DownloadAndInstallStatus.Installed
    "cancelled" -> DownloadAndInstallStatus.Cancelled
    else -> DownloadAndInstallStatus.Error(InstallErrorReason.UNKNOWN, progress.getString("error") ?: "Unknown error")
}
