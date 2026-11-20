package com.programmersbox.kmpuiviews.presentation

import com.programmersbox.telemetry.events.OpenedFrom
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class ScreenOriginTest {
    @Test
    fun oldDetailsJsonDefaultsToOther() {
        val json = """{"title":"t","description":"d","url":"u","imageUrl":"i","source":"s"}"""
        assertEquals(OpenedFrom.OTHER, Json.decodeFromString<Screen.DetailsScreen.Details>(json).from)
    }

    @Test
    fun dailyOverviewDefaultsToOther() = assertEquals(OpenedFrom.OTHER, Screen.DailyOverview().from)

    @Test
    fun dailyOverviewJsonWithoutFromDefaultsToOther() {
        // A saved route written before `from` existed has no "from" key
        val json = Json.encodeToString(Screen.DailyOverview(OpenedFrom.QUICK_ACTION)).replace(""","from":"quick_action"""", "")
        assertEquals(OpenedFrom.OTHER, Json.decodeFromString<Screen.DailyOverview>(json).from)
    }
}
