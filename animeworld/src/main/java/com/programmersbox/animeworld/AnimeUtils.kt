package com.programmersbox.animeworld

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.provider.MediaStore
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.navigation.NavController
import com.programmersbox.animeworld.videoplayer.VideoPlayerActivity
import com.programmersbox.animeworld.videoplayer.VideoViewModel
import com.programmersbox.helpfulutils.sharedPrefNotNullDelegate
import com.programmersbox.uiviews.utils.datastore.DataStoreHandler
import com.programmersbox.uiviews.utils.datastore.dataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.math.roundToInt

var Context.folderLocation: String by sharedPrefNotNullDelegate(
    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).toString() + "/AnimeWorld/"
)

val IGNORE_SSL = booleanPreferencesKey("ignore_ssl")
val Context.ignoreSsl get() = dataStore.data.map { it[IGNORE_SSL] ?: true }

val USER_NEW_PLAYER = booleanPreferencesKey("useNewPlayer")
val Context.useNewPlayerFlow get() = dataStore.data.map { it[USER_NEW_PLAYER] ?: true }

class AnimeDataStoreHandling(context: Context) {
    val useNewPlayer = DataStoreHandler(
        key = booleanPreferencesKey("useNewPlayer"),
        defaultValue = true,
        context = context
    )

    val ignoreSsl = DataStoreHandler(
        key = booleanPreferencesKey("ignore_ssl"),
        defaultValue = true,
        context = context
    )
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
fun Context.navigateToVideoPlayer(
    navController: NavController,
    assetFileStringUri: String?,
    videoName: String?,
    downloadOrStream: Boolean,
    referer: String = "",
) {
    if (runBlocking { useNewPlayerFlow.first() }) {
        VideoViewModel.navigateToVideoPlayer(
            navController,
            assetFileStringUri.orEmpty(),
            videoName.orEmpty(),
            downloadOrStream,
            referer
        )
    } else {
        startActivity(
            Intent(this, VideoPlayerActivity::class.java).apply {
                putExtra("showPath", assetFileStringUri)
                putExtra("showName", videoName)
                putExtra("downloadOrStream", downloadOrStream)
                data = assetFileStringUri?.toUri()
            }
        )
    }
}

data class VideoContent(
    var videoId: Long = 0,
    var videoName: String? = null,
    var path: String? = null,
    var videoDuration: Long = 0,
    var videoSize: Long = 0,
    var assetFileStringUri: String? = null,
    var album: String? = null,
    var artist: String? = null,
    //var dateAdded: Long = 0
)

class videoFolderContent {
    private var videoFiles: ArrayList<VideoContent>
    var folderName: String? = null
    var folderPath: String? = null
    var bucket_id = 0

    constructor() {
        videoFiles = ArrayList()
    }

    constructor(folderPath: String?, folderName: String?) {
        this.folderName = folderName
        this.folderPath = folderPath
        videoFiles = ArrayList()
    }

    fun getVideoFiles(): ArrayList<VideoContent> {
        return videoFiles
    }

    fun setVideoFiles(videoFiles: ArrayList<VideoContent>) {
        this.videoFiles = videoFiles
    }
}

class VideoGet private constructor(private val videoContex: Context) {

    @SuppressLint("InlinedApi")
    private val projections = arrayOf(
        MediaStore.Video.Media.DATA,
        MediaStore.Video.Media.DISPLAY_NAME,
        MediaStore.Video.Media.DURATION,
        MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
        MediaStore.Video.Media.BUCKET_ID,
        MediaStore.Video.Media.SIZE,
        MediaStore.Video.Media._ID,
        MediaStore.Video.Media.ALBUM,
        MediaStore.Video.Media.DATE_TAKEN,
        MediaStore.Video.Media.ARTIST,
        //MediaStore.Video.Media.DATE_ADDED,
    )

    /**Returns an Arraylist of [VideoContent]   */
    @SuppressLint("InlinedApi")
    fun getAllVideoContent(contentLocation: Uri): List<VideoContent> {
        val allVideo = mutableListOf<VideoContent>()
        videoContex.contentResolver
            .query(
                contentLocation,
                projections,
                null,
                null,
                "LOWER (" + MediaStore.Video.Media.DATE_TAKEN + ") DESC"
            ) //DESC ASC
            ?.use { cursor ->
                while (cursor.moveToNext()) {
                    try {
                        val id: Int = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID))
                        val contentUri: Uri = Uri.withAppendedPath(contentLocation, id.toString())
                        allVideo.add(
                            VideoContent(
                                videoDuration = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)),
                                videoId = id.toLong(),
                                videoName = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)),
                                album = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.ALBUM)),
                                artist = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.ARTIST)),
                                assetFileStringUri = contentUri.toString(),
                                path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)),
                                videoSize = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE))
                            )
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                        continue
                    }
                }
            }

        return allVideo
    }

    private fun ContentResolver.registerObserver(
        uri: Uri,
        observer: (selfChange: Boolean) -> Unit,
    ): ContentObserver {
        val contentObserver = object : ContentObserver(Handler()) {
            override fun onChange(selfChange: Boolean) {
                observer(selfChange)
                println("Changed!!!")
            }
        }
        registerContentObserver(uri, true, contentObserver)
        return contentObserver
    }

    private var contentObserver: ContentObserver? = null

    fun unregister() {
        contentObserver?.let { videoContex.contentResolver.unregisterContentObserver(it) }
    }

    val videos2 = MutableStateFlow<List<VideoContent>>(emptyList())

    fun loadVideos(scope: CoroutineScope, contentLocation: Uri) {
        scope.launch {
            val imageList = getAllVideoContent(contentLocation)
            videos2.tryEmit(imageList)

            if (contentObserver == null) {
                contentObserver = videoContex.contentResolver.registerObserver(contentLocation) {
                    loadVideos(scope, contentLocation)
                }
            }
        }
    }

    /**Returns an Arraylist of [videoFolderContent] with each videoFolderContent having an Arraylist of all it videoContent */
    @SuppressLint("InlinedApi")
    fun getAllVideoFolders(contentLocation: Uri): ArrayList<videoFolderContent> {
        val allVideoFolders: ArrayList<videoFolderContent> = ArrayList()
        val videoPaths: ArrayList<Int> = ArrayList()
        cursor = videoContex.contentResolver.query(
            contentLocation,
            projections,
            null,
            null,
            "LOWER (" + MediaStore.Video.Media.DATE_TAKEN + ") DESC"
        ) //DESC

        try {
            cursor!!.moveToFirst()
            do {
                val videoFolder = videoFolderContent()
                val videoContent = VideoContent()
                videoContent.videoName = (cursor!!.getString(cursor!!.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)))
                videoContent.path = (cursor!!.getString(cursor!!.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)))
                videoContent.videoDuration = (cursor!!.getLong(cursor!!.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)))
                videoContent.videoSize = (cursor!!.getLong(cursor!!.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)))
                val id: Int = cursor!!.getInt(cursor!!.getColumnIndexOrThrow(MediaStore.Video.Media._ID))
                videoContent.videoId = (id).toLong()
                val contentUri: Uri = Uri.withAppendedPath(contentLocation, id.toString())
                videoContent.assetFileStringUri = (contentUri.toString())
                videoContent.album = (cursor!!.getString(cursor!!.getColumnIndexOrThrow(MediaStore.Video.Media.ALBUM)))
                videoContent.artist = (cursor!!.getString(cursor!!.getColumnIndexOrThrow(MediaStore.Video.Media.ARTIST)))
                val folder: String = cursor!!.getString(cursor!!.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME))
                val datapath: String = cursor!!.getString(cursor!!.getColumnIndexOrThrow(MediaStore.Video.Media.DATA))
                val bucket_id: Int = cursor!!.getInt(cursor!!.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_ID))
                var folderpaths = datapath.substring(0, datapath.lastIndexOf("$folder/"))
                folderpaths = "$folderpaths$folder/"
                if (!videoPaths.contains(bucket_id)) {
                    videoPaths.add(bucket_id)
                    videoFolder.bucket_id = (bucket_id)
                    videoFolder.folderPath = (folderpaths)
                    videoFolder.folderName = (folder)
                    videoFolder.getVideoFiles().add(videoContent)
                    allVideoFolders.add(videoFolder)
                } else {
                    for (i in 0 until allVideoFolders.size) {
                        if (allVideoFolders[i].bucket_id == bucket_id) {
                            allVideoFolders[i].getVideoFiles().add(videoContent)
                        }
                    }
                }
            } while (cursor!!.moveToNext())
            cursor!!.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return allVideoFolders
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        private var videoGet: VideoGet? = null
        val externalContentUri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        //MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val internalContentUri: Uri = MediaStore.Video.Media.INTERNAL_CONTENT_URI
        private var cursor: Cursor? = null
        fun getInstance(contx: Context): VideoGet? {
            if (videoGet == null) videoGet = VideoGet(contx)
            return videoGet
        }
    }

}

enum class SlideState { Start, End }

@OptIn(ExperimentalFoundationApi::class)
@ExperimentalAnimationApi
@Composable
fun SlideTo(
    modifier: Modifier = Modifier,
    slideHeight: Dp = 60.dp,
    slideWidth: Dp = 400.dp,
    slideColor: Color,
    iconCircleColor: Color = contentColorFor(backgroundColor = slideColor),
    onSlideComplete: suspend () -> Unit = {},
    navigationIcon: @Composable (progress: Float) -> Unit,
    navigationIconPadding: Dp = 0.dp,
    endIcon: @Composable () -> Unit,
    widthAnimationMillis: Int = 300,
    elevation: Dp = 0.dp,
    content: @Composable (Float) -> Unit = {},
) {

    val iconSize = slideHeight - 10.dp

    val slideDistance = with(LocalDensity.current) { (slideWidth - iconSize - 15.dp).toPx() }

    val swipeableState = remember {
        AnchoredDraggableState(
            initialValue = SlideState.Start,
            anchors = DraggableAnchors {
                SlideState.Start at 0f
                SlideState.End at slideDistance
            },
            positionalThreshold = { distance: Float -> distance * 0.5f },
            velocityThreshold = { 125f },
            snapAnimationSpec = spring<Float>(),
            decayAnimationSpec = exponentialDecay<Float>()
        )
    }

    var flag by remember { mutableStateOf(iconSize) }

    if (swipeableState.currentValue == SlideState.End) {
        flag = 0.dp
    }

    val contentAlpha by animateFloatAsState(
        targetValue = if (swipeableState.offset != 0f && swipeableState.offset > 0f)
            (1 - swipeableState.progress)
        else 1f, label = ""
    )

    val iconSizeAnimation by animateDpAsState(targetValue = flag, tween(250), label = "")

    val width by animateDpAsState(
        targetValue = if (iconSizeAnimation == 0.dp) slideHeight else slideWidth,
        tween(widthAnimationMillis), label = ""
    )

    AnimatedVisibility(
        visible = width != slideHeight,
        exit = fadeOut(
            targetAlpha = 0f,
            animationSpec = tween(250, easing = LinearEasing, delayMillis = 1000)
        )
    ) {
        Box(contentAlignment = Alignment.Center) {
            Surface(
                shape = CircleShape,
                modifier = modifier
                    .height(slideHeight)
                    .width(width),
                color = slideColor,
                tonalElevation = elevation
            ) {
                Box(
                    modifier = Modifier.padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(contentAlpha),
                        contentAlignment = Alignment.Center
                    ) { content(swipeableState.offset) }
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = iconCircleColor,
                            modifier = Modifier
                                .size(iconSizeAnimation)
                                .padding(navigationIconPadding)
                                .anchoredDraggable(
                                    state = swipeableState,
                                    orientation = Orientation.Horizontal
                                )
                                .offset { IntOffset(swipeableState.offset.roundToInt(), 0) },
                        ) { navigationIcon(swipeableState.offset / slideWidth.value * 90f) }
                    }
                    AnimatedVisibility(
                        visible = width == slideHeight,
                        enter = expandIn()
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                modifier = Modifier.size(iconSize),
                                color = Color.Transparent
                            ) { endIcon() }
                        }
                        LaunchedEffect(key1 = Unit) { onSlideComplete() }
                    }
                }
            }
        }
    }
}

enum class Qualities(var value: Int) {
    Unknown(0),
    P360(-2), // 360p
    P480(-1), // 480p
    P720(1), // 720p
    P1080(2), // 1080p
    P1440(3), // 1440p
    P2160(4) // 4k or 2160p
}

fun getQualityFromName(qualityName: String): Qualities {
    return when (qualityName.replace("p", "").replace("P", "")) {
        "360" -> Qualities.P360
        "480" -> Qualities.P480
        "720" -> Qualities.P720
        "1080" -> Qualities.P1080
        "1440" -> Qualities.P1440
        "2160" -> Qualities.P2160
        "4k" -> Qualities.P2160
        "4K" -> Qualities.P2160
        else -> Qualities.Unknown
    }
}