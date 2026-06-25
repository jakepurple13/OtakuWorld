# Settings Reorganization Design

**Date:** 2026-06-25  
**Module:** `:kmpuiviews`  
**Scope:** Structural and navigational only — no new functional features, no design system changes.

---

## Goals

1. Replace the current flat settings list with a deeply nested, logically grouped hierarchy.
2. Separate feature entry points (Library, Discover, Sources, Integrations) from actual configuration settings.
3. Add a Quick Actions section at the root for high-frequency shortcuts.
4. Implement cross-tree searchable settings via `AppBarWithSearch` + `ExpandedFullScreenSearchBar`.
5. Extend `ComposeSettingsDsl` so each app (MangaWorld, AnimeWorld, NovelWorld, Desktop) can inject custom items into any tree section and register custom search items.

---

## Settings Tree

```
Settings (root)
│  [AppBarWithSearch → ExpandedFullScreenSearchBar]
│  [SyncIconComposable — existing top-bar action]
│
├── ⚡ Quick Actions               (inline on root, no sub-screen)
│   ├── Scan QR Code
│   ├── Global Search
│   ├── App Downloads
│   └── [quickActionsSettings injection]
│
├── [viewSettings injection]       (existing — after Quick Actions group)
│
├── 📚 Library                    → Screen.Settings.Library
│   ├── Favorites
│   ├── History
│   ├── Bookmarks
│   ├── Notes
│   ├── Custom Lists
│   ├── Saved Notifications (inbox)
│   └── [librarySettings injection]
│
├── 🔍 Discover                   → Screen.Settings.Discover
│   ├── AI Recommendations
│   ├── URL Opener
│   └── [discoverSettings injection]
│
├── 🔌 Sources & Extensions       → Screen.Settings.Sources
│   ├── Current Source (chooser sheet)
│   ├── Source Order
│   ├── Extensions
│   ├── View Source in Browser (external URI)
│   └── [sourcesSettings injection]
│
├── 🔗 Integrations               → Screen.Settings.Integrations
│   ├── Supabase (account/sync)
│   ├── Translation Models
│   └── [integrationsSettings injection]
│
├── 🎨 Appearance                 → Screen.Settings.Appearance
│   ├── Theme                     → Screen.ThemeSettings (existing)
│   │   ├── Light / Dark / System mode
│   │   ├── Theme Color
│   │   ├── AMOLED Mode
│   │   └── Expressiveness
│   ├── Colors                    → Screen.Settings.Colors (new)
│   │   ├── Color Blindness
│   │   ├── Use Palette
│   │   ├── Swatch Type           (visible when Use Palette = true)
│   │   └── Swatch Style          (visible when Use Palette = true)
│   ├── Blur Effects              → Screen.Settings.Blur (existing)
│   │   ├── Show Blur
│   │   ├── Blur Kind (Haze / LiquidGlass)
│   │   ├── [Haze] Progressive Blur
│   │   └── [LiquidGlass] Amount, Refraction Height, Refraction Amount, Depth Effect, Chromatic Aberration
│   └── [appearanceSettings injection]
│
├── ⚙️ Behavior                   → Screen.Settings.Behavior
│   ├── Layout                    → Screen.Settings.Layout (new)
│   │   ├── Grid Type
│   │   ├── Detail Pane
│   │   ├── Floating Navigation
│   │   ├── Middle Navigation Action
│   │   ├── Multiple Actions Config (visible when Middle Action = Multiple)
│   │   └── [layoutSettings injection]
│   ├── Content & Reading         → Screen.Settings.ContentReading (new)
│   │   ├── Details               → Screen.DetailsSettings (existing)
│   │   │   ├── Chapter Swipe: Start-to-End
│   │   │   ├── Chapter Swipe: End-to-Start
│   │   │   ├── Share Chapters
│   │   │   └── Show Download Button
│   │   ├── History Save Count    (slider, was in GeneralSettings)
│   │   ├── Player / App-Specific → Screen.OtherSettings (existing PlaySettings)
│   │   │   ├── Battery Alert %
│   │   │   └── [playerSettings injection]
│   │   ├── [generalSettings injection]   (existing app-specific slot, preserved)
│   │   └── [contentReadingSettings injection]
│   ├── Notifications             → Screen.NotificationsSettings (existing)
│   │   ├── Delete Saved Notifications
│   │   ├── Last Update Check (manual trigger)
│   │   ├── Enable Periodic Updates
│   │   ├── Check Interval (slider, 1–24h)
│   │   ├── Network Type
│   │   ├── Only When Charging
│   │   ├── Don't Run on Low Battery
│   │   ├── Worker Info items
│   │   ├── Clear Update Queue
│   │   └── Notify on Boot
│   ├── Privacy & Security        → Screen.Settings.PrivacySecurity (new)
│   │   ├── Biometric Lock        → Screen.SecuritySettings (existing)
│   │   └── Incognito Sources     → Screen.IncognitoScreen (existing)
│   └── [behaviorSettings injection]
│
├── 💾 Data Management            → Screen.Settings.Data (new)
│   ├── Backup & Restore          → Screen.MoreSettings (existing)
│   │   ├── Create Full Backup
│   │   └── Restore Full Backup
│   ├── Account                   → Screen.AccountInfo (existing)
│   └── [dataSettings injection]
│
└── ℹ️ About                      → Screen.Settings.About (new)
    ├── App Version (tap to check update)
    ├── View Onboarding Again
    ├── Libraries Used
    ├── GitHub
    ├── Discord
    ├── Support (Ko-fi)
    ├── Update Available banner    (conditional — when update exists)
    ├── Diagnostics               → Screen.Settings.Diagnostics (new)
    │   ├── Worker Info           → Screen.WorkerInfoScreen (existing)
    │   └── Exceptions            → Screen.ExceptionScreen (existing)
    ├── Developer                 → Screen.Settings.Developer (new, conditional: isDebug || IS_PRERELEASE)
    │   ├── Debug Menu            → Screen.DebugScreen (isDebug only)
    │   ├── Pre-release Builds    → Screen.PrereleaseScreen (existing)
    │   └── Color Helper          → Screen.ColorHelper (existing)
    └── [aboutSettings injection]
```

---

## New `Screen` Sealed Subclasses

Add to `Screen.kt` — all nested under `Screen.Settings`:

```kotlin
@Serializable sealed class Screen {
    @Serializable data object Settings : Screen("settings") {
        @Serializable data object Blur : Screen("blur")          // existing, moved here
        @Serializable data object Library : Screen("library")
        @Serializable data object Discover : Screen("discover")
        @Serializable data object Sources : Screen("sources")
        @Serializable data object Integrations : Screen("integrations")
        @Serializable data object Appearance : Screen("appearance")
        @Serializable data object Colors : Screen("colors")
        @Serializable data object Behavior : Screen("behavior")
        @Serializable data object Layout : Screen("layout")
        @Serializable data object ContentReading : Screen("content_reading")
        @Serializable data object PrivacySecurity : Screen("privacy_security")
        @Serializable data object Data : Screen("data")
        @Serializable data object About : Screen("about")
        @Serializable data object Diagnostics : Screen("diagnostics")
        @Serializable data object Developer : Screen("developer")
    }
}
```

Existing `Screen` keys that already exist (`ThemeSettings`, `DetailsSettings`, `NotificationsSettings`, `SecuritySettings`, `MoreSettings`, `AccountInfo`, `WorkerInfoScreen`, `ExceptionScreen`, `PrereleaseScreen`, `ColorHelper`, `IncognitoScreen`) are **reused as-is** — no route changes.

> **Note:** `Screen.Settings.Blur` currently has route string `"home"` (legacy). The spec renames it to `"blur"`. Navigation 3 uses the serialized `NavKey` type identity, not the `route` string, so this is safe. Any code referencing `.route` on `Screen.Settings.Blur` must be updated.

---

## `ComposeSettingsDsl` Changes

Full class with all existing fields preserved and new fields added:

```kotlin
class ComposeSettingsDsl {
    // ── Existing (unchanged) ─────────────────────────────────
    var generalSettings: @Composable () -> Unit = {}
    var viewSettings: CategoryGroupScope.() -> Unit = {}
    var playerSettings: @Composable () -> Unit = {}
    var onboardingSettings: OnboardingScope.() -> Unit = {}

    // ── New: search registry ─────────────────────────────────
    var searchItems: () -> List<SettingsSearchItem> = { emptyList() }

    // ── New: per-section injection ───────────────────────────
    var quickActionsSettings: CategoryGroupScope.() -> Unit = {}
    var librarySettings: CategoryGroupScope.() -> Unit = {}
    var discoverSettings: CategoryGroupScope.() -> Unit = {}
    var sourcesSettings: CategoryGroupScope.() -> Unit = {}
    var integrationsSettings: CategoryGroupScope.() -> Unit = {}
    var appearanceSettings: @Composable () -> Unit = {}
    var behaviorSettings: @Composable () -> Unit = {}
    var layoutSettings: @Composable () -> Unit = {}
    var contentReadingSettings: @Composable () -> Unit = {}
    var dataSettings: @Composable () -> Unit = {}
    var aboutSettings: CategoryGroupScope.() -> Unit = {}

    // ── DSL builder functions (mirrors existing pattern) ─────
    fun searchItems(block: () -> List<SettingsSearchItem>) { searchItems = block }
    fun quickActionsSettings(block: CategoryGroupScope.() -> Unit) { quickActionsSettings = block }
    fun librarySettings(block: CategoryGroupScope.() -> Unit) { librarySettings = block }
    fun discoverSettings(block: CategoryGroupScope.() -> Unit) { discoverSettings = block }
    fun sourcesSettings(block: CategoryGroupScope.() -> Unit) { sourcesSettings = block }
    fun integrationsSettings(block: CategoryGroupScope.() -> Unit) { integrationsSettings = block }
    fun appearanceSettings(block: @Composable () -> Unit) { appearanceSettings = block }
    fun behaviorSettings(block: @Composable () -> Unit) { behaviorSettings = block }
    fun layoutSettings(block: @Composable () -> Unit) { layoutSettings = block }
    fun contentReadingSettings(block: @Composable () -> Unit) { contentReadingSettings = block }
    fun dataSettings(block: @Composable () -> Unit) { dataSettings = block }
    fun aboutSettings(block: CategoryGroupScope.() -> Unit) { aboutSettings = block }
}
```

---

## Search System

### `SettingsSearchItem`

```kotlin
data class SettingsSearchItem(
    val displayName: String,
    val keywords: List<String> = emptyList(),
    val breadcrumb: List<NavKey>, // e.g. [Screen.Settings.Appearance, Screen.Settings.Blur]
    val targetScreen: NavKey,
    val highlightKey: String,     // stable key used for scroll + highlight
)
```

`breadcrumb` is a list of parent `NavKey` screens from root → immediate parent. A `SettingsScreenDisplayNames` object maps each `NavKey` to its human-readable label for rendering as `"Appearance > Blur > Blur Kind"`. Screen renames are caught at compile time — no string drift.

### `SettingsSearchRegistry`

Koin singleton. Built once at startup from:
1. Static built-in list (all shared settings items with their breadcrumbs)
2. `composeSettingsDsl.searchItems()` (app-specific additions)

```kotlin
class SettingsSearchRegistry(composeSettingsDsl: ComposeSettingsDsl) {
    val items: List<SettingsSearchItem> = builtInItems() + composeSettingsDsl.searchItems()

    fun search(query: String): List<SettingsSearchItem> =
        if (query.isBlank()) emptyList()
        else items.filter { item ->
            item.displayName.contains(query, ignoreCase = true)
                || item.keywords.any { it.contains(query, ignoreCase = true) }
        }
}
```

### Search UX

- Root `SettingScreen` uses `DynamicSearchBar` (second overload — `AppBarWithSearch` + `SearchBarState`)
- `SyncIconComposable` moves into `actions` slot of `AppBarWithSearch`
- `ExpandedFullScreenSearchBar` shows filtered results
- Each result row: **display name** (headline) + **breadcrumb** (supporting text)
- Tap → close search → `navigationActions.navigate(targetScreen)` with `highlightKey` as nav arg

---

## Highlight Animation & Scroll-to-Item

### Nav arg threading

New `Screen.Settings.*` types that need highlight support get `highlightKey: String? = null`. Normal navigation omits it (null = no highlight).

### In destination screens

```kotlin
@Composable
fun SomeSettingsScreen(highlightKey: String? = null) {
    val listState = rememberLazyListState()
    val activeHighlight = remember { mutableStateOf(highlightKey) }

    LaunchedEffect(highlightKey) {
        if (highlightKey != null) {
            val index = items.indexOfFirst { it.key == highlightKey }
            if (index >= 0) listState.animateScrollToItem(index)
        }
    }

    LazyColumn(state = listState) {
        items(items, key = { it.key }) { item ->
            HighlightableSettingRow(
                activeHighlight = activeHighlight,
                itemKey = item.key,
            ) { /* setting content */ }
        }
    }
}
```

### `HighlightableSettingRow`

```kotlin
@Composable
fun HighlightableSettingRow(
    activeHighlight: MutableState<String?>,
    itemKey: String,
    content: @Composable () -> Unit,
) {
    val isHighlighted = activeHighlight.value == itemKey
    val highlightColor by animateColorAsState(
        targetValue = if (isHighlighted) MaterialTheme.colorScheme.primaryContainer
                      else Color.Transparent,
        animationSpec = tween(durationMillis = 1500),
        finishedListener = { activeHighlight.value = null },
        label = "settingHighlight"
    )
    Box(modifier = Modifier.background(highlightColor)) { content() }
}
```

For `SettingsScaffold` screens (Column + ScrollState, not lazy): items register their offset via `onGloballyPositioned`, stored in a `Map<String, Int>`. `LaunchedEffect` calls `scrollState.animateScrollTo(offset)`.

---

## New Screens to Create

| Screen | File location | Pattern |
|---|---|---|
| `LibraryScreen` | `settings/library/LibraryScreen.kt` | `SettingsScaffold` + `CategoryGroupListItem` |
| `DiscoverScreen` | `settings/discover/DiscoverScreen.kt` | `SettingsScaffold` + `CategoryGroupListItem` |
| `SourcesScreen` | `settings/sources/SourcesScreen.kt` | `SettingsScaffold` + `CategoryGroupListItem` |
| `IntegrationsScreen` | `settings/integrations/IntegrationsScreen.kt` | `SettingsScaffold` + `CategoryGroupListItem` |
| `AppearanceScreen` | `settings/appearance/AppearanceScreen.kt` | `SettingsScaffold` + `CategoryGroupListItem` |
| `ColorsScreen` | `settings/appearance/ColorsScreen.kt` | `SettingsScaffold` + `CategoryGroup` — contains Palette (Use Palette, Swatch Type, Swatch Style) and Color Blindness, extracted from `ThemeSettingsScreen` |
| `BehaviorScreen` | `settings/behavior/BehaviorScreen.kt` | `SettingsScaffold` + `CategoryGroupListItem` |
| `LayoutScreen` | `settings/behavior/LayoutScreen.kt` | `SettingsScaffold` + `CategoryGroup` |
| `ContentReadingScreen` | `settings/behavior/ContentReadingScreen.kt` | `SettingsScaffold` + `CategoryGroupListItem` |
| `PrivacySecurityScreen` | `settings/behavior/PrivacySecurityScreen.kt` | `SettingsScaffold` + `CategoryGroupListItem` |
| `DataManagementScreen` | `settings/data/DataManagementScreen.kt` | `SettingsScaffold` + `CategoryGroupListItem` |
| `AboutScreen` (settings) | `settings/about/AboutScreen.kt` | `SettingsScaffold` + `CategoryGroupListItem` |
| `DiagnosticsScreen` | `settings/about/DiagnosticsScreen.kt` | `SettingsScaffold` + `CategoryGroupListItem` |
| `DeveloperScreen` | `settings/about/DeveloperScreen.kt` | `SettingsScaffold` + `CategoryGroupListItem` |
| `SettingsSearchRegistry` | `settings/search/SettingsSearchRegistry.kt` | Koin singleton |
| `SettingsSearchItem` | `settings/search/SettingsSearchItem.kt` | Data class |
| `HighlightableSettingRow` | `settings/search/HighlightableSettingRow.kt` | Composable |

All files in `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/settings/`.

---

## Existing Screens Modified

| File | Change |
|---|---|
| `SettingScreen.kt` | Replace content with new root hierarchy + `AppBarWithSearch` |
| `SettingViewModel.kt` | Add search query state + registry query |
| `Screen.kt` | Add new `Screen.Settings.*` subclasses |
| `ComposeSettingsDsl.kt` | Add all new fields + builder functions |
| `Nav3Graph.kt` | Add nav entries for all new screens |
| `GeneralSettings.kt` | Extract `HistorySettings` to `ContentReadingScreen`, rest moved |
| `ThemeSettingsScreen.kt` | Extract palette/color blindness to `ColorsScreen` |

---

## Out of Scope

- Redesigning or altering existing design system components
- Adding new functional settings or features
- Backend / API changes
- Platform-specific UI divergence (Android and Desktop must behave identically)
