package com.programmersbox.kmpuiviews.updates

import dev.nucleusframework.updater.UpdateInfo
import dev.nucleusframework.updater.UpdateLevel
import dev.nucleusframework.updater.UpdateResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

fun UpdateLevel.displayName(): String = when (this) {
    UpdateLevel.MAJOR -> "Major"
    UpdateLevel.MINOR -> "Minor"
    UpdateLevel.PATCH -> "Patch"
    UpdateLevel.PRE_RELEASE -> "Nightly"
}

sealed interface DesktopUpdateState {
    data object Idle : DesktopUpdateState
    data object Checking : DesktopUpdateState
    data object UpToDate : DesktopUpdateState
    data class Available(val version: String, val level: UpdateLevel) : DesktopUpdateState
    data class Downloading(val version: String, val percent: Float, val bytes: Long, val total: Long) :
        DesktopUpdateState

    data class Ready(val version: String) : DesktopUpdateState
    data class Error(val message: String) : DesktopUpdateState
    data object Unsupported : DesktopUpdateState
}

class DesktopUpdater(
    private val engine: UpdaterEngine,
    private val prefs: UpdatePrefs,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow<DesktopUpdateState>(
        if (engine.isSupported()) DesktopUpdateState.Idle else DesktopUpdateState.Unsupported
    )
    val state: StateFlow<DesktopUpdateState> = _state.asStateFlow()

    private val _showLaunchDialog = MutableStateFlow(false)
    val showLaunchDialog: StateFlow<Boolean> = _showLaunchDialog.asStateFlow()

    val currentVersion: String get() = engine.currentVersion
    val track: UpdateTrack get() = engine.track

    private var pendingInfo: UpdateInfo? = null
    private var pendingLevel: UpdateLevel = UpdateLevel.PATCH
    private var downloadedFile: File? = null
    private var downloadJob: Job? = null
    private var downloadFromDialog = false

    suspend fun checkOnLaunch() {
        if (!canCheck()) return
        if (!prefs.checkOnLaunch()) return
        runCheck(fromLaunch = true)
    }

    fun checkNow() {
        if (!canCheck()) return
        _showLaunchDialog.value = false
        downloadFromDialog = false
        _state.value = DesktopUpdateState.Checking
        scope.launch { runCheck(fromLaunch = false) }
    }

    private fun canCheck() = when (_state.value) {
        DesktopUpdateState.Unsupported,
        DesktopUpdateState.Checking,
        is DesktopUpdateState.Downloading,
        is DesktopUpdateState.Ready -> false

        else -> true
    }

    private suspend fun runCheck(fromLaunch: Boolean) {
        _state.value = DesktopUpdateState.Checking
        val result = try {
            engine.check()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _state.value = DesktopUpdateState.Error(e.message ?: NETWORK_MESSAGE)
            return
        }
        when (result) {
            is UpdateResult.Available -> {
                pendingInfo = result.info
                pendingLevel = result.level
                _state.value = DesktopUpdateState.Available(result.info.version, result.level)
                if (fromLaunch && result.info.version != prefs.skippedVersion()) {
                    _showLaunchDialog.value = true
                }
            }

            UpdateResult.NotAvailable -> _state.value = DesktopUpdateState.UpToDate
            is UpdateResult.Error -> {
                val causes = generateSequence<Throwable>(result.exception) { it.cause }
                _state.value = if (causes.any { it is NoSuchElementException }) {
                    // No release carries this app's update file yet (e.g. stable build before any stable desktop release).
                    DesktopUpdateState.UpToDate
                } else {
                    DesktopUpdateState.Error(causes.last().message?.takeIf { it.isNotBlank() } ?: NETWORK_MESSAGE)
                }
            }
        }
    }

    fun download() {
        val info = pendingInfo ?: return
        if (_state.value !is DesktopUpdateState.Available) return
        downloadFromDialog = _showLaunchDialog.value
        _state.value = DesktopUpdateState.Downloading(info.version, 0f, 0, 0)
        downloadJob = scope.launch {
            try {
                engine.download(info).collect { progress ->
                    val file = progress.file
                    if (file != null) {
                        downloadedFile = file
                        _state.value = DesktopUpdateState.Ready(info.version)
                        if (downloadFromDialog) _showLaunchDialog.value = true
                    } else {
                        _state.value = DesktopUpdateState.Downloading(
                            info.version,
                            progress.percent.toFloat(),
                            progress.bytesDownloaded,
                            progress.totalBytes,
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = DesktopUpdateState.Error(e.message ?: NETWORK_MESSAGE)
            }
        }
    }

    fun cancelDownload() {
        val info = pendingInfo ?: return
        if (_state.value !is DesktopUpdateState.Downloading) return
        downloadJob?.cancel()
        downloadJob = null
        downloadFromDialog = false
        _state.value = DesktopUpdateState.Available(info.version, pendingLevel)
    }

    fun restart() {
        val file = downloadedFile ?: return
        try {
            engine.installAndRestart(file)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _state.value = DesktopUpdateState.Error(INSTALL_MESSAGE)
        }
    }

    fun skipThisVersion() {
        val version = pendingInfo?.version ?: return
        _showLaunchDialog.value = false
        scope.launch { prefs.setSkippedVersion(version) }
    }

    fun dismissLaunchDialog() {
        _showLaunchDialog.value = false
    }

    fun consumeJustUpdated(): String? = engine.consumeUpdateEvent()?.newVersion

    fun releasePageUrl(): String = engine.foundReleasePage ?: when (engine.track) {
        UpdateTrack.Stable -> "https://github.com/jakepurple13/OtakuWorld/releases/latest"
        UpdateTrack.Nightly -> "https://github.com/jakepurple13/OtakuWorld/releases"
    }

    private companion object {
        const val INSTALL_MESSAGE =
            "Couldn’t install the update. Use Download from GitHub to install it yourself."
        const val NETWORK_MESSAGE = "Couldn’t reach GitHub. Check your connection and retry."
    }
}
