# Desktop Tabs: Hand-Test Checklist

None of the visual or interactive behavior below has been checked by eye yet. Automated tests cover the logic only.
Run on macOS for all three desktop apps (`./gradlew :mangaworld:desktop:run`, `:animeworld:desktop:run`,
`:novelworld:desktop:run`). Note Windows/Linux results where available.

## Shell and parity (Task 5)
1. **Title bar**
   - The tab strip sits in the title bar.
   - The window still drags by empty title-bar space.
   - The window controls aren't overlapped.
   - The shortcuts button is still at the start.
2. **Tab clicks**
   - Clicking a tab activates it, and middle-click closes it.
   - × closes a tab without activating it first. Home has no ×.
   - "+" opens the new-tab page.
3. **Parked state:** open a title in a tab and scroll its chapters, then switch to Home and back. The scroll position is the same, and the log shows no new details request.
4. **Home side nav:** every item works.
   - Recent, All, Favorites and Extensions open in the Home tab.
   - Notifications, Lists, Search and Settings open their own tab, or switch to it if it's already open.
5. **Toolbar**
   - Back/forward are enabled at the right times and work.
   - The path text is correct.
   - The Search, Lists and Settings buttons work.
   - Downloads shows in MangaWorld and AnimeWorld and is hidden in NovelWorld.
6. The Settings badge appears when an update exists.
7. **Onboarding** on first run.
   - It shows in the Home tab with no tab strip or toolbar. Tab shortcuts are off.
   - The window still drags and the title shows.
8. Extensions load. The updater dialog and the just-updated snackbar work.
9. **Quick actions:** ⌘K and the menu bar work. ⇧⌘F opens or switches to the Search tab. The shortcuts dialog (⌘/) works.
10. **Readers and players**
    - The tab strip stays visible and the tab shortcuts work.
    - Reader bare keys (Space, ←, →, F, C) still work.
    - AnimeWorld player keys work.
11. The list-detail layout works on a wide window inside a tab.
12. App routes work: MangaWorld downloads, and AnimeWorld library folders and video export.
13. In-app banners and the quick-actions overlay appear above tab content, in the focused window.

## Opening in tabs (Task 6)
1. **Covers and chapter rows**
   - ⌘-click and middle-click open a background tab.
   - ⌘⇧-click opens a foreground tab.
   - Every right-click menu entry works.
2. Middle-click and right-click don't also trigger the normal click.
3. Opening a title that is already open flashes its tab and doesn't duplicate it.
4. Two background chapter tabs each show their own chapter.
5. ⌘-clicking an inner button (download, mark read) doesn't hijack a later plain click.

## Motion (Task 7)
1. **Fast background opens:** five quick ⌘-clicks fly in order into tabs above the title bar.
2. **Other opens**
   - ⌘⇧-click grows the tab.
   - Opening an already-open title flashes it.
3. **Switching and closing**
   - Switching tabs crossfades.
   - Closing a tab snaps.
4. **Reduced motion:** turning on reduced motion leaves only crossfades. Dark themes show no white flash.

## Shortcuts (Task 7b)
1. Every row in the shortcut table works on macOS, and on Windows/Linux if available.
2. ⌘9 goes to the last tab. ⌘L focuses the search caret.
3. ⌘← and ⌘→ in the search field move the caret.
4. Known limitation: tab shortcuts do nothing while a popup or dialog has focus.
5. The shortcuts dialog lists every binding. "New window" is hidden when tear-out is off.

## Side tabs and settings (Task 8)
1. Switching tabs between top and side with 6 tabs open: each tab glides to its slot and the content keeps its scroll.
2. Every row on Desktop Settings → Tabs & browsing persists across a restart.
3. "Clicking a title opens: New tab" works. Swapped ⌘-click works.
4. The "after close" choices land on the right tab. Turning covers off shows plain icons.
5. App-specific settings entries stay in the Settings tab.
6. AnimeWorld's Library folders and Subtitle settings stay in the Downloads tab.

## New-tab page and single-instance tabs (Task 9)
1. Each "A new tab shows" mode works: the sections and their order, the Continue count, the background, and focus on search.
2. Each toolbar button opens its tab once, then switches to it.
3. **New-tab page actions:** searching or using a quick link from an untouched new tab replaces the new tab. So does picking from Recently closed.
4. ⌘L from a Search tab that already has results expands search.

## Notes panel (Task 10)
1. **Saving and reopening:** on a title with no note, open Notes and type, then switch tabs and come back. Press ⌘W, then reopen the title. The note is saved.
2. **Clearing:** clearing the field mid-edit keeps the note. Closing the panel with the field empty deletes it.
3. **Quitting:** quit with ⌘Q right after typing. The note is saved.

## Mini player (Task 11)
1. **Continuing playback:** play an episode, then switch tabs. The mini player keeps playing at the same position.
2. **Mini player controls**
   - Clicking it goes back to the tab, and × pauses it.
   - The corner setting works and it can be dragged.
   - It pauses when the app loses focus, if that setting is on.
3. **Single audio:** starting a second episode pauses the mini player. Pressing play on the mini player pauses the foreground video.
4. **Hidden-tab playback:** switching away while an episode is still loading puts it in the mini player when it starts. It never plays unseen.

## Restore (Task 12)
1. Quit with tabs open, using ⌘Q and then the window close button. On relaunch the windows, tabs, back stacks and active tab come back.
2. Each "When the app opens" option works.
3. A fresh install with onboarding pending shows only Home.
4. Restored streamed reader and player tabs come back as Details. A restored local video opens paused.

## Tab sleep (Task 12b)
1. With "Put unused tabs to sleep" set, idle tabs dim. They reload when opened and keep their place.
2. A tab that is playing or in the mini player never sleeps.

## Windows (Task 13)
1. **Tearing out and moving tabs**
   - Dragging a tab off the strip opens a new window that grows from the tab (macOS); Windows uses Lift.
   - Dragging the tab back merges it, and tabs can move between windows.
2. ⌘N and ⌘⇧W work. With tear-out turned off, dragging only reorders.
3. **Confirm quit** with 3 or more tabs open:
   - The dialog is visible after ⌘Q from a torn-out window, from a minimized main window, and when quitting from the Dock.
   - Cancel keeps the app open, and "Don't ask again" persists.
4. **Torn-out windows on restart:** they come back, and off-screen ones come back on screen.
5. **Mini player:** it follows its tab to a torn-out window.
6. **Focus:** activating a tab that's open in another window brings that window to the front.
7. **Quit log line:** the log shows "Quit requested from …" on every quit.

## Android
- Spot-check MangaWorld on an emulator: Recent → Details → chapter works, long-pressing a cover works, the nav bar is unchanged, and Details → Note opens the sheet.
