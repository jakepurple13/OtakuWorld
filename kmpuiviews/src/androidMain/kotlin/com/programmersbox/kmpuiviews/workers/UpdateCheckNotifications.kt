package com.programmersbox.kmpuiviews.workers

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.programmersbox.kmpuiviews.domain.updatecheck.UpdateCheckProgress
import com.programmersbox.kmpuiviews.domain.updatecheck.fallbackTitle
import com.programmersbox.kmpuiviews.domain.updatecheck.headline
import com.programmersbox.kmpuiviews.domain.updatecheck.segmentLayout
import com.programmersbox.kmpuiviews.domain.updatecheck.sourceLines
import com.programmersbox.kmpuiviews.domain.updatecheck.summary
import com.programmersbox.kmpuiviews.utils.NotificationChannels
import com.programmersbox.kmpuiviews.utils.NotificationDslBuilder
import com.programmersbox.kmpuiviews.utils.NotificationLogo
import org.jetbrains.compose.resources.getString
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.checking

/** Foreground notification id of the running check. WorkManager cancels it when the worker ends. */
const val UPDATE_CHECK_PROGRESS_ID = 13

/** Separate from [UPDATE_CHECK_PROGRESS_ID] so WorkManager's cleanup of the foreground id doesn't remove it. */
const val UPDATE_CHECK_FINISHED_ID = 14

private const val FAILED_SEGMENT_COLOR = 0xFFB3261E.toInt()
private const val UPDATE_POINT_COLOR = 0xFF2F7D4F.toInt()

/**
 * API 36+ shows one segment per source (red when it failed) and a point where a source found updates.
 * Older versions show one bar across all favorites, with a line per source when expanded.
 */
suspend fun buildCheckProgressNotification(
    context: Context,
    icon: NotificationLogo,
    progress: UpdateCheckProgress,
    cancelIntent: PendingIntent,
): Notification {
    val cancelTitle = context.getString(android.R.string.cancel)
    val checking = getString(Res.string.checking)

    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
        val layout = progress.segmentLayout()
        NotificationCompat.Builder(context, NotificationChannels.UpdateCheck.id)
            .setSmallIcon(icon.notificationId)
            .setContentTitle(progress.headline())
            .setContentText(progress.summary())
            .setSubText(checking)
            .setShowWhen(true)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setStyle(
                NotificationCompat.ProgressStyle()
                    .setProgressSegments(
                        layout.segments.map { (length, failed) ->
                            NotificationCompat.ProgressStyle.Segment(length)
                                .apply { if (failed) setColor(FAILED_SEGMENT_COLOR) }
                        }
                    )
                    .setProgressPoints(
                        layout.points.map { NotificationCompat.ProgressStyle.Point(it).setColor(UPDATE_POINT_COLOR) }
                    )
                    .setProgress(layout.progress)
            )
            .addAction(0, cancelTitle, cancelIntent)
            .build()
    } else {
        val title = progress.fallbackTitle()
        val lines = progress.sourceLines().joinToString("\n")
        val summary = progress.summary()
        // Read here: inside progress { } the name `progress` is the bar's own Int.
        val total = progress.total
        val checked = progress.checked
        val loadingRecents = progress.loadingRecents
        NotificationDslBuilder.builder(
            context,
            NotificationChannels.UpdateCheck.id,
            icon.notificationId
        ) {
            onlyAlertOnce = true
            ongoing = true
            showWhen = true
            this.title = title
            message = summary
            subText = checking
            progress {
                max = total
                this.progress = checked
                indeterminate = loadingRecents
            }
            bigTextStyle {
                // An empty big title or summary would blank the title and "Checking" header when expanded.
                contentTitle = title
                summaryText = checking
                bigText = lines
            }
            addAction {
                actionTitle = cancelTitle
                pendingActionIntent(cancelIntent)
            }
        }
    }
}
