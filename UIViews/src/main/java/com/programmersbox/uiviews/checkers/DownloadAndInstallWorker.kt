package com.programmersbox.uiviews.checkers

import android.Manifest
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.PermissionChecker
import androidx.core.net.toUri
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.programmersbox.uiviews.presentation.settings.downloadstate.DownloadAndInstallStatus
import com.programmersbox.uiviews.presentation.settings.downloadstate.DownloadAndInstaller
import com.programmersbox.uiviews.utils.NotificationLogo
import com.programmersbox.uiviews.utils.logFirebaseMessage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import ru.solrudev.ackpine.session.parameters.Confirmation
import java.util.UUID
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

        runCatching {
            downloadAndInstaller.downloadAndInstall(
                url = url,
                confirmationType = Confirmation.DEFERRED
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
                        }
                    }

                    setProgress(
                        workDataOf(
                            "url" to url,
                            "progress" to it.javaClass.name,
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
            .onSuccess { delay(5000) }

        NotificationManagerCompat
            .from(applicationContext)
            .cancel(NOTIFICATION_ID + url.hashCode())

        return Result.success()
    }

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
        val builder = NotificationCompat.Builder(applicationContext, "download_channel")
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
                .enqueue(
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
                        id = it.id,
                        status = if (it.state == WorkInfo.State.CANCELLED) {
                            DownloadAndInstallStatus.Error(it.progress.getString("error") ?: "Unknown error")
                        } else {
                            when (it.progress.getString("progress")) {
                                "com.programmersbox.uiviews.presentation.settings.downloadstate.DownloadAndInstallStatus\$Downloading" ->
                                    DownloadAndInstallStatus.Downloading(it.progress.getFloat("progressAmount", 0f))

                                "com.programmersbox.uiviews.presentation.settings.downloadstate.DownloadAndInstallStatus\$Downloaded" ->
                                    DownloadAndInstallStatus.Downloaded

                                "com.programmersbox.uiviews.presentation.settings.downloadstate.DownloadAndInstallStatus\$Installing" ->
                                    DownloadAndInstallStatus.Installing

                                "com.programmersbox.uiviews.presentation.settings.downloadstate.DownloadAndInstallStatus\$Installed" ->
                                    DownloadAndInstallStatus.Installed

                                else -> DownloadAndInstallStatus.Error(it.progress.getString("error") ?: "Unknown error")
                            }
                        }
                    )
                }
            }
    }
}

data class DownloadAndInstallState(
    val url: String,
    val name: String = url.toUri().lastPathSegment ?: "",
    val id: UUID,
    val status: DownloadAndInstallStatus,
)