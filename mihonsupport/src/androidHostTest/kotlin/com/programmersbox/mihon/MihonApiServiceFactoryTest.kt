package com.programmersbox.mihon

import com.programmersbox.kmpmodels.KmpConfigurableApiService
import kotlin.test.Test
import kotlin.test.assertIs

class MihonApiServiceFactoryTest {

    @Test
    fun configurableSourceGetsMarker() {
        val configurable = object : eu.kanade.tachiyomi.source.CatalogueSource by FakeCatalogueSource(),
            eu.kanade.tachiyomi.source.ConfigurableSource {
            override fun setupPreferenceScreen(screen: androidx.preference.PreferenceScreen) = Unit
        }
        assertIs<KmpConfigurableApiService>(mihonApiService(configurable))
    }
}
