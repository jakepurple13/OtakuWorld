package com.programmersbox.novelworld

import android.os.Build
import android.view.WindowManager
import com.programmersbox.uiviews.BaseMainActivity

class MainActivity : BaseMainActivity() {

    override fun onCreate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
    }
}