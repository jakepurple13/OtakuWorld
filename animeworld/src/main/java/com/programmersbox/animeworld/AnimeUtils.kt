package com.programmersbox.animeworld

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.FrameLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.programmersbox.helpfulutils.sharedPrefNotNullDelegate


var Context.folderLocation: String by sharedPrefNotNullDelegate(
    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).toString() + "/AnimeWorld/"
)

fun <T : View> BottomSheetBehavior<T>.open() {
    state = BottomSheetBehavior.STATE_EXPANDED
}

fun <T : View> BottomSheetBehavior<T>.close() {
    state = BottomSheetBehavior.STATE_COLLAPSED
}

fun <T : View> BottomSheetBehavior<T>.halfOpen() {
    state = BottomSheetBehavior.STATE_HALF_EXPANDED
}

abstract class BaseBottomSheetDialogFragment : BottomSheetDialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = super.onCreateDialog(savedInstanceState)
        .apply {
            setOnShowListener {
                val sheet = findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
                val bottomSheet = BottomSheetBehavior.from(sheet)
                bottomSheet.skipCollapsed = true
                bottomSheet.open()
            }
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
    private val Projections = arrayOf(
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

    /**Returns an Arraylist of [!ideoContent]   */
    @SuppressLint("InlinedApi")
    fun getAllVideoContent(contentLocation: Uri): List<VideoContent> {
        val allVideo = mutableListOf<VideoContent>()
        val cursor = videoContex.contentResolver.query(
            contentLocation,
            Projections,
            null,
            null,
            "LOWER (" + MediaStore.Video.Media.DATE_TAKEN + ") DESC"
        ) //DESC ASC
        try {
            while (cursor?.moveToNext() == true) {
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
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cursor?.close()
        }
        return allVideo
    }

    /**Returns an Arraylist of [videoFolderContent] with each videoFolderContent having an Arraylist of all it videoContent */
    @SuppressLint("InlinedApi")
    fun getAllVideoFolders(contentLocation: Uri): ArrayList<videoFolderContent> {
        val allVideoFolders: ArrayList<videoFolderContent> = ArrayList()
        val videoPaths: ArrayList<Int> = ArrayList()
        cursor = videoContex.contentResolver.query(
            contentLocation,
            Projections,
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
        val externalContentUri: Uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val internalContentUri: Uri = MediaStore.Video.Media.INTERNAL_CONTENT_URI
        private var cursor: Cursor? = null
        fun getInstance(contx: Context): VideoGet? {
            if (videoGet == null) videoGet = VideoGet(contx)
            return videoGet
        }
    }

}