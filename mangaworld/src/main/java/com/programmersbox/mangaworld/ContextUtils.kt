package com.programmersbox.mangaworld

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.database.ContentObserver
import android.database.Cursor
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.provider.MediaStore
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.core.animation.doOnEnd
import androidx.core.view.drawToBitmap
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import com.google.android.material.behavior.HideBottomViewOnScrollBehavior
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.programmersbox.helpfulutils.sharedPrefNotNullDelegate
import com.programmersbox.uiviews.utils.dataStore
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

var Context.showAdult by sharedPrefNotNullDelegate(false)
var Context.useNewReader by sharedPrefNotNullDelegate(true)

val SHOW_ADULT = booleanPreferencesKey("showAdultSources")
val Context.showAdultFlow get() = dataStore.data.map { it[SHOW_ADULT] ?: false }

val USER_NEW_READER = booleanPreferencesKey("useNewReader")
val Context.useNewReaderFlow get() = dataStore.data.map { it[USER_NEW_READER] ?: true }

val DOWNLOAD_FILE_PATH get() = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString() + "/MangaWorld/"

class CustomHideBottomViewOnScrollBehavior<T : View>(context: Context?, attrs: AttributeSet?) :
    HideBottomViewOnScrollBehavior<T>(context, attrs) {

    var isShowing = true
        private set

    override fun slideDown(child: T) {
        super.slideDown(child)
        isShowing = false
    }

    override fun slideUp(child: T) {
        super.slideUp(child)
        isShowing = true
    }

}

class ChaptersGet private constructor(private val chaptersContex: Context) {

    data class Chapters(
        val name: String,
        val id: String,
        val data: String,
        val assetFileStringUri: String,
        val folder: String = "",
        val folderName: String = "",
        val chapterFolder: String = "",
        val chapterName: String = ""
    )

    /**Returns an Arraylist of [Chapters]   */
    @SuppressLint("InlinedApi")
    fun getAllMangaContent(contentLocation: Uri): List<Chapters> {
        val allVideo = mutableListOf<Chapters>()
        val cursor = chaptersContex.contentResolver.query(
            contentLocation,
            Projections,
            null,
            null,
            "LOWER (" + MediaStore.Files.FileColumns.DATE_TAKEN + ") DESC"
        ) //DESC ASC
        try {
            while (cursor?.moveToNext() == true) {
                val id: Int = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID))
                val contentUri: Uri = Uri.withAppendedPath(contentLocation, id.toString())
                allVideo.add(
                    Chapters(
                        id = id.toString(),
                        name = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)),
                        data = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)),
                        assetFileStringUri = contentUri.toString()
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cursor?.close()
        }
        return allVideo
    }

    private fun ContentResolver.registerObserver(
        uri: Uri,
        observer: (selfChange: Boolean) -> Unit
    ): ContentObserver {
        val contentObserver = object : ContentObserver(Handler()) {
            override fun onChange(selfChange: Boolean) {
                observer(selfChange)
            }
        }
        registerContentObserver(uri, true, contentObserver)
        return contentObserver
    }

    private var contentObserver: ContentObserver? = null

    fun unregister() {
        contentObserver?.let { chaptersContex.contentResolver.unregisterContentObserver(it) }
    }

    val chapters = PublishSubject.create<List<Chapters>>()
    val chapters2 = MutableStateFlow<List<Chapters>>(emptyList())

    fun loadChapters(scope: CoroutineScope, contentLocation: Uri) {
        scope.launch {
            val imageList = getAllMangaContent(contentLocation)
            chapters.onNext(imageList)

            if (contentObserver == null) {
                contentObserver = chaptersContex.contentResolver.registerObserver(contentLocation) {
                    loadChapters(scope, contentLocation)
                }
            }
        }
    }

    fun loadChapters(scope: CoroutineScope, folderLocation: String) {
        scope.launch {
            val imageList = getMangaFolders(chaptersContex, folderLocation)
            chapters.onNext(imageList)
            chapters2.tryEmit(imageList)

            if (contentObserver == null) {
                contentObserver = chaptersContex.contentResolver.registerObserver(externalContentUri) {
                    loadChapters(scope, folderLocation)
                }
            }
        }
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        private var chaptersGet: ChaptersGet? = null
        val externalContentUri: Uri = MediaStore.Files.getContentUri("external")

        //val internalContentUri: Uri = MediaStore.Video.Media.INTERNAL_CONTENT_URI
        private var cursor: Cursor? = null
        fun getInstance(contx: Context): ChaptersGet? {
            if (chaptersGet == null) chaptersGet = ChaptersGet(contx)
            return chaptersGet
        }

        @SuppressLint("InlinedApi")
        private val Projections = arrayOf(
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DATA
        )

        fun getMangaFolders(context: Context, folderLocation: String): List<Chapters> {
            val contentLocation = externalContentUri
            val allVideo = mutableListOf<Chapters>()
            val cursor = context.contentResolver.query(
                contentLocation,
                Projections,
                MediaStore.Files.FileColumns.DATA + " LIKE ?",
                arrayOf("$folderLocation%"),
                "LOWER (" + MediaStore.Files.FileColumns.DATE_ADDED + ") DESC"
            ) //DESC ASC
            try {
                while (cursor?.moveToNext() == true) {
                    val id: Int = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID))
                    val contentUri: Uri = Uri.withAppendedPath(contentLocation, id.toString())
                    val folder = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA))
                        .split("/")
                    allVideo.add(
                        Chapters(
                            id = id.toString(),
                            name = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)),
                            data = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)),
                            assetFileStringUri = contentUri.toString(),
                            folder = folder.dropLast(2).joinToString("/"),
                            folderName = folder.dropLast(2).lastOrNull().orEmpty(),
                            chapterFolder = folder.dropLast(1).joinToString("/"),
                            chapterName = folder.dropLast(1).lastOrNull().orEmpty()
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                cursor?.close()
            }
            return allVideo
        }
    }
}

val PAGE_PADDING = intPreferencesKey("page_padding")
val Context.pagePadding get() = dataStore.data.map { it[PAGE_PADDING] ?: 4 }

val showOrHideNav = BehaviorSubject.createDefault(true)

/**
 * Potentially animate showing a [BottomNavigationView].
 *
 * Abruptly changing the visibility leads to a re-layout of main content, animating
 * `translationY` leaves a gap where the view was that content does not fill.
 *
 * Instead, take a snapshot of the view, and animate this in, only changing the visibility (and
 * thus layout) when the animation completes.
 */
fun BottomNavigationView.show() {
    if (visibility == View.VISIBLE) return

    val parent = parent as ViewGroup
    // View needs to be laid out to create a snapshot & know position to animate. If view isn't
    // laid out yet, need to do this manually.
    if (!isLaidOut) {
        measure(
            View.MeasureSpec.makeMeasureSpec(parent.width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(parent.height, View.MeasureSpec.AT_MOST)
        )
        layout(parent.left, parent.height - measuredHeight, parent.right, parent.height)
    }

    val drawable = BitmapDrawable(context.resources, drawToBitmap())
    drawable.setBounds(left, parent.height, right, parent.height + height)
    parent.overlay.add(drawable)
    ValueAnimator.ofInt(parent.height, top).apply {
        startDelay = 100L
        duration = 300L
        interpolator = AnimationUtils.loadInterpolator(
            context,
            android.R.interpolator.linear_out_slow_in
        )
        addUpdateListener {
            val newTop = it.animatedValue as Int
            drawable.setBounds(left, newTop, right, newTop + height)
        }
        doOnEnd {
            parent.overlay.remove(drawable)
            visibility = View.VISIBLE
        }
        start()
    }
}

/**
 * Potentially animate hiding a [BottomNavigationView].
 *
 * Abruptly changing the visibility leads to a re-layout of main content, animating
 * `translationY` leaves a gap where the view was that content does not fill.
 *
 * Instead, take a snapshot, instantly hide the view (so content lays out to fill), then animate
 * out the snapshot.
 */
fun BottomNavigationView.hide() {
    if (visibility == View.GONE) return

    val drawable = BitmapDrawable(context.resources, drawToBitmap())
    val parent = parent as ViewGroup
    drawable.setBounds(left, top, right, bottom)
    parent.overlay.add(drawable)
    visibility = View.GONE
    ValueAnimator.ofInt(top, parent.height).apply {
        startDelay = 100L
        duration = 200L
        interpolator = AnimationUtils.loadInterpolator(
            context,
            android.R.interpolator.fast_out_linear_in
        )
        addUpdateListener {
            val newTop = it.animatedValue as Int
            drawable.setBounds(left, newTop, right, newTop + height)
        }
        doOnEnd {
            parent.overlay.remove(drawable)
        }
        start()
    }
}