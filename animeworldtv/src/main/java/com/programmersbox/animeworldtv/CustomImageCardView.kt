package com.programmersbox.animeworldtv

import android.content.Context
import android.util.AttributeSet
import androidx.core.content.ContextCompat
import androidx.leanback.widget.ImageCardView

class CustomImageCardView : ImageCardView {

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private val sDefaultBackgroundColor by lazy { ContextCompat.getColor(context, R.color.default_background) }
    private val sSelectedBackgroundColor by lazy { ContextCompat.getColor(context, R.color.selected_background) }

    override fun setSelected(selected: Boolean) {
        updateCardBackgroundColor(selected)
        super.setSelected(selected)
    }

    fun updateCardBackgroundColor(selected: Boolean) {
        val color = if (selected) sSelectedBackgroundColor else sDefaultBackgroundColor
        // Both background colors should be set because the view"s background is temporarily visible
        // during animations.
        setBackgroundColor(color)
        setInfoAreaBackgroundColor(color)
    }
}