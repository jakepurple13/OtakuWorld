package com.programmersbox.kmpuiviews.repository

import com.programmersbox.kmpuiviews.utils.DownloadAndInstallStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.update
import java.io.File

class InstallStatusRepository {
    private val statuses = MutableStateFlow<Map<Int, DownloadAndInstallStatus>>(emptyMap())
    private val tempFiles = mutableMapOf<Int, File>()

    fun flowFor(sessionId: Int): Flow<DownloadAndInstallStatus> =
        statuses.mapNotNull { it[sessionId] }.distinctUntilChanged()

    fun update(sessionId: Int, status: DownloadAndInstallStatus) {
        statuses.update { it + (sessionId to status) }
    }

    @Synchronized
    fun registerTempFile(sessionId: Int, file: File) {
        tempFiles[sessionId] = file
    }

    @Synchronized
    fun consumeTempFile(sessionId: Int): File? = tempFiles.remove(sessionId)

    fun clear(sessionId: Int) {
        statuses.update { it - sessionId }
    }
}
