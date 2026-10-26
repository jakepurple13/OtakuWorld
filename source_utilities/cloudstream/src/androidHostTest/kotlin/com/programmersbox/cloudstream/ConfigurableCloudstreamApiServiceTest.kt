package com.programmersbox.cloudstream

import com.programmersbox.kmpmodels.KmpConfigurableApiService
import kotlin.test.Test
import kotlin.test.assertIs

class ConfigurableCloudstreamApiServiceTest {
    @Test
    fun isConfigurable() {
        assertIs<KmpConfigurableApiService>(
            ConfigurableCloudstreamApiService(FakeMainApi(), "fakeplugin", "cloudstream:Fake") {}
        )
    }
}
