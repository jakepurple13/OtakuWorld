package com.programmersbox.uiviews.utils

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.widget.GridLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Recycler
import com.programmersbox.uiviews.R
import kotlin.math.max


class AutoFitGridLayoutManager(context: Context?, columnWidth: Int) : GridLayoutManager(context, 1) {
    private var columnWidth = 0
    private var columnWidthChanged = true
    private fun setColumnWidth(newColumnWidth: Int) {
        if (newColumnWidth > 0 && newColumnWidth != columnWidth) {
            columnWidth = newColumnWidth
            columnWidthChanged = true
        }
    }

    override fun onLayoutChildren(recycler: Recycler, state: RecyclerView.State) {
        if (columnWidthChanged && columnWidth > 0) {
            val totalSpace: Int = if (orientation == LinearLayoutManager.VERTICAL) {
                width - paddingRight - paddingLeft
            } else {
                height - paddingTop - paddingBottom
            }
            val spanCount = max(1, totalSpace / columnWidth)
            setSpanCount(spanCount)
            columnWidthChanged = false
        }
        super.onLayoutChildren(recycler, state)
    }

    init {
        setColumnWidth(columnWidth)
    }
}

class AutoGridLayout : GridLayout {
    private var defaultColumnCount = 0
    private var columnWidth = 0

    constructor(context: Context?) : super(context) {
        init(null, 0)
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init(attrs, 0)
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(attrs, defStyleAttr)
    }

    private fun init(attrs: AttributeSet?, defStyleAttr: Int) {
        var a: TypedArray = context.obtainStyledAttributes(attrs, R.styleable.AutoGridLayout, 0, defStyleAttr)
        try {
            columnWidth = a.getDimensionPixelSize(R.styleable.AutoGridLayout_columnWidth, 0)
            val set = intArrayOf(R.attr.columnCount /* id 0 */)
            a = context.obtainStyledAttributes(attrs, set, 0, defStyleAttr)
            defaultColumnCount = a.getInt(0, 10)
        } finally {
            a.recycle()
        }

        /* Initially set columnCount to 1, will be changed automatically later. */columnCount = 1
    }

    override fun onMeasure(widthSpec: Int, heightSpec: Int) {
        super.onMeasure(widthSpec, heightSpec)
        val width = MeasureSpec.getSize(widthSpec)
        if (columnWidth > 0 && width > 0) {
            val totalSpace: Int = width - paddingRight - paddingLeft
            val columnCount = max(1, totalSpace / columnWidth)
            setColumnCount(columnCount)
        } else {
            columnCount = defaultColumnCount
        }
    }
}