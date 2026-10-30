package com.programmersbox.kmpuiviews.updates

import com.programmersbox.kmpuiviews.MangaDesktopSettings

interface UpdatePrefs {
    suspend fun checkOnLaunch(): Boolean
    suspend fun skippedVersion(): String
    suspend fun setSkippedVersion(version: String)
}

class DataStoreUpdatePrefs(private val settings: MangaDesktopSettings) : UpdatePrefs {
    override suspend fun checkOnLaunch() = settings.checkUpdatesOnLaunch.get()
    override suspend fun skippedVersion() = settings.skippedUpdateVersion.get()
    override suspend fun setSkippedVersion(version: String) = settings.skippedUpdateVersion.set(version)
}
