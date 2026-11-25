package com.programmersbox.kmpuiviews.tabs

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.programmersbox.kmpuiviews.motion.REDUCED_MOTION_REFRESH_MILLIS
import com.programmersbox.kmpuiviews.motion.systemReducesMotion
import org.koin.compose.koinInject
import kotlin.math.roundToInt

/** A foreground tab's cover growing from where it was clicked. [t] is linear time 0..1; the content fades in from 40 %. */
class GrowGhost(val tab: TabId, val origin: Rect) {
    val t = Animatable(0f)
    internal var taken = false

    val contentAlpha: Float get() = ((t.value - 0.4f) / 0.6f).coerceIn(0f, 1f)

    /** The cover is opaque until 45 % of the time, then fades out. */
    val coverAlpha: Float get() = 1f - ((t.value - 0.45f) / 0.55f).coerceIn(0f, 1f)
}

private class FlyGhost(val tab: TabId, val origin: Rect, val url: String) {
    val t = Animatable(0f)
}

/**
 * Shared motion state for [TabsController]'s windows. The controller calls [record] synchronously
 * when it emits an event, so state the next composition needs (a pending grow, a hidden tab icon)
 * is ready before that composition.
 */
class TabMotionState(
    /** The debug `TAB_MOTION_SCALE`; see [tabMotionScale]. */
    val scale: Float = tabMotionScale(System.getProperty("otaku.tabMotionDebug"), System.getenv("TAB_MOTION_SCALE")),
    mode: TabMotionMode = TabMotionMode.System,
) {
    /** The "Tab animations" setting. */
    var mode: TabMotionMode by mutableStateOf(mode)

    /** The OS reduce-motion setting, kept current by [followSystemReducedMotion]. */
    internal var systemReduces: Boolean by mutableStateOf(false)

    val level: TabMotionLevel get() = tabMotionLevel(mode, systemReduces)

    /** True under Reduced and Off: no flights, grows or glides, only crossfades or instant changes. */
    val reduced: Boolean get() = level != TabMotionLevel.Full

    /** Where the strip is; changed by [switchLayout]. */
    var position: TabPosition by mutableStateOf(TabPosition.Top)
        private set

    /** The position change in progress, if any. */
    internal var layoutSwitch: LayoutSwitch? by mutableStateOf(null)

    /** Each tab's chip (top) or row (side) in window coordinates, from the strip that shows it now. */
    val chipBounds = HashMap<TabId, Rect>()

    /** Icon bounds in window coordinates, by tab. Read when a motion starts and as it runs. */
    val slotBounds = HashMap<TabId, Rect>()
    internal val grows: SnapshotStateList<GrowGhost> = mutableStateListOf()
    internal val flying = mutableStateSetOf<TabId>()
    internal val flashes: SnapshotStateMap<TabId, Int> = mutableStateMapOf()
    private val seen = HashSet<TabId>()

    /** Each window's content area in window coordinates, set by the window. */
    val contentBounds = HashMap<WindowId, Rect>()

    /** True the first time [id] is seen: the strip animates only tabs it has not shown before. */
    fun markSeen(id: TabId): Boolean = seen.add(id)

    fun ms(motion: TabMotion) = tabMotionMs(motion, scale, level)

    fun easing(motion: TabMotion) = tabMotionEasing(motion, level)

    fun <T> fade(motion: TabMotion) = tween<T>(ms(motion), easing = easing(motion))

    fun flash(tab: TabId) {
        flashes[tab] = (flashes[tab] ?: 0) + 1
    }

    /** Whether [tab] has a grow that no content has taken yet. */
    internal fun hasGrow(tab: TabId): Boolean = grows.any { it.tab == tab && !it.taken }

    internal fun takeGrow(tab: TabId): GrowGhost? =
        grows.firstOrNull { it.tab == tab && !it.taken }?.also { it.taken = true }

    internal fun record(event: TabEvent) {
        when (event) {
            is TabEvent.Opened -> when {
                reduced || event.origin == null -> Unit
                event.mode == TabOpenMode.Foreground -> grows.add(GrowGhost(event.tab, event.origin))
                event.mode == TabOpenMode.Background -> flying.add(event.tab)
            }

            is TabEvent.Flash -> flash(event.tab)
            else -> Unit
        }
    }

    /** Keeps [systemReduces] current. [systemReducesMotion] is cached, and the app refreshes it off the main thread. */
    suspend fun followSystemReducedMotion() {
        while (true) {
            systemReduces = withContext(Dispatchers.IO) { systemReducesMotion() }
            delay(REDUCED_MOTION_REFRESH_MILLIS)
        }
    }

    /**
     * Moves the strip to [to]. With [animate] and Full motion every tab glides from its bounds now to its new
     * bounds; Reduced fades the new strip in; Off, or the saved value on launch, just moves it.
     */
    fun switchLayout(to: TabPosition, animate: Boolean) {
        if (to == position) return
        layoutSwitch = if (animate && level != TabMotionLevel.Off) LayoutSwitch(HashMap(chipBounds)) else null
        chipBounds.clear()
        position = to
    }

    /** A strip chip's alpha: hidden under its gliding ghost, or fading in under Reduced. */
    fun chipAlpha(tab: TabId): Float {
        val switch = layoutSwitch ?: return 1f
        return when {
            reduced -> switch.t.value
            tab in switch.from -> 0f
            else -> 1f
        }
    }
}

/** A tab position change: [from] holds each tab's bounds before it, [t] runs 0..1 (linear time). */
internal class LayoutSwitch(val from: Map<TabId, Rect>) {
    val t = Animatable(0f)
}

/**
 * The overlay above a window's content: covers flying into background tabs and growing into foreground
 * ones. Rects from [TabEvent.Opened] and [tabSlotBounds] are in window coordinates, like [contentBounds].
 */
@Composable
fun TabMotionLayer(
    controller: TabsController,
    window: BrowserWindowState,
    tabSlotBounds: (TabId) -> Rect?,
    contentBounds: () -> Rect,
    modifier: Modifier = Modifier,
    keys: TabKeys = koinInject(),
) {
    val motion = controller.motion
    var layerOrigin by remember { mutableStateOf(Offset.Zero) }
    val flights = remember { mutableStateListOf<FlyGhost>() }

    fun imageOf(tab: TabId) = controller.windows.firstNotNullOfOrNull { w -> w.tabs.firstOrNull { it.id == tab } }
        ?.let { keys.labelOf(it.current).imageUrl }

    LaunchedEffect(Unit) { motion.followSystemReducedMotion() }

    // On the UI thread, so the switch reads the chip bounds the strip last wrote.
    LaunchedEffect(Unit) { tabPositionPref.asFlow().collect { motion.switchLayout(it, animate = true) } }

    val layoutSwitch = motion.layoutSwitch
    LaunchedEffect(layoutSwitch) {
        layoutSwitch ?: return@LaunchedEffect
        layoutSwitch.t.animateTo(1f, tween(motion.ms(TabMotion.Layout), easing = LinearEasing))
        if (motion.layoutSwitch === layoutSwitch) motion.layoutSwitch = null
    }

    LaunchedEffect(controller, window.id) {
        controller.events
            .filterIsInstance<TabEvent.Opened>()
            .collect { event ->
                if (event.window != window.id || event.mode != TabOpenMode.Background) return@collect
                // Each open runs on its own so a quick run of them overlaps instead of queueing.
                launch {
                    val url = imageOf(event.tab)
                    if (event.origin == null || url == null || motion.reduced) {
                        motion.flying.remove(event.tab)
                        delay(motion.ms(TabMotion.SlotOpen).toLong())
                        motion.flash(event.tab)
                    } else {
                        flights.add(FlyGhost(event.tab, event.origin, url))
                    }
                }
            }
    }

    Box(
        modifier
            .fillMaxSize()
            .onGloballyPositioned { layerOrigin = it.positionInWindow() },
    ) {
        motion.grows.toList().forEach { ghost ->
            key(ghost) {
                LaunchedEffect(Unit) {
                    ghost.t.animateTo(1f, tween(motion.ms(TabMotion.Grow), easing = LinearEasing))
                    motion.grows.remove(ghost)
                }
                val url = imageOf(ghost.tab)
                if (url != null) {
                    val easing = motion.easing(TabMotion.Grow)
                    Cover(
                        url = url,
                        rect = {
                            val to = contentBounds()
                            lerp(ghost.origin, to, easing.transform(ghost.t.value))
                        },
                        layerOrigin = { layerOrigin },
                        radius = { lerp(6f, 0f, easing.transform(ghost.t.value)) },
                        alpha = { ghost.coverAlpha },
                    )
                }
            }
        }

        flights.toList().forEach { ghost ->
            key(ghost) {
                LaunchedEffect(Unit) {
                    // The slot is still opening; wait for its icon to be measured, then fly to where it is now.
                    var waited = 0
                    while (tabSlotBounds(ghost.tab) == null && waited < 500) {
                        delay(16)
                        waited += 16
                    }
                    ghost.t.animateTo(1f, tween(motion.ms(TabMotion.Background), easing = LinearEasing))
                    motion.flying.remove(ghost.tab)
                    flights.remove(ghost)
                    motion.flash(ghost.tab)
                }
                val easing = motion.easing(TabMotion.Background)
                Cover(
                    url = ghost.url,
                    rect = {
                        val to = tabSlotBounds(ghost.tab) ?: ghost.origin
                        lerp(ghost.origin, to, easing.transform(ghost.t.value))
                    },
                    layerOrigin = { layerOrigin },
                    radius = { lerp(6f, 2f, easing.transform(ghost.t.value)) },
                    alpha = { 1f },
                )
            }
        }

        // Tab position change: each tab glides from where it was to its slot in the new strip, which stays hidden
        // (TabMotionState.chipAlpha) until the glide ends. The target is read every frame, so it follows the side
        // column as it slides in.
        if (layoutSwitch != null && !motion.reduced) {
            val easing = motion.easing(TabMotion.Layout)
            val side = motion.position == TabPosition.Side
            val style = rememberTabIconStyle(keys)
            window.tabs.toList().forEach { tab ->
                val from = layoutSwitch.from[tab.id] ?: return@forEach
                key(tab.id) {
                    TabFace(
                        tab = tab,
                        label = keys.labelOf(tab.current),
                        selected = tab.id == window.activeId.value,
                        side = side,
                        style = style,
                        modifier = Modifier
                            .placeAt(
                                rect = {
                                    val to = motion.chipBounds[tab.id] ?: from
                                    lerp(from, to, easing.transform(layoutSwitch.t.value))
                                },
                                layerOrigin = { layerOrigin },
                            )
                            // A tab the new strip does not show (scrolled out) fades where it was.
                            .graphicsLayer { alpha = if (tab.id in motion.chipBounds) 1f else 1f - layoutSwitch.t.value },
                    )
                }
            }
        }
    }
}

/** Places the element at [rect] (window coordinates), so it can leave the layer's own bounds. */
private fun Modifier.placeAt(rect: () -> Rect, layerOrigin: () -> Offset): Modifier = layout { measurable, _ ->
    val r = rect()
    val w = r.width.roundToInt().coerceAtLeast(0)
    val h = r.height.roundToInt().coerceAtLeast(0)
    val placeable = measurable.measure(Constraints.fixed(w, h))
    layout(0, 0) {
        val o = layerOrigin()
        placeable.place((r.left - o.x).roundToInt(), (r.top - o.y).roundToInt())
    }
}

private fun lerp(from: Rect, to: Rect, f: Float) = Rect(
    left = from.left + (to.left - from.left) * f,
    top = from.top + (to.top - from.top) * f,
    right = from.right + (to.right - from.right) * f,
    bottom = from.bottom + (to.bottom - from.bottom) * f,
)

private fun lerp(from: Float, to: Float, f: Float) = from + (to - from) * f

/** A cover image placed at [rect] (window coordinates), so it can leave the layer's own bounds. */
@Composable
private fun Cover(
    url: String,
    rect: () -> Rect,
    layerOrigin: () -> Offset,
    radius: () -> Float,
    alpha: () -> Float,
) {
    val placeholder = MaterialTheme.colorScheme.surfaceVariant
    KamelImage(
        resource = { asyncPainterResource(url) },
        contentDescription = null,
        contentScale = ContentScale.Crop,
        onLoading = { Box(Modifier.fillMaxSize().background(placeholder)) },
        onFailure = { Box(Modifier.fillMaxSize().background(placeholder)) },
        modifier = Modifier
            .placeAt(rect, layerOrigin)
            .graphicsLayer {
                this.alpha = alpha()
                shape = RoundedCornerShape(radius().dp)
                clip = true
            },
    )
}
