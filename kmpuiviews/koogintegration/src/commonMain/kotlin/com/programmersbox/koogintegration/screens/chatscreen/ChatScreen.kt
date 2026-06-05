package com.programmersbox.koogintegration.screens.chatscreen

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.CopyAll
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import com.programmersbox.koogintegration.AppDimension
import com.programmersbox.koogintegration.agentresponse.AgentRecommendations
import com.programmersbox.koogintegration.agentresponse.AgentResponse
import com.programmersbox.koogintegration.agentresponse.Recommendation
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ChatScreen(
    viewModel: ChatViewModel = koinViewModel(),
    onBack: () -> Unit,
    onKoogSettingsClick: () -> Unit,
    onSearchClick: (String) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ChatScreenContent(
        title = uiState.title,
        chatMessages = uiState.chatMessages,
        debugView = uiState.debugView,
        inputText = uiState.inputText,
        isInputEnabled = uiState.isInputEnabled,
        isLoading = uiState.isLoading,
        hideEmptyState = uiState.hideEmptyState,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        onKoogSettingsClick = onKoogSettingsClick,
        onSearchClick = onSearchClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatScreenContent(
    title: String,
    chatMessages: List<ChatMessage>,
    debugView: DebugView,
    inputText: String,
    isInputEnabled: Boolean,
    isLoading: Boolean,
    hideEmptyState: Boolean,
    onEvent: (ChatUiEvents) -> Unit,
    onBack: () -> Unit,
    onKoogSettingsClick: () -> Unit,
    onSearchClick: (String) -> Unit,
) {
    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val visibleMessages = remember(chatMessages, debugView) {
        chatMessages.filter(debugView::shows)
    }

    // Scroll to bottom when messages change
    LaunchedEffect(visibleMessages.size) {
        if (visibleMessages.isNotEmpty()) {
            listState.animateScrollToItem(visibleMessages.size - 1)
        }
    }

    LaunchedEffect(isInputEnabled) {
        if (isInputEnabled) {
            focusRequester.requestFocus()
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val clipboardManager = LocalClipboardManager.current
    val onCopyEvent: (String) -> Unit = {
        clipboardManager.setText(AnnotatedString(it))
        scope.launch {
            snackbarHostState.showSnackbar("Copied to clipboard")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(
                        onClick = onBack
                    ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    IconButton(
                        onClick = onKoogSettingsClick
                    ) { Icon(Icons.Default.Settings, contentDescription = "Settings") }

                    DebugViewSelector(
                        debugView = debugView,
                        onToggleEnabled = { onEvent(ChatUiEvents.ToggleDebugEnabled) },
                        onToggleOption = { onEvent(ChatUiEvents.ToggleDebugOption(it)) },
                        onShowMermaidGraph = { onEvent(ChatUiEvents.ShowMermaidGraph) },
                        modifier = Modifier.padding(end = AppDimension.spacingMedium),
                    )
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
        ) {
            // Messages list
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(AppDimension.spacingMedium),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = AppDimension.spacingMedium)
            ) {
                items(
                    items = visibleMessages,
                    contentType = { it.type }
                ) { message ->
                    SelectionContainer {
                        when (message) {
                            is ChatMessage.UserMessage -> UserMessageBubble(message.text)
                            is ChatMessage.AgentMessage -> AgentMessageBubble(
                                text = message.response,
                                onCopyEvent = onCopyEvent,
                                onSearchClick = onSearchClick
                            )

                            is ChatMessage.ResultMessage -> AgentMessageBubble(
                                text = message.response,
                                onCopyEvent = onCopyEvent,
                                onSearchClick = onSearchClick
                            )

                            is ChatMessage.SystemMessage -> SystemMessageItem(message.text)
                            is ChatMessage.ErrorMessage -> ErrorMessageItem(message.text)
                            is ChatMessage.ToolCallMessage -> ToolCallMessageItem(
                                message.toolName,
                                message.args
                            )

                            is ChatMessage.LLMCallMessage -> LLMCallMessageItem(message.data)
                            is ChatMessage.ExecutionTraceMessage -> ExecutionTraceMessageItem(
                                message.item
                            )

                            is ChatMessage.MermaidGraphMessage -> MermaidGraphMessageBubble(
                                text = message.mermaidGraphString,
                                onCopyEvent = onCopyEvent
                            )

                            is ChatMessage.LLMTokenUsageMessage -> LLMTokenUsageMessageItem(message)
                        }
                    }
                }

                if (!hideEmptyState) {
                    item(
                        contentType = "empty-state",
                    ) {
                        EmptyState(
                            emptyStateItems = listOf(
                                EmptyStateItem(
                                    title = "Analyze my favorites",
                                    action = {
                                        onEvent(ChatUiEvents.UpdateInputText("Analyze my favorites"))
                                        onEvent(ChatUiEvents.SendMessage)
                                    }
                                ),
                                EmptyStateItem(
                                    title = "Analyze my reading habits",
                                    action = {
                                        onEvent(ChatUiEvents.UpdateInputText("Analyze my reading habits"))
                                        onEvent(ChatUiEvents.SendMessage)
                                    }
                                ),
                                EmptyStateItem(
                                    title = "Analyze my collections (lists)",
                                    action = {
                                        onEvent(ChatUiEvents.UpdateInputText("Analyze my lists"))
                                        onEvent(ChatUiEvents.SendMessage)
                                    }
                                ),
                                EmptyStateItem(
                                    title = "Analyze my bookmarks",
                                    action = {
                                        onEvent(ChatUiEvents.UpdateInputText("Analyze my bookmarks"))
                                        onEvent(ChatUiEvents.SendMessage)
                                    }
                                ),
                                EmptyStateItem(
                                    title = "Recommend me something",
                                    action = { onEvent(ChatUiEvents.UpdateInputText("I want something similar to ")) }
                                )
                            ),
                        )
                    }
                }

                // Add extra space at the bottom for better UX
                item(contentType = "spacer") {
                    Spacer(modifier = Modifier.height(AppDimension.spacingMedium))
                }
            }

            // Input area
            InputArea(
                text = inputText,
                onTextChanged = { onEvent(ChatUiEvents.UpdateInputText(it)) },
                onSendClicked = {
                    onEvent(ChatUiEvents.SendMessage)
                    focusManager.clearFocus()
                },
                isEnabled = isInputEnabled,
                isLoading = isLoading,
                focusRequester = focusRequester
            )
        }
    }
}

@Composable
private fun DebugViewSelector(
    modifier: Modifier = Modifier,
    debugView: DebugView,
    onToggleEnabled: () -> Unit,
    onToggleOption: (DebugOption) -> Unit,
    onShowMermaidGraph: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDimension.spacingSmall)
    ) {
        Text(
            text = "Debug:",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Switch(
            checked = debugView.enabled,
            onCheckedChange = { onToggleEnabled() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onSecondary,
                checkedTrackColor = MaterialTheme.colorScheme.secondary,
                checkedBorderColor = MaterialTheme.colorScheme.secondary,
            ),
        )
        Box {
            TextButton(
                onClick = { expanded = true },
                enabled = debugView.enabled,
            ) {
                Text(
                    text = "View",
                    style = MaterialTheme.typography.labelMedium,
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                DebugOption.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.title) },
                        onClick = { onToggleOption(option) },
                        leadingIcon = {
                            Checkbox(
                                checked = option in debugView.options,
                                onCheckedChange = null,
                            )
                        },
                    )
                }

                DropdownMenuItem(
                    text = { Text("Show mermaid graph") },
                    onClick = onShowMermaidGraph,
                    leadingIcon = { Icon(Icons.Default.AutoGraph, null) },
                )
            }
        }
    }
}


@Composable
private fun UserMessageBubble(text: String) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val maxBubbleWidth =
            maxWidth - AppDimension.messageTitleColumnWidth - AppDimension.spacingSmall
        Row(
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.Top,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = maxBubbleWidth)
                    .clip(RoundedCornerShape(AppDimension.radiusExtraLarge))
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(AppDimension.spacingMedium)
            ) {
                Text(
                    text = text,
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            /*Spacer(modifier = Modifier.width(AppDimension.spacingSmall))
            Image(
                painter = painterResource(Res.drawable.compose_multiplatform),
                contentDescription = "User",
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(AppDimension.messageTitleColumnWidth)
            )*/
        }
    }
}

@Composable
private fun AgentMessageBubble(
    text: AgentResponse,
    onCopyEvent: (String) -> Unit,
    onSearchClick: (String) -> Unit,
) {
    Column {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val avatarSpace = 0.dp
            val maxBubbleWidth = maxWidth - avatarSpace
            Row(
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .widthIn(max = maxBubbleWidth)
                        .clip(RoundedCornerShape(AppDimension.radiusExtraLarge))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(AppDimension.spacingMedium)
                ) {
                    when (text) {
                        is AgentRecommendations -> {
                            Column {
                                Markdown(
                                    content = text.text,
                                    colors = markdownColor(text = MaterialTheme.colorScheme.onPrimaryContainer),
                                    typography = markdownTypography(text = MaterialTheme.typography.bodyLarge)
                                )
                                text.recommendations.forEach { recommendation ->
                                    RecommendationItem(
                                        recommendation = recommendation,
                                        onSearchClick = { onSearchClick(recommendation.title) }
                                    )
                                }
                            }
                        }

                        is AgentResponse.Text -> {
                            Markdown(
                                content = text.text,
                                colors = markdownColor(text = MaterialTheme.colorScheme.onPrimaryContainer),
                                typography = markdownTypography(text = MaterialTheme.typography.bodyLarge)
                            )
                        }
                    }

                }
            }
        }
    }
}

@Composable
private fun MermaidGraphMessageBubble(
    text: String,
    onCopyEvent: (String) -> Unit,
) {
    Column {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val maxBubbleWidth = maxWidth
            Row(
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .widthIn(max = maxBubbleWidth)
                        .clip(RoundedCornerShape(AppDimension.radiusExtraLarge))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(AppDimension.spacingMedium)
                ) {
                    Text(
                        text,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }
        Row(
            horizontalArrangement = Arrangement.End,
            modifier = Modifier.fillMaxWidth(),
        ) {
            IconButton(
                onClick = { onCopyEvent(text) }
            ) { Icon(Icons.Default.CopyAll, null) }
        }
    }
}

@Composable
private fun SystemMessageItem(text: String) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = AppDimension.spacingMedium)
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun ErrorMessageItem(text: String) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val maxBubbleWidth =
            maxWidth - AppDimension.messageTitleColumnWidth - AppDimension.spacingSmall
        Row(
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.Top,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                contentAlignment = Alignment.TopCenter,
                modifier = Modifier.size(AppDimension.messageTitleColumnWidth)
            ) {
                Text(
                    text = "Error",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(top = AppDimension.spacingMedium)
                )
            }
            Spacer(modifier = Modifier.width(AppDimension.spacingSmall))
            Box(
                modifier = Modifier
                    .widthIn(max = maxBubbleWidth)
                    .clip(RoundedCornerShape(AppDimension.radiusExtraLarge))
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(AppDimension.spacingMedium)
            ) {
                Text(
                    text = text,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ToolCallMessageItem(toolName: String, args: Map<String, String>) {
    val borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val maxContentWidth =
            maxWidth - AppDimension.messageTitleColumnWidth - AppDimension.spacingSmall
        Row(
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.Top,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                contentAlignment = Alignment.TopCenter,
                modifier = Modifier.width(AppDimension.messageTitleColumnWidth)
            ) {
                Text(
                    text = "Tool\nCall",
                    color = MaterialTheme.colorScheme.outline,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Spacer(modifier = Modifier.width(AppDimension.spacingSmall))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(AppDimension.spacingSmall),
                verticalArrangement = Arrangement.spacedBy(AppDimension.spacingSmall),
                modifier = Modifier
                    .widthIn(max = maxContentWidth)
                    .clip(RoundedCornerShape(6.dp))
                    .border(1.dp, borderColor, RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(AppDimension.spacingSmall)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f))
                        .border(1.dp, borderColor, RoundedCornerShape(4.dp))
                        .padding(horizontal = AppDimension.spacingSmall, vertical = 2.dp)
                ) {
                    Text(
                        text = toolName,
                        color = MaterialTheme.colorScheme.secondary,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                args.forEach { (key, value) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .border(1.dp, borderColor, RoundedCornerShape(4.dp))
                            .padding(horizontal = AppDimension.spacingSmall, vertical = 2.dp)
                    ) {
                        Text(
                            text = key,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = ": ",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                        )
                        Text(
                            text = value,
                            color = MaterialTheme.colorScheme.secondary,
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExecutionTraceMessageItem(item: ExecutionTraceItem) {
    when (item) {
        is ExecutionTraceItem.Node -> NodeExecutionTraceItem(item.name)
        is ExecutionTraceItem.SubgraphStarted -> SubgraphStartedTraceItem(item.name)
        is ExecutionTraceItem.SubgraphCompleted -> SubgraphCompletedTraceItem(
            item.name,
            item.result
        )
    }
}

@Composable
private fun NodeExecutionTraceItem(name: String) {
    Row(
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.width(AppDimension.messageTitleColumnWidth)
        ) {
            Text(
                text = "Node",
                color = MaterialTheme.colorScheme.outline,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            )
        }
        Spacer(modifier = Modifier.width(AppDimension.spacingSmall))
        Text(
            text = name,
            color = MaterialTheme.colorScheme.outline,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

@Composable
private fun SubgraphStartedTraceItem(name: String) {
    val borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    Row(
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.width(AppDimension.messageTitleColumnWidth)
        ) {
            Text(
                text = "Task\nStart",
                color = MaterialTheme.colorScheme.outline,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            )
        }
        Spacer(modifier = Modifier.width(AppDimension.spacingSmall))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .border(1.dp, borderColor, RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(AppDimension.spacingSmall)
        ) {
            Text(
                text = name,
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

@Composable
private fun SubgraphCompletedTraceItem(name: String, result: String?) {
    val borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    Row(
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.width(AppDimension.messageTitleColumnWidth)
        ) {
            Text(
                text = "Task\nResult",
                color = MaterialTheme.colorScheme.outline,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            )
        }
        Spacer(modifier = Modifier.width(AppDimension.spacingSmall))
        Column(
            verticalArrangement = Arrangement.spacedBy(AppDimension.spacingExtraSmall),
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .border(1.dp, borderColor, RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(AppDimension.spacingSmall)
        ) {
            Text(
                text = name,
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            )
            if (!result.isNullOrBlank()) {
                Text(
                    text = result,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LLMTokenUsageMessageItem(tokenUsage: ChatMessage.LLMTokenUsageMessage) {
    val borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val maxContentWidth =
            maxWidth - AppDimension.messageTitleColumnWidth - AppDimension.spacingSmall
        Row(
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.Top,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                contentAlignment = Alignment.TopCenter,
                modifier = Modifier.size(AppDimension.messageTitleColumnWidth)
            ) {
                Text(
                    text = "Token\nUsage",
                    color = MaterialTheme.colorScheme.outline,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(top = AppDimension.spacingExtraSmall)
                )
            }
            Spacer(modifier = Modifier.width(AppDimension.spacingSmall))
            Column(
                verticalArrangement = Arrangement.spacedBy(AppDimension.spacingExtraSmall),
                modifier = Modifier
                    .widthIn(max = maxContentWidth)
                    .clip(RoundedCornerShape(AppDimension.radiusMedium))
                    .border(1.dp, borderColor, RoundedCornerShape(AppDimension.radiusMedium))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(AppDimension.spacingMedium)
            ) {
                Text(
                    "Input tokens: ${tokenUsage.inputTokens}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    ),
                )
                Text(
                    "Output tokens: ${tokenUsage.outputTokens}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    ),
                )
                Text(
                    "Total tokens: ${tokenUsage.totalTokens}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    ),
                )
            }
        }
    }
}

@Composable
private fun InputArea(
    text: String,
    onTextChanged: (String) -> Unit,
    onSendClicked: () -> Unit,
    isEnabled: Boolean,
    isLoading: Boolean,
    focusRequester: FocusRequester,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = AppDimension.elevationMedium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = AppDimension.spacingMedium,
                    vertical = AppDimension.spacingSmall
                )
        ) {
            var textFieldValue by remember { mutableStateOf(TextFieldValue(text)) }
            LaunchedEffect(text) {
                if (text != textFieldValue.text) {
                    textFieldValue = TextFieldValue(text, TextRange(text.length))
                }
            }
            OutlinedTextField(
                value = textFieldValue,
                onValueChange = { newValue ->
                    textFieldValue = newValue
                    onTextChanged(newValue.text)
                },
                placeholder = { Text("Type a message...") },
                enabled = isEnabled,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSendClicked() }),
                shape = RoundedCornerShape(AppDimension.radiusRound),
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester)
                    .onPreviewKeyEvent { event ->
                        if (event.key == Key.Enter && event.type == KeyEventType.KeyDown) {
                            if (event.isShiftPressed) {
                                val newText = textFieldValue.text + "\n"
                                textFieldValue = TextFieldValue(newText, TextRange(newText.length))
                                onTextChanged(newText)
                                true
                            } else {
                                if (text.isNotBlank()) {
                                    onSendClicked()
                                }
                                true
                            }
                        } else {
                            false
                        }
                    }
            )

            Spacer(modifier = Modifier.width(AppDimension.spacingSmall))

            // Send button or loading indicator
            if (isLoading) {
                CircularWavyProgressIndicator(
                    modifier = Modifier
                        .size(AppDimension.iconButtonSizeLarge)
                        .padding(AppDimension.spacingButtonPadding)
                )
            } else {
                IconButton(
                    onClick = onSendClicked,
                    enabled = isEnabled && text.isNotBlank(),
                    modifier = Modifier
                        .size(AppDimension.iconButtonSizeLarge)
                        .clip(CircleShape)
                        .background(
                            if (isEnabled && text.isNotBlank()) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (isEnabled && text.isNotBlank()) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecommendationItem(
    recommendation: Recommendation,
    modifier: Modifier = Modifier,
    onSearchClick: () -> Unit,
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
            FilledTonalIconButton(
                onClick = onSearchClick
            ) { Icon(Icons.Default.Search, null) }
        }
    }
}

@Composable
private fun EmptyState(
    emptyStateItems: List<EmptyStateItem>,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(AppDimension.spacingMedium),
        modifier = modifier,
    ) {
        Text(
            "Start chatting or choose an option below to get started!",
            style = MaterialTheme.typography.displaySmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        ) {
            emptyStateItems.forEach { item ->
                HeroChip(
                    label = item.title,
                    color = MaterialTheme.colorScheme.primary,
                    onClick = item.action,
                    modifier = Modifier
                        .height(64.dp)
                        .weight(1f)
                )
            }
        }
    }
}

@Composable
private fun HeroChip(
    label: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.25f)),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}