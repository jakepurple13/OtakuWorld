package com.programmersbox.mangaworld

import android.view.WindowManager
import com.programmersbox.uiviews.BaseMainActivity

class MainActivity : BaseMainActivity() {
    override fun onCreate() {
        window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
    }
}