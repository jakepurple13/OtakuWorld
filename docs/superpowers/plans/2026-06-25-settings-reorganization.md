# Settings Reorganization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (
> recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use
> checkbox (`- [ ]`) syntax for tracking.

**Goal:** Reorganize `:kmpuiviews` settings screens into a deeply nested hierarchy with cross-tree
search powered by `AppBarWithSearch`, and extend `ComposeSettingsDsl` so each app can inject custom
items into any section.

**Architecture:** Fourteen new `Screen.Settings.*` NavKeys gate fourteen new hub/sub-screens. A Koin
singleton `SettingsSearchRegistry` indexes all settings items (built-in + app-provided via
`ComposeSettingsDsl.searchItems`). A companion singleton `SettingsHighlightState` carries a one-shot
`highlightKey` through navigation without modifying existing `Screen` types.
`HighlightableSettingRow` wraps individual items, triggering `BringIntoViewRequester` + a 1.5 s
color fade on match.

**Tech Stack:** Kotlin Multiplatform (commonMain), Compose Multiplatform, Navigation 3 (`NavKey`,
nav entries), Koin (`single`, `singleOf`, `koinInject`), Material 3 Expressive (`AppBarWithSearch`,
`SearchBarState`, `TextFieldState`, `BringIntoViewRequester`).

## Global Constraints

- All new code in `kmpuiviews/src/commonMain/` unless platform-specific
- Build command: `./gradlew :mangaworld:assembleNoFirebaseDebug` (noFirebase flavor only — no
  google-services.json)
- Test command: `./gradlew :kmpuiviews:test`
- No changes to existing design system components (`CategoryGroup`, `SegmentedListItem`,
  `SwitchSetting`, `SliderSetting`, `ListSetting`, `PreferenceSetting`)
- All new `Screen.Settings.*` subclasses must be `@Serializable data object` nested inside
  `Screen.Settings`
- `ComposeSettingsDsl` existing four fields (`generalSettings`, `viewSettings`, `playerSettings`,
  `onboardingSettings`) must be preserved exactly as-is
- Navigation 3 only — no NavController / NavHost from Navigation 2
- Match existing `SettingsScaffold` + `CategoryGroupListItem` + `segmentedListItem` pattern for all
  new list screens

---

## File Map

**New files — `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/`**

| File                                                         | Responsibility                                                           |
|--------------------------------------------------------------|--------------------------------------------------------------------------|
| `presentation/settings/search/SettingsSearchItem.kt`         | Data class + `SettingsHighlightState` Koin singleton                     |
| `presentation/settings/search/SettingsScreenDisplayNames.kt` | `NavKey → String` display name map + breadcrumb renderer                 |
| `presentation/settings/search/SettingsSearchRegistry.kt`     | Search logic + `builtInSettingsItems()` list                             |
| `presentation/settings/search/HighlightableSettingRow.kt`    | Highlight animation + BringIntoView composable                           |
| `presentation/settings/library/LibraryScreen.kt`             | Library hub (Favorites, History, Bookmarks, Notes, Lists, Notifications) |
| `presentation/settings/discover/DiscoverScreen.kt`           | Discover hub (AI Recommendations, URL Opener)                            |
| `presentation/settings/sources/SourcesScreen.kt`             | Sources & Extensions hub                                                 |
| `presentation/settings/integrations/IntegrationsScreen.kt`   | Integrations hub (Supabase, Translation)                                 |
| `presentation/settings/appearance/AppearanceScreen.kt`       | Appearance hub (→ Theme, Colors, Blur)                                   |
| `presentation/settings/appearance/ColorsScreen.kt`           | Colors sub-screen (extracted from ThemeSettingsScreen)                   |
| `presentation/settings/behavior/BehaviorScreen.kt`           | Behavior hub (→ Layout, ContentReading, Notifications, PrivacySecurity)  |
| `presentation/settings/behavior/LayoutScreen.kt`             | Layout sub-screen (extracted from GeneralSettings)                       |
| `presentation/settings/behavior/ContentReadingScreen.kt`     | Content & Reading sub-screen (extracted from GeneralSettings)            |
| `presentation/settings/behavior/PrivacySecurityScreen.kt`    | Privacy & Security hub (→ Security, Incognito)                           |
| `presentation/settings/data/DataManagementScreen.kt`         | Data Management hub (→ Backup, Account)                                  |
| `presentation/settings/about/AboutScreen.kt`                 | About hub (version, links, → Diagnostics, → Developer)                   |
| `presentation/settings/about/DiagnosticsScreen.kt`           | Diagnostics hub (→ WorkerInfo, Exceptions)                               |
| `presentation/settings/about/DeveloperScreen.kt`             | Developer hub (debug/prerelease, conditional)                            |

**New test files — `kmpuiviews/src/commonTest/kotlin/com/programmersbox/kmpuiviews/settings/`**

| File                                | Tests                      |
|-------------------------------------|----------------------------|
| `SettingsScreenDisplayNamesTest.kt` | `breadcrumbText` rendering |
| `SettingsSearchRegistryTest.kt`     | `search` filtering logic   |

**Modified files**

| File                                                   | Change                                                       |
|--------------------------------------------------------|--------------------------------------------------------------|
| `utils/ComposeSettingsDsl.kt`                          | Add 11 new fields + builder functions                        |
| `presentation/Screen.kt`                               | Add `Screen.Settings.*` subclasses                           |
| `presentation/navactions/NavigationActions.kt`         | Add 14 new navigation methods                                |
| `presentation/settings/SettingScreen.kt`               | Replace content: AppBarWithSearch + new hierarchy            |
| `presentation/settings/general/GeneralSettings.kt`     | Remove Layout and History items (moved to new screens)       |
| `presentation/settings/general/ThemeSettingsScreen.kt` | Remove Palette/ColorBlind items (moved to ColorsScreen)      |
| `presentation/settings/moreinfo/MoreInfoScreen.kt`     | Remove items redistributed to About cluster                  |
| `presentation/navigation/Nav3Graph.kt`                 | Add nav entries for all 14 new screens                       |
| `di/AppModule.kt`                                      | Register `SettingsSearchRegistry` + `SettingsHighlightState` |

---

### Task 1: `SettingsSearchItem`, `SettingsHighlightState`, and `SettingsScreenDisplayNames`

**Files:**

- Create:
  `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/settings/search/SettingsSearchItem.kt`
- Create:
  `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/settings/search/SettingsScreenDisplayNames.kt`
- Create:
  `kmpuiviews/src/commonTest/kotlin/com/programmersbox/kmpuiviews/settings/SettingsScreenDisplayNamesTest.kt`

**Interfaces:**

- Produces:
  `SettingsSearchItem(displayName, keywords, breadcrumb: List<NavKey>, targetScreen: NavKey, highlightKey: String)`
- Produces: `SettingsHighlightState` — mutable `var pendingHighlightKey: String?`
- Produces: `SettingsScreenDisplayNames.displayNameFor(NavKey): String` and
  `.breadcrumbText(List<NavKey>): String`

- [ ] **Step 1: Create the test directory and write failing test**

```bash
mkdir -p kmpuiviews/src/commonTest/kotlin/com/programmersbox/kmpuiviews/settings
```

Create
`kmpuiviews/src/commonTest/kotlin/com/programmersbox/kmpuiviews/settings/SettingsScreenDisplayNamesTest.kt`:

```kotlin
package com.programmersbox.kmpuiviews.settings

import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.kmpuiviews.presentation.settings.search.SettingsScreenDisplayNames
import kotlin.test.Test
import kotlin.test.assertEquals

class SettingsScreenDisplayNamesTest {

    @Test
    fun breadcrumbTextJoinsWithArrow() {
        val result = SettingsScreenDisplayNames.breadcrumbText(
            listOf(Screen.Settings.Appearance, Screen.ThemeSettings)
        )
        assertEquals("Appearance > Theme", result)
    }

    @Test
    fun breadcrumbTextSingleEntry() {
        val result = SettingsScreenDisplayNames.breadcrumbText(
            listOf(Screen.Settings.Appearance)
        )
        assertEquals("Appearance", result)
    }

    @Test
    fun breadcrumbTextEmpty() {
        assertEquals("", SettingsScreenDisplayNames.breadcrumbText(emptyList()))
    }

    @Test
    fun displayNameForKnownScreens() {
        assertEquals("Theme", SettingsScreenDisplayNames.displayNameFor(Screen.ThemeSettings))
        assertEquals(
            "Notifications",
            SettingsScreenDisplayNames.displayNameFor(Screen.NotificationsSettings)
        )
        assertEquals(
            "Backup & Restore",
            SettingsScreenDisplayNames.displayNameFor(Screen.MoreSettings)
        )
    }
}
```

- [ ] **Step 2: Run to confirm compile failure**

```bash
./gradlew :kmpuiviews:test --tests "*.SettingsScreenDisplayNamesTest" 2>&1 | tail -15
```

Expected: compile error — `SettingsScreenDisplayNames` not found. `Screen.Settings.Appearance` not
found (added in Task 5).

> **Note:** These tests will fully pass only after Task 5 adds the new `Screen.Settings.*` keys. For
> now confirm the compile error is only due to missing types.

- [ ] **Step 3: Create `SettingsSearchItem.kt`**

Create
`kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/settings/search/SettingsSearchItem.kt`:

```kotlin
package com.programmersbox.kmpuiviews.presentation.settings.search

import androidx.navigation3.runtime.NavKey

data class SettingsSearchItem(
    val displayName: String,
    val keywords: List<String> = emptyList(),
    val breadcrumb: List<NavKey>,
    val targetScreen: NavKey,
    val highlightKey: String,
)

class SettingsHighlightState {
    var pendingHighlightKey: String? = null
}
```

- [ ] **Step 4: Create `SettingsScreenDisplayNames.kt`**

Create
`kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/settings/search/SettingsScreenDisplayNames.kt`:

```kotlin
package com.programmersbox.kmpuiviews.presentation.settings.search

import androidx.navigation3.runtime.NavKey
import com.programmersbox.kmpuiviews.presentation.Screen

object SettingsScreenDisplayNames {

    fun displayNameFor(screen: NavKey): String = when (screen) {
        // Existing screens
        Screen.ThemeSettings -> "Theme"
        Screen.DetailsSettings -> "Details"
        Screen.NotificationsSettings -> "Notifications"
        Screen.SecuritySettings -> "Security"
        Screen.MoreSettings -> "Backup & Restore"
        Screen.AccountInfo -> "Account"
        Screen.WorkerInfoScreen -> "Worker Info"
        Screen.ExceptionScreen -> "Exceptions"
        Screen.PrereleaseScreen -> "Pre-release Builds"
        Screen.ColorHelper -> "Color Helper"
        Screen.IncognitoScreen -> "Incognito Sources"
        Screen.DebugScreen -> "Debug Menu"
        Screen.OtherSettings -> "Player"
        // New Screen.Settings.* keys — added in Task 5
        Screen.Settings -> "Settings"
        Screen.Settings.Library -> "Library"
        Screen.Settings.Discover -> "Discover"
        Screen.Settings.Sources -> "Sources & Extensions"
        Screen.Settings.Integrations -> "Integrations"
        Screen.Settings.Appearance -> "Appearance"
        Screen.Settings.Colors -> "Colors"
        Screen.Settings.Behavior -> "Behavior"
        Screen.Settings.Layout -> "Layout"
        Screen.Settings.ContentReading -> "Content & Reading"
        Screen.Settings.PrivacySecurity -> "Privacy & Security"
        Screen.Settings.Data -> "Data Management"
        Screen.Settings.About -> "About"
        Screen.Settings.Diagnostics -> "Diagnostics"
        Screen.Settings.Developer -> "Developer"
        else -> screen::class.simpleName ?: "Unknown"
    }

    fun breadcrumbText(breadcrumb: List<NavKey>): String =
        breadcrumb.joinToString(" > ") { displayNameFor(it) }
}
```

> **Note:** This file references `Screen.Settings.Appearance` etc. which don't exist until Task 5.
> The file will not compile until Task 5 is complete. Add `// TODO: compile after Task 5` comment if
> needed to track, but do not add placeholder implementations — write the full `when` block now.

- [ ] **Step 5: Commit what compiles so far**

```bash
git add kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/settings/search/SettingsSearchItem.kt \
        kmpuiviews/src/commonTest/kotlin/com/programmersbox/kmpuiviews/settings/SettingsScreenDisplayNamesTest.kt
git commit -m "feat(settings): add SettingsSearchItem and SettingsHighlightState"
```

> `SettingsScreenDisplayNames.kt` is committed in Task 5 after the new Screen keys exist.

---

### Task 2: `SettingsSearchRegistry`

**Files:**

- Create:
  `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/settings/search/SettingsSearchRegistry.kt`
- Create:
  `kmpuiviews/src/commonTest/kotlin/com/programmersbox/kmpuiviews/settings/SettingsSearchRegistryTest.kt`

**Interfaces:**

- Consumes: `SettingsSearchItem` (Task 1)
- Produces: `SettingsSearchRegistry(items: List<SettingsSearchItem>)` with
  `fun search(query: String): List<SettingsSearchItem>`
- Produces: top-level `fun builtInSettingsItems(): List<SettingsSearchItem>` — completed in Task 15

- [ ] **Step 1: Write failing tests**

Create
`kmpuiviews/src/commonTest/kotlin/com/programmersbox/kmpuiviews/settings/SettingsSearchRegistryTest.kt`:

```kotlin
package com.programmersbox.kmpuiviews.settings

import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.kmpuiviews.presentation.settings.search.SettingsSearchItem
import com.programmersbox.kmpuiviews.presentation.settings.search.SettingsSearchRegistry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SettingsSearchRegistryTest {

    private fun makeItem(displayName: String, keywords: List<String> = emptyList()) =
        SettingsSearchItem(
            displayName = displayName,
            keywords = keywords,
            breadcrumb = emptyList(),
            targetScreen = Screen.ThemeSettings,
            highlightKey = displayName.lowercase().replace(" ", "_"),
        )

    @Test
    fun blankQueryReturnsEmpty() {
        val registry = SettingsSearchRegistry(listOf(makeItem("Dark Mode")))
        assertTrue(registry.search("").isEmpty())
        assertTrue(registry.search("   ").isEmpty())
    }

    @Test
    fun matchesDisplayNameCaseInsensitive() {
        val item = makeItem("Dark Mode")
        val registry = SettingsSearchRegistry(listOf(item))
        assertEquals(listOf(item), registry.search("dark"))
        assertEquals(listOf(item), registry.search("DARK"))
        assertEquals(listOf(item), registry.search("Dark Mode"))
    }

    @Test
    fun matchesKeyword() {
        val item = makeItem("Theme", listOf("amoled", "dark", "light"))
        val registry = SettingsSearchRegistry(listOf(item))
        assertEquals(listOf(item), registry.search("amoled"))
        assertEquals(listOf(item), registry.search("AMOLED"))
    }

    @Test
    fun noMatchReturnsEmpty() {
        val registry = SettingsSearchRegistry(listOf(makeItem("Dark Mode")))
        assertTrue(registry.search("xyz123").isEmpty())
    }

    @Test
    fun multipleItemsFilteredCorrectly() {
        val darkMode = makeItem("Dark Mode")
        val gridType = makeItem("Grid Type", listOf("columns", "layout"))
        val registry = SettingsSearchRegistry(listOf(darkMode, gridType))
        assertEquals(listOf(darkMode), registry.search("dark"))
        assertEquals(listOf(gridType), registry.search("columns"))
        assertEquals(2, registry.search("e").size) // matches both
    }
}
```

- [ ] **Step 2: Run to confirm compile failure**

```bash
./gradlew :kmpuiviews:test --tests "*.SettingsSearchRegistryTest" 2>&1 | tail -10
```

Expected: compile error — `SettingsSearchRegistry` not found.

- [ ] **Step 3: Implement `SettingsSearchRegistry`**

Create
`kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/settings/search/SettingsSearchRegistry.kt`:

```kotlin
package com.programmersbox.kmpuiviews.presentation.settings.search

class SettingsSearchRegistry(val items: List<SettingsSearchItem>) {

    fun search(query: String): List<SettingsSearchItem> {
        if (query.isBlank()) return emptyList()
        return items.filter { item ->
            item.displayName.contains(query, ignoreCase = true)
                    || item.keywords.any { it.contains(query, ignoreCase = true) }
        }
    }
}

// Completed in Task 15 once all screens and their Screen.* keys exist
fun builtInSettingsItems(): List<SettingsSearchItem> = emptyList()
```

- [ ] **Step 4: Run tests**

```bash
./gradlew :kmpuiviews:test --tests "*.SettingsSearchRegistryTest" 2>&1 | tail -15
```

Expected: all 5 tests PASS.

- [ ] **Step 5: Commit**

```bash
git add kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/settings/search/SettingsSearchRegistry.kt \
        kmpuiviews/src/commonTest/kotlin/com/programmersbox/kmpuiviews/settings/SettingsSearchRegistryTest.kt
git commit -m "feat(settings): add SettingsSearchRegistry with search logic"
```

---

### Task 3: `HighlightableSettingRow`

**Files:**

- Create:
  `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/settings/search/HighlightableSettingRow.kt`

**Interfaces:**

- Produces:
  `HighlightableSettingRow(activeHighlight: MutableState<String?>, itemKey: String, modifier: Modifier, content: @Composable () -> Unit)`
- Produces:
  `rememberActiveHighlight(highlightState: SettingsHighlightState): MutableState<String?>` — reads
  and clears `pendingHighlightKey`

- [ ] **Step 1: Create `HighlightableSettingRow.kt`**

```kotlin
package com.programmersbox.kmpuiviews.presentation.settings.search

import androidx.compose.animation.core.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Box

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HighlightableSettingRow(
    activeHighlight: MutableState<String?>,
    itemKey: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val isHighlighted = activeHighlight.value == itemKey

    val highlightColor by animateColorAsState(
        targetValue = if (isHighlighted) MaterialTheme.colorScheme.primaryContainer
        else Color.Transparent,
        animationSpec = tween(durationMillis = 1500),
        finishedListener = { if (it == Color.Transparent) activeHighlight.value = null },
        label = "settingHighlight",
    )

    LaunchedEffect(isHighlighted) {
        if (isHighlighted) bringIntoViewRequester.bringIntoView()
    }

    Box(
        modifier = modifier
            .bringIntoViewRequester(bringIntoViewRequester)
            .background(highlightColor),
    ) {
        content()
    }
}

@Composable
fun rememberActiveHighlight(highlightState: SettingsHighlightState): MutableState<String?> =
    remember {
        mutableStateOf(highlightState.pendingHighlightKey.also {
            highlightState.pendingHighlightKey = null
        })
    }
```

- [ ] **Step 2: Build to verify compile**

```bash
./gradlew :kmpuiviews:compileKotlinAndroid 2>&1 | grep -E "error:|warning:" | head -20
```

Expected: no errors.

- [ ] **Step 3: Commit**

```bash
git add kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/settings/search/HighlightableSettingRow.kt
git commit -m "feat(settings): add HighlightableSettingRow with BringIntoView + color fade"
```

---

### Task 4: Expand `ComposeSettingsDsl`

**Files:**

- Modify:
  `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/utils/ComposeSettingsDsl.kt`

**Interfaces:**

- Consumes: `SettingsSearchItem` (Task 1)
- Produces: 11 new DSL fields with builder functions

- [ ] **Step 1: Read the current file**

Open `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/utils/ComposeSettingsDsl.kt`and
verify the existing 4 fields: `generalSettings`, `viewSettings`, `playerSettings`,
`onboardingSettings`.

- [ ] **Step 2: Replace with expanded version**

Replace the entire file content:

```kotlin
package com.programmersbox.kmpuiviews.utils

import androidx.compose.runtime.Composable
import com.programmersbox.kmpuiviews.presentation.components.settings.CategoryGroupScope
import com.programmersbox.kmpuiviews.presentation.onboarding.OnboardingScope
import com.programmersbox.kmpuiviews.presentation.settings.search.SettingsSearchItem

class ComposeSettingsDsl {
    // ── Existing (unchanged) ─────────────────────────────────
    var generalSettings: @Composable () -> Unit = {}
    var viewSettings: CategoryGroupScope.() -> Unit = {}
    var playerSettings: @Composable () -> Unit = {}
    var onboardingSettings: OnboardingScope.() -> Unit = {}

    fun generalSettings(block: @Composable () -> Unit) {
        generalSettings = block
    }
    fun viewSettings(block: CategoryGroupScope.() -> Unit) {
        viewSettings = block
    }
    fun playerSettings(block: @Composable () -> Unit) {
        playerSettings = block
    }
    fun onboardingSettings(block: OnboardingScope.() -> Unit) {
        onboardingSettings = block
    }

    // ── New: search registry ─────────────────────────────────
    var searchItems: () -> List<SettingsSearchItem> = { emptyList() }
    fun searchItems(block: () -> List<SettingsSearchItem>) {
        searchItems = block
    }

    // ── New: per-section injection ───────────────────────────
    var quickActionsSettings: CategoryGroupScope.() -> Unit = {}
    fun quickActionsSettings(block: CategoryGroupScope.() -> Unit) {
        quickActionsSettings = block
    }

    var librarySettings: CategoryGroupScope.() -> Unit = {}
    fun librarySettings(block: CategoryGroupScope.() -> Unit) {
        librarySettings = block
    }

    var discoverSettings: CategoryGroupScope.() -> Unit = {}
    fun discoverSettings(block: CategoryGroupScope.() -> Unit) {
        discoverSettings = block
    }

    var sourcesSettings: CategoryGroupScope.() -> Unit = {}
    fun sourcesSettings(block: CategoryGroupScope.() -> Unit) {
        sourcesSettings = block
    }

    var integrationsSettings: CategoryGroupScope.() -> Unit = {}
    fun integrationsSettings(block: CategoryGroupScope.() -> Unit) {
        integrationsSettings = block
    }

    var appearanceSettings: @Composable () -> Unit = {}
    fun appearanceSettings(block: @Composable () -> Unit) {
        appearanceSettings = block
    }

    var behaviorSettings: @Composable () -> Unit = {}
    fun behaviorSettings(block: @Composable () -> Unit) {
        behaviorSettings = block
    }

    var layoutSettings: @Composable () -> Unit = {}
    fun layoutSettings(block: @Composable () -> Unit) {
        layoutSettings = block
    }

    var contentReadingSettings: @Composable () -> Unit = {}
    fun contentReadingSettings(block: @Composable () -> Unit) {
        contentReadingSettings = block
    }

    var dataSettings: @Composable () -> Unit = {}
    fun dataSettings(block: @Composable () -> Unit) {
        dataSettings = block
    }

    var aboutSettings: CategoryGroupScope.() -> Unit = {}
    fun aboutSettings(block: CategoryGroupScope.() -> Unit) {
        aboutSettings = block
    }
}
```

- [ ] **Step 3: Build to verify no breakage**

```bash
./gradlew :kmpuiviews:compileKotlinAndroid 2>&1 | grep -E "error:" | head -20
```

Expected: no errors. All existing callers still compile because all existing fields and functions
are preserved.

- [ ] **Step 4: Commit**

```bash
git add kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/utils/ComposeSettingsDsl.kt
git commit -m "feat(settings): expand ComposeSettingsDsl with per-section injection slots and searchItems"
```

---

### Task 5: New `Screen.Settings.*` subclasses + `NavigationActions` methods

**Files:**

- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/Screen.kt`
- Modify:
  `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/navactions/NavigationActions.kt`
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/di/AppModule.kt`
- Add (previously staged): `presentation/settings/search/SettingsScreenDisplayNames.kt`

**Interfaces:**

- Produces: `Screen.Settings.Library`, `.Discover`, `.Sources`, `.Integrations`, `.Appearance`,
  `.Colors`, `.Behavior`, `.Layout`, `.ContentReading`, `.PrivacySecurity`, `.Data`, `.About`,
  `.Diagnostics`, `.Developer`
- Produces: `NavigationActions.library()`, `.discover()`, `.sources()`, `.integrations()`,
  `.appearance()`, `.colors()`, `.behaviorSettings()`, `.layout()`, `.contentReading()`,
  `.privacySecurity()`, `.dataManagement()`, `.aboutSettings()`, `.diagnostics()`, `.developer()`
- Produces: `SettingsSearchRegistry` + `SettingsHighlightState` registered in Koin

- [ ] **Step 1: Add new Screen subclasses**

In `presentation/Screen.kt`, replace the existing `Screen.Settings` block:

```kotlin
@Serializable
data object Settings : Screen("settings") {
    @Serializable
    data object Blur : Screen("blur")
    @Serializable
    data object Library : Screen("library")
    @Serializable
    data object Discover : Screen("discover")
    @Serializable
    data object Sources : Screen("sources")
    @Serializable
    data object Integrations : Screen("integrations")
    @Serializable
    data object Appearance : Screen("appearance")
    @Serializable
    data object Colors : Screen("colors")
    @Serializable
    data object Behavior : Screen("behavior")
    @Serializable
    data object Layout : Screen("layout")
    @Serializable
    data object ContentReading : Screen("content_reading")
    @Serializable
    data object PrivacySecurity : Screen("privacy_security")
    @Serializable
    data object Data : Screen("data")
    @Serializable
    data object About : Screen("about")
    @Serializable
    data object Diagnostics : Screen("diagnostics")
    @Serializable
    data object Developer : Screen("developer")
}
```

> `Screen.Settings.Blur` previously had route `"home"` — changed to `"blur"`. Navigation 3 uses the
> serialized NavKey type, not the route string, so this is safe. Check for any `.route` property
> usages on `Screen.Settings.Blur` with:
`grep -r "Settings.Blur.route\|Settings\.Blur\.route" . --include="*.kt"` and update any found.

- [ ] **Step 2: Add navigation methods to `NavigationActions`**

Add these methods to the `NavigationActions` interface in
`presentation/navactions/NavigationActions.kt`:

```kotlin
fun library()
fun discover()
fun sources()
fun integrations()
fun appearance()
fun colors()
fun behaviorSettings()
fun layout()
fun contentReading()
fun privacySecurity()
fun dataManagement()
fun aboutSettings()
fun diagnostics()
fun developer()
```

> After adding to the interface, the IDE (and build) will flag all concrete implementations as
> broken. The concrete implementation lives in each app module (MangaWorld, AnimeWorld, NovelWorld).
> Add stub implementations `override fun library() = navigate(Screen.Settings.Library)` etc. to each
> app's `NavigationActions` implementation. Find them with:
`grep -r "class.*NavigationActions\|object.*NavigationActions" . --include="*.kt"` from the repo
> root.

- [ ] **Step 3: Register Koin singletons in `AppModule.kt`**

Add to the `appModule` block in `di/AppModule.kt`:

```kotlin
import com.programmersbox.kmpuiviews.presentation.settings.search.SettingsHighlightState
import com.programmersbox.kmpuiviews.presentation.settings.search.SettingsSearchRegistry
import com.programmersbox.kmpuiviews.presentation.settings.search.builtInSettingsItems
import com.programmersbox.kmpuiviews.utils.ComposeSettingsDsl

// inside the appModule = module { ... } block:
single { SettingsHighlightState() }
single { SettingsSearchRegistry(builtInSettingsItems() + get<ComposeSettingsDsl>().searchItems()) }
```

- [ ] **Step 4: Commit `SettingsScreenDisplayNames.kt` (now compiles)**

```bash
git add kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/settings/search/SettingsScreenDisplayNames.kt \
        kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/Screen.kt \
        kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/navactions/NavigationActions.kt \
        kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/di/AppModule.kt
git commit -m "feat(settings): add Screen.Settings.* nav keys, NavigationActions methods, and Koin registrations"
```

- [ ] **Step 5: Run display names test**

```bash
./gradlew :kmpuiviews:test --tests "*.SettingsScreenDisplayNamesTest" 2>&1 | tail -15
```

Expected: all 4 tests PASS.

---

### Task 6: Feature entry screens — Library, Discover, Sources, Integrations

**Files:**

- Create: `presentation/settings/library/LibraryScreen.kt`
- Create: `presentation/settings/discover/DiscoverScreen.kt`
- Create: `presentation/settings/sources/SourcesScreen.kt`
- Create: `presentation/settings/integrations/IntegrationsScreen.kt`

**Interfaces:**

- Consumes: `SettingsScaffold`, `CategoryGroupListItem`, `segmentedListItem`, `NavigationActions`,
  `ComposeSettingsDsl`
- Each screen is self-contained with no VM dependency

- [ ] **Step 1: Create `LibraryScreen.kt`**

```kotlin
package com.programmersbox.kmpuiviews.presentation.settings.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.programmersbox.kmpuiviews.presentation.components.settings.CategoryGroupListItem
import com.programmersbox.kmpuiviews.presentation.settings.SettingsScaffold
import com.programmersbox.kmpuiviews.utils.ComposeSettingsDsl
import com.programmersbox.kmpuiviews.utils.LocalHistoryDao
import com.programmersbox.kmpuiviews.utils.LocalNavActions
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LibraryScreen(
    composeSettingsDsl: ComposeSettingsDsl = koinInject(),
) {
    val navActions = LocalNavActions.current

    SettingsScaffold(
        title = "Library",
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CategoryGroupListItem {
            segmentedListItem(
                content = { Text("Favorites") },
                leadingContent = { Icon(Icons.Default.Star, null) },
                onClick = navActions::favorites,
            )
            segmentedListItem(
                content = { Text("History") },
                leadingContent = { Icon(Icons.Default.History, null) },
                supportingContent = {
                    val historyCount by LocalHistoryDao.current
                        .getAllRecentHistoryCount()
                        .collectAsStateWithLifecycle(0)
                    Text(historyCount.toString())
                },
                onClick = navActions::history,
            )
            segmentedListItem(
                content = { Text("Bookmarks") },
                leadingContent = { Icon(Icons.Default.Bookmark, null) },
                onClick = navActions::bookmarks,
            )
            segmentedListItem(
                content = { Text("Notes") },
                leadingContent = { Icon(Icons.Default.Edit, null) },
                onClick = navActions::notes,
            )
            segmentedListItem(
                content = { Text("Custom Lists") },
                leadingContent = { Icon(Icons.AutoMirrored.Default.List, null) },
                onClick = navActions::customList,
            )
            segmentedListItem(
                content = { Text("Saved Notifications") },
                leadingContent = { Icon(Icons.Default.Notifications, null) },
                onClick = navActions::notifications,
            )
            apply(composeSettingsDsl.librarySettings)
        }
    }
}
```

- [ ] **Step 2: Create `DiscoverScreen.kt`**

```kotlin
package com.programmersbox.kmpuiviews.presentation.settings.discover

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Web
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.kmpuiviews.presentation.components.settings.CategoryGroupListItem
import com.programmersbox.kmpuiviews.presentation.settings.SettingsScaffold
import com.programmersbox.kmpuiviews.utils.ComposeSettingsDsl
import com.programmersbox.kmpuiviews.utils.LocalNavActions
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DiscoverScreen(
    composeSettingsDsl: ComposeSettingsDsl = koinInject(),
) {
    val navActions = LocalNavActions.current

    SettingsScaffold(
        title = "Discover",
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CategoryGroupListItem {
            segmentedListItem(
                content = { Text("AI Recommendations") },
                leadingContent = { Icon(Icons.Default.AutoAwesome, null) },
                onClick = { navActions.navigate(Screen.GeminiScreen) },
            )
            segmentedListItem(
                content = { Text("URL Opener") },
                leadingContent = { Icon(Icons.Default.Web, null) },
                onClick = { navActions.navigate(Screen.UrlOpener) },
            )
            apply(composeSettingsDsl.discoverSettings)
        }
    }
}
```

- [ ] **Step 3: Create `SourcesScreen.kt`**

```kotlin
package com.programmersbox.kmpuiviews.presentation.settings.sources

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Reorder
import androidx.compose.material.icons.filled.Source
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.programmersbox.kmpuiviews.presentation.components.settings.CategoryGroupListItem
import com.programmersbox.kmpuiviews.presentation.settings.SettingsScaffold
import com.programmersbox.kmpuiviews.presentation.settings.utils.showSourceChooser
import com.programmersbox.kmpuiviews.utils.ComposeSettingsDsl
import com.programmersbox.kmpuiviews.utils.LocalCurrentSource
import com.programmersbox.kmpuiviews.utils.LocalNavActions
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.currentSource
import otakuworld.kmpuiviews.generated.resources.view_extensions
import otakuworld.kmpuiviews.generated.resources.view_source_in_browser

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SourcesScreen(
    composeSettingsDsl: ComposeSettingsDsl = koinInject(),
) {
    val navActions = LocalNavActions.current
    val uriHandler = LocalUriHandler.current
    val source by LocalCurrentSource.current.asFlow().collectAsStateWithLifecycle(null)
    var showSourceChooser by showSourceChooser()

    SettingsScaffold(
        title = "Sources & Extensions",
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CategoryGroupListItem {
            segmentedListItem(
                content = {
                    Text(
                        stringResource(
                            Res.string.currentSource,
                            source?.serviceName.orEmpty()
                        )
                    )
                },
                leadingContent = { Icon(Icons.Default.Source, null) },
                onClick = { showSourceChooser = true },
            )
            segmentedListItem(
                content = { Text("Source Order") },
                leadingContent = { Icon(Icons.Default.Reorder, null) },
                onClick = navActions::order,
            )
            segmentedListItem(
                content = { Text(stringResource(Res.string.view_extensions)) },
                leadingContent = { Icon(Icons.Default.Extension, null) },
                onClick = navActions::extensionList,
            )
            segmentedListItem(
                content = { Text(stringResource(Res.string.view_source_in_browser)) },
                leadingContent = { Icon(Icons.Default.OpenInBrowser, null) },
                onClick = { source?.baseUrl?.let { uriHandler.openUri(it) } },
            )
            apply(composeSettingsDsl.sourcesSettings)
        }
    }
}
```

- [ ] **Step 4: Create `IntegrationsScreen.kt`**

```kotlin
package com.programmersbox.kmpuiviews.presentation.settings.integrations

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import com.programmersbox.kmpuiviews.presentation.components.settings.CategoryGroupListItem
import com.programmersbox.kmpuiviews.presentation.settings.SettingsScaffold
import com.programmersbox.kmpuiviews.presentation.settings.translationmodels.showTranslationScreen
import com.programmersbox.kmpuiviews.utils.ComposeSettingsDsl
import com.programmersbox.kmpuiviews.utils.LocalNavActions
import com.programmersbox.supabaseintegration.ui.SupabaseIcon
import com.programmersbox.supabaseintegration.ui.SupabaseRoutes
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.viewTranslationModels

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun IntegrationsScreen(
    composeSettingsDsl: ComposeSettingsDsl = koinInject(),
) {
    val navActions = LocalNavActions.current
    var showTranslation by showTranslationScreen()

    SettingsScaffold(
        title = "Integrations",
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CategoryGroupListItem {
            segmentedListItem(
                content = { Text("Supabase") },
                leadingContent = { SupabaseIcon() },
                onClick = { navActions.navigate(SupabaseRoutes) },
            )
            segmentedListItem(
                content = { Text(stringResource(Res.string.viewTranslationModels)) },
                leadingContent = { Icon(Icons.Default.Language, null) },
                onClick = { showTranslation = true },
            )
            apply(composeSettingsDsl.integrationsSettings)
        }
    }
}
```

- [ ] **Step 5: Build to verify**

```bash
./gradlew :kmpuiviews:compileKotlinAndroid 2>&1 | grep "error:" | head -20
```

Expected: no errors.

- [ ] **Step 6: Commit**

```bash
git add kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/settings/library/ \
        kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/settings/discover/ \
        kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/settings/sources/ \
        kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/settings/integrations/
git commit -m "feat(settings): add Library, Discover, Sources, and Integrations hub screens"
```

---

### Task 7: `ColorsScreen` — extract from `ThemeSettingsScreen`

**Files:**

- Create: `presentation/settings/appearance/ColorsScreen.kt`
- Modify: `presentation/settings/general/ThemeSettingsScreen.kt`

**Interfaces:**

- Consumes: `ColorBlindTypeSettings`, `PaletteSetting` from `GeneralSettings.kt` (move these
  functions — they currently live in `GeneralSettings.kt`)
- Produces: `ColorsScreen()` composable

- [ ] **Step 1: Create `ColorsScreen.kt`**

The Palette settings (`PaletteSetting`, `ColorBlindTypeSettings`) currently live in
`ThemeSettingsScreen.kt`. Move them to `ColorsScreen.kt`:

```kotlin
package com.programmersbox.kmpuiviews.presentation.settings.appearance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.programmersbox.datastore.ColorBlindnessType
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.datastore.PaletteSwatchType
import com.programmersbox.datastore.rememberSwatchStyle
import com.programmersbox.datastore.rememberSwatchType
import com.programmersbox.kmpuiviews.presentation.components.settings.CategoryGroup
import com.programmersbox.kmpuiviews.presentation.components.settings.CategoryGroupDefaults
import com.programmersbox.kmpuiviews.presentation.components.settings.ListSetting
import com.programmersbox.kmpuiviews.presentation.components.settings.ShowWhen
import com.programmersbox.kmpuiviews.presentation.components.settings.SwitchSetting
import com.programmersbox.kmpuiviews.presentation.settings.SettingsScaffold
import com.materialkolor.PaletteStyle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.cancel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ColorsScreen() {
    val handling: NewSettingsHandling = koinInject()

    SettingsScaffold(
        title = "Colors",
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CategoryGroup {
            item {
                ListSetting(
                    settingTitle = { Text("Color Blindness") },
                    settingIcon = {
                        Icon(
                            Icons.Default.ColorLens,
                            null,
                            modifier = Modifier.fillMaxSize()
                        )
                    },
                    value = handling.rememberColorBlindType().value,
                    updateValue = { it, d ->
                        d.value = false
                        handling.rememberColorBlindType().value = it
                    },
                    options = ColorBlindnessType.entries,
                    summaryValue = {
                        Text(
                            when (handling.rememberColorBlindType().value) {
                                ColorBlindnessType.None -> "None - No Color Blindness"
                                ColorBlindnessType.Protanopia -> "Protanopia - Red-green color blindness"
                                ColorBlindnessType.Deuteranopia -> "Deuteranopia - Blue-yellow color blindness"
                                ColorBlindnessType.Tritanopia -> "Tritanopia - Green-blue color blindness"
                            }
                        )
                    },
                    confirmText = {
                        TextButton(onClick = {
                            it.value = false
                        }) { Text(stringResource(Res.string.cancel)) }
                    },
                    dialogTitle = { Text("Color Blindness") },
                    dialogIcon = { Icon(Icons.Default.ColorLens, null) },
                )
            }
        }

        CategoryGroup {
            item {
                var usePalette by handling.rememberUsePalette()
                SwitchSetting(
                    settingTitle = { Text("Use Palette") },
                    summaryValue = { Text("Color the details screen using image palette") },
                    settingIcon = {
                        Icon(
                            Icons.Default.Palette,
                            null,
                            modifier = Modifier.fillMaxSize()
                        )
                    },
                    value = usePalette,
                    updateValue = { usePalette = it },
                )
                CategoryGroupDefaults.Divider()
                ShowWhen(usePalette) {
                    var paletteSwatchType by rememberSwatchType()
                    ListSetting(
                        settingTitle = { Text("Swatch Type") },
                        dialogIcon = { Icon(Icons.Default.Palette, null) },
                        settingIcon = {
                            Icon(
                                Icons.Default.Palette,
                                null,
                                modifier = Modifier.fillMaxSize()
                            )
                        },
                        dialogTitle = { Text("Choose a Swatch Type") },
                        summaryValue = { Text(paletteSwatchType.name) },
                        confirmText = {
                            TextButton(onClick = { it.value = false }) {
                                Text(
                                    stringResource(Res.string.cancel)
                                )
                            }
                        },
                        value = paletteSwatchType,
                        options = PaletteSwatchType.entries,
                        updateValue = { it, d -> d.value = false; paletteSwatchType = it },
                    )
                    CategoryGroupDefaults.Divider()
                    var paletteStyle by rememberSwatchStyle()
                    ListSetting(
                        settingTitle = { Text("Swatch Style") },
                        dialogIcon = { Icon(Icons.Default.Palette, null) },
                        settingIcon = {
                            Icon(
                                Icons.Default.Palette,
                                null,
                                modifier = Modifier.fillMaxSize()
                            )
                        },
                        dialogTitle = { Text("Choose a Swatch Style") },
                        summaryValue = { Text(paletteStyle.name) },
                        confirmText = {
                            TextButton(onClick = { it.value = false }) {
                                Text(
                                    stringResource(Res.string.cancel)
                                )
                            }
                        },
                        value = paletteStyle,
                        options = PaletteStyle.entries,
                        updateValue = { it, d -> d.value = false; paletteStyle = it },
                    )
                }
            }
        }
    }
}
```

> **Note:** `handling.rememberColorBlindType()` returns a `MutableState`. Use
`var colorBlindType by handling.rememberColorBlindType()` pattern consistent with existing code in
`GeneralSettings.kt`.

- [ ] **Step 2: Trim `ThemeSettingsScreen.kt`**

In `ThemeSettingsScreen.kt`, remove the second `CategoryGroup` block (the one containing
`PaletteSetting` and `ColorBlindTypeSettings`) and delete the private `PaletteSetting` and
`ColorBlindTypeSettings` composable functions. The file should only contain:

```kotlin
@Composable
fun ThemeSettingsScreen() {
    val handling: NewSettingsHandling = koinInject()
    SettingsScaffold(title = "Theme", verticalArrangement = Arrangement.spacedBy(16.dp)) {
        var isAmoledMode by handling.rememberIsAmoledMode()
        CategoryGroup {
            item { ThemeSetting(handling = handling, isAmoledMode = isAmoledMode) }
            item {
                AmoledModeSetting(
                    isAmoledMode = isAmoledMode,
                    onAmoledModeChange = { isAmoledMode = it })
            }
            item { ExpressivenessSetting(handling = handling) }
            item { BlurSetting(handling = handling) }
        }
    }
}
```

Keep `ThemeSetting`, `AmoledModeSetting`, `ExpressivenessSetting` functions in the file. Remove
`PaletteSetting`, `ColorBlindTypeSettings`.

- [ ] **Step 3: Build**

```bash
./gradlew :kmpuiviews:compileKotlinAndroid 2>&1 | grep "error:" | head -20
```

Expected: no errors.

- [ ] **Step 4: Commit**

```bash
git add kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/settings/appearance/ColorsScreen.kt \
        kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/settings/general/ThemeSettingsScreen.kt
git commit -m "feat(settings): add ColorsScreen, extract Palette+ColorBlind from ThemeSettingsScreen"
```

---

### Task 8: `AppearanceScreen`

**Files:**

- Create: `presentation/settings/appearance/AppearanceScreen.kt`

- [ ] **Step 1: Create `AppearanceScreen.kt`**

```kotlin
package com.programmersbox.kmpuiviews.presentation.settings.appearance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.kmpuiviews.presentation.components.settings.CategoryGroupListItem
import com.programmersbox.kmpuiviews.presentation.settings.SettingsScaffold
import com.programmersbox.kmpuiviews.utils.ComposeSettingsDsl
import com.programmersbox.kmpuiviews.utils.LocalNavActions
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppearanceScreen(
    composeSettingsDsl: ComposeSettingsDsl = koinInject(),
) {
    val navActions = LocalNavActions.current

    SettingsScaffold(
        title = "Appearance",
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CategoryGroupListItem {
            segmentedListItem(
                content = { Text("Theme") },
                leadingContent = { Icon(Icons.Default.Palette, null) },
                onClick = { navActions.navigate(Screen.ThemeSettings) },
            )
            segmentedListItem(
                content = { Text("Colors") },
                leadingContent = { Icon(Icons.Default.ColorLens, null) },
                onClick = { navActions.navigate(Screen.Settings.Colors) },
            )
            segmentedListItem(
                content = { Text("Blur Effects") },
                leadingContent = { Icon(Icons.Default.BlurOn, null) },
                onClick = { navActions.navigate(Screen.Settings.Blur) },
            )
        }

        composeSettingsDsl.appearanceSettings()
    }
}
```

- [ ] **Step 2: Build + commit**

```bash
./gradlew :kmpuiviews:compileKotlinAndroid 2>&1 | grep "error:" | head -10
git add kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/settings/appearance/AppearanceScreen.kt
git commit -m "feat(settings): add AppearanceScreen hub"
```

---

### Task 9: `LayoutScreen` — extract from `GeneralSettings`

**Files:**

- Create: `presentation/settings/behavior/LayoutScreen.kt`
- Modify: `presentation/settings/general/GeneralSettings.kt`

- [ ] **Step 1: Create `LayoutScreen.kt`**

The items come from `GeneralSettings.kt` — `GridTypeSettings`, `DetailPaneSettings`,
`NavigationBarSettings`. Move them here:

```kotlin
package com.programmersbox.kmpuiviews.presentation.settings.behavior

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.kmpuiviews.presentation.components.settings.CategoryGroupListItem
import com.programmersbox.kmpuiviews.presentation.settings.SettingsScaffold
import com.programmersbox.kmpuiviews.presentation.settings.general.GridTypeSettings
import com.programmersbox.kmpuiviews.presentation.settings.general.NavigationBarSettings
import com.programmersbox.kmpuiviews.presentation.components.item
import com.programmersbox.kmpuiviews.utils.ComposeSettingsDsl
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LayoutScreen(
    composeSettingsDsl: ComposeSettingsDsl = koinInject(),
) {
    val handling: NewSettingsHandling = koinInject()

    SettingsScaffold(
        title = "Layout",
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CategoryGroupListItem {
            item(false) { GridTypeSettings(handling = handling) }
            item(false) { DetailPaneSettings(handling = handling) }
        }

        CategoryGroupListItem {
            item { NavigationBarSettings(handling = handling) }
        }

        composeSettingsDsl.layoutSettings()
    }
}
```

> `GridTypeSettings`, `DetailPaneSettings`, `NavigationBarSettings` are currently private in
`GeneralSettings.kt`. Change them from `private` to `internal` so `LayoutScreen` can use them, or
> move them to a shared `GeneralSettingsComponents.kt` file. Prefer making them `internal` in
`GeneralSettings.kt` to minimize file moves.

- [ ] **Step 2: Remove Layout items from `GeneralSettings.kt`**

In `GeneralSettings.kt`, remove the `CategoryGroupListItem` block containing`NavigationBarSettings`,
and the `CategoryGroupListItem` block containing `GridTypeSettings`,`DetailPaneSettings`. Remove the
`HistorySettings` block too (moved in Task 10). The file should shrink to only contain the
`CategoryGroupListItem` with navigation to Theme, Details, Blur sub-screens, plus `customSettings()`
call.

Change visibility of `GridTypeSettings`, `DetailPaneSettings`, `NavigationBarSettings`,
`HistorySettings` from `private` to `internal`.

- [ ] **Step 3: Build**

```bash
./gradlew :kmpuiviews:compileKotlinAndroid 2>&1 | grep "error:" | head -20
```

- [ ] **Step 4: Commit**

```bash
git add kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/settings/behavior/LayoutScreen.kt \
        kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/settings/general/GeneralSettings.kt
git commit -m "feat(settings): add LayoutScreen, extract Grid/Navigation/DetailPane from GeneralSettings"
```

---

### Task 10: `ContentReadingScreen` — extract from `GeneralSettings`

**Files:**

- Create: `presentation/settings/behavior/ContentReadingScreen.kt`

- [ ] **Step 1: Create `ContentReadingScreen.kt`**

```kotlin
package com.programmersbox.kmpuiviews.presentation.settings.behavior

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.programmersbox.datastore.DataStoreHandling
import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.kmpuiviews.presentation.components.item
import com.programmersbox.kmpuiviews.presentation.components.settings.CategoryGroupListItem
import com.programmersbox.kmpuiviews.presentation.settings.SettingsScaffold
import com.programmersbox.kmpuiviews.presentation.settings.general.HistorySettings
import com.programmersbox.kmpuiviews.utils.ComposeSettingsDsl
import com.programmersbox.kmpuiviews.utils.LocalNavActions
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ContentReadingScreen(
    composeSettingsDsl: ComposeSettingsDsl = koinInject(),
) {
    val navActions = LocalNavActions.current
    val dataStoreHandling: DataStoreHandling = koinInject()

    SettingsScaffold(
        title = "Content & Reading",
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CategoryGroupListItem {
            segmentedListItem(
                content = { Text("Details") },
                leadingContent = { Icon(Icons.Default.Animation, null) },
                onClick = { navActions.navigate(Screen.DetailsSettings) },
            )
            segmentedListItem(
                content = { Text("Player") },
                leadingContent = { Icon(Icons.Default.PlayCircleOutline, null) },
                onClick = navActions::otherSettings,
            )
        }

        CategoryGroupListItem {
            item(false) { HistorySettings(dataStoreHandling = dataStoreHandling) }
        }

        composeSettingsDsl.generalSettings()
        composeSettingsDsl.contentReadingSettings()
    }
}
```

- [ ] **Step 2: Build + commit**

```bash
./gradlew :kmpuiviews:compileKotlinAndroid 2>&1 | grep "error:" | head -10
git add kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/settings/behavior/ContentReadingScreen.kt
git commit -m "feat(settings): add ContentReadingScreen with Details, Player, History, and generalSettings injection"
```

---

### Task 11: `BehaviorScreen` + `PrivacySecurityScreen`

**Files:**

- Create: `presentation/settings/behavior/BehaviorScreen.kt`
- Create: `presentation/settings/behavior/PrivacySecurityScreen.kt`

- [ ] **Step 1: Create `PrivacySecurityScreen.kt`**

```kotlin
package com.programmersbox.kmpuiviews.presentation.settings.behavior

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.programmersbox.kmpuiviews.presentation.components.settings.CategoryGroupListItem
import com.programmersbox.kmpuiviews.presentation.settings.SettingsScaffold
import com.programmersbox.kmpuiviews.utils.LocalNavActions

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PrivacySecurityScreen() {
    val navActions = LocalNavActions.current

    SettingsScaffold(
        title = "Privacy & Security",
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CategoryGroupListItem {
            segmentedListItem(
                content = { Text("Biometric Lock") },
                leadingContent = { Icon(Icons.Default.Security, null) },
                onClick = navActions::security,
            )
            segmentedListItem(
                content = { Text("Incognito Sources") },
                leadingContent = { Icon(Icons.Default.VisibilityOff, null) },
                onClick = navActions::incognito,
            )
        }
    }
}
```

- [ ] **Step 2: Create `BehaviorScreen.kt`**

```kotlin
package com.programmersbox.kmpuiviews.presentation.settings.behavior

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.programmersbox.kmpuiviews.presentation.components.settings.CategoryGroupListItem
import com.programmersbox.kmpuiviews.presentation.settings.SettingsScaffold
import com.programmersbox.kmpuiviews.utils.ComposeSettingsDsl
import com.programmersbox.kmpuiviews.utils.LocalNavActions
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BehaviorScreen(
    composeSettingsDsl: ComposeSettingsDsl = koinInject(),
) {
    val navActions = LocalNavActions.current

    SettingsScaffold(
        title = "Behavior",
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CategoryGroupListItem {
            segmentedListItem(
                content = { Text("Layout") },
                leadingContent = { Icon(Icons.Default.GridView, null) },
                onClick = navActions::layout,
            )
            segmentedListItem(
                content = { Text("Content & Reading") },
                leadingContent = { Icon(Icons.Default.MenuBook, null) },
                onClick = navActions::contentReading,
            )
            segmentedListItem(
                content = { Text("Notifications") },
                leadingContent = { Icon(Icons.Default.Notifications, null) },
                onClick = navActions::notificationsSettings,
            )
            segmentedListItem(
                content = { Text("Privacy & Security") },
                leadingContent = { Icon(Icons.Default.Security, null) },
                onClick = navActions::privacySecurity,
            )
        }

        composeSettingsDsl.behaviorSettings()
    }
}
```

- [ ] **Step 3: Build + commit**

```bash
./gradlew :kmpuiviews:compileKotlinAndroid 2>&1 | grep "error:" | head -10
git add kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/settings/behavior/
git commit -m "feat(settings): add BehaviorScreen and PrivacySecurityScreen"
```

---

### Task 12: `DataManagementScreen`

**Files:**

- Create: `presentation/settings/data/DataManagementScreen.kt`

- [ ] **Step 1: Create `DataManagementScreen.kt`**

```kotlin
package com.programmersbox.kmpuiviews.presentation.settings.data

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.programmersbox.kmpuiviews.presentation.components.settings.CategoryGroupListItem
import com.programmersbox.kmpuiviews.presentation.settings.SettingsScaffold
import com.programmersbox.kmpuiviews.utils.ComposeSettingsDsl
import com.programmersbox.kmpuiviews.utils.LocalNavActions
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DataManagementScreen(
    composeSettingsDsl: ComposeSettingsDsl = koinInject(),
) {
    val navActions = LocalNavActions.current

    SettingsScaffold(
        title = "Data Management",
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CategoryGroupListItem {
            segmentedListItem(
                content = { Text("Backup & Restore") },
                leadingContent = { Icon(Icons.Default.Backup, null) },
                onClick = navActions::moreSettings,
            )
            segmentedListItem(
                content = { Text("Account") },
                leadingContent = { Icon(Icons.Default.AccountCircle, null) },
                onClick = navActions::accountInfo,
            )
        }

        composeSettingsDsl.dataSettings()
    }
}
```

- [ ] **Step 2: Build + commit**

```bash
./gradlew :kmpuiviews:compileKotlinAndroid 2>&1 | grep "error:" | head -10
git add kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/settings/data/DataManagementScreen.kt
git commit -m "feat(settings): add DataManagementScreen hub"
```

---

### Task 13: About cluster — `AboutScreen`, `DiagnosticsScreen`, `DeveloperScreen`

**Files:**

- Create: `presentation/settings/about/AboutScreen.kt`
- Create: `presentation/settings/about/DiagnosticsScreen.kt`
- Create: `presentation/settings/about/DeveloperScreen.kt`

- [ ] **Step 1: Create `DiagnosticsScreen.kt`**

```kotlin
package com.programmersbox.kmpuiviews.presentation.settings.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.kmpuiviews.presentation.components.settings.CategoryGroupListItem
import com.programmersbox.kmpuiviews.presentation.settings.SettingsScaffold
import com.programmersbox.kmpuiviews.utils.LocalNavActions

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DiagnosticsScreen() {
    val navActions = LocalNavActions.current

    SettingsScaffold(
        title = "Diagnostics",
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CategoryGroupListItem {
            segmentedListItem(
                content = { Text("Background Worker Info") },
                leadingContent = { Icon(Icons.Default.Engineering, null) },
                onClick = navActions::workerInfo,
            )
            segmentedListItem(
                content = { Text("Exceptions") },
                leadingContent = { Icon(Icons.Default.Error, null) },
                onClick = { navActions.navigate(Screen.ExceptionScreen) },
            )
        }
    }
}
```

- [ ] **Step 2: Create `DeveloperScreen.kt`**

```kotlin
package com.programmersbox.kmpuiviews.presentation.settings.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Bento
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.kmpuiviews.presentation.components.settings.CategoryGroupListItem
import com.programmersbox.kmpuiviews.presentation.settings.SettingsScaffold
import com.programmersbox.kmpuiviews.utils.AppConfig
import com.programmersbox.kmpuiviews.utils.LocalNavActions
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DeveloperScreen() {
    val navActions = LocalNavActions.current
    val appConfig: AppConfig = koinInject()

    SettingsScaffold(
        title = "Developer",
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CategoryGroupListItem {
            if (appConfig.isDebug) {
                segmentedListItem(
                    content = { Text("Debug Menu") },
                    leadingContent = { Icon(Icons.Default.Android, null) },
                    onClick = navActions::debug,
                )
            }
            segmentedListItem(
                content = { Text("Pre-release Builds") },
                leadingContent = { Icon(Icons.Default.Bento, null) },
                onClick = navActions::prerelease,
            )
            segmentedListItem(
                content = { Text("Color Helper") },
                leadingContent = { Icon(Icons.Default.Colorize, null) },
                onClick = { navActions.navigate(Screen.ColorHelper) },
            )
        }
    }
}
```

- [ ] **Step 3: Create `AboutScreen.kt`**

```kotlin
package com.programmersbox.kmpuiviews.presentation.settings.about

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CatchingPokemon
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.programmersbox.kmpuiviews.BuildKonfig
import com.programmersbox.kmpuiviews.appVersion
import com.programmersbox.kmpuiviews.domain.AppUpdate
import com.programmersbox.kmpuiviews.domain.AppUpdateCheck
import com.programmersbox.kmpuiviews.painterLogo
import com.programmersbox.kmpuiviews.platform
import com.programmersbox.kmpuiviews.presentation.components.settings.CategoryGroupListItem
import com.programmersbox.kmpuiviews.presentation.settings.SettingsScaffold
import com.programmersbox.kmpuiviews.presentation.settings.moreinfo.MoreInfoViewModel
import com.programmersbox.kmpuiviews.utils.AppConfig
import com.programmersbox.kmpuiviews.utils.LocalNavActions
import com.programmersbox.kmpuiviews.utils.composables.icons.Discord
import com.programmersbox.kmpuiviews.utils.composables.icons.Github
import com.programmersbox.kmpuiviews.versionCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.currentVersion
import otakuworld.kmpuiviews.generated.resources.gotoBrowser
import otakuworld.kmpuiviews.generated.resources.join_discord
import otakuworld.kmpuiviews.generated.resources.notNow
import otakuworld.kmpuiviews.generated.resources.please_update_for_latest_features
import otakuworld.kmpuiviews.generated.resources.support
import otakuworld.kmpuiviews.generated.resources.support_summary
import otakuworld.kmpuiviews.generated.resources.update
import otakuworld.kmpuiviews.generated.resources.update_available
import otakuworld.kmpuiviews.generated.resources.updateTo
import otakuworld.kmpuiviews.generated.resources.view_libraries_used
import otakuworld.kmpuiviews.generated.resources.view_on_github

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AboutScreen(
    composeSettingsDsl: com.programmersbox.kmpuiviews.utils.ComposeSettingsDsl = koinInject(),
    infoViewModel: MoreInfoViewModel = koinViewModel(),
    usedLibraryClick: () -> Unit,
    onViewAccountInfoClick: () -> Unit,
) {
    val navActions = LocalNavActions.current
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    val appConfig: AppConfig = koinInject()
    val appUpdateCheck: AppUpdateCheck = koinInject()
    val appUpdate by appUpdateCheck.updateAppCheck.collectAsStateWithLifecycle(null)
    val appVersion = appVersion()
    var showUpdateDialog by remember { mutableStateOf(false) }

    if (showUpdateDialog) {
        AlertDialog(
            onDismissRequest = { showUpdateDialog = false },
            title = {
                Text(
                    stringResource(
                        Res.string.updateTo,
                        appUpdate?.updateRealVersion.orEmpty()
                    )
                )
            },
            text = { Text(stringResource(Res.string.please_update_for_latest_features)) },
            confirmButton = {
                TextButton(onClick = {
                    showUpdateDialog = false
                }) { Text(stringResource(Res.string.update)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showUpdateDialog = false
                }) { Text(stringResource(Res.string.notNow)) }
                TextButton(onClick = {
                    uriHandler.openUri("https://github.com/jakepurple13/OtakuWorld/releases/latest")
                    showUpdateDialog = false
                }) { Text(stringResource(Res.string.gotoBrowser)) }
            }
        )
    }

    SettingsScaffold(
        title = "About",
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CategoryGroupListItem {
            segmentedListItem(
                leadingContent = {
                    Image(
                        painterLogo(), null,
                        modifier = Modifier.size(24.dp).clip(CircleShape)
                    )
                },
                overlineContent = { Text(platform()) },
                content = { Text(stringResource(Res.string.currentVersion, appVersion)) },
                supportingContent = { Text("Version code: ${versionCode()}") },
                onClick = { scope.launch(Dispatchers.IO) { infoViewModel.updateChecker() } },
            )

            if (AppUpdate.checkForUpdate(appVersion, appUpdate?.updateRealVersion.orEmpty())) {
                segmentedListItem(
                    content = { Text(stringResource(Res.string.update_available)) },
                    supportingContent = {
                        Text(
                            stringResource(
                                Res.string.updateTo,
                                appUpdate?.updateRealVersion.orEmpty()
                            )
                        )
                    },
                    leadingContent = {
                        Icon(Icons.Default.SystemUpdateAlt, null, tint = Color(0xFF00E676))
                    },
                    onClick = { showUpdateDialog = true },
                )
            }

            segmentedListItem(
                content = { Text("View Onboarding Again") },
                leadingContent = { Icon(Icons.Default.CatchingPokemon, null) },
                onClick = { navActions.toOnboarding() },
            )
            segmentedListItem(
                content = { Text(stringResource(Res.string.view_libraries_used)) },
                leadingContent = { Icon(Icons.AutoMirrored.Filled.LibraryBooks, null) },
                onClick = usedLibraryClick,
            )
        }

        CategoryGroupListItem {
            segmentedListItem(
                content = { Text(stringResource(Res.string.view_on_github)) },
                leadingContent = { Icon(Icons.Github, null) },
                onClick = { uriHandler.openUri("https://github.com/jakepurple13/OtakuWorld/releases/latest") },
            )
            segmentedListItem(
                content = { Text(stringResource(Res.string.join_discord)) },
                leadingContent = { Icon(Icons.Discord, null) },
                onClick = { uriHandler.openUri("https://discord.gg/MhhHMWqryg") },
            )
            segmentedListItem(
                content = { Text(stringResource(Res.string.support)) },
                supportingContent = { Text(stringResource(Res.string.support_summary)) },
                leadingContent = { Icon(Icons.Default.AttachMoney, null) },
                onClick = { uriHandler.openUri("https://ko-fi.com/V7V3D3JI") },
            )
        }

        CategoryGroupListItem {
            segmentedListItem(
                content = { Text("Diagnostics") },
                leadingContent = { Icon(Icons.Default.Engineering, null) },
                onClick = navActions::diagnostics,
            )
            if (BuildKonfig.IS_PRERELEASE || appConfig.isDebug) {
                segmentedListItem(
                    content = { Text("Developer") },
                    leadingContent = { Icon(Icons.Default.BugReport, null) },
                    onClick = navActions::developer,
                )
            }
        }

        CategoryGroupListItem {
            apply(composeSettingsDsl.aboutSettings)
        }
    }
}
```

- [ ] **Step 4: Build + commit**

```bash
./gradlew :kmpuiviews:compileKotlinAndroid 2>&1 | grep "error:" | head -20
git add kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/settings/about/
git commit -m "feat(settings): add AboutScreen, DiagnosticsScreen, DeveloperScreen"
```

---

### Task 14: Root `SettingScreen` restructure with `AppBarWithSearch`

**Files:**

- Modify: `presentation/settings/SettingScreen.kt`
- Modify: `presentation/settings/SettingViewModel.kt`

- [ ] **Step 1: Read current `SettingViewModel.kt`**

Open `presentation/settings/SettingViewModel.kt` to understand existing state (likely
`savedNotifications: Int` and `canCheck`-related state).

- [ ] **Step 2: Rewrite `SettingScreen.kt`**

Replace the entire file with:

```kotlin
package com.programmersbox.kmpuiviews.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.GetApp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Source
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.kmpuiviews.presentation.components.DynamicSearchBar
import com.programmersbox.kmpuiviews.presentation.components.OtakuScaffold
import com.programmersbox.kmpuiviews.presentation.components.settings.CategoryGroupListItem
import com.programmersbox.kmpuiviews.presentation.navactions.NavigationActions
import com.programmersbox.kmpuiviews.presentation.settings.search.SettingsSearchRegistry
import com.programmersbox.kmpuiviews.utils.ComposeSettingsDsl
import com.programmersbox.kmpuiviews.utils.LocalNavActions
import com.programmersbox.supabaseintegration.ui.SyncIconComposable
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.settings

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalComposeUiApi::class,
    ExperimentalMaterial3ExpressiveApi::class
)
@Composable
fun SettingScreen(
    composeSettingsDsl: ComposeSettingsDsl,
    navigationActions: NavigationActions = LocalNavActions.current,
    accountSettings: @Composable () -> Unit = {},
) {
    val searchRegistry: SettingsSearchRegistry = koinInject()
    val textFieldState: TextFieldState = rememberTextFieldState()
    val searchBarState = rememberSearchBarState()

    val searchResults by remember {
        derivedStateOf { searchRegistry.search(textFieldState.text.toString()) }
    }

    OtakuScaffold(
        topBar = {
            DynamicSearchBar(
                textFieldState = textFieldState,
                onSearch = {},
                searchBarState = searchBarState,
                placeholder = { Text(stringResource(Res.string.settings)) },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                actions = { SyncIconComposable(modifier = Modifier.padding(horizontal = 16.dp)) },
                isDocked = false,
                scrollBehavior = SearchBarDefaults.enterAlwaysScrollBehavior(searchBarState),
            ) {
                // Search results in ExpandedFullScreenSearchBar
                searchResults.forEach { item ->
                    ListItem(
                        headlineContent = { Text(item.displayName) },
                        supportingContent = {
                            val crumb = remember(item.breadcrumb) {
                                com.programmersbox.kmpuiviews.presentation.settings.search
                                    .SettingsScreenDisplayNames.breadcrumbText(item.breadcrumb)
                            }
                            Text(crumb)
                        },
                        leadingContent = { Icon(Icons.Default.Search, null) },
                        onClick = {
                            val highlightState: com.programmersbox.kmpuiviews.presentation.settings.search.SettingsHighlightState =
                                koinInject()
                            highlightState.pendingHighlightKey = item.highlightKey
                            searchBarState.animateToCollapsed()
                            navigationActions.navigate(item.targetScreen)
                        },
                    )
                }
            }
        },
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets,
    ) { p ->
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(p)
        ) {
            // Quick Actions
            CategoryGroupListItem {
                segmentedListItem(
                    content = { Text("Scan QR Code") },
                    leadingContent = { Icon(Icons.Default.QrCodeScanner, null) },
                    onClick = navigationActions::scanQrCode,
                )
                segmentedListItem(
                    content = { Text("Global Search") },
                    leadingContent = { Icon(Icons.Default.Search, null) },
                    onClick = navigationActions::globalSearch,
                )
                segmentedListItem(
                    content = { Text("App Downloads") },
                    leadingContent = { Icon(Icons.Default.GetApp, null) },
                    onClick = navigationActions::downloadInstall,
                )
                apply(composeSettingsDsl.quickActionsSettings)
            }

            // App-level viewSettings injection (preserved from previous SettingScreen)
            CategoryGroupListItem {
                apply(composeSettingsDsl.viewSettings)
            }

            // Main categories
            CategoryGroupListItem {
                segmentedListItem(
                    content = { Text("Library") },
                    leadingContent = { Icon(Icons.Default.Star, null) },
                    onClick = navigationActions::library,
                )
                segmentedListItem(
                    content = { Text("Discover") },
                    leadingContent = { Icon(Icons.Default.Widgets, null) },
                    onClick = navigationActions::discover,
                )
                segmentedListItem(
                    content = { Text("Sources & Extensions") },
                    leadingContent = { Icon(Icons.Default.Source, null) },
                    onClick = navigationActions::sources,
                )
                segmentedListItem(
                    content = { Text("Integrations") },
                    leadingContent = { Icon(Icons.Default.DataObject, null) },
                    onClick = navigationActions::integrations,
                )
                segmentedListItem(
                    content = { Text("Appearance") },
                    leadingContent = { Icon(Icons.Default.Palette, null) },
                    onClick = navigationActions::appearance,
                )
                segmentedListItem(
                    content = { Text("Behavior") },
                    leadingContent = { Icon(Icons.Default.Settings, null) },
                    onClick = navigationActions::behaviorSettings,
                )
                segmentedListItem(
                    content = { Text("Data Management") },
                    leadingContent = { Icon(Icons.Default.DataObject, null) },
                    onClick = navigationActions::dataManagement,
                )
                segmentedListItem(
                    content = { Text("About") },
                    leadingContent = { Icon(Icons.Default.Info, null) },
                    onClick = navigationActions::aboutSettings,
                )
            }
        }
    }
}
```

- [ ] **Step 3: Build**

```bash
./gradlew :kmpuiviews:compileKotlinAndroid 2>&1 | grep "error:" | head -20
```

- [ ] **Step 4: Commit**

```bash
git add kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/settings/SettingScreen.kt
git commit -m "feat(settings): restructure root SettingScreen with AppBarWithSearch and new hierarchy"
```

---

### Task 15: `Nav3Graph` entries + built-in search items

**Files:**

- Modify: `presentation/navigation/Nav3Graph.kt`
- Modify: `presentation/settings/search/SettingsSearchRegistry.kt` (complete
  `builtInSettingsItems()`)

This is the wiring task — all screens are registered with Navigation 3 and the search index is
populated.

- [ ] **Step 1: Add nav entries to `Nav3Graph.kt`**

Find the settings section in `Nav3Graph.kt` (currently has entries for `Screen.SettingsScreen`,
`Screen.NotificationsSettings`, `Screen.GeneralSettings`, etc.). Add entries for all new screens:

```kotlin
// Add these inside entryGraph() or settingsNav3Setup() — whichever block 
// the existing settings entries live in:

detailEntry<Screen.Settings.Library> { LibraryScreen() }
detailEntry<Screen.Settings.Discover> { DiscoverScreen() }
detailEntry<Screen.Settings.Sources> { SourcesScreen() }
detailEntry<Screen.Settings.Integrations> { IntegrationsScreen() }
detailEntry<Screen.Settings.Appearance> { AppearanceScreen() }
detailEntry<Screen.Settings.Colors> { ColorsScreen() }
detailEntry<Screen.Settings.Behavior> { BehaviorScreen() }
detailEntry<Screen.Settings.Layout> { LayoutScreen() }
detailEntry<Screen.Settings.ContentReading> { ContentReadingScreen() }
detailEntry<Screen.Settings.PrivacySecurity> { PrivacySecurityScreen() }
detailEntry<Screen.Settings.Data> { DataManagementScreen() }
detailEntry<Screen.Settings.About> {
    AboutScreen(
        usedLibraryClick = { /* keep existing usedLibrary nav */ },
        onViewAccountInfoClick = { navActions.accountInfo() },
    )
}
detailEntry<Screen.Settings.Diagnostics> { DiagnosticsScreen() }
detailEntry<Screen.Settings.Developer> { DeveloperScreen() }
```

> Find the existing pattern for `detailEntry` / `twoPaneEntry` in `Nav3Graph.kt` and match it
> exactly — the function name may differ (`detailEntry`, `twoPaneEntry`, `NavEntry`, etc.).

- [ ] **Step 2: Complete `builtInSettingsItems()` in `SettingsSearchRegistry.kt`**

Replace the stub `builtInSettingsItems()` function:

```kotlin
fun builtInSettingsItems(): List<SettingsSearchItem> = listOf(
    // ── Appearance > Theme ──────────────────────────────────
    SettingsSearchItem(
        displayName = "Light / Dark / System Mode",
        keywords = listOf("theme", "dark", "light", "night", "system"),
        breadcrumb = listOf(Screen.Settings.Appearance, Screen.ThemeSettings),
        targetScreen = Screen.ThemeSettings,
        highlightKey = "theme_mode",
    ),
    SettingsSearchItem(
        displayName = "Theme Color",
        keywords = listOf("color", "seed", "palette", "material you"),
        breadcrumb = listOf(Screen.Settings.Appearance, Screen.ThemeSettings),
        targetScreen = Screen.ThemeSettings,
        highlightKey = "theme_color",
    ),
    SettingsSearchItem(
        displayName = "AMOLED Mode",
        keywords = listOf("amoled", "black", "oled", "dark"),
        breadcrumb = listOf(Screen.Settings.Appearance, Screen.ThemeSettings),
        targetScreen = Screen.ThemeSettings,
        highlightKey = "amoled_mode",
    ),
    SettingsSearchItem(
        displayName = "Expressiveness",
        keywords = listOf("animation", "expressive", "motion"),
        breadcrumb = listOf(Screen.Settings.Appearance, Screen.ThemeSettings),
        targetScreen = Screen.ThemeSettings,
        highlightKey = "expressiveness",
    ),
    // ── Appearance > Colors ──────────────────────────────────
    SettingsSearchItem(
        displayName = "Color Blindness",
        keywords = listOf("protanopia", "deuteranopia", "tritanopia", "accessibility"),
        breadcrumb = listOf(Screen.Settings.Appearance, Screen.Settings.Colors),
        targetScreen = Screen.Settings.Colors,
        highlightKey = "color_blindness",
    ),
    SettingsSearchItem(
        displayName = "Use Palette",
        keywords = listOf("palette", "image color", "dynamic color"),
        breadcrumb = listOf(Screen.Settings.Appearance, Screen.Settings.Colors),
        targetScreen = Screen.Settings.Colors,
        highlightKey = "use_palette",
    ),
    SettingsSearchItem(
        displayName = "Swatch Type",
        keywords = listOf("swatch", "vibrant", "muted"),
        breadcrumb = listOf(Screen.Settings.Appearance, Screen.Settings.Colors),
        targetScreen = Screen.Settings.Colors,
        highlightKey = "swatch_type",
    ),
    SettingsSearchItem(
        displayName = "Swatch Style",
        keywords = listOf("swatch style", "palette style"),
        breadcrumb = listOf(Screen.Settings.Appearance, Screen.Settings.Colors),
        targetScreen = Screen.Settings.Colors,
        highlightKey = "swatch_style",
    ),
    // ── Appearance > Blur ────────────────────────────────────
    SettingsSearchItem(
        displayName = "Show Blur",
        keywords = listOf("blur", "glassmorphism", "glass", "haze"),
        breadcrumb = listOf(Screen.Settings.Appearance, Screen.Settings.Blur),
        targetScreen = Screen.Settings.Blur,
        highlightKey = "show_blur",
    ),
    SettingsSearchItem(
        displayName = "Blur Kind",
        keywords = listOf("blur kind", "haze", "liquid glass"),
        breadcrumb = listOf(Screen.Settings.Appearance, Screen.Settings.Blur),
        targetScreen = Screen.Settings.Blur,
        highlightKey = "blur_kind",
    ),
    // ── Behavior > Layout ────────────────────────────────────
    SettingsSearchItem(
        displayName = "Grid Type",
        keywords = listOf("grid", "columns", "adaptive", "fixed"),
        breadcrumb = listOf(Screen.Settings.Behavior, Screen.Settings.Layout),
        targetScreen = Screen.Settings.Layout,
        highlightKey = "grid_type",
    ),
    SettingsSearchItem(
        displayName = "Detail Pane",
        keywords = listOf("detail pane", "split", "two pane"),
        breadcrumb = listOf(Screen.Settings.Behavior, Screen.Settings.Layout),
        targetScreen = Screen.Settings.Layout,
        highlightKey = "detail_pane",
    ),
    SettingsSearchItem(
        displayName = "Floating Navigation",
        keywords = listOf("floating", "nav bar", "navigation"),
        breadcrumb = listOf(Screen.Settings.Behavior, Screen.Settings.Layout),
        targetScreen = Screen.Settings.Layout,
        highlightKey = "floating_navigation",
    ),
    SettingsSearchItem(
        displayName = "Middle Navigation Action",
        keywords = listOf("middle button", "navigation action", "center"),
        breadcrumb = listOf(Screen.Settings.Behavior, Screen.Settings.Layout),
        targetScreen = Screen.Settings.Layout,
        highlightKey = "middle_nav_action",
    ),
    // ── Behavior > Content & Reading ─────────────────────────
    SettingsSearchItem(
        displayName = "History Save Count",
        keywords = listOf("history", "save", "limit", "count"),
        breadcrumb = listOf(Screen.Settings.Behavior, Screen.Settings.ContentReading),
        targetScreen = Screen.Settings.ContentReading,
        highlightKey = "history_save",
    ),
    // ── Behavior > Content & Reading > Details ───────────────
    SettingsSearchItem(
        displayName = "Chapter Swipe: Start to End",
        keywords = listOf("swipe", "chapter", "gesture"),
        breadcrumb = listOf(
            Screen.Settings.Behavior,
            Screen.Settings.ContentReading,
            Screen.DetailsSettings
        ),
        targetScreen = Screen.DetailsSettings,
        highlightKey = "swipe_start_end",
    ),
    SettingsSearchItem(
        displayName = "Chapter Swipe: End to Start",
        keywords = listOf("swipe", "chapter", "gesture"),
        breadcrumb = listOf(
            Screen.Settings.Behavior,
            Screen.Settings.ContentReading,
            Screen.DetailsSettings
        ),
        targetScreen = Screen.DetailsSettings,
        highlightKey = "swipe_end_start",
    ),
    SettingsSearchItem(
        displayName = "Share Chapters",
        keywords = listOf("share", "chapter"),
        breadcrumb = listOf(
            Screen.Settings.Behavior,
            Screen.Settings.ContentReading,
            Screen.DetailsSettings
        ),
        targetScreen = Screen.DetailsSettings,
        highlightKey = "share_chapters",
    ),
    SettingsSearchItem(
        displayName = "Show Download Button",
        keywords = listOf("download", "button"),
        breadcrumb = listOf(
            Screen.Settings.Behavior,
            Screen.Settings.ContentReading,
            Screen.DetailsSettings
        ),
        targetScreen = Screen.DetailsSettings,
        highlightKey = "show_download",
    ),
    // ── Behavior > Content & Reading > Player ─────────────────
    SettingsSearchItem(
        displayName = "Battery Alert Percentage",
        keywords = listOf("battery", "alert", "low battery"),
        breadcrumb = listOf(
            Screen.Settings.Behavior,
            Screen.Settings.ContentReading,
            Screen.OtherSettings
        ),
        targetScreen = Screen.OtherSettings,
        highlightKey = "battery_percent",
    ),
    // ── Behavior > Notifications ─────────────────────────────
    SettingsSearchItem(
        displayName = "Enable Periodic Updates",
        keywords = listOf("notifications", "update check", "background"),
        breadcrumb = listOf(Screen.Settings.Behavior, Screen.NotificationsSettings),
        targetScreen = Screen.NotificationsSettings,
        highlightKey = "periodic_updates",
    ),
    SettingsSearchItem(
        displayName = "Check Interval",
        keywords = listOf("check interval", "hours", "frequency"),
        breadcrumb = listOf(Screen.Settings.Behavior, Screen.NotificationsSettings),
        targetScreen = Screen.NotificationsSettings,
        highlightKey = "check_interval",
    ),
    SettingsSearchItem(
        displayName = "Network Type",
        keywords = listOf("wifi", "metered", "unmetered", "network"),
        breadcrumb = listOf(Screen.Settings.Behavior, Screen.NotificationsSettings),
        targetScreen = Screen.NotificationsSettings,
        highlightKey = "network_type",
    ),
    SettingsSearchItem(
        displayName = "Only Run When Charging",
        keywords = listOf("charging", "battery", "power"),
        breadcrumb = listOf(Screen.Settings.Behavior, Screen.NotificationsSettings),
        targetScreen = Screen.NotificationsSettings,
        highlightKey = "requires_charging",
    ),
    SettingsSearchItem(
        displayName = "Don't Run on Low Battery",
        keywords = listOf("low battery", "battery not low"),
        breadcrumb = listOf(Screen.Settings.Behavior, Screen.NotificationsSettings),
        targetScreen = Screen.NotificationsSettings,
        highlightKey = "battery_not_low",
    ),
    SettingsSearchItem(
        displayName = "Notify on Boot",
        keywords = listOf("boot", "startup", "restart"),
        breadcrumb = listOf(Screen.Settings.Behavior, Screen.NotificationsSettings),
        targetScreen = Screen.NotificationsSettings,
        highlightKey = "notify_boot",
    ),
    // ── Behavior > Privacy & Security ────────────────────────
    SettingsSearchItem(
        displayName = "Biometric Lock",
        keywords = listOf("biometric", "fingerprint", "face", "lock", "pin"),
        breadcrumb = listOf(
            Screen.Settings.Behavior,
            Screen.Settings.PrivacySecurity,
            Screen.SecuritySettings
        ),
        targetScreen = Screen.SecuritySettings,
        highlightKey = "biometric",
    ),
    SettingsSearchItem(
        displayName = "Incognito Sources",
        keywords = listOf("incognito", "private", "history", "tracking"),
        breadcrumb = listOf(
            Screen.Settings.Behavior,
            Screen.Settings.PrivacySecurity,
            Screen.IncognitoScreen
        ),
        targetScreen = Screen.IncognitoScreen,
        highlightKey = "incognito",
    ),
    // ── Data Management ──────────────────────────────────────
    SettingsSearchItem(
        displayName = "Create Full Backup",
        keywords = listOf("backup", "export", "save data"),
        breadcrumb = listOf(Screen.Settings.Data, Screen.MoreSettings),
        targetScreen = Screen.MoreSettings,
        highlightKey = "create_backup",
    ),
    SettingsSearchItem(
        displayName = "Restore Full Backup",
        keywords = listOf("restore", "import", "backup"),
        breadcrumb = listOf(Screen.Settings.Data, Screen.MoreSettings),
        targetScreen = Screen.MoreSettings,
        highlightKey = "restore_backup",
    ),
)
```

> This requires `Screen.OtherSettings` to exist. Check `Screen.kt` — it may be named
`Screen.OtherSettings` or differ. Match what exists.

- [ ] **Step 3: Build full app**

```bash
./gradlew :mangaworld:assembleNoFirebaseDebug 2>&1 | grep -E "error:|BUILD" | tail -20
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Run all tests**

```bash
./gradlew :kmpuiviews:test 2>&1 | tail -20
```

Expected: all tests pass.

- [ ] **Step 5: Commit**

```bash
git add kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/navigation/Nav3Graph.kt \
        kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/settings/search/SettingsSearchRegistry.kt
git commit -m "feat(settings): wire Nav3Graph entries and complete built-in search index"
```

---

## Self-Review

**Spec coverage check:**

| Spec requirement                                          | Task                                                 |
|-----------------------------------------------------------|------------------------------------------------------|
| Deeply nested hierarchy with 8 top-level categories       | Tasks 6–13                                           |
| Quick Actions at root                                     | Task 14                                              |
| Separate feature entry points from settings               | Tasks 6–8 (Library, Discover, Sources, Integrations) |
| AppBarWithSearch with ExpandedFullScreenSearchBar         | Task 14                                              |
| Search results show name + breadcrumb                     | Task 14                                              |
| Tap result → navigate + highlight                         | Task 14 (navigate), Task 3 (highlight)               |
| `HighlightableSettingRow` with BringIntoView + color fade | Task 3                                               |
| `SettingsHighlightState` for one-shot highlight key       | Task 1 + Task 3                                      |
| `ComposeSettingsDsl` expansion with 11 new slots          | Task 4                                               |
| `SettingsSearchItem` with `breadcrumb: List<NavKey>`      | Task 1                                               |
| `SettingsSearchRegistry` Koin singleton                   | Task 2 + Task 5                                      |
| `SettingsScreenDisplayNames` display name map             | Task 1                                               |
| Navigation 3 entries for all new screens                  | Task 15                                              |
| Built-in search items list                                | Task 15                                              |
| `ColorsScreen` extracted from ThemeSettingsScreen         | Task 7                                               |
| Layout settings extracted from GeneralSettings            | Task 9                                               |
| ContentReading extracted from GeneralSettings             | Task 10                                              |
| About cluster (About, Diagnostics, Developer)             | Task 13                                              |
| Screen.Settings.* new subclasses                          | Task 5                                               |
| NavigationActions new methods                             | Task 5                                               |

**Placeholder scan:** No TBD/TODO/incomplete steps found. All code blocks are complete.

**Type consistency check:**

- `SettingsSearchItem.breadcrumb: List<NavKey>` — used consistently in Tasks 1, 2, 15
- `SettingsHighlightState.pendingHighlightKey: String?` — set in Task 14, consumed in Task 3 via
  `rememberActiveHighlight`
- `SettingsSearchRegistry(items: List<SettingsSearchItem>)` — matches test in Task 2 and Koin
  registration in Task 5
- `ComposeSettingsDsl.quickActionsSettings: CategoryGroupScope.() -> Unit` — matches usage in Task
  14
- All `NavigationActions` methods added in Task 5 match call sites in Tasks 6–14

> **One gap to resolve at implementation time:** Task 14's search result `onClick` calls
`koinInject()` inside a lambda. In Compose, `koinInject()` must be called at the composable scope,
> not inside a lambda. Refactor: inject `highlightState` at the composable level —
`val highlightState: SettingsHighlightState = koinInject()` — and capture it in the lambda closure.
