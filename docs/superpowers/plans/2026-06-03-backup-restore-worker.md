# Backup/Restore Worker Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Move backup and restore operations off `viewModelScope` onto Android WorkManager Workers, showing foreground progress and completion/failure notifications.

**Architecture:** Two `CoroutineWorker` classes (`BackupWorker`, `RestoreWorker`) receive the file URI as `WorkData`, call the existing `Backup` class, and manage notifications. `BackgroundWorkHandler` gains two new methods (`startBackup`, `startRestore`) dispatched from `MoreSettingsViewModel`. No ViewModel state change — notifications only.

**Tech Stack:** WorkManager (`CoroutineWorker`), Koin (`workerOf`), `NotificationDslBuilder`, `ForegroundInfo`

---

## File Map

| File | Action |
|---|---|
| `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/repository/WorkRepository.kt` | Add `startBackup`/`startRestore` to `BackgroundWorkHandler` interface |
| `kmpuiviews/src/androidMain/kotlin/com/programmersbox/kmpuiviews/repository/BackgroundWorkHandlerImpl.kt` | Implement both methods |
| `kmpuiviews/src/jvmMain/kotlin/com/programmersbox/kmpuiviews/repository/BackgroundWorkHandlerImpl.kt` | Add no-op stubs |
| `kmpuiviews/src/androidMain/kotlin/com/programmersbox/kmpuiviews/utils/NotificationEnums.kt` | Add `Backup` channel entry |
| `kmpuiviews/src/androidMain/kotlin/com/programmersbox/kmpuiviews/workers/BackupWorker.kt` | Create new file |
| `kmpuiviews/src/androidMain/kotlin/com/programmersbox/kmpuiviews/workers/RestoreWorker.kt` | Create new file |
| `kmpuiviews/src/androidMain/kotlin/com/programmersbox/kmpuiviews/di/WorkerModule.kt` | Register both workers |
| `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/settings/moresettings/MoreSettingsViewModel.kt` | Replace coroutine bodies with handler calls |

---

## Task 1: Add `Backup` Notification Channel

**Files:**
- Modify: `kmpuiviews/src/androidMain/kotlin/com/programmersbox/kmpuiviews/utils/NotificationEnums.kt`

- [ ] **Step 1: Add the `Backup` entry to the `NotificationChannels` enum**

Current last entry is `Download`. Add `Backup` after it:

```kotlin
enum class NotificationChannels(
    val id: String,
    val importance: Int = NotificationManagerCompat.IMPORTANCE_DEFAULT,
) {
    Otaku("otakuChannel", NotificationManagerCompat.IMPORTANCE_HIGH),
    UpdateCheck("updateCheckChannel", NotificationManagerCompat.IMPORTANCE_MIN),
    AppUpdate("appUpdate", NotificationManagerCompat.IMPORTANCE_HIGH),
    SourceUpdate("sourceUpdate", NotificationManagerCompat.IMPORTANCE_DEFAULT),
    Download("download_channel", NotificationManagerCompat.IMPORTANCE_DEFAULT),
    Backup("backup_channel", NotificationManagerCompat.IMPORTANCE_LOW);

    companion object {
        fun setupNotificationChannels(context: Context) {
            val notificationManager = NotificationManagerCompat.from(context)
            entries.forEach {
                notificationManager.createNotificationChannel(
                    NotificationChannelCompat.Builder(it.id, it.importance)
                        .setName(it.id)
                        .build()
                )
            }
        }
    }
}
```

`IMPORTANCE_LOW` — silent, no heads-up. The `setupNotificationChannels` loop picks it up automatically; no other change needed.

- [ ] **Step 2: Verify build**

```bash
./gradlew :kmpuiviews:compileDebugKotlinAndroid
```

Expected: BUILD SUCCESSFUL, no errors.

- [ ] **Step 3: Commit**

```bash
git add kmpuiviews/src/androidMain/kotlin/com/programmersbox/kmpuiviews/utils/NotificationEnums.kt
git commit -m "feat(notifications): add Backup notification channel"
```

---

## Task 2: Extend `BackgroundWorkHandler` Interface

**Files:**
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/repository/WorkRepository.kt`
- Modify: `kmpuiviews/src/jvmMain/kotlin/com/programmersbox/kmpuiviews/repository/BackgroundWorkHandlerImpl.kt`

- [ ] **Step 1: Add methods to the `BackgroundWorkHandler` interface**

In `WorkRepository.kt`, add the two new methods at the bottom of the `BackgroundWorkHandler` interface. Add the `PlatformFile` import at the top of the file:

```kotlin
package com.programmersbox.kmpuiviews.repository

import com.programmersbox.kmpuiviews.presentation.settings.workerinfo.WorkerInfoModel
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDateTime

interface WorkRepository {
    val manualCheck: Flow<List<WorkInfoKmp>>
    val allWorkCheck: Flow<List<WorkInfoKmp>>
    fun pruneWork()
    fun checkManually()
}

data class WorkInfoKmp(
    val state: String,
    val source: String,
    val progress: Int?,
    val max: Int?,
    val nextScheduleTimeMillis: LocalDateTime,
)

interface BackgroundWorkHandler {
    fun localToCloudListener(): Flow<List<WorkInfoKmp>>
    fun cloudToLocalListener(): Flow<List<WorkInfoKmp>>
    fun syncLocalToCloud()
    fun syncCloudToLocal()
    fun setupPeriodicCheckers()
    fun workerInfoFlow(): Flow<List<WorkerInfoModel>>
    fun sourceUpdate()
    fun cancel(uuid: String)
    fun startBackup(file: PlatformFile)
    fun startRestore(file: PlatformFile)
}
```

- [ ] **Step 2: Add no-op stubs to the JVM implementation**

In `kmpuiviews/src/jvmMain/kotlin/com/programmersbox/kmpuiviews/repository/BackgroundWorkHandlerImpl.kt`, add two no-op overrides to `BackgroundWorkHandlerImpl`. Add the import at the top of the file:

```kotlin
import io.github.vinceglb.filekit.PlatformFile
```

Then inside the `BackgroundWorkHandlerImpl` class, after `override fun cancel(uuid: String) { }`:

```kotlin
override fun startBackup(file: PlatformFile) {}
override fun startRestore(file: PlatformFile) {}
```

- [ ] **Step 3: Verify build catches missing Android implementation**

```bash
./gradlew :kmpuiviews:compileDebugKotlinAndroid
```

Expected: BUILD FAILED — `BackgroundWorkHandlerImpl` (androidMain) does not implement `startBackup` and `startRestore`. This confirms the interface change propagated correctly.

- [ ] **Step 4: Commit**

```bash
git add kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/repository/WorkRepository.kt
git add kmpuiviews/src/jvmMain/kotlin/com/programmersbox/kmpuiviews/repository/BackgroundWorkHandlerImpl.kt
git commit -m "feat(backup): add startBackup/startRestore to BackgroundWorkHandler interface"
```

---

## Task 3: Implement Dispatch in Android `BackgroundWorkHandlerImpl`

**Files:**
- Modify: `kmpuiviews/src/androidMain/kotlin/com/programmersbox/kmpuiviews/repository/BackgroundWorkHandlerImpl.kt`

- [ ] **Step 1: Add imports**

Add these imports at the top of `BackgroundWorkHandlerImpl.kt` (androidMain):

```kotlin
import com.programmersbox.kmpuiviews.workers.BackupWorker
import com.programmersbox.kmpuiviews.workers.RestoreWorker
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.toAndroidUri
```

- [ ] **Step 2: Implement `startBackup` and `startRestore`**

Add these two methods to `BackgroundWorkHandlerImpl` after `override fun cancel(uuid: String)`:

```kotlin
override fun startBackup(file: PlatformFile) {
    workManager.enqueueUniqueWork(
        "backup",
        ExistingWorkPolicy.KEEP,
        OneTimeWorkRequestBuilder<BackupWorker>()
            .setInputData(workDataOf("uri" to file.toAndroidUri("").toString()))
            .build()
    )
}

override fun startRestore(file: PlatformFile) {
    workManager.enqueueUniqueWork(
        "restore",
        ExistingWorkPolicy.KEEP,
        OneTimeWorkRequestBuilder<RestoreWorker>()
            .setInputData(workDataOf("uri" to file.toAndroidUri("").toString()))
            .build()
    )
}
```

`ExistingWorkPolicy.KEEP` prevents a second backup from enqueuing while one is already running.

- [ ] **Step 3: Verify build**

```bash
./gradlew :kmpuiviews:compileDebugKotlinAndroid
```

Expected: BUILD SUCCESSFUL (Workers don't exist yet but the enqueue calls compile because `BackupWorker`/`RestoreWorker` will be created before the full build runs in later tasks — if it fails due to missing classes, that's fine; proceed to Task 4).

- [ ] **Step 4: Commit**

```bash
git add kmpuiviews/src/androidMain/kotlin/com/programmersbox/kmpuiviews/repository/BackgroundWorkHandlerImpl.kt
git commit -m "feat(backup): implement startBackup/startRestore in Android BackgroundWorkHandlerImpl"
```

---

## Task 4: Create `BackupWorker`

**Files:**
- Create: `kmpuiviews/src/androidMain/kotlin/com/programmersbox/kmpuiviews/workers/BackupWorker.kt`

- [ ] **Step 1: Create the file**

```kotlin
package com.programmersbox.kmpuiviews.workers

import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.content.getSystemService
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.programmersbox.kmpuiviews.readPlatformFile
import com.programmersbox.kmpuiviews.recordFirebaseException
import com.programmersbox.kmpuiviews.utils.Backup
import com.programmersbox.kmpuiviews.utils.NotificationChannels
import com.programmersbox.kmpuiviews.utils.NotificationDslBuilder
import com.programmersbox.kmpuiviews.utils.NotificationLogo

private const val BACKUP_NOTIFICATION_ID = 200

class BackupWorker(
    context: Context,
    workerParams: WorkerParameters,
    private val backup: Backup,
    private val logo: NotificationLogo,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        setForeground(getForegroundInfo())
        val uri = inputData.getString("uri") ?: return Result.failure()
        return runCatching {
            backup.createBackup(readPlatformFile(uri))
        }.fold(
            onSuccess = {
                postCompletionNotification("Backup complete", timeoutAfter = 3000L)
                Result.success()
            },
            onFailure = { e ->
                recordFirebaseException(e)
                postCompletionNotification("Backup failed", timeoutAfter = null)
                Result.failure()
            }
        )
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val notification = NotificationDslBuilder.builder(
            applicationContext,
            NotificationChannels.Backup.id,
            logo.notificationId,
        ) {
            title = "Backing up…"
            onlyAlertOnce = true
            ongoing = true
            progress {
                max = 0
                progress = 0
                indeterminate = true
            }
        }
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(BACKUP_NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(BACKUP_NOTIFICATION_ID, notification)
        }
    }

    private fun postCompletionNotification(title: String, timeoutAfter: Long?) {
        val notification = NotificationDslBuilder.builder(
            applicationContext,
            NotificationChannels.Backup.id,
            logo.notificationId,
        ) {
            this.title = title
            if (timeoutAfter != null) this.timeoutAfter = timeoutAfter
        }
        applicationContext.getSystemService<NotificationManager>()
            ?.notify(BACKUP_NOTIFICATION_ID, notification)
    }
}
```

The `SystemForegroundService` in `kmpuiviews/src/androidMain/AndroidManifest.xml` already declares `android:foregroundServiceType="dataSync"` — no manifest change needed.

- [ ] **Step 2: Verify build**

```bash
./gradlew :kmpuiviews:compileDebugKotlinAndroid
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add kmpuiviews/src/androidMain/kotlin/com/programmersbox/kmpuiviews/workers/BackupWorker.kt
git commit -m "feat(backup): add BackupWorker with foreground notification"
```

---

## Task 5: Create `RestoreWorker`

**Files:**
- Create: `kmpuiviews/src/androidMain/kotlin/com/programmersbox/kmpuiviews/workers/RestoreWorker.kt`

- [ ] **Step 1: Create the file**

```kotlin
package com.programmersbox.kmpuiviews.workers

import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.content.getSystemService
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.programmersbox.kmpuiviews.readPlatformFile
import com.programmersbox.kmpuiviews.recordFirebaseException
import com.programmersbox.kmpuiviews.utils.Backup
import com.programmersbox.kmpuiviews.utils.NotificationChannels
import com.programmersbox.kmpuiviews.utils.NotificationDslBuilder
import com.programmersbox.kmpuiviews.utils.NotificationLogo

private const val RESTORE_NOTIFICATION_ID = 201

class RestoreWorker(
    context: Context,
    workerParams: WorkerParameters,
    private val backup: Backup,
    private val logo: NotificationLogo,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        setForeground(getForegroundInfo())
        val uri = inputData.getString("uri") ?: return Result.failure()
        return runCatching {
            backup.restoreBackup(readPlatformFile(uri))
        }.fold(
            onSuccess = {
                postCompletionNotification("Restore complete", timeoutAfter = 3000L)
                Result.success()
            },
            onFailure = { e ->
                recordFirebaseException(e)
                postCompletionNotification("Restore failed", timeoutAfter = null)
                Result.failure()
            }
        )
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val notification = NotificationDslBuilder.builder(
            applicationContext,
            NotificationChannels.Backup.id,
            logo.notificationId,
        ) {
            title = "Restoring…"
            onlyAlertOnce = true
            ongoing = true
            progress {
                max = 0
                progress = 0
                indeterminate = true
            }
        }
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(RESTORE_NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(RESTORE_NOTIFICATION_ID, notification)
        }
    }

    private fun postCompletionNotification(title: String, timeoutAfter: Long?) {
        val notification = NotificationDslBuilder.builder(
            applicationContext,
            NotificationChannels.Backup.id,
            logo.notificationId,
        ) {
            this.title = title
            if (timeoutAfter != null) this.timeoutAfter = timeoutAfter
        }
        applicationContext.getSystemService<NotificationManager>()
            ?.notify(RESTORE_NOTIFICATION_ID, notification)
    }
}
```

- [ ] **Step 2: Verify build**

```bash
./gradlew :kmpuiviews:compileDebugKotlinAndroid
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add kmpuiviews/src/androidMain/kotlin/com/programmersbox/kmpuiviews/workers/RestoreWorker.kt
git commit -m "feat(backup): add RestoreWorker with foreground notification"
```

---

## Task 6: Register Workers in Koin

**Files:**
- Modify: `kmpuiviews/src/androidMain/kotlin/com/programmersbox/kmpuiviews/di/WorkerModule.kt`

- [ ] **Step 1: Add imports and register both workers**

Add the imports and two `workerOf` calls to `Module.kmpWorkers()`:

```kotlin
package com.programmersbox.kmpuiviews.di

import com.programmersbox.kmpuiviews.workers.AppCheckWorker
import com.programmersbox.kmpuiviews.workers.AppCleanupWorker
import com.programmersbox.kmpuiviews.workers.BackupWorker
import com.programmersbox.kmpuiviews.workers.CloudToLocalSyncWorker
import com.programmersbox.kmpuiviews.workers.DownloadAndInstallWorker
import com.programmersbox.kmpuiviews.workers.DownloadWorker
import com.programmersbox.kmpuiviews.workers.InstallWorker
import com.programmersbox.kmpuiviews.workers.LocalToCloudSyncWorker
import com.programmersbox.kmpuiviews.workers.NotifySingleWorker
import com.programmersbox.kmpuiviews.workers.RestoreWorker
import com.programmersbox.kmpuiviews.workers.SourceUpdateChecker
import com.programmersbox.kmpuiviews.workers.UpdateFlowWorker
import org.koin.androidx.workmanager.dsl.workerOf
import org.koin.core.module.Module

fun Module.kmpWorkers() {
    workerOf(::AppCleanupWorker)
    workerOf(::NotifySingleWorker)
    workerOf(::LocalToCloudSyncWorker)
    workerOf(::CloudToLocalSyncWorker)
    workerOf(::UpdateFlowWorker)
    workerOf(::SourceUpdateChecker)
    workerOf(::AppCheckWorker)
    workerOf(::DownloadAndInstallWorker)
    workerOf(::DownloadWorker)
    workerOf(::InstallWorker)
    workerOf(::BackupWorker)
    workerOf(::RestoreWorker)
}
```

- [ ] **Step 2: Verify build**

```bash
./gradlew :kmpuiviews:compileDebugKotlinAndroid
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add kmpuiviews/src/androidMain/kotlin/com/programmersbox/kmpuiviews/di/WorkerModule.kt
git commit -m "feat(backup): register BackupWorker and RestoreWorker in Koin"
```

---

## Task 7: Simplify `MoreSettingsViewModel`

**Files:**
- Modify: `kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/settings/moresettings/MoreSettingsViewModel.kt`

- [ ] **Step 1: Replace `exportFullBackup` and `importFullBackup`**

Remove the `viewModelScope.launch` bodies and replace with single-line handler calls. Remove unused imports afterwards (`kotlinx.coroutines.launch` may still be used by other methods — check before removing).

Replace these two functions:

```kotlin
fun exportFullBackup(document: PlatformFile) {
    backgroundWorkHandler.startBackup(document)
}

fun importFullBackup(document: PlatformFile) {
    backgroundWorkHandler.startRestore(document)
}
```

Also remove the `//TODO: Kick off in worker` comment from above `importFullBackup`.

- [ ] **Step 2: Verify `viewModelScope` import is still needed**

`viewModelScope` is still used in `importFavorites`, `writeToFile`, `writeListsToFile`. Keep `kotlinx.coroutines.launch` import. If `kotlinx.coroutines.withContext` and `kotlinx.coroutines.Dispatchers` are only used inside those methods, keep them too.

- [ ] **Step 3: Full app build verification**

```bash
./gradlew :mangaworld:assembleNoFirebaseDebug
```

Expected: BUILD SUCCESSFUL. This compiles the full app with all modules.

- [ ] **Step 4: Commit**

```bash
git add kmpuiviews/src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/settings/moresettings/MoreSettingsViewModel.kt
git commit -m "feat(backup): kick off backup/restore in Workers via BackgroundWorkHandler"
```

---

## Manual Verification Checklist

After the full build succeeds, verify on a device or emulator:

- [ ] Trigger **Export Full Backup** from Settings — notification tray shows "Backing up…" with indeterminate spinner
- [ ] After backup completes — notification replaces with "Backup complete" and auto-dismisses after 3 s
- [ ] Trigger **Import Full Backup** — notification tray shows "Restoring…"
- [ ] After restore completes — "Restore complete" appears and auto-dismisses
- [ ] Trigger backup then immediately trigger it again — second request is silently ignored (`KEEP` policy)
- [ ] Force-kill the app mid-backup — Worker continues running in the background (foreground service keeps process alive)
