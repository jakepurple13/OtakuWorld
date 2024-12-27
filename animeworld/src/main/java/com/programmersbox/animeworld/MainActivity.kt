package com.programmersbox.animeworld

import androidx.compose.runtime.Composable
import androidx.compose.ui.viewinterop.AndroidViewBinding
import androidx.lifecycle.lifecycleScope
import com.programmersbox.animeworld.cast.CastHelper
import com.programmersbox.animeworld.databinding.MiniControllerBinding
import com.programmersbox.animeworld.videos.ViewVideoViewModel
import com.programmersbox.uiviews.BaseMainActivity
import com.programmersbox.uiviews.utils.datastore.updatePref
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.android.ext.android.inject

class MainActivity : BaseMainActivity() {

    val animeDataStoreHandling by inject<AnimeDataStoreHandling>()

    companion object {
        const val VIEW_VIDEOS = "animeworld://${ViewVideoViewModel.VideoViewerRoute}"
        val cast: CastHelper = CastHelper()
    }

    override fun onCreate() {
        try {
            cast.init(this)
        } catch (e: Exception) {

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
