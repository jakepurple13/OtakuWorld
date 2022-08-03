package com.programmersbox.animeworldtv

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.programmersbox.anime_sources.Sources
import com.programmersbox.models.sourceFlow
import com.programmersbox.sharedutils.AppUpdate
import com.programmersbox.sharedutils.updateAppCheck
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Loads [MainFragment].
 */
class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        lifecycleScope.launch {
            if (currentService == null) {
                val s = Sources.values().filterNot(Sources::notWorking).random()
                sourceFlow.emit(s)
                currentService = s.serviceName
            } else if (currentService != null) {
                try {
                    Sources.valueOf(currentService!!)
                } catch (e: IllegalArgumentException) {
                    null
                }?.let { sourceFlow.emit(it) }
            }
        }

        lifecycleScope.launch {
            flow { emit(AppUpdate.getUpdate()) }
                .catch { emit(null) }
                .flowOn(Dispatchers.IO)
                .onEach(updateAppCheck::emit)
                .collect()
        }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.main_browse_fragment, MainFragment())
                .commitNow()
        }
    }
}