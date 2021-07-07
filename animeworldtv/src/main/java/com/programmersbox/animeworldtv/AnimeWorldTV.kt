package com.programmersbox.animeworldtv

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class AnimeWorldTV : Application() {

    override fun onCreate() {
        super.onCreate()

        FirebaseDb.DOCUMENT_ID = "favoriteShows"
        FirebaseDb.CHAPTERS_ID = "episodesWatched"
        FirebaseDb.COLLECTION_ID = "animeworld"
        FirebaseDb.ITEM_ID = "showUrl"
        FirebaseDb.READ_OR_WATCHED_ID = "numEpisodes"

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