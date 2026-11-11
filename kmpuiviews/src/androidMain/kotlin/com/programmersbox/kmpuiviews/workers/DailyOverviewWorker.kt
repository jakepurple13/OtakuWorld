package com.programmersbox.kmpuiviews.workers

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.programmersbox.kmpuiviews.PlatformGenericInfo
import com.programmersbox.kmpuiviews.domain.dailyoverview.DailyFactsBuilder
import com.programmersbox.kmpuiviews.domain.dailyoverview.DailyOverviewSettings
import com.programmersbox.kmpuiviews.domain.dailyoverview.EngineResolver
import com.programmersbox.kmpuiviews.domain.dailyoverview.TemplateEngine
import com.programmersbox.kmpuiviews.domain.dailyoverview.firstSentence
import com.programmersbox.kmpuiviews.domain.dailyoverview.generateDailySummary
import com.programmersbox.kmpuiviews.domain.dailyoverview.morningNotificationTitle
import com.programmersbox.kmpuiviews.recordFirebaseException
import com.programmersbox.kmpuiviews.utils.NotificationChannels
import com.programmersbox.kmpuiviews.utils.NotificationDslBuilder
import com.programmersbox.kmpuiviews.utils.NotificationLogo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import org.jetbrains.compose.resources.getString
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.do_notif_continue
import otakuworld.kmpuiviews.generated.resources.do_notif_open

/**
 * Runs about ten minutes before the chosen time: builds yesterday's facts, generates the summary
 * (the engine caches it for the overview screen) and posts the morning notification right away.
 */
class DailyOverviewWorker(
    context: Context,
    workerParams: WorkerParameters,
    private val factsBuilder: DailyFactsBuilder,
    private val resolver: EngineResolver,
    private val settings: DailyOverviewSettings,
    private val template: TemplateEngine,
    private val info: PlatformGenericInfo,
    private val logo: NotificationLogo,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val prefs = settings.prefs.first()
            if (!prefs.enabled || !prefs.notify) return Result.success()

            val facts = factsBuilder.yesterday()
            if (prefs.skipQuiet && facts.isQuiet) return Result.success()

            val summary = generateDailySummary(
                facts = facts,
                resolver = resolver,
                settings = settings,
                template = template,
                bypassCache = false,
            )
            if (!canPost()) return Result.success()

            val title = morningNotificationTitle(facts)
            val lastTitle = facts.titles.lastOrNull()
            val notification = NotificationDslBuilder.builder(
                applicationContext,
                NotificationChannels.DailyOverview.id,
                logo.notificationId
            ) {
                this.title = title
                message = firstSentence(summary.text)
                bigTextStyle {
                    contentTitle = title
                    bigText = summary.text
                }
                showWhen = true
                autoCancel = true
                pendingIntent { context -> info.deepLink(context, info.deepLinkDailyOverviewUri()) }
                addAction {
                    actionTitle = getString(Res.string.do_notif_open)
                    actionIcon = logo.notificationId
                    pendingActionIntent { context -> info.deepLink(context, info.deepLinkDailyOverviewUri()) }
                }
                if (lastTitle != null) addAction {
                    actionTitle = getString(Res.string.do_notif_continue, lastTitle.title)
                    actionIcon = logo.notificationId
                    pendingActionIntent { context ->
                        info.deepLink(
                            context,
                            info.deepLinkDetailsUri(url = lastTitle.url, imageUrl = lastTitle.imageUrl, source = lastTitle.source)
                        )
                    }
                }
            }
            post(notification)
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            recordFirebaseException(e)
            Result.success()
        }
    }

    private fun canPost(): Boolean =
        NotificationManagerCompat.from(applicationContext).areNotificationsEnabled() &&
            (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED)

    // canPost() checks POST_NOTIFICATIONS right before this.
    @SuppressLint("MissingPermission")
    private fun post(notification: Notification) =
        NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_ID, notification)

    companion object {
        const val WORK_NAME = "daily_overview"
        private const val NOTIFICATION_ID = 480_011
    }
}
