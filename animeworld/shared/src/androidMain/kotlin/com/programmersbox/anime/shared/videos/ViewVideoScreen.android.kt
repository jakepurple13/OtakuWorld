package com.programmersbox.anime.shared.videos

import android.Manifest
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.ImageLoader
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.video.VideoFrameDecoder
import coil3.video.videoFramePercent
import com.programmersbox.kmpuiviews.utils.ComposableUtils
import com.programmersbox.kmpuiviews.utils.PermissionRequest

@Composable
internal actual fun VideoPermissionGate(content: @Composable () -> Unit) {
    PermissionRequest(
        if (Build.VERSION.SDK_INT >= 33)
            listOf(Manifest.permission.READ_MEDIA_VIDEO)
        else listOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
        ),
        content = content,
    )
}

@Composable
internal actual fun VideoThumbnail(path: String, modifier: Modifier) {
    val context = LocalContext.current

    val coilImageLoader = remember {
        ImageLoader(context).newBuilder()
            .components {
                add(VideoFrameDecoder.Factory())
            }
            .build()
    }

    val model = rememberAsyncImagePainter(
        model = ImageRequest.Builder(context)
            .data(path)
            .crossfade(true)
            .size(ComposableUtils.IMAGE_HEIGHT_PX, ComposableUtils.IMAGE_WIDTH_PX)
            .videoFramePercent(.1)
            .build(),
        imageLoader = coilImageLoader
    )

    // Inlined from com.programmersbox.uiviews.presentation.components.CoilGradientImage
    // (UIViews module) rather than depending on it directly - UIViews is a flavored
    // (noFirebase/full) Android library module, and this unflavored KMP module's androidMain
    // can't unambiguously resolve which flavor variant to depend on.
    Box {
        if (model.state.collectAsStateWithLifecycle().value is AsyncImagePainter.State.Success) {
            Image(
                painter = model,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(3f) }),
                modifier = Modifier
                    .scale(1.5f)
                    .blur(70.dp, BlurredEdgeTreatment.Unbounded)
                    .alpha(.5f)
                    .then(modifier)
            )
        }

        Image(
            painter = model,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    }
}
