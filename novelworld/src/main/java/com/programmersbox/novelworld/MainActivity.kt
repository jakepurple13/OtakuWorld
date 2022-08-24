package com.programmersbox.novelworld

import androidx.lifecycle.lifecycleScope
import com.programmersbox.models.sourceFlow
import com.programmersbox.novel_sources.Sources
import com.programmersbox.uiviews.BaseMainActivity
import com.programmersbox.uiviews.utils.currentService
import kotlinx.coroutines.launch

class MainActivity : BaseMainActivity() {
    override fun onCreate() {

        lifecycleScope.launch {
            if (currentService == null) {
                val s = Sources.values().random()
                sourceFlow.emit(s)
                currentService = s.serviceName
            }
        }
    }
}