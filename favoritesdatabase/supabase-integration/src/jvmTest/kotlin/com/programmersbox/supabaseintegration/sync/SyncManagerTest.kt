package com.programmersbox.supabaseintegration.sync

import com.programmersbox.supabaseintegration.auth.AuthManager
import com.programmersbox.supabaseintegration.auth.AuthState
import com.programmersbox.supabaseintegration.auth.SupabaseUser
import dev.jordond.connectivity.Connectivity
import io.github.jan.supabase.auth.providers.OAuthProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SyncManagerTest {

    private val engine = FakeSyncEngine()
    private val manager = SyncManager(
        syncEngine = engine,
        authManager = FakeAuthManager(
            AuthState.Authenticated(SupabaseUser(id = "user", email = null, phone = null, displayName = null))
        ),
        connectivityMonitor = object : ConnectivityMonitor {
            override val isOnline: StateFlow<Connectivity.Status> =
                MutableStateFlow(Connectivity.Status.Connected(metered = false))
        },
        fullSyncHandler = FullSyncHandler(),
    )

    @AfterTest
    fun tearDown() = manager.stop()

    // Crashlytics b444464d058bf6dfbe183b1b71be7721: start() runs from a LaunchedEffect, so it is called again
    // whenever the activity is recreated. Each call added another collector, and the collectors raced to open
    // realtime subscriptions on the same channel topic.
    @Test
    fun startCalledTwiceOpensOneRealtimeSubscription() = runBlocking {
        manager.start()
        manager.start()

        withTimeout(5_000) { while (engine.realtimeSubscriptions.get() == 0) delay(10) }
        delay(500)

        assertEquals(1, engine.realtimeSubscriptions.get())
    }
}

private class FakeSyncEngine : SyncEngine {
    val realtimeSubscriptions = AtomicInteger()

    override suspend fun pushLocalChanges() = Unit
    override suspend fun pullRemoteChanges(since: Long, tables: Set<String>?) = Unit
    override suspend fun fullSync() = Unit
    override fun observeLocalChanges(): Flow<Unit> = emptyFlow()
    override fun subscribeRealtime(scope: CoroutineScope, onEvent: suspend (Set<String>) -> Unit): Job =
        scope.launch {
            realtimeSubscriptions.incrementAndGet()
            awaitCancellation()
        }
}

private class FakeAuthManager(state: AuthState) : AuthManager {
    override val authState: StateFlow<AuthState> = MutableStateFlow(state)
    override fun isLoggedIn() = true
    override suspend fun signInWithEmail(email: String, password: String, context: Any?) = Unit
    override suspend fun signUpWithEmail(email: String, password: String, context: Any?) = Unit
    override suspend fun signInWithOAuth(provider: OAuthProvider) = Unit
    override suspend fun signInWithMagicLink(email: String) = Unit
    override suspend fun signInWithPhone(phone: String, otp: String) = Unit
    override suspend fun signInAnonymously() = Unit
    override suspend fun signOut() = Unit
    override suspend fun deleteAccount() = Unit
    override suspend fun refreshSession() = Unit
    override fun reportError(message: String) = Unit
}
