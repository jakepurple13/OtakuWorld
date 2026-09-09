package com.programmersbox.kmpuiviews.screensaver

import android.service.dreams.DreamService
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.programmersbox.kmpuiviews.theme.OtakuMaterialTheme
import org.koin.compose.koinInject

class ScreensaverService : DreamService(),
    LifecycleOwner,
    SavedStateRegistryOwner,
    ViewModelStoreOwner {

    // 2. Add the ViewModelStore
    private val store = ViewModelStore()
    override val viewModelStore: ViewModelStore
        get() = store

    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle
        field = LifecycleRegistry(this)
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        isInteractive = false
        isFullscreen = true

        // 2. Mark the lifecycle as active
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        // 3. Manually create the ViewModel using a factory to bind it to the store

        // 3. Create the ComposeView
        val composeView = ComposeView(this).apply {
            // Ensure Compose cleans up when the screensaver stops
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                OtakuMaterialTheme(
                    settingsHandling = koinInject()
                ) {
                    ScreensaverScreen()
                }
            }
        }

        // 4. Bind the registries to the View tree so Compose can find them
        composeView.setViewTreeLifecycleOwner(this)
        composeView.setViewTreeSavedStateRegistryOwner(this)
        composeView.setViewTreeViewModelStoreOwner(this)
        //ViewTreeLifecycleOwner.set(composeView, this)
        //ViewTreeSavedStateRegistryOwner.set(composeView, this)

        setContentView(composeView)
    }

    override fun onDetachedFromWindow() {
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        super.onDetachedFromWindow()
    }

    override fun onDestroy() {
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)

        // 6. Clear the store to cancel active coroutines inside the ViewModel
        store.clear()

        super.onDestroy()
    }
}