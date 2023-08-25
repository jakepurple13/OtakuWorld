package com.programmersbox.sharedutils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.ui.util.fastMap
import com.firebase.ui.auth.AuthUI
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.toObjects
import com.programmersbox.favoritesdatabase.ChapterWatched
import com.programmersbox.favoritesdatabase.DbModel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

object FirebaseAuthentication : KoinComponent {

    private const val RC_SIGN_IN = 32

    val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val logo: AppLogo by inject()
    private val style: FirebaseUIStyle by inject()

    fun signIn(activity: Activity) {
        //val signInIntent = googleSignInClient!!.signInIntent
        //activity.startActivityForResult(signInIntent, RC_SIGN_IN)
        // Choose authentication providers
        val providers = arrayListOf(
            AuthUI.IdpConfig.GoogleBuilder().build()
        )

        // Create and launch sign-in intent
        activity.startActivityForResult(
            AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setTheme(style.style)
                //.setLogo(R.mipmap.big_logo)
                .setLogo(logo.logoId)
                .setAvailableProviders(providers)
                .build(),
            RC_SIGN_IN
        )
    }

    fun signOut() {
        auth.signOut()
        //currentUser = null
    }

    internal val currentUser: FirebaseUser? get() = FirebaseAuth.getInstance().currentUser

    private var update: ((FirebaseAuth?) -> Unit)? = null

    fun addAuthStateListener(update: (CustomFirebaseUser?) -> Unit) {
        this.update = { u -> update(u?.currentUser?.let { CustomFirebaseUser(it.displayName, it.photoUrl) }) }
        auth.addAuthStateListener(this.update!!)
    }

    fun clear() {
        update?.let { auth.removeAuthStateListener(it) }
    }

    fun signInOrOut(context: Context, activity: ComponentActivity, title: Int, message: Int, positive: Int, no: Int) {
        currentUser?.let {
            MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(positive) { d, _ ->
                    signOut()
                    d.dismiss()
                }
                .setNegativeButton(no) { d, _ -> d.dismiss() }
                .show()
        } ?: signIn(activity)
    }
}

data class CustomFirebaseUser(val displayName: String?, val photoUrl: Uri?)

object FirebaseDb {

    var DOCUMENT_ID = ""
    var CHAPTERS_ID = ""
    var COLLECTION_ID = ""
    var ITEM_ID = ""
    var READ_OR_WATCHED_ID = ""

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

    private val showDoc2 get() = FirebaseAuthentication.currentUser?.let { db.collection(COLLECTION_ID).document(DOCUMENT_ID).collection(it.uid) }
    private val episodeDoc2 get() = FirebaseAuthentication.currentUser?.let { db.collection(COLLECTION_ID).document(CHAPTERS_ID).collection(it.uid) }

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

    fun getAllShows() = showDoc2
        ?.get()
        ?.await()
        ?.toObjects<FirebaseDbModel>()
        ?.fastMap { it.toDbModel() }
        .orEmpty()

    fun insertShowFlow(showDbModel: DbModel) = callbackFlow {
        showDoc2?.document(showDbModel.url.urlToPath())
            ?.set(showDbModel.toFirebaseDbModel())
            ?.addOnSuccessListener {
                trySend(Unit)
                close()
            }
            ?.addOnFailureListener { close(it) } ?: run {
            trySend(Unit)
            close()
        }
        awaitClose()
    }

    fun removeShowFlow(showDbModel: DbModel) = callbackFlow {
        showDoc2?.document(showDbModel.url.urlToPath())
            ?.delete()
            ?.addOnSuccessListener {
                trySend(Unit)
                close()
            }
            ?.addOnFailureListener { close(it) } ?: run {
            trySend(Unit)
            close()
        }
        awaitClose()
    }

    fun updateShowFlow(showDbModel: DbModel) = callbackFlow {
        showDoc2?.document(showDbModel.url.urlToPath())
            ?.update(READ_OR_WATCHED_ID, showDbModel.numChapters)
            ?.addOnSuccessListener {
                trySend(Unit)
                close()
            }
            ?.addOnFailureListener { close(it) } ?: run {
            trySend(Unit)
            close()
        }
        awaitClose()
    }

    fun insertEpisodeWatchedFlow(episodeWatched: ChapterWatched) = callbackFlow {
        episodeDoc2
            ?.document(episodeWatched.favoriteUrl.urlToPath())
            //?.set("create" to 1)
            //?.update("watched", FieldValue.arrayUnion(episodeWatched.toFirebaseChapterWatched()))
            ?.get()
            ?.addOnSuccessListener { value ->
                if (value?.exists() == true) {
                    episodeDoc2
                        ?.document(episodeWatched.favoriteUrl.urlToPath())
                        ?.update("watched", FieldValue.arrayUnion(episodeWatched.toFirebaseChapterWatched()))
                        ?.addOnSuccessListener {
                            trySend(Unit)
                            close()
                        }
                        ?.addOnFailureListener { close() } ?: run {
                        trySend(Unit)
                        close()
                    }
                } else {
                    episodeDoc2
                        ?.document(episodeWatched.favoriteUrl.urlToPath())
                        ?.set("create" to 1)
                        ?.addOnSuccessListener {
                            episodeDoc2
                                ?.document(episodeWatched.favoriteUrl.urlToPath())
                                ?.update("watched", FieldValue.arrayUnion(episodeWatched.toFirebaseChapterWatched()))
                                ?.addOnSuccessListener {
                                    trySend(Unit)
                                    close()
                                }
                                ?.addOnFailureListener { close() } ?: run {
                                trySend(Unit)
                                close()
                            }
                        }
                }
            } ?: trySend(Unit)
        awaitClose()
    }

    fun removeEpisodeWatchedFlow(episodeWatched: ChapterWatched) = callbackFlow {
        episodeDoc2
            ?.document(episodeWatched.favoriteUrl.urlToPath())
            ?.update("watched", FieldValue.arrayRemove(episodeWatched.toFirebaseChapterWatched()))
            //?.collection(episodeWatched.url.urlToPath())
            //?.document("watched")
            //?.delete()
            ?.addOnSuccessListener {
                trySend(Unit)
                close()
            }
            ?.addOnFailureListener { close(it) } ?: run {
            trySend(Unit)
            close()
        }
        awaitClose()
    }

    class FirebaseListener {

        private var listener: ListenerRegistration? = null

        fun getAllShowsFlow() = callbackFlow {
            listener?.remove()
            listener = showDoc2?.addSnapshotListener { value, error ->
                value?.toObjects<FirebaseDbModel>()
                    ?.fastMap { it.toDbModel() }
                    ?.let { trySend(it) }
                error?.let(this::close)
            }
            if (listener == null) trySend(emptyList())
            awaitClose { listener?.remove() }
        }

        fun findItemByUrlFlow(url: String?) = callbackFlow {
            listener?.remove()
            listener = showDoc2?.whereEqualTo(ITEM_ID, url)?.addSnapshotListener { value, error ->
                value?.toObjects<FirebaseDbModel>()
                    .also { println(it) }
                    ?.fastMap { it.toDbModel() }
                    ?.let { trySend(it.isNotEmpty()) }
                error?.let(this::close)
            }
            if (listener == null) trySend(false)
            awaitClose { listener?.remove() }
        }

        fun getAllEpisodesByShowFlow(showUrl: String) = callbackFlow {
            listener?.remove()
            listener = episodeDoc2
                ?.document(showUrl.urlToPath())
                ?.addSnapshotListener { value, error ->
                    value?.toObject(Watched::class.java)?.watched
                        ?.fastMap { it.toChapterWatchedModel() }
                        ?.let { trySend(it) }
                    error?.let(this::close)
                }
            if (listener == null) trySend(emptyList())
            awaitClose()
        }

        fun unregister() {
            listener?.remove()
            listener = null
        }

    }

    private class Watched(val watched: List<FirebaseChapterWatched> = emptyList())
}