# AnimeWorld & NovelWorld Desktop + Shared Modules Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add `:animeworld:desktop` and `:novelworld:desktop` JVM modules, plus `:animeworld:shared` (new) and an extended `:novelworld:shared`, following the exact pattern of `:mangaworld:shared` / `:mangaworld:desktop`.

**Architecture:** Each app gets a `GenericSharedX : KmpGenericInfo` abstract class (commonMain) that owns the platform-agnostic parts of `GenericInfo` (list rendering, shimmer placeholder, and — where genuinely shared — chapter/episode click and download). The existing Android `GenericX` class and a new `GenericXDesktop` class both extend it, each implementing only what actually differs per platform. Desktop entry points (`App.kt`/`main.kt`/`PlatformSettings.kt`) are near-verbatim copies of `mangaworld/desktop`'s.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, Koin, Navigation3, Gradle Kotlin DSL, Ktor client (JVM download).

## Global Constraints

- Do not modify `:mangaworld:shared` or `:mangaworld:desktop` — reference only.
- Do not touch iOS targets/configuration.
- No unit tests required for this task.
- No new backend/server-side components.
- Do not add Chromecast/casting to desktop, and do not move it out of the `animeworld` Android app module.
- NovelWorld downloads stay a no-op on every platform.
- AnimeWorld desktop video playback is stubbed (no player library) — clicking an episode navigates to a placeholder screen.
- AnimeWorld desktop downloads are basic: a JVM-only download manager saves the stream to disk, fire-and-forget, with no progress-tracked UI.
- Do not modify `kmpuiviews` (the global/shared module) — the existing `MangaDesktopSettings`-backed `platformModule()` already works correctly for other apps via per-app `AppDirs` scoping.
- Follow existing code style exactly; do not reformat or "improve" unrelated code.
- Only remove imports/fields that THIS refactor makes unused; leave pre-existing unused/dead code alone (e.g. `GenericNovel`'s `context` constructor param, unused even before this change, stays).

---

### Task 1: NovelWorld shared module — extend with `GenericSharedNovel` and `novelSharedModule()`

**Files:**
- Create: `novelworld/shared/src/jvmMain/kotlin/com/programmersbox/novel/shared/Platform.jvm.kt`
- Create: `novelworld/shared/src/commonMain/kotlin/com/programmersbox/novel/shared/GenericSharedNovel.kt`
- Create: `novelworld/shared/src/commonMain/kotlin/com/programmersbox/novel/shared/NovelModule.kt`
- Modify: `novelworld/src/main/java/com/programmersbox/novelworld/GenericNovel.kt` (full rewrite)

**Interfaces:**
- Consumes: `com.programmersbox.novel.shared.ChapterHolder` (existing, `novelworld/shared/src/commonMain/.../ChapterHolder.kt`), `com.programmersbox.novel.shared.reader.ReadViewModel` (existing, has `companion object { fun navigateToNovelReader(navController: NavigationActions, novelTitle: String?, novelUrl: String?, novelInfoUrl: String?, downloaded: Boolean = false, filePath: String? = null) }` and nested `data class NovelReader(...) : NavKey`), `com.programmersbox.novel.shared.reader.NovelReadView` (existing composable, takes `viewModel: ReadViewModel`), `KmpGenericInfo` interface (`kmpuiviews/commonMain/.../GenericInfo.kt`).
- Produces: `abstract class GenericSharedNovel(chapterHolder: ChapterHolder) : KmpGenericInfo` — subclasses must implement `apkString` and `ProfileIcon()` (the only two `KmpGenericInfo` members it leaves abstract). `fun novelSharedModule(): Module` — a Koin module providing `ChapterHolder` (single) and `ReadViewModel` (viewModel).

- [ ] **Step 1: Add the missing JVM `platform()` actual**

Create `novelworld/shared/src/jvmMain/kotlin/com/programmersbox/novel/shared/Platform.jvm.kt`:

```kotlin
package com.programmersbox.shared

actual fun platform() = "Desktop"
```

(Matches the existing `expect fun platform(): String` in `novelworld/shared/src/commonMain/kotlin/com/programmersbox/novel/shared/Platform.kt`, which is declared under package `com.programmersbox.shared` — keep that package for the actual too, since it must match the expect's package exactly.)

- [ ] **Step 2: Create `GenericSharedNovel`**

Create `novelworld/shared/src/commonMain/kotlin/com/programmersbox/novel/shared/GenericSharedNovel.kt`:

```kotlin
package com.programmersbox.novel.shared

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.kmpmodels.KmpChapterModel
import com.programmersbox.kmpmodels.KmpInfoModel
import com.programmersbox.kmpmodels.KmpItemModel
import com.programmersbox.kmpuiviews.KmpGenericInfo
import com.programmersbox.kmpuiviews.presentation.components.placeholder.PlaceholderHighlight
import com.programmersbox.kmpuiviews.presentation.components.placeholder.m3placeholder
import com.programmersbox.kmpuiviews.presentation.components.placeholder.shimmer
import com.programmersbox.kmpuiviews.presentation.navactions.NavigationActions
import com.programmersbox.kmpuiviews.utils.ComponentState
import com.programmersbox.kmpuiviews.utils.composables.modifiers.combineClickableWithIndication
import com.programmersbox.novel.shared.reader.NovelReadView
import com.programmersbox.novel.shared.reader.ReadViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

abstract class GenericSharedNovel(
    private val chapterHolder: ChapterHolder,
) : KmpGenericInfo {

    override val sourceType: String get() = "novel"

    override suspend fun chapterOnClick(
        model: KmpChapterModel,
        allChapters: List<KmpChapterModel>,
        infoModel: KmpInfoModel,
        navController: NavigationActions,
    ) {
        chapterHolder.chapterModel = model
        ReadViewModel.navigateToNovelReader(
            navController,
            infoModel.title,
            model.url,
            model.sourceUrl
        )
    }

    override fun downloadChapter(
        model: KmpChapterModel,
        allChapters: List<KmpChapterModel>,
        infoModel: KmpInfoModel,
        navController: NavigationActions,
    ) {
    }

    @Composable
    override fun ComposeShimmerItem() {
        LazyColumn {
            items(10) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    tonalElevation = 4.dp,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        "",
                        modifier = Modifier
                            .fillMaxWidth()
                            .m3placeholder(
                                true,
                                highlight = PlaceholderHighlight.shimmer()
                            )
                            .padding(4.dp)
                    )
                }
            }
        }
    }

    @OptIn(
        ExperimentalAnimationApi::class,
        ExperimentalFoundationApi::class,
        ExperimentalMaterial3Api::class
    )
    @Composable
    override fun ItemListView(
        list: List<KmpItemModel>,
        favorites: List<DbModel>,
        listState: LazyGridState,
        onLongPress: (KmpItemModel, ComponentState) -> Unit,
        modifier: Modifier,
        paddingValues: PaddingValues,
        onClick: (KmpItemModel) -> Unit,
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(1),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = paddingValues,
            modifier = modifier.fillMaxSize(),
        ) {
            items(list) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                        .combineClickableWithIndication(
                            onLongPress = { c -> onLongPress(it, c) },
                            onClick = { onClick(it) }
                        ),
                    tonalElevation = 4.dp,
                    shape = MaterialTheme.shapes.medium
                ) {
                    ListItem(
                        leadingContent = {
                            Icon(
                                if (favorites.fastAny { f -> f.url == it.url }) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = null,
                            )
                        },
                        headlineContent = { Text(it.title) },
                        overlineContent = { Text(it.source.serviceName) },
                        supportingContent = if (it.description.isNotEmpty()) {
                            { Text(it.description) }
                        } else null
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalAnimationApi::class, ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
    context(navGraph: EntryProviderScope<NavKey>)
    override fun globalNav3Setup() {
        navGraph.entry<ReadViewModel.NovelReader> {
            NovelReadView(
                viewModel = koinViewModel { parametersOf(it) }
            )
        }
    }
}
```

Note the Koin import is `org.koin.compose.viewmodel.koinViewModel` (the multiplatform Koin-Compose artifact), **not** `org.koin.androidx.compose.koinViewModel` which the old Android-only `GenericNovel.kt` used — that Android-only import would not compile in `commonMain`.

- [ ] **Step 3: Create `novelSharedModule()`**

Create `novelworld/shared/src/commonMain/kotlin/com/programmersbox/novel/shared/NovelModule.kt`:

```kotlin
package com.programmersbox.novel.shared

import com.programmersbox.novel.shared.reader.ReadViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

fun novelSharedModule(): Module = module {
    singleOf(::ChapterHolder)
    viewModelOf(::ReadViewModel)
}
```

- [ ] **Step 4: Rewrite the Android `GenericNovel.kt` to extend `GenericSharedNovel`**

Replace the full contents of `novelworld/src/main/java/com/programmersbox/novelworld/GenericNovel.kt`:

```kotlin
package com.programmersbox.novelworld

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.TaskStackBuilder
import com.programmersbox.kmpmodels.KmpItemModel
import com.programmersbox.kmpuiviews.BuildType
import com.programmersbox.kmpuiviews.SystemAlerter
import com.programmersbox.kmpuiviews.domain.AppUpdate
import com.programmersbox.kmpuiviews.utils.AppConfig
import com.programmersbox.kmpuiviews.utils.Backup
import com.programmersbox.kmpuiviews.utils.NotificationLogo
import com.programmersbox.kmpuiviews.utils.Zipper
import com.programmersbox.novel.shared.ChapterHolder
import com.programmersbox.novel.shared.GenericSharedNovel
import com.programmersbox.novel.shared.novelSharedModule
import com.programmersbox.sharedtools.BackupProcessor
import com.programmersbox.uiviews.GenericInfo
import com.programmersbox.uiviews.utils.bindsGenericInfo
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val appModule = module {
    singleOf(::GenericNovel) { bindsGenericInfo() }
    single { NotificationLogo(R.mipmap.ic_launcher_foreground) }
    single { SystemAlerter(get(), get(), BuildConfig.APPLICATION_ID) }
    singleOf(::Backup)
    factory { Zipper(get(), getAll<BackupProcessor>(), get()) }

    includes(novelSharedModule())
}

class GenericNovel(
    val context: Context,
    val appConfig: AppConfig,
    chapterHolder: ChapterHolder,
) : GenericSharedNovel(chapterHolder = chapterHolder), GenericInfo {

    override val deepLinkUri: String get() = "novelworld://"

    override val apkString: AppUpdate.AppUpdates.() -> String?
        get() = {
            when (appConfig.buildType) {
                BuildType.NoFirebase -> novelNoFirebaseFile
                BuildType.Full -> novelFile
            }
        }

    override fun deepLinkDetails(context: Context, itemModel: KmpItemModel?): PendingIntent? {
        val deepLinkIntent = Intent(
            Intent.ACTION_VIEW,
            deepLinkDetailsUri(itemModel),
            context,
            MainActivity::class.java
        )

        return TaskStackBuilder.create(context).run {
            addNextIntentWithParentStack(deepLinkIntent)
            getPendingIntent(itemModel?.hashCode() ?: 0, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }
    }

    override fun deepLinkSettings(context: Context): PendingIntent? {
        val deepLinkIntent = Intent(
            Intent.ACTION_VIEW,
            deepLinkSettingsUri(),
            context,
            MainActivity::class.java
        )

        return TaskStackBuilder.create(context).run {
            addNextIntentWithParentStack(deepLinkIntent)
            getPendingIntent(13, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }
    }
}
```

`context` stays as an unused-looking constructor param — it was already unused outside `deepLinkDetails`/`deepLinkSettings`'s own shadowing local params before this change; leave it (pre-existing, not orphaned by this refactor).

- [ ] **Step 5: Build to verify**

Run: `./gradlew :novelworld:assembleNoFirebaseDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add novelworld/shared/src/jvmMain/kotlin/com/programmersbox/novel/shared/Platform.jvm.kt \
        novelworld/shared/src/commonMain/kotlin/com/programmersbox/novel/shared/GenericSharedNovel.kt \
        novelworld/shared/src/commonMain/kotlin/com/programmersbox/novel/shared/NovelModule.kt \
        novelworld/src/main/java/com/programmersbox/novelworld/GenericNovel.kt
git commit -m "refactor(novelworld): extract GenericSharedNovel into shared module

Moves ItemListView, ComposeShimmerItem, chapterOnClick, downloadChapter,
and reader nav setup out of the Android-only GenericNovel into a new
GenericSharedNovel base class in :novelworld:shared, so a future desktop
target can reuse them. Adds the missing jvmMain Platform actual."
```

---

### Task 2: NovelWorld desktop module

**Files:**
- Modify: `settings.gradle.kts` (add `:novelworld:desktop` include)
- Create: `novelworld/desktop/build.gradle.kts`
- Create: `novelworld/desktop/src/commonMain/kotlin/com/programmersbox/desktop/App.kt`
- Create: `novelworld/desktop/src/jvmMain/kotlin/com/programmersbox/desktop/main.kt`
- Create: `novelworld/desktop/src/jvmMain/kotlin/com/programmersbox/desktop/GenericNovelDesktop.kt`
- Create: `novelworld/desktop/src/jvmMain/kotlin/com/programmersbox/desktop/PlatformSettings.kt`

**Interfaces:**
- Consumes: `GenericSharedNovel(chapterHolder: ChapterHolder)` and `novelSharedModule(): Module` from Task 1. `BaseDesktopUi(title: String, moduleBlock: KoinApplication.() -> Unit)` (existing, `kmpuiviews/jvmMain/.../DesktopUi.kt`). `bindsGenericInfo()` (existing, `kmpuiviews/commonMain/.../utils/Utils.kt`).
- Produces: `class GenericNovelDesktop(chapterHolder: ChapterHolder, navigationActions: NavigationActions) : GenericSharedNovel(...), PlatformGenericInfo`. `data object PlatformSettings : NavKey` + `@Composable fun JvmSettingsScreen()`.

- [ ] **Step 1: Add the module to `settings.gradle.kts`**

In `settings.gradle.kts`, find:

```kotlin
    ":mangaworld:desktop"?  // (not present verbatim in include(...) block — see actual line below)
```

Actually locate the existing block (inside the top-level `include(...)` call around line 73-76):

```kotlin
    ":animeworld",
    ":mangaworld",
    ":novelworld",
```

Leave that block untouched — the new modules use the separate `include(":x")` calls below (around line 90-101). Find:

```kotlin
include(":mangaworld:desktop")
include(":mangaworld:shared")
include(":novelworld:shared")
```

Replace with:

```kotlin
include(":mangaworld:desktop")
include(":mangaworld:shared")
include(":novelworld:shared")
include(":novelworld:desktop")
```

- [ ] **Step 2: Create the desktop module's `build.gradle.kts`**

Create `novelworld/desktop/build.gradle.kts`:

```kotlin
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeFeatureFlag

plugins {
    id("kotlin-multiplatform")
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.compose.compiler)
    id("org.jetbrains.compose.hot-reload")
    id("kotlinx-serialization")
}

configurations.all {
    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-android")
}

kotlin {
    jvm()

    compilerOptions {
        freeCompilerArgs.add("-Xwhen-guards")
        freeCompilerArgs.add("-Xcontext-parameters")
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinStLib)
            implementation(projects.kmpuiviews)
            implementation(commonLibs.compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.runtime)
            implementation(compose.ui)
            implementation(compose.uiUtil)
            implementation(compose.foundation)
            implementation(compose.material3AdaptiveNavigationSuite)
            implementation(compose.components.resources)
            implementation(commonLibs.material.kolor)

            implementation(project.dependencies.platform(commonLibs.koin.bom))
            implementation(commonLibs.bundles.koinKmp)

            implementation(projects.favoritesdatabase)
            implementation(projects.datastore)
            implementation(projects.kmpmodels)
            implementation(projects.novelworld.shared)
            implementation(commonLibs.bundles.datastoreLibs)
            implementation(commonLibs.coroutinesCore)
            implementation(desktopLibs.kotlinx.coroutines.swing)
            api(commonLibs.androidx.navigation3.runtime)
            api(commonLibs.filekit.core)
            api(commonLibs.filekit.dialogs.compose)
        }

        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(desktopLibs.kotlinx.coroutines.swing)
        }
    }
}

composeCompiler {
    featureFlags.add(ComposeFeatureFlag.OptimizeNonSkippingGroups)
}

compose.desktop {
    application {
        mainClass = "com.programmersbox.desktop.MainKt"

        jvmArgs("--add-opens", "java.desktop/sun.awt=ALL-UNNAMED")
        jvmArgs("--add-opens", "java.desktop/java.awt.peer=ALL-UNNAMED")

        if (System.getProperty("os.name").contains("Mac")) {
            jvmArgs("--add-opens", "java.desktop/sun.lwawt=ALL-UNNAMED")
            jvmArgs("--add-opens", "java.desktop/sun.lwawt.macosx=ALL-UNNAMED")
        }

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.programmersbox.desktop"
            packageVersion = "1.0.0"
        }
    }
}
```

- [ ] **Step 3: Create the empty `App.kt` commonMain stub**

Create `novelworld/desktop/src/commonMain/kotlin/com/programmersbox/desktop/App.kt`:

```kotlin
package com.programmersbox.desktop
```

- [ ] **Step 4: Create `GenericNovelDesktop.kt`**

Create `novelworld/desktop/src/jvmMain/kotlin/com/programmersbox/desktop/GenericNovelDesktop.kt`:

```kotlin
package com.programmersbox.desktop

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DesktopMac
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.programmersbox.kmpuiviews.PlatformGenericInfo
import com.programmersbox.kmpuiviews.domain.AppUpdate
import com.programmersbox.kmpuiviews.presentation.navactions.NavigationActions
import com.programmersbox.kmpuiviews.utils.ComposeSettingsDsl
import com.programmersbox.novel.shared.ChapterHolder
import com.programmersbox.novel.shared.GenericSharedNovel

class GenericNovelDesktop(
    chapterHolder: ChapterHolder,
    private val navigationActions: NavigationActions,
) : GenericSharedNovel(chapterHolder = chapterHolder), PlatformGenericInfo {

    override val apkString: AppUpdate.AppUpdates.() -> String? = { "" }

    @Composable
    override fun ProfileIcon(): String = ""

    context(navGraph: EntryProviderScope<NavKey>)
    override fun settingsNav3Setup() {
        navGraph.entry<PlatformSettings> { JvmSettingsScreen() }
    }

    override fun composeCustomPreferences(): ComposeSettingsDsl.() -> Unit = {
        viewSettings {
            segmentedListItem(
                content = { Text("Platform Settings") },
                leadingContent = { Icon(Icons.Default.DesktopMac, null) },
                onClick = { navigationActions.navigate(PlatformSettings) }
            )
        }
    }
}
```

- [ ] **Step 5: Create `PlatformSettings.kt`**

Create `novelworld/desktop/src/jvmMain/kotlin/com/programmersbox/desktop/PlatformSettings.kt`:

```kotlin
package com.programmersbox.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dataset
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import ca.gosyer.appdirs.AppDirs
import com.programmersbox.kmpuiviews.presentation.components.BackButton
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject
import java.awt.Desktop
import java.io.File

@Serializable
data object PlatformSettings : NavKey

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun JvmSettingsScreen() {
    val appDirs = koinInject<AppDirs>()

    val colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("Desktop Settings") },
                navigationIcon = { BackButton() },
                scrollBehavior = scrollBehavior
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
    ) { padding ->
        LazyColumn(
            contentPadding = padding,
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            item(contentType = "viewFolders") {
                SegmentedListItem(
                    content = { Text("View Data Directory") },
                    supportingContent = { Text("View the directory where the data is stored") },
                    leadingContent = { Icon(Icons.Default.Dataset, null) },
                    onClick = {
                        if (Desktop.isDesktopSupported()) {
                            Desktop.getDesktop().open(File(appDirs.getUserDataDir()))
                        }
                    },
                    colors = colors,
                    shapes = ListItemDefaults.segmentedShapes(index = 0, count = 1)
                )
            }
        }
    }
}
```

- [ ] **Step 6: Create `main.kt`**

Create `novelworld/desktop/src/jvmMain/kotlin/com/programmersbox/desktop/main.kt`:

```kotlin
package com.programmersbox.desktop

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.window.application
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import ca.gosyer.appdirs.AppDirs
import com.programmersbox.datastore.DataStoreSettings
import com.programmersbox.kmpuiviews.BaseDesktopUi
import com.programmersbox.kmpuiviews.BuildType
import com.programmersbox.kmpuiviews.repository.BackgroundWorkHandlerImpl
import com.programmersbox.kmpuiviews.utils.AppConfig
import com.programmersbox.kmpuiviews.utils.bindsGenericInfo
import com.programmersbox.novel.shared.novelSharedModule
import dev.nucleusframework.systeminfo.SystemInfo
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import java.io.File

fun main(args: Array<String>) {
    val appDirs = AppDirs {
        appName = "NovelWorld"
        appAuthor = "jakepurple13"
    }

    DataStoreSettings { File(appDirs.getUserDataDir(), it).absolutePath }

    if (BackgroundWorkHandlerImpl.setupSyncCheckers(args)) return
    val desktopViewModelStoreOwner = DesktopViewModelStoreOwner()
    application {
        CompositionLocalProvider(
            LocalViewModelStoreOwner provides desktopViewModelStoreOwner
        ) {
            BaseDesktopUi(
                title = "NovelWorld",
                moduleBlock = {
                    modules(
                        module {
                            single {
                                AppConfig(
                                    appName = "NovelWorld",
                                    buildType = BuildType.NoFirebase,
                                    isDebug = false,
                                    userName = SystemInfo.users().firstOrNull()?.name
                                )
                            }
                            singleOf(::GenericNovelDesktop) { bindsGenericInfo() }

                            includes(novelSharedModule())
                        }
                    )
                }
            )
        }
    }
}

private class DesktopViewModelStoreOwner : ViewModelStoreOwner {
    override val viewModelStore: ViewModelStore = ViewModelStore()
}
```

- [ ] **Step 7: Verify the desktop app launches**

Run:

```bash
./gradlew :novelworld:desktop:run > /tmp/novelworld-desktop-run.log 2>&1 &
RUN_PID=$!
sleep 40
if ! kill -0 $RUN_PID 2>/dev/null; then
  echo "Process exited early:"; cat /tmp/novelworld-desktop-run.log; exit 1
fi
echo "Still running after 40s — OK"
kill $RUN_PID
```

Expected: "Still running after 40s — OK" (a window process that's still alive after 40s and produced no exception in the log indicates a successful launch). If it exits early, `cat /tmp/novelworld-desktop-run.log` will show the stack trace to debug.

- [ ] **Step 8: Commit**

```bash
git add settings.gradle.kts novelworld/desktop
git commit -m "feat(novelworld): add :novelworld:desktop JVM module

Mirrors :mangaworld:desktop's structure — GenericNovelDesktop extends
the new GenericSharedNovel, wired through BaseDesktopUi with its own
minimal platform settings screen (no download support, matching
NovelWorld's existing no-op downloadChapter)."
```

---

### Task 3: AnimeWorld shared module — `GenericSharedAnime`

**Files:**
- Modify: `settings.gradle.kts` (add `:animeworld:shared` include)
- Create: `animeworld/shared/build.gradle.kts`
- Create: `animeworld/shared/src/commonMain/kotlin/com/programmersbox/anime/shared/GenericSharedAnime.kt`
- Modify: `animeworld/build.gradle.kts` (add shared module dependency)
- Modify: `animeworld/src/main/java/com/programmersbox/animeworld/GenericAnime.kt`

**Interfaces:**
- Consumes: `KmpGenericInfo` interface (`kmpuiviews`), `AppConfig` (`kmpuiviews/commonMain/.../utils/AppConfig.kt`, has `.buildType: BuildType`), `AppUpdate.AppUpdates` data class (`kmpuiviews/commonMain/.../domain/AppUpdate.kt`, has `val animeFile: String?` and `val animeNoFirebaseFile: String?` members).
- Produces: `abstract class GenericSharedAnime(appConfig: AppConfig) : KmpGenericInfo` — implements `sourceType`, `apkString`, `ComposeShimmerItem`, `ItemListView`; leaves `chapterOnClick`, `downloadChapter`, and `ProfileIcon` abstract (genuinely diverge per platform).

- [ ] **Step 1: Add the module to `settings.gradle.kts`**

Find (after Task 2's edit):

```kotlin
include(":novelworld:shared")
include(":novelworld:desktop")
```

Replace with:

```kotlin
include(":novelworld:shared")
include(":novelworld:desktop")
include(":animeworld:shared")
```

- [ ] **Step 2: Create `animeworld/shared/build.gradle.kts`**

```kotlin
plugins {
    `otaku-multiplatform`
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.compose.compiler)
    id("kotlinx-serialization")
}

kotlin {
    android {
        namespace = "com.programmersbox.anime.shared"
        androidResources {
            enable = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinStLib)
            implementation(projects.kmpuiviews)
            implementation(commonLibs.compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.runtime)
            implementation(compose.ui)
            implementation(compose.foundation)
            implementation(compose.material3AdaptiveNavigationSuite)
            implementation(compose.components.resources)
            implementation(commonLibs.material.kolor)

            implementation(project.dependencies.platform(commonLibs.koin.bom))
            implementation(commonLibs.bundles.koinKmp)

            implementation(projects.favoritesdatabase)
            implementation(projects.datastore)
            implementation(projects.kmpmodels)
            implementation(commonLibs.bundles.datastoreLibs)

            implementation(commonLibs.androidx.navigation3.runtime)
        }

        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(desktopLibs.kotlinx.coroutines.swing)
        }
    }
}
```

- [ ] **Step 3: Create `GenericSharedAnime.kt`**

Create `animeworld/shared/src/commonMain/kotlin/com/programmersbox/anime/shared/GenericSharedAnime.kt`:

```kotlin
package com.programmersbox.anime.shared

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.kmpmodels.KmpItemModel
import com.programmersbox.kmpuiviews.BuildType
import com.programmersbox.kmpuiviews.KmpGenericInfo
import com.programmersbox.kmpuiviews.domain.AppUpdate
import com.programmersbox.kmpuiviews.presentation.components.placeholder.PlaceholderHighlight
import com.programmersbox.kmpuiviews.presentation.components.placeholder.m3placeholder
import com.programmersbox.kmpuiviews.presentation.components.placeholder.shimmer
import com.programmersbox.kmpuiviews.utils.AppConfig
import com.programmersbox.kmpuiviews.utils.ComponentState
import com.programmersbox.kmpuiviews.utils.composables.modifiers.combineClickableWithIndication

abstract class GenericSharedAnime(
    protected val appConfig: AppConfig,
) : KmpGenericInfo {

    override val sourceType: String get() = "anime"

    override val apkString: AppUpdate.AppUpdates.() -> String?
        get() = {
            when (appConfig.buildType) {
                BuildType.NoFirebase -> animeNoFirebaseFile
                BuildType.Full -> animeFile
            }
        }

    @Composable
    override fun ComposeShimmerItem() {
        LazyColumn {
            items(10) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .m3placeholder(
                                true,
                                highlight = PlaceholderHighlight.shimmer()
                            )
                    ) {
                        Icon(
                            Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )

                        Text(
                            "",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp)
                        )
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    override fun ItemListView(
        list: List<KmpItemModel>,
        favorites: List<DbModel>,
        listState: LazyGridState,
        onLongPress: (KmpItemModel, ComponentState) -> Unit,
        modifier: Modifier,
        paddingValues: PaddingValues,
        onClick: (KmpItemModel) -> Unit,
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(1),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = paddingValues,
            modifier = modifier.fillMaxSize()
        ) {
            items(list) {
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                        .combineClickableWithIndication(
                            onLongPress = { c -> onLongPress(it, c) },
                            onClick = { onClick(it) }
                        )
                ) {
                    ListItem(
                        leadingContent = {
                            Icon(
                                if (favorites.fastAny { f -> f.url == it.url }) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = null,
                            )
                        },
                        headlineContent = { Text(it.title) },
                        overlineContent = { Text(it.source.serviceName) },
                        supportingContent = if (it.description.isNotEmpty()) {
                            { Text(it.description) }
                        } else null
                    )
                }
            }
        }
    }
}
```

- [ ] **Step 4: Add the shared-module dependency to the Android app**

In `animeworld/build.gradle.kts`, find:

```kotlin
    implementation(commonLibs.ktorAndroid)

    implementation(androidx.profileinstaller.profileinstaller)
    baselineProfile(projects.animeWorldbaselineprofile)
```

Replace with:

```kotlin
    implementation(commonLibs.ktorAndroid)

    implementation(projects.animeworld.shared)

    implementation(androidx.profileinstaller.profileinstaller)
    baselineProfile(projects.animeWorldbaselineprofile)
```

- [ ] **Step 5: Update `GenericAnime.kt` to extend `GenericSharedAnime`**

In `animeworld/src/main/java/com/programmersbox/animeworld/GenericAnime.kt`:

1. Change the class declaration and constructor (remove the `appConfig` `val` since it's now only needed by the superclass call; delete the `sourceType`, `apkString`, `ComposeShimmerItem`, and `ItemListView` overrides — they're now inherited):

Find:

```kotlin
class GenericAnime(
    val context: Context,
    val storageHolder: StorageHolder,
    val animeDataStoreHandling: AnimeDataStoreHandling,
    val appConfig: AppConfig,
) : GenericInfo {

    override val apkString: AppUpdate.AppUpdates.() -> String?
        get() = {
            when (appConfig.buildType) {
                BuildType.NoFirebase -> animeNoFirebaseFile
                BuildType.Full -> animeFile
            }
        }
    override val deepLinkUri: String get() = "animeworld://"

    override val sourceType: String get() = "anime"

    override suspend fun chapterOnClick(
```

Replace with:

```kotlin
class GenericAnime(
    val context: Context,
    val storageHolder: StorageHolder,
    val animeDataStoreHandling: AnimeDataStoreHandling,
    appConfig: AppConfig,
) : GenericSharedAnime(appConfig = appConfig), GenericInfo {

    override val deepLinkUri: String get() = "animeworld://"

    override suspend fun chapterOnClick(
```

2. Delete the `ComposeShimmerItem` and `ItemListView` override bodies. Find:

```kotlin
    @Composable
    override fun ComposeShimmerItem() {
        LazyColumn {
            items(10) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .m3placeholder(
                                true,
                                highlight = PlaceholderHighlight.shimmer()
                            )
                    ) {
                        Icon(
                            Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )

                        Text(
                            "",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp)
                        )
                    }
                }
            }
        }
    }

    @OptIn(
        ExperimentalFoundationApi::class,
    )
    @Composable
    override fun ItemListView(
        list: List<KmpItemModel>,
        favorites: List<DbModel>,
        listState: LazyGridState,
        onLongPress: (KmpItemModel, ComponentState) -> Unit,
        modifier: Modifier,
        paddingValues: PaddingValues,
        onClick: (KmpItemModel) -> Unit,
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(1),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = paddingValues,
            modifier = modifier.fillMaxSize()
        ) {
            items(list) {
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                        .combineClickableWithIndication(
                            onLongPress = { c -> onLongPress(it, c) },
                            onClick = { onClick(it) }
                        )
                ) {
                    ListItem(
                        leadingContent = {
                            Icon(
                                if (favorites.fastAny { f -> f.url == it.url }) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = null,
                            )
                        },
                        headlineContent = { Text(it.title) },
                        overlineContent = { Text(it.source.serviceName) },
                        supportingContent = if (it.description.isNotEmpty()) {
                            { Text(it.description) }
                        } else null
                    )
                }
            }
        }
    }

    class CastingViewModel : ViewModel() {
```

Replace with:

```kotlin
    class CastingViewModel : ViewModel() {
```

3. Add the import for `GenericSharedAnime` and remove now-unused imports. Find:

```kotlin
import com.programmersbox.animeworld.videos.VideoViewerRoute
import com.programmersbox.animeworld.videos.ViewVideoScreen
import com.programmersbox.datastore.asState
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.helpfulutils.downloadManager
import com.programmersbox.helpfulutils.requestPermissions
import com.programmersbox.helpfulutils.runOnUIThread
import com.programmersbox.kmpmodels.KmpChapterModel
import com.programmersbox.kmpmodels.KmpInfoModel
import com.programmersbox.kmpmodels.KmpItemModel
import com.programmersbox.kmpmodels.KmpStorage
import com.programmersbox.kmpuiviews.BuildType
import com.programmersbox.kmpuiviews.SystemAlerter
import com.programmersbox.kmpuiviews.domain.AppUpdate
import com.programmersbox.kmpuiviews.presentation.components.placeholder.PlaceholderHighlight
import com.programmersbox.kmpuiviews.presentation.components.placeholder.m3placeholder
import com.programmersbox.kmpuiviews.presentation.components.placeholder.shimmer
import com.programmersbox.kmpuiviews.presentation.components.settings.CategoryGroup
import com.programmersbox.kmpuiviews.presentation.components.settings.PreferenceSetting
import com.programmersbox.kmpuiviews.presentation.components.settings.ShowWhen
import com.programmersbox.kmpuiviews.presentation.components.settings.SwitchSetting
import com.programmersbox.kmpuiviews.presentation.navactions.NavigationActions
import com.programmersbox.kmpuiviews.utils.AppConfig
import com.programmersbox.kmpuiviews.utils.Backup
import com.programmersbox.kmpuiviews.utils.ComponentState
import com.programmersbox.kmpuiviews.utils.ComposeSettingsDsl
import com.programmersbox.kmpuiviews.utils.LocalNavActions
import com.programmersbox.kmpuiviews.utils.NotificationLogo
import com.programmersbox.kmpuiviews.utils.Zipper
import com.programmersbox.kmpuiviews.utils.composables.modifiers.combineClickableWithIndication
import com.programmersbox.sharedtools.BackupProcessor
import com.programmersbox.uiviews.GenericInfo
import com.programmersbox.uiviews.utils.bindsGenericInfo
```

Replace with:

```kotlin
import com.programmersbox.animeworld.videos.VideoViewerRoute
import com.programmersbox.animeworld.videos.ViewVideoScreen
import com.programmersbox.anime.shared.GenericSharedAnime
import com.programmersbox.datastore.asState
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.helpfulutils.downloadManager
import com.programmersbox.helpfulutils.requestPermissions
import com.programmersbox.helpfulutils.runOnUIThread
import com.programmersbox.kmpmodels.KmpChapterModel
import com.programmersbox.kmpmodels.KmpInfoModel
import com.programmersbox.kmpmodels.KmpItemModel
import com.programmersbox.kmpmodels.KmpStorage
import com.programmersbox.kmpuiviews.SystemAlerter
import com.programmersbox.kmpuiviews.presentation.components.settings.CategoryGroup
import com.programmersbox.kmpuiviews.presentation.components.settings.PreferenceSetting
import com.programmersbox.kmpuiviews.presentation.components.settings.ShowWhen
import com.programmersbox.kmpuiviews.presentation.components.settings.SwitchSetting
import com.programmersbox.kmpuiviews.presentation.navactions.NavigationActions
import com.programmersbox.kmpuiviews.utils.AppConfig
import com.programmersbox.kmpuiviews.utils.Backup
import com.programmersbox.kmpuiviews.utils.ComponentState
import com.programmersbox.kmpuiviews.utils.ComposeSettingsDsl
import com.programmersbox.kmpuiviews.utils.LocalNavActions
import com.programmersbox.kmpuiviews.utils.NotificationLogo
import com.programmersbox.kmpuiviews.utils.Zipper
import com.programmersbox.sharedtools.BackupProcessor
import com.programmersbox.uiviews.GenericInfo
import com.programmersbox.uiviews.utils.bindsGenericInfo
```

(`DbModel`, `ComponentState`, `combineClickableWithIndication`, `PlaceholderHighlight`/`m3placeholder`/`shimmer`, `BuildType`, `AppUpdate` were only used by the two deleted overrides and the deleted `apkString`/`sourceType` — except `DbModel`/`ComponentState` are still used as `ItemListView`'s parameter types... but that override no longer exists in this file, so they truly are unused here now. `KmpItemModel` stays — still used by `chapterOnClick`'s... actually check: `deepLinkDetails(context: Context, itemModel: KmpItemModel?)` still uses it.)

Note: leave `GridCells`, `LazyVerticalGrid`, `LazyGridState`, `ElevatedCard`, `ListItem`, `Card`, `LazyColumn`, `Row`, `Icons.Default.Favorite`/`FavoriteBorder`, `Alignment` import lines as-is if they're still referenced elsewhere in the file (e.g. `Icons.Default.FavoriteBorder` is also used in `DetailActions`? — check before deleting: scan the remaining file for each symbol; only remove an import if grep shows zero remaining usages after the two function bodies are deleted).

- [ ] **Step 6: Build to verify**

Run: `./gradlew :animeworld:assembleNoFirebaseDebug`
Expected: BUILD SUCCESSFUL. If it fails with "unused import" it's just a warning (non-fatal); if it fails with "unresolved reference," an import was removed that's still needed elsewhere — add it back.

- [ ] **Step 7: Commit**

```bash
git add settings.gradle.kts animeworld/shared animeworld/build.gradle.kts animeworld/src/main/java/com/programmersbox/animeworld/GenericAnime.kt
git commit -m "refactor(animeworld): extract GenericSharedAnime into new shared module

Moves ItemListView and ComposeShimmerItem (and the buildType-branching
apkString) into a new :animeworld:shared module so a future desktop
target can reuse them. chapterOnClick and downloadChapter stay
abstract — Android's casting/ExoPlayer/DownloadManager-based
implementation is untouched."
```

---

### Task 4: AnimeWorld desktop downloads and playback stub (shared jvmMain)

**Files:**
- Create: `animeworld/shared/src/jvmMain/kotlin/com/programmersbox/anime/shared/AnimeDesktopSettings.kt`
- Create: `animeworld/shared/src/jvmMain/kotlin/com/programmersbox/anime/shared/downloads/AnimeDownloadManager.kt`
- Create: `animeworld/shared/src/jvmMain/kotlin/com/programmersbox/anime/shared/VideoNotSupportedScreen.kt`

**Interfaces:**
- Consumes: `KmpChapterModel` (has `fun getChapterInfo(): Flow<List<KmpStorage>>`), `KmpStorage` (`link: String?`, `headers: MutableMap<String, String>`) — both `kmpmodels/commonMain/.../Models.kt`. `DataStoreHandler` (`com.programmersbox.datastore`, constructed with `key`/`defaultValue`, exposes `.get(): T` suspend and `.asState()` composable — same API `MangaDesktopSettings` uses). `BackButton()` composable (`kmpuiviews/commonMain/.../presentation/components/`).
- Produces: `class AnimeDesktopSettings(appDirs: AppDirs)` with `val downloadsDirectory: DataStoreHandler<String>`. `class AnimeDownloadManager(scope: CoroutineScope, animeDesktopSettings: AnimeDesktopSettings, trayState: TrayState)` with `fun downloadChapter(chapter: KmpChapterModel, animeTitle: String)`. `data object VideoNotSupportedRoute : NavKey` and `@Composable fun VideoNotSupportedScreen()`.

- [ ] **Step 1: Create `AnimeDesktopSettings`**

Create `animeworld/shared/src/jvmMain/kotlin/com/programmersbox/anime/shared/AnimeDesktopSettings.kt`:

```kotlin
package com.programmersbox.anime.shared

import androidx.datastore.preferences.core.stringPreferencesKey
import ca.gosyer.appdirs.AppDirs
import com.programmersbox.datastore.DataStoreHandler
import java.io.File

class AnimeDesktopSettings(
    appDirs: AppDirs,
) {
    val downloadsDirectory = DataStoreHandler(
        key = stringPreferencesKey("downloadsDirectory"),
        defaultValue = File("${System.getProperty("user.home")}/Downloads/AnimeWorld").absolutePath
    )
}
```

- [ ] **Step 2: Create `AnimeDownloadManager`**

Create `animeworld/shared/src/jvmMain/kotlin/com/programmersbox/anime/shared/downloads/AnimeDownloadManager.kt`:

```kotlin
package com.programmersbox.anime.shared.downloads

import androidx.compose.ui.window.Notification
import androidx.compose.ui.window.TrayState
import com.programmersbox.anime.shared.AnimeDesktopSettings
import com.programmersbox.kmpmodels.KmpChapterModel
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.headers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.io.File

class AnimeDownloadManager(
    private val scope: CoroutineScope,
    private val animeDesktopSettings: AnimeDesktopSettings,
    private val trayState: TrayState,
) {

    private val httpClient = HttpClient()

    init {
        scope.coroutineContext[Job]?.invokeOnCompletion { httpClient.close() }
    }

    fun downloadChapter(chapter: KmpChapterModel, animeTitle: String) {
        scope.launch {
            val storage = chapter.getChapterInfo().firstOrNull()?.firstOrNull { it.link != null }
            val link = storage?.link
            if (storage == null || link == null) {
                notify("Download Failed", "${chapter.name}: no downloadable stream found", isError = true)
                return@launch
            }

            val rootDir = animeDesktopSettings.downloadsDirectory.get()
            val destDir = File(rootDir, animeTitle.sanitizeForPath()).also { it.mkdirs() }
            val destFile = File(destDir, "${chapter.name.sanitizeForPath()}.mp4")

            try {
                val bytes: ByteArray = httpClient.get(link) {
                    headers { storage.headers.forEach { (key, value) -> append(key, value) } }
                }.body()
                destFile.writeBytes(bytes)
                notify("Downloaded", "$animeTitle — ${chapter.name}", isError = false)
            } catch (e: Exception) {
                notify("Download Failed", "${chapter.name}: ${e.message}", isError = true)
            }
        }
    }

    private suspend fun notify(title: String, message: String, isError: Boolean) {
        withContext(Dispatchers.Main) {
            trayState.sendNotification(
                Notification(
                    title = title,
                    message = message,
                    type = if (isError) Notification.Type.Error else Notification.Type.Info
                )
            )
        }
    }
}

private fun String.sanitizeForPath(): String = replace(Regex("[\\\\/:*?\"<>|]"), "_")
```

- [ ] **Step 3: Create `VideoNotSupportedScreen`**

Create `animeworld/shared/src/jvmMain/kotlin/com/programmersbox/anime/shared/VideoNotSupportedScreen.kt`:

```kotlin
package com.programmersbox.anime.shared

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import com.programmersbox.kmpuiviews.presentation.components.BackButton
import kotlinx.serialization.Serializable

@Serializable
data object VideoNotSupportedRoute : NavKey

@Composable
fun VideoNotSupportedScreen() {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Playback") },
                navigationIcon = { BackButton() }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                Icons.Default.DesktopWindows,
                contentDescription = null,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Text(
                "Video playback isn't supported on desktop yet.",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}
```

- [ ] **Step 4: Build to verify**

Run: `./gradlew :animeworld:shared:compileKotlinJvm`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add animeworld/shared/src/jvmMain
git commit -m "feat(animeworld): add desktop-only download manager and playback stub

AnimeDownloadManager saves an episode stream to disk (fire-and-forget,
no progress UI). VideoNotSupportedScreen is the desktop placeholder
shown instead of playing, since no video player library is added."
```

---

### Task 5: AnimeWorld desktop module

**Files:**
- Modify: `settings.gradle.kts` (add `:animeworld:desktop` include)
- Create: `animeworld/desktop/build.gradle.kts`
- Create: `animeworld/desktop/src/commonMain/kotlin/com/programmersbox/desktop/App.kt`
- Create: `animeworld/desktop/src/jvmMain/kotlin/com/programmersbox/desktop/main.kt`
- Create: `animeworld/desktop/src/jvmMain/kotlin/com/programmersbox/desktop/GenericAnimeDesktop.kt`
- Create: `animeworld/desktop/src/jvmMain/kotlin/com/programmersbox/desktop/PlatformSettings.kt`

**Interfaces:**
- Consumes: `GenericSharedAnime(appConfig: AppConfig)` (Task 3), `AnimeDesktopSettings`, `AnimeDownloadManager(scope, animeDesktopSettings, trayState)`, `VideoNotSupportedRoute`, `VideoNotSupportedScreen()` (Task 4).
- Produces: `class GenericAnimeDesktop(appConfig: AppConfig, navigationActions: NavigationActions, animeDownloadManager: AnimeDownloadManager) : GenericSharedAnime(...), PlatformGenericInfo`.

- [ ] **Step 1: Add the module to `settings.gradle.kts`**

Find:

```kotlin
include(":novelworld:desktop")
include(":animeworld:shared")
```

Replace with:

```kotlin
include(":novelworld:desktop")
include(":animeworld:shared")
include(":animeworld:desktop")
```

- [ ] **Step 2: Create `animeworld/desktop/build.gradle.kts`**

```kotlin
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeFeatureFlag

plugins {
    id("kotlin-multiplatform")
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.compose.compiler)
    id("org.jetbrains.compose.hot-reload")
    id("kotlinx-serialization")
}

configurations.all {
    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-android")
}

kotlin {
    jvm()

    compilerOptions {
        freeCompilerArgs.add("-Xwhen-guards")
        freeCompilerArgs.add("-Xcontext-parameters")
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinStLib)
            implementation(projects.kmpuiviews)
            implementation(commonLibs.compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.runtime)
            implementation(compose.ui)
            implementation(compose.uiUtil)
            implementation(compose.foundation)
            implementation(compose.material3AdaptiveNavigationSuite)
            implementation(compose.components.resources)
            implementation(commonLibs.material.kolor)

            implementation(project.dependencies.platform(commonLibs.koin.bom))
            implementation(commonLibs.bundles.koinKmp)

            implementation(projects.favoritesdatabase)
            implementation(projects.datastore)
            implementation(projects.kmpmodels)
            implementation(projects.animeworld.shared)
            implementation(commonLibs.bundles.datastoreLibs)
            implementation(commonLibs.coroutinesCore)
            implementation(desktopLibs.kotlinx.coroutines.swing)
            api(commonLibs.androidx.navigation3.runtime)
            api(commonLibs.filekit.core)
            api(commonLibs.filekit.dialogs.compose)
        }

        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(desktopLibs.kotlinx.coroutines.swing)
        }
    }
}

composeCompiler {
    featureFlags.add(ComposeFeatureFlag.OptimizeNonSkippingGroups)
}

compose.desktop {
    application {
        mainClass = "com.programmersbox.desktop.MainKt"

        jvmArgs("--add-opens", "java.desktop/sun.awt=ALL-UNNAMED")
        jvmArgs("--add-opens", "java.desktop/java.awt.peer=ALL-UNNAMED")

        if (System.getProperty("os.name").contains("Mac")) {
            jvmArgs("--add-opens", "java.desktop/sun.lwawt=ALL-UNNAMED")
            jvmArgs("--add-opens", "java.desktop/sun.lwawt.macosx=ALL-UNNAMED")
        }

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.programmersbox.desktop"
            packageVersion = "1.0.0"
        }
    }
}
```

- [ ] **Step 3: Create the empty `App.kt` commonMain stub**

Create `animeworld/desktop/src/commonMain/kotlin/com/programmersbox/desktop/App.kt`:

```kotlin
package com.programmersbox.desktop
```

- [ ] **Step 4: Create `GenericAnimeDesktop.kt`**

Create `animeworld/desktop/src/jvmMain/kotlin/com/programmersbox/desktop/GenericAnimeDesktop.kt`:

```kotlin
package com.programmersbox.desktop

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DesktopMac
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.programmersbox.anime.shared.GenericSharedAnime
import com.programmersbox.anime.shared.VideoNotSupportedRoute
import com.programmersbox.anime.shared.VideoNotSupportedScreen
import com.programmersbox.anime.shared.downloads.AnimeDownloadManager
import com.programmersbox.kmpmodels.KmpChapterModel
import com.programmersbox.kmpmodels.KmpInfoModel
import com.programmersbox.kmpuiviews.PlatformGenericInfo
import com.programmersbox.kmpuiviews.presentation.navactions.NavigationActions
import com.programmersbox.kmpuiviews.utils.AppConfig
import com.programmersbox.kmpuiviews.utils.ComposeSettingsDsl

class GenericAnimeDesktop(
    appConfig: AppConfig,
    private val navigationActions: NavigationActions,
    private val animeDownloadManager: AnimeDownloadManager,
) : GenericSharedAnime(appConfig = appConfig), PlatformGenericInfo {

    @Composable
    override fun ProfileIcon(): String = ""

    override suspend fun chapterOnClick(
        model: KmpChapterModel,
        allChapters: List<KmpChapterModel>,
        infoModel: KmpInfoModel,
        navController: NavigationActions,
    ) {
        navController.navigate(VideoNotSupportedRoute)
    }

    override fun downloadChapter(
        model: KmpChapterModel,
        allChapters: List<KmpChapterModel>,
        infoModel: KmpInfoModel,
        navController: NavigationActions,
    ) {
        animeDownloadManager.downloadChapter(model, infoModel.title.ifBlank { infoModel.url })
    }

    context(navGraph: EntryProviderScope<NavKey>)
    override fun globalNav3Setup() {
        navGraph.entry<VideoNotSupportedRoute> { VideoNotSupportedScreen() }
    }

    context(navGraph: EntryProviderScope<NavKey>)
    override fun settingsNav3Setup() {
        navGraph.entry<PlatformSettings> { JvmSettingsScreen() }
    }

    override fun composeCustomPreferences(): ComposeSettingsDsl.() -> Unit = {
        viewSettings {
            segmentedListItem(
                content = { Text("Platform Settings") },
                leadingContent = { Icon(Icons.Default.DesktopMac, null) },
                onClick = { navigationActions.navigate(PlatformSettings) }
            )
        }
    }
}
```

- [ ] **Step 5: Create `PlatformSettings.kt`**

Create `animeworld/desktop/src/jvmMain/kotlin/com/programmersbox/desktop/PlatformSettings.kt`:

```kotlin
package com.programmersbox.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dataset
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import ca.gosyer.appdirs.AppDirs
import com.programmersbox.anime.shared.AnimeDesktopSettings
import com.programmersbox.datastore.asState
import com.programmersbox.kmpuiviews.presentation.components.BackButton
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.absolutePath
import io.github.vinceglb.filekit.dialogs.compose.rememberDirectoryPickerLauncher
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject
import java.awt.Desktop
import java.io.File

@Serializable
data object PlatformSettings : NavKey

private const val PLATFORM_SETTINGS_COUNT = 2

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun JvmSettingsScreen() {
    val settings = koinInject<AnimeDesktopSettings>()
    val appDirs = koinInject<AppDirs>()

    val colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("Desktop Settings") },
                navigationIcon = { BackButton() },
                scrollBehavior = scrollBehavior
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
    ) { padding ->
        LazyColumn(
            contentPadding = padding,
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            item(contentType = "downloadChapterPath") {
                var downloadPath by settings
                    .downloadsDirectory
                    .asState()

                val directoryPicker = rememberDirectoryPickerLauncher(
                    directory = PlatformFile(downloadPath)
                ) { file -> file?.let { downloadPath = it.absolutePath() } }

                SegmentedListItem(
                    content = { Text("Download Path") },
                    supportingContent = { Text(downloadPath) },
                    leadingContent = { Icon(Icons.Default.Download, null) },
                    onClick = { directoryPicker.launch() },
                    colors = colors,
                    shapes = ListItemDefaults.segmentedShapes(index = 0, count = PLATFORM_SETTINGS_COUNT)
                )
            }

            item(contentType = "viewFolders") {
                SegmentedListItem(
                    content = { Text("View Data Directory") },
                    supportingContent = { Text("View the directory where the data is stored") },
                    leadingContent = { Icon(Icons.Default.Dataset, null) },
                    onClick = {
                        if (Desktop.isDesktopSupported()) {
                            Desktop.getDesktop().open(File(appDirs.getUserDataDir()))
                        }
                    },
                    colors = colors,
                    shapes = ListItemDefaults.segmentedShapes(index = 1, count = PLATFORM_SETTINGS_COUNT)
                )
            }
        }
    }
}
```

- [ ] **Step 6: Create `main.kt`**

Create `animeworld/desktop/src/jvmMain/kotlin/com/programmersbox/desktop/main.kt`:

```kotlin
package com.programmersbox.desktop

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.window.application
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import ca.gosyer.appdirs.AppDirs
import com.programmersbox.anime.shared.AnimeDesktopSettings
import com.programmersbox.anime.shared.downloads.AnimeDownloadManager
import com.programmersbox.datastore.DataStoreSettings
import com.programmersbox.kmpuiviews.BaseDesktopUi
import com.programmersbox.kmpuiviews.BuildType
import com.programmersbox.kmpuiviews.repository.BackgroundWorkHandlerImpl
import com.programmersbox.kmpuiviews.utils.AppConfig
import com.programmersbox.kmpuiviews.utils.bindsGenericInfo
import dev.nucleusframework.systeminfo.SystemInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import java.io.File

fun main(args: Array<String>) {
    val appDirs = AppDirs {
        appName = "AnimeWorld"
        appAuthor = "jakepurple13"
    }

    DataStoreSettings { File(appDirs.getUserDataDir(), it).absolutePath }

    if (BackgroundWorkHandlerImpl.setupSyncCheckers(args)) return
    val desktopViewModelStoreOwner = DesktopViewModelStoreOwner()
    application {
        CompositionLocalProvider(
            LocalViewModelStoreOwner provides desktopViewModelStoreOwner
        ) {
            BaseDesktopUi(
                title = "AnimeWorld",
                moduleBlock = {
                    modules(
                        module {
                            single {
                                AppConfig(
                                    appName = "AnimeWorld",
                                    buildType = BuildType.NoFirebase,
                                    isDebug = false,
                                    userName = SystemInfo.users().firstOrNull()?.name
                                )
                            }
                            singleOf(::GenericAnimeDesktop) { bindsGenericInfo() }
                            singleOf(::AnimeDesktopSettings)
                            single {
                                AnimeDownloadManager(
                                    scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
                                    animeDesktopSettings = get(),
                                    trayState = get(),
                                )
                            }
                        }
                    )
                }
            )
        }
    }
}

private class DesktopViewModelStoreOwner : ViewModelStoreOwner {
    override val viewModelStore: ViewModelStore = ViewModelStore()
}
```

- [ ] **Step 7: Verify the desktop app launches**

Run:

```bash
./gradlew :animeworld:desktop:run > /tmp/animeworld-desktop-run.log 2>&1 &
RUN_PID=$!
sleep 40
if ! kill -0 $RUN_PID 2>/dev/null; then
  echo "Process exited early:"; cat /tmp/animeworld-desktop-run.log; exit 1
fi
echo "Still running after 40s — OK"
kill $RUN_PID
```

Expected: "Still running after 40s — OK".

- [ ] **Step 8: Commit**

```bash
git add settings.gradle.kts animeworld/desktop
git commit -m "feat(animeworld): add :animeworld:desktop JVM module

Mirrors :mangaworld:desktop's structure. Episode clicks navigate to a
stub \"not supported on desktop yet\" screen; downloads go through the
new basic AnimeDownloadManager. Casting/ExoPlayer are not part of this
target."
```

---

### Task 6: README updates

**Files:**
- Modify: `README.md`

- [ ] **Step 1: Find the existing MangaWorld Desktop section**

Run: `grep -n "Desktop" README.md`

Read the surrounding section (likely a "## MangaWorld Desktop" or similar heading with a build command and feature bullet list) to match its exact heading level and format.

- [ ] **Step 2: Add AnimeWorld Desktop and NovelWorld Desktop sections**

Immediately after the existing MangaWorld Desktop section, add two new sections at the same heading level, in this shape (adjust heading level `##`/`###` to match whatever the MangaWorld section uses):

```markdown
## AnimeWorld Desktop

A JVM/Desktop Compose build of AnimeWorld, sharing UI with the Android app via `:animeworld:shared`.

```bash
./gradlew :animeworld:desktop:run
```

**Works:** browsing, search, favorites, episode lists/details, basic downloads (saves the episode
stream to disk, configurable download folder in Platform Settings).

**Not yet supported:** in-app video playback (clicking an episode shows a placeholder screen) and
Chromecast/casting — both are Android-only for now.

## NovelWorld Desktop

A JVM/Desktop Compose build of NovelWorld, sharing UI with the Android app via `:novelworld:shared`.

```bash
./gradlew :novelworld:desktop:run
```

**Works:** browsing, search, favorites, reading chapters in the built-in reader.

**Not yet supported:** downloads (NovelWorld has no download support on any platform yet).
```

- [ ] **Step 3: Commit**

```bash
git add README.md
git commit -m "docs: document AnimeWorld Desktop and NovelWorld Desktop modules"
```

---

## Self-Review Notes

- **Spec coverage:** settings.gradle.kts scaffolding (Tasks 1/2/3/5), `:animeworld:shared` + `:novelworld:shared` extension (Tasks 1, 3, 4), `:animeworld:desktop` + `:novelworld:desktop` (Tasks 2, 5), stubbed playback (Task 4/5), basic anime downloads (Task 4/5), no novel downloads (Task 1 — `downloadChapter` stays a no-op), no `kmpuiviews` changes (confirmed — no task touches it), README (Task 6). No spec section is uncovered.
- **Placeholder scan:** no TODO/TBD; every step has complete, concrete code.
- **Type consistency:** `GenericSharedNovel(chapterHolder: ChapterHolder)` constructor shape matches across Tasks 1 and 2. `GenericSharedAnime(appConfig: AppConfig)` matches across Tasks 3 and 5. `AnimeDownloadManager(scope, animeDesktopSettings, trayState)` and `AnimeDesktopSettings(appDirs)` match between Task 4 (definition) and Task 5 (construction in `main.kt`/injection in `PlatformSettings.kt`). `VideoNotSupportedRoute`/`VideoNotSupportedScreen` match between Task 4 (definition) and Task 5 (`GenericAnimeDesktop` usage).
