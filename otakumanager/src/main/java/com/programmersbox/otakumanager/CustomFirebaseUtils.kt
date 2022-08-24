package com.programmersbox.otakumanager

import android.annotation.SuppressLint
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.toObjects
import com.programmersbox.favoritesdatabase.ChapterWatched
import com.programmersbox.favoritesdatabase.DbModel

data class FirebaseDb2(
    val DOCUMENT_ID: String,
    val CHAPTERS_ID: String,
    val COLLECTION_ID: String,
    val ITEM_ID: String,
    val READ_OR_WATCHED_ID: String
) {

    @SuppressLint("StaticFieldLeak")
    private val db = FirebaseFirestore.getInstance().apply {
        firestoreSettings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            /*.setHost("10.0.2.2:8080")
            .setSslEnabled(false)
            .setPersistenceEnabled(false)*/
            //.setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
            //.setCacheSizeBytes()
            .build()
    }

    private fun <TResult> Task<TResult>.await(): TResult = Tasks.await(this)

    private val showDoc2 get() = db.collection(COLLECTION_ID).document(DOCUMENT_ID).collection("it.uid")
    private val episodeDoc2 get() = db.collection(COLLECTION_ID).document(CHAPTERS_ID).collection("it.uid")

    private inner class FirebaseAllShows(val first: String = DOCUMENT_ID, val second: List<FirebaseDbModel> = emptyList())

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

    fun getAllShows() = showDoc2
        ?.get()
        ?.await()
        ?.toObjects<FirebaseDbModel>()
        ?.map { it.toDbModel() }
        .orEmpty()

    fun insertShow(showDbModel: DbModel) = Completable.create { emitter ->
        showDoc2?.document(showDbModel.url.urlToPath())
            ?.set(showDbModel.toFirebaseDbModel())
            ?.addOnSuccessListener { emitter.onComplete() }
            ?.addOnFailureListener { emitter.onError(it) } ?: emitter.onComplete()
    }

    fun removeShow(showDbModel: DbModel) = Completable.create { emitter ->
        showDoc2?.document(showDbModel.url.urlToPath())
            ?.delete()
            ?.addOnSuccessListener { emitter.onComplete() }
            ?.addOnFailureListener { emitter.onError(it) } ?: emitter.onComplete()
    }

    fun updateShow(showDbModel: DbModel) = Completable.create { emitter ->
        showDoc2?.document(showDbModel.url.urlToPath())
            ?.update(READ_OR_WATCHED_ID, showDbModel.numChapters)
            ?.addOnSuccessListener { emitter.onComplete() }
            ?.addOnFailureListener { emitter.onError(it) } ?: emitter.onComplete()
    }

    fun insertEpisodeWatched(episodeWatched: ChapterWatched) = Completable.create { emitter ->
        episodeDoc2?.document(episodeWatched.favoriteUrl.urlToPath())
            ?.set("create" to 1)
            ?.addOnSuccessListener {
                episodeDoc2?.document(episodeWatched.favoriteUrl.urlToPath())
                    //?.set("watched" to listOf(episodeWatched.toFirebaseEpisodeWatched()), SetOptions.merge())
                    ?.update("watched", FieldValue.arrayUnion(episodeWatched.toFirebaseChapterWatched()))
                    //?.collection(episodeWatched.url.urlToPath())
                    //?.document("watched")
                    //?.set(episodeWatched.toFirebaseEpisodeWatched())
                    ?.addOnSuccessListener { emitter.onComplete() }
                    ?.addOnFailureListener { emitter.onError(it) } ?: emitter.onComplete()
            } ?: emitter.onComplete()
    }

    fun removeEpisodeWatched(episodeWatched: ChapterWatched) = Completable.create { emitter ->
        episodeDoc2?.document(episodeWatched.favoriteUrl.urlToPath())
            ?.update("watched", FieldValue.arrayRemove(episodeWatched.toFirebaseChapterWatched()))
            //?.collection(episodeWatched.url.urlToPath())
            //?.document("watched")
            //?.delete()
            ?.addOnSuccessListener { emitter.onComplete() }
            ?.addOnFailureListener { emitter.onError(it) } ?: emitter.onComplete()
        emitter.onComplete()
    }

    inner class FirebaseListener {

        private var listener: ListenerRegistration? = null

        fun getAllShowsFlowable() = PublishSubject.create<List<DbModel>> { emitter ->
            //assert(listener == null)
            listener?.remove()
            listener = showDoc2?.addSnapshotListener { value, error ->
                value?.toObjects<FirebaseDbModel>()
                    ?.map { it.toDbModel() }
                    ?.let(emitter::onNext)
                error?.let(emitter::onError)
            }
            emitter.setCancellable { listener?.remove() }
            if (listener == null) emitter.onNext(emptyList())
        }.toLatestFlowable()

        fun findItemByUrl(url: String?) = PublishSubject.create<Boolean> { emitter ->
            //assert(listener == null)
            listener?.remove()
            listener = showDoc2?.whereEqualTo(ITEM_ID, url)?.addSnapshotListener { value, error ->
                value?.toObjects<FirebaseDbModel>()
                    .also { println(it) }
                    ?.map { it.toDbModel() }
                    ?.let { emitter.onNext(it.isNotEmpty()) }
                error?.let(emitter::onError)
            }
            emitter.setCancellable { listener?.remove() }
            if (listener == null) emitter.onNext(false)
        }.toLatestFlowable()

        fun getAllEpisodesByShow(showUrl: String) = PublishSubject.create<List<ChapterWatched>> { emitter ->
            //assert(listener == null)
            listener?.remove()
            listener = episodeDoc2
                ?.document(showUrl.urlToPath())
                ?.addSnapshotListener { value, error ->
                    value?.toObject(Watched::class.java)?.watched
                        ?.map { it.toChapterWatchedModel() }
                        ?.let(emitter::onNext)
                    error?.let(emitter::onError)
                }
            emitter.setCancellable { listener?.remove() }
            if (listener == null) emitter.onNext(emptyList())
        }.toLatestFlowable()

        //fun getAllEpisodesByShow(showDbModel: DbModel) = getAllEpisodesByShow(showDbModel.showUrl)

        fun unregister() {
            listener?.remove()
            listener = null
        }

    }

    private class Watched(val watched: List<FirebaseChapterWatched> = emptyList())
}