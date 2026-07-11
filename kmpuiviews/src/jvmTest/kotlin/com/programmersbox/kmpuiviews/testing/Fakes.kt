package com.programmersbox.kmpuiviews.testing

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.programmersbox.favoritesdatabase.ChapterWatched
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.favoritesdatabase.ExceptionDao
import com.programmersbox.favoritesdatabase.ExceptionItem
import com.programmersbox.favoritesdatabase.ItemDatabase
import com.programmersbox.kmpmodels.KmpChapterModel
import com.programmersbox.kmpmodels.KmpInfoModel
import com.programmersbox.kmpmodels.KmpItemModel
import com.programmersbox.kmpuiviews.KmpGenericInfo
import com.programmersbox.kmpuiviews.domain.AppUpdate
import com.programmersbox.kmpuiviews.domain.KmpCustomRemoteModel
import com.programmersbox.kmpuiviews.domain.TranslationHandler
import com.programmersbox.kmpuiviews.domain.TranslationModelHandler
import com.programmersbox.kmpuiviews.presentation.navactions.NavigationActions
import com.programmersbox.kmpuiviews.presentation.settings.workerinfo.WorkerInfoModel
import com.programmersbox.kmpuiviews.repository.BackgroundWorkHandler
import com.programmersbox.kmpuiviews.repository.DownloadAndInstallState
import com.programmersbox.kmpuiviews.repository.DownloadStateInterface
import com.programmersbox.kmpuiviews.repository.WorkInfoKmp
import com.programmersbox.kmpuiviews.utils.ComponentState
import com.programmersbox.kmpuiviews.utils.DownloadAndInstallStatus
import com.programmersbox.kmpuiviews.utils.KmpFirebaseConnection
import com.programmersbox.datastore.DataStoreHandling
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.datastore.SettingsSerializer
import com.programmersbox.datastore.createProtobuf
import com.programmersbox.datastore.otakuDataStore
import com.programmersbox.sharedcomponents.backup.ItemResult
import com.programmersbox.supabaseintegration.auth.AuthManager
import com.programmersbox.supabaseintegration.auth.AuthState
import io.github.jan.supabase.auth.providers.OAuthProvider
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import okio.Path.Companion.toPath
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

fun createTestDataStoreHandling(): DataStoreHandling {
    val file = File.createTempFile("datastore-test", ".preferences_pb").also {
        it.delete()
        it.deleteOnExit()
    }
    otakuDataStore = PreferenceDataStoreFactory.createWithPath(
        produceFile = { file.absolutePath.toPath() }
    )
    return DataStoreHandling()
}

fun createTestNewSettingsHandling(): NewSettingsHandling {
    val file = File.createTempFile("settings-test", ".preferences_pb").also {
        it.delete()
        it.deleteOnExit()
    }
    return NewSettingsHandling(
        createProtobuf(
            serializer = SettingsSerializer(),
            fileName = file.absolutePath,
        )
    )
}

class FakeKmpGenericInfo : KmpGenericInfo {
    override val apkString: AppUpdate.AppUpdates.() -> String? = { null }

    override suspend fun chapterOnClick(
        model: KmpChapterModel,
        allChapters: List<KmpChapterModel>,
        infoModel: KmpInfoModel,
        navController: NavigationActions,
    ) {
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
    }

    @Composable
    override fun ProfileIcon(): String = ""
}

class FakeBackgroundWorkHandler : BackgroundWorkHandler {
    override fun localToCloudListener(): Flow<List<WorkInfoKmp>> = flowOf(emptyList())
    override fun cloudToLocalListener(): Flow<List<WorkInfoKmp>> = flowOf(emptyList())
    override fun syncLocalToCloud() {}
    override fun syncCloudToLocal() {}
    override fun setupPeriodicCheckers() {}
    override fun workerInfoFlow(): Flow<List<WorkerInfoModel>> = flowOf(emptyList())
    override fun sourceUpdate() {}
    override fun cancel(uuid: String) {}
    override fun startBackup(file: PlatformFile, selectedKeys: Set<String>) {}
    override fun startRestore(file: PlatformFile, selectedKeys: Set<String>) {}
    override fun backupResultsFlow(): Flow<List<ItemResult>> = flowOf(emptyList())
    override fun restoreResultsFlow(): Flow<List<ItemResult>> = flowOf(emptyList())
}

class FakeTranslationModelHandler : TranslationModelHandler {
    override fun getModels(onSuccess: (List<KmpCustomRemoteModel>) -> Unit) {
        onSuccess(emptyList())
    }

    override suspend fun deleteModel(model: KmpCustomRemoteModel) {}
    override suspend fun modelList(): List<KmpCustomRemoteModel> = emptyList()
    override suspend fun delete(model: KmpCustomRemoteModel) {}
}

class FakeTranslationHandler : TranslationHandler {
    override fun translateDescription(
        textToTranslate: String,
        progress: (Boolean) -> Unit,
        translatedText: (String) -> Unit,
    ) {
        translatedText(textToTranslate)
    }

    override suspend fun translate(textToTranslate: String): String = textToTranslate
    override fun clear() {}
}

class FakeDownloadStateInterface : DownloadStateInterface {
    override val downloadList: Flow<List<DownloadAndInstallState>> = flowOf(emptyList())
    override fun cancelDownload(id: String) {}
    override fun install(url: String): Flow<DownloadAndInstallStatus> = emptyFlow()
    override fun downloadAndInstall(url: String) {}
    override fun downloadThenInstall(url: String) {}
}

class FakeExceptionDao : ExceptionDao {
    val insertedExceptions = mutableListOf<ExceptionItem>()
    override fun getAllExceptions(): Flow<List<ExceptionItem>> = flowOf(insertedExceptions.toList())
    override fun getExceptionCount(): Flow<Int> = flowOf(insertedExceptions.size)
    override suspend fun insertException(model: ExceptionItem) {
        insertedExceptions += model
    }

    override suspend fun deleteException(model: ExceptionItem) {
        insertedExceptions -= model
    }

    override suspend fun deleteAll() {
        insertedExceptions.clear()
    }
}
