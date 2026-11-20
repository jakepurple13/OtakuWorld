package com.programmersbox.manga.shared.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.BorderOuter
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.SwipeRight
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Pageview
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.programmersbox.datastore.mangasettings.ImageLoaderType
import com.programmersbox.datastore.mangasettings.PageIndicatorStyle
import com.programmersbox.datastore.mangasettings.ReaderType
import com.programmersbox.kmpuiviews.presentation.components.settings.CategoryGroup
import com.programmersbox.kmpuiviews.presentation.components.settings.SwitchSetting
import com.programmersbox.kmpuiviews.presentation.onboarding.HeroShape
import com.programmersbox.kmpuiviews.presentation.onboarding.OnboardingScope
import com.programmersbox.kmpuiviews.presentation.onboarding.OnboardingStepSpec
import com.programmersbox.kmpuiviews.presentation.onboarding.OnboardingTags
import com.programmersbox.kmpuiviews.presentation.onboarding.frame.StepLayout
import com.programmersbox.kmpuiviews.presentation.onboarding.motion.onboardingReducedMotion
import com.programmersbox.manga.shared.reader.PageIndicatorOverlay
import com.programmersbox.manga.shared.reader.label
import com.programmersbox.manga.shared.settings.workingImageLoaders
import com.programmersbox.mangasettings.MangaNewSettingsHandling
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

internal const val ReaderPaddingTag = "onb_reader_padding"
internal const val ReaderDrawerTag = "onb_reader_drawer"
internal const val ReaderPagesChipTag = "onb_reader_pages_chip"

private const val PaddingMax = 16f
private const val DrawerPeekMs = 1500

private val ReaderType.label: String
    get() = when (this) {
        ReaderType.List -> "List"
        ReaderType.Pager -> "Pager"
        ReaderType.FlipPager -> "Flip"
        ReaderType.CurlPager -> "Curl"
    }

private val ReaderType.summaryLabel: String
    get() = when (this) {
        ReaderType.List -> "List reader"
        ReaderType.Pager -> "Pager reader"
        ReaderType.FlipPager -> "Flip reader"
        ReaderType.CurlPager -> "Curl reader"
    }

/** Step 5 for MangaWorld: reader type, page gap, page number style, bar, gestures, insets and image loader. */
fun OnboardingScope.readerSetupStep(handling: MangaNewSettingsHandling) {
    val spec = OnboardingStepSpec(
        id = "app",
        title = "Set up the reader",
        body = "Pick how pages move. More options live in Settings › Reader.",
        icon = Icons.Default.AutoStories,
        shape = HeroShape.Clover,
        summaryLabel = "Reader",
        summary = {
            val type by handling.rememberReaderType()
            type.summaryLabel
        },
    )
    step(spec) { ReaderSetupContent(spec, handling) }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReaderSetupContent(spec: OnboardingStepSpec, handling: MangaNewSettingsHandling) {
    val scope = rememberCoroutineScope()
    var readerType by handling.rememberReaderType()
    var style by handling.rememberPageIndicatorStyle()
    var floatingBar by handling.rememberUseFloatingReaderBottomBar()
    var showPages by handling.rememberShowPageNumberInBar()
    var drawerGesture by handling.rememberUserGestureEnabled()
    var includeInsets by handling.rememberIncludeInsetsForReader()
    var imageLoader by handling.rememberImageLoaderType()

    var padding by remember { mutableFloatStateOf(4f) }
    LaunchedEffect(handling) { padding = handling.pagePadding.flow.first().toFloat().coerceIn(0f, PaddingMax) }

    var peekTrigger by remember { mutableIntStateOf(0) }

    Box(Modifier.fillMaxSize().testTag(OnboardingTags.step(spec.id))) {
        StepLayout(
            spec,
            preview = {
                ReaderPreview(
                    readerType = readerType,
                    gap = padding.toInt().dp,
                    style = style,
                    floatingBar = floatingBar,
                    showPages = showPages,
                    peekTrigger = peekTrigger,
                    includeInsets = includeInsets,
                )
            },
        ) {
            SectionLabel("Reader type")
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                ReaderType.entries.forEachIndexed { index, type ->
                    SegmentedButton(
                        selected = readerType == type,
                        onClick = { readerType = type },
                        shape = SegmentedButtonDefaults.itemShape(index, ReaderType.entries.size),
                        label = { Text(type.label) },
                    )
                }
            }

            SectionLabel("Space between pages · ${padding.toInt()}dp")
            Slider(
                value = padding,
                onValueChange = { padding = it },
                valueRange = 0f..PaddingMax,
                steps = PaddingMax.toInt() - 1,
                onValueChangeFinished = {
                    val value = padding.toInt()
                    scope.launch { handling.pagePadding.updateSetting(value) }
                },
                modifier = Modifier.testTag(ReaderPaddingTag),
            )

            SectionLabel("Page number")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                PageIndicatorStyle.entries.forEach { option ->
                    FilterChip(
                        selected = style == option,
                        onClick = { style = option },
                        label = { Text(option.label) },
                    )
                }
            }

            SectionLabel("Controls")
            CategoryGroup {
                item {
                    SwitchSetting(
                        value = floatingBar,
                        updateValue = { floatingBar = it },
                        settingTitle = { Text("Floating bottom bar") },
                        summaryValue = { Text("A pill instead of a full-width bar") },
                        settingIcon = { Icon(Icons.Default.Navigation, null, modifier = Modifier.fillMaxSize()) },
                    )
                }
                if (floatingBar) {
                    item {
                        SwitchSetting(
                            value = showPages,
                            updateValue = { showPages = it },
                            settingTitle = { Text("Pages button in the bar") },
                            summaryValue = { Text("Shows 3 / 24 and opens the page list") },
                            settingIcon = { Icon(Icons.Default.Pageview, null, modifier = Modifier.fillMaxSize()) },
                        )
                    }
                }
                item {
                    SwitchSetting(
                        value = drawerGesture,
                        updateValue = {
                            drawerGesture = it
                            if (it) peekTrigger++
                        },
                        settingTitle = { Text("Swipe for chapter list") },
                        summaryValue = { Text("Swipe in from the edge to open it") },
                        settingIcon = { Icon(Icons.Default.SwipeRight, null, modifier = Modifier.fillMaxSize()) },
                    )
                }
            }

            SectionLabel("Display")
            CategoryGroup {
                item {
                    SwitchSetting(
                        value = includeInsets,
                        updateValue = { includeInsets = it },
                        settingTitle = { Text("Include insets") },
                        summaryValue = { Text("Keep pages clear of the status and nav bars") },
                        settingIcon = { Icon(Icons.Default.BorderOuter, null, modifier = Modifier.fillMaxSize()) },
                    )
                }
            }

            // Same filtering as the Settings screen: loaders that do not work are not offered.
            val savedLoader = if (imageLoader in workingImageLoaders) imageLoader else ImageLoaderType.Kamel
            SectionLabel("Image loader")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                workingImageLoaders.forEach { option ->
                    FilterChip(
                        selected = savedLoader == option,
                        onClick = { imageLoader = option },
                        label = { Text(option.name) },
                        leadingIcon = { Icon(Icons.Default.Image, null, Modifier.size(18.dp)) },
                    )
                }
            }
            if (ImageLoaderType.Telephoto in workingImageLoaders || ImageLoaderType.Panpf in workingImageLoaders) {
                Text(
                    "Telephoto and Panpf add pinch-to-zoom.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp),
    )
}

private val PreviewHeight = 178.dp
private val BarHeight = 34.dp
private val PaperColor = Color(0xFFF4F1EA)

/**
 * Sticky live preview: sample pages turn the way [readerType] does with [gap] between them, the real
 * [PageIndicatorOverlay] draws [style], the bar docks or floats, and [peekTrigger] (when raised) peeks the
 * chapter drawer in once. Loops are static when reduced motion is on.
 */
@Composable
private fun ReaderPreview(
    readerType: ReaderType,
    gap: Dp,
    style: PageIndicatorStyle,
    floatingBar: Boolean,
    showPages: Boolean,
    peekTrigger: Int,
    includeInsets: Boolean,
) {
    val reduced = onboardingReducedMotion()
    val transition = rememberInfiniteTransition(label = "readerTurn")
    val looping by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            keyframes {
                durationMillis = 2800
                0f at 900 using FastOutSlowInEasing
                1f at 2000
            },
            RepeatMode.Restart,
        ),
        label = "phase",
    )
    val phase = { if (reduced) 0f else looping }

    val barSide by animateDpAsState(if (floatingBar) 28.dp else 0.dp, if (reduced) snap() else tween(300), label = "barSide")
    val barBottom by animateDpAsState(if (floatingBar) 8.dp else 0.dp, if (reduced) snap() else tween(300), label = "barBottom")
    val barCorner by animateDpAsState(if (floatingBar) 17.dp else 0.dp, if (reduced) snap() else tween(300), label = "barCorner")

    var peeking by remember { mutableStateOf(false) }
    val peek = remember { Animatable(0f) }
    LaunchedEffect(peekTrigger) {
        if (peekTrigger == 0) return@LaunchedEffect
        peeking = true
        if (reduced) {
            peek.snapTo(1f)
            delay(DrawerPeekMs.toLong())
        } else {
            peek.animateTo(1f, tween(300))
            delay(DrawerPeekMs - 600L)
            peek.animateTo(0f, tween(300))
        }
        peeking = false
        peek.snapTo(0f)
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF202124),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth().height(PreviewHeight).padding(top = 4.dp).clip(RoundedCornerShape(16.dp)),
    ) {
        Box(Modifier.fillMaxSize()) {
            // The status/nav bar insets show as thin strips the pages keep clear of
            val inset = if (includeInsets) 8.dp else 0.dp
            Box(Modifier.fillMaxSize().padding(top = inset, bottom = inset)) {
                SamplePages(readerType, gap, phase)
            }
            if (includeInsets) {
                Box(Modifier.fillMaxWidth().height(8.dp).background(Color.White.copy(alpha = 0.12f)))
                Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(8.dp).background(Color.White.copy(alpha = 0.12f)))
            }

            PageIndicatorOverlay(
                style = style,
                currentPage = 2,
                pages = 24,
                showItems = true,
                scrolling = false,
                topPadding = 0.dp,
                bottomPadding = BarHeight + barBottom,
                onOpenPages = {},
                onJumpToPage = {},
            )

            Surface(
                shape = RoundedCornerShape(barCorner),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shadowElevation = if (floatingBar) 6.dp else 0.dp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = barSide, end = barSide, bottom = barBottom)
                    .fillMaxWidth()
                    .height(BarHeight),
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    Icon(Icons.Default.Menu, null, Modifier.size(16.dp))
                    AnimatedVisibility(
                        visible = floatingBar && showPages,
                        enter = if (reduced) fadeIn() else scaleIn(initialScale = 0.4f) + fadeIn(),
                        exit = if (reduced) fadeOut() else scaleOut(targetScale = 0.4f) + fadeOut(),
                    ) {
                        Text(
                            "3 / 24",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier
                                .testTag(ReaderPagesChipTag)
                                .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
                                .padding(horizontal = 10.dp, vertical = 3.dp),
                        )
                    }
                    Icon(Icons.Default.Navigation, null, Modifier.size(16.dp))
                }
            }

            if (peeking) {
                val width = 96.dp
                Box(
                    Modifier
                        .testTag(ReaderDrawerTag)
                        .align(Alignment.CenterStart)
                        .offset(x = width * (peek.value - 1f))
                        .width(width)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .padding(10.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Chapters", style = MaterialTheme.typography.labelMedium)
                        repeat(3) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .background(MaterialTheme.colorScheme.outlineVariant, CircleShape)
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Pages that turn over once per loop: slide (Pager), swing (Flip), swing and fade (Curl), or scroll (List). */
@Composable
private fun SamplePages(readerType: ReaderType, gap: Dp, phase: () -> Float) {
    val density = LocalDensity.current
    BoxWithConstraints(Modifier.fillMaxSize().padding(vertical = 6.dp)) {
        val pageH = maxHeight
        val pageW = pageH * 0.68f
        val stepPx = with(density) { (pageW + gap).toPx() }
        val listStepPx = with(density) { (pageH * 0.8f + gap).toPx() }

        @Composable
        fun Page(modifier: Modifier = Modifier, height: Dp = pageH) {
            Box(
                modifier
                    .size(pageW, height)
                    .clip(RoundedCornerShape(3.dp))
                    .background(PaperColor)
            ) { SampleMangaPage() }
        }

        when (readerType) {
            ReaderType.List -> Column(
                verticalArrangement = Arrangement.spacedBy(gap),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .graphicsLayer { translationY = -phase() * listStepPx },
            ) { repeat(4) { Page(height = pageH * 0.8f) } }

            ReaderType.Pager -> {
                Page(Modifier.align(Alignment.Center).graphicsLayer { translationX = -phase() * stepPx })
                Page(Modifier.align(Alignment.Center).graphicsLayer { translationX = (1f - phase()) * stepPx })
            }

            ReaderType.FlipPager -> {
                Page(Modifier.align(Alignment.Center))
                Page(
                    Modifier
                        .align(Alignment.Center)
                        .graphicsLayer {
                            transformOrigin = TransformOrigin(0f, 0.5f)
                            cameraDistance = 12f * density.density
                            rotationY = -phase() * 90f
                            alpha = if (phase() >= 0.99f) 0f else 1f
                        }
                )
            }

            ReaderType.CurlPager -> {
                Page(Modifier.align(Alignment.Center))
                Page(
                    Modifier
                        .align(Alignment.Center)
                        .graphicsLayer {
                            transformOrigin = TransformOrigin(0f, 0.5f)
                            cameraDistance = 12f * density.density
                            rotationY = -phase() * 40f
                            translationX = -phase() * stepPx * 0.6f
                            alpha = 1f - phase()
                        }
                )
            }
        }
    }
}

/** Placeholder manga page for the reader tip demo: a few panel outlines. */
@Composable
fun BoxScope.SampleMangaPage() {
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .matchParentSize()
            .padding(10.dp)
    ) {
        listOf(0.35f, 0.25f, 0.4f).forEach { weight ->
            Box(
                Modifier
                    .weight(weight)
                    .fillMaxWidth()
                    .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(2.dp))
            )
        }
    }
}
