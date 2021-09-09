package com.programmersbox.animeworld

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.GranularRoundedCorners
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.android.material.composethemeadapter.MdcTheme
import com.programmersbox.animeworld.databinding.FragmentViewVideosBinding
import com.programmersbox.dragswipe.*
import com.programmersbox.helpfulutils.*
import com.programmersbox.uiviews.BaseMainActivity
import com.programmersbox.uiviews.utils.*
import com.skydoves.landscapist.glide.GlideImage
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.flow.onEach
import java.io.File
import java.util.concurrent.TimeUnit

class ViewVideosFragment : BaseBottomSheetDialogFragment() {

    private lateinit var binding: FragmentViewVideosBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentViewVideosBinding.inflate(inflater, container, false)
        return binding.root
    }

    private val disposable = CompositeDisposable()

    @ExperimentalPermissionsApi
    @ExperimentalMaterialApi
    @ExperimentalAnimationApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        MainActivity.cast.setMediaRouteMenu(requireContext(), binding.toolbarmenu.menu)
        getStuff()
    }

    @ExperimentalPermissionsApi
    @ExperimentalAnimationApi
    @ExperimentalMaterialApi
    private fun getStuff() {
        binding.composeLayout.setContent {
            MdcTheme {
                PermissionRequest(listOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    LaunchedEffect(Unit) {
                        val v = VideoGet.getInstance(requireContext())
                        v?.loadVideos(lifecycleScope, VideoGet.externalContentUri)
                    }

                    VideoLoad()
                }
            }
        }
    }

    @ExperimentalAnimationApi
    @ExperimentalMaterialApi
    @Composable
    private fun VideoLoad() {

        val v = remember { VideoGet.getInstance(requireContext()) }

        val prefs = remember { requireContext().getSharedPreferences("videos", Context.MODE_PRIVATE) }

        val items by v!!.videos2
            .onEach {
                @Suppress("RegExpRedundantEscape") val fileRegex = "(\\/[^*|\"<>?\\n]*)|(\\\\\\\\.*?\\\\.*)".toRegex()
                val filePrefs = prefs.all.keys.filter(fileRegex::containsMatchIn)
                filePrefs.fastForEach { p ->
                    if (it.none { it1 -> it1.assetFileStringUri == p }) {
                        prefs.edit().remove(p).apply()
                    }
                }
            }
            .collectAsState(emptyList())

        if (items.isEmpty()) {
            EmptyState()
        } else {
            BottomSheetDeleteScaffold(
                listOfItems = items,
                multipleTitle = stringResource(id = R.string.delete),
                onRemove = { context?.deleteDialog(it) {} },
                customSingleRemoveDialog = {
                    context?.deleteDialog(it) {}
                    false
                },
                onMultipleRemove = { downloadedItems ->
                    downloadedItems.forEach {
                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                it.assetFileStringUri?.toUri()?.let { it1 ->
                                    context?.contentResolver?.delete(
                                        it1,
                                        "${MediaStore.Video.Media._ID} = ?",
                                        arrayOf(it.videoId.toString())
                                    )
                                }
                            } else {
                                File(it.path!!).delete()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(requireContext(), "Something went wrong with ${it.videoName}", Toast.LENGTH_SHORT).show()
                        }
                    }
                    downloadedItems.clear()
                },
                itemUi = { item ->

                    ListItem(
                        modifier = Modifier.padding(5.dp),
                        icon = {

                            Box {

                                /*convert millis to appropriate time*/
                                val runTimeString = remember {
                                    val duration = item.videoDuration
                                    if (duration > TimeUnit.HOURS.toMillis(1)) {
                                        String.format(
                                            "%02d:%02d:%02d",
                                            TimeUnit.MILLISECONDS.toHours(duration),
                                            TimeUnit.MILLISECONDS.toMinutes(duration) - TimeUnit.HOURS.toMinutes(
                                                TimeUnit.MILLISECONDS.toHours(duration)
                                            ),
                                            TimeUnit.MILLISECONDS.toSeconds(duration) - TimeUnit.MINUTES.toSeconds(
                                                TimeUnit.MILLISECONDS.toMinutes(duration)
                                            )
                                        )
                                    } else {
                                        String.format(
                                            "%02d:%02d",
                                            TimeUnit.MILLISECONDS.toMinutes(duration),
                                            TimeUnit.MILLISECONDS.toSeconds(duration) - TimeUnit.MINUTES.toSeconds(
                                                TimeUnit.MILLISECONDS.toMinutes(duration)
                                            )
                                        )
                                    }
                                }

                                GlideImage(
                                    imageModel = item.assetFileStringUri.orEmpty(),
                                    contentDescription = item.videoName,
                                    contentScale = ContentScale.Crop,
                                    requestBuilder = Glide.with(LocalView.current)
                                        .asDrawable()
                                        .override(360, 480)
                                        .thumbnail(0.5f)
                                        .transform(RoundedCorners(15)),
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .size(ComposableUtils.IMAGE_WIDTH, ComposableUtils.IMAGE_HEIGHT)
                                )

                                Text(
                                    runTimeString,
                                    color = Color.White,
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .background(Color(0x99000000))
                                        .border(BorderStroke(1.dp, Color(0x00000000)), shape = RoundedCornerShape(5.dp))
                                )

                            }
                        },
                        overlineText = if (requireContext().getSharedPreferences("videos", Context.MODE_PRIVATE).contains(item.path)) {
                            { Text(requireContext().getSharedPreferences("videos", Context.MODE_PRIVATE).getLong(item.path, 0).stringForTime()) }
                        } else null,
                        text = { Text(item.videoName.orEmpty()) },
                        secondaryText = { Text(item.path.orEmpty()) }
                    )
                }
            ) {
                Scaffold(modifier = Modifier.padding(it)) { p ->
                    val videos by updateAnimatedItemsState(newList = items)

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                        contentPadding = p,
                        state = rememberLazyListState(),
                        modifier = Modifier.padding(5.dp)
                    ) {
                        animatedItems(
                            videos,
                            enterTransition = slideInHorizontally({ x -> x / 2 }),
                            exitTransition = slideOutHorizontally()
                        ) { i -> VideoContentView(i) }
                    }
                }
            }

        }

    }

    @Composable
    private fun EmptyState() {

        Box(modifier = Modifier.fillMaxSize()) {

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(5.dp),
                elevation = 5.dp,
                shape = RoundedCornerShape(5.dp)
            ) {

                Column(modifier = Modifier) {

                    Text(
                        text = stringResource(id = R.string.get_started),
                        style = MaterialTheme.typography.h4,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )

                    Text(
                        text = stringResource(id = R.string.download_a_video),
                        style = MaterialTheme.typography.body1,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )

                    Button(
                        onClick = {
                            dismiss()
                            (activity as? BaseMainActivity)?.goToScreen(BaseMainActivity.Screen.RECENT)
                        },
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(bottom = 5.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.go_download),
                            style = MaterialTheme.typography.button
                        )
                    }

                }

            }
        }

    }

    @ExperimentalMaterialApi
    @Composable
    private fun VideoContentView(item: VideoContent) {

        val dismissState = rememberDismissState(
            confirmStateChange = {
                if (it == DismissValue.DismissedToEnd) {
                    context?.startActivity(
                        Intent(context, VideoPlayerActivity::class.java).apply {
                            putExtra("showPath", item.assetFileStringUri)
                            putExtra("showName", item.videoName)
                            putExtra("downloadOrStream", true)
                        }
                    )
                } else if (it == DismissValue.DismissedToStart) {
                    context?.deleteDialog(item) {}
                }
                false
            }
        )

        SwipeToDismiss(
            state = dismissState,
            directions = setOf(DismissDirection.StartToEnd, DismissDirection.EndToStart),
            dismissThresholds = { FractionalThreshold(0.5f) },
            background = {
                val direction = dismissState.dismissDirection ?: return@SwipeToDismiss
                val color by animateColorAsState(
                    when (dismissState.targetValue) {
                        DismissValue.Default -> Color.Transparent
                        DismissValue.DismissedToEnd -> Color.Green
                        DismissValue.DismissedToStart -> Color.Red
                    }
                )
                val alignment = when (direction) {
                    DismissDirection.StartToEnd -> Alignment.CenterStart
                    DismissDirection.EndToStart -> Alignment.CenterEnd
                }
                val icon = when (direction) {
                    DismissDirection.StartToEnd -> Icons.Default.Done
                    DismissDirection.EndToStart -> Icons.Default.Delete
                }
                val scale by animateFloatAsState(if (dismissState.targetValue == DismissValue.Default) 0.75f else 1f)

                Box(
                    Modifier
                        .fillMaxSize()
                        .background(color)
                        .padding(horizontal = 20.dp),
                    contentAlignment = alignment
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        modifier = Modifier.scale(scale)
                    )
                }
            }
        ) {
            Card(
                modifier = Modifier.fillMaxSize(),
                interactionSource = MutableInteractionSource(),
                indication = rememberRipple(),
                onClick = {
                    if (MainActivity.cast.isCastActive()) {
                        MainActivity.cast.loadMedia(
                            File(item.path!!),
                            context?.getSharedPreferences("videos", Context.MODE_PRIVATE)?.getLong(item.assetFileStringUri, 0) ?: 0L,
                            null, null
                        )
                    } else {
                        context?.startActivity(
                            Intent(context, VideoPlayerActivity::class.java).apply {
                                putExtra("showPath", item.assetFileStringUri)
                                putExtra("showName", item.videoName)
                                putExtra("downloadOrStream", true)
                            }
                        )
                    }
                }
            ) {

                Row {

                    Box {

                        /*convert millis to appropriate time*/
                        val runTimeString = remember {
                            val duration = item.videoDuration
                            if (duration > TimeUnit.HOURS.toMillis(1)) {
                                String.format(
                                    "%02d:%02d:%02d",
                                    TimeUnit.MILLISECONDS.toHours(duration),
                                    TimeUnit.MILLISECONDS.toMinutes(duration) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(duration)),
                                    TimeUnit.MILLISECONDS.toSeconds(duration) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(duration))
                                )
                            } else {
                                String.format(
                                    "%02d:%02d",
                                    TimeUnit.MILLISECONDS.toMinutes(duration),
                                    TimeUnit.MILLISECONDS.toSeconds(duration) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(duration))
                                )
                            }
                        }

                        GlideImage(
                            imageModel = item.assetFileStringUri.orEmpty(),
                            contentDescription = "",
                            contentScale = ContentScale.Crop,
                            requestBuilder = Glide.with(LocalView.current)
                                .asDrawable()
                                .thumbnail(0.5f)
                                .transform(GranularRoundedCorners(0f, 15f, 15f, 0f)),
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(
                                    with(LocalDensity.current) { 480.toDp() },
                                    with(LocalDensity.current) { 360.toDp() }
                                ),
                            failure = { Text(text = "image request failed.") }
                        )

                        Text(
                            runTimeString,
                            color = Color.White,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .background(Color(0x99000000))
                                .border(BorderStroke(1.dp, Color(0x00000000)), shape = RoundedCornerShape(bottomEnd = 5.dp))
                        )

                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 16.dp, top = 4.dp)
                    ) {
                        val shared = requireContext().getSharedPreferences("videos", Context.MODE_PRIVATE)
                        if (shared.contains(item.assetFileStringUri))
                            Text(shared.getLong(item.assetFileStringUri, 0).stringForTime(), style = MaterialTheme.typography.overline)
                        Text(item.videoName.orEmpty(), style = MaterialTheme.typography.subtitle2)
                        Text(item.path.orEmpty(), style = MaterialTheme.typography.body2, fontSize = 10.sp)
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.Top)
                            .padding(horizontal = 2.dp)
                    ) {

                        var showDropDown by remember { mutableStateOf(false) }

                        val dropDownDismiss = { showDropDown = false }

                        DropdownMenu(
                            expanded = showDropDown,
                            onDismissRequest = dropDownDismiss
                        ) {
                            DropdownMenuItem(
                                onClick = {
                                    dropDownDismiss()
                                    context?.deleteDialog(item) {}
                                }
                            ) { Text(stringResource(R.string.remove)) }
                        }

                        IconButton(onClick = { showDropDown = true }) { Icon(Icons.Default.MoreVert, null) }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        disposable.dispose()
        VideoGet.getInstance(requireContext())?.unregister()
    }

}