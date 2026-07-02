package com.programmersbox.kmpuiviews.repository

import kotlin.test.Test
import kotlin.test.assertFalse

class PlatformRepositoryTest {

    @Test fun `hasBiometric is false on jvm`() {
        assertFalse(PlatformRepository().hasBiometric())
    }
}
