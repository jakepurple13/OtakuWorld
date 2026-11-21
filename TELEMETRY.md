# Telemetry

This page lists everything OtakuWorld apps send for analytics and crash reporting.

> Maintainers: update this file in the same PR as any event change. `TelemetryDocTest` fails if an
> event in `telemetry/src/commonMain/.../events/TelemetryEvents.kt` is missing here.

## When anything is sent

- Only **`full`** Android builds send anything. They use Firebase Analytics and Firebase Crashlytics.
- **`noFirebase`** builds, the desktop apps, and iOS send nothing. Their telemetry is a no-op that at
  most prints crash-log lines to the local console in debug builds.
- Both kinds of data can be turned off in the app (below).

## How to turn it off

Settings › Behavior › Privacy & Security › **Usage & crash data**:

- **Share usage analytics** turns off every analytics event and user property.
- **Send crash reports** turns off crash reports. Crashlytics applies this the next time the app starts.

The section only appears in builds that can send data.

## Never sent

Item titles, item or chapter links, search text, note text, custom list names, account email
addresses, and file paths. This applies to analytics events, user properties, crash keys, and crash
log lines. Errors are reported by their type (for example `SocketTimeoutException`), never by their
message.

## Source names

Events that name a source or extension send the name the extension declares, with two exceptions:

- A source you marked **incognito** is sent as `incognito`.
- A JavaScript extension (package starting with `js.`) is sent as `custom`.

## Rounded values

Counts and durations are rounded into ranges before they are sent:

- Counts: `0`, `1-5`, `6-20`, `21+`
- Durations: `<1s`, `1-5s`, `5-30s`, `30s-2m`, `2m+`
- Percentages: `0-25`, `26-50`, `51-75`, `76-100`

## Usage analytics events

`media_type` is `manga`, `anime` or `novel`. `from` is where the screen was opened from: `details`,
`search`, `recent`, `all`, `favorites`, `history`, `notification` (the in-app Notifications screen),
`update_notification` (the new-chapter system notification), `deep_link`, `quick_action`,
`downloads`, `long_press`, `lists` or `other`.

| Event | Sent when | Parameters |
|---|---|---|
| `onboarding_finished` | Onboarding closes | `skipped`, `steps_seen` |
| `onboarding_step` | An onboarding page is completed | `step` |
| `source_selected` | You switch the current source | `source`, `media_type` |
| `search` | A global or source search is submitted | `scope` (`global`/`source`), `source_count`, `result_count` |
| `describe_search` | AI describe-to-search runs | `outcome` (`success`/`empty`/`error`), `result_count` |
| `item_viewed` | A details screen loads | `source`, `media_type`, `from` |
| `favorite_added` | You favorite an item | `source`, `from` |
| `favorite_removed` | You unfavorite an item | `source` |
| `list_item_added` | An item is added to custom lists | `list_count` |
| `list_created` | You create a custom list | none |
| `list_imported` | A list is imported from a file | `item_count`, `kind` (`list`/`full`) |
| `item_shared` | You share an item | `method` (`link`/`card`/`qr`/`save`) |
| `note_saved` | You save a note | `is_new` |
| `mood_tag_added` | A mood tag is added | `kind` (`preset`/`custom`/`ai`) |
| `update_check_toggled` | You turn update notifications on or off for an item | `enabled` |
| `chapter_opened` | A chapter or episode is opened | `source`, `media_type`, `from` |
| `reading_session` | The reader closes | `media_type`, `pages_read`, `duration`, `reached_end` |
| `reader_setting_changed` | You change a setting inside the reader | `key`, `value` |
| `chapter_bookmarked` | You bookmark a chapter | `source` |
| `dictionary_lookup` | You open or add a dictionary entry | `action` (`lookup`/`add`) |
| `episode_play_requested` | You tap Play on an episode (AnimeWorld) | `source`, `link_count` |
| `playback_started` | The first video frame shows (AnimeWorld) | `player_style`, `casting`, `ttff` |
| `playback_error` | The video player reports an error (AnimeWorld) | `source`, `error_type` |
| `download_started` | A chapter or episode download starts | `source`, `media_type` |
| `download_finished` | A download ends | `media_type`, `outcome` (`success`/`fail`/`cancel`), `duration` |
| `favorites_filtered` | You apply the favorites filter sheet | `filters` (how many), `sort` |
| `favorites_searched` | You search your favorites | `result_count` |
| `history_cleared` | You clear history | `scope` (`all`/`item`) |
| `update_check_finished` | A check for new chapters finishes | `trigger` (`periodic`/`manual`/`retry`), `items_checked`, `updates_found`, `sources_failed`, `duration` |
| `update_notification_opened` | You tap a new-chapter notification | `count` |
| `catch_up_used` | You act on a catch-up card | `action` (`read`/`skip`/`delete`/`remind`) |
| `notification_dismissed` | You swipe away an update notification | none |
| `extension_installed` | An extension is installed | `package`, `catalog` (`otaku`, or the catalog that owns it, e.g. `mihon`) |
| `extension_install_failed` | An extension install fails | `catalog` (`apk`), `reason` (`blocked`/`conflict`/`incompatible`/`invalid`/`storage`/`generic`) |
| `extension_uninstalled` | An extension is removed | `package` |
| `extension_updated` | An extension is updated | `package` |
| `source_update_skipped` | You skip a source in update checks | `package` |
| `source_error` | A source request fails | `source`, `operation` (`recent`/`all`/`search`/`info`/`chapters`/`pages`), `error_class` |
| `backup_created` | A backup is written | `destination` (`file`/`cloud`), `success` |
| `restore_finished` | A restore finishes | `success`, `categories` |
| `sync_sign_in` | You sign in to cloud sync | `provider` |
| `sync_failed` | A cloud sync push or pull fails | `provider`, `error_class` |
| `ai_task` | An on-device AI feature runs | `feature`, `engine`, `outcome` (`success`/`error`/`unavailable`), `latency` |
| `ai_model_installed` | An AI model is downloaded or imported | `engine`, `via` (`download`/`import`) |
| `translation_used` | A description or TL;DR is translated | `kind` (`description`/`tldr`) |
| `quick_action_used` | You run a quick action | `action`, `surface` |
| `daily_overview_opened` | The daily overview opens | `from` |

Screen views (`screen_view`) send the screen's route name, such as `recent` or `details`, and
nothing from the screen's contents.

## User properties

| Property | Value |
|---|---|
| `buildType` / `buildFlavor` | `debug`/`release`/`beta` and `full` |
| `app_name` | `MangaWorld`, `AnimeWorld`, `NovelWorld`, … |
| `extension_count` | Rounded count of installed sources |
| `favorites_count` | Rounded count of favorites |
| `ai_engine` | `automatic`, `nano` or `gemma` |
| `theme_mode` | The app theme setting |
| `floating_nav` | `on` or `off` |
| `player_style` | AnimeWorld player style |

## Crash reports

A crash report contains the stack trace, the device and app details Crashlytics always adds, and:

**Custom keys:** `buildType`, `buildFlavor`, `commit_sha`, `current_source` (named by the rules
above, or `none`), `incognito` (whether the current source is incognito), `ai_engine`,
`image_loader` (MangaWorld), `player_style` (AnimeWorld), `last_worker`.

**Recent app steps** (the last 64 KB of log lines): screens visited (`nav → details`), update-check
progress by source and count, background job start/finish, remote config key updates, AI failures
by error type, extension loading, reader and player state, downloads, and backup/restore/sync
steps. These lines follow the "never sent" rules.

**Caught errors** (non-fatal reports): source request failures, background job failures, extension
load and install failures, video player errors, AI engine start failures, and database errors.

## What Firebase adds automatically

Firebase attaches device model, OS version, app version, language, country (from IP address) and an
app instance ID to every event and crash report. It also records `first_open`, `session_start`,
`user_engagement`, `app_update`, `os_update` and `app_remove` on its own.
