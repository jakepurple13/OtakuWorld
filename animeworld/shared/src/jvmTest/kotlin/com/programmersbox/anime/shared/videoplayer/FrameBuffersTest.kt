package com.programmersbox.anime.shared.videoplayer

import androidx.compose.ui.graphics.asSkiaBitmap
import java.nio.ByteBuffer
import kotlin.test.Test
import kotlin.test.assertEquals

class FrameBuffersTest {

    /** A padded BGRA frame whose pixel (x, y) is blue = x + 1, green = y + 1, red = [tag]. */
    private fun frame(bufferWidth: Int, bufferHeight: Int, tag: Int): ByteBuffer {
        val buffer = ByteBuffer.allocateDirect(bufferWidth * bufferHeight * 4)
        for (y in 0 until bufferHeight) for (x in 0 until bufferWidth) {
            buffer.put((x + 1).toByte()).put((y + 1).toByte()).put(tag.toByte()).put(0xFF.toByte())
        }
        return buffer
    }

    private fun expected(x: Int, y: Int, tag: Int) = (0xFF shl 24) or (tag shl 16) or ((y + 1) shl 8) or (x + 1)

    private fun assertPixels(frames: FrameBuffers, bufferWidth: Int, tag: Int) {
        val image = frames.copy(frame(bufferWidth, 4, tag), rowBytes = bufferWidth * 4, width = 2, height = 3)
        assertEquals(2, image.width)
        assertEquals(3, image.height)
        val bitmap = image.asSkiaBitmap()
        for (y in 0 until 3) for (x in 0 until 2) {
            assertEquals(expected(x, y, tag), bitmap.getColor(x, y), "pixel ($x, $y) of frame $tag")
        }
    }

    @Test
    fun cropsPaddedFrame() {
        assertPixels(FrameBuffers(count = 3), bufferWidth = 4, tag = 1)
    }

    @Test
    fun newStrideAtSameCropSizeReallocates() {
        val frames = FrameBuffers(count = 1)
        assertPixels(frames, bufferWidth = 4, tag = 1)
        assertPixels(frames, bufferWidth = 6, tag = 2) // wider padding, same 2x3 crop
        assertPixels(frames, bufferWidth = 3, tag = 3) // narrower padding
    }
}
