package com.programmersbox.manga.shared.reader

import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.programmersbox.manga.shared.reader.curl.ExperimentalPageCurlApi
import com.programmersbox.manga.shared.reader.curl.PageCurlConfig
import com.programmersbox.manga.shared.reader.curl.pageCurlConfigSaver
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalPageCurlApi::class)
class PageCurlConfigSaverTest {

    // Same rule as Android's Bundle: only primitives and Strings (lists of them via listSaver)
    private val bundleScope = object : SaverScope {
        override fun canBeSaved(value: Any): Boolean =
            value is String || value is Number || value is Boolean || value is List<*>
    }

    private fun config(drag: PageCurlConfig.DragInteraction) = PageCurlConfig(
        backPageColor = Color.White,
        backPageContentAlpha = 0.1f,
        shadowColor = Color.Black,
        shadowAlpha = 0.2f,
        shadowRadius = 15.dp,
        shadowOffset = DpOffset((-5).dp, 0.dp),
        dragForwardEnabled = true,
        dragBackwardEnabled = false,
        tapForwardEnabled = true,
        tapBackwardEnabled = false,
        tapCustomEnabled = true,
        dragInteraction = drag,
        tapInteraction = PageCurlConfig.TargetTapInteraction(),
        onCustomTap = { _, _ -> false },
    )

    private fun roundTrip(original: PageCurlConfig): PageCurlConfig {
        val saver = pageCurlConfigSaver { _, _ -> false }
        val saved = with(saver) { bundleScope.save(original) }!!
        (saved as List<*>).forEach { assertTrue(bundleScope.canBeSaved(it!!), "not bundle-safe: $it") }
        return saver.restore(saved)!!
    }

    @Test fun `start end drag config survives save and restore`() {
        val original = config(PageCurlConfig.StartEndDragInteraction())
        val restored = roundTrip(original)

        assertEquals(original.dragInteraction, restored.dragInteraction)
        assertEquals(original.tapInteraction, restored.tapInteraction)
        assertEquals(original.dragBackwardEnabled, restored.dragBackwardEnabled)
    }

    @Test fun `gesture drag config survives save and restore`() {
        val original = config(PageCurlConfig.GestureDragInteraction())
        val restored = roundTrip(original)

        assertEquals(original.dragInteraction, restored.dragInteraction)
    }
}
