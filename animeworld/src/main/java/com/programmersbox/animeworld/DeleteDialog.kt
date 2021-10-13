package com.programmersbox.animeworld

import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Done
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.programmersbox.helpfulutils.runOnUIThread
import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.Fetch
import java.io.File

@ExperimentalAnimationApi
@ExperimentalMaterialApi
@Composable
fun SlideToDeleteDialog(
    showDialog: MutableState<Boolean>,
    video: VideoContent
) {
    val context = LocalContext.current
    SlideToDeleteDialog(
        title = video.videoName.orEmpty(),
        showDialog = showDialog,
        onSlide = {
            runOnUIThread {
                try {
                    val file = File(video.path!!)
                    if (file.exists()) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            video.assetFileStringUri
                                ?.toUri()
                                ?.let { it1 ->
                                    context.contentResolver
                                        ?.delete(it1, "${MediaStore.Video.Media._ID} = ?", arrayOf(video.videoId.toString()))
                                }
                        } else {
                            Toast.makeText(context, if (file.delete()) R.string.fileDeleted else R.string.fileNotDeleted, Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, R.string.something_went_wrong, Toast.LENGTH_SHORT).show()
                }
            }
        },
        onCancel = {}
    )
}

@ExperimentalAnimationApi
@ExperimentalMaterialApi
@Composable
fun SlideToDeleteDialog(
    showDialog: MutableState<Boolean>,
    download: Download
) {
    SlideToDeleteDialog(
        title = Uri.parse(download.url).lastPathSegment.orEmpty(),
        showDialog = showDialog,
        onSlide = { Fetch.getDefaultInstance().delete(download.id) },
        onCancel = { Fetch.getDefaultInstance().resume(download.id) }
    )
}

@ExperimentalMaterialApi
@ExperimentalAnimationApi
@Composable
private fun SlideToDeleteDialog(
    title: String,
    showDialog: MutableState<Boolean>,
    onSlide: suspend () -> Unit,
    onCancel: () -> Unit
) {
    if (showDialog.value) {
        AlertDialog(
            onDismissRequest = { showDialog.value = false },
            title = { Text(stringResource(R.string.do_you_want_to_delete)) },
            text = {
                Column {
                    Text(title)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SlideTo(
                            modifier = Modifier.padding(16.dp),
                            slideHeight = 60.dp,
                            slideWidth = 300.dp,
                            slideColor = colorResource(R.color.alizarin),
                            iconCircleColor = MaterialTheme.colors.background,
                            navigationIcon = {
                                Icon(
                                    Icons.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = colorResource(R.color.alizarin),
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .rotate(it)
                                )
                            },
                            endIcon = { Icon(Icons.Filled.Done, contentDescription = null) },
                            onSlideComplete = {
                                showDialog.value = false
                                onSlide()
                            }
                        ) { Text(stringResource(R.string.delete), color = MaterialTheme.colors.background) }
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDialog.value = false
                        onCancel()
                    }
                ) { Text(stringResource(R.string.cancel), style = MaterialTheme.typography.button) }
            },
            confirmButton = {}
        )
    }
}