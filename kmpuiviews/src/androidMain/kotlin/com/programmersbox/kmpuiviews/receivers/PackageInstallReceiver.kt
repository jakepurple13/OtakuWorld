package com.programmersbox.kmpuiviews.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import com.programmersbox.kmpuiviews.repository.InstallStatusRepository
import com.programmersbox.kmpuiviews.utils.DownloadAndInstallStatus
import com.programmersbox.kmpuiviews.utils.InstallErrorReason
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class PackageInstallReceiver : BroadcastReceiver(), KoinComponent {

    private val installStatusRepository: InstallStatusRepository by inject()

    override fun onReceive(context: Context, intent: Intent) {
        val sessionId = intent.getIntExtra(PackageInstaller.EXTRA_SESSION_ID, -1)
        if (sessionId == -1) return

        when (intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                installStatusRepository.update(sessionId, DownloadAndInstallStatus.PendingUserAction)
                val confirmation = confirmationIntent(intent)
                val shown = confirmation != null && runCatching {
                    context.startActivity(confirmation.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }.isSuccess
                if (!shown) {
                    installStatusRepository.update(
                        sessionId,
                        DownloadAndInstallStatus.Error(InstallErrorReason.GENERIC, "Could not show install confirmation")
                    )
                    installStatusRepository.consumeTempFile(sessionId)
                }
            }

            PackageInstaller.STATUS_SUCCESS -> {
                installStatusRepository.update(sessionId, DownloadAndInstallStatus.Installed)
                installStatusRepository.consumeTempFile(sessionId)?.delete()
            }

            PackageInstaller.STATUS_FAILURE_ABORTED -> {
                installStatusRepository.update(sessionId, DownloadAndInstallStatus.Cancelled)
                installStatusRepository.consumeTempFile(sessionId)
            }

            PackageInstaller.STATUS_FAILURE_BLOCKED ->
                fail(sessionId, InstallErrorReason.BLOCKED, intent)

            PackageInstaller.STATUS_FAILURE_CONFLICT ->
                fail(sessionId, InstallErrorReason.CONFLICT, intent)

            PackageInstaller.STATUS_FAILURE_INCOMPATIBLE ->
                fail(sessionId, InstallErrorReason.INCOMPATIBLE, intent)

            PackageInstaller.STATUS_FAILURE_INVALID ->
                fail(sessionId, InstallErrorReason.INVALID, intent)

            PackageInstaller.STATUS_FAILURE_STORAGE ->
                fail(sessionId, InstallErrorReason.STORAGE, intent)

            else -> fail(sessionId, InstallErrorReason.GENERIC, intent)
        }
    }

    private fun fail(sessionId: Int, reason: InstallErrorReason, intent: Intent) {
        val detail = when (reason) {
            InstallErrorReason.BLOCKED ->
                intent.getStringExtra(PackageInstaller.EXTRA_OTHER_PACKAGE_NAME)?.let { "Blocked by $it" }

            InstallErrorReason.CONFLICT ->
                intent.getStringExtra(PackageInstaller.EXTRA_OTHER_PACKAGE_NAME)?.let { "Conflicts with $it" }

            InstallErrorReason.STORAGE ->
                intent.getStringExtra(PackageInstaller.EXTRA_STORAGE_PATH)?.let { "Not enough storage at $it" }

            else -> null
        } ?: intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE) ?: reason.name

        installStatusRepository.update(sessionId, DownloadAndInstallStatus.Error(reason, detail))
        installStatusRepository.consumeTempFile(sessionId)
    }

    @Suppress("DEPRECATION")
    private fun confirmationIntent(intent: Intent): Intent? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
        } else {
            intent.getParcelableExtra(Intent.EXTRA_INTENT)
        }
}
