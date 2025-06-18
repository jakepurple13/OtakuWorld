package com.programmersbox.kmpuiviews.presentation.settings.moreinfo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.kmpuiviews.KmpGenericInfo
import com.programmersbox.kmpuiviews.domain.AppUpdate
import com.programmersbox.kmpuiviews.domain.AppUpdateCheck
import com.programmersbox.kmpuiviews.utils.DownloadAndInstaller
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi

class MoreInfoViewModel(
    val downloadAndInstaller: DownloadAndInstaller,
    private val genericInfo: KmpGenericInfo,
    private val appUpdateCheck: AppUpdateCheck,
) : ViewModel() {

    @OptIn(ExperimentalAtomicApi::class)
    private val checker = AtomicBoolean(false)

    fun update(a: AppUpdate.AppUpdates) {
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

    @OptIn(ExperimentalAtomicApi::class)
    suspend fun updateChecker() {
        try {
            if (!checker.load()) {
                checker.store(true)
                AppUpdate.getUpdate()?.let(appUpdateCheck.updateAppCheck::tryEmit)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            checker.store(false)
        }
    }
}