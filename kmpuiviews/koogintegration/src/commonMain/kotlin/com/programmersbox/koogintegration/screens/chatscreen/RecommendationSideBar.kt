package com.programmersbox.koogintegration.screens.chatscreen

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerState
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.programmersbox.favoritesdatabase.Recommendation

@Composable
internal fun RecommendationSideBar(
    drawerState: DrawerState,
    savedRecommendations: List<Recommendation>,
    deleteRecommendation: (Recommendation) -> Unit,
    onSearchClick: (Recommendation) -> Unit,
    content: @Composable () -> Unit,
) {
    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = drawerState.isOpen,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
            ) {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Saved Recommendations") },
                        )
                    }
                ) { padding ->
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        contentPadding = padding,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(savedRecommendations) {
                            var showDialog by remember { mutableStateOf(false) }
                            if (showDialog) {
                                AlertDialog(
                                    onDismissRequest = { showDialog = false },
                                    icon = { Icon(Icons.Default.Warning, null) },
                                    title = { Text("Delete Recommendation") },
                                    text = { Text("Are you sure you want to delete ${it.title}?") },
                                    confirmButton = {
                                        TextButton(
                                            onClick = {
                                                deleteRecommendation(it)
                                                showDialog = false
                                            },
                                            colors = ButtonDefaults.textButtonColors(
                                                contentColor = MaterialTheme.colorScheme.error
                                            )
                                        ) { Text("Yes") }
                                    },
                                    dismissButton = {
                                        TextButton(
                                            onClick = { showDialog = false }
                                        ) { Text("No") }
                                    }
                                )
                            }
                            RecommendationItem(
                                recommendation = it,
                                onSearchClick = { onSearchClick(it) },
                                onDeleteClick = { showDialog = true },
                                modifier = Modifier.animateItem()
                            )
                        }
                    }
                }
            }
        },
        modifier = Modifier.fillMaxSize(),
        content = content
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecommendationItem(
    recommendation: Recommendation,
    modifier: Modifier = Modifier,
    onSearchClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    var showRecs by remember { mutableStateOf(false) }
    OutlinedCard(
        onClick = { showRecs = !showRecs },
        modifier = modifier
    ) {
        AnimatedContent(showRecs, label = "") { target ->
            if (target) {
                SelectionContainer {
                    ListItem(
                        headlineContent = { Text(recommendation.title) },
                        supportingContent = {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(recommendation.description)
                                HorizontalDivider(
                                    modifier = Modifier.fillMaxWidth(0.5f),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text("Reason: " + recommendation.reason)
                            }
                        },
                        overlineContent = {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                recommendation.genre.forEach {
                                    Text(it)
                                }
                            }
                        },
                        colors = ListItemDefaults.colors(
                            containerColor = Color.Transparent
                        )
                    )
                }
            } else {
                ListItem(
                    trailingContent = { Icon(Icons.Default.KeyboardArrowDown, null) },
                    headlineContent = { Text(recommendation.title) },
                    colors = ListItemDefaults.colors(
                        containerColor = Color.Transparent
                    )
                )
            }
        }

        HorizontalDivider()

        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier
                .padding(4.dp)
                .fillMaxWidth()
        ) {
            FilledTonalIconButton(
                onClick = onDeleteClick,
                shapes = IconButtonDefaults.shapes()
            ) { Icon(Icons.Default.Delete, null) }

            FilledTonalIconButton(
                onClick = onSearchClick,
                shapes = IconButtonDefaults.shapes()
            ) { Icon(Icons.Default.Search, null) }
        }
    }
}