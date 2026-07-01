package com.programmersbox.kmpuiviews.testing

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.programmersbox.favoritesdatabase.ChapterWatched
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.favoritesdatabase.ItemDatabase
import com.programmersbox.kmpuiviews.utils.KmpFirebaseConnection
import com.programmersbox.supabaseintegration.auth.AuthManager
import com.programmersbox.supabaseintegration.auth.AuthState
import io.github.jan.supabase.auth.providers.OAuthProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import java.io.File

fun createTestItemDatabase(): ItemDatabase {
    val dbFile = File.createTempFile("kmpuiviews-test", ".db").also { it.deleteOnExit() }
    return Room.databaseBuilder<ItemDatabase>(name = dbFile.absolutePath)
        .setDriver(BundledSQLiteDriver())
        .build()
}

class FakeKmpFirebaseConnection(
    private val shows: List<DbModel> = emptyList(),
) : KmpFirebaseConnection {
    override fun getAllShows(): List<DbModel> = shows
    override fun insertShowFlow(showDbModel: DbModel): Flow<Unit> = flowOf(Unit)
    override fun removeShowFlow(showDbModel: DbModel): Flow<Unit> = flowOf(Unit)
    override fun updateShowFlow(showDbModel: DbModel): Flow<Unit> = flowOf(Unit)
    override fun toggleUpdateCheckShowFlow(showDbModel: DbModel): Flow<Unit> = flowOf(Unit)
    override fun insertEpisodeWatchedFlow(episodeWatched: ChapterWatched): Flow<Unit> = flowOf(Unit)
    override fun removeEpisodeWatchedFlow(episodeWatched: ChapterWatched): Flow<Unit> = flowOf(Unit)
}

class FakeKmpFirebaseListener(
    private val showsFlow: MutableStateFlow<List<DbModel>> = MutableStateFlow(emptyList()),
) : KmpFirebaseConnection.KmpFirebaseListener {
    override fun getAllShowsFlow(): Flow<List<DbModel>> = showsFlow
    override fun getShowFlow(url: String?): Flow<DbModel?> = flowOf(showsFlow.value.find { it.url == url })
    override fun findItemByUrlFlow(url: String?): Flow<Boolean> = flowOf(showsFlow.value.any { it.url == url })
    override fun getAllEpisodesByShowFlow(showUrl: String): Flow<List<ChapterWatched>> = flowOf(emptyList())
    override fun unregister() {}
}

class FakeAuthManager(
    private val loggedIn: Boolean = false,
) : AuthManager {
    override val authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    override fun isLoggedIn(): Boolean = loggedIn
    override suspend fun signInWithEmail(email: String, password: String) {}
    override suspend fun signUpWithEmail(email: String, password: String) {}
    override suspend fun signInWithOAuth(provider: OAuthProvider) {}
    override suspend fun signInWithMagicLink(email: String) {}
    override suspend fun signInWithPhone(phone: String, otp: String) {}
    override suspend fun signInAnonymously() {}
    override suspend fun signOut() {}
    override suspend fun deleteAccount() {}
    override suspend fun refreshSession() {}
}
