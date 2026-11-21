package com.programmersbox.kmpuiviews.motion

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class WindowPictureTest {
    @Test
    fun captureWaitsForTheNextFrame() = runComposeUiTest {
        val picture = WindowPicture()
        var colour by mutableStateOf(Color.Red)
        lateinit var scope: CoroutineScope
        setContent {
            scope = rememberCoroutineScope()
            Box(Modifier.size(10.dp).recordInto(picture).background(colour))
        }
        waitForIdle()
        var image: ImageBitmap? = null
        // As a quit skipping the launch does: change what the content draws, then capture straight away.
        runOnUiThread {
            colour = Color.Blue
            scope.launch { image = picture.capture() }
        }
        waitUntil { image != null }
        val pixels = IntArray(1)
        image!!.readPixels(pixels, startX = 5, startY = 5, width = 1, height = 1)
        assertEquals(Color.Blue, Color(pixels[0]))
    }
}
