package com.programmersbox.kmpuiviews.workers

import android.app.NotificationManager
import android.content.Context
import androidx.core.content.getSystemService
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.programmersbox.kmpuiviews.appVersion
import com.programmersbox.kmpuiviews.domain.AppUpdate
import com.programmersbox.kmpuiviews.logFirebaseMessage
import com.programmersbox.kmpuiviews.recordFirebaseException
import com.programmersbox.kmpuiviews.utils.NotificationChannels
import com.programmersbox.kmpuiviews.utils.NotificationDslBuilder
import com.programmersbox.kmpuiviews.utils.NotificationLogo
import kotlinx.coroutines.withTimeoutOrNull
import org.jetbrains.compose.resources.getString
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.theresAnUpdate
import otakuworld.kmpuiviews.generated.resources.versionAvailable

class AppCheckWorker(
    context: Context,
    workerParams: WorkerParameters,
    private val logo: NotificationLogo,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val f = withTimeoutOrNull(60000) { AppUpdate.getUpdate()?.updateRealVersion.orEmpty() }
            logFirebaseMessage("Current Version: $f")
            val appVersion = applicationContext.appVersion
            if (f != null && AppUpdate.checkForUpdate(appVersion, f)) {
                val n = NotificationDslBuilder.builder(
                    applicationContext,
                    NotificationChannels.AppUpdate.id,
                    logo.notificationId
                ) {
                    title = getString(Res.string.theresAnUpdate)
                    subText = getString(Res.string.versionAvailable, f)
                }
                applicationContext.getSystemService<NotificationManager>()?.notify(12, n)
            }
            Result.success()
        } catch (e: Exception) {
            recordFirebaseException(e)
            Result.success()
        }
    }
}