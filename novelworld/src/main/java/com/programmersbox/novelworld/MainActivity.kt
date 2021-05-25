package com.programmersbox.novelworld

import com.programmersbox.models.sourcePublish
import com.programmersbox.novel_sources.Sources
import com.programmersbox.uiviews.BaseMainActivity
import com.programmersbox.uiviews.utils.currentService

class MainActivity : BaseMainActivity() {
    override fun onCreate() {

        if (currentService == null) {
            val s = Sources.values().random()

            sourcePublish.onNext(s)
            currentService = s.serviceName
        }

    }

}