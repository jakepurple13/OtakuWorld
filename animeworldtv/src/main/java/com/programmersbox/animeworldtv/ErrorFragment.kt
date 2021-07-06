package com.programmersbox.animeworldtv

import android.os.Bundle
import android.view.View

import androidx.core.content.ContextCompat

/**
 * This class demonstrates how to extend [androidx.leanback.app.ErrorSupportFragment].
 */
class ErrorFragment : androidx.leanback.app.ErrorSupportFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = resources.getString(R.string.app_name)
    }

    internal fun setErrorContent() {
        imageDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.lb_ic_sad_cloud)
        message = resources.getString(R.string.error_fragment_message)
        setDefaultBackground(TRANSLUCENT)

        buttonText = resources.getString(R.string.dismiss_error)
        buttonClickListener = View.OnClickListener {
            requireFragmentManager().beginTransaction().remove(this@ErrorFragment).commit()
        }
    }

    companion object {
        private val TRANSLUCENT = true
    }
}