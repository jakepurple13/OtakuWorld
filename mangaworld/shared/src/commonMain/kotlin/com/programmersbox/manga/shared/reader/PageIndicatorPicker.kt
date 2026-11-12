package com.programmersbox.manga.shared.reader

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.programmersbox.datastore.mangasettings.PageIndicatorStyle
import com.programmersbox.kmpuiviews.presentation.components.settings.CategorySetting
import com.programmersbox.kmpuiviews.presentation.components.settings.SwitchSetting
import com.programmersbox.mangasettings.MangaNewSettingsHandling
import kotlinx.coroutines.delay

private const val PREVIEW_PAGES = 24
private val PreviewWidth = 280.dp
private val PreviewHeight = 590.dp

/**
 * Row of page number styles, each card a looping mini reader playing its style. Picking a card calls [onSelect].
 */
@Composable
internal fun PageIndicatorPicker(
    selected: PageIndicatorStyle,
    onSelect: (PageIndicatorStyle) -> Unit,
    modifier: Modifier = Modifier,
    previewScale: Float = .27f,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
) {
    LazyRow(
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        itemsIndexed(PageIndicatorStyle.entries, key = { _, style -> style.name }) { i, style ->
            PageIndicatorCard(
                style = style,
                selected = style == selected,
                onClick = { onSelect(style) },
                scale = previewScale,
                startPage = (i * 5) % PREVIEW_PAGES,
                modifier = Modifier.animateItem()
            )
        }
    }
}

@Composable
private fun PageIndicatorCard(
    style: PageIndicatorStyle,
    selected: Boolean,
    onClick: () -> Unit,
    scale: Float,
    startPage: Int,
    modifier: Modifier = Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary
    val outline by animateColorAsState(if (selected) primary else Color.Transparent)
    val check by animateFloatAsState(
        if (selected) 1f else 0f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier.selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
    ) {
        Box {
            PageIndicatorPreview(
                style = style,
                scale = scale,
                startPage = startPage,
                modifier = Modifier
                    .border(2.dp, outline, PreviewShape)
                    .padding(4.dp)
            )
            // Swallows taps so they pick the card instead of reaching the preview's own controls
            Box(
                Modifier
                    .matchParentSize()
                    .clickable(interactionSource = null, indication = null, onClick = onClick)
            )
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 6.dp, y = (-6).dp)
                    .graphicsLayer {
                        scaleX = check
                        scaleY = check
                    }
                    .background(primary, CircleShape)
                    .padding(3.dp)
                    .size(14.dp)
            )
        }
        Text(
            style.label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * A scaled-down reader that turns pages on a loop and shows the bars now and then, so [style] can be seen in
 * both states.
 */
@Composable
internal fun PageIndicatorPreview(
    style: PageIndicatorStyle,
    scale: Float,
    modifier: Modifier = Modifier,
    startPage: Int = 0,
) {
    var page by remember { mutableIntStateOf(startPage) }
    var showItems by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        var tick = 0
        while (true) {
            delay(1900)
            tick++
            page = (page + 1) % PREVIEW_PAGES
            showItems = tick % 4 == 3
        }
    }
    val topPadding by animateDpAsState(if (showItems) 64.dp else 0.dp)
    val bottomPadding by animateDpAsState(if (showItems) 96.dp else 0.dp)

    Box(
        modifier
            .size(PreviewWidth * scale, PreviewHeight * scale)
            .clip(PreviewShape)
    ) {
        Box(
            Modifier
                .requiredSize(PreviewWidth, PreviewHeight)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
        ) {
            AnimatedContent(
                targetState = page,
                transitionSpec = { (slideInHorizontally { it / 3 } + fadeIn()) togetherWith fadeOut() },
            ) { MockPage(it) }

            AnimatedVisibility(
                visible = showItems,
                enter = slideInVertically { -it } + fadeIn(),
                exit = slideOutVertically { -it } + fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                )
            }
            AnimatedVisibility(
                visible = showItems,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Box(
                    Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                        .height(64.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.extraLarge)
                )
            }

            PageIndicatorOverlay(
                style = style,
                currentPage = page,
                pages = PREVIEW_PAGES,
                showItems = showItems,
                scrolling = false,
                topPadding = topPadding,
                bottomPadding = bottomPadding,
                onOpenPages = {},
                onJumpToPage = {},
            )
        }
    }
}

/** Placeholder manga page: a few bordered panels whose layout shifts with [page]. */
@Composable
private fun MockPage(page: Int) {
    val ink = Color(0xFF111111)
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF6F3EC))
            .padding(horizontal = 12.dp, vertical = 18.dp)
    ) {
        val rows = MockLayouts[page % MockLayouts.size]
        rows.forEachIndexed { r, (weight, columns) ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(weight)
            ) {
                repeat(columns) { c ->
                    val dark = (page + r * 3 + c) % 7 == 0
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .background(if (dark) ink else Color.White)
                            .border(3.dp, ink)
                    )
                }
            }
        }
    }
}

private val MockLayouts = listOf(
    listOf(1.1f to 1, 1f to 2, 1.3f to 1),
    listOf(1f to 2, 1f to 2, 1f to 1),
    listOf(1.4f to 1, 1f to 2, 1f to 2),
    listOf(1f to 2, 1.6f to 1, .9f to 2),
)

private val PreviewShape = RoundedCornerShape(12.dp)

/** "Page number" settings shared by Reader Settings and the in-reader settings sheet. */
@Composable
internal fun PageNumberSettings(
    mangaSettingsHandling: MangaNewSettingsHandling,
    previewScale: Float,
    modifier: Modifier = Modifier,
    showLargePreview: Boolean = false,
) {
    var style by mangaSettingsHandling.rememberPageIndicatorStyle()
    var showInBar by mangaSettingsHandling.rememberShowPageNumberInBar()
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxWidth()
    ) {
        CategorySetting(settingTitle = { Text("Page number") })
        if (showLargePreview) {
            PageIndicatorPreview(
                style = style,
                scale = .5f,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        PageIndicatorPicker(
            selected = style,
            onSelect = { style = it },
            previewScale = previewScale,
        )
        SwitchSetting(
            value = showInBar,
            updateValue = { showInBar = it },
            settingTitle = { Text("Page number in floating bar") },
            summaryValue = { Text("Adds a Pages item to the floating bar. Tapping it opens the page grid.") },
            settingIcon = { Icon(Icons.Default.Numbers, null, modifier = Modifier.fillMaxSize()) },
        )
    }
}
