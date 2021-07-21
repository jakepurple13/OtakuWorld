package com.programmersbox.animeworld

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.net.toUri
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.ncorti.slidetoact.SlideToActView
import com.programmersbox.animeworld.databinding.DeleteDialogLayoutBinding
import com.programmersbox.helpfulutils.layoutInflater
import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.Fetch
import java.io.File

private fun Context.deleteDialogSet(title: CharSequence, onSlide: () -> Unit, onCancel: () -> Unit) {
    val binding = DeleteDialogLayoutBinding.inflate(layoutInflater)

    val dialog = MaterialAlertDialogBuilder(this)
        .setView(binding.root)
        .setTitle(getString(R.string.deleteDownload, title))
        .setNegativeButton(R.string.cancel) { d, _ ->
            d.dismiss()
            onCancel()
        }
        .setOnDismissListener { onCancel() }
        .create()

    binding.slideButton.onSlideCompleteListener = object : SlideToActView.OnSlideCompleteListener {
        override fun onSlideComplete(view: SlideToActView) {
            onSlide()
            dialog.dismiss()
        }
    }
    dialog.show()
}

fun Context.deleteDialog(video: VideoContent, onCancel: () -> Unit) {
    deleteDialogSet(
        video.videoName.orEmpty(),
        onSlide = {
            try {
                val file = File(video.path!!)
                if (file.exists()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        video.assetFileStringUri
                            ?.toUri()
                            ?.let { it1 -> contentResolver?.delete(it1, "${MediaStore.Video.Media._ID} = ?", arrayOf(video.videoId.toString())) }
                    } else {
                        Toast.makeText(this, if (file.delete()) R.string.fileDeleted else R.string.fileNotDeleted, Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Something went wrong", Toast.LENGTH_SHORT).show()
            }
        },
        onCancel
    )
}

fun Context.deleteDialog(download: Download, onCancel: () -> Unit) {
    deleteDialogSet(
        getString(R.string.delete_title, Uri.parse(download.url).lastPathSegment),
        onSlide = { Fetch.getDefaultInstance().delete(download.id) },
        onCancel = {
            onCancel()
            Fetch.getDefaultInstance().resume(download.id)
        }
    )
}