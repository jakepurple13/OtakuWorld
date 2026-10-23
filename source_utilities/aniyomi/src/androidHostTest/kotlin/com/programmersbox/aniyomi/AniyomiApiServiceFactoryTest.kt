package com.programmersbox.aniyomi

import com.programmersbox.kmpmodels.KmpConfigurableApiService
import kotlin.test.Test
import kotlin.test.assertIs

class AniyomiApiServiceFactoryTest {

    @Test
    fun configurableSourceGetsMarker() {
        assertIs<KmpConfigurableApiService>(aniyomiApiService(FakeConfigurableAnimeSource(), 16.0))
    }
}
