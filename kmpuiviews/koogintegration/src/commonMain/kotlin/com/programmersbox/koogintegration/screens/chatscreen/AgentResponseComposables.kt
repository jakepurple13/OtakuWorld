package com.programmersbox.koogintegration.screens.chatscreen

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import com.programmersbox.koogintegration.agentresponse.AgentResult
import com.programmersbox.koogintegration.agentresponse.GeneratedCustomListResponse
import com.programmersbox.koogintegration.agentresponse.Recommendation

@Composable
internal fun TextResponse(text: AgentResult.Text) {
    Markdown(
        state = text.state,
        colors = markdownColor(text = MaterialTheme.colorScheme.onPrimaryContainer),
        typography = markdownTypography(text = MaterialTheme.typography.bodyLarge)
    )
}

@Composable
internal fun RecommendationsResponse(
    text: AgentResult.Recommendation,
    koogNavigation: KoogNavigation,
    isRecommendationSavedAlready: (Recommendation) -> Boolean,
    onEvent: (ChatUiEvents) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Markdown(
            state = text.state,
            colors = markdownColor(text = MaterialTheme.colorScheme.onPrimaryContainer),
            typography = markdownTypography(text = MaterialTheme.typography.bodyLarge)
        )
        text.recommendations.forEach { recommendation ->
            RecommendationItem(
                recommendation = recommendation,
                onSearchClick = { koogNavigation.onSearchClick(recommendation.title) },
                onEvent = onEvent,
                isRecommendationSavedAlready = isRecommendationSavedAlready(recommendation)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun RecommendationItem(
    recommendation: Recommendation,
    isRecommendationSavedAlready: Boolean,
    onSearchClick: () -> Unit,
    onEvent: (ChatUiEvents) -> Unit,
    modifier: Modifier = Modifier,
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
            horizontalArrangement = Arrangement.End,
            modifier = Modifier
                .padding(4.dp)
                .fillMaxWidth()
        ) {
            FilledTonalButton(
                onClick = { onEvent(ChatUiEvents.SaveRecommendation(recommendation)) },
                enabled = !isRecommendationSavedAlready,
                shapes = ButtonDefaults.shapes()
            ) { Text("Save") }

            FilledTonalIconButton(
                onClick = onSearchClick,
                shapes = IconButtonDefaults.shapes()
            ) { Icon(Icons.Default.Search, null) }
        }
    }
}

@Composable
internal fun ListResponseItem(
    text: AgentResult.CustomList,
    koogNavigation: KoogNavigation,
) {
    Column {
        Markdown(
            state = text.state,
            colors = markdownColor(text = MaterialTheme.colorScheme.onPrimaryContainer),
            typography = markdownTypography(text = MaterialTheme.typography.bodyLarge)
        )

        HorizontalDivider()

        Button(
            onClick = koogNavigation.onListClick,
            modifier = Modifier.align(Alignment.End)
        ) { Text("View Lists") }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun GeneratedListResponse(
    text: GeneratedCustomListResponse,
    onEvent: (ChatUiEvents) -> Unit,
) {
    var useBiometrics by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf(text.listName) }
    val itemsToSave = remember { mutableStateListOf(*text.items.toTypedArray()) }
    var viewList by remember { mutableStateOf(false) }
    var saveButtonState by remember { mutableStateOf(true) }

    if (viewList) {
        ModalBottomSheet(
            onDismissRequest = { viewList = false },
            containerColor = MaterialTheme.colorScheme.background,
            sheetState = rememberModalBottomSheetState(true)
        ) {
            Scaffold(
                topBar = {
                    CenterAlignedTopAppBar(
                        title = { Text("Choose what items to keep in the new list!") }
                    )
                }
            ) { padding ->
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(padding)
                ) {
                    text.items.forEach {
                        ListItem(
                            selected = it in itemsToSave,
                            onClick = {
                                if (it in itemsToSave) {
                                    itemsToSave.remove(it)
                                } else {
                                    itemsToSave.add(it)
                                }
                            },
                            enabled = saveButtonState,
                            content = { Text(it.title) },
                            overlineContent = { Text(it.source) },
                            supportingContent = { Text(it.url) }
                        )
                    }
                }
            }
        }
    }

    Column {
        Text(text.response)

        HorizontalDivider()

        ListItem(
            headlineContent = {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    trailingIcon = {
                        IconButton(
                            onClick = { title = text.listName }
                        ) { Icon(Icons.Default.Restore, null) }
                    },
                    label = { Text("List Title") },
                    enabled = saveButtonState,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            supportingContent = { Text(text.listDescription) },
            colors = ListItemDefaults.colors(
                containerColor = Color.Transparent
            )
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Use Biometrics")

            Switch(
                checked = useBiometrics,
                onCheckedChange = { useBiometrics = it },
                enabled = saveButtonState
            )
        }

        Text("Items suggested: ${text.items.size}")
        Text("Items selected: ${itemsToSave.size}")

        HorizontalDivider()

        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = { viewList = true }
            ) { Text("View List") }

            Button(
                onClick = {
                    saveButtonState = false
                    onEvent(
                        ChatUiEvents.SaveGeneratedList(
                            generatedCustomListResponse = text,
                            chosenName = title,
                            useBiometrics = useBiometrics,
                            itemsToSave = itemsToSave
                        )
                    )
                },
                enabled = saveButtonState
            ) { Text("Save") }
        }
    }
}