package com.programmersbox.mangaworld

import android.os.Build
import android.view.WindowManager
import com.github.piasy.biv.BigImageViewer
import com.github.piasy.biv.loader.glide.GlideImageLoader
import com.google.android.gms.ads.MobileAds
import com.programmersbox.uiviews.BaseMainActivity

class MainActivity : BaseMainActivity() {
    override fun onCreate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        BigImageViewer.initialize(GlideImageLoader.with(applicationContext))
        MobileAds.initialize(this)
    }
}