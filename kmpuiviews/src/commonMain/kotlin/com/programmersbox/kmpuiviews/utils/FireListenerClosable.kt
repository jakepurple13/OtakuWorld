package com.programmersbox.kmpuiviews.utils

import androidx.lifecycle.ViewModel
import com.programmersbox.favoritesdatabase.ChapterWatched
import com.programmersbox.favoritesdatabase.DbModel
import kotlinx.coroutines.flow.Flow

class FireListenerClosable(
    private val fireListener: KmpFirebaseConnection.KmpFirebaseListener,
) : AutoCloseable, KmpFirebaseConnection.KmpFirebaseListener by fireListener {
    override fun close() {
        unregister()
    }
}

fun ViewModel.fireListener(
    key: String = "default",
    itemListener: KmpFirebaseConnection.KmpFirebaseListener,
): FireListenerClosable {
    val fireListener = FireListenerClosable(itemListener)
    addCloseable("firebase_listener_$key", fireListener)
    return fireListener
}

interface KmpFirebaseConnection {
    fun getAllShows(): List<DbModel>
    fun insertShowFlow(showDbModel: DbModel): Flow<Unit>
    fun removeShowFlow(showDbModel: DbModel): Flow<Unit>
    fun updateShowFlow(showDbModel: DbModel): Flow<Unit>
    fun toggleUpdateCheckShowFlow(showDbModel: DbModel): Flow<Unit>
    fun insertEpisodeWatchedFlow(episodeWatched: ChapterWatched): Flow<Unit>
    fun removeEpisodeWatchedFlow(episodeWatched: ChapterWatched): Flow<Unit>

    interface KmpFirebaseListener {
        fun getAllShowsFlow(): Flow<List<DbModel>>
        fun getShowFlow(url: String?): Flow<DbModel?>
        fun findItemByUrlFlow(url: String?): Flow<Boolean>
        fun getAllEpisodesByShowFlow(showUrl: String): Flow<List<ChapterWatched>>
        fun unregister()
    }
}