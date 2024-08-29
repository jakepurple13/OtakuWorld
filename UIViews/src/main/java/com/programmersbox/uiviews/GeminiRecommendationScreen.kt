package com.programmersbox.uiviews

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.BlockThreshold
import com.google.ai.client.generativeai.type.HarmCategory
import com.google.ai.client.generativeai.type.SafetySetting
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import com.programmersbox.uiviews.utils.BackButton
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.math.ln

val key = ""

private val HARASSMENT_PARAM = SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.NONE)
private val HATE_SPEECH_PARAM = SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.NONE)
private val DANGEROUS_CONTENT_PARAM = SafetySetting(HarmCategory.DANGEROUS_CONTENT, BlockThreshold.NONE)
private val SEXUALLY_EXPLICIT_PARAM = SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, BlockThreshold.NONE)
private val SAFETY_SETTINGS = listOf(HARASSMENT_PARAM, HATE_SPEECH_PARAM, DANGEROUS_CONTENT_PARAM, SEXUALLY_EXPLICIT_PARAM)

//TODO: THIS IS NOT WORKING DUE TO Ktor HttpTimeout not being found!
// I think an update is needed then it'll work...maybe? Hopefully?
class GeminiRecommendationViewModel : ViewModel() {

    val generativeModel = GenerativeModel(
        "gemini-1.5-flash",
        // Retrieve API key as an environmental variable defined in a Build Configuration
        // see https://github.com/google/secrets-gradle-plugin for further instructions
        key,
        generationConfig = generationConfig {
            temperature = 1f
            topK = 64
            topP = 0.95f
            maxOutputTokens = 8192
            responseMimeType = "application/json"
        },
        // safetySettings = Adjust safety settings
        // See https://ai.google.dev/gemini-api/docs/safety-settings
        safetySettings = SAFETY_SETTINGS,
        systemInstruction = content { text("You are a human-like, minimalistic bot speaking to adults who really like anime, manga, and novels. They are asking about recommendations based on what they have currently read or watched or just random recommendations in general. When responding, make sure to include the title, a short summary without any spoilers, and a few genre tags for the recommendation. Try to recommend at least 3 per response.\nWhen responding, respond with json like the following:\n{\"response\":response,\"recommendations\":[{\"title\":title, \"description\":description, \"reason\": reason, genre:[genres]}]}") },
    )

    private val chat = generativeModel.startChat()

    val messageList = mutableStateListOf<Message>()

    var isLoading by mutableStateOf(false)

    private val json = Json

    fun send(input: String) {
        viewModelScope.launch {
            isLoading = true
            runCatching {
                messageList.add(Message.User(input))
                chat.sendMessage(input)
            }
                .onSuccess {
                    runCatching {
                        messageList.add(Message.Gemini(json.decodeFromString(it.text.orEmpty().trim())))
                    }.onFailure {
                        it.printStackTrace()
                        messageList.add(Message.Error(it.localizedMessage.orEmpty()))
                    }
                }
                .onFailure {
                    it.printStackTrace()
                    messageList.add(Message.Error(it.localizedMessage.orEmpty()))
                }
            isLoading = false
        }
    }
}

sealed class Message {
    data class Gemini(val recommendationResponse: RecommendationResponse) : Message()
    data class User(val text: String) : Message()
    data class Error(val text: String) : Message()
}

@Serializable
data class Recommendation(
    val title: String,
    val description: String,
    val reason: String,
    val genre: List<String>,
)

@Serializable
data class RecommendationResponse(
    val response: String,
    val recommendations: List<Recommendation>,
)

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class
)
@Composable
fun GeminiRecommendationScreen(
    viewModel: GeminiRecommendationViewModel = viewModel { GeminiRecommendationViewModel() },
) {
    val scope = rememberCoroutineScope()
    val lazyState = rememberLazyListState()
    LaunchedEffect(viewModel.messageList.lastIndex) {
        lazyState.animateScrollToItem(0)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("OtakuBot: Powered by Gemini") },
                navigationIcon = { BackButton() }
            )
        },
        bottomBar = {
            MessageInput(
                onSendMessage = { viewModel.send(it) },
                resetScroll = { scope.launch { lazyState.animateScrollToItem(0) } },
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                    .navigationBarsPadding()
            )
        },
    ) { padding ->
        LazyColumn(
            state = lazyState,
            verticalArrangement = Arrangement.spacedBy(2.dp),
            contentPadding = padding,
            reverseLayout = true,
            modifier = Modifier
                .fillMaxSize() // fill the entire window
            //.imePadding() // padding for the bottom for the IME
            //.imeNestedScroll(), // scroll IME at the bottom
        ) {

            if (viewModel.isLoading) {
                item {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            viewModel.messageList.reversed().forEach {
                when (it) {
                    is Message.Error -> item {
                        ErrorMessage(
                            message = it,
                        )
                    }

                    is Message.Gemini -> item {
                        GeminiMessage(
                            message = it,
                        )
                    }

                    is Message.User -> stickyHeader {
                        UserMessage(
                            message = it,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GeminiMessage(
    message: Message.Gemini,
    modifier: Modifier = Modifier,
) {
    ChatBubbleItem(
        chatMessage = message,
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
    Row(modifier = modifier) {
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
) {
    val isModelMessage = chatMessage is Message.Gemini

    val backgroundColor = when (chatMessage) {
        is Message.Gemini -> MaterialTheme.colorScheme.primaryContainer
        is Message.User -> MaterialTheme.colorScheme.secondaryContainer
        is Message.Error -> MaterialTheme.colorScheme.errorContainer
    }

    val bubbleShape = if (isModelMessage) {
        RoundedCornerShape(4.dp, 20.dp, 20.dp, 20.dp)
    } else {
        RoundedCornerShape(20.dp, 4.dp, 20.dp, 20.dp)
    }

    val horizontalAlignment = if (isModelMessage) {
        Alignment.Start
    } else {
        Alignment.End
    }

    Column(
        horizontalAlignment = horizontalAlignment,
        modifier = modifier
            .padding(horizontal = 8.dp, vertical = 4.dp)
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
                        Text(chatMessage.recommendationResponse.response)

                        chatMessage.recommendationResponse.recommendations.forEach {
                            ListItem(
                                headlineContent = { Text(it.title) },
                                supportingContent = {
                                    Column {
                                        Text(it.description)
                                        Text(it.reason)
                                    }
                                },
                                overlineContent = { Text(it.genre.joinToString(", ")) }
                            )
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
                /*SelectionContainer {
                    Text(
                        text = chatMessage.text,
                        modifier = Modifier.padding(16.dp)
                    )
                }*/
            }
        }
    }
}

@Composable
fun MessageInput(
    onSendMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
    resetScroll: () -> Unit = {},
) {
    var userMessage by rememberSaveable { mutableStateOf("") }

    ElevatedCard(
        shape = RoundedCornerShape(
            topStart = 12.dp,
            topEnd = 12.dp,
            bottomEnd = 0.dp,
            bottomStart = 0.dp
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = userMessage,
            label = { Text("Message") },
            onValueChange = { userMessage = it },
            shape = MaterialTheme.shapes.large,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
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

internal fun ColorScheme.surfaceColorAtElevation(
    elevation: Dp,
): Color {
    if (elevation == 0.dp) return surface
    val alpha = ((4.5f * ln(elevation.value + 1)) + 2f) / 100f
    return primary.copy(alpha = alpha).compositeOver(surface)
}