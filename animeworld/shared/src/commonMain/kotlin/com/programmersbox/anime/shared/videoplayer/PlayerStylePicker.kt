package com.programmersbox.anime.shared.videoplayer

import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.Theaters
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.programmersbox.datastore.asState
import com.programmersbox.kmpuiviews.presentation.components.settings.PreferenceSetting
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

/** The platform's own controls on the preview's bars, so the preview looks like that platform's player. */
class PlayerPreviewExtras(
    val designWidth: Dp,
    val top: @Composable RowScope.() -> Unit,
    val bottom: @Composable RowScope.() -> Unit = {},
)

object PlayerPreviewDefaults {
    val Android = PlayerPreviewExtras(
        designWidth = 720.dp,
        top = {
            PreviewIcon(Icons.Default.Cast)
            PreviewIcon(Icons.Default.ClosedCaption)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
                Icon(Icons.Default.BatteryFull, null)
                Text("84%", style = MaterialTheme.typography.bodyLarge)
            }
        },
    )

    val Desktop = PlayerPreviewExtras(
        designWidth = 640.dp,
        top = {
            PreviewIcon(Icons.Default.ClosedCaption)
            PreviewIcon(Icons.Default.Keyboard)
        },
        bottom = {
            PreviewIcon(Icons.AutoMirrored.Filled.VolumeUp)
            Box(
                Modifier
                    .width(72.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Brush.horizontalGradient(0f to MaterialTheme.colorScheme.primary, .75f to MaterialTheme.colorScheme.primary, .75f to Color.White.copy(alpha = .25f)))
            )
            PreviewIcon(Icons.Default.Fullscreen)
        },
    )
}

@Composable
private fun PreviewIcon(icon: ImageVector) {
    Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) { Icon(icon, null) }
}

val PlayerStyle.icon: ImageVector
    get() = when (this) {
        PlayerStyle.Classic -> Icons.Default.ViewAgenda
        PlayerStyle.Floating -> Icons.Default.PictureInPictureAlt
        PlayerStyle.Theater -> Icons.Default.Theaters
    }

private val PlayerStyle.tagline: String
    get() = when (this) {
        PlayerStyle.Classic -> "Default"
        PlayerStyle.Floating -> "Most video"
        PlayerStyle.Theater -> "Cinematic"
    }

private val PreviewState = PlayerChromeState(
    title = "Sample episode",
    subtitle = "Episode 7 · Player preview",
    positionMs = 342_000,
    durationMs = 1_428_000,
    bufferedMs = 460_000,
    isPlaying = true,
)

private val PreviewActions = PlayerChromeActions(
    onPlayPause = {},
    onRewind = {},
    onFastForward = {},
    onSkip = {},
    onSeek = {},
    onSpeed = {},
)

private const val PREVIEW_HEIGHT = 360f

/**
 * The real [PlayerChrome] in [style] over a looping sample frame, laid out at player size and
 * scaled down to fit, with the user's blur settings. A style change plays the old bars out and the
 * new bars in. When [animate], the bars show and hide on a loop; when [toggleOnClick], a click
 * toggles them (otherwise clicks go to the parent, e.g. a selectable card).
 */
@Composable
fun PlayerStylePreview(
    style: PlayerStyle,
    extras: PlayerPreviewExtras,
    modifier: Modifier = Modifier,
    animate: Boolean = true,
    toggleOnClick: Boolean = animate,
    shape: Shape = RoundedCornerShape(14.dp),
) {
    val blurOn by videoPlayerBlur.asState()
    val blur = rememberPlayerBlur(blurOn)
    var shownStyle by remember { mutableStateOf(style) }
    var visible by remember { mutableStateOf(true) }
    var interactions by remember { mutableIntStateOf(0) }

    LaunchedEffect(style) {
        if (style == shownStyle) return@LaunchedEffect
        visible = false
        delay(300)
        shownStyle = style
        delay(30)
        visible = true
        interactions++
    }

    if (animate) {
        // Restarts on every click or style change, so the bars hold a while after one.
        LaunchedEffect(interactions) {
            delay(if (interactions == 0) 2600 else 4000)
            while (true) {
                visible = !visible
                delay(if (visible) 2600 else 1300)
            }
        }
    }

    val designWidth = extras.designWidth
    BoxWithConstraints(
        modifier
            .aspectRatio(designWidth.value / PREVIEW_HEIGHT)
            .clip(shape)
            .background(Color.Black)
    ) {
        val scale = maxWidth / designWidth
        Box(
            Modifier
                .wrapContentSize(Alignment.TopStart, unbounded = true)
                .requiredSize(designWidth, PREVIEW_HEIGHT.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    transformOrigin = TransformOrigin(0f, 0f)
                }
        ) {
            SampleFrame(
                animate = animate,
                modifier = Modifier
                    .fillMaxSize()
                    .playerBlurSource(blur, shownStyle)
            )
            PlayerChrome(
                style = shownStyle,
                visible = visible,
                state = PreviewState,
                actions = PreviewActions,
                blur = blur,
                navigationIcon = { PreviewIcon(Icons.AutoMirrored.Filled.ArrowBack) },
                topActions = extras.top,
                bottomActions = extras.bottom,
            )
        }
        // Covers the bars so their buttons (Back included) can't be pressed in a preview.
        Box(
            Modifier
                .matchParentSize()
                .then(
                    if (toggleOnClick) {
                        Modifier.clickable(interactionSource = null, indication = null) {
                            visible = !visible
                            interactions++
                        }
                    } else {
                        Modifier.pointerInput(Unit) {}
                    }
                )
        )
    }
}

/** A dusk landscape that drifts, so blur over it is visible. Time is read only while drawing. */
@Composable
private fun SampleFrame(animate: Boolean, modifier: Modifier = Modifier) {
    val time = remember { mutableFloatStateOf(340f) }
    if (animate) {
        LaunchedEffect(Unit) {
            var last = 0L
            while (true) {
                withFrameNanos { now ->
                    if (last != 0L) time.floatValue += (now - last) / 1_000_000_000f
                    last = now
                }
            }
        }
    }
    val path = remember { Path() }
    Canvas(modifier) {
        val t = time.floatValue
        val w = size.width
        val h = size.height
        drawRect(
            Brush.verticalGradient(
                0f to Color(0xFF2B3A78),
                .65f to Color(0xFFE59A6A),
                1f to Color(0xFF8A3A5C),
            )
        )
        val sun = Offset(w * (.15f + .7f * ((t / 90f) % 1f)), h * (.32f + .08f * sin(t / 20f)))
        drawCircle(
            Brush.radialGradient(
                listOf(Color(0xF2FFF0C8), Color(0xD9FFDCA0), Color(0x00FFC896)),
                center = sun,
                radius = h * .28f,
            ),
            radius = h * .28f,
            center = sun,
        )
        val layers = listOf(
            Triple(.58f, Color(0xFF5B4A82), 8f),
            Triple(.68f, Color(0xFF3E2C5C), 18f),
            Triple(.80f, Color(0xFF2A1838), 40f),
        )
        layers.forEachIndexed { i, (base, color, speed) ->
            val length = 180f - i * 50f
            val amp = (.07f - i * .01f) * h
            path.reset()
            path.moveTo(0f, h)
            var x = 0f
            while (x <= w) {
                val u = x / w * 1000f + t * speed
                path.lineTo(x, h * base + sin(u / length) * amp + sin(u / (length * .37f)) * amp * .4f)
                x += w / 60f
            }
            path.lineTo(w, h)
            path.close()
            drawPath(path, color)
        }
        repeat(30) { k ->
            val px = ((k * 137 + t * (22 + k % 5 * 9)) % 1040f) / 1000f * w - 20f
            val py = ((k * 89 + t * (30 + k % 4 * 7)) % 1040f) / 1000f * h - 20f
            val r = (w / 320f).coerceAtLeast(2f) * (1 + k % 3 * .4f)
            drawCircle(Color(0xE6FFBED7), radius = r, center = Offset(px, py + sin((t + k) * PI.toFloat() / 3) * 4f))
        }
    }
}

/**
 * Desktop's picker: a live preview above a connected button group, with the selected style's
 * description under it. Clicking the preview toggles its bars.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PlayerStyleInlinePicker(
    extras: PlayerPreviewExtras,
    modifier: Modifier = Modifier,
) {
    var style by videoPlayerStyle.asState()
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Player style", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text("Click the preview to toggle the bars", style = MaterialTheme.typography.labelMedium)
        }
        PlayerStylePreview(style, extras, Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
            val entries = PlayerStyle.entries
            entries.forEachIndexed { index, option ->
                ToggleButton(
                    checked = option == style,
                    onCheckedChange = { style = option },
                    shapes = when (index) {
                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                        entries.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .semantics { role = Role.RadioButton },
                ) {
                    Icon(option.icon, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(option.label)
                }
            }
        }
        AnimatedContent(
            targetState = style,
            transitionSpec = { (slideInVertically { it / 2 } + fadeIn()) togetherWith (slideOutVertically { -it / 2 } + fadeOut()) },
            label = "styleDescription",
        ) {
            Text(it.description, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/**
 * Android's picker: a settings row with a small live thumbnail of the current style; tapping it
 * opens a sheet with a swipeable preview of each style.
 */
@Composable
fun PlayerStyleSheetSetting(
    extras: PlayerPreviewExtras,
    modifier: Modifier = Modifier,
) {
    var style by videoPlayerStyle.asState()
    var open by remember { mutableStateOf(false) }
    PreferenceSetting(
        settingTitle = { Text("Player style") },
        summaryValue = { Text(style.label) },
        settingIcon = { Icon(Icons.Default.Dashboard, null, modifier = Modifier.fillMaxSize()) },
        endIcon = {
            PlayerStylePreview(
                style = style,
                extras = extras,
                animate = false,
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.width(80.dp),
            )
        },
        onClick = { open = true },
        modifier = modifier,
    )
    if (open) {
        PlayerStyleSheet(
            current = style,
            extras = extras,
            onDismiss = { open = false },
            onConfirm = {
                style = it
                open = false
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerStyleSheet(
    current: PlayerStyle,
    extras: PlayerPreviewExtras,
    onDismiss: () -> Unit,
    onConfirm: (PlayerStyle) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = current.ordinal) { PlayerStyle.entries.size }
    var selected by remember { mutableStateOf(current) }
    LaunchedEffect(pagerState.settledPage) { selected = PlayerStyle.entries[pagerState.settledPage] }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Text(
                "Player style",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Text(
                "Swipe to compare. Each preview plays its own bar animation.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            HorizontalPager(
                state = pagerState,
                contentPadding = PaddingValues(horizontal = 32.dp),
                pageSpacing = 8.dp,
            ) { page ->
                val option = PlayerStyle.entries[page]
                StyleCard(
                    style = option,
                    selected = option == selected,
                    extras = extras,
                    onClick = {
                        selected = option
                        scope.launch { pagerState.animateScrollToPage(page) }
                    },
                )
            }
            PageDots(pagerState, Modifier.align(Alignment.CenterHorizontally))
            Button(
                onClick = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion { onConfirm(selected) }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) { Text("Use ${selected.label}") }
        }
    }
}

@Composable
private fun StyleCard(style: PlayerStyle, selected: Boolean, extras: PlayerPreviewExtras, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.semantics { role = Role.RadioButton },
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(8.dp)
        ) {
            PlayerStylePreview(style, extras, Modifier.fillMaxWidth(), animate = true, toggleOnClick = false)
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = selected, onClick = null, modifier = Modifier.padding(horizontal = 8.dp))
                Text(style.label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text(style.tagline, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(end = 8.dp))
            }
        }
    }
}

@Composable
private fun PageDots(state: PagerState, modifier: Modifier = Modifier) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = modifier) {
        repeat(state.pageCount) { page ->
            val current = state.currentPage == page
            val width by animateDpAsState(if (current) 18.dp else 6.dp, label = "dot")
            Box(
                Modifier
                    .size(width, 6.dp)
                    .clip(CircleShape)
                    .background(if (current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
            )
        }
    }
}
