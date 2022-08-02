package com.programmersbox.sharedutils

import android.app.Activity
import android.content.Context
import android.net.Uri
import androidx.activity.ComponentActivity
import com.programmersbox.favoritesdatabase.ChapterWatched
import com.programmersbox.favoritesdatabase.DbModel
import io.reactivex.Completable
import io.reactivex.Flowable
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import org.koin.core.component.KoinComponent

object FirebaseAuthentication : KoinComponent {

    fun signIn(activity: Activity) {
        //val signInIntent = googleSignInClient!!.signInIntent
        //activity.startActivityForResult(signInIntent, RC_SIGN_IN)
        // Choose authentication providers

    }

    fun signOut() {
        //auth.signOut()
        //currentUser = null
    }

    fun addAuthStateListener(update: (CustomFirebaseUser?) -> Unit) = Unit
    fun clear() = Unit
    fun signInOrOut(context: Context, activity: ComponentActivity, title: Int, message: Int, positive: Int, no: Int) = Unit

}

data class CustomFirebaseUser(val displayName: String?, val photoUrl: Uri?)

object FirebaseDb {

    var DOCUMENT_ID = ""
    var CHAPTERS_ID = ""
    var COLLECTION_ID = ""
    var ITEM_ID = ""
    var READ_OR_WATCHED_ID = ""

    private data class FirebaseAllShows(val first: String = DOCUMENT_ID, val second: List<FirebaseDbModel> = emptyList())

    private data class FirebaseDbModel(
        val title: String? = null,
        val description: String? = null,
        val showUrl: String? = null,
        val mangaUrl: String? = null,
        val imageUrl: String? = null,
        val source: String? = null,
        var numEpisodes: Int? = null,
        var chapterCount: Int? = null
    )

    private data class FirebaseChapterWatched(
        val url: String? = null,
        val name: String? = null,
        val showUrl: String? = null,
    )

    private fun FirebaseDbModel.toDbModel() = DbModel(
        title.orEmpty(),
        description.orEmpty(),
        (showUrl ?: mangaUrl).orEmpty(),
        imageUrl.orEmpty(),
        source.orEmpty(),
        numEpisodes ?: chapterCount ?: 0,
    )

    private fun DbModel.toFirebaseDbModel() = FirebaseDbModel(
        title,
        description,
        url,
        url,
        imageUrl,
        source,
        numChapters,
        numChapters
    )

    private fun FirebaseChapterWatched.toChapterWatchedModel() = ChapterWatched(
        url.orEmpty().pathToUrl(),
        name.orEmpty(),
        showUrl.orEmpty().pathToUrl(),
    )

    private fun ChapterWatched.toFirebaseChapterWatched() = FirebaseChapterWatched(
        url,
        name,
        this.favoriteUrl,
    )

    private fun String.urlToPath() = replace("/", "<")
    private fun String.pathToUrl() = replace("<", "/")

    fun getAllShows() = emptyList<DbModel>()

    fun insertShow(showDbModel: DbModel) = Completable.complete()

    fun insertShowFlow(showDbModel: DbModel) = flowOf(Unit)

    fun removeShow(showDbModel: DbModel) = Completable.complete()

    fun removeShowFlow(showDbModel: DbModel) = flowOf(Unit)

    fun updateShow(showDbModel: DbModel) = Completable.complete()

    fun updateShowFlow(showDbModel: DbModel) = flowOf(Unit)

    fun insertEpisodeWatched(episodeWatched: ChapterWatched) = Completable.complete()

    fun insertEpisodeWatchedFlow(episodeWatched: ChapterWatched) = flowOf(Unit)

    fun removeEpisodeWatched(episodeWatched: ChapterWatched) = Completable.complete()

    fun removeEpisodeWatchedFlow(episodeWatched: ChapterWatched) = flowOf(Unit)

    class FirebaseListener {

        fun getAllShowsFlowable() = Flowable.just<List<DbModel>>(emptyList())

        @ExperimentalCoroutinesApi
        fun getAllShowsFlow() = flowOf(emptyList<DbModel>())

        fun findItemByUrl(url: String?) = Flowable.just(false)

        fun findItemByUrlFlow(url: String?) = flowOf(false)

        fun getAllEpisodesByShow(showUrl: String) = Flowable.just<List<ChapterWatched>>(emptyList())

        fun getAllEpisodesByShowFlow(showUrl: String) = flowOf(emptyList<ChapterWatched>())

        //fun getAllEpisodesByShow(showDbModel: DbModel) = getAllEpisodesByShow(showDbModel.showUrl)

        fun unregister() {}

    }

    private class Watched(val watched: List<FirebaseChapterWatched> = emptyList())
}