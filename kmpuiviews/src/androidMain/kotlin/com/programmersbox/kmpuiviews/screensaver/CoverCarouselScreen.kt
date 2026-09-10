package com.programmersbox.kmpuiviews.screensaver

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.programmersbox.favoritesdatabase.NotificationItem
import com.programmersbox.kmpuiviews.utils.composables.imageloaders.ImageLoaderChoice
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Duration.Companion.seconds

private val CAROUSEL_INTERVAL = 10.seconds

@Composable
fun CoverCarouselScreen(
    viewModel: CoverCarouselViewModel = koinViewModel(),
) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val physicalOrientation by rememberPhysicalDeviceOrientation()

    var currentIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(items) {
        if (items.isEmpty()) return@LaunchedEffect
        while (isActive) {
            delay(CAROUSEL_INTERVAL)
            currentIndex = (currentIndex + 1) % items.size
        }
    }

    Scaffold { _ ->
        SensorRotatedLayout(physicalOrientation = physicalOrientation) {
            Box(modifier = Modifier.fillMaxSize()) {
                val item = items.getOrNull(currentIndex % items.size.coerceAtLeast(1))
                if (item != null) {
                    AnimatedContent(
                        targetState = item,
                        transitionSpec = { fadeIn(tween(1500)) togetherWith fadeOut(tween(1500)) },
                        modifier = Modifier.fillMaxSize()
                    ) { target ->
                        KenBurnsCover(item = target)
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Nothing saved yet", style = MaterialTheme.typography.headlineSmall)
                    }
                }

                DateBatteryCard(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            }
        }
    }
}

@Composable
private fun KenBurnsCover(item: NotificationItem) {
    val scale = remember(item.url) { Animatable(1f) }

    LaunchedEffect(item.url) {
        scale.snapTo(1f)
        scale.animateTo(
            targetValue = 1.15f,
            animationSpec = tween(
                durationMillis = CAROUSEL_INTERVAL.inWholeMilliseconds.toInt(),
                easing = LinearEasing
            )
        )
    }

    ImageLoaderChoice(
        imageUrl = item.imageUrl.orEmpty(),
        name = item.notiTitle,
        placeHolder = { rememberVectorPainter(Icons.Default.Settings) },
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
    )
}
