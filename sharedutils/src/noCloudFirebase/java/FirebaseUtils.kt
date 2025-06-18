package com.programmersbox.sharedutils

import android.app.Activity
import android.content.Context
import androidx.activity.ComponentActivity
import com.programmersbox.favoritesdatabase.ChapterWatched
import com.programmersbox.favoritesdatabase.DbModel
import kotlinx.coroutines.flow.Flow
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

    fun isLoggedIn(): Boolean = false
}

object FirebaseDbImpl : FirebaseConnection {

    override fun getAllShows() = emptyList<DbModel>()
    override fun toggleUpdateCheckShowFlow(showDbModel: DbModel): Flow<Unit> = flowOf(Unit)
    override fun insertShowFlow(showDbModel: DbModel) = flowOf(Unit)
    override fun removeShowFlow(showDbModel: DbModel) = flowOf(Unit)
    override fun updateShowFlow(showDbModel: DbModel) = flowOf(Unit)
    override fun insertEpisodeWatchedFlow(episodeWatched: ChapterWatched) = flowOf(Unit)
    override fun removeEpisodeWatchedFlow(episodeWatched: ChapterWatched) = flowOf(Unit)

    class FirebaseListener : FirebaseConnection.FirebaseListener {
        override fun getAllShowsFlow() = flowOf(emptyList<DbModel>())
        override fun getShowFlow(url: String?): Flow<DbModel?> = flowOf(null)
        override fun findItemByUrlFlow(url: String?) = flowOf(false)
        override fun getAllEpisodesByShowFlow(showUrl: String) = flowOf(emptyList<ChapterWatched>())
        override fun unregister() {}
    }
}