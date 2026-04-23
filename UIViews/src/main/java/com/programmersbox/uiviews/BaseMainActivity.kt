package com.programmersbox.uiviews

import android.app.assist.AssistContent
import android.os.Bundle
import androidx.activity.compose.ReportDrawn
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
import com.programmersbox.kmpmodels.ExampleService
import com.programmersbox.kmpmodels.SourceRepository
import com.programmersbox.kmpuiviews.presentation.navactions.NavigationActions
import com.programmersbox.kmpuiviews.repository.ChangingSettingsRepository
import com.programmersbox.kmpuiviews.repository.SetupRepository
import com.programmersbox.kmpuiviews.utils.ComposeSettingsDsl
import com.programmersbox.uiviews.presentation.navigation.HomeNav
import com.programmersbox.uiviews.utils.currentDetailsUrl
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

abstract class BaseMainActivity : FragmentActivity() {

    protected val genericInfo: GenericInfo by inject()
    private val navigationActions by inject<NavigationActions>()
    private val customPreferences = ComposeSettingsDsl()
        .apply(genericInfo.composeCustomPreferences())
    private val changingSettingsRepository: ChangingSettingsRepository by inject()
    private val dataStoreHandling: DataStoreHandling by inject()
    private val setupRepository by inject<SetupRepository>()
    private val sourceRepository by inject<SourceRepository>()

    protected abstract fun onCreate()

    @Composable
    protected open fun BottomBarAdditions() = Unit

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupRepository.setup(lifecycleScope)
        onCreate()

        enableEdgeToEdge()

        if (BuildConfig.DEBUG) {
            sourceRepository.addSource(ExampleService.getSourceInformation())
        }

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

        lifecycleScope.launch {
            if (dataStoreHandling.hasGoneThroughOnboarding.getOrNull() == false) {
                navigationActions.toOnboarding()
            }
        }

        setContent {
            HomeNav(
                activity = this,
                customPreferences = customPreferences,
                bottomBarAdditions = { BottomBarAdditions() }
            )
            ReportDrawn()
        }
    }

    override fun onProvideAssistContent(outContent: AssistContent?) {
        super.onProvideAssistContent(outContent)
        outContent?.webUri = currentDetailsUrl.toUri()
    }
}