package com.programmersbox.animeworld

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.viewinterop.AndroidViewBinding
import androidx.lifecycle.lifecycleScope
import com.programmersbox.animeworld.cast.CastHelper
import com.programmersbox.animeworld.databinding.MiniControllerBinding
import com.programmersbox.anime.shared.videos.VideoViewerRoute
import com.programmersbox.helpfulutils.requestPermissions
import com.programmersbox.uiviews.BaseMainActivity
import com.programmersbox.uiviews.datastore.updatePref
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.android.ext.android.inject

class MainActivity : BaseMainActivity() {

    val animeDataStoreHandling by inject<AnimeDataStoreHandling>()

    companion object {
        val VIEW_VIDEOS = "animeworld://$VideoViewerRoute"
        val cast: CastHelper = CastHelper()
    }

    override fun onCreate() {
        try {
            cast.init(this)
        } catch (e: Exception) {

        }

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
    }

    @Composable
    override fun BottomBarAdditions() {
        //TODO: get it working with the minicontroller fragment in play
        if (cast.isCastActive()) AndroidViewBinding(MiniControllerBinding::inflate)
    }

}
