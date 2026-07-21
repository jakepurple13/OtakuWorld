package com.programmersbox.uiviews

import android.app.assist.AssistContent
import android.content.Intent
import android.net.Uri
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
import androidx.navigation3.runtime.deeplink.DeepLinkRequest
import com.programmersbox.datastore.DataStoreHandling
import com.programmersbox.jsextensionloader.ExtensionDiscovery
import com.programmersbox.jsextensionloader.JSExtensionLoader
import com.programmersbox.jsextensionloader.JsExtensionRepository
import com.programmersbox.kmpmodels.ExampleService
import com.programmersbox.kmpmodels.SourceRepository
import com.programmersbox.kmpuiviews.presentation.navactions.NavigationActions
import com.programmersbox.kmpuiviews.repository.JsExtensionSourceBridge
import com.programmersbox.kmpuiviews.repository.SetupRepository
import com.programmersbox.kmpuiviews.utils.ComposeSettingsDsl
import com.programmersbox.kmpuiviews.utils.DeepLinks
import com.programmersbox.sharedcomponents.repository.ChangingSettingsRepository
import com.programmersbox.uiviews.presentation.navigation.HomeNav
import com.programmersbox.uiviews.utils.currentDetailsUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

abstract class BaseMainActivity : FragmentActivity() {

    protected val genericInfo: GenericInfo by inject()
    private val navigationActions by inject<NavigationActions>()
    private val customPreferences by inject<ComposeSettingsDsl>()
    private val changingSettingsRepository: ChangingSettingsRepository by inject()
    private val dataStoreHandling: DataStoreHandling by inject()
    private val setupRepository by inject<SetupRepository>()
    private val sourceRepository by inject<SourceRepository>()
    private val jsExtensionLoader by inject<JSExtensionLoader>()
    private val jsExtensionRepository by inject<JsExtensionRepository>()
    private val jsExtensionDiscovery by inject<ExtensionDiscovery>()
    private val jsExtensionSourceBridge by inject<JsExtensionSourceBridge>()
    private val deepLinks by inject<DeepLinks>()

    protected abstract fun onCreate()

    @Composable
    protected open fun BottomBarAdditions() = Unit

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupRepository.setup(lifecycleScope)
        onCreate()

        enableEdgeToEdge()

        // Forces Koin to construct this lazy single now, starting its reactive
        // JsExtensionRepository -> SourceRepository mirroring for the process lifetime.
        // by inject<T>() only resolves on first access - it is never referenced elsewhere.
        jsExtensionSourceBridge.let { }

        if (BuildConfig.DEBUG) {
            sourceRepository.addSource(ExampleService.getSourceInformation())
            lifecycleScope.launch(Dispatchers.Default) {
                jsExtensionDiscovery.scanBundledResources().forEach { source ->
                    val extension = jsExtensionLoader.load(source.scriptText, source.fileName, source.companionManifestJson)
                    jsExtensionRepository.register(extension)
                }
            }
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

        intent.data?.let { handleDeepLink(it) }

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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.data?.let { handleDeepLink(it) }
    }

    private fun handleDeepLink(uri: Uri) {
        val request = DeepLinkRequest.fromUri(uri)

        println("[DEEP LINK]: $request")

        for (i in deepLinks.deepLinkMatching) {
            i.match(request)?.let { match ->
                println("[DEEP LINK]: $match")
                navigationActions.navigate(match.key)
                break
            }
        }
    }
}