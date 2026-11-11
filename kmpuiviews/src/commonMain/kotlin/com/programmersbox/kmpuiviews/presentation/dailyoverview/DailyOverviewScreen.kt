package com.programmersbox.kmpuiviews.presentation.dailyoverview

import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.programmersbox.kmpuiviews.domain.dailyoverview.DailyFacts
import com.programmersbox.kmpuiviews.domain.dailyoverview.TitleRead
import com.programmersbox.kmpuiviews.domain.dailyoverview.UpdateItem
import com.programmersbox.kmpuiviews.painterLogo
import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.kmpuiviews.presentation.dailyoverview.components.HourGraph
import com.programmersbox.kmpuiviews.presentation.dailyoverview.components.StatTiles
import com.programmersbox.kmpuiviews.presentation.dailyoverview.components.SummaryCard
import com.programmersbox.kmpuiviews.presentation.dailyoverview.components.peakHourLabel
import com.programmersbox.kmpuiviews.utils.LocalNavActions
import com.programmersbox.kmpuiviews.utils.LocalNavHostPadding
import com.programmersbox.kmpuiviews.utils.LocalSourcesRepository
import com.programmersbox.kmpuiviews.utils.composables.imageloaders.ImageLoaderChoice
import com.programmersbox.kmpuiviews.utils.composables.sharedelements.LocalNavigationAnimatedScope
import com.programmersbox.kmpuiviews.utils.composables.sharedelements.LocalSharedElementScope
import com.programmersbox.sharedcomponents.repository.ChangingSettingsRepository
import kotlinx.coroutines.delay
import kotlinx.datetime.LocalDate
import kotlinx.datetime.format.DayOfWeekNames
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.do_all_updates
import otakuworld.kmpuiviews.generated.resources.do_caught_up
import otakuworld.kmpuiviews.generated.resources.do_empty_day
import otakuworld.kmpuiviews.generated.resources.do_history
import otakuworld.kmpuiviews.generated.resources.do_read_range
import otakuworld.kmpuiviews.generated.resources.do_settings
import otakuworld.kmpuiviews.generated.resources.do_update_check_failed
import otakuworld.kmpuiviews.generated.resources.do_updated_overnight
import otakuworld.kmpuiviews.generated.resources.do_you_read
import otakuworld.kmpuiviews.generated.resources.do_yesterday

private val OverviewDate = LocalDate.Format {
    dayOfWeek(DayOfWeekNames.ENGLISH_FULL)
    chars(", ")
    monthName(MonthNames.ENGLISH_ABBREVIATED)
    char(' ')
    day(padding = Padding.NONE)
}

private const val UpdatesShown = 3

/** Shared between the quick actions row and this screen's root, so the row grows into the screen. */
const val DailyOverviewSharedKey = "daily-overview"

// Container transform timing from the approved mockup: 480ms, emphasized decelerate.
@OptIn(ExperimentalSharedTransitionApi::class)
internal val dailyOverviewBoundsTransform = BoundsTransform { _, _ ->
    tween(durationMillis = 480, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f))
}

// First-load entrance, matching the approved mockup: items rise 14dp and fade in from 180ms,
// 55ms apart; read-progress bars grow at 800ms.
private val EnterEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
private const val EnterDelayMs = 180L
private const val EnterStaggerMs = 55L
private const val FirstLoadMs = 1500L
private const val ProgressDelayMs = 800L

/**
 * Like HideNavBarWhileOnScreen, but from STARTED instead of RESUMED. NavDisplay holds an entry at
 * STARTED until its transition settles, and the container transform draws in the shared-element
 * overlay above the nav bar. Waiting for RESUMED let the nav reappear over the screen when the
 * transform landed, then slide away. Hiding from STARTED slides it out as the transform begins.
 */
@Composable
private fun HideNavBarFromTransitionStart() {
    val changingSettingsRepository: ChangingSettingsRepository = koinInject()
    LifecycleStartEffect(Unit) {
        changingSettingsRepository.showNavBar.tryEmit(false)
        onStopOrDispose { changingSettingsRepository.showNavBar.tryEmit(true) }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun DailyOverviewScreen(
    viewModel: DailyOverviewViewModel = koinViewModel(),
) {
    HideNavBarFromTransitionStart()
    val navActions = LocalNavActions.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val facts = state.facts
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val sharedScope = LocalSharedElementScope.current
    val navScope = LocalNavigationAnimatedScope.current
    // Grows out of the quick actions row; outside a nav entry it just appears
    val sharedBounds = if (sharedScope != null && navScope != null) {
        with(sharedScope) {
            Modifier.sharedBounds(
                sharedContentState = rememberSharedContentState(DailyOverviewSharedKey),
                animatedVisibilityScope = navScope,
                boundsTransform = dailyOverviewBoundsTransform,
                resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
            )
        }
    } else {
        Modifier
    }

    Scaffold(
        modifier = sharedBounds.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(Res.string.do_yesterday), fontWeight = FontWeight.Bold)
                        facts?.let {
                            Text(
                                OverviewDate.format(it.date),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navActions.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                actions = {
                    FilledTonalIconButton(
                        onClick = { navActions.navigate(Screen.DailyOverview.Settings) },
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        ),
                    ) { Icon(Icons.Default.Settings, stringResource(Res.string.do_settings)) }
                },
                scrollBehavior = scrollBehavior,
            )
        }
    ) { padding ->
        // Only the first load staggers in; items scrolled into view later just appear.
        var firstLoadDone by rememberSaveable { mutableStateOf(false) }
        LaunchedEffect(facts != null) {
            if (facts != null) {
                delay(FirstLoadMs)
                firstLoadDone = true
            }
        }

        when {
            facts == null -> Unit
            facts.isQuiet -> EmptyDay(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .enterOnFirstLoad(0, !firstLoadDone)
            )

            else -> LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = padding.calculateTopPadding(),
                    bottom = padding.calculateBottomPadding() +
                        LocalNavHostPadding.current.calculateBottomPadding() + 16.dp,
                ),
                modifier = Modifier.fillMaxSize()
            ) {
                overview(
                    facts = facts,
                    state = state,
                    animateIn = !firstLoadDone,
                    onRegenerate = viewModel::regenerate,
                    onSetUp = { navActions.navigate(Screen.DailyOverview.Engine) },
                    onHistory = { navActions.history() },
                    onAllUpdates = { navActions.notifications() },
                    onOpen = { title, url, imageUrl, source ->
                        navActions.details(
                            title = title,
                            description = "",
                            url = url,
                            imageUrl = imageUrl,
                            source = source,
                        )
                    },
                )
            }
        }
    }
}

private fun LazyListScope.overview(
    facts: DailyFacts,
    state: DailyOverviewUiState,
    animateIn: Boolean,
    onRegenerate: () -> Unit,
    onSetUp: () -> Unit,
    onHistory: () -> Unit,
    onAllUpdates: () -> Unit,
    onOpen: (title: String, url: String, imageUrl: String, source: String) -> Unit,
) {
    var index = 0
    fun next() = index++

    next().let { i ->
        item(key = "summary") {
            SummaryCard(
                summary = state.summary,
                needsSetup = state.needsSetup,
                onRegenerate = onRegenerate,
                onSetUp = onSetUp,
                modifier = Modifier
                    .animateItem()
                    .enterOnFirstLoad(i, animateIn)
            )
        }
    }

    next().let { i ->
        item(key = "stats") {
            StatTiles(
                readCount = facts.readCount,
                titleCount = facts.titles.size,
                updateCount = facts.updates.size,
                unit = facts.unit,
                animateIn = animateIn,
                modifier = Modifier
                    .animateItem()
                    .padding(top = 12.dp)
                    .enterOnFirstLoad(i, animateIn)
            )
        }
    }

    if (peakHourLabel(facts.readsByHour) != null) {
        next().let { i ->
            item(key = "hours") {
                HourGraph(
                    readsByHour = facts.readsByHour,
                    animateIn = animateIn,
                    modifier = Modifier
                        .animateItem()
                        .padding(top = 8.dp)
                        .fillMaxWidth()
                        .enterOnFirstLoad(i, animateIn)
                )
            }
        }
    }

    if (facts.titles.isNotEmpty()) {
        next().let { i ->
            item(key = "readHeader") {
                SectionHeader(
                    title = stringResource(Res.string.do_you_read),
                    action = stringResource(Res.string.do_history),
                    onAction = onHistory,
                    modifier = Modifier
                        .animateItem()
                        .enterOnFirstLoad(i, animateIn)
                )
            }
        }
        val most = facts.titles.maxOf { it.count }.coerceAtLeast(1)
        facts.titles.forEach { title ->
            val i = next()
            item(key = "read:${title.url}") {
                ReadRow(
                    title = title,
                    fraction = title.count.toFloat() / most,
                    animateIn = animateIn,
                    onClick = { onOpen(title.title, title.url, title.imageUrl, title.source) },
                    modifier = Modifier
                        .animateItem()
                        .padding(bottom = 6.dp)
                        .enterOnFirstLoad(i, animateIn)
                )
            }
        }
    }

    if (facts.updates.isNotEmpty() || facts.failedSources.isNotEmpty()) {
        next().let { i ->
            item(key = "updatesHeader") {
                SectionHeader(
                    title = stringResource(Res.string.do_updated_overnight),
                    action = facts.updates.size
                        .takeIf { it > 0 }
                        ?.let { stringResource(Res.string.do_all_updates, it) },
                    onAction = onAllUpdates,
                    modifier = Modifier
                        .animateItem()
                        .enterOnFirstLoad(i, animateIn)
                )
            }
        }
        facts.updates.take(UpdatesShown).forEach { update ->
            val i = next()
            item(key = "update:${update.url}") {
                UpdateRow(
                    update = update,
                    onClick = { onOpen(update.title, update.url, update.imageUrl.orEmpty(), update.source) },
                    modifier = Modifier
                        .animateItem()
                        .padding(bottom = 6.dp)
                        .enterOnFirstLoad(i, animateIn)
                )
            }
        }
        facts.failedSources.forEach { source ->
            val i = next()
            item(key = "failed:$source") {
                FailedSourceRow(
                    source = source,
                    modifier = Modifier
                        .animateItem()
                        .padding(bottom = 6.dp)
                        .enterOnFirstLoad(i, animateIn)
                )
            }
        }
    }
}

/** Rises 14dp and fades in, [index] × 55ms after 180ms, when [play] is true at first composition. */
@Composable
private fun Modifier.enterOnFirstLoad(index: Int, play: Boolean): Modifier {
    val progress = remember { Animatable(if (play) 0f else 1f) }
    LaunchedEffect(progress) {
        if (progress.value < 1f) {
            delay(EnterDelayMs + index * EnterStaggerMs)
            progress.animateTo(1f, tween(500, easing = EnterEasing))
        }
    }
    return graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * 14.dp.toPx()
    }
}

@Composable
private fun SectionHeader(
    title: String,
    action: String?,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp, top = 18.dp, bottom = 8.dp)
    ) {
        Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        if (action != null) {
            Text(
                action,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onAction)
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun OverviewRow(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    cover: @Composable () -> Unit,
    trailing: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(8.dp)
    ) {
        cover()
        Column(Modifier.weight(1f)) { content() }
        trailing()
    }
}

@Composable
private fun Cover(imageUrl: String, name: String) {
    ImageLoaderChoice(
        imageUrl = imageUrl,
        name = name,
        placeHolder = { painterLogo() },
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .size(width = 40.dp, height = 54.dp)
            .clip(RoundedCornerShape(8.dp))
    )
}

@Composable
private fun RowTitle(text: String) = Text(
    text,
    fontSize = 13.5.sp,
    fontWeight = FontWeight.SemiBold,
    maxLines = 1,
    overflow = TextOverflow.Ellipsis,
)

@Composable
private fun RowSubtitle(text: String) = Text(
    text,
    fontSize = 11.5.sp,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    maxLines = 1,
    overflow = TextOverflow.Ellipsis,
)

@Composable
private fun Badge(text: String, container: Color, content: Color) = Text(
    text,
    fontSize = 11.sp,
    fontWeight = FontWeight.Bold,
    color = content,
    modifier = Modifier
        .clip(RoundedCornerShape(10.dp))
        .background(container)
        .padding(horizontal = 8.dp, vertical = 2.dp)
)

@Composable
private fun ReadRow(
    title: TitleRead,
    fraction: Float,
    animateIn: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Grows in on first load only; scrolling back into view shows it full.
    var grown by remember { mutableStateOf(!animateIn) }
    LaunchedEffect(Unit) {
        if (!grown) {
            delay(ProgressDelayMs)
            grown = true
        }
    }
    val progress by animateFloatAsState(
        targetValue = if (grown) fraction else 0f,
        animationSpec = tween(900, easing = EnterEasing),
        label = "readProgress",
    )
    OverviewRow(
        modifier = modifier,
        onClick = onClick,
        cover = { Cover(title.imageUrl, title.title) },
        trailing = {
            Badge(
                title.count.toString(),
                MaterialTheme.colorScheme.primary,
                MaterialTheme.colorScheme.onPrimary,
            )
        },
    ) {
        RowTitle(title.title)
        val range = if (title.firstChapter == title.lastChapter) {
            title.lastChapter
        } else {
            stringResource(Res.string.do_read_range, title.firstChapter, title.lastChapter)
        }
        RowSubtitle(if (title.caughtUp) stringResource(Res.string.do_caught_up, range) else range)
        Box(
            Modifier
                .padding(top = 5.dp)
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

@Composable
private fun UpdateRow(
    update: UpdateItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OverviewRow(
        modifier = modifier,
        onClick = onClick,
        cover = { Cover(update.imageUrl.orEmpty(), update.title) },
        trailing = {},
    ) {
        RowTitle(update.title)
        val source = LocalSourcesRepository.current.displayNameFor(update.source)
        RowSubtitle(listOf(update.summary, source).filter { it.isNotBlank() }.joinToString(" · "))
    }
}

@Composable
private fun FailedSourceRow(
    source: String,
    modifier: Modifier = Modifier,
) {
    OverviewRow(
        modifier = modifier,
        cover = {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(width = 40.dp, height = 54.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            ) {
                Icon(
                    Icons.Default.CloudOff,
                    null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        },
        trailing = {
            Badge("!", MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.error)
        },
    ) {
        RowTitle(LocalSourcesRepository.current.displayNameFor(source))
        RowSubtitle(stringResource(Res.string.do_update_check_failed))
    }
}

@Composable
private fun EmptyDay(modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        modifier = modifier.padding(bottom = LocalNavHostPadding.current.calculateBottomPadding())
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Icon(
                Icons.Default.Bedtime,
                null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp)
            )
        }
        Text(
            stringResource(Res.string.do_empty_day),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
