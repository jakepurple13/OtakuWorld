package com.programmersbox.uiviews.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.favoritesdatabase.NotificationItem
import com.programmersbox.sharedutils.AppLogo
import com.programmersbox.uiviews.R
import com.programmersbox.uiviews.presentation.Screen
import com.programmersbox.uiviews.presentation.components.imageloaders.ImageLoaderChoice
import com.programmersbox.uiviews.utils.ComposableUtils
import com.programmersbox.uiviews.utils.LoadingDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

//TODO: Trying this out...Maybe this is an option?
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptionsSheet(
    sheet: SheetState = rememberModalBottomSheetState(true),
    scope: CoroutineScope,
    navController: NavController,
    imageUrl: String,
    title: String,
    description: String,
    serviceName: String,
    url: String,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
    dao: ItemDao = koinInject(),
) {
    val isSaved by dao
        .doesNotificationExistFlow(url)
        .collectAsStateWithLifecycle(false)

    var showLoadingDialog by remember { mutableStateOf(false) }

    LoadingDialog(
        showLoadingDialog = showLoadingDialog,
        onDismissRequest = { showLoadingDialog = false }
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheet
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
        ) {
            ListItem(
                leadingContent = {
                    val logo = koinInject<AppLogo>().logo
                    ImageLoaderChoice(
                        imageUrl = imageUrl,
                        name = title,
                        placeHolder = rememberDrawablePainter(logo),
                        modifier = Modifier
                            .size(ComposableUtils.IMAGE_WIDTH, ComposableUtils.IMAGE_HEIGHT)
                            .clip(MaterialTheme.shapes.small)
                    )
                },
                overlineContent = { Text(serviceName) },
                headlineContent = { Text(title) },
                supportingContent = { Text(description) },
                colors = ListItemDefaults.colors(
                    containerColor = Color.Transparent
                )
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                OptionsItem(
                    title = "Open",
                    onClick = {
                        scope.launch {
                            sheet.hide()
                        }.invokeOnCompletion {
                            onDismiss()
                            onOpen()
                        }
                    }
                )

                OptionsItem(
                    title = stringResource(R.string.global_search_by_name),
                    onClick = {
                        scope.launch { sheet.hide() }
                            .invokeOnCompletion {
                                navController.navigate(Screen.GlobalSearchScreen(title))
                            }
                    }
                )

                if (!isSaved) {
                    OptionsItem(
                        title = stringResource(id = R.string.save_for_later),
                        onClick = {
                            scope.launch {
                                dao.insertNotification(
                                    NotificationItem(
                                        id = "$title$url$imageUrl$serviceName$description".hashCode(),
                                        url = url,
                                        summaryText = "$title had an update",
                                        notiTitle = title,
                                        imageUrl = imageUrl,
                                        source = serviceName,
                                        contentTitle = title
                                    )
                                )
                            }
                        }
                    )
                } else {
                    OptionsItem(
                        title = stringResource(R.string.removeNotification),
                        onClick = {
                            scope.launch {
                                dao.getNotificationItemFlow(url)
                                    .firstOrNull()
                                    ?.let { dao.deleteNotification(it) }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.OptionsItem(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        modifier = modifier
    ) {
        ListItem(
            headlineContent = { Text(title) },
            colors = ListItemDefaults.colors(
                containerColor = Color.Transparent
            )
        )
    }

    HorizontalDivider()
}