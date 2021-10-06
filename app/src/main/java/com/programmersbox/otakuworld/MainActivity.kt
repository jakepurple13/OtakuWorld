package com.programmersbox.otakuworld

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastMap
import com.bumptech.glide.Glide
import com.google.android.material.composethemeadapter.MdcTheme
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.models.ApiService
import com.programmersbox.models.ItemModel
import com.programmersbox.models.SwatchInfo
import com.programmersbox.uiviews.utils.*
import com.skydoves.landscapist.glide.GlideImage
import com.skydoves.landscapist.palette.BitmapPalette
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.random.Random
import com.programmersbox.anime_sources.Sources as ASources
import com.programmersbox.manga_sources.Sources as MSources
import com.programmersbox.novel_sources.Sources as NSources

class MainActivity : ComponentActivity() {

    private var count = 0
    private val disposable = CompositeDisposable()
    private val sourceList = mutableStateListOf<ItemModel>()
    private val favorites = mutableStateListOf<DbModel>()

    @ExperimentalFoundationApi
    @ExperimentalMaterialApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Single.merge(
            SourceChoice
                .values()
                .flatMap { it.choices.toList() }
                .fastMap {
                    it
                        .getRecent()
                        .subscribeOn(Schedulers.io())
                        .observeOn(Schedulers.io())
                }
        )
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy { sourceList.addAll(it) }
            .addTo(disposable)

        setContent {
            MdcTheme {

                /*BottomDrawer(
                    drawerContent = {
                        TestData()
                    }
                ) {
                    ModalDrawer(
                        drawerContent = {
                            TestData()
                        }
                    ) {
                        BackdropScaffold(
                            appBar = { TopAppBar(title = { Text("Hello World") }) },
                            backLayerContent = { TestData() },
                            frontLayerContent = { TestData() }
                        )
                    }
                }*/

                /*Scaffold(
                    topBar = { TopAppBar(title = { Text("UI Test") }) }
                ) { p ->
                    LazyColumn(
                        contentPadding = p,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(sourceList) {
                            InfoCard2(info = it, favorites = favorites) {
                                val db = it.toDbModel()
                                if (db in favorites) favorites.remove(db) else favorites.add(db)
                            }
                        }

                        items(sourceList) {
                            InfoCard(info = it, favorites = favorites) {
                                val db = it.toDbModel()
                                if (db in favorites) favorites.remove(db) else favorites.add(db)
                            }
                        }

                        item { MaterialCardPreview() }
                    }
                }*/

                //NestedScrollExample()
                CustomNestedScrollExample()
            }
        }
    }

    @Composable
    fun TestData() = Scaffold { Column { repeat(10) { Text("Hello ${count++}") } } }

    override fun onDestroy() {
        super.onDestroy()
        disposable.dispose()
    }
}

enum class SourceChoice(vararg val choices: ApiService) {
    ANIME(ASources.GOGOANIME_VC, ASources.VIDSTREAMING),
    MANGA(MSources.MANGA_HERE, MSources.MANGAMUTINY),
    NOVEL(NSources.WUXIAWORLD)
}

@Composable
private fun Color.animate() = animateColorAsState(this)

@ExperimentalMaterialApi
@Composable
fun InfoCard2(
    info: ItemModel,
    favorites: List<DbModel>,
    onClick: () -> Unit
) {
    val swatchInfo = remember { mutableStateOf<SwatchInfo?>(null) }

    MaterialCard(
        backgroundColor = swatchInfo.value?.rgb?.toComposeColor()?.animate()?.value ?: MaterialTheme.colors.surface,
        modifier = Modifier.clickable { onClick() },
        media = {
            GlideImage(
                imageModel = info.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                requestBuilder = Glide.with(LocalContext.current.applicationContext).asDrawable(),
                bitmapPalette = BitmapPalette { p ->
                    swatchInfo.value = p.vibrantSwatch?.let { s -> SwatchInfo(s.rgb, s.titleTextColor, s.bodyTextColor) }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ComposableUtils.IMAGE_HEIGHT)
                    /*.size(
                        ComposableUtils.IMAGE_WIDTH,
                        ComposableUtils.IMAGE_HEIGHT
                    )*/
                    .align(Alignment.CenterHorizontally)
            )
        },
        headerOnTop = false,
        header = {
            ListItem(
                icon = {
                    Box {
                        if (favorites.fastAny { f -> f.url == info.url }) {
                            Icon(
                                Icons.Default.Favorite,
                                contentDescription = null,
                                tint = swatchInfo.value?.bodyColor?.toComposeColor()?.animate()?.value ?: MaterialTheme.colors.primary,
                                modifier = Modifier.align(Alignment.TopStart)
                            )
                        }
                        Icon(
                            Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            tint = MaterialTheme.colors.onPrimary,
                            modifier = Modifier.align(Alignment.TopStart)
                        )
                    }
                },
                text = {
                    Text(
                        info.title,
                        color = swatchInfo.value?.bodyColor?.toComposeColor()?.animate()?.value ?: Color.Unspecified
                    )
                },
                secondaryText = {
                    Text(
                        info.source.serviceName,
                        color = swatchInfo.value?.titleColor?.toComposeColor()?.animate()?.value ?: Color.Unspecified
                    )
                }
            )
        },
        supportingText = {
            Text(
                info.description,
                color = swatchInfo.value?.titleColor?.toComposeColor()?.animate()?.value ?: Color.Unspecified
            )
        }
    )
}

@ExperimentalMaterialApi
@Composable
fun InfoCard(
    info: ItemModel,
    favorites: List<DbModel>,
    onClick: () -> Unit
) {

    val swatchInfo = remember { mutableStateOf<SwatchInfo?>(null) }

    Card(
        backgroundColor = swatchInfo.value?.rgb?.toComposeColor()?.animate()?.value ?: MaterialTheme.colors.surface,
        modifier = Modifier.padding(4.dp),
        onClick = onClick
    ) {
        Column {
            Row {
                GlideImage(
                    imageModel = info.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    requestBuilder = Glide.with(LocalContext.current.applicationContext).asDrawable(),
                    bitmapPalette = BitmapPalette { p ->
                        swatchInfo.value = p.vibrantSwatch?.let { s -> SwatchInfo(s.rgb, s.titleTextColor, s.bodyTextColor) }
                    },
                    modifier = Modifier
                        .clip(
                            MaterialTheme.shapes.medium
                                .copy(
                                    topEnd = CornerSize(0.dp),
                                    bottomStart = CornerSize(0.dp),
                                    bottomEnd = CornerSize(0.dp)
                                )
                        )
                        .size(
                            ComposableUtils.IMAGE_WIDTH / 1.5f,
                            ComposableUtils.IMAGE_HEIGHT / 1.5f
                        )
                )

                ListItem(
                    overlineText = {
                        Text(
                            info.source.serviceName,
                            color = swatchInfo.value?.titleColor?.toComposeColor()?.animate()?.value ?: Color.Unspecified
                        )
                    },
                    text = {
                        Text(
                            info.title,
                            color = swatchInfo.value?.bodyColor?.toComposeColor()?.animate()?.value ?: Color.Unspecified
                        )
                    },
                    secondaryText = {
                        Text(
                            info.description,
                            color = swatchInfo.value?.titleColor?.toComposeColor()?.animate()?.value ?: Color.Unspecified
                        )
                    }
                )

            }

            Divider(
                thickness = 0.5.dp,
                color = swatchInfo.value?.titleColor?.toComposeColor()?.animate()?.value ?: MaterialTheme.colors.onSurface.copy(alpha = .12f)
            )

            Row {

                Box(Modifier.padding(5.dp)) {
                    if (favorites.fastAny { f -> f.url == info.url }) {
                        Icon(
                            Icons.Default.Favorite,
                            contentDescription = null,
                            tint = swatchInfo.value?.bodyColor?.toComposeColor()?.animate()?.value ?: MaterialTheme.colors.primary,
                            modifier = Modifier.align(Alignment.TopStart)
                        )
                    }
                    Icon(
                        Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        tint = MaterialTheme.colors.onPrimary,
                        modifier = Modifier.align(Alignment.TopStart)
                    )
                }

            }
        }
    }
}

fun Random.nextColor(
    alpha: Int = nextInt(0, 255),
    red: Int = nextInt(0, 255),
    green: Int = nextInt(0, 255),
    blue: Int = nextInt(0, 255)
) = Color(red, green, blue, alpha)

@ExperimentalMaterialApi
@Preview
@Composable
fun MaterialCardPreview() {

    val media: @Composable ColumnScope.() -> Unit = {
        Image(
            painter = painterResource(id = R.drawable.github_icon),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .height(ComposableUtils.IMAGE_WIDTH)
                .align(Alignment.CenterHorizontally)
                .background(Color.DarkGray)
        )
    }

    val actions: @Composable RowScope.() -> Unit = {
        TextButton(
            onClick = {},
            modifier = Modifier.weight(1f)
        ) { Text("Action 1", style = MaterialTheme.typography.button) }
        TextButton(
            onClick = {},
            modifier = Modifier.weight(1f)
        ) { Text("Action 2", style = MaterialTheme.typography.button) }

        Icon(
            Icons.Default.Favorite,
            null,
            modifier = Modifier.weight(1f)
        )
        Icon(
            Icons.Default.Share,
            null,
            modifier = Modifier.weight(1f)
        )
    }

    val supportingText: @Composable () -> Unit = {
        Text("Greyhound divisively hello coldly wonderfully marginally far upon excluding.")
    }

    Column(
        modifier = Modifier.padding(5.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        MaterialCard(
            supportingText = supportingText,
            header = {
                ListItem(
                    text = { Text("Title goes here") },
                    secondaryText = { Text("Secondary text") },
                    icon = { Icon(Icons.Default.Image, null) }
                )
            },
            media = media,
            actions = actions
        )

        MaterialCard(
            headerOnTop = false,
            supportingText = supportingText,
            header = {
                ListItem(
                    text = { Text("Title goes here") },
                    secondaryText = { Text("Secondary text") }
                )
            },
            media = media,
            actions = actions
        )

        MaterialCard(
            headerOnTop = false,
            header = {
                ListItem(
                    text = { Text("Title goes here") },
                    secondaryText = { Text("Secondary text") }
                )
            },
            media = media,
            actions = actions
        )

        MaterialCard(
            supportingText = supportingText,
            media = media,
            actions = actions
        )

        MaterialCard(
            headerOnTop = false,
            supportingText = supportingText,
            header = {
                ListItem(
                    text = { Text("Title goes here") },
                    secondaryText = { Text("Secondary text") }
                )
            },
            actions = actions
        )

        MaterialCard(
            headerOnTop = false,
            supportingText = supportingText,
            header = {
                ListItem(
                    text = { Text("Title goes here") },
                    secondaryText = { Text("Secondary text") }
                )
            },
            media = media,
        )

        MaterialCard(
            media = media,
            actions = actions
        )

        MaterialCard(
            headerOnTop = false,
            supportingText = supportingText,
            header = {
                ListItem(
                    text = { Text("Title goes here") },
                    secondaryText = { Text("Secondary text") }
                )
            }
        )

        MaterialCard(
            supportingText = supportingText,
            actions = actions
        )

        MaterialCard(
            header = {
                ListItem(
                    text = { Text("Title goes here") },
                    secondaryText = { Text("Secondary text") }
                )
            },
            actions = actions
        )

        MaterialCard(
            shape = RoundedCornerShape(16.dp),
            backgroundColor = Color.Blue,
            border = BorderStroke(1.dp, Color.Red),
            elevation = 5.dp,
            headerOnTop = false,
            supportingText = supportingText,
            header = {
                ListItem(
                    text = { Text("Title goes here") },
                    secondaryText = { Text("Secondary text") }
                )
            },
            media = media,
            actions = actions
        )

    }
}

@Composable
fun NestedScrollExample() {
    // here we use LazyColumn that has build-in nested scroll, but we want to act like a
    // parent for this LazyColumn and participate in its nested scroll.
    // Let's make a collapsing toolbar for LazyColumn
    val toolbarHeight = 48.dp
    val toolbarHeightPx = with(LocalDensity.current) { toolbarHeight.roundToPx().toFloat() }
    // our offset to collapse toolbar
    val toolbarOffsetHeightPx = remember { mutableStateOf(0f) }
    // now, let's create connection to the nested scroll system and listen to the scroll
    // happening inside child LazyColumn
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // try to consume before LazyColumn to collapse toolbar if needed, hence pre-scroll
                val delta = available.y
                val newOffset = toolbarOffsetHeightPx.value + delta
                toolbarOffsetHeightPx.value = newOffset.coerceIn(-toolbarHeightPx, 0f)
                // here's the catch: let's pretend we consumed 0 in any case, since we want
                // LazyColumn to scroll anyway for good UX
                // We're basically watching scroll without taking it
                return Offset.Zero
            }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            // attach as a parent to the nested scroll system
            .nestedScroll(nestedScrollConnection)
    ) {
        // our list with build in nested scroll support that will notify us about its scroll
        LazyColumn(contentPadding = PaddingValues(top = toolbarHeight)) {
            items(100) { index ->
                Text(
                    "I'm item $index", modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            }
        }
        TopAppBar(
            modifier = Modifier
                .height(toolbarHeight)
                .align(Alignment.TopCenter)
                .offset { IntOffset(x = 0, y = toolbarOffsetHeightPx.value.roundToInt()) },
            title = { Text("toolbar offset is ${toolbarOffsetHeightPx.value}") }
        )

        BottomAppBar(
            modifier = Modifier
                .height(toolbarHeight)
                .align(Alignment.BottomCenter)
                .offset { IntOffset(x = 0, y = -toolbarOffsetHeightPx.value.roundToInt()) }
        ) { Text("bottom bar offset is ${toolbarOffsetHeightPx.value}") }
    }
}

@Composable
fun CustomNestedScrollExample() {

    val scope = rememberCoroutineScope()
    val state = rememberLazyListState()

    var showInfo by remember { mutableStateOf(false) }

    val animationSpec = spring<Int>(Spring.DampingRatioMediumBouncy)

    val topBar = remember {
        CoordinatorModel(56.dp) { it, model ->
            val animateTopBar by animateIntAsState(if (showInfo) 0 else (it.roundToInt()), animationSpec)
            TopAppBar(
                modifier = Modifier
                    .height(56.dp)
                    .alpha(1f - (-animateTopBar / model.heightPx))
                    .align(Alignment.TopCenter)
                    .coordinatorOffset(y = animateTopBar),
                title = { Text("toolbar offset is $it") }
            )
        }
    }

    val bottomBar = remember {
        CoordinatorModel(56.dp) { it, model ->
            val animateBottomBar by animateIntAsState(if (showInfo) 0 else (it.roundToInt()), animationSpec)
            BottomAppBar(
                modifier = Modifier
                    .height(56.dp)
                    .alpha(1f - (-animateBottomBar / model.heightPx))
                    .align(Alignment.BottomCenter)
                    .coordinatorOffset(y = -animateBottomBar)
            ) { Text("bottom bar offset is $it") }
        }
    }

    val scrollToTop = remember {
        CoordinatorModel(72.dp) { it, model ->
            val animateFab by animateIntAsState(if (showInfo) 0 else (-it.roundToInt()), animationSpec)
            FloatingActionButton(
                onClick = { scope.launch { state.animateScrollToItem(0) } },
                modifier = Modifier
                    .padding(bottom = 56.dp)
                    .padding(12.dp)
                    .alpha(1f - (animateFab / model.heightPx))
                    .align(Alignment.BottomEnd)
                    .coordinatorOffset(animateFab)
            ) { Icon(Icons.Default.VerticalAlignTop, null, modifier = Modifier.rotate(animateFab.toFloat())) }
        }
    }

    val scrollToBottom = remember {
        CoordinatorModel(72.dp) { it, model ->
            val animateFab by animateIntAsState(if (showInfo) 0 else (it.roundToInt()), animationSpec)
            FloatingActionButton(
                onClick = { scope.launch { state.animateScrollToItem(100) } },
                modifier = Modifier
                    .padding(bottom = 56.dp)
                    .padding(12.dp)
                    .alpha(1f - (-animateFab / model.heightPx))
                    .align(Alignment.BottomStart)
                    .coordinatorOffset(animateFab)
            ) { Icon(Icons.Default.VerticalAlignBottom, null, modifier = Modifier.rotate(-animateFab.toFloat())) }
        }
    }

    Coordinator(
        topBar = topBar,
        bottomBar = bottomBar,
        scrollToTop, scrollToBottom
    ) {
        LazyColumn(
            state = state,
            contentPadding = PaddingValues(
                top = animateDpAsState(if (showInfo) 56.dp else 0.dp).value,
                bottom = animateDpAsState(if (showInfo) 56.dp else 0.dp).value
            )
        ) {
            items(100) { index ->
                Text(
                    "I'm item $index",
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showInfo = !showInfo
                            if (!showInfo) {
                                topBar.offsetHeightPx.value = -topBar.heightPx
                                bottomBar.offsetHeightPx.value = -bottomBar.heightPx
                                scrollToTop.offsetHeightPx.value = -scrollToTop.heightPx
                                scrollToBottom.offsetHeightPx.value = -scrollToBottom.heightPx
                            }
                        }
                        .padding(16.dp)
                )
            }
        }
    }
}