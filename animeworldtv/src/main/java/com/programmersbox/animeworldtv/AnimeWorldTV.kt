package com.programmersbox.animeworldtv

import android.app.Application
import com.programmersbox.loggingutils.Loged
import io.reactivex.plugins.RxJavaPlugins
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class AnimeWorldTV : Application() {

    override fun onCreate() {
        super.onCreate()

        Loged.FILTER_BY_PACKAGE_NAME = "programmersbox"
        Loged.TAG = this::class.java.simpleName

        FirebaseDb.DOCUMENT_ID = "favoriteShows"
        FirebaseDb.CHAPTERS_ID = "episodesWatched"
        FirebaseDb.COLLECTION_ID = "animeworld"
        FirebaseDb.ITEM_ID = "showUrl"
        FirebaseDb.READ_OR_WATCHED_ID = "numEpisodes"

        RxJavaPlugins.setErrorHandler {
            it.printStackTrace()
            //FirebaseCrashlytics.getInstance().recordException(it)
        }

        startKoin {
            androidLogger()
            androidContext(this@AnimeWorldTV)
            /*module {
                single { MainLogo(R.mipmap.ic_launcher) }
                single { NotificationLogo(R.mipmap.ic_launcher_foreground) }
            }*/
        }
    }

}