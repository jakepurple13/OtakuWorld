# Android Credential Manager Integration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add Android-only Credential Manager sign-in (saved passwords) and passkey registration
to the existing Supabase auth screen in `favoritesdatabase/supabase-integration`, without changing
the existing email/password flow.

**Architecture:** A new `CredentialSignIn` expect/actual interface (mirroring the module's existing
`CredentialManager` expect/actual pattern) wraps `androidx.credentials` on Android and is a no-op
on iOS/JVM. Two new `AuthManager` methods wrap the stable `auth.passkeys` API for registration.
`AuthViewModel` wires both together; `AuthScreen.kt` gets two new Android-only buttons via a second
expect/actual composable pair.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, Koin, supabase-kt 3.7.0 (`auth-kt`,
already pinned via `commonLibs.supabase.bom`), androidx.credentials 1.6.0.

## Global Constraints

- Spec doc: `docs/superpowers/specs/2026-07-24-android-credential-manager-design.md` — read it
  first if anything here is ambiguous.
- Passkey **sign-in** is explicitly out of scope (supabase-kt 3.7.0 has no `startAuthentication`/
  `verifyAuthentication` in a published release). Do not add a passkey sign-in button or wire one
  up. Only saved-password sign-in and passkey **registration** ship in this plan.
- Existing email/password sign-in/sign-up/magic-link flow in `AuthScreen.kt` /
  `AuthManagerImpl.kt` must not change behavior. Only additive changes.
- All new Credential Manager / passkey UI is Android-only — iOS and JVM/Desktop builds must
  compile with no visible button and no behavior change.
- No automated unit tests requested for this feature (per spec) — verify each task by compiling
  the affected source set(s), not by writing test files. Deviation from the writing-plans default:
  this is an explicit, spec-approved scope decision, not a shortcut.
- `androidx.credentials:credentials:1.6.0` and `androidx.credentials:credentials-play-services-auth:1.6.0`
  are the exact versions to pin (confirmed latest stable on Google's Maven as of 2026-07-24).
- `androidx.credentials.CredentialManager.getCredential()` / `.createCredential()` require an
  **Activity** context to display their UI (an application context will fail). Thread the calling
  `Context` through as a per-call parameter (`context: Any?`), obtained from `LocalContext.current`
  at the button's `onClick`, not stored in the DI-provided singleton. This is a refinement over the
  spec doc, which showed context only at factory construction — noted here since it changes the
  `CredentialSignIn` method signatures from what's written there.

---

### Task 1: Gradle dependencies

**Files:**
- Modify: `gradle/android.versions.toml`
- Modify: `favoritesdatabase/supabase-integration/build.gradle.kts`

**Interfaces:**
- Produces: `androidLibs.androidx.credentials`, `androidLibs.androidx.credentials.play.services.auth`
  version catalog aliases, consumed by Task 3.

- [ ] **Step 1: Add version catalog entries**

Open `gradle/android.versions.toml`. Find the existing `androidx-security-crypto` entry (used by
`AndroidCredentialManager`) and add two entries next to it, in the same `[libraries]` table:

```toml
androidx-credentials = { module = "androidx.credentials:credentials", version = "1.6.0" }
androidx-credentials-play-services-auth = { module = "androidx.credentials:credentials-play-services-auth", version = "1.6.0" }
```

- [ ] **Step 2: Wire the dependencies into the module**

Open `favoritesdatabase/supabase-integration/build.gradle.kts`. In the `androidMain.dependencies`
block, find this existing line:

```kotlin
            implementation(androidLibs.androidx.security.crypto)
```

Add directly below it:

```kotlin
            implementation(androidLibs.androidx.credentials)
            implementation(androidLibs.androidx.credentials.play.services.auth)
```

- [ ] **Step 3: Verify the sync**

Run: `./gradlew :favoritesdatabase:supabase-integration:dependencies --configuration androidDebugCompileClasspath | grep credentials`
Expected: output lists `androidx.credentials:credentials:1.6.0` and
`androidx.credentials:credentials-play-services-auth:1.6.0`.

- [ ] **Step 4: Commit**

```bash
git add gradle/android.versions.toml favoritesdatabase/supabase-integration/build.gradle.kts
git commit -m "build: add androidx.credentials dependencies for Credential Manager support"
```

---

### Task 2: `CredentialSignIn` commonMain contract

**Files:**
- Create: `favoritesdatabase/supabase-integration/src/commonMain/kotlin/com/programmersbox/supabaseintegration/credentials/CredentialSignIn.kt`

**Interfaces:**
- Produces: `CredentialSignInResult` (sealed interface: `Success(email, password)`,
  `NoCredentials`, `Cancelled`, `Error(message)`), `PasskeyRegistrationResult` (sealed interface:
  `Success(credentialJson)`, `Cancelled`, `Error(message)`), `CredentialSignIn` interface
  (`isSupported: Boolean`, `suspend fun signInWithSavedPassword(context: Any?): CredentialSignInResult`,
  `suspend fun registerPasskey(context: Any?, challengeId: String, creationOptionsJson: String): PasskeyRegistrationResult`),
  `expect fun createCredentialSignIn(): CredentialSignIn`. Consumed by Tasks 3, 4, 5, 7.

- [ ] **Step 1: Write the file**

```kotlin
package com.programmersbox.supabaseintegration.credentials

/**
 * Result of attempting to sign in with a saved password via the platform credential store.
 */
sealed interface CredentialSignInResult {
    data class Success(val email: String, val password: String) : CredentialSignInResult
    data object NoCredentials : CredentialSignInResult
    data object Cancelled : CredentialSignInResult
    data class Error(val message: String) : CredentialSignInResult
}

/**
 * Result of registering a new passkey via the platform credential store.
 */
sealed interface PasskeyRegistrationResult {
    data class Success(val credentialJson: String) : PasskeyRegistrationResult
    data object Cancelled : PasskeyRegistrationResult
    data class Error(val message: String) : PasskeyRegistrationResult
}

/**
 * Platform credential store integration (Android Credential Manager / androidx.credentials).
 * Distinct from [CredentialManager] in this same package, which persists the Supabase project
 * URL and anon key — unrelated to platform credential storage.
 *
 * [isSupported] is false on every platform except Android; the sign-in/register methods are
 * unreachable on unsupported platforms because the UI that calls them is not rendered there.
 *
 * [context] is an opaque per-call Android Activity context (passed as `Any?` to keep this
 * interface multiplatform); androidx.credentials requires an Activity context to display its
 * picker UI. Ignored on platforms where [isSupported] is false.
 */
interface CredentialSignIn {
    val isSupported: Boolean
    suspend fun signInWithSavedPassword(context: Any?): CredentialSignInResult
    suspend fun registerPasskey(context: Any?, challengeId: String, creationOptionsJson: String): PasskeyRegistrationResult
}

expect fun createCredentialSignIn(): CredentialSignIn
```

- [ ] **Step 2: Verify it compiles (commonMain only, will fail without actuals — expected at this stage)**

Run: `./gradlew :favoritesdatabase:supabase-integration:compileKotlinMetadata`
Expected: FAIL with "Expected function 'createCredentialSignIn' has no actual declaration". This
confirms the commonMain file parses correctly; the missing-actual error is expected until Task 3/4.

- [ ] **Step 3: Commit**

```bash
git add favoritesdatabase/supabase-integration/src/commonMain/kotlin/com/programmersbox/supabaseintegration/credentials/CredentialSignIn.kt
git commit -m "feat: add CredentialSignIn commonMain contract"
```

---

### Task 3: Android actual implementation

**Files:**
- Create: `favoritesdatabase/supabase-integration/src/androidMain/kotlin/com/programmersbox/supabaseintegration/credentials/CredentialSignIn.android.kt`

**Interfaces:**
- Consumes: `CredentialSignIn`, `CredentialSignInResult`, `PasskeyRegistrationResult` from Task 2.
- Produces: `AndroidCredentialSignIn : CredentialSignIn`, `actual fun createCredentialSignIn(): CredentialSignIn`.
  Consumed by Task 5 (DI).

- [ ] **Step 1: Write the file**

```kotlin
package com.programmersbox.supabaseintegration.credentials

import android.content.Context
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.CredentialManager as AndroidxCredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPasswordOption
import androidx.credentials.PasswordCredential
import androidx.credentials.exceptions.CreateCredentialCancellationException
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException

/*
 * Supabase dashboard setup for passkeys (registration only — sign-in is not yet supported by
 * supabase-kt, see docs/superpowers/specs/2026-07-24-android-credential-manager-design.md):
 *
 * 1. Dashboard -> Authentication -> Passkeys.
 * 2. Turn on "Enable Passkey authentication".
 * 3. Set Relying Party ID: bare domain, no scheme/port/path (e.g. "example.com").
 * 4. Set Relying Party Display Name: human-readable app name shown in the OS passkey prompt.
 * 5. Set Relying Party Origins: comma-separated allowed origins (up to 5). HTTPS required except
 *    for "localhost" / "127.0.0.1" / "[::1]".
 * 6. Passkeys are cryptographically bound to the Relying Party ID — changing it invalidates every
 *    previously registered passkey.
 */
class AndroidCredentialSignIn(private val context: Context) : CredentialSignIn {

    override val isSupported: Boolean = true

    override suspend fun signInWithSavedPassword(context: Any?): CredentialSignInResult {
        val activityContext = context as? Context ?: this.context
        val manager = AndroidxCredentialManager.create(activityContext)
        val request = GetCredentialRequest(listOf(GetPasswordOption()))
        return try {
            val response = manager.getCredential(activityContext, request)
            val credential = response.credential as? PasswordCredential
            if (credential != null) {
                CredentialSignInResult.Success(email = credential.id, password = credential.password)
            } else {
                CredentialSignInResult.Error("Unexpected credential type returned")
            }
        } catch (e: NoCredentialException) {
            CredentialSignInResult.NoCredentials
        } catch (e: GetCredentialCancellationException) {
            CredentialSignInResult.Cancelled
        } catch (e: GetCredentialException) {
            CredentialSignInResult.Error(e.message ?: "Credential Manager sign-in failed")
        }
    }

    override suspend fun registerPasskey(
        context: Any?,
        challengeId: String,
        creationOptionsJson: String,
    ): PasskeyRegistrationResult {
        val activityContext = context as? Context ?: this.context
        val manager = AndroidxCredentialManager.create(activityContext)
        val request = CreatePublicKeyCredentialRequest(requestJson = creationOptionsJson)
        return try {
            val response = manager.createCredential(activityContext, request) as CreatePublicKeyCredentialResponse
            PasskeyRegistrationResult.Success(credentialJson = response.registrationResponseJson)
        } catch (e: CreateCredentialCancellationException) {
            PasskeyRegistrationResult.Cancelled
        } catch (e: CreateCredentialException) {
            PasskeyRegistrationResult.Error(e.message ?: "Passkey registration failed")
        }
    }
}

actual fun createCredentialSignIn(): CredentialSignIn = error(
    "AndroidCredentialSignIn requires a Context — use the Koin-provided single<CredentialSignIn> binding instead of calling this factory directly on Android."
)
```

Note: unlike the existing `createCredentialManager(context: Any?)` factory, the Android
`createCredentialSignIn()` factory intentionally cannot construct a real instance without a
`Context`. Task 5 binds `single<CredentialSignIn> { AndroidCredentialSignIn(get()) }` directly on
Android instead of calling this factory — this stub only exists so the `expect`/`actual` pair
compiles for the Android target. iOS/JVM (Task 4) use the factory normally.

- [ ] **Step 2: Verify it compiles**

Run: `./gradlew :favoritesdatabase:supabase-integration:compileDebugKotlinAndroid`
Expected: BUILD SUCCESSFUL (or fails only on the still-missing iOS/JVM actuals if those targets are
built as part of the same task — Task 4 covers those; running the Android-specific compile task
above isolates this task's correctness).

- [ ] **Step 3: Commit**

```bash
git add favoritesdatabase/supabase-integration/src/androidMain/kotlin/com/programmersbox/supabaseintegration/credentials/CredentialSignIn.android.kt
git commit -m "feat(android): implement CredentialSignIn via androidx.credentials"
```

---

### Task 4: iOS and JVM no-op actuals

**Files:**
- Create: `favoritesdatabase/supabase-integration/src/iosMain/kotlin/com/programmersbox/supabaseintegration/credentials/CredentialSignIn.ios.kt`
- Create: `favoritesdatabase/supabase-integration/src/jvmMain/kotlin/com/programmersbox/supabaseintegration/credentials/CredentialSignIn.jvm.kt`

**Interfaces:**
- Consumes: `CredentialSignIn`, `CredentialSignInResult`, `PasskeyRegistrationResult` from Task 2.
- Produces: `actual fun createCredentialSignIn(): CredentialSignIn` for iOS and JVM targets.
  Consumed by Task 5 (DI).

- [ ] **Step 1: Write the iOS file**

```kotlin
package com.programmersbox.supabaseintegration.credentials

private class UnsupportedCredentialSignIn : CredentialSignIn {
    override val isSupported: Boolean = false

    override suspend fun signInWithSavedPassword(context: Any?): CredentialSignInResult =
        CredentialSignInResult.Error("Credential Manager sign-in is not supported on this platform")

    override suspend fun registerPasskey(
        context: Any?,
        challengeId: String,
        creationOptionsJson: String,
    ): PasskeyRegistrationResult =
        PasskeyRegistrationResult.Error("Passkey registration is not supported on this platform")
}

actual fun createCredentialSignIn(): CredentialSignIn = UnsupportedCredentialSignIn()
```

- [ ] **Step 2: Write the JVM file (identical body, separate actual declaration per target)**

```kotlin
package com.programmersbox.supabaseintegration.credentials

private class UnsupportedCredentialSignIn : CredentialSignIn {
    override val isSupported: Boolean = false

    override suspend fun signInWithSavedPassword(context: Any?): CredentialSignInResult =
        CredentialSignInResult.Error("Credential Manager sign-in is not supported on this platform")

    override suspend fun registerPasskey(
        context: Any?,
        challengeId: String,
        creationOptionsJson: String,
    ): PasskeyRegistrationResult =
        PasskeyRegistrationResult.Error("Passkey registration is not supported on this platform")
}

actual fun createCredentialSignIn(): CredentialSignIn = UnsupportedCredentialSignIn()
```

- [ ] **Step 3: Verify iOS and JVM compile**

Run: `./gradlew :favoritesdatabase:supabase-integration:compileKotlinIosArm64`
Expected: BUILD SUCCESSFUL

Run: `./gradlew :favoritesdatabase:supabase-integration:compileKotlinJvm`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add favoritesdatabase/supabase-integration/src/iosMain/kotlin/com/programmersbox/supabaseintegration/credentials/CredentialSignIn.ios.kt
git add favoritesdatabase/supabase-integration/src/jvmMain/kotlin/com/programmersbox/supabaseintegration/credentials/CredentialSignIn.jvm.kt
git commit -m "feat: add no-op CredentialSignIn actuals for iOS and JVM"
```

---

### Task 5: DI wiring

**Files:**
- Modify: `favoritesdatabase/supabase-integration/src/androidMain/kotlin/com/programmersbox/supabaseintegration/di/SupabaseModule.android.kt`
- Modify: `favoritesdatabase/supabase-integration/src/iosMain/kotlin/com/programmersbox/supabaseintegration/di/SupabaseModule.ios.kt`
- Modify: `favoritesdatabase/supabase-integration/src/jvmMain/kotlin/com/programmersbox/supabaseintegration/di/SupabaseModule.jvm.kt`

**Interfaces:**
- Consumes: `CredentialSignIn`, `AndroidCredentialSignIn` (Task 3), `createCredentialSignIn()`
  (Task 4).
- Produces: Koin `single<CredentialSignIn>` binding on all three platforms. Consumed by Task 7
  (`AuthViewModel` constructor injection).

- [ ] **Step 1: Bind on Android**

In `SupabaseModule.android.kt`, add the import:

```kotlin
import com.programmersbox.supabaseintegration.credentials.AndroidCredentialSignIn
import com.programmersbox.supabaseintegration.credentials.CredentialSignIn
```

Add inside the `module { ... }` block, next to the existing `single<CredentialManager>` line:

```kotlin
    single<CredentialSignIn> { AndroidCredentialSignIn(get()) }
```

- [ ] **Step 2: Bind on iOS**

In `SupabaseModule.ios.kt`, add the import:

```kotlin
import com.programmersbox.supabaseintegration.credentials.CredentialSignIn
import com.programmersbox.supabaseintegration.credentials.createCredentialSignIn
```

Add inside the `module { ... }` block:

```kotlin
    single<CredentialSignIn> { createCredentialSignIn() }
```

- [ ] **Step 3: Bind on JVM**

In `SupabaseModule.jvm.kt`, add the import:

```kotlin
import com.programmersbox.supabaseintegration.credentials.CredentialSignIn
import com.programmersbox.supabaseintegration.credentials.createCredentialSignIn
```

Add inside the `module { ... }` block:

```kotlin
    single<CredentialSignIn> { createCredentialSignIn() }
```

- [ ] **Step 4: Verify each platform compiles**

Run: `./gradlew :favoritesdatabase:supabase-integration:compileDebugKotlinAndroid :favoritesdatabase:supabase-integration:compileKotlinIosArm64 :favoritesdatabase:supabase-integration:compileKotlinJvm`
Expected: BUILD SUCCESSFUL for all three.

- [ ] **Step 5: Commit**

```bash
git add favoritesdatabase/supabase-integration/src/androidMain/kotlin/com/programmersbox/supabaseintegration/di/SupabaseModule.android.kt
git add favoritesdatabase/supabase-integration/src/iosMain/kotlin/com/programmersbox/supabaseintegration/di/SupabaseModule.ios.kt
git add favoritesdatabase/supabase-integration/src/jvmMain/kotlin/com/programmersbox/supabaseintegration/di/SupabaseModule.jvm.kt
git commit -m "feat: bind CredentialSignIn in Koin on all platforms"
```

---

### Task 6: `AuthManager` passkey registration + external error reporting

**Files:**
- Modify: `favoritesdatabase/supabase-integration/src/commonMain/kotlin/com/programmersbox/supabaseintegration/auth/AuthManager.kt`
- Modify: `favoritesdatabase/supabase-integration/src/commonMain/kotlin/com/programmersbox/supabaseintegration/auth/AuthManagerImpl.kt`

**Interfaces:**
- Produces: `AuthManager.startPasskeyRegistration(): PasskeyRegistrationResponse`,
  `AuthManager.verifyPasskeyRegistration(challengeId: String, credentialJson: String): PasskeyRegistrationVerifyResponse`,
  `AuthManager.reportError(message: String)`. Consumed by Task 7 (`AuthViewModel`).
- Consumes: `io.github.jan.supabase.auth.passkey.PasskeyRegistrationResponse`,
  `io.github.jan.supabase.auth.passkey.PasskeyRegistrationVerifyResponse`,
  `io.github.jan.supabase.auth.passkeys` extension on the `Auth` plugin (already available via
  the existing `commonLibs.supabase.auth` dependency).

- [ ] **Step 1: Add the three new methods to the interface**

In `AuthManager.kt`, add these imports:

```kotlin
import io.github.jan.supabase.auth.passkey.PasskeyRegistrationResponse
import io.github.jan.supabase.auth.passkey.PasskeyRegistrationVerifyResponse
```

Add to the `interface AuthManager` body, after `suspend fun signInWithEmail(...)`:

```kotlin
    suspend fun startPasskeyRegistration(): PasskeyRegistrationResponse
    suspend fun verifyPasskeyRegistration(challengeId: String, credentialJson: String): PasskeyRegistrationVerifyResponse
    fun reportError(message: String)
```

- [ ] **Step 2: Implement in `AuthManagerImpl`**

Add these imports to `AuthManagerImpl.kt`:

```kotlin
import io.github.jan.supabase.annotations.SupabaseExperimental
import io.github.jan.supabase.auth.passkey.PasskeyRegistrationResponse
import io.github.jan.supabase.auth.passkey.PasskeyRegistrationVerifyResponse
```

Note: `Auth.passkeys` (the property both new methods call) is annotated
`@RequiresOptIn(level = ERROR)` via `@SupabaseExperimental` in supabase-kt — it does not need a
separate import beyond `SupabaseExperimental` itself, since `passkeys` is a member property of the
`Auth` interface (`io.github.jan.supabase.auth.auth` extension already imported in this file gives
access to it), not a top-level extension function.

Add these methods, following the existing `runCatching { ... }.onFailure { ... }` pattern used by
every other method in this class. Both require `@OptIn(SupabaseExperimental::class)` because they
call the experimental `auth.passkeys` API:

```kotlin
    @OptIn(SupabaseExperimental::class)
    override suspend fun startPasskeyRegistration(): PasskeyRegistrationResponse {
        return auth.passkeys.startRegistration()
    }

    @OptIn(SupabaseExperimental::class)
    override suspend fun verifyPasskeyRegistration(
        challengeId: String,
        credentialJson: String,
    ): PasskeyRegistrationVerifyResponse {
        return auth.passkeys.verifyRegistration(challengeId, credentialJson)
    }

    override fun reportError(message: String) {
        _authState.value = AuthState.Error(message)
    }
```

Note: `startPasskeyRegistration`/`verifyPasskeyRegistration` deliberately do **not** wrap in
`runCatching` here — unlike sign-in/sign-up, a passkey registration failure must not overwrite
`_authState` (the user is already authenticated; `AuthState.Error` is reserved for the
unauthenticated sign-in flow). Task 7's `AuthViewModel.registerPasskey()` catches exceptions from
these two calls itself and routes them into the separate `passkeyRegistrationState`, not
`authState`.

- [ ] **Step 3: Verify it compiles**

Run: `./gradlew :favoritesdatabase:supabase-integration:compileKotlinMetadata`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add favoritesdatabase/supabase-integration/src/commonMain/kotlin/com/programmersbox/supabaseintegration/auth/AuthManager.kt
git add favoritesdatabase/supabase-integration/src/commonMain/kotlin/com/programmersbox/supabaseintegration/auth/AuthManagerImpl.kt
git commit -m "feat: add passkey registration and external error reporting to AuthManager"
```

---

### Task 7: `AuthViewModel` wiring

**Files:**
- Modify: `favoritesdatabase/supabase-integration/src/commonMain/kotlin/com/programmersbox/supabaseintegration/ui/viewmodel/AuthViewModel.kt`

**Interfaces:**
- Consumes: `CredentialSignIn`, `CredentialSignInResult`, `PasskeyRegistrationResult` (Task 2),
  `AuthManager.startPasskeyRegistration`/`verifyPasskeyRegistration`/`reportError` (Task 6).
- Produces: `AuthViewModel.signInWithCredentialManager(context: Any?)`,
  `AuthViewModel.registerPasskey(context: Any?)`,
  `AuthViewModel.passkeyRegistrationState: StateFlow<PasskeyRegistrationUiState>`,
  `AuthViewModel.credentialManagerSupported: Boolean`. Consumed by Task 9 (`AuthScreen.kt`).

- [ ] **Step 1: Add imports, constructor param, and UI state type**

Add these imports:

```kotlin
import com.programmersbox.supabaseintegration.credentials.CredentialSignIn
import com.programmersbox.supabaseintegration.credentials.CredentialSignInResult
import com.programmersbox.supabaseintegration.credentials.PasskeyRegistrationResult
import kotlinx.coroutines.flow.MutableStateFlow as MutableStateFlowAlias // not needed if already imported — see note below
```

(Note: `MutableStateFlow`, `StateFlow`, `asStateFlow`, `update` are already imported in this file —
do not duplicate; the alias line above is a reminder, not a real import to add.)

Change the constructor:

```kotlin
class AuthViewModel(
    private val authManager: AuthManager,
    private val credentialManager: CredentialManager,
    private val credentialSignIn: CredentialSignIn,
    private val databaseRepository: DatabaseRepository,
) : ViewModel() {
```

Add this sealed type near the top of the file, alongside `LogoutUiState`:

```kotlin
@Stable
sealed interface PasskeyRegistrationUiState {
    data object Idle : PasskeyRegistrationUiState
    data object Loading : PasskeyRegistrationUiState
    data object Success : PasskeyRegistrationUiState
    data class Error(val message: String) : PasskeyRegistrationUiState
}
```

Add this property, alongside `authState`:

```kotlin
    val credentialManagerSupported: Boolean = credentialSignIn.isSupported

    private val _passkeyRegistrationState = MutableStateFlow<PasskeyRegistrationUiState>(PasskeyRegistrationUiState.Idle)
    val passkeyRegistrationState: StateFlow<PasskeyRegistrationUiState> = _passkeyRegistrationState.asStateFlow()
```

- [ ] **Step 2: Add `signInWithCredentialManager`**

Add this method next to `signInWithEmail`:

```kotlin
    fun signInWithCredentialManager(context: Any?) {
        viewModelScope.launch {
            when (val result = credentialSignIn.signInWithSavedPassword(context)) {
                is CredentialSignInResult.Success -> signInWithEmail(result.email, result.password)
                CredentialSignInResult.Cancelled, CredentialSignInResult.NoCredentials -> Unit
                is CredentialSignInResult.Error -> authManager.reportError(result.message)
            }
        }
    }
```

- [ ] **Step 3: Add `registerPasskey`**

```kotlin
    fun registerPasskey(context: Any?) {
        viewModelScope.launch {
            _passkeyRegistrationState.value = PasskeyRegistrationUiState.Loading
            try {
                val registration = authManager.startPasskeyRegistration()
                when (val result = credentialSignIn.registerPasskey(context, registration.challengeId, registration.options.toString())) {
                    is PasskeyRegistrationResult.Success -> {
                        authManager.verifyPasskeyRegistration(registration.challengeId, result.credentialJson)
                        _passkeyRegistrationState.value = PasskeyRegistrationUiState.Success
                    }
                    PasskeyRegistrationResult.Cancelled -> {
                        _passkeyRegistrationState.value = PasskeyRegistrationUiState.Idle
                    }
                    is PasskeyRegistrationResult.Error -> {
                        _passkeyRegistrationState.value = PasskeyRegistrationUiState.Error(result.message)
                    }
                }
            } catch (e: Exception) {
                _passkeyRegistrationState.value = PasskeyRegistrationUiState.Error(e.message ?: "Passkey registration failed")
            }
        }
    }

    fun dismissPasskeyRegistrationResult() {
        _passkeyRegistrationState.value = PasskeyRegistrationUiState.Idle
    }
```

- [ ] **Step 4: Verify it compiles**

Run: `./gradlew :favoritesdatabase:supabase-integration:compileKotlinMetadata`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add favoritesdatabase/supabase-integration/src/commonMain/kotlin/com/programmersbox/supabaseintegration/ui/viewmodel/AuthViewModel.kt
git commit -m "feat: wire CredentialSignIn and passkey registration into AuthViewModel"
```

---

### Task 8: `CredentialManagerButton` expect/actual composables

**Files:**
- Create: `favoritesdatabase/supabase-integration/src/commonMain/kotlin/com/programmersbox/supabaseintegration/ui/CredentialManagerButton.kt`
- Create: `favoritesdatabase/supabase-integration/src/androidMain/kotlin/com/programmersbox/supabaseintegration/ui/CredentialManagerButton.android.kt`
- Create: `favoritesdatabase/supabase-integration/src/iosMain/kotlin/com/programmersbox/supabaseintegration/ui/CredentialManagerButton.ios.kt`
- Create: `favoritesdatabase/supabase-integration/src/jvmMain/kotlin/com/programmersbox/supabaseintegration/ui/CredentialManagerButton.jvm.kt`

**Interfaces:**
- Produces: `@Composable expect fun CredentialManagerSignInButton(onClick: (context: Any?) -> Unit, enabled: Boolean, modifier: Modifier)`,
  `@Composable expect fun RegisterPasskeyButton(onClick: (context: Any?) -> Unit, enabled: Boolean, modifier: Modifier)`.
  Consumed by Task 9 (`AuthScreen.kt`). `onClick` takes the Activity context so `AuthScreen.kt`
  (commonMain) never needs to reference any platform-context API itself — only the Android actual
  captures `LocalContext.current` (androidx Compose API, valid in `androidMain` only) and passes it
  through; iOS/JVM actuals never invoke `onClick` since the button renders nothing there.

- [ ] **Step 1: Write the commonMain declarations**

```kotlin
package com.programmersbox.supabaseintegration.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun CredentialManagerSignInButton(
    onClick: (context: Any?) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
)

@Composable
expect fun RegisterPasskeyButton(
    onClick: (context: Any?) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
)
```

- [ ] **Step 2: Write the Android actuals**

```kotlin
package com.programmersbox.supabaseintegration.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun CredentialManagerSignInButton(
    onClick: (context: Any?) -> Unit,
    enabled: Boolean,
    modifier: Modifier,
) {
    val context = LocalContext.current
    OutlinedButton(
        onClick = { onClick(context) },
        enabled = enabled,
        modifier = modifier,
    ) {
        Icon(Icons.Default.Fingerprint, contentDescription = null)
        Text("Sign in with Credential Manager")
    }
}

@Composable
actual fun RegisterPasskeyButton(
    onClick: (context: Any?) -> Unit,
    enabled: Boolean,
    modifier: Modifier,
) {
    val context = LocalContext.current
    OutlinedButton(
        onClick = { onClick(context) },
        enabled = enabled,
        modifier = modifier,
    ) {
        Icon(Icons.Default.Key, contentDescription = null)
        Text("Register a Passkey")
    }
}
```

- [ ] **Step 3: Write the iOS no-op actuals**

```kotlin
package com.programmersbox.supabaseintegration.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun CredentialManagerSignInButton(onClick: (context: Any?) -> Unit, enabled: Boolean, modifier: Modifier) {
    // Not supported on iOS — intentionally renders nothing.
}

@Composable
actual fun RegisterPasskeyButton(onClick: (context: Any?) -> Unit, enabled: Boolean, modifier: Modifier) {
    // Not supported on iOS — intentionally renders nothing.
}
```

- [ ] **Step 4: Write the JVM no-op actuals**

```kotlin
package com.programmersbox.supabaseintegration.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun CredentialManagerSignInButton(onClick: (context: Any?) -> Unit, enabled: Boolean, modifier: Modifier) {
    // Not supported on JVM/Desktop — intentionally renders nothing.
}

@Composable
actual fun RegisterPasskeyButton(onClick: (context: Any?) -> Unit, enabled: Boolean, modifier: Modifier) {
    // Not supported on JVM/Desktop — intentionally renders nothing.
}
```

- [ ] **Step 5: Verify all three targets compile**

Run: `./gradlew :favoritesdatabase:supabase-integration:compileDebugKotlinAndroid :favoritesdatabase:supabase-integration:compileKotlinIosArm64 :favoritesdatabase:supabase-integration:compileKotlinJvm`
Expected: BUILD SUCCESSFUL for all three.

- [ ] **Step 6: Commit**

```bash
git add favoritesdatabase/supabase-integration/src/commonMain/kotlin/com/programmersbox/supabaseintegration/ui/CredentialManagerButton.kt
git add favoritesdatabase/supabase-integration/src/androidMain/kotlin/com/programmersbox/supabaseintegration/ui/CredentialManagerButton.android.kt
git add favoritesdatabase/supabase-integration/src/iosMain/kotlin/com/programmersbox/supabaseintegration/ui/CredentialManagerButton.ios.kt
git add favoritesdatabase/supabase-integration/src/jvmMain/kotlin/com/programmersbox/supabaseintegration/ui/CredentialManagerButton.jvm.kt
git commit -m "feat: add Android-only Credential Manager button composables"
```

---

### Task 9: `AuthScreen.kt` UI wiring

**Files:**
- Modify: `favoritesdatabase/supabase-integration/src/commonMain/kotlin/com/programmersbox/supabaseintegration/ui/AuthScreen.kt`

**Interfaces:**
- Consumes: `CredentialManagerSignInButton`, `RegisterPasskeyButton` (Task 8),
  `AuthViewModel.signInWithCredentialManager`, `AuthViewModel.registerPasskey`,
  `AuthViewModel.passkeyRegistrationState`, `AuthViewModel.credentialManagerSupported`,
  `AuthViewModel.dismissPasskeyRegistrationResult` (Task 7).

- [ ] **Step 1: Pass new state/handlers down (no platform-context import needed here)**

`AuthScreen.kt` stays commonMain-only — it never touches a platform context type. The Android
button (Task 8) captures `LocalContext.current` itself and passes it into its `onClick(context)`
callback, so `AuthScreen.kt` just forwards that opaque `context: Any?` straight into the
ViewModel call.

In the `AuthScreen` composable function body, add:

```kotlin
    val passkeyRegistrationState by viewModel.passkeyRegistrationState.collectAsStateWithLifecycle()
```

Update the `UnauthenticatedState` call to pass the new pieces:

```kotlin
                        UnauthenticatedState(
                            state = state,
                            isLoading = isLoading,
                            onSignIn = viewModel::signInWithEmail,
                            onSignUp = viewModel::signUpWithEmail,
                            signInWithMagicLink = viewModel::signInWithMagicLink,
                            credentialManagerSupported = viewModel.credentialManagerSupported,
                            onCredentialManagerSignIn = viewModel::signInWithCredentialManager,
                        )
```

Update the `AuthenticatedState` call to pass the new pieces:

```kotlin
                        AuthenticatedState(
                            state = state,
                            logoutUiState = logoutUiState,
                            onConfirmLogout = viewModel::confirmLogout,
                            onManageDatabasesEnabledChange = viewModel::setManageDatabasesEnabled,
                            onTableActionChange = viewModel::setTableAction,
                            credentialManagerSupported = viewModel.credentialManagerSupported,
                            onRegisterPasskey = viewModel::registerPasskey,
                            passkeyRegistrationState = passkeyRegistrationState,
                            onDismissPasskeyResult = viewModel::dismissPasskeyRegistrationResult,
                        )
```

- [ ] **Step 2: Add the passkey result dialog and button to `AuthenticatedState`**

Add these imports:

```kotlin
import com.programmersbox.supabaseintegration.ui.viewmodel.PasskeyRegistrationUiState
```

Change the `AuthenticatedState` signature to:

```kotlin
private fun AuthenticatedState(
    state: AuthState.Authenticated,
    logoutUiState: LogoutUiState,
    onConfirmLogout: () -> Unit,
    onManageDatabasesEnabledChange: (Boolean) -> Unit,
    onTableActionChange: (ManagedTable, SupportedTableAction) -> Unit,
    credentialManagerSupported: Boolean,
    onRegisterPasskey: (context: Any?) -> Unit,
    passkeyRegistrationState: PasskeyRegistrationUiState,
    onDismissPasskeyResult: () -> Unit,
    modifier: Modifier = Modifier,
) {
```

Add this block right after the existing `if (showLogOutDialog) { ... }` block, still inside
`AuthenticatedState`:

```kotlin
    if (passkeyRegistrationState is PasskeyRegistrationUiState.Success) {
        AlertDialog(
            onDismissRequest = onDismissPasskeyResult,
            title = { Text("Passkey Registered") },
            text = { Text("You can now sign in with this passkey once passkey sign-in ships.") },
            confirmButton = {
                TextButton(onClick = onDismissPasskeyResult) { Text("OK") }
            },
        )
    }

    if (passkeyRegistrationState is PasskeyRegistrationUiState.Error) {
        AlertDialog(
            onDismissRequest = onDismissPasskeyResult,
            title = { Text("Passkey Registration Failed") },
            text = { Text(passkeyRegistrationState.message) },
            confirmButton = {
                TextButton(onClick = onDismissPasskeyResult) { Text("OK") }
            },
        )
    }
```

Add the button right after the existing "Sign Out" `Button` block, still inside the `Column`:

```kotlin
            if (credentialManagerSupported) {
                Spacer(Modifier.height(8.dp))
                RegisterPasskeyButton(
                    onClick = onRegisterPasskey,
                    enabled = passkeyRegistrationState !is PasskeyRegistrationUiState.Loading,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
```

- [ ] **Step 3: Add the button to `UnauthenticatedState`**

Change the `UnauthenticatedState` signature to:

```kotlin
private fun UnauthenticatedState(
    state: AuthState,
    isLoading: Boolean,
    onSignIn: (String, String) -> Unit,
    onSignUp: (String, String) -> Unit,
    signInWithMagicLink: (String) -> Unit,
    credentialManagerSupported: Boolean,
    onCredentialManagerSignIn: (context: Any?) -> Unit,
    modifier: Modifier = Modifier,
) {
```

Add this right after the existing "Send Magic Link" `OutlinedButton` block, still inside the
`Column`:

```kotlin
        if (credentialManagerSupported) {
            Spacer(Modifier.height(16.dp))
            CredentialManagerSignInButton(
                onClick = onCredentialManagerSignIn,
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
            )
        }
```

- [ ] **Step 4: Verify all three targets compile**

Run: `./gradlew :favoritesdatabase:supabase-integration:compileDebugKotlinAndroid :favoritesdatabase:supabase-integration:compileKotlinIosArm64 :favoritesdatabase:supabase-integration:compileKotlinJvm`
Expected: BUILD SUCCESSFUL for all three.

- [ ] **Step 5: Commit**

```bash
git add favoritesdatabase/supabase-integration/src/commonMain/kotlin/com/programmersbox/supabaseintegration/ui/AuthScreen.kt
git commit -m "feat: wire Credential Manager sign-in and passkey registration into AuthScreen"
```

---

### Task 10: End-to-end manual verification

**Files:** none (verification only).

- [ ] **Step 1: Build the Android app**

Run: `./gradlew :mangaworld:assembleNoFirebaseDebug` (or `:animeworld:assembleNoFirebaseDebug` —
whichever app in this checkout has `supabase-integration` wired in; confirm with
`grep -rl supabase-integration */build.gradle.kts` if unsure).
Expected: BUILD SUCCESSFUL.

- [ ] **Step 2: Manual check — email/password flow unchanged**

Install the debug APK, open the sign-in screen, confirm Login/Sign Up tabs, email/password
fields, and "Send Magic Link" all behave exactly as before this plan (no visual or behavioral
change).

- [ ] **Step 3: Manual check — saved password sign-in**

With at least one saved password for the app's package/domain in the device's Credential Manager
(add one via Settings > Passwords, or sign in once via the form with "save password" accepted),
tap "Sign in with Credential Manager", pick the saved password from the sheet, confirm it signs in
and lands on `AuthenticatedState`.

- [ ] **Step 4: Manual check — passkey registration**

While signed in, tap "Register a Passkey", complete the biometric/device-credential prompt,
confirm the "Passkey Registered" dialog appears and dismisses cleanly.

- [ ] **Step 5: Manual check — cancellation is silent**

From the sign-in screen, tap "Sign in with Credential Manager" and dismiss the sheet (back button
or tap outside). Confirm no error message appears and the email/password form is still usable.

- [ ] **Step 6: Manual check — desktop build unaffected**

Run: `./gradlew :mangaworld:desktop:run`
Expected: app launches, no Credential Manager or passkey button visible anywhere on the sign-in or
account screens.

- [ ] **Step 7: Final commit (if any fixups were needed during manual verification)**

```bash
git add -A
git commit -m "fix: address issues found during manual Credential Manager verification"
```

(Skip this step if no fixups were needed.)
