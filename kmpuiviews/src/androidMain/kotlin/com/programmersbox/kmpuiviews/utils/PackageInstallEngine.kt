package com.programmersbox.kmpuiviews.utils

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import com.programmersbox.kmpuiviews.receivers.PackageInstallReceiver
import java.io.File

class PackageInstallEngine(private val context: Context) {

    private val packageInstaller
        get() = context.packageManager.packageInstaller

    fun canRequestPackageInstalls(): Boolean =
        context.packageManager.canRequestPackageInstalls()

    fun commit(file: File): Int {
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
        params.setSize(file.length())
        val sessionId = packageInstaller.createSession(params)

        try {
            packageInstaller.openSession(sessionId).use { session ->
                session.openWrite(file.name, 0, file.length()).use { out ->
                    file.inputStream().use { input -> input.copyTo(out) }
                    session.fsync(out)
                }

                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    sessionId,
                    Intent(context, PackageInstallReceiver::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or
                        (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0)
                )

                session.commit(pendingIntent.intentSender)
            }
        } catch (t: Throwable) {
            abandon(sessionId)
            throw t
        }

        return sessionId
    }

    fun abandon(sessionId: Int) {
        runCatching { packageInstaller.abandonSession(sessionId) }
    }
}
