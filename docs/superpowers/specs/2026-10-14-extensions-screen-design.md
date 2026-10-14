# Extensions Screen Overhaul ("Catalog Shelves") — Design

Date: 2026-10-14
Status: Approved in chat (proposal B of four HTML mockups), pending spec review
Mockup: https://claude.ai/artifact/Azr23CeM53CTJa648WEec8 (tab "B · Catalog shelves")

## Goal

Replace the current `ExtensionList` screen (`kmpuiviews/.../settings/extensions/ExtensionListScreen.kt`)
with a clearer Installed / Browse experience:

- Installed answers "what do I have, and what needs my attention" at a glance.
- Browse is a visual, icon-led catalog that scales to repos with 1,000+ extensions.
- Install progress is visible (today `DownloadStateInterface.downloadList` is never read; see the
  `TODO` in the current screen).

Success: a user can see update/untrusted counts on entry, update everything in two taps, find any
remote extension through one search, see what an extension contains before installing, and watch
install progress without leaving the screen.

## Non-goals

- No change to how extensions are downloaded, installed, trusted, or loaded.
- No change to the custom-URL (custom repo) management UI itself; it only moves entry point.
- No repo-first navigation (proposal D) and no merged single list (proposal C).

## Screens

### Layout and navigation

- Keep `ListDetailPaneScaffold`. List pane = **Installed**. Detail pane = **Browse**.
  - Phone: Installed shows first; the "Get extensions" extended FAB navigates to the detail pane.
    Back returns to Installed (existing `BackHandler`).
  - Tablet/expanded: both panes side by side; FAB hidden when Browse is already visible.
- The app-bar "View Remote/Installed Extensions" toggle icon is removed (FAB replaces it on phone;
  panes are both visible on tablet).
- New nav3 key `Screen.ExtensionListScreen.Catalog(catalogName: String)` for "See all", registered
  in `buildKmpGraph()` next to the existing extension list entry.

### App bar

Title: "Extensions" (phone, Installed) / "Browse" (phone, detail pane). Overflow menu:

- Check for Source Updates (existing 10-minute debounce kept)
- Refresh Extensions
- Share Extensions
- Enable Incognito
- Custom repos (shown when `hasCustomBridge`; opens the existing `CustomUrlDialog` sheet unchanged,
  renamed in the menu only)

### Installed (list pane)

1. **Summary row**: three tiles — Installed (count of distinct packages), Updates, Untrusted.
   - Updates tile uses primary-container color; Untrusted uses a warning-container color. A tile
     with count 0 renders in the neutral surface color and is not clickable.
   - Tapping Updates or Untrusted sets a filter; tapping the active tile again, or tapping
     Installed, clears it. Only one filter at a time.
   - While the Updates filter is active, an "Update all" button appears above the list.
2. **Catalog cards**: one card per catalog (`KmpSourceInformation.catalog?.name`, null grouped as
   today). Header: up to 3 stacked source icons, catalog name, "N extensions", and "· M updates" in
   primary color when M > 0, plus an expand chevron.
   - Expanded by default when the card contains an update or an untrusted extension; otherwise
     collapsed. User toggles persist for the screen session (existing `InstalledViewState.showItems`).
   - With a filter active, cards show only matching rows and are force-expanded; empty cards hide.
   - Catalog-level uninstall (today's trash icon when `canUninstallAll`) moves into a card overflow.
3. **Extension row**: icon, name, "Current" pill when it is the current source, supporting line.
   - Supporting line: untrusted → `KmpUntrustedApiService.reason` in error color; update →
     "1.4.198 → 1.4.201"; else "v1.4.198".
   - One trailing primary action, by priority: Trust (tonal warning button, only when `canTrust`)
     → Update (tonal button with target version; becomes progress while installing) → Settings icon
     (when `KmpConfigurableApiService`) → none.
   - Trailing ⋮ menu: Set as current source (disabled for untrusted), Settings (if configurable and
     not already the primary action), Uninstall (opens a confirm dialog).
   - Row tap keeps today's behavior: set as current source unless untrusted.

### Browse (detail pane)

1. **Search field** at top. Empty: shows shelves. Non-empty: shelves are replaced by one flat
   3-column grid of matches across all catalogs (name contains query, case-insensitive), each tile
   captioned with its catalog name.
2. **Shelves**: one per catalog in `remoteSources`, in current map order.
   - Header: catalog name and "See all N" (navigates to the catalog page).
   - Body: 3-column grid of the first 6 extensions. Sort order within a shelf: update available,
     then installing, then not installed, then installed; ties by name.
   - Failed catalog (`RemoteErrorState`): error card with the failure message and a Retry button.
3. **Tile**: 56dp rounded icon (`iconUrl`, initial-letter fallback), name (1 line, ellipsized),
   subtitle by priority: "Update" (primary color) → "NN%" while installing → language → "v" +
   version. Installed tiles get a check badge. Tap opens the detail sheet.

### Catalog page (`Screen.ExtensionListScreen.Catalog`)

- Top app bar with catalog name and count; search field.
- Filter chips: "All" plus each distinct `lang` present in the catalog (`all` displayed as
  "Multi"), single-select; a "Hide 18+" toggle chip (off by default; hidden when no extension in
  the catalog is nsfw).
- Full 3-column `LazyVerticalGrid` of tiles (same tile and sort order as shelves; uses keys and
  content types).

### Detail sheet

`ModalBottomSheet` opened from any tile (Browse, search results, catalog page):

- Icon, name, catalog name, 18+ pill when nsfw.
- Key/values: Version (installed → remote when update), Package, Language.
- "Sources in this extension": chips of `KmpSources.name` (language appended when the extension is
  multi-language and names collide).
- Action row by state:
  - Not installed → "Install".
  - Update available → "Update to X".
  - Installing → progress bar with status text (Downloading NN% / Installing / Waiting for you).
  - Installed → "Uninstall" (error-colored text button, confirm dialog) and "Settings" when
    configurable.
- Install starts immediately (the current "Are you sure?" dialog is removed); the sheet stays open
  showing progress.

## Data

### `KmpRemoteSources` (kmpmodels)

Add two defaulted fields so existing plugin/serialized data stays compatible:

```kotlin
val lang: String = "",
val nsfw: Boolean = false,
```

### `MihonRepoClient` (mihonsupport)

- Index v1 (`MihonIndexEntry.toRemote`): `lang = lang`, `nsfw = nsfw == 1`.
- Index v2 (`V2Extension.toRemote`): `lang` = the single distinct non-blank `V2Source.language`, or
  `"all"` when more than one; `nsfw = contentWarning == CONTENT_WARNING_NSFW`.
- Stop appending `" (18+)"` to `name` in both mappers (the UI shows the pill instead).

Other `KmpRemoteSources` constructors (`OtakuWorldCatalog`, `Platform.android.kt`,
`JvmModelMapper`) keep defaults.

### `ExtensionListViewModel` (kmpuiviews)

New UI-facing state, computed with pure functions so they can be unit tested:

- `RemoteItemState`: `NotInstalled`, `Installed(installed: KmpSourceInformation)`,
  `UpdateAvailable(installed, installedVersion)`, `Installing(status: DownloadAndInstallStatus)`.
- Matching remote → installed: by `packageName` first; fall back to today's rule (any
  `KmpSources.baseUrl` equals `apiService.baseUrl`). Update = `AppUpdate.checkForUpdate(installed
  version, remote version)` using `SourceInfoRepository.versionName` as today.
- Installing: matched by `downloadLink == DownloadAndInstallState.url` from
  `downloadStateRepository.downloadList` (collected in the VM); takes precedence over other states
  until the entry leaves the list.
- `InstalledFilter { None, Updates, Untrusted }` with counts; filtered catalog groups derived from it.
- `updateAll()`: calls `downloadAndInstall` once per extension with an update.
- `search(query)` across all `RemoteViewState` catalogs; catalog page filtering by lang and nsfw.
- `retry(catalogName)`: re-fetches one catalog's remote sources (same code path as init) and
  replaces its `RemoteState`.
- `RemoteErrorState` gains `message: String?` from the caught exception.

The catalog page resolves the same `ExtensionListViewModel` via Koin with the catalog name as input.

## Error handling

- Catalog fetch failure → per-catalog error card with message and Retry; other catalogs unaffected.
- Install failure: whatever terminal status `DownloadAndInstallStatus` reports is shown in the sheet
  and the tile reverts to its previous state when the entry leaves `downloadList`.
- Uninstall always confirms.

## Testing

- `ExtensionListViewModelTest` (kmpuiviews jvmTest) with fake `SourceRepository`,
  `DownloadStateInterface`, catalogs:
  - package-name match, base-URL fallback, update detection
  - Installing state precedence and progress mapping
  - summary counts and filter behavior (including toggle-off)
  - `updateAll` calls install once per update and never for up-to-date items
  - cross-catalog search
  - catalog lang / nsfw filtering and lang chip list
  - shelf sort order
  - retry replaces only the targeted catalog's state
- `MihonRepoClientTest` (mihonsupport commonTest): v1 and v2 `lang`/`nsfw` mapping, multi-language
  v2 → `"all"`, no " (18+)" suffix.
- Manual: `./gradlew :mangaworld:assembleNoFirebaseDebug`, install on device; check Installed
  summary + filters, Browse shelves, See all, detail sheet, one real install with progress, phone
  and tablet/landscape layouts. Before installing on the phone, compare Room DB versions with the
  main checkout.
