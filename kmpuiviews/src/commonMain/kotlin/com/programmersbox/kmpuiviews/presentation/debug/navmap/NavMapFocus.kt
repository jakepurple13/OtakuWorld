package com.programmersbox.kmpuiviews.presentation.debug.navmap

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * One screen at a time: what leads in, the screen itself, what it leads to. Tapping a row walks there;
 * the trail at the top steps back.
 */
@Composable
fun NavMapFocus(
    state: NavMapState,
    graph: NavGraph,
    onWalk: (String) -> Unit,
    onJumpTrail: (Int) -> Unit,
    onOpen: (NavNode) -> Unit,
    canOpen: (NavNode) -> Boolean,
    onShowOnMap: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Slide left when walking forward, right when stepping back along the trail.
    var lastTrailSize by remember { mutableIntStateOf(state.trail.size) }
    val forward = state.trail.size >= lastTrailSize
    lastTrailSize = state.trail.size

    AnimatedContent(
        targetState = state.trail.lastOrNull(),
        transitionSpec = {
            val dir = if (forward) 1 else -1
            (slideInHorizontally { it / 6 * dir } + fadeIn()) togetherWith (slideOutHorizontally { -it / 6 * dir } + fadeOut())
        },
        label = "focus",
        modifier = modifier,
    ) { id ->
        val node = id?.let { i -> graph.nodes.firstOrNull { it.id == i } }
        if (node == null) {
            Text("Pick a screen on the map", Modifier.padding(16.dp))
            return@AnimatedContent
        }
        val ins = graph.inbound(node.id)
        val outs = graph.outbound(node.id)
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            item(key = "trail") {
                FlowRow(Modifier.animateItem(), verticalArrangement = Arrangement.Center) {
                    state.trail.forEachIndexed { index, t ->
                        val name = t.substringAfterLast('.')
                        if (index == state.trail.lastIndex) {
                            Text(name, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(8.dp))
                        } else {
                            TextButton(onClick = { onJumpTrail(index) }) { Text(name, fontFamily = FontFamily.Monospace) }
                            Text("›", Modifier.align(Alignment.CenterVertically))
                        }
                    }
                }
            }

            item(key = "in-header") {
                SectionHeader("Comes from · ${ins.size}", MaterialTheme.colorScheme.tertiary, Modifier.animateItem())
            }
            if (ins.isEmpty()) {
                item(key = "in-empty") {
                    Text(
                        if (node.pseudo) "Entry point. The system or user starts here." else "Nothing navigates here.",
                        color = if (node.pseudo) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.animateItem(),
                    )
                }
            }
            items(ins, key = { "in-${it.from}" }) { e ->
                EdgeRow(graph.label(e.from) + "  →", e, onClick = { onWalk(e.from) }, modifier = Modifier.animateItem())
            }

            item(key = "card") {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth().animateItem(),
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(node.label, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
                        if (node.args.isNotEmpty()) {
                            Text(
                                "(" + node.args.joinToString { "${it.name}: ${it.type}${if (it.nullable) "?" else ""}" } + ")",
                                fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                            if (!node.pseudo) {
                                Button(
                                    onClick = { onOpen(node) },
                                    enabled = canOpen(node),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.onPrimary,
                                        contentColor = MaterialTheme.colorScheme.primary,
                                    ),
                                ) { Text(if (node.args.isEmpty()) "Open" else "Open with sample args") }
                            }
                            OutlinedButton(
                                onClick = { onShowOnMap(node.id) },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onPrimary),
                            ) { Text("Show on map") }
                        }
                    }
                }
            }

            item(key = "out-header") {
                SectionHeader("Goes to · ${outs.size}", MaterialTheme.colorScheme.primary, Modifier.animateItem())
            }
            if (outs.isEmpty()) {
                item(key = "out-empty") {
                    Text(
                        "Leaf screen. Only back from here.",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.animateItem(),
                    )
                }
            }
            items(outs, key = { "out-${it.to}" }) { e ->
                EdgeRow("→  " + graph.label(e.to), e, onClick = { onWalk(e.to) }, modifier = Modifier.animateItem())
            }
        }
    }
}

private fun NavGraph.label(id: String) = nodes.firstOrNull { it.id == id }?.label ?: id

@Composable
private fun SectionHeader(text: String, color: Color, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        color = color,
        style = MaterialTheme.typography.labelSmall,
        fontFamily = FontFamily.Monospace,
        modifier = modifier.padding(top = 8.dp),
    )
}

@Composable
private fun EdgeRow(name: String, edge: NavEdge, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedCard(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(name, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
            val source = if (edge.isOverride) {
                "override" + (edge.note?.let { " · $it" } ?: "")
            } else {
                edge.sites.firstOrNull()?.let { "${it.file.substringAfterLast('/')}:${it.line}" + if (edge.sites.size > 1) " +${edge.sites.size - 1}" else "" }.orEmpty()
            }
            Text(
                source,
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.labelSmall,
                color = if (edge.isOverride) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(0.8f, fill = false),
            )
        }
    }
}
