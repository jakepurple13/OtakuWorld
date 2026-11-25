package com.programmersbox.kmpuiviews.motion

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

class TitleBarBandTest {
    @Test fun windowMinusContent() = assertEquals(40.dp, titleBarBand(640.dp, 600.dp))
    @Test fun neverNegative() = assertEquals(0.dp, titleBarBand(600.dp, 640.dp))
    @Test fun unspecifiedIsZero() {
        assertEquals(0.dp, titleBarBand(Dp.Unspecified, 600.dp))
        assertEquals(0.dp, titleBarBand(640.dp, Dp.Unspecified))
    }
}
