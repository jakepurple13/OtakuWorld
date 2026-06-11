# Download Location Picker Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let MangaWorld Android users pick and persist a custom root directory for chapter downloads, defaulting to `context.filesDir` when none is set.

**Architecture:** Add a `downloadPath` string field to `manga_settings.proto`; read it in `DownloadChapterWorker` (injected via constructor) to resolve either a `File` (default `filesDir`) or a `DocumentFile` (SAF content URI). Surface the picker in a new `AndroidSettingsScreen` behind a `PlatformSettings` nav key registered via `GenericManga.settingsNav3Setup()`.

**Tech Stack:** Wire proto / Kotlin protobuf, Koin WorkManager, AndroidX DocumentFile, FileKit 0.14.1 `rememberDirectoryPickerLauncher`, Jetpack Compose / Navigation3.

---

## File Map

| Action | Path |
|--------|------|
| Modify | `datastore/mangasettings/src/commonMain/proto/manga_settings.proto` |
| Modify | `datastore/mangasettings/src/commonMain/kotlin/com/programmersbox/mangasettings/MangaNewSettingsHandling.kt` |
| Modify | `mangaworld/shared/src/androidMain/kotlin/com/programmersbox/manga/shared/downloads/DownloadChapterWorker.kt` |
| Modify | `mangaworld/src/main/java/com/programmersbox/mangaworld/GenericManga.kt` |
| Create | `mangaworld/src/main/java/com/programmersbox/mangaworld/settings/PlatformSettings.kt` |
| Create | `mangaworld/src/main/java/com/programmersbox/mangaworld/settings/AndroidSettingsScreen.kt` |
| Modify | `README.md` |

---

## Task 1: Add `downloadPath` to the proto schema

**Files:**
- Modify: `datastore/mangasettings/src/commonMain/proto/manga_settings.proto`

- [ ] **Step 1: Add the field**

  Open `datastore/mangasettings/src/commonMain/proto/manga_settings.proto`. After line 14 (`bool includeInsetsForReader = 11;`), add:

  ```protobuf
  string downloadPath = 12;
  ```

  The full `MangaSettings` message should now look like:

  ```protobuf
  message MangaSettings {
    bool useNewReader = 3;
    int32 pagePadding = 4;
    ReaderType readerType = 5;
    ImageLoaderType imageLoaderType = 6;
    bool useFlipPager = 7;
    bool allowUserDrawerGesture = 8;
    bool useFloatingReaderBottomBar = 9;
    bool hasMigrated = 10;
    bool includeInsetsForReader = 11;
    string downloadPath = 12;
  }
  ```

- [ ] **Step 2: Trigger proto code generation**

  Run:
  ```bash
  ./gradlew :datastore:mangasettings:generateProtos 2>&1 | tail -20
  ```
  Expected: `BUILD SUCCESSFUL`. Wire generates the updated `MangaSettings` Kotlin class with the new `downloadPath` property (type `String`, default `""`).

- [ ] **Step 3: Commit**

  ```bash
  git add datastore/mangasettings/src/commonMain/proto/manga_settings.proto
  git commit -m "feat(datastore): add downloadPath field to MangaSettings proto"
  ```

---

## Task 2: Add `downloadPath` accessor to `MangaNewSettingsHandling`

**Files:**
- Modify: `datastore/mangasettings/src/commonMain/kotlin/com/programmersbox/mangasettings/MangaNewSettingsHandling.kt`

- [ ] **Step 1: Add the `ProtoStoreHandler`**

  In `MangaNewSettingsHandling.kt`, after the `hasMigrated` block (lines 92–97), add:

  ```kotlin
  val downloadPath = ProtoStoreHandler(
      preferences = preferences,
      key = { it.downloadPath },
      update = { copy(downloadPath = it) },
      defaultValue = ""
  )
  ```

  The class body from line 92 onward should now read:

  ```kotlin
  val hasMigrated = ProtoStoreHandler(
      preferences = preferences,
      key = { it.hasMigrated },
      update = { copy(hasMigrated = it) },
      defaultValue = false
  )

  val downloadPath = ProtoStoreHandler(
      preferences = preferences,
      key = { it.downloadPath },
      update = { copy(downloadPath = it) },
      defaultValue = ""
  )

  @Composable
  fun rememberIncludeInsetsForReader() = preferences.rememberPreference(
  ```

- [ ] **Step 2: Verify compile**

  ```bash
  ./gradlew :datastore:mangasettings:compileKotlinJvm 2>&1 | tail -20
  ```
  Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

  ```bash
  git add datastore/mangasettings/src/commonMain/kotlin/com/programmersbox/mangasettings/MangaNewSettingsHandling.kt
  git commit -m "feat(mangasettings): expose downloadPath ProtoStoreHandler"
  ```

---

## Task 3: Adapt `DownloadChapterWorker` for configurable download path

**Files:**
- Modify: `mangaworld/shared/src/androidMain/kotlin/com/programmersbox/manga/shared/downloads/DownloadChapterWorker.kt`

- [ ] **Step 1: Add new imports**

  Add these imports to `DownloadChapterWorker.kt` (the existing import block starts at line 1):

  ```kotlin
  import android.net.Uri
  import androidx.documentfile.provider.DocumentFile
  import com.programmersbox.mangasettings.MangaNewSettingsHandling
  ```

- [ ] **Step 2: Add constructor parameter and rewrite `doWork()`**

  Replace the entire class declaration and `doWork()` method (lines 22–139) with:

  ```kotlin
  class DownloadChapterWorker(
      context: Context,
      workerParams: WorkerParameters,
      private val mangaSettings: MangaNewSettingsHandling,
  ) : CoroutineWorker(context, workerParams) {

      private val notificationManager by lazy {
          NotificationManagerCompat.from(applicationContext)
      }

      override suspend fun doWork(): Result {
          println(tags)
          val mangaTitle = inputData.getString(KEY_MANGA_TITLE) ?: return Result.failure()
          val chapterName = inputData.getString(KEY_CHAPTER_NAME) ?: return Result.failure()
          val chapterUrl = inputData.getString(KEY_CHAPTER_URL) ?: return Result.failure()
          val imageUrls = inputData.getString(KEY_IMAGE_URLS)
              ?.let { Json.decodeFromString<List<String>>(it) }
              ?: return Result.failure()
          val headers = inputData.getString(KEY_HEADERS)
              ?.let { Json.decodeFromString<Map<String, String>>(it) }
              ?: emptyMap()

          val notifId = (chapterUrl.hashCode() and Int.MAX_VALUE) % 100_000
          val notifCompleteId = notifId + 100_000
          val notifFailId = notifId + 200_000

          var destFile: File? = null
          var destDoc: DocumentFile? = null

          val client = HttpClient()
          return try {
              val storedPath = mangaSettings.downloadPath.get()
              val subPath = "MangaWorld/${mangaTitle.sanitize()}/${chapterName.sanitize()}"

              val writeBytes: suspend (Int, ByteArray) -> Unit

              if (storedPath.isEmpty()) {
                  val dir = File(applicationContext.filesDir, subPath).also { it.mkdirs() }
                  destFile = dir
                  writeBytes = { index, bytes ->
                      File(dir, "%03d.png".format(index)).writeBytes(bytes)
                  }
              } else {
                  val root = DocumentFile.fromTreeUri(applicationContext, Uri.parse(storedPath))
                      ?: return Result.failure(workDataOf(KEY_ERROR to "Invalid download directory"))
                  fun DocumentFile.sub(name: String) = findFile(name) ?: createDirectory(name)
                      ?: error("Cannot create directory: $name")
                  val dir = root.sub("MangaWorld").sub(mangaTitle.sanitize()).sub(chapterName.sanitize())
                  destDoc = dir
                  writeBytes = { index, bytes ->
                      val doc = dir.createFile("image/png", "%03d.png".format(index))
                          ?: error("Cannot create image file")
                      applicationContext.contentResolver.openOutputStream(doc.uri)!!.use { it.write(bytes) }
                  }
              }

              val request = DownloadRequest(
                  chapterUrl = chapterUrl,
                  chapterName = chapterName,
                  mangaTitle = mangaTitle,
                  imageUrls = imageUrls,
                  headers = headers,
              )
              println(request)

              postNotification(
                  id = notifId,
                  notification = buildProgressNotification(
                      mangaTitle = mangaTitle,
                      chapterName = chapterName,
                      done = 0,
                      total = 0,
                      indeterminate = true,
                  ),
              )

              executeDownload(
                  client = client,
                  request = request,
                  onProgress = { done, total ->
                      setProgress(
                          workDataOf(
                              KEY_PROGRESS_DONE to done,
                              KEY_PROGRESS_TOTAL to total,
                              KEY_CHAPTER_NAME to chapterName,
                              KEY_MANGA_TITLE to mangaTitle,
                          )
                      )
                      postNotification(
                          id = notifId,
                          notification = buildProgressNotification(
                              mangaTitle = mangaTitle,
                              chapterName = chapterName,
                              done = done,
                              total = total,
                              indeterminate = false,
                          ),
                      )
                  },
                  writeBytes = writeBytes,
              )
              if (destFile != null) {
                  MediaScannerConnection.scanFile(
                      applicationContext,
                      destFile.listFiles()?.map { it.absolutePath }?.toTypedArray() ?: emptyArray(),
                      null,
                      null,
                  )
              }
              notificationManager.cancel(notifId)
              postNotification(
                  id = notifCompleteId,
                  notification = buildCompleteNotification(mangaTitle, chapterName),
              )
              Result.success()
          } catch (e: CancellationException) {
              notificationManager.cancel(notifId)
              throw e
          } catch (e: Exception) {
              e.printStackTrace()
              if (runAttemptCount < 3) {
                  Result.retry()
              } else {
                  notificationManager.cancel(notifId)
                  postNotification(
                      id = notifFailId,
                      notification = buildFailedNotification(chapterName, e.message ?: "Unknown error"),
                  )
                  destFile?.deleteRecursively()
                  destDoc?.delete()
                  Result.failure(workDataOf(KEY_ERROR to (e.message ?: "Unknown error")))
              }
          } finally {
              client.close()
          }
      }
  ```

  Leave the notification builder methods and companion object unchanged (lines 141–208).

- [ ] **Step 3: Verify `workerOf` wiring still compiles**

  Koin's `workerOf(::DownloadChapterWorker)` in `GenericManga.kt` (line 62) already uses the Koin WorkManager factory (`workManagerFactory()` is called in `KmpOtakuApp.koinSetup()`). Adding `MangaNewSettingsHandling` to the constructor is enough — Koin injects it automatically because it's already registered as a singleton in `appModule`.

  ```bash
  ./gradlew :mangaworld:compileNoFirebaseDebugKotlin 2>&1 | tail -30
  ```
  Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

  ```bash
  git add mangaworld/shared/src/androidMain/kotlin/com/programmersbox/manga/shared/downloads/DownloadChapterWorker.kt
  git commit -m "feat(download): inject MangaNewSettingsHandling into DownloadChapterWorker for configurable root path"
  ```

---

## Task 4: Create `PlatformSettings` NavKey and `AndroidSettingsViewModel`

**Files:**
- Create: `mangaworld/src/main/java/com/programmersbox/mangaworld/settings/PlatformSettings.kt`
- Modify: `mangaworld/src/main/java/com/programmersbox/mangaworld/GenericManga.kt` (ViewModel registration only)

- [ ] **Step 1: Create `PlatformSettings.kt`**

  Create the file `mangaworld/src/main/java/com/programmersbox/mangaworld/settings/PlatformSettings.kt`:

  ```kotlin
  package com.programmersbox.mangaworld.settings

  import androidx.lifecycle.ViewModel
  import androidx.lifecycle.viewModelScope
  import com.programmersbox.mangasettings.MangaNewSettingsHandling
  import kotlinx.coroutines.flow.SharingStarted
  import kotlinx.coroutines.flow.stateIn
  import kotlinx.coroutines.launch
  import kotlinx.serialization.Serializable
  import androidx.navigation3.runtime.NavKey

  @Serializable
  data object PlatformSettings : NavKey

  class AndroidSettingsViewModel(
      private val mangaSettings: MangaNewSettingsHandling,
  ) : ViewModel() {

      val downloadPath = mangaSettings.downloadPath.asFlow()
          .stateIn(viewModelScope, SharingStarted.Eagerly, "")

      fun setDownloadPath(uri: String) {
          viewModelScope.launch { mangaSettings.downloadPath.set(uri) }
      }

      fun resetDownloadPath() {
          viewModelScope.launch { mangaSettings.downloadPath.set("") }
      }
  }
  ```

- [ ] **Step 2: Register the ViewModel in `appModule`**

  In `mangaworld/src/main/java/com/programmersbox/mangaworld/GenericManga.kt`, add the import:
  ```kotlin
  import com.programmersbox.mangaworld.settings.AndroidSettingsViewModel
  ```

  In the `appModule` block, after line 62 (`workerOf(::DownloadChapterWorker)`), add:
  ```kotlin
  viewModelOf(::AndroidSettingsViewModel)
  ```

- [ ] **Step 3: Verify compile**

  ```bash
  ./gradlew :mangaworld:compileNoFirebaseDebugKotlin 2>&1 | tail -20
  ```
  Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

  ```bash
  git add mangaworld/src/main/java/com/programmersbox/mangaworld/settings/PlatformSettings.kt \
          mangaworld/src/main/java/com/programmersbox/mangaworld/GenericManga.kt
  git commit -m "feat(mangaworld): add PlatformSettings NavKey and AndroidSettingsViewModel"
  ```

---

## Task 5: Create `AndroidSettingsScreen` composable

**Files:**
- Create: `mangaworld/src/main/java/com/programmersbox/mangaworld/settings/AndroidSettingsScreen.kt`

- [ ] **Step 1: Create the screen file**

  Create `mangaworld/src/main/java/com/programmersbox/mangaworld/settings/AndroidSettingsScreen.kt`:

  ```kotlin
  package com.programmersbox.mangaworld.settings

  import android.content.Intent
  import android.net.Uri
  import androidx.compose.foundation.layout.Arrangement
  import androidx.compose.foundation.layout.fillMaxWidth
  import androidx.compose.foundation.layout.padding
  import androidx.compose.foundation.lazy.LazyColumn
  import androidx.compose.material.icons.Icons
  import androidx.compose.material.icons.filled.Clear
  import androidx.compose.material.icons.filled.Folder
  import androidx.compose.material3.ExperimentalMaterial3Api
  import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
  import androidx.compose.material3.Icon
  import androidx.compose.material3.IconButton
  import androidx.compose.material3.LargeTopAppBar
  import androidx.compose.material3.ListItemDefaults
  import androidx.compose.material3.MaterialTheme
  import androidx.compose.material3.Scaffold
  import androidx.compose.material3.SegmentedListItem
  import androidx.compose.material3.Text
  import androidx.compose.material3.TopAppBarDefaults
  import androidx.compose.runtime.Composable
  import androidx.compose.runtime.getValue
  import androidx.compose.ui.Modifier
  import androidx.compose.ui.input.nestedscroll.nestedScroll
  import androidx.compose.ui.platform.LocalContext
  import androidx.compose.ui.unit.dp
  import androidx.lifecycle.compose.collectAsStateWithLifecycle
  import com.programmersbox.kmpuiviews.presentation.components.BackButton
  import com.programmersbox.mangasettings.MangaNewSettingsHandling
  import io.github.vinceglb.filekit.AndroidFile
  import io.github.vinceglb.filekit.dialogs.compose.rememberDirectoryPickerLauncher
  import org.koin.compose.koinInject
  import org.koin.compose.viewmodel.koinViewModel

  @OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
  @Composable
  fun AndroidSettingsScreen() {
      val viewModel = koinViewModel<AndroidSettingsViewModel>()
      val mangaSettings = koinInject<MangaNewSettingsHandling>()
      val downloadPath by viewModel.downloadPath.collectAsStateWithLifecycle()
      val context = LocalContext.current

      val directoryPicker = rememberDirectoryPickerLauncher { platformFile ->
          platformFile?.let { file ->
              val uri: Uri = when (val f = file.androidFile) {
                  is AndroidFile.FileWrapper -> Uri.fromFile(f.file)
                  is AndroidFile.UriWrapper -> f.uri
              }
              context.contentResolver.takePersistableUriPermission(
                  uri,
                  Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
              )
              viewModel.setDownloadPath(uri.toString())
          }
      }

      val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
      val colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer)

      Scaffold(
          topBar = {
              LargeTopAppBar(
                  title = { Text("Platform Settings") },
                  navigationIcon = { BackButton() },
                  scrollBehavior = scrollBehavior,
              )
          },
          modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
      ) { padding ->
          LazyColumn(
              contentPadding = padding,
              verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
              modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 16.dp),
          ) {
              item(contentType = "downloadLocation") {
                  SegmentedListItem(
                      content = { Text("Download Location") },
                      supportingContent = {
                          Text(
                              if (downloadPath.isEmpty()) "Default (Internal Storage)"
                              else downloadPath
                          )
                      },
                      leadingContent = { Icon(Icons.Default.Folder, contentDescription = null) },
                      trailingContent = {
                          if (downloadPath.isNotEmpty()) {
                              IconButton(onClick = { viewModel.resetDownloadPath() }) {
                                  Icon(Icons.Default.Clear, contentDescription = "Reset to default")
                              }
                          }
                      },
                      onClick = { directoryPicker.launch() },
                      colors = colors,
                      shapes = ListItemDefaults.segmentedShapes(index = 0, count = 1),
                  )
              }
          }
      }
  }
  ```

- [ ] **Step 2: Verify compile**

  ```bash
  ./gradlew :mangaworld:compileNoFirebaseDebugKotlin 2>&1 | tail -30
  ```
  Expected: `BUILD SUCCESSFUL`. If `BackButton` import is not found at `com.programmersbox.kmpuiviews.presentation.components.BackButton`, check the desktop `PlatformSettings.kt` import path and adjust.

- [ ] **Step 3: Commit**

  ```bash
  git add mangaworld/src/main/java/com/programmersbox/mangaworld/settings/AndroidSettingsScreen.kt
  git commit -m "feat(mangaworld): add AndroidSettingsScreen with download location picker"
  ```

---

## Task 6: Register navigation in `GenericManga`

**Files:**
- Modify: `mangaworld/src/main/java/com/programmersbox/mangaworld/GenericManga.kt`

- [ ] **Step 1: Add missing imports to `GenericManga.kt`**

  Add these imports:
  ```kotlin
  import androidx.compose.material.icons.Icons
  import androidx.compose.material.icons.filled.PhoneAndroid
  import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
  import androidx.compose.material3.Icon
  import androidx.compose.material3.Text
  import androidx.navigation3.runtime.EntryProviderScope
  import androidx.navigation3.runtime.NavKey
  import com.programmersbox.kmpuiviews.utils.ComposeSettingsDsl
  import com.programmersbox.mangaworld.settings.AndroidSettingsScreen
  import com.programmersbox.mangaworld.settings.PlatformSettings
  ```

- [ ] **Step 2: Override `settingsNav3Setup()` in `GenericManga`**

  In `GenericManga.kt`, after the `deepLinkSettings()` function (line 148, before the closing `}`), add:

  ```kotlin
  context(navGraph: EntryProviderScope<NavKey>)
  override fun settingsNav3Setup() {
      super.settingsNav3Setup()
      navGraph.entry<PlatformSettings> { AndroidSettingsScreen() }
  }
  ```

- [ ] **Step 3: Override `composeCustomPreferences()` in `GenericManga`**

  After the `settingsNav3Setup()` override, add:

  ```kotlin
  @OptIn(ExperimentalMaterial3ExpressiveApi::class)
  override fun composeCustomPreferences(): ComposeSettingsDsl.() -> Unit {
      val compose = ComposeSettingsDsl()
          .apply(super.composeCustomPreferences())

      return {
          viewSettings {
              compose.viewSettings(this)
              segmentedListItem(
                  content = { Text("Platform Settings") },
                  leadingContent = { Icon(Icons.Default.PhoneAndroid, null) },
                  onClick = { navigationActions.navigate(PlatformSettings) },
              )
          }
          generalSettings = compose.generalSettings
          onboardingSettings = compose.onboardingSettings
          playerSettings = compose.playerSettings
      }
  }
  ```

- [ ] **Step 4: Build the app**

  ```bash
  ./gradlew :mangaworld:assembleNoFirebaseDebug 2>&1 | tail -30
  ```
  Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

  ```bash
  git add mangaworld/src/main/java/com/programmersbox/mangaworld/GenericManga.kt
  git commit -m "feat(mangaworld): register PlatformSettings screen and navigation entry"
  ```

---

## Task 7: Update README

**Files:**
- Modify: `README.md`

- [ ] **Step 1: Add download location section**

  In `README.md`, find the Features section (or add after the sources/compatibility paragraph, around line 36). Add the following block:

  ```markdown
  ## Download Location (MangaWorld Android)

  MangaWorld lets you choose where downloaded chapters are saved.

  - **Default:** Chapters are stored in the app's internal storage (`filesDir/MangaWorld/`). No storage permissions required.
  - **Custom folder:** Go to **Settings → Platform Settings → Download Location** and tap the folder icon to pick any directory using the system file picker. The selected location persists across sessions.
  - **Reset:** Tap the ✕ icon next to the path to revert to the default internal storage location.

  > **Note:** When a custom location is set, the app requests persistent read/write access to that directory. Revoking this permission in system settings will cause downloads to fail until you reset or re-select a location.
  ```

- [ ] **Step 2: Commit**

  ```bash
  git add README.md
  git commit -m "docs: document MangaWorld download location picker feature"
  ```

---

## Self-Review Checklist

- [x] **Spec coverage:** All four spec sections covered — proto field (Task 1), settings accessor (Task 2), worker adaptation (Task 3), navigation + screen (Tasks 4–6). README update (Task 7).
- [x] **No placeholders:** All steps contain complete code.
- [x] **Type consistency:** `downloadPath` field name used consistently across proto, `MangaNewSettingsHandling`, worker, and screen. `PlatformSettings` NavKey matches `navGraph.entry<PlatformSettings>` and `navigationActions.navigate(PlatformSettings)` in all occurrences. `AndroidSettingsViewModel.setDownloadPath(String)` called from screen.
- [x] **Worker scope:** `destFile`/`destDoc` declared as `var` outside the try block so they're accessible in the catch cleanup path.
- [x] **Koin wiring:** `workerOf(::DownloadChapterWorker)` relies on `workManagerFactory()` already called in `KmpOtakuApp` — no new factory registration needed.
- [x] **SAF permission:** `takePersistableUriPermission` called in the screen before storing the URI — required for the URI to remain valid across app restarts.
