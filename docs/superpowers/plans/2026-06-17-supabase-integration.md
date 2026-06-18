# Supabase Integration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (
> recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use
> checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the Firebase Firestore sync in `sharedutils` with a KMP `supabase-integration`
module that provides credential management, auth, offline-first two-way sync, backup/restore, and
Compose UI screens for Android, JVM Desktop, and iOS.

**Architecture:** Room remains the local source of truth; sync metadata columns (`supabase_id`,
`created_at`, `updated_at`, `is_deleted`, `is_dirty`) are added to every entity via Room migrations.
A `SyncEngine` pushes dirty records to Supabase PostgREST and pulls remote changes; a `SyncManager`
orchestrates Realtime WebSocket subscriptions with 5-minute polling fallback when the socket drops.

**Tech Stack:** Supabase KMP SDK 3.1.4 (postgrest-kt, gotrue-kt, realtime-kt, storage-kt), Ktor
3.3.3 (OkHttp/CIO/Darwin engines), KMP Room 3.0.0-alpha06, Koin 4.2.2, Compose Multiplatform,
Navigation3, `dev.jordond.connectivity` for network state.

## Global Constraints

- Module path `:favoritesdatabase:supabase-integration`, directory
  `favoritesdatabase/supabase-integration/`, package `com.programmersbox.supabaseintegration`
- Use `otaku-multiplatform` convention plugin (Android + JVM + iOS targets via
  `applyDefaultHierarchyTemplate`)
- All version catalog additions go in `gradle/common.versions.toml` (common),
  `gradle/ios.versions.toml` (iOS Ktor engine), `gradle/android.versions.toml` (security-crypto)
- No hardcoded Supabase URLs or keys anywhere — runtime only
- Room package prefix is `androidx.room3` (KMP Room alpha)
- Supabase SDK version: `3.1.4`; Ktor version: `3.3.3` (already in `commonLibs.ktorVersion`)
- `ItemDatabase/7.json` and `ListDatabase/12.json` exist as untracked schema files — check them
  before writing migrations; delete if they don't match this plan's columns
- Firebase removal happens only after full sync verified working — do not delete Firebase code
  during implementation
- Navigation3 route objects must be `@Serializable` data objects

---

### Task 1: Gradle Module Setup

**Files:**

- Create: `favoritesdatabase/supabase-integration/build.gradle.kts`
- Modify: `gradle/common.versions.toml` (add supabase + ktor-cio entries)
- Modify: `gradle/ios.versions.toml` (add ktor-darwin)
- Modify: `settings.gradle.kts` (add include)

**Interfaces:**

- Consumes: nothing
- Produces: compilable KMP module skeleton; `commonLibs.supabase.*`, `commonLibs.ktorCio`,
  `iosLibs.ktorDarwin` catalog entries

- [ ] **Step 1: Add Supabase + Ktor-CIO to `gradle/common.versions.toml`**

  In the `[versions]` section add:
  ```toml
  supabase = "3.1.4"
  ```
  In the `[libraries]` section add:
  ```toml
  supabase-bom = { module = "io.github.jan-tennert.supabase:bom", version.ref = "supabase" }
  supabase-postgrest = { module = "io.github.jan-tennert.supabase:postgrest-kt" }
  supabase-gotrue = { module = "io.github.jan-tennert.supabase:gotrue-kt" }
  supabase-realtime = { module = "io.github.jan-tennert.supabase:realtime-kt" }
  supabase-storage = { module = "io.github.jan-tennert.supabase:storage-kt" }
  supabase-composeAuth = { module = "io.github.jan-tennert.supabase:compose-auth" }
  ktorCio = { module = "io.ktor:ktor-client-cio", version.ref = "ktorVersion" }
  ```

- [ ] **Step 2: Add Ktor Darwin engine to `gradle/ios.versions.toml`**

  In the `[libraries]` section add:
  ```toml
  ktorDarwin = { module = "io.ktor:ktor-client-darwin", version = "3.3.3" }
  ```

- [ ] **Step 3: Create `favoritesdatabase/supabase-integration/build.gradle.kts`**

  ```kotlin
  plugins {
      `otaku-multiplatform`
      id("kotlinx-serialization")
  }

  otakuDependencies {
      androidPackageName = "com.programmersbox.supabaseintegration"
  }

  kotlin {
      sourceSets {
          commonMain.dependencies {
              implementation(project.dependencies.platform(commonLibs.supabase.bom))
              implementation(commonLibs.supabase.postgrest)
              implementation(commonLibs.supabase.gotrue)
              implementation(commonLibs.supabase.realtime)
              implementation(commonLibs.supabase.storage)
              implementation(commonLibs.supabase.composeAuth)
              implementation(project.dependencies.platform(commonLibs.koin.bom))
              implementation(commonLibs.koinCores)
              implementation(commonLibs.koinComposeKmp)
              implementation(commonLibs.koinViewModel)
              implementation(commonLibs.coroutinesCore)
              implementation(commonLibs.kotlinxSerialization)
              implementation(commonLibs.kotlinx.datetime)
              implementation(commonLibs.connectivity.core)
              implementation(commonLibs.connectivity.device)
              implementation(commonLibs.cmp.navigation3.ui)
              implementation(commonLibs.lifecycle.viewmodel.compose)
              implementation(projects.favoritesdatabase)
              implementation(projects.kmpmodels)
          }
          androidMain.dependencies {
              implementation(commonLibs.ktorOkHttp)
              implementation(androidLibs.workRuntime)
              implementation(androidLibs.koin.workmanager)
              implementation("androidx.security:security-crypto:1.1.0-alpha06")
          }
          jvmMain.dependencies {
              implementation(commonLibs.ktorCio)
          }
          iosMain.dependencies {
              implementation(iosLibs.ktorDarwin)
          }
      }
  }
  ```

- [ ] **Step 4: Add module to `settings.gradle.kts`**

  Inside the `include(...)` block, add:
  ```kotlin
  include(":favoritesdatabase:supabase-integration")
  ```

- [ ] **Step 5: Verify Gradle sync**

  Run: `./gradlew :favoritesdatabase:supabase-integration:assemble`

  Expected: BUILD SUCCESSFUL (empty module, nothing to compile yet)

- [ ] **Step 6: Commit**

  ```bash
  git add favoritesdatabase/supabase-integration/build.gradle.kts gradle/common.versions.toml gradle/ios.versions.toml settings.gradle.kts
  git commit -m "build: add supabase-integration KMP module scaffold"
  ```

---

### Task 2: SupabaseCredentials + CredentialManager

**Files:**

- Create:
  `favoritesdatabase/supabase-integration/src/commonMain/kotlin/com/programmersbox/supabaseintegration/credentials/SupabaseCredentials.kt`
- Create:
  `favoritesdatabase/supabase-integration/src/commonMain/kotlin/com/programmersbox/supabaseintegration/credentials/CredentialManager.kt`
- Create:
  `favoritesdatabase/supabase-integration/src/androidMain/kotlin/com/programmersbox/supabaseintegration/credentials/CredentialManager.android.kt`
- Create:
  `favoritesdatabase/supabase-integration/src/jvmMain/kotlin/com/programmersbox/supabaseintegration/credentials/CredentialManager.jvm.kt`
- Create:
  `favoritesdatabase/supabase-integration/src/iosMain/kotlin/com/programmersbox/supabaseintegration/credentials/CredentialManager.ios.kt`

**Interfaces:**

- Consumes: nothing
- Produces: `CredentialManager` interface;
  `SupabaseCredentials(projectUrl: String, anonKey: String)`;
  `expect fun createCredentialManager(context: Any?): CredentialManager`

- [ ] **Step 1: Create `SupabaseCredentials.kt`**

  ```kotlin
  package com.programmersbox.supabaseintegration.credentials

  import kotlinx.serialization.Serializable

  @Serializable
  data class SupabaseCredentials(
      val projectUrl: String,
      val anonKey: String,
  )
  ```

- [ ] **Step 2: Create `CredentialManager.kt`**

  ```kotlin
  package com.programmersbox.supabaseintegration.credentials

  import kotlinx.coroutines.flow.Flow

  interface CredentialManager {
      fun hasCredentials(): Flow<Boolean>
      suspend fun saveCredentials(credentials: SupabaseCredentials)
      fun getCredentials(): SupabaseCredentials?
      suspend fun clearCredentials()
  }

  expect fun createCredentialManager(context: Any?): CredentialManager
  ```

- [ ] **Step 3: Create `CredentialManager.android.kt`**

  ```kotlin
  package com.programmersbox.supabaseintegration.credentials

  import android.content.Context
  import androidx.security.crypto.EncryptedSharedPreferences
  import androidx.security.crypto.MasterKey
  import kotlinx.coroutines.flow.Flow
  import kotlinx.coroutines.flow.MutableStateFlow
  import kotlinx.serialization.json.Json

  class AndroidCredentialManager(context: Context) : CredentialManager {
      private val masterKey = MasterKey.Builder(context)
          .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
          .build()
      private val prefs = EncryptedSharedPreferences.create(
          context,
          "supabase_credentials",
          masterKey,
          EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
          EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
      )
      private val _hasCredentials = MutableStateFlow(prefs.contains(KEY_CREDENTIALS))

      override fun hasCredentials(): Flow<Boolean> = _hasCredentials

      override suspend fun saveCredentials(credentials: SupabaseCredentials) {
          prefs.edit()
              .putString(KEY_CREDENTIALS, Json.encodeToString(SupabaseCredentials.serializer(), credentials))
              .apply()
          _hasCredentials.value = true
      }

      override fun getCredentials(): SupabaseCredentials? {
          val json = prefs.getString(KEY_CREDENTIALS, null) ?: return null
          return runCatching { Json.decodeFromString(SupabaseCredentials.serializer(), json) }.getOrNull()
      }

      override suspend fun clearCredentials() {
          prefs.edit().remove(KEY_CREDENTIALS).apply()
          _hasCredentials.value = false
      }

      companion object { private const val KEY_CREDENTIALS = "credentials_json" }
  }

  actual fun createCredentialManager(context: Any?): CredentialManager =
      AndroidCredentialManager(context as Context)
  ```

- [ ] **Step 4: Create `CredentialManager.jvm.kt`**

  ```kotlin
  package com.programmersbox.supabaseintegration.credentials

  import kotlinx.coroutines.flow.Flow
  import kotlinx.coroutines.flow.MutableStateFlow
  import kotlinx.serialization.json.Json
  import java.io.File
  import javax.crypto.Cipher
  import javax.crypto.KeyGenerator
  import javax.crypto.SecretKey
  import javax.crypto.spec.SecretKeySpec

  class JvmCredentialManager : CredentialManager {
      private val configDir = File(System.getProperty("user.home"), ".otakuworld")
      private val credFile = File(configDir, "supabase.enc")
      private val keyFile = File(configDir, "supabase.key")
      private val _hasCredentials = MutableStateFlow(credFile.exists())

      private fun getOrCreateKey(): SecretKey {
          if (!keyFile.exists()) {
              configDir.mkdirs()
              val kg = KeyGenerator.getInstance("AES").apply { init(256) }
              keyFile.writeBytes(kg.generateKey().encoded)
          }
          return SecretKeySpec(keyFile.readBytes(), "AES")
      }

      override fun hasCredentials(): Flow<Boolean> = _hasCredentials

      override suspend fun saveCredentials(credentials: SupabaseCredentials) {
          configDir.mkdirs()
          val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding").apply {
              init(Cipher.ENCRYPT_MODE, getOrCreateKey())
          }
          val json = Json.encodeToString(SupabaseCredentials.serializer(), credentials)
          credFile.writeBytes(cipher.doFinal(json.toByteArray()))
          _hasCredentials.value = true
      }

      override fun getCredentials(): SupabaseCredentials? {
          if (!credFile.exists()) return null
          return runCatching {
              val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding").apply {
                  init(Cipher.DECRYPT_MODE, getOrCreateKey())
              }
              Json.decodeFromString(SupabaseCredentials.serializer(), String(cipher.doFinal(credFile.readBytes())))
          }.getOrNull()
      }

      override suspend fun clearCredentials() {
          credFile.delete()
          _hasCredentials.value = false
      }
  }

  actual fun createCredentialManager(context: Any?): CredentialManager = JvmCredentialManager()
  ```

- [ ] **Step 5: Create `CredentialManager.ios.kt`**

  ```kotlin
  package com.programmersbox.supabaseintegration.credentials

  import kotlinx.coroutines.flow.Flow
  import kotlinx.coroutines.flow.MutableStateFlow
  import kotlinx.serialization.json.Json
  import platform.Foundation.NSUserDefaults

  // Production upgrade: replace NSUserDefaults with Security.framework kSecClass/SecItemAdd calls
  class IosCredentialManager : CredentialManager {
      private val defaults = NSUserDefaults.standardUserDefaults
      private val _hasCredentials = MutableStateFlow(defaults.stringForKey(KEY) != null)

      override fun hasCredentials(): Flow<Boolean> = _hasCredentials

      override suspend fun saveCredentials(credentials: SupabaseCredentials) {
          defaults.setObject(Json.encodeToString(SupabaseCredentials.serializer(), credentials), KEY)
          _hasCredentials.value = true
      }

      override fun getCredentials(): SupabaseCredentials? {
          val json = defaults.stringForKey(KEY) ?: return null
          return runCatching { Json.decodeFromString(SupabaseCredentials.serializer(), json) }.getOrNull()
      }

      override suspend fun clearCredentials() {
          defaults.removeObjectForKey(KEY)
          _hasCredentials.value = false
      }

      companion object { private const val KEY = "supabase_credentials_json" }
  }

  actual fun createCredentialManager(context: Any?): CredentialManager = IosCredentialManager()
  ```

- [ ] **Step 6: Build to verify expect/actual linkage**

  Run: `./gradlew :favoritesdatabase:supabase-integration:compileKotlinAndroid`

  Expected: BUILD SUCCESSFUL

- [ ] **Step 7: Commit**

  ```bash
  git add favoritesdatabase/supabase-integration/src/
  git commit -m "feat(supabase): add CredentialManager with platform-secure storage"
  ```

---

### Task 3: SupabaseClientProvider

**Files:**

- Create:
  `favoritesdatabase/supabase-integration/src/commonMain/kotlin/com/programmersbox/supabaseintegration/client/SupabaseClientProvider.kt`

**Interfaces:**

- Consumes: `CredentialManager` (Task 2)
- Produces: `SupabaseClientProvider` — `getOrCreate(): SupabaseClient?`,
  `recreate(): SupabaseClient?`, `clientState: StateFlow<SupabaseClient?>`

- [ ] **Step 1: Create `SupabaseClientProvider.kt`**

  ```kotlin
  package com.programmersbox.supabaseintegration.client

  import com.programmersbox.supabaseintegration.credentials.CredentialManager
  import com.programmersbox.supabaseintegration.credentials.SupabaseCredentials
  import io.github.jan.supabase.SupabaseClient
  import io.github.jan.supabase.createSupabaseClient
  import io.github.jan.supabase.gotrue.GoTrue
  import io.github.jan.supabase.postgrest.Postgrest
  import io.github.jan.supabase.realtime.Realtime
  import io.github.jan.supabase.storage.Storage
  import kotlinx.coroutines.CoroutineScope
  import kotlinx.coroutines.Dispatchers
  import kotlinx.coroutines.flow.SharingStarted
  import kotlinx.coroutines.flow.StateFlow
  import kotlinx.coroutines.flow.map
  import kotlinx.coroutines.flow.stateIn

  class SupabaseClientProvider(private val credentialManager: CredentialManager) {
      private val scope = CoroutineScope(Dispatchers.Default)
      private var _client: SupabaseClient? = null

      val clientState: StateFlow<SupabaseClient?> = credentialManager.hasCredentials()
          .map { hasCredentials -> if (hasCredentials) getOrCreate() else null }
          .stateIn(scope, SharingStarted.Eagerly, null)

      fun getOrCreate(): SupabaseClient? {
          val credentials = credentialManager.getCredentials() ?: return null
          if (_client == null) _client = buildClient(credentials)
          return _client
      }

      suspend fun recreate(): SupabaseClient? {
          _client?.close()
          _client = null
          return getOrCreate()
      }

      suspend fun close() {
          _client?.close()
          _client = null
      }

      private fun buildClient(credentials: SupabaseCredentials): SupabaseClient =
          createSupabaseClient(
              supabaseUrl = credentials.projectUrl,
              supabaseKey = credentials.anonKey
          ) {
              install(GoTrue)
              install(Postgrest)
              install(Realtime)
              install(Storage)
          }
  }
  ```

- [ ] **Step 2: Commit**

  ```bash
  git add favoritesdatabase/supabase-integration/src/commonMain/kotlin/com/programmersbox/supabaseintegration/client/
  git commit -m "feat(supabase): add lazy SupabaseClientProvider"
  ```

---

### Task 4: AuthState + AuthManager + AuthManagerImpl

**Files:**

- Create:
  `favoritesdatabase/supabase-integration/src/commonMain/kotlin/com/programmersbox/supabaseintegration/auth/AuthState.kt`
- Create:
  `favoritesdatabase/supabase-integration/src/commonMain/kotlin/com/programmersbox/supabaseintegration/auth/SupabaseUser.kt`
- Create:
  `favoritesdatabase/supabase-integration/src/commonMain/kotlin/com/programmersbox/supabaseintegration/auth/AuthManager.kt`
- Create:
  `favoritesdatabase/supabase-integration/src/commonMain/kotlin/com/programmersbox/supabaseintegration/auth/AuthManagerImpl.kt`

**Interfaces:**

- Consumes: `SupabaseClientProvider` (Task 3)
- Produces: `AuthManager.authState: StateFlow<AuthState>`, `AuthState` sealed class,
  `SupabaseUser(id, email, phone, displayName)`

- [ ] **Step 1: Create `AuthState.kt` and `SupabaseUser.kt`**

  `AuthState.kt`:
  ```kotlin
  package com.programmersbox.supabaseintegration.auth

  sealed class AuthState {
      object Unauthenticated : AuthState()
      object Loading : AuthState()
      data class Authenticated(val user: SupabaseUser) : AuthState()
      data class Error(val message: String) : AuthState()
  }
  ```

  `SupabaseUser.kt`:
  ```kotlin
  package com.programmersbox.supabaseintegration.auth

  data class SupabaseUser(
      val id: String,
      val email: String?,
      val phone: String?,
      val displayName: String?,
  )
  ```

- [ ] **Step 2: Create `AuthManager.kt`**

  ```kotlin
  package com.programmersbox.supabaseintegration.auth

  import io.github.jan.supabase.gotrue.providers.OAuthProvider
  import kotlinx.coroutines.flow.StateFlow

  interface AuthManager {
      val authState: StateFlow<AuthState>
      suspend fun signInWithEmail(email: String, password: String)
      suspend fun signUpWithEmail(email: String, password: String)
      suspend fun signInWithOAuth(provider: OAuthProvider)
      suspend fun signInWithMagicLink(email: String)
      suspend fun signInWithPhone(phone: String, otp: String)
      suspend fun signInAnonymously()
      suspend fun signOut()
      suspend fun deleteAccount()
      suspend fun refreshSession()
  }
  ```

- [ ] **Step 3: Create `AuthManagerImpl.kt`**

  ```kotlin
  package com.programmersbox.supabaseintegration.auth

  import com.programmersbox.supabaseintegration.client.SupabaseClientProvider
  import io.github.jan.supabase.gotrue.SessionStatus
  import io.github.jan.supabase.gotrue.gotrue
  import io.github.jan.supabase.gotrue.providers.OAuthProvider
  import io.github.jan.supabase.gotrue.providers.builtin.Email
  import io.github.jan.supabase.gotrue.providers.builtin.OTP
  import io.github.jan.supabase.gotrue.providers.builtin.Phone
  import kotlinx.coroutines.CoroutineScope
  import kotlinx.coroutines.Dispatchers
  import kotlinx.coroutines.flow.MutableStateFlow
  import kotlinx.coroutines.flow.StateFlow
  import kotlinx.coroutines.flow.asStateFlow
  import kotlinx.coroutines.launch

  class AuthManagerImpl(private val clientProvider: SupabaseClientProvider) : AuthManager {
      private val scope = CoroutineScope(Dispatchers.Default)
      private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
      override val authState: StateFlow<AuthState> = _authState.asStateFlow()

      init {
          scope.launch {
              clientProvider.clientState.collect { client ->
                  if (client == null) { _authState.value = AuthState.Unauthenticated; return@collect }
                  client.gotrue.sessionStatus.collect { status ->
                      _authState.value = when (status) {
                          is SessionStatus.Authenticated -> {
                              val user = status.session.user
                              AuthState.Authenticated(SupabaseUser(
                                  id = user?.id ?: "",
                                  email = user?.email,
                                  phone = user?.phone,
                                  displayName = user?.userMetadata?.get("full_name")?.toString()
                              ))
                          }
                          is SessionStatus.NotAuthenticated -> AuthState.Unauthenticated
                          is SessionStatus.LoadingFromStorage -> AuthState.Loading
                          is SessionStatus.NetworkError -> AuthState.Error(status.cause.message ?: "Network error")
                      }
                  }
              }
          }
      }

      private val gotrue get() = clientProvider.getOrCreate()?.gotrue
          ?: error("Supabase client not initialized — save credentials first")

      override suspend fun signInWithEmail(email: String, password: String) {
          _authState.value = AuthState.Loading
          runCatching { gotrue.signInWith(Email) { this.email = email; this.password = password } }
              .onFailure { _authState.value = AuthState.Error(it.message ?: "Sign in failed") }
      }

      override suspend fun signUpWithEmail(email: String, password: String) {
          _authState.value = AuthState.Loading
          runCatching { gotrue.signUpWith(Email) { this.email = email; this.password = password } }
              .onFailure { _authState.value = AuthState.Error(it.message ?: "Sign up failed") }
      }

      override suspend fun signInWithOAuth(provider: OAuthProvider) {
          _authState.value = AuthState.Loading
          runCatching { gotrue.signInWith(provider) }
              .onFailure { _authState.value = AuthState.Error(it.message ?: "OAuth failed") }
      }

      override suspend fun signInWithMagicLink(email: String) {
          runCatching { gotrue.signInWith(OTP) { this.email = email } }
              .onFailure { _authState.value = AuthState.Error(it.message ?: "Magic link failed") }
      }

      override suspend fun signInWithPhone(phone: String, otp: String) {
          runCatching { gotrue.signInWith(Phone) { this.phone = phone; this.token = otp } }
              .onFailure { _authState.value = AuthState.Error(it.message ?: "Phone auth failed") }
      }

      override suspend fun signInAnonymously() {
          runCatching { gotrue.signInAnonymously() }
              .onFailure { _authState.value = AuthState.Error(it.message ?: "Anonymous auth failed") }
      }

      override suspend fun signOut() { runCatching { gotrue.signOut() } }
      override suspend fun deleteAccount() { runCatching { gotrue.deleteCurrentUser() } }
      override suspend fun refreshSession() { runCatching { gotrue.refreshCurrentSession() } }
  }
  ```

- [ ] **Step 4: Commit**

  ```bash
  git add favoritesdatabase/supabase-integration/src/commonMain/kotlin/com/programmersbox/supabaseintegration/auth/
  git commit -m "feat(supabase): add AuthManager with full GoTrue support"
  ```

---

### Task 5: Sync Metadata Types

**Files:**

- Create:
  `favoritesdatabase/supabase-integration/src/commonMain/kotlin/com/programmersbox/supabaseintegration/sync/SyncableEntity.kt`
- Create:
  `favoritesdatabase/supabase-integration/src/commonMain/kotlin/com/programmersbox/supabaseintegration/sync/SyncState.kt`
- Create:
  `favoritesdatabase/supabase-integration/src/commonMain/kotlin/com/programmersbox/supabaseintegration/sync/SyncConfig.kt`

**Interfaces:**

- Consumes: nothing
- Produces: `SyncableEntity` interface; `SyncState` sealed class;
  `SyncConfig(pollIntervalMs, maxRetries, initialBackoffMs, maxBackoffMs)`

- [ ] **Step 1: Create the three files**

  `SyncableEntity.kt`:
  ```kotlin
  package com.programmersbox.supabaseintegration.sync

  interface SyncableEntity {
      val supabaseId: String?
      val createdAt: Long
      val updatedAt: Long
      val isDeleted: Boolean
      val isDirty: Boolean
  }
  ```

  `SyncState.kt`:
  ```kotlin
  package com.programmersbox.supabaseintegration.sync

  sealed class SyncState {
      object Idle : SyncState()
      data class Syncing(val entity: String? = null) : SyncState()
      data class Error(val message: String) : SyncState()
      object Offline : SyncState()
  }
  ```

  `SyncConfig.kt`:
  ```kotlin
  package com.programmersbox.supabaseintegration.sync

  data class SyncConfig(
      val pollIntervalMs: Long = 5 * 60 * 1000L,
      val maxRetries: Int = 5,
      val initialBackoffMs: Long = 1_000L,
      val maxBackoffMs: Long = 30_000L,
  )
  ```

- [ ] **Step 2: Commit**

  ```bash
  git add favoritesdatabase/supabase-integration/src/commonMain/kotlin/com/programmersbox/supabaseintegration/sync/
  git commit -m "feat(supabase): add sync metadata types"
  ```

---

### Task 6: Room Migration — ItemDatabase (v6 → v7)

**Files:**

- Modify:
  `favoritesdatabase/src/commonMain/kotlin/com/programmersbox/favoritesdatabase/ItemModels.kt`
- Modify:
  `favoritesdatabase/src/commonMain/kotlin/com/programmersbox/favoritesdatabase/ItemDatabase.kt`

**Interfaces:**

- Consumes: existing `DbModel`, `ChapterWatched`, `NotificationItem`, `SourceOrder`,
  `IncognitoSource` entities
- Produces: all 5 entities gain `supabaseId`, `createdAt`, `updatedAt`, `isDeleted`, `isDirty`
  fields; `ItemDatabase` at version 7

- [ ] **Step 1: Check untracked schema file**

  Run:
  `cat favoritesdatabase/schemas/com.programmersbox.favoritesdatabase.ItemDatabase/7.json | python3 -c "import sys,json; d=json.load(sys.stdin); [print(e['tableName'], [c['columnName'] for c in e['fields']]) for e in d['database']['entities']]" 2>/dev/null || echo "NOT_FOUND"`

  If output shows `supabase_id`, `created_at`, `updated_at`, `is_deleted`, `is_dirty` columns — the
  schema already matches; skip to Step 3.
  If output shows different columns or NOT_FOUND — continue with Step 2.

- [ ] **Step 2: Delete conflicting schema if needed**

  Only run this if Step 1 revealed columns that differ from the 5 sync columns above:
  ```bash
  rm favoritesdatabase/schemas/com.programmersbox.favoritesdatabase.ItemDatabase/7.json
  ```

- [ ] **Step 3: Add sync fields to `DbModel` in `ItemModels.kt`**

  After `val shouldCheckForUpdate: Boolean = true,` add:
  ```kotlin
  @ColumnInfo(name = "supabase_id", defaultValue = "")
  val supabaseId: String? = null,
  @ColumnInfo(name = "created_at", defaultValue = "0")
  val createdAt: Long = 0L,
  @ColumnInfo(name = "updated_at", defaultValue = "0")
  val updatedAt: Long = 0L,
  @ColumnInfo(name = "is_deleted", defaultValue = "0")
  val isDeleted: Boolean = false,
  @ColumnInfo(name = "is_dirty", defaultValue = "1")
  val isDirty: Boolean = true,
  ```

  Repeat the same 5-field block at the end of `ChapterWatched`, `NotificationItem`, `SourceOrder`,
  and `IncognitoSource`.

- [ ] **Step 4: Bump version and add migration in `ItemDatabase.kt`**

  Change `version = 6` → `version = 7`.

  In the `companion object`, add after the existing `MIGRATION_1_2` declaration:
  ```kotlin
  val MIGRATION_6_7 = object : Migration(6, 7) {
      override suspend fun migrate(connection: SQLiteConnection) {
          listOf("FavoriteItem", "ChapterWatched", "Notifications", "SourceOrder", "IncognitoSourceTable").forEach { table ->
              connection.execSQL("ALTER TABLE `$table` ADD COLUMN `supabase_id` TEXT DEFAULT NULL")
              connection.execSQL("ALTER TABLE `$table` ADD COLUMN `created_at` INTEGER NOT NULL DEFAULT 0")
              connection.execSQL("ALTER TABLE `$table` ADD COLUMN `updated_at` INTEGER NOT NULL DEFAULT 0")
              connection.execSQL("ALTER TABLE `$table` ADD COLUMN `is_deleted` INTEGER NOT NULL DEFAULT 0")
              connection.execSQL("ALTER TABLE `$table` ADD COLUMN `is_dirty` INTEGER NOT NULL DEFAULT 1")
          }
      }
  }
  ```

  In `getInstance(...)`, add `MIGRATION_6_7` to the `addMigrations(...)` call.

- [ ] **Step 5: Build to verify Room schema generation**

  Run: `./gradlew :favoritesdatabase:kspCommonMainKotlinMetadata`

  Expected: BUILD SUCCESSFUL, new `7.json` appears in
  `favoritesdatabase/schemas/com.programmersbox.favoritesdatabase.ItemDatabase/`

- [ ] **Step 6: Commit**

  ```bash
  git add favoritesdatabase/src/commonMain/kotlin/com/programmersbox/favoritesdatabase/ItemModels.kt
  git add favoritesdatabase/src/commonMain/kotlin/com/programmersbox/favoritesdatabase/ItemDatabase.kt
  git add favoritesdatabase/schemas/com.programmersbox.favoritesdatabase.ItemDatabase/
  git commit -m "feat(supabase): add sync metadata columns to ItemDatabase (v6→v7)"
  ```

---

### Task 7: Room Migrations — HistoryDatabase, BookmarkDatabase, NotesDatabase

**Files:**

- Modify:
  `favoritesdatabase/src/commonMain/kotlin/com/programmersbox/favoritesdatabase/HistoryDatabase.kt`
- Modify:
  `favoritesdatabase/src/commonMain/kotlin/com/programmersbox/favoritesdatabase/BookmarkDatabase.kt`
- Modify:
  `favoritesdatabase/src/commonMain/kotlin/com/programmersbox/favoritesdatabase/NotesDatabase.kt`

**Interfaces:**

- Consumes: `HistoryItem`, `RecentModel`, `BookmarkedChapter`, `NoteItem`
- Produces: all 4 entities gain 5 sync columns; `HistoryDatabase` v3, `BookmarkDatabase` v2,
  `NotesDatabase` v2

- [ ] **Step 1: HistoryDatabase — add sync fields and migration**

  Add 5 sync fields (from Task 6 Step 3 pattern) to `HistoryItem` and `RecentModel` in
  `HistoryDatabase.kt`.

  Change `version = 2` → `version = 3`. Add to companion object:
  ```kotlin
  val MIGRATION_2_3 = object : Migration(2, 3) {
      override suspend fun migrate(connection: SQLiteConnection) {
          listOf("History", "RecentlyViewed").forEach { table ->
              connection.execSQL("ALTER TABLE `$table` ADD COLUMN `supabase_id` TEXT DEFAULT NULL")
              connection.execSQL("ALTER TABLE `$table` ADD COLUMN `created_at` INTEGER NOT NULL DEFAULT 0")
              connection.execSQL("ALTER TABLE `$table` ADD COLUMN `updated_at` INTEGER NOT NULL DEFAULT 0")
              connection.execSQL("ALTER TABLE `$table` ADD COLUMN `is_deleted` INTEGER NOT NULL DEFAULT 0")
              connection.execSQL("ALTER TABLE `$table` ADD COLUMN `is_dirty` INTEGER NOT NULL DEFAULT 1")
          }
      }
  }
  ```

  Update `getInstance` to include `MIGRATION_2_3`.

- [ ] **Step 2: BookmarkDatabase — add sync fields and migration**

  Add 5 sync fields to `BookmarkedChapter` only (skip `BookmarkedChapterFts` — it is a virtual FTS4
  table, ALTER TABLE is not supported).

  Change `version = 1` → `version = 2`. Add companion object with:
  ```kotlin
  val MIGRATION_1_2 = object : Migration(1, 2) {
      override suspend fun migrate(connection: SQLiteConnection) {
          connection.execSQL("ALTER TABLE `bookmarked_chapters` ADD COLUMN `supabase_id` TEXT DEFAULT NULL")
          connection.execSQL("ALTER TABLE `bookmarked_chapters` ADD COLUMN `created_at` INTEGER NOT NULL DEFAULT 0")
          connection.execSQL("ALTER TABLE `bookmarked_chapters` ADD COLUMN `updated_at` INTEGER NOT NULL DEFAULT 0")
          connection.execSQL("ALTER TABLE `bookmarked_chapters` ADD COLUMN `is_deleted` INTEGER NOT NULL DEFAULT 0")
          connection.execSQL("ALTER TABLE `bookmarked_chapters` ADD COLUMN `is_dirty` INTEGER NOT NULL DEFAULT 1")
      }
  }
  ```

  Update `getInstance` to include `MIGRATION_1_2`.

- [ ] **Step 3: NotesDatabase — add sync fields and migration**

  `NoteItem` actual columns in DB are: `itemUrl`, `itemTitle`, `content`, `timestamp`. Add 5 sync
  fields to `NoteItem` only (skip `NoteItemFts`).

  Change `version = 1` → `version = 2`. Add companion object with:
  ```kotlin
  val MIGRATION_1_2 = object : Migration(1, 2) {
      override suspend fun migrate(connection: SQLiteConnection) {
          connection.execSQL("ALTER TABLE `notes` ADD COLUMN `supabase_id` TEXT DEFAULT NULL")
          connection.execSQL("ALTER TABLE `notes` ADD COLUMN `created_at` INTEGER NOT NULL DEFAULT 0")
          connection.execSQL("ALTER TABLE `notes` ADD COLUMN `updated_at` INTEGER NOT NULL DEFAULT 0")
          connection.execSQL("ALTER TABLE `notes` ADD COLUMN `is_deleted` INTEGER NOT NULL DEFAULT 0")
          connection.execSQL("ALTER TABLE `notes` ADD COLUMN `is_dirty` INTEGER NOT NULL DEFAULT 1")
      }
  }
  ```

  Update `getInstance` to include `MIGRATION_1_2`.

- [ ] **Step 4: Build**

  Run: `./gradlew :favoritesdatabase:kspCommonMainKotlinMetadata`

  Expected: BUILD SUCCESSFUL, new schema JSON files generated for all three databases.

- [ ] **Step 5: Commit**

  ```bash
  git add favoritesdatabase/src/commonMain/kotlin/com/programmersbox/favoritesdatabase/HistoryDatabase.kt
  git add favoritesdatabase/src/commonMain/kotlin/com/programmersbox/favoritesdatabase/BookmarkDatabase.kt
  git add favoritesdatabase/src/commonMain/kotlin/com/programmersbox/favoritesdatabase/NotesDatabase.kt
  git add favoritesdatabase/schemas/
  git commit -m "feat(supabase): add sync metadata columns to History/Bookmark/Notes databases"
  ```

---

### Task 8: Room Migrations — Remaining Databases

**Files:**

- Modify:
  `favoritesdatabase/src/commonMain/kotlin/com/programmersbox/favoritesdatabase/BlurHashDatabase.kt`
- Modify:
  `favoritesdatabase/src/commonMain/kotlin/com/programmersbox/favoritesdatabase/HeatMapDatabase.kt`
- Modify:
  `favoritesdatabase/src/commonMain/kotlin/com/programmersbox/favoritesdatabase/Recommendations.kt`
- Modify:
  `favoritesdatabase/src/commonMain/kotlin/com/programmersbox/favoritesdatabase/CustomList.kt`

**Interfaces:**

- Consumes: `BlurHashItem`, `HeatMapItem`, `Recommendation`, `CustomListItem`, `CustomListInfo`
- Produces: all 6 entities gain 5 sync columns; `BlurHashDatabase` v2, `HeatMapDatabase` v2,
  `RecommendationDatabase` v2, `ListDatabase` v12

- [ ] **Step 1: Check ListDatabase untracked schema file**

  Run:
  `cat favoritesdatabase/schemas/com.programmersbox.favoritesdatabase.ListDatabase/12.json | python3 -c "import sys,json; d=json.load(sys.stdin); [print(e['tableName'], [c['columnName'] for c in e['fields']]) for e in d['database']['entities']]" 2>/dev/null || echo "NOT_FOUND"`

  Same logic as Task 6 Step 1 — delete if mismatched.

- [ ] **Step 2: BlurHashDatabase (1→2)**

  `BlurHashItem` fields (verify in `BlurHashDatabase.kt`): `key` (TEXT PK), `hash` (TEXT). Add 5
  sync fields. Table name is `BlurHashItem`.

  Add companion object with `MIGRATION_1_2` altering `BlurHashItem`. Bump version to 2.

  ```kotlin
  val MIGRATION_1_2 = object : Migration(1, 2) {
      override suspend fun migrate(connection: SQLiteConnection) {
          connection.execSQL("ALTER TABLE `BlurHashItem` ADD COLUMN `supabase_id` TEXT DEFAULT NULL")
          connection.execSQL("ALTER TABLE `BlurHashItem` ADD COLUMN `created_at` INTEGER NOT NULL DEFAULT 0")
          connection.execSQL("ALTER TABLE `BlurHashItem` ADD COLUMN `updated_at` INTEGER NOT NULL DEFAULT 0")
          connection.execSQL("ALTER TABLE `BlurHashItem` ADD COLUMN `is_deleted` INTEGER NOT NULL DEFAULT 0")
          connection.execSQL("ALTER TABLE `BlurHashItem` ADD COLUMN `is_dirty` INTEGER NOT NULL DEFAULT 1")
      }
  }
  ```

- [ ] **Step 4: HeatMapDatabase (1→2)**

  `HeatMapItem` fields: `time` (LocalDate PK, stored as TEXT via TypeConverter), `day_count` (INT).
  Table: `HeatMapItem`. Bump to v2.

  ```kotlin
  val MIGRATION_1_2 = object : Migration(1, 2) {
      override suspend fun migrate(connection: SQLiteConnection) {
          connection.execSQL("ALTER TABLE `HeatMapItem` ADD COLUMN `supabase_id` TEXT DEFAULT NULL")
          connection.execSQL("ALTER TABLE `HeatMapItem` ADD COLUMN `created_at` INTEGER NOT NULL DEFAULT 0")
          connection.execSQL("ALTER TABLE `HeatMapItem` ADD COLUMN `updated_at` INTEGER NOT NULL DEFAULT 0")
          connection.execSQL("ALTER TABLE `HeatMapItem` ADD COLUMN `is_deleted` INTEGER NOT NULL DEFAULT 0")
          connection.execSQL("ALTER TABLE `HeatMapItem` ADD COLUMN `is_dirty` INTEGER NOT NULL DEFAULT 1")
      }
  }
  ```

- [ ] **Step 6: ListDatabase (11→12)**

  `CustomListItem` fields: `uuid` (TEXT PK), `name`, `time` (LONG), `useBiometric` (INT),
  `description`. Table: `CustomListItem`.
  `CustomListInfo` fields: `uniqueId` (TEXT PK), `uuid`, `title`, `description`, `url`, `imageUrl`,
  `sources`. Table: `CustomListInfo`.

  Change `version = 11` → `version = 12`. Add:
  ```kotlin
  val MIGRATION_11_12 = object : Migration(11, 12) {
      override suspend fun migrate(connection: SQLiteConnection) {
          listOf("CustomListItem", "CustomListInfo").forEach { table ->
              connection.execSQL("ALTER TABLE `$table` ADD COLUMN `supabase_id` TEXT DEFAULT NULL")
              connection.execSQL("ALTER TABLE `$table` ADD COLUMN `created_at` INTEGER NOT NULL DEFAULT 0")
              connection.execSQL("ALTER TABLE `$table` ADD COLUMN `updated_at` INTEGER NOT NULL DEFAULT 0")
              connection.execSQL("ALTER TABLE `$table` ADD COLUMN `is_deleted` INTEGER NOT NULL DEFAULT 0")
              connection.execSQL("ALTER TABLE `$table` ADD COLUMN `is_dirty` INTEGER NOT NULL DEFAULT 1")
          }
      }
  }
  ```

  Add `MIGRATION_11_12` to `getInstance`.

- [ ] **Step 7: Build all databases**

  Run: `./gradlew :favoritesdatabase:kspCommonMainKotlinMetadata`

  Expected: BUILD SUCCESSFUL, schema JSONs generated for all 5 databases.

- [ ] **Step 8: Commit**

  ```bash
  git add favoritesdatabase/src/commonMain/kotlin/com/programmersbox/favoritesdatabase/
  git add favoritesdatabase/schemas/
  git commit -m "feat(supabase): add sync metadata columns to all remaining databases"
  ```

---

### Task 9: ConnectivityMonitor

**Files:**

- Create:
  `favoritesdatabase/supabase-integration/src/commonMain/kotlin/com/programmersbox/supabaseintegration/sync/ConnectivityMonitor.kt`
- Create:
  `favoritesdatabase/supabase-integration/src/androidMain/kotlin/com/programmersbox/supabaseintegration/sync/ConnectivityMonitor.android.kt`
- Create:
  `favoritesdatabase/supabase-integration/src/jvmMain/kotlin/com/programmersbox/supabaseintegration/sync/ConnectivityMonitor.jvm.kt`
- Create:
  `favoritesdatabase/supabase-integration/src/iosMain/kotlin/com/programmersbox/supabaseintegration/sync/ConnectivityMonitor.ios.kt`

**Interfaces:**

- Consumes: `dev.jordond.connectivity:connectivity-device` (already in `commonLibs`)
- Produces: `ConnectivityMonitor` — `isOnline: StateFlow<Boolean>`, `observe(): Flow<Boolean>`;
  `expect fun createConnectivityMonitor(context: Any?): ConnectivityMonitor`

- [ ] **Step 1: Create `ConnectivityMonitor.kt` (commonMain)**

  ```kotlin
  package com.programmersbox.supabaseintegration.sync

  import kotlinx.coroutines.flow.Flow
  import kotlinx.coroutines.flow.StateFlow

  interface ConnectivityMonitor {
      val isOnline: StateFlow<Boolean>
      fun observe(): Flow<Boolean>
  }

  expect fun createConnectivityMonitor(context: Any?): ConnectivityMonitor
  ```

- [ ] **Step 2: Create `ConnectivityMonitor.android.kt`**

  ```kotlin
  package com.programmersbox.supabaseintegration.sync

  import android.content.Context
  import dev.jordond.connectivity.Connectivity
  import kotlinx.coroutines.CoroutineScope
  import kotlinx.coroutines.Dispatchers
  import kotlinx.coroutines.flow.Flow
  import kotlinx.coroutines.flow.SharingStarted
  import kotlinx.coroutines.flow.StateFlow
  import kotlinx.coroutines.flow.map
  import kotlinx.coroutines.flow.stateIn

  class AndroidConnectivityMonitor(context: Context) : ConnectivityMonitor {
      private val connectivity = Connectivity()
      private val scope = CoroutineScope(Dispatchers.Default)
      override val isOnline: StateFlow<Boolean> = connectivity.statusUpdates
          .map { it.isConnected }
          .stateIn(scope, SharingStarted.Eagerly, true)
      override fun observe(): Flow<Boolean> = isOnline
  }

  actual fun createConnectivityMonitor(context: Any?): ConnectivityMonitor =
      AndroidConnectivityMonitor(context as Context)
  ```

- [ ] **Step 3: Create `ConnectivityMonitor.jvm.kt`**

  ```kotlin
  package com.programmersbox.supabaseintegration.sync

  import kotlinx.coroutines.flow.Flow
  import kotlinx.coroutines.flow.MutableStateFlow
  import kotlinx.coroutines.flow.StateFlow
  import java.net.InetSocketAddress
  import java.net.Socket

  class JvmConnectivityMonitor : ConnectivityMonitor {
      private val _isOnline = MutableStateFlow(checkConnection())
      override val isOnline: StateFlow<Boolean> = _isOnline
      override fun observe(): Flow<Boolean> = _isOnline

      private fun checkConnection(): Boolean = runCatching {
          Socket().use { it.connect(InetSocketAddress("8.8.8.8", 53), 1500) }
          true
      }.getOrDefault(false)
  }

  actual fun createConnectivityMonitor(context: Any?): ConnectivityMonitor = JvmConnectivityMonitor()
  ```

- [ ] **Step 4: Create `ConnectivityMonitor.ios.kt`**

  ```kotlin
  package com.programmersbox.supabaseintegration.sync

  import kotlinx.coroutines.flow.Flow
  import kotlinx.coroutines.flow.MutableStateFlow
  import kotlinx.coroutines.flow.StateFlow

  // Production: replace with NWPathMonitor via platform.Network
  class IosConnectivityMonitor : ConnectivityMonitor {
      private val _isOnline = MutableStateFlow(true)
      override val isOnline: StateFlow<Boolean> = _isOnline
      override fun observe(): Flow<Boolean> = _isOnline
  }

  actual fun createConnectivityMonitor(context: Any?): ConnectivityMonitor = IosConnectivityMonitor()
  ```

- [ ] **Step 5: Commit**

  ```bash
  git add favoritesdatabase/supabase-integration/src/
  git commit -m "feat(supabase): add ConnectivityMonitor with platform impls"
  ```

---

### Task 10: SyncEngine

**Files:**

- Create:
  `favoritesdatabase/supabase-integration/src/commonMain/kotlin/com/programmersbox/supabaseintegration/sync/SyncEngine.kt`
- Create:
  `favoritesdatabase/supabase-integration/src/commonMain/kotlin/com/programmersbox/supabaseintegration/sync/SyncEngineImpl.kt`
- Create:
  `favoritesdatabase/supabase-integration/src/commonMain/kotlin/com/programmersbox/supabaseintegration/sync/SupabaseRows.kt`
- Modify: `favoritesdatabase/src/commonMain/kotlin/com/programmersbox/favoritesdatabase/ItemDao.kt`

**Interfaces:**

- Consumes: `SupabaseClientProvider` (Task 3), `AuthManager` (Task 4), Room DAOs, sync-column
  entities (Tasks 6–8)
- Produces: `SyncEngine` — `pushLocalChanges()`, `pullRemoteChanges(since: Long)`, `fullSync()`

- [ ] **Step 1: Create `SyncEngine.kt`**

  ```kotlin
  package com.programmersbox.supabaseintegration.sync

  interface SyncEngine {
      suspend fun pushLocalChanges()
      suspend fun pullRemoteChanges(since: Long)
      suspend fun fullSync()
  }
  ```

- [ ] **Step 2: Create `SupabaseRows.kt` — serializable row DTOs for PostgREST**

  ```kotlin
  package com.programmersbox.supabaseintegration.sync

  import com.programmersbox.favoritesdatabase.ChapterWatched
  import com.programmersbox.favoritesdatabase.DbModel
  import kotlinx.serialization.SerialName
  import kotlinx.serialization.Serializable

  @Serializable
  data class FavoriteItemRow(
      @SerialName("user_id") val userId: String,
      val url: String,
      val title: String,
      val description: String,
      @SerialName("image_url") val imageUrl: String,
      val source: String,
      @SerialName("num_chapters") val numChapters: Int,
      @SerialName("should_check_for_update") val shouldCheckForUpdate: Boolean,
      @SerialName("supabase_id") val supabaseId: String? = null,
      @SerialName("created_at") val createdAt: Long = 0L,
      @SerialName("updated_at") val updatedAt: Long = 0L,
      @SerialName("is_deleted") val isDeleted: Boolean = false,
  )

  fun FavoriteItemRow.toDbModel() = DbModel(
      url = url, title = title, description = description,
      imageUrl = imageUrl, source = source, numChapters = numChapters,
      shouldCheckForUpdate = shouldCheckForUpdate,
      supabaseId = supabaseId, createdAt = createdAt, updatedAt = updatedAt,
      isDeleted = isDeleted, isDirty = false,
  )

  fun DbModel.toFavoriteRow(userId: String) = FavoriteItemRow(
      userId = userId, url = url, title = title, description = description,
      imageUrl = imageUrl, source = source, numChapters = numChapters,
      shouldCheckForUpdate = shouldCheckForUpdate,
      supabaseId = supabaseId, createdAt = createdAt, updatedAt = updatedAt,
      isDeleted = isDeleted,
  )

  @Serializable
  data class ChapterWatchedRow(
      @SerialName("user_id") val userId: String,
      val url: String,
      val name: String,
      @SerialName("favorite_url") val favoriteUrl: String,
      @SerialName("supabase_id") val supabaseId: String? = null,
      @SerialName("created_at") val createdAt: Long = 0L,
      @SerialName("updated_at") val updatedAt: Long = 0L,
      @SerialName("is_deleted") val isDeleted: Boolean = false,
  )

  fun ChapterWatchedRow.toChapterWatched() = ChapterWatched(
      url = url, name = name, favoriteUrl = favoriteUrl,
      supabaseId = supabaseId, createdAt = createdAt, updatedAt = updatedAt,
      isDeleted = isDeleted, isDirty = false,
  )

  fun ChapterWatched.toChapterRow(userId: String) = ChapterWatchedRow(
      userId = userId, url = url, name = name, favoriteUrl = favoriteUrl,
      supabaseId = supabaseId, createdAt = createdAt, updatedAt = updatedAt,
      isDeleted = isDeleted,
  )
  ```

- [ ] **Step 3: Add dirty-record DAO methods to `ItemDao.kt`**

  Add these queries to the `ItemDao` interface:
  ```kotlin
  @Query("SELECT * FROM FavoriteItem WHERE is_dirty = 1")
  suspend fun getDirtyFavorites(): List<DbModel>

  @Query("SELECT * FROM FavoriteItem WHERE url = :url")
  suspend fun getFavoriteByUrl(url: String): DbModel?

  @Update
  suspend fun updateFavorite(model: DbModel)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertFavorite(model: DbModel)

  @Query("SELECT * FROM ChapterWatched WHERE is_dirty = 1")
  suspend fun getDirtyChapters(): List<ChapterWatched>

  @Query("SELECT * FROM ChapterWatched WHERE url = :url")
  suspend fun getChapterByUrl(url: String): ChapterWatched?

  @Update
  suspend fun updateChapterWatched(model: ChapterWatched)
  ```

- [ ] **Step 4: Create `SyncEngineImpl.kt`**

  ```kotlin
  package com.programmersbox.supabaseintegration.sync

  import com.programmersbox.favoritesdatabase.ItemDao
  import com.programmersbox.supabaseintegration.auth.AuthManager
  import com.programmersbox.supabaseintegration.auth.AuthState
  import com.programmersbox.supabaseintegration.client.SupabaseClientProvider
  import io.github.jan.supabase.postgrest.postgrest
  import kotlinx.coroutines.coroutineScope
  import kotlinx.coroutines.launch

  class SyncEngineImpl(
      private val clientProvider: SupabaseClientProvider,
      private val authManager: AuthManager,
      private val itemDao: ItemDao,
  ) : SyncEngine {

      private val client get() = clientProvider.getOrCreate() ?: error("Client not initialized")
      private val userId get() = (authManager.authState.value as? AuthState.Authenticated)?.user?.id
          ?: error("Not authenticated")

      override suspend fun pushLocalChanges() = coroutineScope {
          val uid = userId
          launch {
              itemDao.getDirtyFavorites().forEach { model ->
                  if (model.isDeleted) {
                      client.postgrest["favorite_items"].delete {
                          filter { eq("user_id", uid); eq("url", model.url) }
                      }
                  } else {
                      client.postgrest["favorite_items"].upsert(model.toFavoriteRow(uid))
                  }
                  itemDao.updateFavorite(model.copy(isDirty = false))
              }
          }
          launch {
              itemDao.getDirtyChapters().forEach { model ->
                  if (model.isDeleted) {
                      client.postgrest["chapters_watched"].delete {
                          filter { eq("user_id", uid); eq("url", model.url) }
                      }
                  } else {
                      client.postgrest["chapters_watched"].upsert(model.toChapterRow(uid))
                  }
                  itemDao.updateChapterWatched(model.copy(isDirty = false))
              }
          }
      }

      override suspend fun pullRemoteChanges(since: Long) {
          val uid = userId
          client.postgrest["favorite_items"]
              .select { filter { eq("user_id", uid); gt("updated_at", since) } }
              .decodeList<FavoriteItemRow>()
              .forEach { row ->
                  val local = itemDao.getFavoriteByUrl(row.url)
                  if (local == null || row.updatedAt > local.updatedAt) {
                      itemDao.insertFavorite(row.toDbModel())
                  }
              }
          client.postgrest["chapters_watched"]
              .select { filter { eq("user_id", uid); gt("updated_at", since) } }
              .decodeList<ChapterWatchedRow>()
              .forEach { row ->
                  val local = itemDao.getChapterByUrl(row.url)
                  if (local == null || row.updatedAt > local.updatedAt) {
                      itemDao.updateChapterWatched(row.toChapterWatched())
                  }
              }
      }

      override suspend fun fullSync() {
          pullRemoteChanges(since = 0L)
          pushLocalChanges()
      }
  }
  ```

- [ ] **Step 5: Build**

  Run: `./gradlew :favoritesdatabase:supabase-integration:compileKotlinAndroid`

  Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

  ```bash
  git add favoritesdatabase/supabase-integration/src/commonMain/kotlin/com/programmersbox/supabaseintegration/sync/
  git add favoritesdatabase/src/commonMain/kotlin/com/programmersbox/favoritesdatabase/ItemDao.kt
  git commit -m "feat(supabase): add SyncEngine with favorites + chapters push/pull"
  ```

---

### Task 11: SyncManager

**Files:**

- Create:
  `favoritesdatabase/supabase-integration/src/commonMain/kotlin/com/programmersbox/supabaseintegration/sync/SyncManager.kt`

**Interfaces:**

- Consumes: `SyncEngine` (Task 10), `AuthManager` (Task 4), `ConnectivityMonitor` (Task 9),
  `SyncConfig` (Task 5)
- Produces: `SyncManager` — `start()`, `stop()`, `triggerSync()`, `syncState: StateFlow<SyncState>`

- [ ] **Step 1: Create `SyncManager.kt`**

  ```kotlin
  package com.programmersbox.supabaseintegration.sync

  import com.programmersbox.supabaseintegration.auth.AuthManager
  import com.programmersbox.supabaseintegration.auth.AuthState
  import kotlinx.coroutines.*
  import kotlinx.coroutines.flow.*

  class SyncManager(
      private val syncEngine: SyncEngine,
      private val authManager: AuthManager,
      private val connectivityMonitor: ConnectivityMonitor,
      private val config: SyncConfig = SyncConfig(),
  ) {
      private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
      private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
      val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

      private var realtimeJob: Job? = null
      private var pollingJob: Job? = null

      fun start() {
          scope.launch {
              combine(authManager.authState, connectivityMonitor.isOnline) { auth, online -> auth to online }
                  .collect { (auth, online) ->
                      when {
                          auth is AuthState.Authenticated && online -> {
                              stopPolling()
                              startInitialSync()
                          }
                          auth is AuthState.Authenticated && !online -> {
                              stopRealtime()
                              startPolling()
                              _syncState.value = SyncState.Offline
                          }
                          else -> {
                              stopRealtime()
                              stopPolling()
                              _syncState.value = SyncState.Idle
                          }
                      }
                  }
          }
      }

      private fun startInitialSync() {
          realtimeJob?.cancel()
          realtimeJob = scope.launch {
              withRetry(config) {
                  _syncState.value = SyncState.Syncing()
                  syncEngine.fullSync()
                  _syncState.value = SyncState.Idle
              }
          }
      }

      private fun stopRealtime() { realtimeJob?.cancel() }

      private fun startPolling() {
          if (pollingJob?.isActive == true) return
          pollingJob = scope.launch {
              while (isActive) {
                  delay(config.pollIntervalMs)
                  if (connectivityMonitor.isOnline.value) {
                      withRetry(config) {
                          _syncState.value = SyncState.Syncing()
                          syncEngine.fullSync()
                          _syncState.value = SyncState.Idle
                      }
                  }
              }
          }
      }

      private fun stopPolling() { pollingJob?.cancel() }

      suspend fun triggerSync() {
          withRetry(config) {
              _syncState.value = SyncState.Syncing()
              syncEngine.fullSync()
              _syncState.value = SyncState.Idle
          }
      }

      fun stop() { scope.cancel() }
  }

  private suspend fun withRetry(config: SyncConfig, block: suspend () -> Unit) {
      var attempt = 0
      var backoff = config.initialBackoffMs
      while (attempt <= config.maxRetries) {
          runCatching { block() }
              .onSuccess { return }
              .onFailure { e ->
                  attempt++
                  if (attempt > config.maxRetries) throw e
                  delay(backoff)
                  backoff = minOf(backoff * 2, config.maxBackoffMs)
              }
      }
  }
  ```

- [ ] **Step 2: Commit**

  ```bash
  git add favoritesdatabase/supabase-integration/src/commonMain/kotlin/com/programmersbox/supabaseintegration/sync/SyncManager.kt
  git commit -m "feat(supabase): add SyncManager with polling fallback and exponential backoff"
  ```

---

### Task 12: BackupManager + RestoreManager

**Files:**

- Create:
  `favoritesdatabase/supabase-integration/src/commonMain/kotlin/com/programmersbox/supabaseintegration/backup/BackupManager.kt`
- Create:
  `favoritesdatabase/supabase-integration/src/commonMain/kotlin/com/programmersbox/supabaseintegration/backup/BackupManagerImpl.kt`
- Create:
  `favoritesdatabase/supabase-integration/src/commonMain/kotlin/com/programmersbox/supabaseintegration/backup/RestoreManager.kt`
- Create:
  `favoritesdatabase/supabase-integration/src/commonMain/kotlin/com/programmersbox/supabaseintegration/backup/RestoreManagerImpl.kt`
- Create:
  `favoritesdatabase/supabase-integration/src/commonMain/kotlin/com/programmersbox/supabaseintegration/backup/FileBytes.kt`
- Create:
  `favoritesdatabase/supabase-integration/src/androidMain/kotlin/com/programmersbox/supabaseintegration/backup/FileBytes.android.kt`
- Create:
  `favoritesdatabase/supabase-integration/src/jvmMain/kotlin/com/programmersbox/supabaseintegration/backup/FileBytes.jvm.kt`
- Create:
  `favoritesdatabase/supabase-integration/src/iosMain/kotlin/com/programmersbox/supabaseintegration/backup/FileBytes.ios.kt`
- Create:
  `favoritesdatabase/supabase-integration/src/androidMain/kotlin/com/programmersbox/supabaseintegration/backup/BackupWorker.kt`

**Interfaces:**

- Consumes: `SupabaseClientProvider` (Task 3), `AuthManager` (Task 4)
- Produces: `BackupManager.uploadBackup(filePath: String): Result<String>`;
  `RestoreManager.listBackups(): List<BackupEntry>`;
  `RestoreManager.downloadBackup(entry: BackupEntry, localPath: String): Result<String>`;
  `BackupEntry(remotePath, name, createdAt, sizeBytes)`

- [ ] **Step 1: Create `BackupManager.kt` and `RestoreManager.kt`**

  `BackupManager.kt`:
  ```kotlin
  package com.programmersbox.supabaseintegration.backup

  import kotlinx.coroutines.flow.Flow

  interface BackupManager {
      suspend fun uploadBackup(filePath: String): Result<String>
      fun getUploadProgress(): Flow<Float>
  }
  ```

  `RestoreManager.kt`:
  ```kotlin
  package com.programmersbox.supabaseintegration.backup

  import kotlinx.coroutines.flow.Flow

  data class BackupEntry(
      val remotePath: String,
      val name: String,
      val createdAt: Long,
      val sizeBytes: Long,
  )

  interface RestoreManager {
      suspend fun listBackups(): List<BackupEntry>
      suspend fun downloadBackup(entry: BackupEntry, localPath: String): Result<String>
      fun getDownloadProgress(): Flow<Float>
  }
  ```

- [ ] **Step 2: Create `FileBytes.kt` expect/actual**

  `FileBytes.kt` (commonMain):
  ```kotlin
  package com.programmersbox.supabaseintegration.backup

  expect fun readFileBytes(filePath: String): ByteArray
  expect fun writeFileBytes(path: String, bytes: ByteArray)
  ```

  `FileBytes.android.kt`:
  ```kotlin
  package com.programmersbox.supabaseintegration.backup
  import java.io.File
  actual fun readFileBytes(filePath: String): ByteArray = File(filePath).readBytes()
  actual fun writeFileBytes(path: String, bytes: ByteArray) { File(path).writeBytes(bytes) }
  ```

  `FileBytes.jvm.kt`:
  ```kotlin
  package com.programmersbox.supabaseintegration.backup
  import java.io.File
  actual fun readFileBytes(filePath: String): ByteArray = File(filePath).readBytes()
  actual fun writeFileBytes(path: String, bytes: ByteArray) { File(path).writeBytes(bytes) }
  ```

  `FileBytes.ios.kt`:
  ```kotlin
  package com.programmersbox.supabaseintegration.backup
  import platform.Foundation.NSData
  import platform.Foundation.NSURL
  import platform.Foundation.dataWithContentsOfFile
  import platform.Foundation.writeToFile
  actual fun readFileBytes(filePath: String): ByteArray {
      val data = NSData.dataWithContentsOfFile(filePath) ?: error("Cannot read $filePath")
      return ByteArray(data.length.toInt()).also { data.getBytes(it.refTo(0), data.length) }
  }
  actual fun writeFileBytes(path: String, bytes: ByteArray) {
      NSData.create(bytes = bytes.refTo(0), length = bytes.size.toULong()).writeToFile(path, true)
  }
  ```

- [ ] **Step 3: Create `BackupManagerImpl.kt`**

  ```kotlin
  package com.programmersbox.supabaseintegration.backup

  import com.programmersbox.supabaseintegration.auth.AuthManager
  import com.programmersbox.supabaseintegration.auth.AuthState
  import com.programmersbox.supabaseintegration.client.SupabaseClientProvider
  import io.github.jan.supabase.storage.storage
  import kotlinx.coroutines.flow.Flow
  import kotlinx.coroutines.flow.MutableStateFlow
  import kotlinx.datetime.Clock

  class BackupManagerImpl(
      private val clientProvider: SupabaseClientProvider,
      private val authManager: AuthManager,
  ) : BackupManager {
      private val _progress = MutableStateFlow(0f)
      override fun getUploadProgress(): Flow<Float> = _progress

      override suspend fun uploadBackup(filePath: String): Result<String> = runCatching {
          val uid = (authManager.authState.value as? AuthState.Authenticated)?.user?.id
              ?: error("Not authenticated")
          val client = clientProvider.getOrCreate() ?: error("Client not initialized")
          val timestamp = Clock.System.now().toEpochMilliseconds()
          val remotePath = "backups/$uid/backup_$timestamp.db"
          client.storage["otakuworld-backups"].upload(remotePath, readFileBytes(filePath)) {
              upsert = false
          }
          remotePath
      }
  }
  ```

- [ ] **Step 4: Create `RestoreManagerImpl.kt`**

  ```kotlin
  package com.programmersbox.supabaseintegration.backup

  import com.programmersbox.supabaseintegration.auth.AuthManager
  import com.programmersbox.supabaseintegration.auth.AuthState
  import com.programmersbox.supabaseintegration.client.SupabaseClientProvider
  import io.github.jan.supabase.storage.storage
  import kotlinx.coroutines.flow.Flow
  import kotlinx.coroutines.flow.MutableStateFlow

  class RestoreManagerImpl(
      private val clientProvider: SupabaseClientProvider,
      private val authManager: AuthManager,
  ) : RestoreManager {
      private val _progress = MutableStateFlow(0f)
      override fun getDownloadProgress(): Flow<Float> = _progress

      override suspend fun listBackups(): List<BackupEntry> {
          val uid = (authManager.authState.value as? AuthState.Authenticated)?.user?.id
              ?: return emptyList()
          val client = clientProvider.getOrCreate() ?: return emptyList()
          return client.storage["otakuworld-backups"]
              .list("backups/$uid")
              .map { obj ->
                  BackupEntry(
                      remotePath = "backups/$uid/${obj.name}",
                      name = obj.name,
                      createdAt = obj.createdAt?.toLongOrNull() ?: 0L,
                      sizeBytes = obj.metadata?.size?.toLongOrNull() ?: 0L,
                  )
              }
              .sortedByDescending { it.createdAt }
      }

      override suspend fun downloadBackup(entry: BackupEntry, localPath: String): Result<String> = runCatching {
          val client = clientProvider.getOrCreate() ?: error("Client not initialized")
          val bytes = client.storage["otakuworld-backups"].downloadAuthenticated(entry.remotePath)
          writeFileBytes(localPath, bytes)
          localPath
      }
  }
  ```

- [ ] **Step 5: Create `BackupWorker.kt` (androidMain)**

  ```kotlin
  package com.programmersbox.supabaseintegration.backup

  import android.content.Context
  import androidx.work.CoroutineWorker
  import androidx.work.WorkerParameters
  import org.koin.core.component.KoinComponent
  import org.koin.core.component.inject

  class BackupWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params), KoinComponent {
      private val backupManager: BackupManager by inject()

      override suspend fun doWork(): Result {
          val dbPath = applicationContext.getDatabasePath("item_database.db").absolutePath
          return backupManager.uploadBackup(dbPath)
              .fold(onSuccess = { Result.success() }, onFailure = { Result.retry() })
      }
  }
  ```

- [ ] **Step 6: Build**

  Run: `./gradlew :favoritesdatabase:supabase-integration:compileKotlinAndroid`

  Expected: BUILD SUCCESSFUL

- [ ] **Step 7: Commit**

  ```bash
  git add favoritesdatabase/supabase-integration/src/
  git commit -m "feat(supabase): add BackupManager, RestoreManager, and Android BackupWorker"
  ```

---

### Task 13: MigrationManager

**Files:**

- Create:
  `favoritesdatabase/supabase-integration/src/commonMain/kotlin/com/programmersbox/supabaseintegration/migration/MigrationManager.kt`
- Create:
  `favoritesdatabase/supabase-integration/src/androidMain/kotlin/com/programmersbox/supabaseintegration/migration/MigrationPrefs.android.kt`
- Create:
  `favoritesdatabase/supabase-integration/src/jvmMain/kotlin/com/programmersbox/supabaseintegration/migration/MigrationPrefs.jvm.kt`
- Create:
  `favoritesdatabase/supabase-integration/src/iosMain/kotlin/com/programmersbox/supabaseintegration/migration/MigrationPrefs.ios.kt`

**Interfaces:**

- Consumes: `SyncEngine` (Task 10)
- Produces: `MigrationManager.runIfNeeded()`, `migrationState: Flow<MigrationState>`,
  `MigrationPrefs` interface + expect/actual

- [ ] **Step 1: Create `MigrationManager.kt`**

  ```kotlin
  package com.programmersbox.supabaseintegration.migration

  import com.programmersbox.supabaseintegration.sync.SyncEngine
  import kotlinx.coroutines.flow.Flow
  import kotlinx.coroutines.flow.MutableStateFlow

  enum class MigrationState { Unknown, Running, Complete, Failed }

  interface MigrationPrefs {
      fun isMigrationComplete(): Boolean
      fun markMigrationComplete()
  }

  expect fun createMigrationPrefs(context: Any?): MigrationPrefs

  class MigrationManager(
      private val syncEngine: SyncEngine,
      private val prefs: MigrationPrefs,
  ) {
      private val _migrationState = MutableStateFlow(MigrationState.Unknown)
      val migrationState: Flow<MigrationState> = _migrationState

      suspend fun runIfNeeded() {
          if (prefs.isMigrationComplete()) {
              _migrationState.value = MigrationState.Complete
              return
          }
          _migrationState.value = MigrationState.Running
          runCatching {
              syncEngine.fullSync()
              prefs.markMigrationComplete()
              _migrationState.value = MigrationState.Complete
          }.onFailure {
              _migrationState.value = MigrationState.Failed
          }
      }
  }
  ```

- [ ] **Step 2: Create platform MigrationPrefs actuals**

  `MigrationPrefs.android.kt`:
  ```kotlin
  package com.programmersbox.supabaseintegration.migration

  import android.content.Context

  class AndroidMigrationPrefs(context: Context) : MigrationPrefs {
      private val prefs = context.getSharedPreferences("supabase_migration", Context.MODE_PRIVATE)
      override fun isMigrationComplete() = prefs.getBoolean("complete", false)
      override fun markMigrationComplete() { prefs.edit().putBoolean("complete", true).apply() }
  }

  actual fun createMigrationPrefs(context: Any?): MigrationPrefs = AndroidMigrationPrefs(context as Context)
  ```

  `MigrationPrefs.jvm.kt`:
  ```kotlin
  package com.programmersbox.supabaseintegration.migration

  import java.io.File

  class JvmMigrationPrefs : MigrationPrefs {
      private val file = File(System.getProperty("user.home"), ".otakuworld/migration_complete")
      override fun isMigrationComplete() = file.exists()
      override fun markMigrationComplete() { file.parentFile?.mkdirs(); file.createNewFile() }
  }

  actual fun createMigrationPrefs(context: Any?): MigrationPrefs = JvmMigrationPrefs()
  ```

  `MigrationPrefs.ios.kt`:
  ```kotlin
  package com.programmersbox.supabaseintegration.migration

  import platform.Foundation.NSUserDefaults

  class IosMigrationPrefs : MigrationPrefs {
      private val defaults = NSUserDefaults.standardUserDefaults
      override fun isMigrationComplete() = defaults.boolForKey("supabase_migration_complete")
      override fun markMigrationComplete() { defaults.setBool(true, "supabase_migration_complete") }
  }

  actual fun createMigrationPrefs(context: Any?): MigrationPrefs = IosMigrationPrefs()
  ```

- [ ] **Step 3: Commit**

  ```bash
  git add favoritesdatabase/supabase-integration/src/
  git commit -m "feat(supabase): add MigrationManager with platform MigrationPrefs"
  ```

---

### Task 14: ViewModels

**Files:**

- Create:
  `favoritesdatabase/supabase-integration/src/commonMain/kotlin/com/programmersbox/supabaseintegration/ui/viewmodel/SupabaseConfigViewModel.kt`
- Create:
  `favoritesdatabase/supabase-integration/src/commonMain/kotlin/com/programmersbox/supabaseintegration/ui/viewmodel/AuthViewModel.kt`
- Create:
  `favoritesdatabase/supabase-integration/src/commonMain/kotlin/com/programmersbox/supabaseintegration/ui/viewmodel/SyncViewModel.kt`
- Create:
  `favoritesdatabase/supabase-integration/src/commonMain/kotlin/com/programmersbox/supabaseintegration/ui/viewmodel/BackupRestoreViewModel.kt`

**Interfaces:**

- Consumes: `CredentialManager` (Task 2), `SupabaseClientProvider` (Task 3), `AuthManager` (Task 4),
  `SyncManager` (Task 11), `BackupManager` / `RestoreManager` (Task 12)
- Produces: 4 KMP `ViewModel` classes exposing `StateFlow` properties consumed by UI screens

- [ ] **Step 1: Create `SupabaseConfigViewModel.kt`**

  ```kotlin
  package com.programmersbox.supabaseintegration.ui.viewmodel

  import androidx.lifecycle.ViewModel
  import androidx.lifecycle.viewModelScope
  import com.programmersbox.supabaseintegration.client.SupabaseClientProvider
  import com.programmersbox.supabaseintegration.credentials.CredentialManager
  import com.programmersbox.supabaseintegration.credentials.SupabaseCredentials
  import kotlinx.coroutines.flow.MutableStateFlow
  import kotlinx.coroutines.flow.SharingStarted
  import kotlinx.coroutines.flow.StateFlow
  import kotlinx.coroutines.flow.stateIn
  import kotlinx.coroutines.launch

  class SupabaseConfigViewModel(
      private val credentialManager: CredentialManager,
      private val clientProvider: SupabaseClientProvider,
  ) : ViewModel() {
      val projectUrl = MutableStateFlow("")
      val anonKey = MutableStateFlow("")
      val connectionResult = MutableStateFlow<String?>(null)
      val hasCredentials: StateFlow<Boolean> = credentialManager.hasCredentials()
          .stateIn(viewModelScope, SharingStarted.Eagerly, false)

      init {
          credentialManager.getCredentials()?.let {
              projectUrl.value = it.projectUrl
              anonKey.value = it.anonKey
          }
      }

      fun onProjectUrlChange(value: String) { projectUrl.value = value }
      fun onAnonKeyChange(value: String) { anonKey.value = value }

      fun testConnection() {
          viewModelScope.launch {
              connectionResult.value = null
              runCatching {
                  val testClient = com.programmersbox.supabaseintegration.credentials.SupabaseCredentials(
                      projectUrl.value.trim(), anonKey.value.trim()
                  )
                  credentialManager.saveCredentials(testClient)
                  clientProvider.recreate()
                  connectionResult.value = "✓ Connection successful"
              }.onFailure {
                  connectionResult.value = "✗ ${it.message}"
              }
          }
      }

      fun save() {
          viewModelScope.launch {
              credentialManager.saveCredentials(SupabaseCredentials(projectUrl.value.trim(), anonKey.value.trim()))
              clientProvider.recreate()
          }
      }

      fun clear() {
          viewModelScope.launch {
              credentialManager.clearCredentials()
              clientProvider.close()
              projectUrl.value = ""
              anonKey.value = ""
          }
      }
  }
  ```

- [ ] **Step 2: Create `AuthViewModel.kt`**

  ```kotlin
  package com.programmersbox.supabaseintegration.ui.viewmodel

  import androidx.lifecycle.ViewModel
  import androidx.lifecycle.viewModelScope
  import com.programmersbox.supabaseintegration.auth.AuthManager
  import com.programmersbox.supabaseintegration.auth.AuthState
  import kotlinx.coroutines.flow.StateFlow
  import kotlinx.coroutines.launch

  class AuthViewModel(private val authManager: AuthManager) : ViewModel() {
      val authState: StateFlow<AuthState> = authManager.authState

      fun signInWithEmail(email: String, password: String) {
          viewModelScope.launch { authManager.signInWithEmail(email, password) }
      }
      fun signUpWithEmail(email: String, password: String) {
          viewModelScope.launch { authManager.signUpWithEmail(email, password) }
      }
      fun signInWithMagicLink(email: String) {
          viewModelScope.launch { authManager.signInWithMagicLink(email) }
      }
      fun signOut() { viewModelScope.launch { authManager.signOut() } }
  }
  ```

- [ ] **Step 3: Create `SyncViewModel.kt`**

  ```kotlin
  package com.programmersbox.supabaseintegration.ui.viewmodel

  import androidx.lifecycle.ViewModel
  import androidx.lifecycle.viewModelScope
  import com.programmersbox.supabaseintegration.sync.SyncManager
  import com.programmersbox.supabaseintegration.sync.SyncState
  import kotlinx.coroutines.flow.StateFlow
  import kotlinx.coroutines.launch

  class SyncViewModel(private val syncManager: SyncManager) : ViewModel() {
      val syncState: StateFlow<SyncState> = syncManager.syncState
      fun triggerSync() { viewModelScope.launch { syncManager.triggerSync() } }
  }
  ```

- [ ] **Step 4: Create `BackupRestoreViewModel.kt`**

  ```kotlin
  package com.programmersbox.supabaseintegration.ui.viewmodel

  import androidx.lifecycle.ViewModel
  import androidx.lifecycle.viewModelScope
  import com.programmersbox.supabaseintegration.backup.BackupEntry
  import com.programmersbox.supabaseintegration.backup.BackupManager
  import com.programmersbox.supabaseintegration.backup.RestoreManager
  import kotlinx.coroutines.flow.MutableStateFlow
  import kotlinx.coroutines.flow.StateFlow
  import kotlinx.coroutines.launch

  class BackupRestoreViewModel(
      private val backupManager: BackupManager,
      private val restoreManager: RestoreManager,
  ) : ViewModel() {
      private val _backups = MutableStateFlow<List<BackupEntry>>(emptyList())
      val backups: StateFlow<List<BackupEntry>> = _backups
      private val _status = MutableStateFlow<String?>(null)
      val status: StateFlow<String?> = _status
      val uploadProgress = backupManager.getUploadProgress()
      val downloadProgress = restoreManager.getDownloadProgress()

      fun loadBackups() {
          viewModelScope.launch {
              runCatching { _backups.value = restoreManager.listBackups() }
                  .onFailure { _status.value = "Failed to load backups: ${it.message}" }
          }
      }

      fun uploadBackup(filePath: String) {
          viewModelScope.launch {
              backupManager.uploadBackup(filePath)
                  .onSuccess { _status.value = "Backup uploaded: $it" }
                  .onFailure { _status.value = "Upload failed: ${it.message}" }
              loadBackups()
          }
      }

      fun downloadBackup(entry: BackupEntry, localPath: String) {
          viewModelScope.launch {
              restoreManager.downloadBackup(entry, localPath)
                  .onSuccess { _status.value = "Downloaded to: $it" }
                  .onFailure { _status.value = "Download failed: ${it.message}" }
          }
      }
  }
  ```

- [ ] **Step 5: Commit**

  ```bash
  git add favoritesdatabase/supabase-integration/src/commonMain/kotlin/com/programmersbox/supabaseintegration/ui/viewmodel/
  git commit -m "feat(supabase): add ViewModels for config, auth, sync, backup/restore"
  ```

---

### Task 15: UI Screens

**Files:**

- Create:
  `favoritesdatabase/supabase-integration/src/commonMain/kotlin/com/programmersbox/supabaseintegration/ui/SupabaseRoutes.kt`
- Create:
  `favoritesdatabase/supabase-integration/src/commonMain/kotlin/com/programmersbox/supabaseintegration/ui/SupabaseConfigScreen.kt`
- Create:
  `favoritesdatabase/supabase-integration/src/commonMain/kotlin/com/programmersbox/supabaseintegration/ui/AuthScreen.kt`
- Create:
  `favoritesdatabase/supabase-integration/src/commonMain/kotlin/com/programmersbox/supabaseintegration/ui/SyncStatusScreen.kt`
- Create:
  `favoritesdatabase/supabase-integration/src/commonMain/kotlin/com/programmersbox/supabaseintegration/ui/BackupRestoreScreen.kt`

**Interfaces:**

- Consumes: all 4 ViewModels (Task 14), `AuthState` (Task 4), `SyncState` (Task 5), `BackupEntry` (
  Task 12)
- Produces: 4 Compose screens + 4 `@Serializable` Navigation3 route objects

- [ ] **Step 1: Create `SupabaseRoutes.kt`**

  ```kotlin
  package com.programmersbox.supabaseintegration.ui

  import kotlinx.serialization.Serializable

  @Serializable data object SupabaseConfigRoute: NavKey
  @Serializable data object AuthRoute: NavKey
  @Serializable data object SyncStatusRoute: NavKey
  @Serializable data object BackupRestoreRoute: NavKey
  ```

- [ ] **Step 2: Create `SupabaseConfigScreen.kt`**

  ```kotlin
  package com.programmersbox.supabaseintegration.ui

  import androidx.compose.foundation.layout.*
  import androidx.compose.material3.*
  import androidx.compose.runtime.*
  import androidx.compose.ui.Modifier
  import androidx.compose.ui.unit.dp
  import androidx.lifecycle.compose.collectAsStateWithLifecycle
  import com.programmersbox.supabaseintegration.ui.viewmodel.SupabaseConfigViewModel
  import org.koin.compose.viewmodel.koinViewModel

  @Composable
  fun SupabaseConfigScreen(
      viewModel: SupabaseConfigViewModel = koinViewModel(),
      onSaved: () -> Unit = {},
  ) {
      val projectUrl by viewModel.projectUrl.collectAsStateWithLifecycle()
      val anonKey by viewModel.anonKey.collectAsStateWithLifecycle()
      val connectionResult by viewModel.connectionResult.collectAsStateWithLifecycle()
      val hasCredentials by viewModel.hasCredentials.collectAsStateWithLifecycle()

      Column(Modifier.fillMaxSize().padding(16.dp)) {
          Text("Supabase Configuration", style = MaterialTheme.typography.headlineMedium)
          Spacer(Modifier.height(24.dp))
          OutlinedTextField(
              value = projectUrl, onValueChange = viewModel::onProjectUrlChange,
              label = { Text("Project URL") },
              placeholder = { Text("https://xxxxxxxxxxxx.supabase.co") },
              modifier = Modifier.fillMaxWidth(), singleLine = true,
          )
          Spacer(Modifier.height(12.dp))
          OutlinedTextField(
              value = anonKey, onValueChange = viewModel::onAnonKeyChange,
              label = { Text("Anon Key") },
              modifier = Modifier.fillMaxWidth(), singleLine = true,
          )
          Spacer(Modifier.height(16.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Button(
                  onClick = viewModel::testConnection,
                  enabled = projectUrl.isNotBlank() && anonKey.isNotBlank()
              ) { Text("Test Connection") }
              Button(
                  onClick = { viewModel.save(); onSaved() },
                  enabled = projectUrl.isNotBlank() && anonKey.isNotBlank()
              ) { Text("Save") }
              if (hasCredentials) {
                  OutlinedButton(onClick = viewModel::clear) { Text("Clear") }
              }
          }
          connectionResult?.let { result ->
              Spacer(Modifier.height(12.dp))
              Text(
                  result,
                  color = if (result.startsWith("✓")) MaterialTheme.colorScheme.primary
                  else MaterialTheme.colorScheme.error,
              )
          }
      }
  }
  ```

- [ ] **Step 3: Create `AuthScreen.kt`**

  ```kotlin
  package com.programmersbox.supabaseintegration.ui

  import androidx.compose.foundation.layout.*
  import androidx.compose.material3.*
  import androidx.compose.runtime.*
  import androidx.compose.ui.Modifier
  import androidx.compose.ui.text.input.PasswordVisualTransformation
  import androidx.compose.ui.unit.dp
  import androidx.lifecycle.compose.collectAsStateWithLifecycle
  import com.programmersbox.supabaseintegration.auth.AuthState
  import com.programmersbox.supabaseintegration.ui.viewmodel.AuthViewModel
  import org.koin.compose.viewmodel.koinViewModel

  @Composable
  fun AuthScreen(viewModel: AuthViewModel = koinViewModel()) {
      val authState by viewModel.authState.collectAsStateWithLifecycle()
      var selectedTab by remember { mutableIntStateOf(0) }
      var email by remember { mutableStateOf("") }
      var password by remember { mutableStateOf("") }

      when (val state = authState) {
          is AuthState.Authenticated -> Column(Modifier.padding(16.dp)) {
              Text("Signed in as ${state.user.email ?: state.user.phone ?: "Anonymous"}")
              Spacer(Modifier.height(16.dp))
              Button(onClick = viewModel::signOut) { Text("Sign Out") }
          }
          else -> Column(Modifier.fillMaxSize().padding(16.dp)) {
              TabRow(selectedTab) {
                  Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Login") })
                  Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Sign Up") })
              }
              Spacer(Modifier.height(16.dp))
              OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
              Spacer(Modifier.height(8.dp))
              OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), singleLine = true)
              Spacer(Modifier.height(16.dp))
              Button(
                  onClick = {
                      if (selectedTab == 0) viewModel.signInWithEmail(email, password)
                      else viewModel.signUpWithEmail(email, password)
                  },
                  modifier = Modifier.fillMaxWidth()
              ) { Text(if (selectedTab == 0) "Login" else "Create Account") }
              Spacer(Modifier.height(8.dp))
              OutlinedButton(
                  onClick = { viewModel.signInWithMagicLink(email) },
                  modifier = Modifier.fillMaxWidth(),
                  enabled = email.isNotBlank(),
              ) { Text("Send Magic Link") }
              if (state is AuthState.Error) {
                  Spacer(Modifier.height(8.dp))
                  Text(state.message, color = MaterialTheme.colorScheme.error)
              }
              if (state is AuthState.Loading) {
                  Spacer(Modifier.height(8.dp))
                  CircularProgressIndicator()
              }
          }
      }
  }
  ```

- [ ] **Step 4: Create `SyncStatusScreen.kt`**

  ```kotlin
  package com.programmersbox.supabaseintegration.ui

  import androidx.compose.foundation.layout.*
  import androidx.compose.material3.*
  import androidx.compose.runtime.*
  import androidx.compose.ui.Modifier
  import androidx.compose.ui.unit.dp
  import androidx.lifecycle.compose.collectAsStateWithLifecycle
  import com.programmersbox.supabaseintegration.sync.SyncState
  import com.programmersbox.supabaseintegration.ui.viewmodel.SyncViewModel
  import org.koin.compose.viewmodel.koinViewModel

  @Composable
  fun SyncStatusScreen(viewModel: SyncViewModel = koinViewModel()) {
      val syncState by viewModel.syncState.collectAsStateWithLifecycle()

      Column(Modifier.fillMaxSize().padding(16.dp)) {
          Text("Sync Status", style = MaterialTheme.typography.headlineMedium)
          Spacer(Modifier.height(16.dp))
          val (label, color) = when (syncState) {
              is SyncState.Idle -> "Idle" to MaterialTheme.colorScheme.onSurface
              is SyncState.Syncing -> "Syncing…" to MaterialTheme.colorScheme.primary
              is SyncState.Error -> "Error: ${(syncState as SyncState.Error).message}" to MaterialTheme.colorScheme.error
              is SyncState.Offline -> "Offline — polling when connection restores" to MaterialTheme.colorScheme.tertiary
          }
          Text(label, color = color, style = MaterialTheme.typography.bodyLarge)
          if (syncState is SyncState.Syncing) {
              Spacer(Modifier.height(8.dp))
              LinearProgressIndicator(Modifier.fillMaxWidth())
          }
          Spacer(Modifier.height(24.dp))
          Button(
              onClick = viewModel::triggerSync,
              enabled = syncState is SyncState.Idle || syncState is SyncState.Error,
              modifier = Modifier.fillMaxWidth()
          ) { Text("Sync Now") }
      }
  }
  ```

- [ ] **Step 5: Create `BackupRestoreScreen.kt`**

  ```kotlin
  package com.programmersbox.supabaseintegration.ui

  import androidx.compose.foundation.layout.*
  import androidx.compose.foundation.lazy.LazyColumn
  import androidx.compose.foundation.lazy.items
  import androidx.compose.material3.*
  import androidx.compose.runtime.*
  import androidx.compose.ui.Modifier
  import androidx.compose.ui.unit.dp
  import androidx.lifecycle.compose.collectAsStateWithLifecycle
  import com.programmersbox.supabaseintegration.backup.BackupEntry
  import com.programmersbox.supabaseintegration.ui.viewmodel.BackupRestoreViewModel
  import kotlinx.datetime.Instant
  import org.koin.compose.viewmodel.koinViewModel

  @Composable
  fun BackupRestoreScreen(
      viewModel: BackupRestoreViewModel = koinViewModel(),
      getLocalDbPath: () -> String,
  ) {
      val backups by viewModel.backups.collectAsStateWithLifecycle()
      val status by viewModel.status.collectAsStateWithLifecycle()
      var confirmRestore by remember { mutableStateOf<BackupEntry?>(null) }

      LaunchedEffect(Unit) { viewModel.loadBackups() }

      Column(Modifier.fillMaxSize().padding(16.dp)) {
          Text("Backup & Restore", style = MaterialTheme.typography.headlineMedium)
          Spacer(Modifier.height(16.dp))
          Button(onClick = { viewModel.uploadBackup(getLocalDbPath()) }, modifier = Modifier.fillMaxWidth()) {
              Text("Back Up Now")
          }
          status?.let {
              Spacer(Modifier.height(8.dp))
              Text(it, style = MaterialTheme.typography.bodySmall)
          }
          Spacer(Modifier.height(24.dp))
          Text("Available Backups", style = MaterialTheme.typography.titleMedium)
          Spacer(Modifier.height(8.dp))
          LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              items(backups) { entry ->
                  Card(Modifier.fillMaxWidth()) {
                      Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                          Column(Modifier.weight(1f)) {
                              Text(entry.name, style = MaterialTheme.typography.bodyMedium)
                              Text(
                                  Instant.fromEpochMilliseconds(entry.createdAt).toString(),
                                  style = MaterialTheme.typography.bodySmall,
                              )
                          }
                          OutlinedButton(onClick = { confirmRestore = entry }) { Text("Restore") }
                      }
                  }
              }
          }
      }

      confirmRestore?.let { entry ->
          AlertDialog(
              onDismissRequest = { confirmRestore = null },
              title = { Text("Restore Backup?") },
              text = { Text("This will replace your local data with \"${entry.name}\". The app must be restarted after restore.") },
              confirmButton = {
                  TextButton(onClick = {
                      viewModel.downloadBackup(entry, getLocalDbPath())
                      confirmRestore = null
                  }) { Text("Restore") }
              },
              dismissButton = {
                  TextButton(onClick = { confirmRestore = null }) { Text("Cancel") }
              }
          )
      }
  }
  ```

- [ ] **Step 6: Build**

  Run: `./gradlew :favoritesdatabase:supabase-integration:compileKotlinAndroid`

  Expected: BUILD SUCCESSFUL

- [ ] **Step 7: Commit**

  ```bash
  git add favoritesdatabase/supabase-integration/src/commonMain/kotlin/com/programmersbox/supabaseintegration/ui/
  git commit -m "feat(supabase): add Compose UI screens and Navigation3 routes"
  ```

---

### Task 16: Koin Module

**Files:**

- Create:
  `favoritesdatabase/supabase-integration/src/commonMain/kotlin/com/programmersbox/supabaseintegration/di/SupabaseModule.kt`

**Interfaces:**

- Consumes: all components from Tasks 2–15
- Produces: `val supabaseModule: Module` — top-level Koin module for host app to include

- [ ] **Step 1: Create `SupabaseModule.kt`**

  ```kotlin
  package com.programmersbox.supabaseintegration.di

  import com.programmersbox.supabaseintegration.auth.AuthManager
  import com.programmersbox.supabaseintegration.auth.AuthManagerImpl
  import com.programmersbox.supabaseintegration.backup.BackupManager
  import com.programmersbox.supabaseintegration.backup.BackupManagerImpl
  import com.programmersbox.supabaseintegration.backup.RestoreManager
  import com.programmersbox.supabaseintegration.backup.RestoreManagerImpl
  import com.programmersbox.supabaseintegration.client.SupabaseClientProvider
  import com.programmersbox.supabaseintegration.credentials.CredentialManager
  import com.programmersbox.supabaseintegration.credentials.createCredentialManager
  import com.programmersbox.supabaseintegration.migration.MigrationManager
  import com.programmersbox.supabaseintegration.migration.MigrationPrefs
  import com.programmersbox.supabaseintegration.migration.createMigrationPrefs
  import com.programmersbox.supabaseintegration.sync.*
  import com.programmersbox.supabaseintegration.ui.viewmodel.*
  import org.koin.core.module.dsl.viewModelOf
  import org.koin.dsl.module

  val supabaseModule = module {
      single<CredentialManager> { createCredentialManager(getOrNull()) }
      single { SupabaseClientProvider(get()) }
      single<AuthManager> { AuthManagerImpl(get()) }
      single<ConnectivityMonitor> { createConnectivityMonitor(getOrNull()) }
      single { SyncConfig() }
      single<SyncEngine> { SyncEngineImpl(get(), get(), get()) }
      single { SyncManager(get(), get(), get(), get()) }
      single<BackupManager> { BackupManagerImpl(get(), get()) }
      single<RestoreManager> { RestoreManagerImpl(get(), get()) }
      single<MigrationPrefs> { createMigrationPrefs(getOrNull()) }
      single { MigrationManager(get(), get()) }

      viewModelOf(::SupabaseConfigViewModel)
      viewModelOf(::AuthViewModel)
      viewModelOf(::SyncViewModel)
      viewModelOf(::BackupRestoreViewModel)
  }
  ```

    - [ ] **Step 3: Create a function to easily add the routes to the Navigation3 graph**

      In `kmpuiviews/src/commonMain/kotlin/.../navigation/Nav3Graph.kt`, inside `entryGraph()`:
      ```kotlin
      import com.programmersbox.supabaseintegration.ui.*
  
      fun NavGraphBuilder.supabaseGraph() {
      entry<SupabaseConfigRoute> { SupabaseConfigScreen(onSaved = { navController.navigateUp() }) }
      entry<AuthRoute> { AuthScreen() }
      entry<SyncStatusRoute> { SyncStatusScreen() }
      entry<BackupRestoreRoute> {
          BackupRestoreScreen(getLocalDbPath = { context.getDatabasePath("item_database.db").absolutePath })
      }
      }
      ```

- [ ] **Step 4: Build full project**

  Run: `./gradlew :mangaworld:assembleNoFirebaseDebug`

  Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

  ```bash
  git add favoritesdatabase/supabase-integration/src/commonMain/kotlin/com/programmersbox/supabaseintegration/di/
  git commit -m "feat(supabase): add Koin module and wire routes into nav graph"
  ```

---

### Task 17: SQL Schema

**Files:**

- Create: `docs/supabase/supabase_schema.sql`

**Interfaces:**

- Consumes: all entity table structures from Tasks 6–8
- Produces: complete DDL to paste into Supabase SQL Editor — run once per project

- [ ] **Step 1: Create `docs/supabase/supabase_schema.sql`**

  ```sql
  -- Run this in your Supabase project SQL Editor once.
  -- Requires: Authentication enabled in your Supabase project.

  CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

  -- ─── FAVORITES ────────────────────────────────────────────────────────────────
  CREATE TABLE IF NOT EXISTS favorite_items (
      id                      UUID    DEFAULT uuid_generate_v4() PRIMARY KEY,
      user_id                 UUID    NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
      url                     TEXT    NOT NULL,
      title                   TEXT    NOT NULL DEFAULT '',
      description             TEXT    NOT NULL DEFAULT '',
      image_url               TEXT    NOT NULL DEFAULT '',
      source                  TEXT    NOT NULL DEFAULT '',
      num_chapters            INTEGER NOT NULL DEFAULT 0,
      should_check_for_update BOOLEAN NOT NULL DEFAULT true,
      created_at              BIGINT  NOT NULL DEFAULT 0,
      updated_at              BIGINT  NOT NULL DEFAULT 0,
      is_deleted              BOOLEAN NOT NULL DEFAULT false,
      UNIQUE(user_id, url)
  );
  ALTER TABLE favorite_items ENABLE ROW LEVEL SECURITY;
  CREATE POLICY "own_favorites" ON favorite_items FOR ALL USING (auth.uid() = user_id);
  CREATE INDEX idx_favorites_updated ON favorite_items(user_id, updated_at);

  -- ─── CHAPTERS WATCHED ─────────────────────────────────────────────────────────
  CREATE TABLE IF NOT EXISTS chapters_watched (
      id           UUID   DEFAULT uuid_generate_v4() PRIMARY KEY,
      user_id      UUID   NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
      url          TEXT   NOT NULL,
      name         TEXT   NOT NULL DEFAULT '',
      favorite_url TEXT   NOT NULL DEFAULT '',
      created_at   BIGINT NOT NULL DEFAULT 0,
      updated_at   BIGINT NOT NULL DEFAULT 0,
      is_deleted   BOOLEAN NOT NULL DEFAULT false,
      UNIQUE(user_id, url)
  );
  ALTER TABLE chapters_watched ENABLE ROW LEVEL SECURITY;
  CREATE POLICY "own_chapters" ON chapters_watched FOR ALL USING (auth.uid() = user_id);
  CREATE INDEX idx_chapters_updated ON chapters_watched(user_id, updated_at);

  -- ─── BOOKMARKED CHAPTERS ──────────────────────────────────────────────────────
  CREATE TABLE IF NOT EXISTS bookmarked_chapters (
      id               UUID   DEFAULT uuid_generate_v4() PRIMARY KEY,
      user_id          UUID   NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
      chapter_url      TEXT   NOT NULL,
      chapter_name     TEXT   NOT NULL DEFAULT '',
      parent_url       TEXT   NOT NULL DEFAULT '',
      parent_title     TEXT   NOT NULL DEFAULT '',
      parent_image_url TEXT   NOT NULL DEFAULT '',
      source           TEXT   NOT NULL DEFAULT '',
      timestamp        BIGINT NOT NULL DEFAULT 0,
      created_at       BIGINT NOT NULL DEFAULT 0,
      updated_at       BIGINT NOT NULL DEFAULT 0,
      is_deleted       BOOLEAN NOT NULL DEFAULT false,
      UNIQUE(user_id, chapter_url)
  );
  ALTER TABLE bookmarked_chapters ENABLE ROW LEVEL SECURITY;
  CREATE POLICY "own_bookmarks" ON bookmarked_chapters FOR ALL USING (auth.uid() = user_id);
  CREATE INDEX idx_bookmarks_updated ON bookmarked_chapters(user_id, updated_at);

  -- ─── NOTES ────────────────────────────────────────────────────────────────────
  CREATE TABLE IF NOT EXISTS notes (
      id         UUID   DEFAULT uuid_generate_v4() PRIMARY KEY,
      user_id    UUID   NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
      item_url   TEXT   NOT NULL,
      item_title TEXT   NOT NULL DEFAULT '',
      content    TEXT   NOT NULL DEFAULT '',
      timestamp  BIGINT NOT NULL DEFAULT 0,
      created_at BIGINT NOT NULL DEFAULT 0,
      updated_at BIGINT NOT NULL DEFAULT 0,
      is_deleted BOOLEAN NOT NULL DEFAULT false,
      UNIQUE(user_id, item_url)
  );
  ALTER TABLE notes ENABLE ROW LEVEL SECURITY;
  CREATE POLICY "own_notes" ON notes FOR ALL USING (auth.uid() = user_id);
  CREATE INDEX idx_notes_updated ON notes(user_id, updated_at);

  -- ─── HISTORY ──────────────────────────────────────────────────────────────────
  CREATE TABLE IF NOT EXISTS history (
      id          UUID   DEFAULT uuid_generate_v4() PRIMARY KEY,
      user_id     UUID   NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
      search_text TEXT   NOT NULL,
      time        BIGINT NOT NULL DEFAULT 0,
      created_at  BIGINT NOT NULL DEFAULT 0,
      updated_at  BIGINT NOT NULL DEFAULT 0,
      is_deleted  BOOLEAN NOT NULL DEFAULT false,
      UNIQUE(user_id, search_text)
  );
  ALTER TABLE history ENABLE ROW LEVEL SECURITY;
  CREATE POLICY "own_history" ON history FOR ALL USING (auth.uid() = user_id);
  CREATE INDEX idx_history_updated ON history(user_id, updated_at);
  
  -- ─── CUSTOM LIST ITEMS ────────────────────────────────────────────────────────
  CREATE TABLE IF NOT EXISTS custom_list_items (
      id           UUID    DEFAULT uuid_generate_v4() PRIMARY KEY,
      user_id      UUID    NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
      uuid         TEXT    NOT NULL,
      name         TEXT    NOT NULL DEFAULT '',
      time         BIGINT  NOT NULL DEFAULT 0,
      use_biometric BOOLEAN NOT NULL DEFAULT false,
      description  TEXT    NOT NULL DEFAULT '',
      created_at   BIGINT  NOT NULL DEFAULT 0,
      updated_at   BIGINT  NOT NULL DEFAULT 0,
      is_deleted   BOOLEAN NOT NULL DEFAULT false,
      UNIQUE(user_id, uuid)
  );
  ALTER TABLE custom_list_items ENABLE ROW LEVEL SECURITY;
  CREATE POLICY "own_custom_list_items" ON custom_list_items FOR ALL USING (auth.uid() = user_id);
  CREATE INDEX idx_custom_list_items_updated ON custom_list_items(user_id, updated_at);

  -- ─── CUSTOM LIST INFO ─────────────────────────────────────────────────────────
  CREATE TABLE IF NOT EXISTS custom_list_info (
      id          UUID   DEFAULT uuid_generate_v4() PRIMARY KEY,
      user_id     UUID   NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
      unique_id   TEXT   NOT NULL,
      uuid        TEXT   NOT NULL,
      title       TEXT   NOT NULL DEFAULT '',
      description TEXT   NOT NULL DEFAULT '',
      url         TEXT   NOT NULL DEFAULT '',
      image_url   TEXT   NOT NULL DEFAULT '',
      source      TEXT   NOT NULL DEFAULT '',
      created_at  BIGINT NOT NULL DEFAULT 0,
      updated_at  BIGINT NOT NULL DEFAULT 0,
      is_deleted  BOOLEAN NOT NULL DEFAULT false,
      UNIQUE(user_id, unique_id)
  );
  ALTER TABLE custom_list_info ENABLE ROW LEVEL SECURITY;
  CREATE POLICY "own_custom_list_info" ON custom_list_info FOR ALL USING (auth.uid() = user_id);
  CREATE INDEX idx_custom_list_info_updated ON custom_list_info(user_id, updated_at);

  -- ─── RECOMMENDATIONS ──────────────────────────────────────────────────────────
  CREATE TABLE IF NOT EXISTS recommendations (
      id          UUID   DEFAULT uuid_generate_v4() PRIMARY KEY,
      user_id     UUID   NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
      title       TEXT   NOT NULL,
      description TEXT   NOT NULL DEFAULT '',
      reason      TEXT   NOT NULL DEFAULT '',
      genre       TEXT   NOT NULL DEFAULT '[]',  -- JSON array stored as TEXT
      created_at  BIGINT NOT NULL DEFAULT 0,
      updated_at  BIGINT NOT NULL DEFAULT 0,
      is_deleted  BOOLEAN NOT NULL DEFAULT false,
      UNIQUE(user_id, title)
  );
  ALTER TABLE recommendations ENABLE ROW LEVEL SECURITY;
  CREATE POLICY "own_recommendations" ON recommendations FOR ALL USING (auth.uid() = user_id);
  CREATE INDEX idx_recommendations_updated ON recommendations(user_id, updated_at);

  -- ─── HEATMAP ──────────────────────────────────────────────────────────────────
  -- HeatMapItem.time is a LocalDate stored as JSON TEXT by Room's TypeConverter
  CREATE TABLE IF NOT EXISTS heatmap_items (
      id         UUID    DEFAULT uuid_generate_v4() PRIMARY KEY,
      user_id    UUID    NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
      time       TEXT    NOT NULL,  -- ISO-8601 date string e.g. "2026-06-17"
      day_count  INTEGER NOT NULL DEFAULT 0,
      created_at BIGINT  NOT NULL DEFAULT 0,
      updated_at BIGINT  NOT NULL DEFAULT 0,
      is_deleted BOOLEAN NOT NULL DEFAULT false,
      UNIQUE(user_id, time)
  );
  ALTER TABLE heatmap_items ENABLE ROW LEVEL SECURITY;
  CREATE POLICY "own_heatmap" ON heatmap_items FOR ALL USING (auth.uid() = user_id);
  CREATE INDEX idx_heatmap_updated ON heatmap_items(user_id, updated_at);

  -- ─── STORAGE BUCKET ───────────────────────────────────────────────────────────
  INSERT INTO storage.buckets (id, name, public)
  VALUES ('otakuworld-backups', 'otakuworld-backups', false)
  ON CONFLICT (id) DO NOTHING;

  CREATE POLICY "own_backups"
      ON storage.objects FOR ALL
      USING (
          bucket_id = 'otakuworld-backups'
          AND auth.uid()::text = (storage.foldername(name))[2]
      );
  ```

- [ ] **Step 2: Commit**

  ```bash
  git add docs/supabase/supabase_schema.sql
  git commit -m "docs(supabase): add complete Supabase SQL schema with RLS policies"
  ```

---

## Post-Implementation Notes

### Firebase removal checklist (run after verifying sync works end-to-end)

1. In `sharedutils/build.gradle.kts`: remove `firebaseFirestore`, `firebaseAuth`, `firebaseUiAuth`
   from the `full` flavor dependencies.
2. Delete `sharedutils/src/full/java/FirebaseUtils.kt`.
3. Delete `sharedutils/src/main/java/com/programmersbox/sharedutils/FirebaseSharedModels.kt`.
4. Remove any call sites that reference `FirebaseDb`, `FirebaseAuthentication`, or
   `FirebaseConnection` — replace with `AuthManager` and `SyncEngine` equivalents from this module.

### Recommended sub-branch strategy

Implement and verify as 3 sub-branches before merging to `feat/supabase-integration`:

- `supabase/foundation` — Tasks 1–5 (Gradle, Credentials, Client, Auth, Sync types)
- `supabase/sync` — Tasks 6–11 (Room migrations, SyncEngine, SyncManager)
- `supabase/features` — Tasks 12–17 (Backup, ViewModels, UI, Koin, SQL)
