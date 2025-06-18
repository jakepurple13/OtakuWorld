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
import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.kmpuiviews.repository.ChangingSettingsRepository
import com.programmersbox.kmpuiviews.repository.SetupRepository
import com.programmersbox.kmpuiviews.utils.ComposeSettingsDsl
import com.programmersbox.uiviews.presentation.navigation.HomeNav
import com.programmersbox.uiviews.utils.currentDetailsUrl
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.android.inject

abstract class BaseMainActivity : FragmentActivity() {

    protected val genericInfo: GenericInfo by inject()
    private val customPreferences = ComposeSettingsDsl()
        .apply(genericInfo.composeCustomPreferences())
    private val changingSettingsRepository: ChangingSettingsRepository by inject()
    private val dataStoreHandling: DataStoreHandling by inject()
    private val setupRepository by inject<SetupRepository>()

    protected abstract fun onCreate()

    @Composable
    protected open fun BottomBarAdditions() = Unit

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupRepository.setup(lifecycleScope)
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

    override fun onProvideAssistContent(outContent: AssistContent?) {
        super.onProvideAssistContent(outContent)
        outContent?.webUri = currentDetailsUrl.toUri()
    }
}