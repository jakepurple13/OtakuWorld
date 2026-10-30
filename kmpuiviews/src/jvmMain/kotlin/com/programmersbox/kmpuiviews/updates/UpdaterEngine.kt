package com.programmersbox.kmpuiviews.updates

import dev.nucleusframework.updater.DownloadProgress
import dev.nucleusframework.updater.NucleusUpdater
import dev.nucleusframework.updater.UpdateEvent
import dev.nucleusframework.updater.UpdateInfo
import dev.nucleusframework.updater.UpdateResult
import dev.nucleusframework.updater.UpdaterConfig
import kotlinx.coroutines.flow.Flow
import java.io.File

/** Seam over [NucleusUpdater] so [DesktopUpdater] can be tested without network or installers. */
interface UpdaterEngine {
    val currentVersion: String
    val track: UpdateTrack
    fun isSupported(): Boolean
    suspend fun check(): UpdateResult
    fun download(info: UpdateInfo): Flow<DownloadProgress>
    fun installAndRestart(file: File)
    fun consumeUpdateEvent(): UpdateEvent?

    /** Web page of the release the last [check] resolved, if any. */
    val foundReleasePage: String?
}

class NucleusUpdaterEngine(channel: String) : UpdaterEngine {
    override val currentVersion: String = UpdaterConfig().currentVersion
    override val track: UpdateTrack = UpdateTrack.fromVersion(currentVersion)

    private val releaseProvider = OtakuReleaseProvider(track)
    private val updater = NucleusUpdater {
        provider = releaseProvider
        this.channel = channel
    }

    override fun isSupported() = updater.isUpdateSupported()
    override suspend fun check() = updater.checkForUpdates()
    override fun download(info: UpdateInfo) = updater.downloadUpdate(info)
    override fun installAndRestart(file: File) = updater.installAndRestart(file)
    override fun consumeUpdateEvent() = updater.consumeUpdateEvent()
    override val foundReleasePage: String? get() = releaseProvider.resolvedRelease?.htmlUrl
}
