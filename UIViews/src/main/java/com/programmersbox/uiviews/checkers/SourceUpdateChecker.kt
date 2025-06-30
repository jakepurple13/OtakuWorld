package com.programmersbox.uiviews.checkers

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.programmersbox.helpfulutils.GroupBehavior
import com.programmersbox.helpfulutils.NotificationDslBuilder
import com.programmersbox.helpfulutils.notificationManager
import com.programmersbox.kmpextensionloader.SourceLoader
import com.programmersbox.kmpmodels.KmpExternalApiServicesCatalog
import com.programmersbox.kmpmodels.SourceRepository
import com.programmersbox.kmpuiviews.OtakuWorldCatalog
import com.programmersbox.kmpuiviews.domain.AppUpdate
import com.programmersbox.kmpuiviews.utils.NotificationChannels
import com.programmersbox.kmpuiviews.utils.NotificationGroups
import com.programmersbox.kmpuiviews.utils.NotificationLogo
import com.programmersbox.kmpuiviews.recordFirebaseException
import org.koin.core.component.KoinComponent

class SourceUpdateChecker(
    context: Context,
    workerParams: WorkerParameters,
    private val logo: NotificationLogo,
    private val sourceRepository: SourceRepository,
    private val sourceLoader: SourceLoader,
    private val otakuWorldCatalog: OtakuWorldCatalog,
) : CoroutineWorker(context, workerParams), KoinComponent {
    override suspend fun doWork(): Result {
        return try {
            val notificationManager = applicationContext.notificationManager
            val packageManager = applicationContext.packageManager
            if (sourceRepository.list.isEmpty()) {
                sourceLoader.blockingLoad()
            }
            val remoteSources = otakuWorldCatalog.getRemoteSources() +
                    sourceRepository.list
                        .filter { it.catalog is KmpExternalApiServicesCatalog }
                        .flatMap { (it.catalog as? KmpExternalApiServicesCatalog)?.getRemoteSources().orEmpty() }

            val updateList = sourceRepository.list
                .filter { l -> remoteSources.any { it.packageName == l.packageName } }
                .mapNotNull {
                    val localVersion = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        packageManager.getPackageInfo(it.packageName, PackageManager.PackageInfoFlags.of(0L))
                    } else {
                        packageManager.getPackageInfo(it.packageName, 0)
                    }
                        ?.versionName
                        .orEmpty()

                    remoteSources
                        .find { r -> r.packageName == it.packageName }
                        ?.let { r -> AppUpdate.checkForUpdate(localVersion, r.version) }
                        ?.let { r -> if (r) it else null }
                }

            updateList.forEach {
                val r = remoteSources.find { r -> r.packageName == it.packageName }!!
                val n = NotificationDslBuilder.builder(
                    applicationContext,
                    NotificationChannels.SourceUpdate.id,
                    logo.notificationId
                ) {
                    title = "${it.name} has an update!"
                    subText = "${r.version} is available."
                    groupId = NotificationGroups.Sources.id
                }
                notificationManager.notify(it.hashCode(), n)
            }

            if (updateList.isNotEmpty()) {
                notificationManager.notify(
                    15,
                    NotificationDslBuilder.builder(
                        applicationContext,
                        NotificationChannels.SourceUpdate.id,
                        logo.notificationId
                    ) {
                        title = "Sources have updates!"
                        subText = "Sources have updates!"
                        showWhen = true
                        groupSummary = true
                        groupAlertBehavior = GroupBehavior.ALL
                        groupId = NotificationGroups.Sources.id
                    }
                )
            }
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            recordFirebaseException(e)
            Result.success()
        }
    }
}