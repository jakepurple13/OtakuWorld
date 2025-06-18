package com.programmersbox.mangaworld

import android.view.WindowManager
import com.github.piasy.biv.BigImageViewer
import com.github.piasy.biv.loader.glide.GlideImageLoader
import com.programmersbox.uiviews.BaseMainActivity

class MainActivity : BaseMainActivity() {
    override fun onCreate() {
        window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        BigImageViewer.initialize(GlideImageLoader.with(applicationContext))
    }
}