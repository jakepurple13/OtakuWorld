package com.programmersbox.kmpuiviews.presentation.urlopener

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.Source
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.favoritesdatabase.NotificationItem
import com.programmersbox.kmpuiviews.painterLogo
import com.programmersbox.kmpuiviews.presentation.components.BackButton
import com.programmersbox.kmpuiviews.presentation.components.OtakuScaffold
import com.programmersbox.kmpuiviews.presentation.components.settings.PreferenceSetting
import com.programmersbox.kmpuiviews.presentation.components.textflow.TextFlow
import com.programmersbox.kmpuiviews.presentation.settings.utils.showCustomSourceChooser
import com.programmersbox.kmpuiviews.utils.ComposableUtils
import com.programmersbox.kmpuiviews.utils.LocalNavActions
import com.programmersbox.kmpuiviews.utils.composables.imageloaders.ImageLoaderChoice
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun UrlOpenerScreen(
    viewModel: UrlOpenerViewModel = koinViewModel(),
) {
    var url by remember { mutableStateOf("") }
    val navActions = LocalNavActions.current
    val dao: ItemDao = koinInject()
    val scope = rememberCoroutineScope()
    val listItemColors = ListItemDefaults.colors()
    var showSources by showCustomSourceChooser {
        viewModel.currentChosenSource = it
    }

    OtakuScaffold(
        topBar = {
            TopAppBar(
                title = { Text("Url Opener") },
                navigationIcon = { BackButton() }
            )
        }
    ) { padding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            PreferenceSetting(
                settingTitle = { Text("Source") },
                settingIcon = { Icon(Icons.Default.Source, null) },
                summaryValue = if (viewModel.currentChosenSource == null) {
                    {}
                } else {
                    { Text(viewModel.currentChosenSource?.apiService?.serviceName.orEmpty()) }
                },
                endIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                modifier = Modifier.clickable { showSources = true }
            )

            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("Url") },
                trailingIcon = {
                    IconButton(
                        onClick = { viewModel.open(url) }
                    ) { Icon(Icons.Default.FindInPage, null) }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )

            viewModel.kmpItemModel?.let {
                TextFlow(
                    text = buildAnnotatedString {
                        withStyle(
                            MaterialTheme.typography.labelSmall
                                .copy(color = listItemColors.overlineColor)
                                .toSpanStyle()
                        ) { appendLine(it.source.serviceName) }

                        withStyle(
                            MaterialTheme.typography.bodyLarge
                                .copy(color = listItemColors.headlineColor)
                                .toSpanStyle()
                        ) { appendLine(it.title) }

                        withStyle(
                            MaterialTheme.typography.bodySmall
                                .copy(color = listItemColors.supportingTextColor)
                                .toSpanStyle()
                        ) { appendLine(it.description.trimIndent()) }
                    },
                    lineHeight = MaterialTheme.typography.bodyMedium.lineHeight,
                    obstacleContent = {
                        ImageLoaderChoice(
                            imageUrl = it.imageUrl,
                            name = it.title,
                            placeHolder = { painterLogo() },
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .size(ComposableUtils.IMAGE_WIDTH, ComposableUtils.IMAGE_HEIGHT)
                                .clip(MaterialTheme.shapes.small)
                        )
                    },
                    modifier = Modifier.padding(16.dp)
                )
            }

            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = {
                        scope.launch {
                            viewModel.kmpItemModel?.let {
                                dao.insertNotification(
                                    NotificationItem(
                                        id = it.toString().hashCode(),
                                        url = it.url,
                                        summaryText = "Waiting for source",
                                        notiTitle = it.title,
                                        imageUrl = it.imageUrl,
                                        source = it.source.serviceName,
                                        contentTitle = it.title
                                    )
                                )
                            }
                        }
                    },
                    enabled = viewModel.kmpItemModel != null,
                    shapes = ButtonDefaults.shapes()
                ) { Text("Save for later") }

                Button(
                    onClick = { viewModel.kmpItemModel?.let { navActions.details(it) } },
                    enabled = viewModel.kmpItemModel != null,
                    shapes = ButtonDefaults.shapes()
                ) { Text("Open") }
            }
        }
    }
}