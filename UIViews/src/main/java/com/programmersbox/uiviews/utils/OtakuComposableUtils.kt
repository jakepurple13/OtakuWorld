@file:OptIn(ExperimentalGlideComposeApi::class)

package com.programmersbox.uiviews.utils

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.programmersbox.models.ItemModel
import com.programmersbox.sharedutils.AppLogo
import com.programmersbox.uiviews.R
import com.programmersbox.uiviews.presentation.Screen
import com.programmersbox.uiviews.presentation.components.imageloaders.ImageLoaderChoice
import com.programmersbox.uiviews.presentation.components.placeholder.PlaceholderHighlight
import com.programmersbox.uiviews.presentation.components.placeholder.m3placeholder
import com.programmersbox.uiviews.presentation.components.placeholder.shimmer
import com.programmersbox.uiviews.presentation.navigateToDetails
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

object ComposableUtils {
    const val IMAGE_WIDTH_PX = 360
    const val IMAGE_HEIGHT_PX = 480
    val IMAGE_WIDTH @Composable get() = with(LocalDensity.current) { IMAGE_WIDTH_PX.toDp() }
    val IMAGE_HEIGHT @Composable get() = with(LocalDensity.current) { IMAGE_HEIGHT_PX.toDp() }
}

@Composable
fun M3PlaceHolderCoverCard(placeHolder: Int, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.size(
            ComposableUtils.IMAGE_WIDTH,
            ComposableUtils.IMAGE_HEIGHT
        ),
        tonalElevation = 4.dp,
        shape = MaterialTheme.shapes.medium
    ) {
        Box {
            Image(
                painter = painterResource(placeHolder),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.Center)
                    .m3placeholder(
                        true,
                        highlight = PlaceholderHighlight.shimmer()
                    )
                    .size(ComposableUtils.IMAGE_WIDTH, ComposableUtils.IMAGE_HEIGHT)
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black
                            ),
                            startY = 50f
                        )
                    )
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Text(
                    "",
                    style = MaterialTheme
                        .typography
                        .bodyLarge
                        .copy(textAlign = TextAlign.Center, color = Color.White),
                    maxLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .m3placeholder(
                            true,
                            highlight = PlaceholderHighlight.shimmer()
                        )
                        .align(Alignment.BottomCenter)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtakuBannerBox(
    placeholder: Int,
    modifier: Modifier = Modifier,
    showBanner: Boolean = false,
    content: @Composable BannerScope.() -> Unit,
) {
    var itemInfo by remember { mutableStateOf<ItemModel?>(null) }

    val bannerScope = BannerScope { itemModel -> itemInfo = itemModel }

    /*DisposableEffect(Unit) {
        bannerScope = BannerScope { itemModel -> itemInfo = itemModel }
        onDispose { bannerScope = null }
    }*/

    val scope = rememberCoroutineScope()

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val navController = LocalNavController.current

    itemInfo?.let {
        OptionsSheet(
            sheet = sheetState,
            scope = scope,
            navController = LocalNavController.current,
            imageUrl = it.imageUrl,
            title = it.title,
            description = it.description,
            serviceName = it.source.serviceName,
            onOpen = { navController.navigateToDetails(it) },
            onDismiss = { itemInfo = null }
        )
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        bannerScope.content()
    }

    /*BannerBox(
        modifier = modifier,
        showBanner = showBanner,
        banner = {
            Surface(
                modifier = Modifier.align(Alignment.TopCenter),
                shape = MaterialTheme.shapes.medium.copy(topStart = CornerSize(0.dp), topEnd = CornerSize(0.dp)),
                tonalElevation = 4.dp,
                shadowElevation = 10.dp
            ) {
                ListItem(
                    leadingContent = {
                        GlideGradientImage(
                            model = itemInfo?.imageUrl.orEmpty(),
                            placeholder = placeholder,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(ComposableUtils.IMAGE_WIDTH, ComposableUtils.IMAGE_HEIGHT)
                                .clip(MaterialTheme.shapes.small)
                        )
                    },
                    overlineContent = { Text(itemInfo?.source?.serviceName.orEmpty()) },
                    headlineContent = { Text(itemInfo?.title.orEmpty()) },
                    supportingContent = {
                        Text(
                            itemInfo?.description.orEmpty(),
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 5
                        )
                    }
                )
            }
        },
        content = { bannerScope.content() }
    )*/
}

//TODO: Trying this out...Maybe this is an option?
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OptionsSheet(
    sheet: SheetState = rememberModalBottomSheetState(true),
    scope: CoroutineScope,
    navController: NavController,
    imageUrl: String,
    title: String,
    description: String,
    serviceName: String,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
) {
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
                Card(
                    onClick = {
                        scope.launch {
                            sheet.hide()
                        }.invokeOnCompletion {
                            onDismiss()
                            onOpen()
                        }
                    },
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Transparent
                    )
                ) {
                    ListItem(
                        headlineContent = { Text("Open") },
                        colors = ListItemDefaults.colors(
                            containerColor = Color.Transparent
                        )
                    )
                }

                HorizontalDivider()

                Card(
                    onClick = {
                        scope.launch { sheet.hide() }
                            .invokeOnCompletion {
                                navController.navigate(Screen.GlobalSearchScreen(title))
                            }
                    },
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Transparent
                    )
                ) {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.global_search_by_name)) },
                        colors = ListItemDefaults.colors(
                            containerColor = Color.Transparent
                        )
                    )
                }

                HorizontalDivider()
            }
        }
    }
}

fun interface BannerScope {
    //TODO: Maybe add a modifier into here for onLongClick?
    fun newItemModel(itemModel: ItemModel?)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> CustomBannerBox(
    bannerContent: @Composable BoxScope.(T?) -> Unit,
    modifier: Modifier = Modifier,
    showBanner: Boolean = false,
    itemToImageUrl: (T) -> String,
    itemToTitle: (T) -> String,
    itemToDescription: (T) -> String,
    itemToSource: (T) -> String,
    onOpen: (T) -> Unit,
    content: @Composable CustomBannerScope<T>.() -> Unit,
) {
    var itemInfo by remember { mutableStateOf<T?>(null) }

    var bannerScope by remember { mutableStateOf<CustomBannerScope<T>?>(null) }

    DisposableEffect(Unit) {
        bannerScope = object : CustomBannerScope<T> {
            override fun newItem(item: T?) {
                itemInfo = item
            }
        }
        onDispose { bannerScope = null }
    }

    /*BannerBox(
        modifier = modifier,
        showBanner = showBanner,
        banner = {
            Surface(
                shape = MaterialTheme.shapes.medium.copy(topStart = CornerSize(0.dp), topEnd = CornerSize(0.dp)),
                tonalElevation = 4.dp,
                shadowElevation = 10.dp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .animateContentSize(),
            ) {
                bannerContent(itemInfo)
            }
        },
        content = { bannerScope?.content() }
    )*/

    val scope = rememberCoroutineScope()

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val navController = LocalNavController.current

    itemInfo?.let {
        OptionsSheet(
            sheet = sheetState,
            scope = scope,
            navController = LocalNavController.current,
            imageUrl = itemToImageUrl(it),
            title = itemToTitle(it),
            description = itemToDescription(it),
            serviceName = itemToSource(it),
            onOpen = { onOpen(it) },
            onDismiss = { itemInfo = null }
        )
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        bannerScope?.content()
    }
}

interface CustomBannerScope<T> {
    fun newItem(item: T?)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourceNotInstalledModal(
    showItem: String?,
    onShowItemDismiss: (String?) -> Unit,
    source: String?,
    additionOptions: @Composable ColumnScope.() -> Unit = {},
) {
    val navController = LocalNavController.current

    if (showItem != null) {
        BackHandler { onShowItemDismiss(null) }

        ModalBottomSheet(
            onDismissRequest = { onShowItemDismiss(null) },
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Text(
                source.orEmpty(),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            CenterAlignedTopAppBar(title = { Text(showItem) })
            ListItem(
                headlineContent = { Text(stringResource(id = R.string.global_search)) },
                leadingContent = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.clickable {
                    onShowItemDismiss(null)
                    navController.navigate(Screen.GlobalSearchScreen(showItem))
                }
            )
            additionOptions()
        }
    }
}