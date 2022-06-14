package com.programmersbox.mangaworld

import android.view.WindowManager
import com.github.piasy.biv.BigImageViewer
import com.github.piasy.biv.loader.glide.GlideImageLoader
import com.google.android.gms.ads.MobileAds
import com.programmersbox.manga_sources.Sources
import com.programmersbox.models.sourcePublish
import com.programmersbox.uiviews.BaseMainActivity
import com.programmersbox.uiviews.utils.currentService

class MainActivity : BaseMainActivity() {

    override fun onCreate() {

        window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES

        BigImageViewer.initialize(GlideImageLoader.with(applicationContext))

        if (currentService == null) {
            sourcePublish.onNext(Sources.NINE_ANIME)
            currentService = Sources.NINE_ANIME.serviceName
        }

        MobileAds.initialize(this)
    }

}