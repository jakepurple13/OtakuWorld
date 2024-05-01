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
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.toObjects
import com.programmersbox.favoritesdatabase.ChapterWatched
import com.programmersbox.favoritesdatabase.DbModel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
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

object FirebaseDbImpl : FirebaseConnection {

    @SuppressLint("StaticFieldLeak")
    private val db = FirebaseFirestore.getInstance().apply {
        firestoreSettings = FirebaseFirestoreSettings.Builder()
            .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
            /*.setHost("10.0.2.2:8080")
            .setSslEnabled(false)
            .setPersistenceEnabled(false)*/
            //.setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
            //.setCacheSizeBytes()
            .build()
    }

    private fun <TResult> Task<TResult>.await(): TResult = Tasks.await(this)

    private val showDoc2
        get() = FirebaseAuthentication.currentUser?.let {
            runCatching { db.collection(FirebaseDb.COLLECTION_ID).document(FirebaseDb.DOCUMENT_ID).collection(it.uid) }.getOrNull()
        }
    private val episodeDoc2
        get() = FirebaseAuthentication.currentUser?.let {
            runCatching { db.collection(FirebaseDb.COLLECTION_ID).document(FirebaseDb.CHAPTERS_ID).collection(it.uid) }.getOrNull()
        }

    override fun getAllShows() = showDoc2
        ?.get()
        ?.await()
        ?.toObjects<FirebaseDbModel>()
        ?.fastMap { it.toDbModel() }
        .orEmpty()

    override fun insertShowFlow(showDbModel: DbModel) = callbackFlow<Unit> {
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

    override fun removeShowFlow(showDbModel: DbModel) = callbackFlow<Unit> {
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

    override fun updateShowFlow(showDbModel: DbModel) = callbackFlow<Unit> {
        showDoc2?.document(showDbModel.url.urlToPath())
            ?.update(FirebaseDb.READ_OR_WATCHED_ID, showDbModel.numChapters)
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

    override fun toggleUpdateCheckShowFlow(showDbModel: DbModel) = callbackFlow<Unit> {
        showDoc2?.document(showDbModel.url.urlToPath())
            ?.update(FirebaseDb.SHOW_CHECK_FOR_UPDATE_ID, showDbModel.shouldCheckForUpdate)
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

    override fun insertEpisodeWatchedFlow(episodeWatched: ChapterWatched) = callbackFlow<Unit> {
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

    override fun removeEpisodeWatchedFlow(episodeWatched: ChapterWatched) = callbackFlow<Unit> {
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

    class FirebaseListener : FirebaseConnection.FirebaseListener {

        private var listener: ListenerRegistration? = null

        override fun getAllShowsFlow() = callbackFlow {
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

        override fun getShowFlow(url: String?): Flow<DbModel?> = callbackFlow {
            listener?.remove()
            listener = showDoc2?.whereEqualTo(FirebaseDb.ITEM_ID, url)?.addSnapshotListener { value, error ->
                value?.toObjects<FirebaseDbModel>()
                    ?.fastMap { it.toDbModel() }
                    ?.let { trySend(it.firstOrNull()) }
                error?.let(this::close)
            }
            if (listener == null) trySend(null)
            awaitClose { listener?.remove() }
        }

        override fun findItemByUrlFlow(url: String?) = callbackFlow {
            listener?.remove()
            listener = showDoc2?.whereEqualTo(FirebaseDb.ITEM_ID, url)?.addSnapshotListener { value, error ->
                value?.toObjects<FirebaseDbModel>()
                    ?.fastMap { it.toDbModel() }
                    ?.let { trySend(it.isNotEmpty()) }
                error?.let(this::close)
            }
            if (listener == null) trySend(false)
            awaitClose { listener?.remove() }
        }

        override fun getAllEpisodesByShowFlow(showUrl: String) = callbackFlow {
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

        override fun unregister() {
            listener?.remove()
            listener = null
        }
    }
}