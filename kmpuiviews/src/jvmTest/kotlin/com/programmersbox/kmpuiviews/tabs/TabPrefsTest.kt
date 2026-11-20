package com.programmersbox.kmpuiviews.tabs

import kotlin.test.Test
import kotlin.test.assertEquals

class TabPrefsTest {
    @Test
    fun unknownEnumNameFallsBackToDefault() {
        assertEquals(TabPosition.Top, TabPosition.fromName("garbage"))
        assertEquals(ClickOpens.SameTab, ClickOpens.fromName("garbage"))
    }

    @Test
    fun knownEnumNamesMap() {
        assertEquals(TabPosition.Side, TabPosition.fromName("Side"))
        assertEquals(ClickOpens.NewTab, ClickOpens.fromName("NewTab"))
    }
}
