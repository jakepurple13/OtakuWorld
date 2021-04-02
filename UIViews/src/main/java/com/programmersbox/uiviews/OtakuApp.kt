package com.programmersbox.uiviews

import android.app.Application
import android.os.Build
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.programmersbox.helpfulutils.NotificationChannelImportance
import com.programmersbox.helpfulutils.createNotificationChannel
import com.programmersbox.helpfulutils.createNotificationGroup
import com.programmersbox.loggingutils.Loged
import com.programmersbox.uiviews.utils.shouldCheck
import io.reactivex.plugins.RxJavaPlugins
import java.util.concurrent.TimeUnit

abstract class OtakuApp : Application() {

    override fun onCreate() {
        super.onCreate()

        Loged.FILTER_BY_PACKAGE_NAME = "programmersbox"
        Loged.TAG = this::class.java.simpleName

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel("otakuChannel", importance = NotificationChannelImportance.HIGH)
            createNotificationGroup("otakuGroup")
            createNotificationChannel("updateCheckChannel", importance = NotificationChannelImportance.MIN)
            createNotificationChannel("appUpdate", importance = NotificationChannelImportance.HIGH)
        }

        RxJavaPlugins.setErrorHandler {
            it.printStackTrace()
            //FirebaseCrashlytics.getInstance().recordException(it)
        }

        onCreated()

        val work = WorkManager.getInstance(this)
        //work.cancelAllWork()
        if (shouldCheck) {
            work.enqueueUniquePeriodicWork(
                "updateChecks",
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequest.Builder(UpdateWorker::class.java, 1, TimeUnit.HOURS)
                    //PeriodicWorkRequest.Builder(UpdateWorker::class.java, 15, TimeUnit.MINUTES)
                    .setConstraints(
                        Constraints.Builder()
                            .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                            .setRequiresBatteryNotLow(false)
                            .setRequiresCharging(false)
                            .setRequiresDeviceIdle(false)
                            .setRequiresStorageNotLow(false)
                            .build()
                    )
                    .setInitialDelay(10, TimeUnit.SECONDS)
                    .build()
            ).state.observeForever { println(it) }
        } else work.cancelAllWork()

    }

    abstract fun onCreated()

}