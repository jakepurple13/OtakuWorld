# Android Credential Manager Integration — Design

Status: approved, pending implementation plan.

## Goal

Add Android Credential Manager support (saved passwords + passkeys) to the existing Supabase
sign-in screen in `favoritesdatabase/supabase-integration`, without touching the existing
email/password flow, and gated to Android only.

## Feasibility finding (load-bearing)

supabase-kt (pinned via `commonLibs.supabase.bom`, currently resolves to **3.7.0**, the latest
published release as of 2026-07-20) ships passkey **registration** support
(`auth.passkeys.startRegistration()` / `verifyRegistration()`) but **not** sign-in. The 3.7.0
changelog states explicitly: "there is currently no sign-in method. We are still deciding how to
move on with Native APIs." Checked `supabase-kt` master on GitHub: a `startAuthentication()` /
`verifyAuthentication()` pair now exists in source, but it has not shipped in any published Maven
Central artifact (`auth-kt` `maven-metadata.xml` still lists `3.7.0` as latest/release).

**Decision (user-approved):** scope this feature to what's stable today —

- Saved-password sign-in via Credential Manager: full flow, ships now.
- Passkey **registration** (adding a passkey to an already-authenticated account): full flow,
  ships now, using the stable `auth.passkeys` API.
- Passkey **sign-in** (unauthenticated user picks a passkey to log in): **not built**. No button,
  no UI entry point. Left as a documented extension point for once supabase-kt publishes
  `startAuthentication`/`verifyAuthentication` in a release.

This intentionally does not implement "single request, all options" (passkey + password in one
`GetCredentialRequest`) from the original ask, because there is nothing to verify a passkey
assertion against server-side yet. Requesting a passkey credential the app can't verify would be a
dead-end for the user (biometric prompt, then failure).

## Components

### `favoritesdatabase/supabase-integration` (commonMain)

New file `credentials/CredentialSignIn.kt`:

```kotlin
sealed interface CredentialSignInResult {
    data class Success(val email: String, val password: String) : CredentialSignInResult
    data object NoCredentials : CredentialSignInResult
    data object Cancelled : CredentialSignInResult
    data class Error(val message: String) : CredentialSignInResult
}

sealed interface PasskeyRegistrationResult {
    data class Success(val credentialJson: String) : PasskeyRegistrationResult
    data object Cancelled : PasskeyRegistrationResult
    data class Error(val message: String) : PasskeyRegistrationResult
}

interface CredentialSignIn {
    val isSupported: Boolean
    suspend fun signInWithSavedPassword(): CredentialSignInResult
    suspend fun registerPasskey(challengeId: String, creationOptionsJson: String): PasskeyRegistrationResult
}

expect fun createCredentialSignIn(context: Any?): CredentialSignIn
```

Named `CredentialSignIn`, distinct from the existing `credentials.CredentialManager` interface in
this module (which persists the Supabase project URL/anon key — unrelated to
`androidx.credentials`, do not confuse the two or rename the existing one).

New file `ui/CredentialManagerButton.kt`:

```kotlin
@Composable
expect fun CredentialManagerSignInButton(onClick: () -> Unit, enabled: Boolean, modifier: Modifier)

@Composable
expect fun RegisterPasskeyButton(onClick: () -> Unit, enabled: Boolean, modifier: Modifier)
```

### androidMain

`credentials/CredentialSignIn.android.kt` — real implementation via `androidx.credentials`:

- `signInWithSavedPassword()`:
  `androidx.credentials.CredentialManager.getCredential(context, GetCredentialRequest(listOf(GetPasswordOption())))`.
  Map exceptions: `NoCredentialException` → `NoCredentials`; `GetCredentialCancellationException`
  → `Cancelled`; any other `GetCredentialException` → `Error(message)`.
- `registerPasskey(challengeId, creationOptionsJson)`:
  `createCredential(context, CreatePublicKeyCredentialRequest(requestJson = creationOptionsJson))`.
  The `options: JsonObject` returned by `auth.passkeys.startRegistration()` is standard WebAuthn
  `PublicKeyCredentialCreationOptions` JSON — pass `options.toString()` straight through as
  `requestJson`. Map `CreateCredentialCancellationException` → `Cancelled`; other
  `CreateCredentialException` → `Error(message)`.
- `isSupported = true`.

`ui/CredentialManagerButton.android.kt` — real `OutlinedButton`/`TextButton` composables.

### iosMain / jvmMain

No-op actuals: `isSupported = false`, both methods return `Error("Not supported on this
platform")` (never invoked — the buttons render as empty composables on these platforms, so the
methods are unreachable in practice). This mirrors the existing `CredentialManager` expect/actual
pattern (`CredentialManager.ios.kt`, `.jvm.kt`) already in this module.

### `AuthManager` / `AuthManagerImpl`

Two new methods, thin wrappers over the existing `auth` (Supabase Auth plugin) accessor already
used by every other method in `AuthManagerImpl`:

```kotlin
suspend fun startPasskeyRegistration(): PasskeyRegistrationResponse
suspend fun verifyPasskeyRegistration(challengeId: String, credentialJson: String): PasskeyRegistrationVerifyResponse
```

Implemented as `auth.passkeys.startRegistration()` / `auth.passkeys.verifyRegistration(challengeId,
credential)`, following the same `runCatching { ... }.onFailure { exceptionDao.insertException(it) }`
pattern as `signInWithEmail` etc.

No new method needed for saved-password sign-in — once `CredentialSignIn` resolves an
email/password pair, the ViewModel just calls the existing `signInWithEmail(email, password)`.

### `AuthViewModel`

New constructor dependency: `credentialSignIn: CredentialSignIn` (Koin-injected).

```kotlin
fun signInWithCredentialManager() {
    viewModelScope.launch {
        when (val result = credentialSignIn.signInWithSavedPassword()) {
            is CredentialSignInResult.Success -> signInWithEmail(result.email, result.password)
            CredentialSignInResult.Cancelled, CredentialSignInResult.NoCredentials -> Unit
            is CredentialSignInResult.Error -> authManager.reportError(result.message)
        }
    }
}
```

`Cancelled`/`NoCredentials` are silent no-ops — the user just stays on the email/password form,
per the "graceful fallback" requirement. Only a genuine `Error` surfaces a message. This needs one
small addition to `AuthManager`/`AuthManagerImpl`: a `reportError(message: String)` that just sets
`_authState.value = AuthState.Error(message)`, since today every error path in `AuthManagerImpl`
sets that state internally as a side effect of a failed suspend call — there's no existing public
method to push an error from outside. Alternative considered: expose `_authState` as a
`MutableStateFlow` — rejected, keeps the state private to the manager as it is now.

`registerPasskey()`: calls `authManager.startPasskeyRegistration()` →
`credentialSignIn.registerPasskey(challengeId, options.toString())` → on `Success`,
`authManager.verifyPasskeyRegistration(challengeId, credentialJson)`. Tracked via a small new
`passkeyRegistrationState: StateFlow<PasskeyRegistrationUiState>` (`Idle` / `Loading` / `Success` /
`Error(message)`) — kept separate from `AuthState` since this only runs while already
authenticated.

### `AuthScreen.kt`

- `UnauthenticatedState`: add `CredentialManagerSignInButton` below the existing "OR" divider,
  next to "Send Magic Link". Existing tab switch, email/password fields, sign-in/sign-up button,
  and magic link button are untouched.
- `AuthenticatedState`: add `RegisterPasskeyButton` next to the existing "Sign Out" button, with a
  small dialog (reusing the existing `AlertDialog` pattern already in this composable) to show
  registration success/error.

### DI (`SupabaseModule.kt` / `.android.kt` / `.ios.kt` / `.jvm.kt`)

- Android: `single<CredentialSignIn> { createCredentialSignIn(get()) }` (Context via Koin, same
  pattern as `AndroidCredentialManager(get())`).
- iOS/JVM: `single<CredentialSignIn> { createCredentialSignIn(null) }`.
- No change to the common `supabaseModule()` beyond what `viewModelOf(::AuthViewModel)` already
  auto-resolves once `CredentialSignIn` is bound per platform.

### Gradle

- `gradle/android.versions.toml`: add (1.6.0 confirmed as latest stable on Google Maven as of
  2026-07-24)
  - `androidx-credentials = "androidx.credentials:credentials:1.6.0"`
  - `androidx-credentials-play-services-auth = "androidx.credentials:credentials-play-services-auth:1.6.0"`
- `favoritesdatabase/supabase-integration/build.gradle.kts`, `androidMain.dependencies`: add both,
  alongside the existing `androidLibs.androidx.security.crypto` line.

## Supabase dashboard passkey setup (documentation only — no code)

Added as a comment block on `CredentialSignIn.android.kt` near the registration call site:

1. Dashboard → **Authentication → Passkeys**.
2. Turn on **Enable Passkey authentication**.
3. Set **Relying Party ID** — bare domain, no scheme/port/path (e.g. `example.com`).
4. Set **Relying Party Display Name** — human-readable app name shown in the OS passkey prompt.
5. Set **Relying Party Origins** — comma-separated allowed origins (≤5), HTTPS required except
   `localhost`/`127.0.0.1`/`[::1]`.
6. Note: passkeys are cryptographically bound to the RP ID; changing it invalidates every
   previously registered passkey.
7. Note: passkey **sign-in** (`startAuthentication`/`verifyAuthentication`) is not yet in a
   published supabase-kt release — revisit this integration once it ships.

## Error handling

| Source | Case | Handling |
|---|---|---|
| `androidx.credentials` (get) | `NoCredentialException` | `CredentialSignInResult.NoCredentials` → silent, stay on form |
| `androidx.credentials` (get) | `GetCredentialCancellationException` | `Cancelled` → silent |
| `androidx.credentials` (get) | other `GetCredentialException` | `Error(message)` → `AuthState.Error` |
| `androidx.credentials` (create) | `CreateCredentialCancellationException` | `Cancelled` → silent, no dialog |
| `androidx.credentials` (create) | other `CreateCredentialException` | `Error(message)` → passkey registration dialog |
| supabase-kt passkey API | any exception during start/verify | caught via existing `runCatching` pattern, logged to `exceptionDao`, surfaced same as above |

## Out of scope (unchanged from original ask)

Google Sign-In/federated identity, iOS/Desktop Credential Manager equivalents, custom backend
passkey implementation, account registration beyond passkeys, standalone biometric auth, token
refresh/session logic, unit tests, README updates, and — newly identified — passkey **sign-in**
UI (blocked upstream, see Feasibility finding above).

## Testing

No automated tests requested (out of scope, per original ask). Manual verification: build
`:mangaworld:assembleNoFirebaseDebug` or `:animeworld:assembleNoFirebaseDebug` (whichever app wires
in `supabase-integration`), sign in with a saved password via Credential Manager, register a
passkey while signed in, confirm the email/password form still works unchanged, confirm both new
buttons are absent on the JVM desktop build.
