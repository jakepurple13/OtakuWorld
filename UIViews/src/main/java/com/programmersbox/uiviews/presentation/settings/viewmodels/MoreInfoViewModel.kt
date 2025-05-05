package com.programmersbox.uiviews.presentation.settings.viewmodels

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.kmpuiviews.domain.AppUpdate
import com.programmersbox.kmpuiviews.domain.AppUpdateCheck
import com.programmersbox.uiviews.GenericInfo
import com.programmersbox.uiviews.presentation.settings.downloadstate.DownloadAndInstaller
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

class MoreInfoViewModel(
    val downloadAndInstaller: DownloadAndInstaller,
    private val genericInfo: GenericInfo,
    private val appUpdateCheck: AppUpdateCheck,
) : ViewModel() {

    private val checker = AtomicBoolean(false)

    fun update(
        a: AppUpdate.AppUpdates,
    ) {
        viewModelScope.launch {
            val url = a.downloadUrl(genericInfo.apkString)

            downloadAndInstaller
                .downloadAndInstall(
                    url = url,
                    destinationPath = url.split("/").lastOrNull() ?: "update_apk"
                )
                .launchIn(viewModelScope)
        }
    }

    suspend fun updateChecker(context: Context) {
        try {
            if (!checker.get()) {
                checker.set(true)
                AppUpdate.getUpdate()?.let(appUpdateCheck.updateAppCheck::tryEmit)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            checker.set(false)
            withContext(Dispatchers.Main) { context.let { c -> Toast.makeText(c, "Done Checking", Toast.LENGTH_SHORT).show() } }
        }
    }
}