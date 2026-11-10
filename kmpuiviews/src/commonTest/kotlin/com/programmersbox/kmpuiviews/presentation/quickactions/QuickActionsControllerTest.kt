package com.programmersbox.kmpuiviews.presentation.quickactions

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class QuickActionsControllerTest {
    @Test
    fun openShortcutsClosesQuickActions() {
        val controller = QuickActionsController()
        controller.open(OpenOrigin.Keyboard)
        controller.openShortcuts()
        assertNull(controller.open.value)
        assertTrue(controller.shortcutsOpen.value)
    }

    @Test
    fun closeShortcuts() {
        val controller = QuickActionsController()
        controller.openShortcuts()
        controller.closeShortcuts()
        assertFalse(controller.shortcutsOpen.value)
    }
}
