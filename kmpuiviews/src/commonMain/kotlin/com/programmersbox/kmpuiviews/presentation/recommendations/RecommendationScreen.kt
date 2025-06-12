package com.programmersbox.kmpuiviews.presentation.recommendations

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.programmersbox.datastore.AiService
import com.programmersbox.datastore.GeminiSettings
import com.programmersbox.favoritesdatabase.Recommendation
import com.programmersbox.favoritesdatabase.RecommendationResponse
import com.programmersbox.kmpuiviews.presentation.components.BackButton
import com.programmersbox.kmpuiviews.utils.HideNavBarWhileOnScreen
import com.programmersbox.kmpuiviews.utils.LocalNavActions
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun RecommendationScreen(
    navigationIcon: @Composable () -> Unit = { BackButton() },
    viewModel: RecommendationViewModel = koinViewModel(),
) {
    RecommendationScreen(
        navigationIcon = navigationIcon,
        viewModel = viewModel,
        deleteRecommendation = viewModel::deleteRecommendation,
        insertRecommendation = viewModel::insertRecommendation
    )
}

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
    ExperimentalComposeUiApi::class,
    ExperimentalMaterial3ExpressiveApi::class
)
@Composable
private fun RecommendationScreen(
    viewModel: RecommendationViewModel,
    deleteRecommendation: (Recommendation) -> Unit,
    insertRecommendation: (Recommendation) -> Unit,
    navigationIcon: @Composable () -> Unit,
) {
    HideNavBarWhileOnScreen()

    val savedRecommendations by viewModel.savedRecommendation.collectAsStateWithLifecycle(emptyList())

    val navActions = LocalNavActions.current

    val aiService by viewModel.aiService.rememberPreference()

    val topBarScrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scope = rememberCoroutineScope()
    val lazyState = rememberLazyListState()
    LaunchedEffect(viewModel.messageList.lastIndex) {
        lazyState.animateScrollToItem(0)
    }

    val drawerState = rememberDrawerState(DrawerValue.Closed)

    var showSettings by remember { mutableStateOf(false) }

    if (showSettings) {
        AiSettings(
            onDismissRequest = { showSettings = false },
            aiService = aiService,
            geminiSettings = viewModel.geminiSettings.rememberPreference().value,
            onSave = {
                viewModel.updateGeminiSettings(it)
                showSettings = false
            }
        )
    }

    BackHandler(drawerState.isOpen) { scope.launch { drawerState.close() } }

    ModalNavigationDrawer(
        drawerState = drawerState,
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
                                onSearchClick = { navActions.globalSearch(it.title) },
                                onDeleteClick = { showDialog = true },
                                modifier = Modifier.animateItem()
                            )
                        }
                    }
                }
            }
        },
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("OtakuBot") },
                    subtitle = { Text("Powered by ${aiService.name}") },
                    navigationIcon = navigationIcon,
                    actions = {
                        IconButton(
                            onClick = { scope.launch { drawerState.open() } }
                        ) { Icon(Icons.Default.Save, null) }

                        IconButton(
                            onClick = { showSettings = true }
                        ) { Icon(Icons.Default.Settings, null) }
                    },
                    scrollBehavior = topBarScrollBehavior
                )
            },
            bottomBar = {
                MessageInput(
                    onSendMessage = { viewModel.send(it) },
                    resetScroll = { scope.launch { lazyState.animateScrollToItem(0) } },
                    modifier = Modifier
                        .background(BottomAppBarDefaults.containerColor)
                        .navigationBarsPadding()
                )
            },
            modifier = Modifier
                .nestedScroll(topBarScrollBehavior.nestedScrollConnection)
                .imePadding()
        ) { padding ->
            LazyColumn(
                state = lazyState,
                verticalArrangement = Arrangement.spacedBy(2.dp),
                contentPadding = padding,
                reverseLayout = true,
                modifier = Modifier.fillMaxSize()
            ) {
                if (viewModel.isLoading) {
                    item {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) { CircularProgressIndicator() }
                    }
                }

                items(
                    viewModel.messageList.reversed(),
                    contentType = { it }
                ) {
                    when (it) {
                        is Message.Error -> ErrorMessage(
                            message = it,
                            modifier = Modifier.animateItem()
                        )

                        is Message.Gemini -> GeminiMessage(
                            message = it,
                            onSaveClick = insertRecommendation,
                            savedRecommendations = savedRecommendations,
                            modifier = Modifier.animateItem()
                        )

                        is Message.User -> UserMessage(
                            message = it,
                            modifier = Modifier.animateItem()
                        )
                    }
                }

                item {
                    GeminiMessage(
                        message = Message.Gemini(
                            RecommendationResponse(
                                response = "Hi! Ask me for anime, manga, or novel recommendations and I will give them!",
                                recommendations = emptyList()
                            )
                        ),
                        onSaveClick = {},
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
    }
}

@Composable
private fun GeminiMessage(
    message: Message.Gemini,
    modifier: Modifier = Modifier,
    onSaveClick: (Recommendation) -> Unit,
    savedRecommendations: List<Recommendation> = emptyList(),
) {
    ChatBubbleItem(
        chatMessage = message,
        onSaveClick = onSaveClick,
        savedRecommendations = savedRecommendations,
        modifier = modifier
    )
}

@Composable
private fun UserMessage(
    message: Message.User,
    modifier: Modifier = Modifier,
) {
    ChatBubbleItem(
        chatMessage = message,
        modifier = modifier
    )
}

@Composable
private fun ErrorMessage(
    message: Message.Error,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Icon(
            Icons.Outlined.Warning,
            contentDescription = "Person Icon",
            tint = MaterialTheme.colorScheme.error
        )
        SelectionContainer {
            Text(
                text = message.text,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}

@Composable
fun ChatBubbleItem(
    chatMessage: Message,
    modifier: Modifier = Modifier,
    onSaveClick: (Recommendation) -> Unit = {},
    savedRecommendations: List<Recommendation> = emptyList(),
) {
    val backgroundColor = when (chatMessage) {
        is Message.Gemini -> MaterialTheme.colorScheme.primaryContainer
        is Message.User -> MaterialTheme.colorScheme.secondaryContainer
        is Message.Error -> MaterialTheme.colorScheme.errorContainer
    }

    val bubbleShape = when (chatMessage) {
        is Message.Error -> MaterialTheme.shapes.medium
        is Message.Gemini -> RoundedCornerShape(4.dp, 20.dp, 20.dp, 20.dp)
        is Message.User -> RoundedCornerShape(20.dp, 4.dp, 20.dp, 20.dp)
    }

    val horizontalAlignment = when (chatMessage) {
        is Message.Error -> Alignment.CenterHorizontally
        is Message.Gemini -> Alignment.Start
        is Message.User -> Alignment.End
    }

    Column(
        horizontalAlignment = horizontalAlignment,
        modifier = modifier
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .animateContentSize()
            .fillMaxWidth()
    ) {
        Text(
            text = when (chatMessage) {
                is Message.Error -> "Error"
                is Message.Gemini -> "OtakuBot"
                is Message.User -> "You"
            },
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        BoxWithConstraints {
            Card(
                colors = CardDefaults.cardColors(containerColor = backgroundColor),
                shape = bubbleShape,
                modifier = Modifier.widthIn(0.dp, maxWidth * 0.9f)
            ) {
                when (chatMessage) {
                    is Message.Error -> ErrorMessage(message = chatMessage)
                    is Message.Gemini -> {
                        Text(
                            chatMessage
                                .recommendationResponse
                                .response
                                ?: "Showing recommendations",
                            modifier = Modifier.padding(16.dp)
                        )

                        if (chatMessage.recommendationResponse.recommendations.isNotEmpty()) {
                            var showRecs by remember { mutableStateOf(false) }

                            AnimatedContent(showRecs, label = "") { target ->
                                if (target) {
                                    Column {
                                        ListItem(
                                            headlineContent = { Text("Recommendations") },
                                            trailingContent = {
                                                Icon(
                                                    Icons.Default.KeyboardArrowUp,
                                                    null
                                                )
                                            },
                                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                            modifier = Modifier.clickable { showRecs = !showRecs }
                                        )

                                        chatMessage.recommendationResponse.recommendations.forEach {
                                            Recommendations(
                                                recommendation = it,
                                                trailingContent = {
                                                    Crossfade(
                                                        savedRecommendations.fastAny { s -> s.title == it.title },
                                                        label = "",
                                                        modifier = Modifier.size(40.dp)
                                                    ) { target ->
                                                        if (target) {
                                                            Icon(
                                                                Icons.Default.CheckCircleOutline,
                                                                null,
                                                                tint = Color.Green
                                                            )
                                                        } else {
                                                            IconButton(
                                                                onClick = { onSaveClick(it) }
                                                            ) { Icon(Icons.Default.Save, null) }
                                                        }
                                                    }
                                                }
                                            )
                                        }
                                    }
                                } else {
                                    ListItem(
                                        headlineContent = {
                                            Column {
                                                chatMessage
                                                    .recommendationResponse
                                                    .recommendations
                                                    .forEach { Text(it.title) }
                                            }
                                        },
                                        trailingContent = {
                                            Icon(Icons.Default.KeyboardArrowDown, null)
                                        },
                                        colors = ListItemDefaults.colors(
                                            containerColor = Color.Transparent
                                        ),
                                        modifier = Modifier.clickable {
                                            showRecs = !showRecs
                                        }
                                    )
                                }
                            }
                        }
                    }

                    is Message.User -> {
                        SelectionContainer {
                            Text(
                                text = chatMessage.text,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Recommendations(
    recommendation: Recommendation,
    modifier: Modifier = Modifier,
    trailingContent: @Composable () -> Unit = {},
) {
    Column(modifier = modifier) {
        HorizontalDivider(
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        SelectionContainer {
            ListItem(
                trailingContent = trailingContent,
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
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecommendationItem(
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

        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth()
        ) {
            FilledTonalIconButton(
                onClick = onSearchClick
            ) { Icon(Icons.Default.Search, null) }

            FilledTonalIconButton(
                onClick = onDeleteClick
            ) { Icon(Icons.Default.Delete, null) }
        }
    }
}

@Composable
fun MessageInput(
    onSendMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
    resetScroll: () -> Unit = {},
) {
    var userMessage by remember { mutableStateOf("") }

    ElevatedCard(
        shape = RoundedCornerShape(
            topStart = 12.dp,
            topEnd = 12.dp,
            bottomEnd = 0.dp,
            bottomStart = 0.dp
        ),
        modifier = modifier
            .animateContentSize()
            .fillMaxWidth()
    ) {
        OutlinedTextField(
            value = userMessage,
            label = { Text("Message") },
            onValueChange = { userMessage = it },
            shape = MaterialTheme.shapes.large,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Send
            ),
            keyboardActions = KeyboardActions(
                onSend = {
                    if (userMessage.isNotBlank()) {
                        onSendMessage(userMessage)
                        userMessage = ""
                        resetScroll()
                    }
                }
            ),
            trailingIcon = {
                IconButton(
                    onClick = {
                        if (userMessage.isNotBlank()) {
                            onSendMessage(userMessage)
                            userMessage = ""
                            resetScroll()
                        }
                    },
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "send",
                        modifier = Modifier
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AiSettings(
    onDismissRequest: () -> Unit,
    aiService: AiService,
    geminiSettings: GeminiSettings,
    onSave: (GeminiSettings) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true
        ),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Scaffold {
            Box(modifier = Modifier.padding(it)) {
                when (aiService) {
                    AiService.Gemini -> GeminiSettings(
                        geminiSettings = geminiSettings,
                        onSave = onSave
                    )
                }
            }
        }
    }
}

@Composable
private fun GeminiSettings(
    geminiSettings: GeminiSettings,
    onSave: (GeminiSettings) -> Unit,
) {
    var currentSettings by remember(geminiSettings) { mutableStateOf(geminiSettings) }
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            currentSettings.apiKey,
            onValueChange = { currentSettings = currentSettings.copy(apiKey = it) },
            label = { Text("API Key") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            currentSettings.modelName,
            onValueChange = { currentSettings = currentSettings.copy(modelName = it) },
            label = { Text("Model") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            currentSettings.prompt,
            onValueChange = { currentSettings = currentSettings.copy(prompt = it) },
            label = { Text("Prompt (BE VERY CAREFUL ABOUT MODIFYING THIS! THINGS COULD BREAK!)") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = { onSave(currentSettings) },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) { Text("Save") }
    }
}