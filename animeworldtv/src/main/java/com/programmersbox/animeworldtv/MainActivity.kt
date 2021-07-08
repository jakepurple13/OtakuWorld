package com.programmersbox.animeworldtv

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import com.programmersbox.anime_sources.Sources
import com.programmersbox.models.sourcePublish

/**
 * Loads [MainFragment].
 */
class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (currentService == null) {
            val s = Sources.values().random()
            sourcePublish.onNext(s)
            currentService = s.serviceName
        } else if (currentService != null) {
            try {
                Sources.valueOf(currentService!!)
            } catch (e: IllegalArgumentException) {
                null
            }?.let(sourcePublish::onNext)
        }

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                .replace(R.id.main_browse_fragment, MainFragment())
                .commitNow()
        }
    }
}