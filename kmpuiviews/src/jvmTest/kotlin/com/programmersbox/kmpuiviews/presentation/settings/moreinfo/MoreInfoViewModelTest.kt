package com.programmersbox.kmpuiviews.presentation.settings.moreinfo

import androidx.lifecycle.ViewModelStore
import com.programmersbox.kmpuiviews.domain.AppUpdate
import com.programmersbox.kmpuiviews.domain.AppUpdateCheck
import com.programmersbox.kmpuiviews.testing.FakeKmpGenericInfo
import com.programmersbox.kmpuiviews.utils.DownloadAndInstaller
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class MoreInfoViewModelTest {

    private val viewModelStore = ViewModelStore()

    private fun viewModel(appUpdateCheck: AppUpdateCheck = AppUpdateCheck()) = MoreInfoViewModel(
        downloadAndInstaller = DownloadAndInstaller(),
        genericInfo = FakeKmpGenericInfo(),
        appUpdateCheck = appUpdateCheck,
    ).also { viewModelStore.put(System.identityHashCode(it).toString(), it) }

    @OptIn(ExperimentalCoroutinesApi::class)
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Default)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        viewModelStore.clear()
    }

    @Test fun `update does not throw when downloading and installing`() = runTest {
        val vm = viewModel()

        vm.update(
            AppUpdate.AppUpdates(
                updateVersion = 1.0,
                updateRealVersion = "1.0.0",
                updateUrl = "https://example.com/",
                mangaFile = "manga.apk",
                animeFile = null,
                novelFile = null,
                animetvFile = null,
                otakumanagerFile = null,
                mangaNoFirebaseFile = null,
                animeNoFirebaseFile = null,
                novelNoFirebaseFile = null,
                animetvNoFirebaseFile = null,
                mangaNoCloudFile = null,
                animeNoCloudFile = null,
                novelNoCloudFile = null,
                animetvNoCloudFile = null,
            )
        )
    }

    // AppUpdate.getUpdate() makes a real network call (not injectable), so these tests
    // don't assert on the resulting value — only that the call completes without throwing,
    // regardless of network availability or outcome in the sandbox.
    @Test fun `updateChecker completes without throwing`() = runTest {
        val vm = viewModel()

        withContext(Dispatchers.Default.limitedParallelism(1)) {
            withTimeout(30_000) { vm.updateChecker() }
        }
    }

    @Test fun `updateChecker can be called repeatedly without throwing`() = runTest {
        val vm = viewModel()

        withContext(Dispatchers.Default.limitedParallelism(1)) {
            withTimeout(30_000) {
                vm.updateChecker()
                vm.updateChecker()
            }
        }
    }
}
