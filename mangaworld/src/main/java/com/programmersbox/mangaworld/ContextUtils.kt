package com.programmersbox.mangaworld

import android.content.Context
import android.util.AttributeSet
import android.view.View
import com.google.android.material.behavior.HideBottomViewOnScrollBehavior
import com.programmersbox.helpfulutils.sharedPrefNotNullDelegate

var Context.showAdult by sharedPrefNotNullDelegate(false)

class CustomHideBottomViewOnScrollBehavior<T : View>(context: Context?, attrs: AttributeSet?) :
    HideBottomViewOnScrollBehavior<T>(context, attrs) {

    var isShowing = true
        private set

    override fun slideDown(child: T) {
        super.slideDown(child)
        isShowing = false
    }

    override fun slideUp(child: T) {
        super.slideUp(child)
        isShowing = true
    }

}