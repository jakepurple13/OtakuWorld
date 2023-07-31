package com.programmersbox.animeworldtv

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.programmersbox.anime_sources.Sources
import com.programmersbox.animeworldtv.compose.HomeScreen
import com.programmersbox.sharedutils.AppUpdate
import com.programmersbox.sharedutils.updateAppCheck
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * Loads [MainFragment].
 */
class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            if (currentService == null) {
                val s = Sources.values().filterNot(Sources::notWorking).random()
                //sourceFlow.emit(s)
                currentService = s.serviceName
            } else if (currentService != null) {
                /*try {
                    Sources.valueOf(currentService!!)
                } catch (e: IllegalArgumentException) {
                    null
                }?.let { sourceFlow.emit(it) }*/
            }
        }

        lifecycleScope.launch {
            flow { emit(AppUpdate.getUpdate()) }
                .catch { emit(null) }
                .flowOn(Dispatchers.IO)
                .onEach(updateAppCheck::emit)
                .collect()
        }
        if (true) {
            setContentView(R.layout.activity_main)
            if (savedInstanceState == null) {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.main_browse_fragment, MainFragment())
                    .commitNow()
            }
        } else {
            setContent {
                HomeScreen()
            }
        }

    }
}