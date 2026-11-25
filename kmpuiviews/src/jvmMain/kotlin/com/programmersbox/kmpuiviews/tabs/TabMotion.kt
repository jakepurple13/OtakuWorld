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
    reduced: Boolean = false,
) {
    var reduced: Boolean by mutableStateOf(reduced)
        private set
    private var loaded = false

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

    fun ms(motion: TabMotion) = tabMotionMs(motion, scale, reduced)

    fun easing(motion: TabMotion) = tabMotionEasing(motion, reduced)

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

    /** Reads the OS reduce-motion setting once, off the main thread. */
    suspend fun loadReduced() {
        if (loaded) return
        loaded = true
        reduced = withContext(Dispatchers.IO) { reducedMotion() }
    }
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

    LaunchedEffect(Unit) { motion.loadReduced() }

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
            .layout { measurable, _ ->
                val r = rect()
                val w = r.width.roundToInt().coerceAtLeast(0)
                val h = r.height.roundToInt().coerceAtLeast(0)
                val placeable = measurable.measure(Constraints.fixed(w, h))
                layout(0, 0) {
                    val o = layerOrigin()
                    placeable.place((r.left - o.x).roundToInt(), (r.top - o.y).roundToInt())
                }
            }
            .graphicsLayer {
                this.alpha = alpha()
                shape = RoundedCornerShape(radius().dp)
                clip = true
            },
    )
}
