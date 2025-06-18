package com.programmersbox.datastore

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class DataStoreHandling {

    val currentService = DataStoreHandlerNullable(
        key = stringPreferencesKey("currentService"),
    )

    val showBySource = DataStoreHandler(
        key = booleanPreferencesKey("showBySource"),
        defaultValue = false
    )

    val shouldCheck = DataStoreHandler(
        key = booleanPreferencesKey("shouldCheckUpdate"),
        defaultValue = true
    )

    val historySave = DataStoreHandler(
        key = intPreferencesKey("history_save"),
        defaultValue = 50
    )

    @OptIn(ExperimentalTime::class)
    val updateCheckingStart = DataStoreHandler(
        key = longPreferencesKey("lastUpdateCheckStart"),
        defaultValue = Clock.System.now().toEpochMilliseconds()
    )

    @OptIn(ExperimentalTime::class)
    val updateCheckingEnd = DataStoreHandler(
        key = longPreferencesKey("lastUpdateCheckEnd"),
        defaultValue = Clock.System.now().toEpochMilliseconds()
    )

    val floatingNavigation = DataStoreHandler(
        key = booleanPreferencesKey("floatingNavigation"),
        defaultValue = true
    )

    val updateHourCheck = DataStoreHandler(
        key = longPreferencesKey("updateHourCheck"),
        defaultValue = 1
    )

    val hasGoneThroughOnboarding = DataStoreHandler(
        key = booleanPreferencesKey("hasGoneThroughOnboarding"),
        defaultValue = false
    )

    val hasMigrated = DataStoreHandler(
        key = booleanPreferencesKey("hasMigrated"),
        defaultValue = false
    )

    val timeSpentDoing = DataStoreHandler(
        key = longPreferencesKey("timeSpentDoing"),
        defaultValue = 0
    )
}