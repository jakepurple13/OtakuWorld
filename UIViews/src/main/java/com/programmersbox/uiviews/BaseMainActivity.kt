package com.programmersbox.uiviews

import android.app.assist.AssistContent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.programmersbox.datastore.DataStoreHandling
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.favoritesdatabase.SourceOrder
import com.programmersbox.kmpmodels.SourceRepository
import com.programmersbox.kmpuiviews.domain.AppUpdate
import com.programmersbox.kmpuiviews.domain.AppUpdateCheck
import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.kmpuiviews.repository.ChangingSettingsRepository
import com.programmersbox.kmpuiviews.repository.CurrentSourceRepository
import com.programmersbox.kmpuiviews.utils.ComposeSettingsDsl
import com.programmersbox.uiviews.presentation.navigation.HomeNav
import com.programmersbox.uiviews.utils.currentDetailsUrl
import com.programmersbox.uiviews.utils.dispatchIo
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.android.inject

abstract class BaseMainActivity : FragmentActivity() {

    protected val genericInfo: GenericInfo by inject()
    private val customPreferences = ComposeSettingsDsl()
        .apply(genericInfo.composeCustomPreferences())
    private val changingSettingsRepository: ChangingSettingsRepository by inject()
    private val itemDao: ItemDao by inject()
    private val dataStoreHandling: DataStoreHandling by inject()
    private val appUpdateCheck: AppUpdateCheck by inject()
    private val sourceRepository by inject<SourceRepository>()
    private val currentSourceRepository by inject<CurrentSourceRepository>()

    protected abstract fun onCreate()

    @Composable
    protected open fun BottomBarAdditions() = Unit

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setup()
        onCreate()

        enableEdgeToEdge()

        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        changingSettingsRepository
            .showInsets
            .onEach {
                if (it) {
                    insetsController.show(WindowInsetsCompat.Type.systemBars())
                } else {
                    insetsController.hide(WindowInsetsCompat.Type.systemBars())
                }
            }
            .launchIn(lifecycleScope)

        val startDestination = if (runBlocking { dataStoreHandling.hasGoneThroughOnboarding.getOrNull() } == false) {
            Screen.OnboardingScreen
        } else {
            Screen.RecentScreen
        }

        setContent {
            HomeNav(
                activity = this,
                startDestination = startDestination,
                customPreferences = customPreferences,
                bottomBarAdditions = { BottomBarAdditions() }
            )
        }
    }

    private fun setup() {
        sourceRepository
            .sources
            .onEach {
                it.forEachIndexed { index, sourceInformation ->
                    itemDao.insertSourceOrder(
                        SourceOrder(
                            source = sourceInformation.packageName,
                            name = sourceInformation.apiService.serviceName,
                            order = index
                        )
                    )
                }
            }
            .launchIn(lifecycleScope)

        dataStoreHandling
            .currentService
            .asFlow()
            .mapNotNull {
                if (it == null) {
                    sourceRepository
                        .list
                        .filter { c -> c.catalog == null }
                        .randomOrNull()
                } else {
                    sourceRepository.toSourceByApiServiceName(it)
                }
            }
            .onEach { currentSourceRepository.emit(it.apiService) }
            .launchIn(lifecycleScope)

        flow { emit(AppUpdate.getUpdate()) }
            .catch { emit(null) }
            .dispatchIo()
            .onEach(appUpdateCheck.updateAppCheck::emit)
            .launchIn(lifecycleScope)
    }

    override fun onProvideAssistContent(outContent: AssistContent?) {
        super.onProvideAssistContent(outContent)
        outContent?.webUri = currentDetailsUrl.toUri()
    }
}