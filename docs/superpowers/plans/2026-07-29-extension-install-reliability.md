# Reliable Extension Install (PackageInstaller) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the Ackpine-based APK install engine with Android's native `PackageInstaller` API, keeping the existing Ktor download and WorkManager orchestration intact, so extension source installs are crash-free with clear per-stage status.

**Architecture:** Three new Android-only classes (`PackageInstallEngine`, `PackageInstallReceiver`, `InstallStatusRepository`) replace Ackpine inside the Android `actual class DownloadAndInstaller`. The manifest-declared receiver captures `PackageInstaller` broadcast callbacks (including the critical `STATUS_PENDING_USER_ACTION` intent) and forwards them into a `StateFlow`-backed repository that `DownloadAndInstaller.install()` collects from — keeping its `Flow<DownloadAndInstallStatus>` contract unchanged so `DownloadAndInstallWorker`, `DownloadStateRepository`, `ExtensionListViewModel`, and the UI screens don't need structural changes, only new `when` branches for the added states.

**Tech Stack:** Kotlin, Kotlin Coroutines/Flow, Koin, `android.content.pm.PackageInstaller`, WorkManager, Jetpack Compose (Compose Multiplatform commonMain).

## Global Constraints

- minSdk 28, compileSdk/targetSdk 37 (`buildSrc/src/main/kotlin/AppInfo.kt`).
- Use the `noFirebase` flavor for all local build/compile verification (per `CLAUDE.md`) — Gradle task variant suffix is `NoFirebaseDebug`.
- Unit tests are explicitly out of scope for this feature (project constraint) — verification steps use Gradle compile/assemble commands, not test suites.
- **Kotlin compile verification command is `./gradlew :kmpuiviews:compileAndroidMain`** (not a flavor-qualified name — `kmpuiviews`'s Kotlin Multiplatform `androidTarget()` has no product-flavor dimension, unlike app modules, so there is no `NoFirebaseDebug`-suffixed variant of this task for it). This single task compiles `kmpuiviews`'s merged `commonMain` + `androidMain` sources together, so it is the correct verification command for every task in this plan that touches Kotlin, whether the file is in `commonMain` or `androidMain`. Do not use `:kmpuiviews:compileCommonMainKotlinMetadata` — it fails independent of this plan on a pre-existing, unrelated issue (`favoritesdatabase`'s `@Database` classes need `@ConstructedBy` for Room 3's KSP metadata target; confirmed by running it against an unmodified checkout — the failing files, `Recommendations.kt` and `DictionaryDatabase.kt`, are untouched by this plan). App-module task names (`:mangaworld:assembleNoFirebaseDebug`, `:mangaworld:assembleNoFirebaseRelease`, `:mangaworld:processNoFirebaseDebugMainManifest`) are flavor-qualified and correct as written elsewhere in this plan — only the `kmpuiviews`-scoped Kotlin compile task is exempt from flavor qualification.
- **Building requires a valid `local.properties`** with `sdk.dir` pointing at an installed Android SDK. If `./gradlew :kmpuiviews:compileAndroidMain` fails immediately with "SDK location not found," create `local.properties` at the repo root with one line: `sdk.dir=<path to your Android SDK>` (commonly `~/Library/Android/sdk` on macOS) — this file is gitignored and machine-specific, so it is never committed.
- **This plan's tasks land in a specific order and the module will not compile cleanly (`BUILD SUCCESSFUL`) until Task 10 completes.** Each task's Kotlin-compile step below states exactly which pre-existing errors (in files not yet touched, scheduled for a later task) are expected and not a sign of that task's own failure — a task is correct if it introduces *zero new errors* and touches *zero unexpected files*, even while the overall module remains red. Only Task 10's compile step should show `BUILD SUCCESSFUL`.
- Ackpine (`ru.solrudev.ackpine:*`) must be fully removed: no `ru.solrudev.ackpine.*` import may remain anywhere in the repo after this plan completes.
- `ConfirmationType` (`IMMEDIATE`/`DEFERRED`) stays in the `DownloadAndInstaller` expect/actual signature as a no-op on Android — do not remove it or change any call site's arguments.
- `DownloadWorker` and `InstallWorker` (the unused chained-worker experiment in `DownloadAndInstallWorker.kt`) are left untouched beyond the minimum required for the file to compile against the new `DownloadAndInstallStatus` shape (an added `InstallErrorReason` argument to existing `Error(...)` constructor calls, and one `else` branch per exhaustive `when` that would otherwise fail to compile). Do not otherwise modify their logic.
- No new expect/actual plumbing for Settings-launching. `PermissionRequired` is guidance text + tap-to-retry only.
- The install queue is sequential via WorkManager `enqueueUniqueWork`, not a custom coordinator/mutex.
- `PackageInstallReceiver` is manifest-declared with `android:exported="false"` and delivered via an explicit-component `Intent`/`PendingIntent` (no `<intent-filter>` action string needed or used).

---

### Task 1: Extend `DownloadAndInstallStatus` and add `InstallErrorReason` (commonMain)

**Files:**
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/utils/DownloadAndInstaller.kt`

**Interfaces:**
- Produces: `DownloadAndInstallStatus.PendingUserAction`, `DownloadAndInstallStatus.PermissionRequired`, `DownloadAndInstallStatus.Cancelled` (all `data object`), `DownloadAndInstallStatus.Error(reason: InstallErrorReason, message: String)` (changed from single-arg), `enum class InstallErrorReason { BLOCKED, CONFLICT, INCOMPATIBLE, INVALID, STORAGE, GENERIC, UNKNOWN }`. All consumed by every later task.

This file currently ends with:
```kotlin
@Serializable
sealed class DownloadAndInstallStatus {
    @Serializable
    data class Downloading(val progress: Float) : DownloadAndInstallStatus()

    @Serializable
    data object Downloaded : DownloadAndInstallStatus()

    @Serializable
    data object Installing : DownloadAndInstallStatus()

    @Serializable
    data object Installed : DownloadAndInstallStatus()

    @Serializable
    data class Error(val message: String) : DownloadAndInstallStatus()
}

enum class ConfirmationType {
    IMMEDIATE,
    DEFERRED
}
```

- [ ] **Step 1: Replace the sealed class and add the new enum**

Replace the whole block above with:

```kotlin
@Serializable
sealed class DownloadAndInstallStatus {
    @Serializable
    data class Downloading(val progress: Float) : DownloadAndInstallStatus()

    @Serializable
    data object Downloaded : DownloadAndInstallStatus()

    @Serializable
    data object Installing : DownloadAndInstallStatus()

    @Serializable
    data object PendingUserAction : DownloadAndInstallStatus()

    @Serializable
    data object PermissionRequired : DownloadAndInstallStatus()

    @Serializable
    data object Installed : DownloadAndInstallStatus()

    @Serializable
    data object Cancelled : DownloadAndInstallStatus()

    @Serializable
    data class Error(val reason: InstallErrorReason, val message: String) : DownloadAndInstallStatus()
}

@Serializable
enum class InstallErrorReason {
    BLOCKED, CONFLICT, INCOMPATIBLE, INVALID, STORAGE, GENERIC, UNKNOWN,
}

enum class ConfirmationType {
    IMMEDIATE,
    DEFERRED
}
```

- [ ] **Step 2: Verify**

Run: `./gradlew :kmpuiviews:compileAndroidMain`
Expected: BUILD FAILED, with errors in exactly these pre-existing, not-yet-migrated locations and nowhere else (the errors below prove the new 2-arg `Error` and 3 new sealed cases took effect and are now enforced — that's the point of this check, not a clean build):
- `kmpuiviews/src/androidMain/kotlin/com/programmersbox/kmpuiviews/utils/DownloadAndInstaller.kt` (still calls the old 1-arg `Error(message)` — fixed in Task 5)
- `kmpuiviews/src/androidMain/kotlin/com/programmersbox/kmpuiviews/workers/DownloadAndInstallWorker.kt`, multiple lines (old 1-arg `Error(...)` calls and non-exhaustive `when`s — fixed in Task 8)
- `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/settings/downloadstate/DownloadStateScreen.kt` (non-exhaustive `when` — fixed in Task 9)
- `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/settings/prerelease/PrereleaseScreen.kt` (non-exhaustive `when` — fixed in Task 10)

If any error appears outside these four files, that's a real regression from this task's change — stop and fix it before committing.

- [ ] **Step 3: Commit**

```bash
git add kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/utils/DownloadAndInstaller.kt
git commit -m "feat(install): extend DownloadAndInstallStatus with PendingUserAction, PermissionRequired, Cancelled, and typed Error reason"
```

---

### Task 2: Add `InstallStatusRepository` (androidMain)

**Files:**
- Create: `kmpuiviews/src/androidMain/kotlin/com/programmersbox/kmpuiviews/repository/InstallStatusRepository.kt`

**Interfaces:**
- Consumes: `DownloadAndInstallStatus` (Task 1).
- Produces: `class InstallStatusRepository { fun flowFor(sessionId: Int): Flow<DownloadAndInstallStatus>; fun update(sessionId: Int, status: DownloadAndInstallStatus); fun registerTempFile(sessionId: Int, file: File); fun consumeTempFile(sessionId: Int): File?; fun clear(sessionId: Int) }` — consumed by Task 4 (receiver) and Task 5 (android actual).

- [ ] **Step 1: Create the repository**

```kotlin
package com.programmersbox.kmpuiviews.repository

import com.programmersbox.kmpuiviews.utils.DownloadAndInstallStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.update
import java.io.File

class InstallStatusRepository {
    private val statuses = MutableStateFlow<Map<Int, DownloadAndInstallStatus>>(emptyMap())
    private val tempFiles = mutableMapOf<Int, File>()

    fun flowFor(sessionId: Int): Flow<DownloadAndInstallStatus> =
        statuses.mapNotNull { it[sessionId] }

    fun update(sessionId: Int, status: DownloadAndInstallStatus) {
        statuses.update { it + (sessionId to status) }
    }

    @Synchronized
    fun registerTempFile(sessionId: Int, file: File) {
        tempFiles[sessionId] = file
    }

    @Synchronized
    fun consumeTempFile(sessionId: Int): File? = tempFiles.remove(sessionId)

    fun clear(sessionId: Int) {
        statuses.update { it - sessionId }
    }
}
```

`registerTempFile`/`consumeTempFile` are `@Synchronized` because they're written from `DownloadAndInstaller.install()`'s calling coroutine and read from `PackageInstallReceiver.onReceive`, which runs on the main thread outside that coroutine's dispatcher.

- [ ] **Step 2: Verify**

Run: `./gradlew :kmpuiviews:compileAndroidMain`
Expected: BUILD FAILED, with the exact same four pre-existing error locations listed in Task 1's verification step, and no others. This new file has no dependents yet and doesn't touch any of those four files, so the error set should be identical to Task 1's — if it isn't, something in this task's code is broken.

- [ ] **Step 3: Commit**

```bash
git add kmpuiviews/src/androidMain/kotlin/com/programmersbox/kmpuiviews/repository/InstallStatusRepository.kt
git commit -m "feat(install): add InstallStatusRepository to bridge PackageInstaller broadcasts to Flow"
```

---

### Task 3: Add `PackageInstallEngine` (androidMain)

**Files:**
- Create: `kmpuiviews/src/androidMain/kotlin/com/programmersbox/kmpuiviews/utils/PackageInstallEngine.kt`

**Interfaces:**
- Consumes: `com.programmersbox.kmpuiviews.receivers.PackageInstallReceiver` (class reference only, created in Task 4 — this task references it by name before it exists, which is fine since Kotlin resolves the whole module together, but compile verification for this task alone will fail until Task 4 lands; see note in Step 2).
- Produces: `class PackageInstallEngine(context: Context) { fun canRequestPackageInstalls(): Boolean; fun commit(file: File): Int; fun abandon(sessionId: Int) }` — consumed by Task 5.

- [ ] **Step 1: Create the engine**

```kotlin
package com.programmersbox.kmpuiviews.utils

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import com.programmersbox.kmpuiviews.receivers.PackageInstallReceiver
import java.io.File

class PackageInstallEngine(private val context: Context) {

    private val packageInstaller
        get() = context.packageManager.packageInstaller

    fun canRequestPackageInstalls(): Boolean =
        context.packageManager.canRequestPackageInstalls()

    fun commit(file: File): Int {
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
        params.setSize(file.length())
        val sessionId = packageInstaller.createSession(params)

        packageInstaller.openSession(sessionId).use { session ->
            session.openWrite(file.name, 0, file.length()).use { out ->
                file.inputStream().use { input -> input.copyTo(out) }
                session.fsync(out)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                sessionId,
                Intent(context, PackageInstallReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or
                    (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0)
            )

            session.commit(pendingIntent.intentSender)
        }

        return sessionId
    }

    fun abandon(sessionId: Int) {
        runCatching { packageInstaller.abandonSession(sessionId) }
    }
}
```

`abandon` swallows failures via `runCatching` because calling it on an already-finalized session throws — that race (cleanup running just as the receiver's terminal broadcast arrives) is expected and not an error.

- [ ] **Step 2: Note on verification order**

This task references `PackageInstallReceiver`, which doesn't exist until Task 4. Do not run a standalone compile after this task — proceed directly to Task 4, then verify both together at the end of Task 4.

- [ ] **Step 3: Commit**

```bash
git add kmpuiviews/src/androidMain/kotlin/com/programmersbox/kmpuiviews/utils/PackageInstallEngine.kt
git commit -m "feat(install): add PackageInstallEngine wrapping native PackageInstaller session creation"
```

---

### Task 4: Add `PackageInstallReceiver` (androidMain)

**Files:**
- Create: `kmpuiviews/src/androidMain/kotlin/com/programmersbox/kmpuiviews/receivers/PackageInstallReceiver.kt`

**Interfaces:**
- Consumes: `InstallStatusRepository` (Task 2), `DownloadAndInstallStatus`/`InstallErrorReason` (Task 1).
- Produces: `class PackageInstallReceiver : BroadcastReceiver()` — referenced by class name in Task 3 and registered in the manifest in Task 6.

- [ ] **Step 1: Create the receiver**

```kotlin
package com.programmersbox.kmpuiviews.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import com.programmersbox.kmpuiviews.repository.InstallStatusRepository
import com.programmersbox.kmpuiviews.utils.DownloadAndInstallStatus
import com.programmersbox.kmpuiviews.utils.InstallErrorReason
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class PackageInstallReceiver : BroadcastReceiver(), KoinComponent {

    private val installStatusRepository: InstallStatusRepository by inject()

    override fun onReceive(context: Context, intent: Intent) {
        val sessionId = intent.getIntExtra(PackageInstaller.EXTRA_SESSION_ID, -1)
        if (sessionId == -1) return

        when (intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                installStatusRepository.update(sessionId, DownloadAndInstallStatus.PendingUserAction)
                confirmationIntent(intent)?.let {
                    context.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
            }

            PackageInstaller.STATUS_SUCCESS -> {
                installStatusRepository.update(sessionId, DownloadAndInstallStatus.Installed)
                installStatusRepository.consumeTempFile(sessionId)?.delete()
            }

            PackageInstaller.STATUS_FAILURE_ABORTED -> {
                installStatusRepository.update(sessionId, DownloadAndInstallStatus.Cancelled)
                installStatusRepository.consumeTempFile(sessionId)?.delete()
            }

            PackageInstaller.STATUS_FAILURE_BLOCKED ->
                fail(sessionId, InstallErrorReason.BLOCKED, intent)

            PackageInstaller.STATUS_FAILURE_CONFLICT ->
                fail(sessionId, InstallErrorReason.CONFLICT, intent)

            PackageInstaller.STATUS_FAILURE_INCOMPATIBLE ->
                fail(sessionId, InstallErrorReason.INCOMPATIBLE, intent)

            PackageInstaller.STATUS_FAILURE_INVALID ->
                fail(sessionId, InstallErrorReason.INVALID, intent)

            PackageInstaller.STATUS_FAILURE_STORAGE ->
                fail(sessionId, InstallErrorReason.STORAGE, intent)

            else -> fail(sessionId, InstallErrorReason.GENERIC, intent)
        }
    }

    private fun fail(sessionId: Int, reason: InstallErrorReason, intent: Intent) {
        val detail = when (reason) {
            InstallErrorReason.BLOCKED ->
                intent.getStringExtra(PackageInstaller.EXTRA_OTHER_PACKAGE_NAME)?.let { "Blocked by $it" }

            InstallErrorReason.CONFLICT ->
                intent.getStringExtra(PackageInstaller.EXTRA_OTHER_PACKAGE_NAME)?.let { "Conflicts with $it" }

            InstallErrorReason.STORAGE ->
                intent.getStringExtra(PackageInstaller.EXTRA_STORAGE_PATH)?.let { "Not enough storage at $it" }

            else -> null
        } ?: intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE) ?: reason.name

        installStatusRepository.update(sessionId, DownloadAndInstallStatus.Error(reason, detail))
        installStatusRepository.consumeTempFile(sessionId)?.delete()
    }

    @Suppress("DEPRECATION")
    private fun confirmationIntent(intent: Intent): Intent? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
        } else {
            intent.getParcelableExtra(Intent.EXTRA_INTENT)
        }
}
```

- [ ] **Step 2: Verify Tasks 3 and 4 compile together**

Run: `./gradlew :kmpuiviews:compileAndroidMain`
Expected: BUILD FAILED, with the exact same four pre-existing error locations listed in Task 1's verification step, and no others. Neither `PackageInstallEngine.kt` nor `PackageInstallReceiver.kt` should themselves produce any error — they reference each other and `InstallStatusRepository` correctly but nothing yet calls into them from `DownloadAndInstaller.kt` (that wiring is Task 5).

- [ ] **Step 3: Commit**

```bash
git add kmpuiviews/src/androidMain/kotlin/com/programmersbox/kmpuiviews/receivers/PackageInstallReceiver.kt
git commit -m "feat(install): add PackageInstallReceiver handling PackageInstaller status broadcasts"
```

---

### Task 5: Rewrite the Android `actual class DownloadAndInstaller`

**Files:**
- Modify: `kmpuiviews/src/androidMain/kotlin/com/programmersbox/kmpuiviews/utils/DownloadAndInstaller.kt` (full rewrite)

**Interfaces:**
- Consumes: `PackageInstallEngine` (Task 3), `InstallStatusRepository` (Task 2), `DownloadAndInstallStatus`/`InstallErrorReason` (Task 1).
- Produces: `actual class DownloadAndInstaller(context: Context, packageInstallEngine: PackageInstallEngine, installStatusRepository: InstallStatusRepository)` with unchanged public method signatures (`uninstall`, `downloadAndInstall`, `download`, `install`) — consumed unchanged by `DownloadAndInstallWorker`, `DownloadStateRepository`, `ExtensionListViewModel`, `PrereleaseViewModel` (Koin resolves the two new constructor params automatically once Task 7 registers them).

This is a full-file replacement. The whole file becomes:

```kotlin
package com.programmersbox.kmpuiviews.utils

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.google.firebase.perf.trace
import com.programmersbox.kmpuiviews.logFirebaseMessage
import com.programmersbox.kmpuiviews.repository.InstallStatusRepository
import io.github.vinceglb.filekit.AndroidFile
import io.github.vinceglb.filekit.PlatformFile
import io.ktor.client.HttpClient
import io.ktor.client.plugins.onDownload
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.util.cio.writeChannel
import io.ktor.utils.io.copyAndClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.transformWhile
import java.io.File

actual class DownloadAndInstaller(
    private val context: Context,
    private val packageInstallEngine: PackageInstallEngine,
    private val installStatusRepository: InstallStatusRepository,
) {
    private val client = HttpClient()

    actual suspend fun uninstall(packageName: String) {
        context.startActivity(
            Intent(Intent.ACTION_UNINSTALL_PACKAGE, "package:$packageName".toUri())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    actual fun downloadAndInstall(
        url: String,
        destinationPath: String,
        confirmationType: ConfirmationType,
    ): Flow<DownloadAndInstallStatus> {
        val file = File(context.cacheDir, "${url.toUri().lastPathSegment}.apk")

        return channelFlow<DownloadAndInstallStatus> {
            trace("download_and_install") {
                client.prepareGet(url) {
                    onDownload { bytesSentTotal, contentLength ->
                        send(DownloadAndInstallStatus.Downloading(bytesSentTotal.toFloat() / (contentLength ?: 1L)))
                    }
                }.execute {
                    it.bodyAsChannel().copyAndClose(file.writeChannel())
                    send(DownloadAndInstallStatus.Downloaded)
                }

                printLogs { "Starting Install Session" }

                install(PlatformFile(file), confirmationType)
                    .onEach { send(it) }
                    .launchIn(this@channelFlow)
            }
        }
            .catch {
                it.printStackTrace()
                emit(DownloadAndInstallStatus.Error(InstallErrorReason.GENERIC, it.message ?: "Unknown error"))
            }
            .onEach {
                printLogs { it }
                if (it !is DownloadAndInstallStatus.Downloading) logFirebaseMessage(it.toString())
            }
            .onCompletion { cause -> if (cause != null) file.delete() }
    }

    actual fun download(
        url: String,
        destinationPath: String,
    ): Flow<DownloadAndInstallStatus> {
        val file = File(context.cacheDir, "${url.toUri().lastPathSegment}.apk")

        return channelFlow<DownloadAndInstallStatus> {
            trace("download") {
                client.prepareGet(url) {
                    onDownload { bytesSentTotal, contentLength ->
                        send(DownloadAndInstallStatus.Downloading(bytesSentTotal.toFloat() / (contentLength ?: 1L)))
                    }
                }.execute {
                    it.bodyAsChannel().copyAndClose(file.writeChannel())
                    send(DownloadAndInstallStatus.Downloaded)
                }
            }
        }
            .catch {
                it.printStackTrace()
                emit(DownloadAndInstallStatus.Error(InstallErrorReason.GENERIC, it.message ?: "Unknown error"))
            }
            .onEach {
                printLogs { it }
                if (it !is DownloadAndInstallStatus.Downloading) logFirebaseMessage(it.toString())
            }
            .onCompletion { cause -> if (cause != null) file.delete() }
    }

    actual fun install(
        file: PlatformFile,
        confirmationType: ConfirmationType,
    ): Flow<DownloadAndInstallStatus> {
        var sessionId: Int? = null
        var terminalReached = false

        return flow {
            if (!packageInstallEngine.canRequestPackageInstalls()) {
                emit(DownloadAndInstallStatus.PermissionRequired)
                return@flow
            }

            val localFile = resolveLocalFile(file)

            sessionId = runCatching { packageInstallEngine.commit(localFile) }
                .onFailure {
                    emit(DownloadAndInstallStatus.Error(InstallErrorReason.GENERIC, it.message ?: "Unable to start install"))
                }
                .getOrNull()
            val id = sessionId ?: return@flow

            installStatusRepository.registerTempFile(id, localFile)
            emit(DownloadAndInstallStatus.Installing)

            emitAll(
                installStatusRepository.flowFor(id).transformWhile { status ->
                    emit(status)
                    val terminal = status is DownloadAndInstallStatus.Installed ||
                        status is DownloadAndInstallStatus.Cancelled ||
                        status is DownloadAndInstallStatus.Error
                    if (terminal) terminalReached = true
                    !terminal
                }
            )
        }.onCompletion {
            val id = sessionId
            if (id != null && !terminalReached) {
                packageInstallEngine.abandon(id)
                installStatusRepository.consumeTempFile(id)?.delete()
                installStatusRepository.clear(id)
            }
        }
    }

    private fun resolveLocalFile(file: PlatformFile): File =
        when (val androidFile = file.androidFile) {
            is AndroidFile.FileWrapper -> androidFile.file
            is AndroidFile.UriWrapper -> File(context.cacheDir, "install_${androidFile.uri.hashCode()}.apk").also { copy ->
                context.contentResolver.openInputStream(androidFile.uri)?.use { input ->
                    copy.outputStream().use { input.copyTo(it) }
                }
            }
        }
}
```

Notes on what changed from the Ackpine version:
- All `ru.solrudev.ackpine.*` imports are gone.
- `uninstall` no longer suspends on a session result — it fires `ACTION_UNINSTALL_PACKAGE` and returns; the OS shows its own confirmation UI independently of our process.
- The explicit `if (it is DownloadAndInstallStatus.Installed) file.delete()` from the old code is removed — cleanup now lives in `PackageInstallReceiver`/`InstallStatusRepository` (Task 4) and the new `onCompletion` blocks here, which is the single place responsible for it now.
- `install()`'s cancellation handling (`onCompletion` calling `abandon` + delete) satisfies the cancel-mid-install requirement; `downloadAndInstall`/`download`'s `onCompletion` satisfies cancel-mid-download.

- [ ] **Step 1: Replace the file with the code above.**

- [ ] **Step 2: Verify**

Run: `./gradlew :kmpuiviews:compileAndroidMain`
Expected: BUILD FAILED, but `DownloadAndInstaller.kt` itself should now be error-free — it's fully rewritten and no longer calls the old 1-arg `Error(...)`. Remaining errors should only be in `DownloadAndInstallWorker.kt` (fixed in Task 8), `DownloadStateScreen.kt` (Task 9), and `PrereleaseScreen.kt` (Task 10). If `DownloadAndInstaller.kt` itself still shows an error, check for a typo against the code block above before proceeding. (Koin's `singleOf(::DownloadAndInstaller)` in `AppModule.android.kt` doesn't get its two new constructor params wired up until Task 7 — that's a runtime DI concern, not a compile-time one, since `PackageInstallEngine`/`InstallStatusRepository` already exist as real classes from Tasks 2–3, so it produces no compile error here.)

- [ ] **Step 3: Commit**

```bash
git add kmpuiviews/src/androidMain/kotlin/com/programmersbox/kmpuiviews/utils/DownloadAndInstaller.kt
git commit -m "feat(install): replace Ackpine with native PackageInstaller in DownloadAndInstaller"
```

---

### Task 6: Manifest registration for `PackageInstallReceiver`

**Files:**
- Modify: `kmpuiviews/src/androidMain/AndroidManifest.xml`

**Interfaces:**
- Consumes: `com.programmersbox.kmpuiviews.receivers.PackageInstallReceiver` (Task 4).

- [ ] **Step 1: Add the receiver declaration**

In `kmpuiviews/src/androidMain/AndroidManifest.xml`, inside the existing `<application>` block, alongside the other `<receiver>` entries, add:

```xml
        <receiver
            android:name=".receivers.PackageInstallReceiver"
            android:exported="false" />
```

The full `<application>` block should read:

```xml
    <application>

        <service
            android:name="androidx.work.impl.foreground.SystemForegroundService"
            android:foregroundServiceType="dataSync"
            tools:node="merge" />

        <receiver
            android:name=".receivers.BootReceived"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.BOOT_COMPLETED" />
                <action android:name="android.intent.action.PACKAGE_FIRST_LAUNCH" />
            </intent-filter>
        </receiver>

        <receiver
            android:name=".receivers.DeleteNotificationReceiver"
            android:enabled="true"
            android:exported="true" />

        <receiver
            android:name=".receivers.SwipeAwayReceiver"
            android:enabled="true"
            android:exported="true" />

        <receiver
            android:name=".receivers.PackageInstallReceiver"
            android:exported="false" />

    </application>
```

No `<intent-filter>` is needed: `PackageInstallEngine.commit()` (Task 3) targets this receiver via an explicit-component `Intent(context, PackageInstallReceiver::class.java)`, which is delivered by class name regardless of intent filters.

- [ ] **Step 2: Verify the manifest merges cleanly**

Run: `./gradlew :mangaworld:processNoFirebaseDebugMainManifest`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add kmpuiviews/src/androidMain/AndroidManifest.xml
git commit -m "feat(install): register PackageInstallReceiver in the manifest"
```

---

### Task 7: Koin registration and Ackpine dependency removal

**Files:**
- Modify: `kmpuiviews/src/androidMain/kotlin/com/programmersbox/kmpuiviews/di/AppModule.android.kt:25-27`
- Modify: `kmpuiviews/build.gradle.kts:185-186`
- Modify: `gradle/android.versions.toml:2,54-55`

**Interfaces:**
- Consumes: `PackageInstallEngine` (Task 3), `InstallStatusRepository` (Task 2).

- [ ] **Step 1: Register the two new singletons**

In `kmpuiviews/src/androidMain/kotlin/com/programmersbox/kmpuiviews/di/AppModule.android.kt`, change:

```kotlin
actual fun platformModule(): Module = module {
    singleOf(::DownloadAndInstaller)
    singleOf(::IconLoader)
```

to:

```kotlin
actual fun platformModule(): Module = module {
    singleOf(::PackageInstallEngine)
    singleOf(::InstallStatusRepository)
    singleOf(::DownloadAndInstaller)
    singleOf(::IconLoader)
```

Add the two matching imports at the top of the file:

```kotlin
import com.programmersbox.kmpuiviews.repository.InstallStatusRepository
import com.programmersbox.kmpuiviews.utils.PackageInstallEngine
```

- [ ] **Step 2: Remove the Ackpine dependency declarations**

In `kmpuiviews/build.gradle.kts`, delete these two lines from the `androidMain.dependencies` block:

```kotlin
                implementation(androidLibs.ackpine.core)
                implementation(androidLibs.ackpine.ktx)
```

In `gradle/android.versions.toml`, delete:

```toml
ackpineVersion = "0.25.2"
```

and:

```toml
ackpine-core = { module = "ru.solrudev.ackpine:ackpine-core", version.ref = "ackpineVersion" }
ackpine-ktx = { module = "ru.solrudev.ackpine:ackpine-ktx", version.ref = "ackpineVersion" }
```

- [ ] **Step 3: Confirm no Ackpine references remain**

Run: `grep -rn "ackpine" --include="*.kt" --include="*.kts" --include="*.toml" .`
Expected: no output.

- [ ] **Step 4: Verify**

Run: `./gradlew :kmpuiviews:compileAndroidMain`
Expected: BUILD FAILED, with the exact same remaining errors as after Task 5 (`DownloadAndInstallWorker.kt`, `DownloadStateScreen.kt`, `PrereleaseScreen.kt` — fixed in Tasks 8–10) and no others. `AppModule.android.kt` itself should be error-free — this confirms the Koin wiring compiles and Ackpine is fully gone from this module's dependency graph. A full app assemble (`:mangaworld:assembleNoFirebaseDebug`) isn't expected to succeed until Task 10 lands, since it depends on the same not-yet-fixed files.

- [ ] **Step 5: Commit**

```bash
git add kmpuiviews/src/androidMain/kotlin/com/programmersbox/kmpuiviews/di/AppModule.android.kt kmpuiviews/build.gradle.kts gradle/android.versions.toml
git commit -m "feat(install): wire PackageInstallEngine/InstallStatusRepository into DI, remove Ackpine dependency"
```

---

### Task 8: `DownloadAndInstallWorker` — sequential queue, new states, error-shape fix

**Files:**
- Modify: `kmpuiviews/src/androidMain/kotlin/com/programmersbox/kmpuiviews/workers/DownloadAndInstallWorker.kt`

**Interfaces:**
- Consumes: `DownloadAndInstallStatus`/`InstallErrorReason` (Task 1).

- [ ] **Step 1: Add the `ExistingWorkPolicy` import**

At the top of the file, add:

```kotlin
import androidx.work.ExistingWorkPolicy
```

- [ ] **Step 2: Serialize install requests via `enqueueUniqueWork`**

In `DownloadAndInstallWorker.Companion.downloadAndInstall`, replace:

```kotlin
        fun downloadAndInstall(context: Context, url: String) {
            WorkManager.getInstance(context)
                .enqueue(
                    OneTimeWorkRequestBuilder<DownloadAndInstallWorker>()
                        .setInputData(workDataOf("url" to url))
                        .addTag("downloadAndInstall")
                        .keepResultsForAtLeast(10, TimeUnit.MINUTES)
                        .setConstraints(
                            Constraints.Builder()
                                .setRequiredNetworkType(NetworkType.CONNECTED)
                                .build()
                        )
                        .build()
                )
        }
```

with:

```kotlin
        fun downloadAndInstall(context: Context, url: String) {
            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    "downloadAndInstall",
                    ExistingWorkPolicy.APPEND_OR_REPLACE,
                    OneTimeWorkRequestBuilder<DownloadAndInstallWorker>()
                        .setInputData(workDataOf("url" to url))
                        .addTag("downloadAndInstall")
                        .keepResultsForAtLeast(10, TimeUnit.MINUTES)
                        .setConstraints(
                            Constraints.Builder()
                                .setRequiredNetworkType(NetworkType.CONNECTED)
                                .build()
                        )
                        .build()
                )
        }
```

- [ ] **Step 3: Add the new states to the active worker's notification `when`**

In `DownloadAndInstallWorker.doWork()`, the `notify(notificationLogo, url) { when (it) { ... } }` block currently ends with the `Installed` branch. Add three branches after it and enrich the `Error` branch's text:

Replace:

```kotlin
                            is DownloadAndInstallStatus.Error -> {
                                setContentText("Error during download and install.")
                                    .setProgress(0, 0, false)
                            }
```

with:

```kotlin
                            is DownloadAndInstallStatus.Error -> {
                                setContentText("Error: ${it.message}")
                                    .setProgress(0, 0, false)
                            }
```

and replace:

```kotlin
                            DownloadAndInstallStatus.Installed -> {
                                setContentText("Download and install completed.")
                                    .setProgress(0, 0, false)
                                    .setTimeoutAfter(5000)
                            }
                        }
                    }

                    //TODO: Replace this with the repository
                    setProgress(
                        workDataOf(
                            "url" to url,
                            "progress" to it.javaClass.name,
                            "progressAmount" to when (it) {
                                is DownloadAndInstallStatus.Downloading -> it.progress
                                else -> 0
                            },
                            "error" to when (it) {
                                is DownloadAndInstallStatus.Error -> it.message
                                else -> null
                            }
                        )
                    )
                }
                .collect()
        }
            .onSuccess { delay(5000) }

        NotificationManagerCompat
            .from(applicationContext)
            .cancel(NOTIFICATION_ID + url.hashCode())

        return Result.success()
    }

    @SuppressLint("MissingPermission")
    private fun notify(
        logo: NotificationLogo,
        url: String,
        buildMore: NotificationCompat.Builder.() -> NotificationCompat.Builder,
    ) {
```

with:

```kotlin
                            DownloadAndInstallStatus.Installed -> {
                                setContentText("Download and install completed.")
                                    .setProgress(0, 0, false)
                                    .setTimeoutAfter(5000)
                            }

                            DownloadAndInstallStatus.PendingUserAction -> {
                                setContentText("Waiting for install confirmation...")
                                    .setProgress(0, 0, false)
                            }

                            DownloadAndInstallStatus.PermissionRequired -> {
                                setContentText("Enable install permission in Settings, then retry.")
                                    .setProgress(0, 0, false)
                            }

                            DownloadAndInstallStatus.Cancelled -> {
                                setContentText("Install cancelled.")
                                    .setProgress(0, 0, false)
                            }
                        }
                    }

                    //TODO: Replace this with the repository
                    setProgress(
                        workDataOf(
                            "url" to url,
                            "progress" to it.javaClass.name,
                            "progressAmount" to when (it) {
                                is DownloadAndInstallStatus.Downloading -> it.progress
                                else -> 0
                            },
                            "error" to when (it) {
                                is DownloadAndInstallStatus.Error -> it.message
                                else -> null
                            }
                        )
                    )
                }
                .collect()
        }
            .onSuccess { delay(5000) }

        NotificationManagerCompat
            .from(applicationContext)
            .cancel(NOTIFICATION_ID + url.hashCode())

        return Result.success()
    }

    @SuppressLint("MissingPermission")
    private fun notify(
        logo: NotificationLogo,
        url: String,
        buildMore: NotificationCompat.Builder.() -> NotificationCompat.Builder,
    ) {
```

- [ ] **Step 4: Add matching branches to `listToDownloads()` and fix the `Error(...)` constructor calls**

In `DownloadAndInstallWorker.Companion.listToDownloads`, replace:

```kotlin
                        status = if (it.state == WorkInfo.State.CANCELLED) {
                            DownloadAndInstallStatus.Error(it.progress.getString("error") ?: "Unknown error")
                        } else {
                            when (it.progress.getString("progress")) {
                                "com.programmersbox.kmpuiviews.utils.DownloadAndInstallStatus\$Downloading" ->
                                    DownloadAndInstallStatus.Downloading(it.progress.getFloat("progressAmount", 0f))

                                "com.programmersbox.kmpuiviews.utils.DownloadAndInstallStatus\$Downloaded" ->
                                    DownloadAndInstallStatus.Downloaded

                                "com.programmersbox.kmpuiviews.utils.DownloadAndInstallStatus\$Installing" ->
                                    DownloadAndInstallStatus.Installing

                                "com.programmersbox.kmpuiviews.utils.DownloadAndInstallStatus\$Installed" ->
                                    DownloadAndInstallStatus.Installed

                                else -> DownloadAndInstallStatus.Error(it.progress.getString("error") ?: "Unknown error")
                            }
                        }
```

with:

```kotlin
                        status = if (it.state == WorkInfo.State.CANCELLED) {
                            DownloadAndInstallStatus.Error(InstallErrorReason.UNKNOWN, it.progress.getString("error") ?: "Unknown error")
                        } else {
                            when (it.progress.getString("progress")) {
                                "com.programmersbox.kmpuiviews.utils.DownloadAndInstallStatus\$Downloading" ->
                                    DownloadAndInstallStatus.Downloading(it.progress.getFloat("progressAmount", 0f))

                                "com.programmersbox.kmpuiviews.utils.DownloadAndInstallStatus\$Downloaded" ->
                                    DownloadAndInstallStatus.Downloaded

                                "com.programmersbox.kmpuiviews.utils.DownloadAndInstallStatus\$Installing" ->
                                    DownloadAndInstallStatus.Installing

                                "com.programmersbox.kmpuiviews.utils.DownloadAndInstallStatus\$PendingUserAction" ->
                                    DownloadAndInstallStatus.PendingUserAction

                                "com.programmersbox.kmpuiviews.utils.DownloadAndInstallStatus\$PermissionRequired" ->
                                    DownloadAndInstallStatus.PermissionRequired

                                "com.programmersbox.kmpuiviews.utils.DownloadAndInstallStatus\$Cancelled" ->
                                    DownloadAndInstallStatus.Cancelled

                                "com.programmersbox.kmpuiviews.utils.DownloadAndInstallStatus\$Installed" ->
                                    DownloadAndInstallStatus.Installed

                                else -> DownloadAndInstallStatus.Error(InstallErrorReason.UNKNOWN, it.progress.getString("error") ?: "Unknown error")
                            }
                        }
```

Add the import at the top of the file:

```kotlin
import com.programmersbox.kmpuiviews.utils.InstallErrorReason
```

- [ ] **Step 5: Fix the two remaining `Error(...)` single-arg call sites so the file compiles**

These are in the unused `DownloadWorker`/`InstallWorker` chained-worker experiment further down the same file (left otherwise untouched per the Global Constraints). In `DownloadWorker.Companion.listToDownloads`, replace:

```kotlin
                        status = if (it.state == WorkInfo.State.CANCELLED) {
                            DownloadAndInstallStatus.Error(it.progress.getString("error") ?: "Unknown error")
                        } else {
                            when (it.progress.getString("progress")) {
                                "com.programmersbox.uiviews.presentation.settings.downloadstate.DownloadAndInstallStatus\$Downloading" ->
                                    DownloadAndInstallStatus.Downloading(it.progress.getFloat("progressAmount", 0f))

                                "com.programmersbox.uiviews.presentation.settings.downloadstate.DownloadAndInstallStatus\$Downloaded" ->
                                    DownloadAndInstallStatus.Downloaded

                                "com.programmersbox.uiviews.presentation.settings.downloadstate.DownloadAndInstallStatus\$Installing" ->
                                    DownloadAndInstallStatus.Installing

                                "com.programmersbox.uiviews.presentation.settings.downloadstate.DownloadAndInstallStatus\$Installed" ->
                                    DownloadAndInstallStatus.Installed

                                else -> DownloadAndInstallStatus.Error(it.progress.getString("error") ?: "Unknown error")
                            }
                        }
```

with:

```kotlin
                        status = if (it.state == WorkInfo.State.CANCELLED) {
                            DownloadAndInstallStatus.Error(InstallErrorReason.UNKNOWN, it.progress.getString("error") ?: "Unknown error")
                        } else {
                            when (it.progress.getString("progress")) {
                                "com.programmersbox.uiviews.presentation.settings.downloadstate.DownloadAndInstallStatus\$Downloading" ->
                                    DownloadAndInstallStatus.Downloading(it.progress.getFloat("progressAmount", 0f))

                                "com.programmersbox.uiviews.presentation.settings.downloadstate.DownloadAndInstallStatus\$Downloaded" ->
                                    DownloadAndInstallStatus.Downloaded

                                "com.programmersbox.uiviews.presentation.settings.downloadstate.DownloadAndInstallStatus\$Installing" ->
                                    DownloadAndInstallStatus.Installing

                                "com.programmersbox.uiviews.presentation.settings.downloadstate.DownloadAndInstallStatus\$Installed" ->
                                    DownloadAndInstallStatus.Installed

                                else -> DownloadAndInstallStatus.Error(InstallErrorReason.UNKNOWN, it.progress.getString("error") ?: "Unknown error")
                            }
                        }
```

- [ ] **Step 6: Add `else` fallbacks to the two dead workers' notification `when` blocks**

In both `DownloadWorker.doWork()` and `InstallWorker.doWork()`, their `notify(...) { when (it) { ... } }` blocks are exhaustive over the old 5-case `DownloadAndInstallStatus` and will fail to compile now that 3 more cases exist. In each of the two blocks (they're identical in shape), add one line right before the block's closing `}` — after the existing `DownloadAndInstallStatus.Installed -> { ... }` branch:

```kotlin
                            else -> {
                                setContentText("Status: $it")
                                    .setProgress(0, 0, false)
                            }
```

- [ ] **Step 7: Verify**

Run: `./gradlew :kmpuiviews:compileAndroidMain`
Expected: BUILD FAILED, but `DownloadAndInstallWorker.kt` should now be error-free. Remaining errors should only be in `DownloadStateScreen.kt` (fixed in Task 9) and `PrereleaseScreen.kt` (fixed in Task 10).

- [ ] **Step 8: Commit**

```bash
git add kmpuiviews/src/androidMain/kotlin/com/programmersbox/kmpuiviews/workers/DownloadAndInstallWorker.kt
git commit -m "feat(install): serialize installs via enqueueUniqueWork, handle new DownloadAndInstallStatus states"
```

---

### Task 9: `DownloadStateScreen` — render the new states

**Files:**
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/settings/downloadstate/DownloadStateScreen.kt`

**Interfaces:**
- Consumes: `DownloadAndInstallStatus` (Task 1).

- [ ] **Step 1: Extend the tap-to-retry condition**

Replace:

```kotlin
    OutlinedCard(
        onClick = {
            if (item.status is DownloadAndInstallStatus.Installing || item.status is DownloadAndInstallStatus.Error) {
                onInstall()
            }
        },
```

with:

```kotlin
    OutlinedCard(
        onClick = {
            if (
                item.status is DownloadAndInstallStatus.Installing ||
                item.status is DownloadAndInstallStatus.Error ||
                item.status is DownloadAndInstallStatus.PermissionRequired
            ) {
                onInstall()
            }
        },
```

- [ ] **Step 2: Add the three new `when` branches**

Replace:

```kotlin
        DownloadAndInstallStatus.Installing -> {
            ListItem(
                headlineContent = { Text("Installing") },
                supportingContent = { LinearWavyProgressIndicator() },
            )
        }
    }
}
```

with:

```kotlin
        DownloadAndInstallStatus.Installing -> {
            ListItem(
                headlineContent = { Text("Installing") },
                supportingContent = { LinearWavyProgressIndicator() },
            )
        }

        DownloadAndInstallStatus.PendingUserAction -> {
            ListItem(
                headlineContent = { Text("Waiting for confirmation") },
                supportingContent = { Text("Check the system install dialog") },
            )
        }

        DownloadAndInstallStatus.PermissionRequired -> {
            ListItem(
                headlineContent = { Text("Permission required") },
                supportingContent = { Text("Enable install from this source in Settings, then tap to retry") },
            )
        }

        DownloadAndInstallStatus.Cancelled -> {
            ListItem(
                headlineContent = { Text("Cancelled") },
            )
        }
    }
}
```

- [ ] **Step 3: Verify**

Run: `./gradlew :kmpuiviews:compileAndroidMain`
Expected: BUILD FAILED, but `DownloadStateScreen.kt` should now be error-free. The only remaining error should be in `PrereleaseScreen.kt` (fixed in Task 10).

- [ ] **Step 4: Commit**

```bash
git add kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/settings/downloadstate/DownloadStateScreen.kt
git commit -m "feat(install): render PendingUserAction, PermissionRequired, and Cancelled states in DownloadStateScreen"
```

---

### Task 10: `PrereleaseScreen` — render the new states

**Files:**
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/settings/prerelease/PrereleaseScreen.kt`

**Interfaces:**
- Consumes: `DownloadAndInstallStatus` (Task 1).

- [ ] **Step 1: Add the three new `when` branches**

Replace:

```kotlin
        DownloadAndInstallStatus.Installing -> {
            ListItem(
                headlineContent = { Text("Installing") },
                supportingContent = { LinearWavyProgressIndicator() },
            )
        }
    }
}
```

with:

```kotlin
        DownloadAndInstallStatus.Installing -> {
            ListItem(
                headlineContent = { Text("Installing") },
                supportingContent = { LinearWavyProgressIndicator() },
            )
        }

        DownloadAndInstallStatus.PendingUserAction -> {
            ListItem(
                headlineContent = { Text("Waiting for confirmation") },
                supportingContent = { Text("Check the system install dialog") },
            )
        }

        DownloadAndInstallStatus.PermissionRequired -> {
            ListItem(
                headlineContent = { Text("Permission required") },
                supportingContent = { Text("Enable install from this source in Settings, then tap Download to retry") },
            )
        }

        DownloadAndInstallStatus.Cancelled -> {
            ListItem(
                headlineContent = { Text("Cancelled") },
            )
        }
    }
}
```

(`PrereleaseScreen`'s retry is already unconditional — the download `IconButton` at line 110 always calls `viewModel.update(it.url)` regardless of status — so no click-condition change is needed here, unlike Task 9.)

- [ ] **Step 2: Verify**

Run: `./gradlew :kmpuiviews:compileAndroidMain`
Expected: BUILD SUCCESSFUL. This is the first point in the plan where the whole module compiles clean — every file touched by Tasks 1–10 is now consistent. If anything still fails, check it against the expected-error lists in the earlier tasks' verification steps to find which one was missed.

- [ ] **Step 3: Commit**

```bash
git add kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/settings/prerelease/PrereleaseScreen.kt
git commit -m "feat(install): render PendingUserAction, PermissionRequired, and Cancelled states in PrereleaseScreen"
```

---

### Task 11: ProGuard keep rule for `PackageInstallReceiver`

**Files:**
- Modify: `mangaworld/proguard-rules.pro`
- Modify: `animeworld/proguard-rules.pro`
- Modify: `novelworld/proguard-rules.pro`

**Interfaces:**
- Consumes: `com.programmersbox.kmpuiviews.receivers.PackageInstallReceiver` (Task 4).

Modern AGP auto-generates keep rules for manifest-declared components, but this repo has no existing rule covering this class and no evidence the auto-generated rule set is relied on elsewhere for receivers — add an explicit rule defensively, consistent with the design spec.

- [ ] **Step 1: Append to each of the three files**

Add to the end of `mangaworld/proguard-rules.pro`, `animeworld/proguard-rules.pro`, and `novelworld/proguard-rules.pro`:

```proguard

# PackageInstaller broadcast receiver — instantiated by the OS via manifest registration
-keep class com.programmersbox.kmpuiviews.receivers.PackageInstallReceiver { public <init>(); }
```

- [ ] **Step 2: Verify a minified build still assembles**

Run: `./gradlew :mangaworld:assembleNoFirebaseRelease`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add mangaworld/proguard-rules.pro animeworld/proguard-rules.pro novelworld/proguard-rules.pro
git commit -m "chore(install): keep PackageInstallReceiver from R8 stripping/renaming"
```

---

### Task 12: Manual verification pass

**Files:** none (verification only).

- [ ] **Step 1: Build and install the app**

Run: `./gradlew :mangaworld:installNoFirebaseDebug`

- [ ] **Step 2: Success path**

From the extensions screen, install a source that isn't currently installed. Confirm: download progress shows, system install confirmation dialog appears, accepting it results in an "Installed" status in the UI, and the temp APK file in the app's cache dir is gone afterward (`adb shell run-as <applicationId> ls cache` should not show the `.apk`).

- [ ] **Step 3: Failure path**

Attempt to install an APK signed differently than an already-installed package with the same package name (or use any other reliable way to trigger a conflict/incompatible failure in your test setup). Confirm the UI shows `Error` with a specific, readable message (not a generic string), and the temp file is cleaned up.

- [ ] **Step 4: Cancel mid-download**

Start an install, cancel it (the download-state screen's cancel button) while the progress bar is still in the "Downloading" phase. Confirm: no crash, the item disappears/stops, and the temp `.apk` file is not left behind in the cache dir.

- [ ] **Step 5: Cancel mid-install**

Start an install, and once the system confirmation dialog appears, dismiss/cancel it (tap "Cancel" in the system dialog, not the app's own cancel button). Confirm the status becomes `Cancelled` (not `Error`), no crash, and the temp file is cleaned up.

- [ ] **Step 6: Permission-required path**

On the device/emulator, go to Settings and revoke "Install unknown apps" for this app specifically (or use a fresh install where it's not yet granted). Attempt an install from the app. Confirm the UI shows the `PermissionRequired` guidance text. Go grant the permission in Settings, return to the app, tap the item again, and confirm the install proceeds normally this time.

- [ ] **Step 7: Backgrounded during prompt**

Start an install, and immediately press Home to background the app before the system confirmation dialog would normally appear. Confirm the system install confirmation dialog still appears on top (interrupting whatever is on screen), proving `PendingUserAction`'s `startActivity` call works regardless of app foreground state.

- [ ] **Step 8: Sequential queue**

Trigger two extension installs back-to-back quickly (tap install on two different sources in immediate succession). Confirm they do not show two simultaneous system confirmation dialogs — the second one's download/install only proceeds after the first fully finishes (success, failure, or cancel).

- [ ] **Step 9: Report results**

Note which of Steps 2–8 passed as expected and which didn't, with enough detail (device/emulator API level, exact repro steps, screenshot if useful) for a follow-up fix if something didn't behave as designed.
