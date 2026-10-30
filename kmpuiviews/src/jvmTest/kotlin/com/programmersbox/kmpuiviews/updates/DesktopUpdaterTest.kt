package com.programmersbox.kmpuiviews.updates

import dev.nucleusframework.updater.DownloadProgress
import dev.nucleusframework.updater.UpdateEvent
import dev.nucleusframework.updater.UpdateFile
import dev.nucleusframework.updater.UpdateInfo
import dev.nucleusframework.updater.UpdateLevel
import dev.nucleusframework.updater.UpdateResult
import dev.nucleusframework.updater.exception.NetworkException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import java.io.File
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private fun info(version: String) = UpdateInfo(
    version = version,
    releaseDate = "2026-10-30",
    files = emptyList(),
    currentFile = UpdateFile("https://example.com/f", "sha", 100, null, "f.dmg"),
)

private class FakeUpdaterEngine(
    var supported: Boolean = true,
    var result: UpdateResult = UpdateResult.NotAvailable,
    override val track: UpdateTrack = UpdateTrack.Stable,
    override var foundReleasePage: String? = null,
    var event: UpdateEvent? = null,
) : UpdaterEngine {
    override val currentVersion = "33.0.0"
    var checkCalls = 0
    val progress = MutableSharedFlow<DownloadProgress>()
    var installed: File? = null
    var installError: Exception? = null
    var downloadError: Exception? = null

    override fun isSupported() = supported
    override suspend fun check(): UpdateResult {
        checkCalls++
        return result
    }

    override fun download(info: UpdateInfo): Flow<DownloadProgress> =
        downloadError?.let { e -> flow { throw e } } ?: progress

    override fun installAndRestart(file: File) {
        installError?.let { throw it }
        installed = file
    }

    override fun consumeUpdateEvent(): UpdateEvent? = event.also { event = null }
}

private class FakeUpdatePrefs(
    var launch: Boolean = true,
    var skipped: String = "",
) : UpdatePrefs {
    override suspend fun checkOnLaunch() = launch
    override suspend fun skippedVersion() = skipped
    override suspend fun setSkippedVersion(version: String) {
        skipped = version
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class DesktopUpdaterTest {
    private fun TestScope.updater(engine: FakeUpdaterEngine, prefs: FakeUpdatePrefs = FakeUpdatePrefs()) =
        DesktopUpdater(engine, prefs, CoroutineScope(StandardTestDispatcher(testScheduler)))

    private fun available(v: String) = UpdateResult.Available(info(v), UpdateLevel.PATCH)

    @Test
    fun unsupportedEngineNeverChecks() = runTest {
        val engine = FakeUpdaterEngine(supported = false)
        val u = updater(engine)
        u.checkOnLaunch()
        u.checkNow()
        advanceUntilIdle()
        assertEquals(0, engine.checkCalls)
        assertEquals(DesktopUpdateState.Unsupported, u.state.value)
    }

    @Test
    fun launchCheckOffDoesNothing() = runTest {
        val engine = FakeUpdaterEngine()
        val u = updater(engine, FakeUpdatePrefs(launch = false))
        u.checkOnLaunch()
        advanceUntilIdle()
        assertEquals(0, engine.checkCalls)
        assertEquals(DesktopUpdateState.Idle, u.state.value)
    }

    @Test
    fun noReleaseForAppIsUpToDate() = runTest {
        val error = NetworkException(
            "Failed to check for updates",
            NoSuchElementException("No release with mangaworld-mac.yml"),
        )
        val u = updater(FakeUpdaterEngine(result = UpdateResult.Error(error)))
        u.checkOnLaunch()
        advanceUntilIdle()
        assertEquals(DesktopUpdateState.UpToDate, u.state.value)
        assertFalse(u.showLaunchDialog.value)
    }

    @Test
    fun errorShowsInnermostCauseMessage() = runTest {
        val error = NetworkException("Failed to check for updates", IOException("GitHub API returned 403"))
        val u = updater(FakeUpdaterEngine(result = UpdateResult.Error(error)))
        u.checkOnLaunch()
        advanceUntilIdle()
        assertEquals(DesktopUpdateState.Error("GitHub API returned 403"), u.state.value)
    }

    @Test
    fun launchCheckAvailableShowsDialog() = runTest {
        val u = updater(FakeUpdaterEngine(result = available("33.0.1")))
        u.checkOnLaunch()
        advanceUntilIdle()
        assertEquals(DesktopUpdateState.Available("33.0.1", UpdateLevel.PATCH), u.state.value)
        assertTrue(u.showLaunchDialog.value)
    }

    @Test
    fun launchCheckErrorShowsNoDialog() = runTest {
        val u = updater(FakeUpdaterEngine(result = UpdateResult.Error(NetworkException("offline"))))
        u.checkOnLaunch()
        advanceUntilIdle()
        assertEquals(DesktopUpdateState.Error("offline"), u.state.value)
        assertFalse(u.showLaunchDialog.value)
    }

    @Test
    fun skippedVersionOnlyHidesThatVersion() = runTest {
        val engine = FakeUpdaterEngine(result = available("33.0.1"))
        val u = updater(engine, FakeUpdatePrefs(skipped = "33.0.1"))
        u.checkOnLaunch()
        advanceUntilIdle()
        assertFalse(u.showLaunchDialog.value)
        engine.result = available("33.0.2")
        u.checkOnLaunch()
        advanceUntilIdle()
        assertTrue(u.showLaunchDialog.value)
    }

    @Test
    fun checkNowIgnoresSkipAndNeverOpensDialog() = runTest {
        val u = updater(FakeUpdaterEngine(result = available("33.0.1")), FakeUpdatePrefs(skipped = "33.0.1"))
        u.checkNow()
        advanceUntilIdle()
        assertEquals(DesktopUpdateState.Available("33.0.1", UpdateLevel.PATCH), u.state.value)
        assertFalse(u.showLaunchDialog.value)
    }

    @Test
    fun downloadProgressThenReady() = runTest {
        val engine = FakeUpdaterEngine(result = available("33.0.1"))
        val u = updater(engine)
        u.checkNow()
        advanceUntilIdle()
        u.download()
        advanceUntilIdle()
        engine.progress.emit(DownloadProgress(50, 100, 50.0))
        advanceUntilIdle()
        assertEquals(DesktopUpdateState.Downloading("33.0.1", 50f, 50, 100), u.state.value)
        engine.progress.emit(DownloadProgress(100, 100, 100.0, File("f.dmg")))
        advanceUntilIdle()
        assertEquals(DesktopUpdateState.Ready("33.0.1"), u.state.value)
    }

    @Test
    fun checkNowDuringDownloadIsIgnored() = runTest {
        val engine = FakeUpdaterEngine(result = available("33.0.1"))
        val u = updater(engine)
        u.checkNow()
        advanceUntilIdle()
        u.download()
        advanceUntilIdle()
        engine.progress.emit(DownloadProgress(50, 100, 50.0))
        advanceUntilIdle()
        u.checkNow()
        advanceUntilIdle()
        assertEquals(1, engine.checkCalls)
        assertTrue(u.state.value is DesktopUpdateState.Downloading)
    }

    @Test
    fun cancelDownloadReturnsToAvailable() = runTest {
        val engine = FakeUpdaterEngine(result = available("33.0.1"))
        val u = updater(engine)
        u.checkNow()
        advanceUntilIdle()
        u.download()
        advanceUntilIdle()
        u.cancelDownload()
        advanceUntilIdle()
        assertEquals(DesktopUpdateState.Available("33.0.1", UpdateLevel.PATCH), u.state.value)
    }

    @Test
    fun restartInstallsDownloadedFile() = runTest {
        val engine = FakeUpdaterEngine(result = available("33.0.1"))
        val u = updater(engine)
        u.checkNow()
        advanceUntilIdle()
        u.download()
        advanceUntilIdle()
        val file = File("f.dmg")
        engine.progress.emit(DownloadProgress(100, 100, 100.0, file))
        advanceUntilIdle()
        u.restart()
        assertEquals(file, engine.installed)
    }

    @Test
    fun skipThisVersionPersistsAndHidesDialog() = runTest {
        val prefs = FakeUpdatePrefs()
        val u = updater(FakeUpdaterEngine(result = available("33.0.1")), prefs)
        u.checkOnLaunch()
        advanceUntilIdle()
        assertTrue(u.showLaunchDialog.value)
        u.skipThisVersion()
        advanceUntilIdle()
        assertEquals("33.0.1", prefs.skipped)
        assertFalse(u.showLaunchDialog.value)
    }

    @Test
    fun justUpdatedConsumedOnce() = runTest {
        val u = updater(
            FakeUpdaterEngine(event = UpdateEvent("33.0.0", "33.0.1", UpdateLevel.PATCH))
        )
        assertEquals("33.0.1", u.consumeJustUpdated())
        assertNull(u.consumeJustUpdated())
    }

    @Test
    fun releasePageUrlPrefersFoundRelease() = runTest {
        val u = updater(FakeUpdaterEngine(foundReleasePage = "https://github.com/x/y/releases/tag/v1"))
        assertEquals("https://github.com/x/y/releases/tag/v1", u.releasePageUrl())
    }

    @Test
    fun releasePageUrlFallbackStable() = runTest {
        val u = updater(FakeUpdaterEngine(track = UpdateTrack.Stable))
        assertEquals("https://github.com/jakepurple13/OtakuWorld/releases/latest", u.releasePageUrl())
    }

    @Test
    fun releasePageUrlFallbackNightly() = runTest {
        val u = updater(FakeUpdaterEngine(track = UpdateTrack.Nightly))
        assertEquals("https://github.com/jakepurple13/OtakuWorld/releases", u.releasePageUrl())
    }

    @Test
    fun checkNowClearsLaunchDialog() = runTest {
        val engine = FakeUpdaterEngine(result = available("33.0.1"))
        val u = updater(engine)
        u.checkOnLaunch()
        advanceUntilIdle()
        assertTrue(u.showLaunchDialog.value)
        u.checkNow()
        assertFalse(u.showLaunchDialog.value)
        advanceUntilIdle()
        assertTrue(u.state.value is DesktopUpdateState.Available)
        assertFalse(u.showLaunchDialog.value)
    }

    @Test
    fun downloadFailureSetsError() = runTest {
        val engine = FakeUpdaterEngine(result = available("33.0.1"))
        val u = updater(engine)
        u.checkOnLaunch()
        advanceUntilIdle()
        engine.downloadError = NetworkException("offline")
        u.download()
        advanceUntilIdle()
        assertEquals(DesktopUpdateState.Error("offline"), u.state.value)
        assertTrue(u.showLaunchDialog.value)
    }

    @Test
    fun downloadIsNoOpOutsideAvailable() = runTest {
        val engine = FakeUpdaterEngine(result = available("33.0.1"))
        val u = updater(engine)
        u.download()
        advanceUntilIdle()
        assertEquals(DesktopUpdateState.Idle, u.state.value)
        u.checkNow()
        advanceUntilIdle()
        u.download()
        advanceUntilIdle()
        u.download()
        advanceUntilIdle()
        assertTrue(u.state.value is DesktopUpdateState.Downloading)
    }

    @Test
    fun hiddenDialogDownloadReopensAtReady() = runTest {
        val engine = FakeUpdaterEngine(result = available("33.0.1"))
        val u = updater(engine)
        u.checkOnLaunch()
        advanceUntilIdle()
        u.download()
        advanceUntilIdle()
        u.dismissLaunchDialog()
        assertFalse(u.showLaunchDialog.value)
        engine.progress.emit(DownloadProgress(100, 100, 100.0, File("f.dmg")))
        advanceUntilIdle()
        assertEquals(DesktopUpdateState.Ready("33.0.1"), u.state.value)
        assertTrue(u.showLaunchDialog.value)
    }

    @Test
    fun settingsDownloadDoesNotOpenDialogAtReady() = runTest {
        val engine = FakeUpdaterEngine(result = available("33.0.1"))
        val u = updater(engine)
        u.checkNow()
        advanceUntilIdle()
        u.download()
        advanceUntilIdle()
        engine.progress.emit(DownloadProgress(100, 100, 100.0, File("f.dmg")))
        advanceUntilIdle()
        assertEquals(DesktopUpdateState.Ready("33.0.1"), u.state.value)
        assertFalse(u.showLaunchDialog.value)
    }

    @Test
    fun restartFailureSetsError() = runTest {
        val engine = FakeUpdaterEngine(result = available("33.0.1"))
        val u = updater(engine)
        u.checkNow()
        advanceUntilIdle()
        u.download()
        advanceUntilIdle()
        engine.progress.emit(DownloadProgress(100, 100, 100.0, File("f.dmg")))
        advanceUntilIdle()
        engine.installError = IllegalStateException("boom")
        u.restart()
        assertEquals(
            DesktopUpdateState.Error("Couldn’t install the update. Use Download from GitHub to install it yourself."),
            u.state.value
        )
    }
}
