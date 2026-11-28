package com.programmersbox.kmpuiviews.tabs

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.animateBounds
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import com.programmersbox.kmpuiviews.presentation.Screen
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.tabs_home_section
import otakuworld.kmpuiviews.generated.resources.tabs_open_in_tab_section
import otakuworld.kmpuiviews.generated.resources.tabs_open_page_in_new_tab
import otakuworld.kmpuiviews.generated.resources.tabs_sidebar_collapse
import otakuworld.kmpuiviews.generated.resources.tabs_sidebar_expand

data class SidebarItem(val key: NavKey, val label: StringResource, val icon: ImageVector)

private val ExpandedWidth = 224.dp
private val CollapsedWidth = 64.dp
private val RowHeight = 40.dp
private val IconStart = 14.dp
private val EdgeFade = 24.dp

/**
 * The Home tab's sidebar. [homeItems] navigate inside Home ([onHome]) and offer an open-in-new-tab button on hover
 * or focus; [tabItems] always open a new tab ([onOpenTab]). Collapsed, it shrinks to icons with tooltips, and the
 * Notifications badge moves from the row end onto the bell.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeSidebar(
    homeItems: List<SidebarItem>,
    tabItems: List<SidebarItem>,
    selected: (NavKey) -> Boolean,
    notificationCount: Int,
    collapsed: Boolean,
    onToggleCollapsed: () -> Unit,
    onHome: (NavKey) -> Unit,
    onOpenTab: (NavKey) -> Unit,
    modifier: Modifier = Modifier,
) {
    val width by animateDpAsState(
        if (collapsed) CollapsedWidth else ExpandedWidth,
        MaterialTheme.motionScheme.defaultSpatialSpec(),
        label = "sidebarWidth",
    )
    val contentAlpha by animateFloatAsState(
        if (collapsed) 0f else 1f,
        MaterialTheme.motionScheme.fastEffectsSpec(),
        label = "sidebarContent",
    )
    val listState = rememberLazyListState()

    LookaheadScope {
        val lookahead = this
        Column(
            modifier
                .width(width)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surface)
                .padding(vertical = 8.dp),
        ) {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(horizontal = 8.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .edgeFade(listState),
            ) {
                item(key = "header-home") {
                    SectionHeader(Res.string.tabs_home_section, first = true, contentAlpha, Modifier.animateItem())
                }
                items(homeItems, key = { "home-${it.key}" }) { item ->
                    SidebarRow(
                        item = item,
                        selected = selected(item.key),
                        collapsed = collapsed,
                        contentAlpha = contentAlpha,
                        lookahead = lookahead,
                        onClick = { onHome(item.key) },
                        onNewTab = { onOpenTab(item.key) },
                        modifier = Modifier.animateItem(),
                    )
                }
                item(key = "header-tabs") {
                    SectionHeader(Res.string.tabs_open_in_tab_section, first = false, contentAlpha, Modifier.animateItem())
                }
                items(tabItems, key = { "tab-${it.key}" }) { item ->
                    SidebarRow(
                        item = item,
                        selected = false,
                        collapsed = collapsed,
                        contentAlpha = contentAlpha,
                        lookahead = lookahead,
                        onClick = { onOpenTab(item.key) },
                        opensTab = true,
                        badgeCount = if (item.key == Screen.NotificationScreen) {
                            notificationCount
                        } else {
                            0
                        },
                        modifier = Modifier.animateItem(),
                    )
                }
            }

            CollapseRow(collapsed, contentAlpha, onToggleCollapsed)
        }
    }
}

/** Expanded: the title. Collapsed: a thin divider, or nothing for the [first] section. */
@Composable
private fun SectionHeader(title: StringResource, first: Boolean, contentAlpha: Float, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth()) {
        when {
            contentAlpha > 0f -> Text(
                stringResource(title),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
                modifier = Modifier
                    .padding(start = IconStart, top = if (first) 4.dp else 12.dp, bottom = 4.dp)
                    .alpha(contentAlpha),
            )

            !first -> HorizontalDivider(Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun SidebarRow(
    item: SidebarItem,
    selected: Boolean,
    collapsed: Boolean,
    contentAlpha: Float,
    lookahead: LookaheadScope,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onNewTab: (() -> Unit)? = null,
    opensTab: Boolean = false,
    badgeCount: Int = 0,
) {
    val label = stringResource(item.label)
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    var focused by remember { mutableStateOf(false) }
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    SidebarTooltip(label, enabled = collapsed, modifier = modifier) {
        // The badge sits outside the clipped, clickable pill so it can overhang the bell when collapsed.
        Box(Modifier.fillMaxWidth().height(RowHeight)) {
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
                    .onFocusChanged { focused = it.hasFocus }
                    .hoverable(interaction)
                    .clickable(interactionSource = interaction, indication = ripple(), onClick = onClick),
            ) {
                Row(
                    Modifier
                        .fillMaxSize()
                        .padding(start = IconStart, end = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        item.icon,
                        // Collapsed, the icon is all there is, so it carries the label.
                        contentDescription = if (contentAlpha > 0f) null else label,
                        tint = contentColor,
                        modifier = Modifier.size(20.dp),
                    )
                    if (contentAlpha > 0f) {
                        Spacer(Modifier.width(12.dp))
                        Text(
                            label,
                            color = contentColor,
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Clip,
                            modifier = Modifier
                                .weight(1f)
                                .alpha(contentAlpha),
                        )
                        if (onNewTab != null && !collapsed && (hovered || focused)) {
                            IconButton(
                                onClick = onNewTab,
                                modifier = Modifier
                                    .size(28.dp)
                                    .alpha(contentAlpha),
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Default.OpenInNew,
                                    contentDescription = stringResource(Res.string.tabs_open_page_in_new_tab, label),
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                        if (opensTab && badgeCount <= 0) {
                            Icon(
                                Icons.AutoMirrored.Default.OpenInNew,
                                contentDescription = null,
                                tint = contentColor,
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .size(14.dp)
                                    .alpha(contentAlpha),
                            )
                        }
                    }
                }
            }

            if (badgeCount > 0) {
                // One badge that glides between the row end (expanded) and the bell's top-right (collapsed).
                val place = if (collapsed) {
                    Modifier
                        .align(Alignment.TopStart)
                        .offset(x = IconStart + 10.dp, y = 4.dp)
                } else {
                    Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 10.dp)
                }
                Badge(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                    modifier = place.animateBounds(
                        lookahead,
                        Modifier.size(if (collapsed) 18.dp else 26.dp, if (collapsed) 16.dp else 18.dp),
                    ),
                ) {
                    AnimatedContent(
                        targetState = badgeCount,
                        transitionSpec = {
                            (slideInVertically { -it } + fadeIn()) togetherWith (slideOutVertically { it } + fadeOut())
                        },
                        label = "badgeCount",
                    ) { count -> Text(if (count > 99) "99+" else count.toString()) }
                }
            }
        }
    }
}

@Composable
private fun CollapseRow(collapsed: Boolean, contentAlpha: Float, onToggle: () -> Unit) {
    val description = stringResource(if (collapsed) Res.string.tabs_sidebar_expand else Res.string.tabs_sidebar_collapse)
    SidebarTooltip(description, enabled = collapsed, modifier = Modifier.padding(horizontal = 8.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(RowHeight)
                .clip(CircleShape)
                .clickable(onClick = onToggle)
                .padding(start = IconStart),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                if (collapsed) Icons.Default.ChevronRight else Icons.Default.ChevronLeft,
                contentDescription = description,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
            if (contentAlpha > 0f) {
                Spacer(Modifier.width(12.dp))
                Text(
                    stringResource(Res.string.tabs_sidebar_collapse),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier.alpha(contentAlpha),
                )
            }
        }
    }
}

/** A plain tooltip with [label], shown only while [enabled] (the sidebar is collapsed). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SidebarTooltip(label: String, enabled: Boolean, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.End),
        tooltip = { PlainTooltip { Text(label) } },
        state = rememberTooltipState(),
        enableUserInput = enabled,
        modifier = modifier,
        content = content,
    )
}

/** Fades the top and bottom [EdgeFade] of the list, each only while there is more to scroll that way. */
private fun Modifier.edgeFade(state: LazyListState): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        val fade = EdgeFade.toPx()
        if (state.canScrollBackward) {
            drawRect(
                brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Black), startY = 0f, endY = fade),
                size = Size(size.width, fade),
                blendMode = BlendMode.DstIn,
            )
        }
        if (state.canScrollForward) {
            drawRect(
                brush = Brush.verticalGradient(listOf(Color.Black, Color.Transparent), startY = size.height - fade, endY = size.height),
                topLeft = Offset(0f, size.height - fade),
                size = Size(size.width, fade),
                blendMode = BlendMode.DstIn,
            )
        }
    }
