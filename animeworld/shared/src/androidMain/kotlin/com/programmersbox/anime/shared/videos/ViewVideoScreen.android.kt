package com.programmersbox.anime.shared.videos

import android.Manifest
import android.os.Build
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
import androidx.compose.ui.unit.dp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.programmersbox.kmpuiviews.utils.ComposableUtils
import com.programmersbox.kmpuiviews.utils.PermissionRequest
import androidx.core.net.toUri

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

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
internal actual fun VideoThumbnail(path: String, modifier: Modifier) {
    // Library paths are MediaStore content:// URIs; Glide decodes a frame from them via MediaMetadataRetriever.
    // Wrapping one in File() pointed Glide at a file that doesn't exist, so no thumbnail ever loaded.
    val model = remember(path) { path.toUri() }

    // Inlined from com.programmersbox.uiviews.presentation.components.GlideGradientImage
    // (UIViews module) rather than depending on it directly - UIViews is a flavored
    // (noFirebase/full) Android library module, and this unflavored KMP module's androidMain
    // can't unambiguously resolve which flavor variant to depend on.
    Box {
        GlideImage(
            model = model,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(3f) }),
            requestBuilderTransform = { it.override(ComposableUtils.IMAGE_HEIGHT_PX, ComposableUtils.IMAGE_WIDTH_PX) },
            modifier = Modifier
                .scale(1.5f)
                .blur(70.dp, BlurredEdgeTreatment.Unbounded)
                .alpha(.5f)
                .then(modifier)
        )

        GlideImage(
            model = model,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            requestBuilderTransform = { it.override(ComposableUtils.IMAGE_HEIGHT_PX, ComposableUtils.IMAGE_WIDTH_PX) },
            modifier = modifier
        )
    }
}
