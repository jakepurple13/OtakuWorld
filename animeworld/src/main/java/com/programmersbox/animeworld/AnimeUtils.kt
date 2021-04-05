package com.programmersbox.animeworld

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.FrameLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.programmersbox.helpfulutils.sharedPrefNotNullDelegate

var Context.folderLocation: String by sharedPrefNotNullDelegate(
    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).toString() + "/AnimeWorld/"
)

fun <T : View> BottomSheetBehavior<T>.open() {
    state = BottomSheetBehavior.STATE_EXPANDED
}

fun <T : View> BottomSheetBehavior<T>.close() {
    state = BottomSheetBehavior.STATE_COLLAPSED
}

fun <T : View> BottomSheetBehavior<T>.halfOpen() {
    state = BottomSheetBehavior.STATE_HALF_EXPANDED
}

abstract class BaseBottomSheetDialogFragment : BottomSheetDialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = super.onCreateDialog(savedInstanceState)
        .apply {
            setOnShowListener {
                val sheet = findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
                val bottomSheet = BottomSheetBehavior.from(sheet)
                bottomSheet.skipCollapsed = true
                bottomSheet.open()
            }
        }
}