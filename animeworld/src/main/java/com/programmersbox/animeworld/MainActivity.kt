package com.programmersbox.animeworld

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.programmersbox.anime.shared.cast.CastConnection
import com.programmersbox.anime.shared.cast.CastController
import com.programmersbox.anime.shared.cast.CastMiniBar
import com.programmersbox.anime.shared.cast.CastRemoteRoute
import com.programmersbox.anime.shared.videos.VideoViewerRoute
import com.programmersbox.helpfulutils.requestPermissions
import com.programmersbox.kmpuiviews.presentation.navactions.Navigation3Actions
import com.programmersbox.kmpuiviews.presentation.navactions.NavigationActions
import com.programmersbox.kmpuiviews.utils.LocalNavActions
import com.programmersbox.uiviews.BaseMainActivity
import com.programmersbox.uiviews.datastore.updatePref
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class MainActivity : BaseMainActivity() {

    val animeDataStoreHandling by inject<AnimeDataStoreHandling>()
    private val castController by inject<CastController>()
    private val navigationActions by inject<NavigationActions>()
    private var keyVolume: Float? = null

    companion object {
        val VIEW_VIDEOS = "animeworld://$VideoViewerRoute"
        val CAST_REMOTE = "animeworld://$CastRemoteRoute"
    }

    override fun onCreate() {
        // Android 17+ hides LAN devices (Chromecast/Google TV) from cast discovery without this
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CINNAMON_BUN &&
            checkSelfPermission(Manifest.permission.ACCESS_LOCAL_NETWORK) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(Manifest.permission.ACCESS_LOCAL_NETWORK) {}
        }

        animeDataStoreHandling
            .useNewPlayer
            .asFlow()
            .onEach { updatePref(USER_NEW_PLAYER, it) }
            .launchIn(lifecycleScope)

        animeDataStoreHandling
            .ignoreSsl
            .asFlow()
            .onEach { updatePref(IGNORE_SSL, it) }
            .launchIn(lifecycleScope)

        // The one collector of receiver playback errors; active whatever navigation style hosts the mini bar.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                castController.playbackErrors.collect {
                    Toast.makeText(this@MainActivity, it, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Fresh launches only, so a config change doesn't reopen a remote the user minimized.
        if (savedInstanceState == null) openCastRemoteIfRequested(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        openCastRemoteIfRequested(intent)
    }

    // BaseMainActivity's deep link matchers don't cover CastRemoteRoute, so it is handled here.
    private fun openCastRemoteIfRequested(intent: Intent) {
        if (intent.data?.toString() != CAST_REMOTE) return
        // Relaunching from recents replays the task's root intent; don't reopen the remote for it.
        if (intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY != 0) return
        navigationActions.remove(CastRemoteRoute)
        navigationActions.navigate(CastRemoteRoute)
    }

    /** While casting, the hardware volume keys drive the TV volume instead of the phone's. */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val isVolumeKey = event.keyCode == KeyEvent.KEYCODE_VOLUME_UP || event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
        val state = castController.state.value
        if (!isVolumeKey || state.connection != CastConnection.Connected) return super.dispatchKeyEvent(event)
        when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                val step = if (event.keyCode == KeyEvent.KEYCODE_VOLUME_UP) VOLUME_STEP else -VOLUME_STEP
                // Key repeats outpace receiver status updates, so build on the last value sent.
                val target = ((keyVolume ?: state.volume) + step).coerceIn(0f, 1f)
                keyVolume = target
                castController.setVolume(target)
            }

            KeyEvent.ACTION_UP -> keyVolume = null
        }
        // The matching ACTION_UP is consumed too, so the system doesn't also react to the key.
        return true
    }

    @Composable
    override fun BottomBarAdditions() {
        val navActions = LocalNavActions.current
        // The bottom bar renders on every screen, including the remote itself.
        val onRemote = (navActions as? Navigation3Actions)?.backStack?.lastOrNull() == CastRemoteRoute
        if (!onRemote) {
            CastMiniBar(
                onOpen = {
                    navActions.remove(CastRemoteRoute)
                    navActions.navigate(CastRemoteRoute)
                }
            )
        }
    }

}

private const val VOLUME_STEP = 0.05f
