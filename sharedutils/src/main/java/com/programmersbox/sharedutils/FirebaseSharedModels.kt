package com.programmersbox.sharedutils

import android.net.Uri
import com.programmersbox.favoritesdatabase.ChapterWatched
import com.programmersbox.favoritesdatabase.DbModel
import kotlinx.coroutines.flow.Flow

data class CustomFirebaseUser(val displayName: String?, val photoUrl: Uri?)

object FirebaseDb : FirebaseConnection by FirebaseDbImpl {
    var DOCUMENT_ID = ""
    var CHAPTERS_ID = ""
    var COLLECTION_ID = ""
    var ITEM_ID = ""
    var READ_OR_WATCHED_ID = ""
    var SHOW_CHECK_FOR_UPDATE_ID = "shouldCheckForUpdate"

    class FirebaseListener : FirebaseConnection.FirebaseListener by FirebaseDbImpl.FirebaseListener()
}

internal class Watched(val watched: List<FirebaseChapterWatched> = emptyList())

internal data class FirebaseAllShows(val first: String = FirebaseDb.DOCUMENT_ID, val second: List<FirebaseDbModel> = emptyList())

internal data class FirebaseDbModel(
    val title: String? = null,
    val description: String? = null,
    val showUrl: String? = null,
    val mangaUrl: String? = null,
    val imageUrl: String? = null,
    val source: String? = null,
    var numEpisodes: Int? = null,
    var chapterCount: Int? = null,
    var shouldCheckForUpdate: Boolean? = null,
)

internal data class FirebaseChapterWatched(
    val url: String? = null,
    val name: String? = null,
    val showUrl: String? = null,
)

internal fun FirebaseDbModel.toDbModel() = DbModel(
    title.orEmpty(),
    description.orEmpty(),
    (showUrl ?: mangaUrl).orEmpty(),
    imageUrl.orEmpty(),
    source.orEmpty(),
    numEpisodes ?: chapterCount ?: 0,
    shouldCheckForUpdate ?: true
)

internal fun DbModel.toFirebaseDbModel() = FirebaseDbModel(
    title,
    description,
    url,
    url,
    imageUrl,
    source,
    numChapters,
    numChapters,
    shouldCheckForUpdate
)

internal fun FirebaseChapterWatched.toChapterWatchedModel() = ChapterWatched(
    url.orEmpty().pathToUrl(),
    name.orEmpty(),
    showUrl.orEmpty().pathToUrl(),
)

internal fun ChapterWatched.toFirebaseChapterWatched() = FirebaseChapterWatched(
    url,
    name,
    this.favoriteUrl,
)

internal fun String.urlToPath() = replace("/", "<")
internal fun String.pathToUrl() = replace("<", "/")

//TODO: Find out all firebase uses and abstract them out.
// The android applications themselves will hold that information and pass them out to kmpuiviews.
// With a firebaseShared module that will house them.

interface FirebaseConnection {
    fun getAllShows(): List<DbModel>
    fun insertShowFlow(showDbModel: DbModel): Flow<Unit>
    fun removeShowFlow(showDbModel: DbModel): Flow<Unit>
    fun updateShowFlow(showDbModel: DbModel): Flow<Unit>
    fun toggleUpdateCheckShowFlow(showDbModel: DbModel): Flow<Unit>
    fun insertEpisodeWatchedFlow(episodeWatched: ChapterWatched): Flow<Unit>
    fun removeEpisodeWatchedFlow(episodeWatched: ChapterWatched): Flow<Unit>

    interface FirebaseListener {
        fun getAllShowsFlow(): Flow<List<DbModel>>
        fun getShowFlow(url: String?): Flow<DbModel?>
        fun findItemByUrlFlow(url: String?): Flow<Boolean>
        fun getAllEpisodesByShowFlow(showUrl: String): Flow<List<ChapterWatched>>
        fun unregister()
    }
}