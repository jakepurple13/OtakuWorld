package com.programmersbox.mihon

import com.programmersbox.kmpmodels.KmpConfigurableApiService
import kotlin.test.Test
import kotlin.test.assertFalse

class MihonApiServiceFactoryJvmTest {

    @Test
    fun configurableSourceIsNotMarkedOnDesktop() {
        val configurable = object : eu.kanade.tachiyomi.source.CatalogueSource by FakeCatalogueSource(),
            eu.kanade.tachiyomi.source.ConfigurableSource {
            override fun setupPreferenceScreen(screen: androidx.preference.PreferenceScreen) = Unit
        }
        assertFalse(mihonApiService(configurable) is KmpConfigurableApiService)
    }
}
