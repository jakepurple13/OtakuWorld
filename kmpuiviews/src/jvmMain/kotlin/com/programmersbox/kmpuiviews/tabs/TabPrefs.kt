package com.programmersbox.kmpuiviews.tabs

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.programmersbox.datastore.DataStoreHandlerObject
import com.programmersbox.kmpuiviews.presentation.navactions.Navigation3Actions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module

/** Where the tab strip sits. */
enum class TabPosition {
    Top, Side;

    companion object {
        fun fromName(name: String?): TabPosition = entries.firstOrNull { it.name == name } ?: Top
    }
}

/** Whether a normal click on a link reuses the current tab or opens a new one. */
enum class ClickOpens {
    SameTab, NewTab;

    companion object {
        fun fromName(name: String?): ClickOpens = entries.firstOrNull { it.name == name } ?: SameTab
    }
}

val tabPositionPref = DataStoreHandlerObject(
    key = stringPreferencesKey("desktop_tab_position"),
    defaultValue = TabPosition.Top,
    mapToType = { TabPosition.fromName(it) },
    mapToKey = { it.name },
)

val tabClickOpensPref = DataStoreHandlerObject(
    key = stringPreferencesKey("desktop_tab_click_opens"),
    defaultValue = ClickOpens.SameTab,
    mapToType = { ClickOpens.fromName(it) },
    mapToKey = { it.name },
)

val tabsRestorePref = DataStoreHandlerObject(
    key = booleanPreferencesKey("desktop_tabs_restore"),
    defaultValue = true,
    mapToType = { it },
    mapToKey = { it },
)

val tabsMiniPlayerPref = DataStoreHandlerObject(
    key = booleanPreferencesKey("desktop_tabs_mini_player"),
    defaultValue = true,
    mapToType = { it },
    mapToKey = { it },
)

/**
 * Desktop tabs wiring. [TabsController] is created at start so it is already the Home tab's
 * navigation interceptor before the first navigation (the onboarding redirect).
 */
internal val desktopTabsModule: Module = module {
    single { CoreTabKeyProvider } bind TabKeyProvider::class
    single { TabKeys(getAll()) }
    single(createdAtStart = true) {
        TabsController(
            homeNav = get<Navigation3Actions>(),
            keys = get(),
            prefs = TabPrefsSnapshot(clickOpensNewTab = false),
        ).also { controller ->
            tabClickOpensPref.asFlow()
                .onEach { controller.prefs = TabPrefsSnapshot(clickOpensNewTab = it == ClickOpens.NewTab) }
                .launchIn(CoroutineScope(Dispatchers.Default + SupervisorJob()))
        }
    }
}
