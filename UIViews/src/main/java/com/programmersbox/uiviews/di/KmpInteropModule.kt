package com.programmersbox.uiviews.di

import android.content.Context
import com.programmersbox.favoritesdatabase.ChapterWatched
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.kmpuiviews.AboutLibraryBuilder
import com.programmersbox.kmpuiviews.BuildType
import com.programmersbox.kmpuiviews.domain.KmpCustomRemoteModel
import com.programmersbox.kmpuiviews.domain.TranslationHandler
import com.programmersbox.kmpuiviews.domain.TranslationModelHandler
import com.programmersbox.kmpuiviews.utils.AppConfig
import com.programmersbox.kmpuiviews.utils.KmpFirebaseConnection
import com.programmersbox.sharedutils.CustomRemoteModel
import com.programmersbox.sharedutils.FirebaseConnection
import com.programmersbox.sharedutils.FirebaseDb
import com.programmersbox.sharedutils.TranslateItems
import com.programmersbox.sharedutils.TranslatorUtils
import com.programmersbox.uiviews.BuildConfig
import com.programmersbox.uiviews.R
import kotlinx.coroutines.flow.Flow
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val kmpInterop = module {
    singleOf<KmpFirebaseConnection>(::KmpFirebaseConnectionImpl)
    factory<KmpFirebaseConnection.KmpFirebaseListener> { KmpFirebaseConnectionImpl.KmpFirebaseListenerImpl() }
    singleOf(::AboutLibraryBuilder)

    single {
        AppConfig(
            appName = get<Context>().getString(R.string.app_name),
            buildType = when (BuildConfig.FLAVOR) {
                "noFirebase" -> BuildType.NoFirebase
                "noCloudFirebase" -> BuildType.NoCloudFirebase
                else -> BuildType.Full
            }
        )
    }

    factory<TranslationHandler> { TranslationItemHandler() }
    factory<TranslationModelHandler> { TranslationModelHandlerImpl() }
}

class TranslationModelHandlerImpl : TranslationModelHandler {
    override fun getModels(onSuccess: (List<KmpCustomRemoteModel>) -> Unit) = TranslatorUtils.getModels {
        onSuccess(it.map(::toRemoteModel))
    }

    override suspend fun deleteModel(model: KmpCustomRemoteModel) = TranslatorUtils
        .deleteModel(toRemoteModel(model))

    override suspend fun modelList(): List<KmpCustomRemoteModel> = TranslatorUtils
        .modelList()
        .map(::toRemoteModel)

    override suspend fun delete(model: KmpCustomRemoteModel) = TranslatorUtils
        .delete(toRemoteModel(model))

    private fun toRemoteModel(kmpCustomRemoteModel: KmpCustomRemoteModel) = CustomRemoteModel(
        hash = kmpCustomRemoteModel.hash,
        language = kmpCustomRemoteModel.language
    )

    private fun toRemoteModel(customRemoteModel: CustomRemoteModel) = KmpCustomRemoteModel(
        hash = customRemoteModel.hash.orEmpty(),
        language = customRemoteModel.language
    )
}


class TranslationItemHandler(
    private val translateItems: TranslateItems = TranslateItems(),
) : TranslationHandler {
    override fun translateDescription(
        textToTranslate: String,
        progress: (Boolean) -> Unit,
        translatedText: (String) -> Unit,
    ) = translateItems.translateDescription(textToTranslate, progress, translatedText)

    override suspend fun translate(textToTranslate: String): String = translateItems.translate(textToTranslate)
    override fun clear() = translateItems.clear()
}

class KmpFirebaseConnectionImpl : KmpFirebaseConnection {
    override fun getAllShows(): List<DbModel> = FirebaseDb.getAllShows()
    override fun insertShowFlow(showDbModel: DbModel): Flow<Unit> = FirebaseDb.insertShowFlow(showDbModel)
    override fun removeShowFlow(showDbModel: DbModel): Flow<Unit> = FirebaseDb.removeShowFlow(showDbModel)
    override fun updateShowFlow(showDbModel: DbModel): Flow<Unit> = FirebaseDb.updateShowFlow(showDbModel)
    override fun toggleUpdateCheckShowFlow(showDbModel: DbModel): Flow<Unit> =
        FirebaseDb.toggleUpdateCheckShowFlow(showDbModel)

    override fun insertEpisodeWatchedFlow(episodeWatched: ChapterWatched): Flow<Unit> =
        FirebaseDb.insertEpisodeWatchedFlow(episodeWatched)

    override fun removeEpisodeWatchedFlow(episodeWatched: ChapterWatched): Flow<Unit> =
        FirebaseDb.removeEpisodeWatchedFlow(episodeWatched)

    class KmpFirebaseListenerImpl(
        private val firebaseListener: FirebaseConnection.FirebaseListener = FirebaseDb.FirebaseListener(),
    ) : KmpFirebaseConnection.KmpFirebaseListener {
        override fun getAllShowsFlow(): Flow<List<DbModel>> = firebaseListener.getAllShowsFlow()
        override fun getShowFlow(url: String?): Flow<DbModel?> = firebaseListener.getShowFlow(url)
        override fun findItemByUrlFlow(url: String?): Flow<Boolean> = firebaseListener.findItemByUrlFlow(url)
        override fun getAllEpisodesByShowFlow(showUrl: String): Flow<List<ChapterWatched>> =
            firebaseListener.getAllEpisodesByShowFlow(showUrl)

        override fun unregister() = firebaseListener.unregister()
    }
}