package com.programmersbox.uiviews.checkers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.programmersbox.helpfulutils.NotificationDslBuilder
import com.programmersbox.helpfulutils.notificationManager
import com.programmersbox.kmpuiviews.appVersion
import com.programmersbox.kmpuiviews.domain.AppUpdate
import com.programmersbox.uiviews.R
import com.programmersbox.kmpuiviews.utils.NotificationChannels
import com.programmersbox.uiviews.utils.NotificationLogo
import com.programmersbox.kmpuiviews.logFirebaseMessage
import com.programmersbox.kmpuiviews.recordFirebaseException
import kotlinx.coroutines.withTimeoutOrNull

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
                    title = applicationContext.getString(R.string.theresAnUpdate)
                    subText = applicationContext.getString(R.string.versionAvailable, f)
                }
                applicationContext.notificationManager.notify(12, n)
            }
            Result.success()
        } catch (e: Exception) {
            recordFirebaseException(e)
            Result.success()
        }
    }
}