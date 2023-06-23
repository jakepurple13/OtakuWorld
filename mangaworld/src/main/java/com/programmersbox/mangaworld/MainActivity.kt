package com.programmersbox.mangaworld

import android.os.Build
import android.view.WindowManager
import androidx.lifecycle.lifecycleScope
import com.github.piasy.biv.BigImageViewer
import com.github.piasy.biv.loader.glide.GlideImageLoader
import com.google.android.gms.ads.MobileAds
import com.programmersbox.manga_sources.Sources
import com.programmersbox.models.sourceFlow
import com.programmersbox.uiviews.BaseMainActivity
import com.programmersbox.uiviews.utils.currentService
import kotlinx.coroutines.launch

class MainActivity : BaseMainActivity() {

    override fun onCreate() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        BigImageViewer.initialize(GlideImageLoader.with(applicationContext))

        lifecycleScope.launch {
            if (currentService == null) {
                sourceFlow.emit(Sources.MANGA_READ)
                currentService = Sources.MANGA_READ.serviceName
            }
        }

        MobileAds.initialize(this)
    }

}