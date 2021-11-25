package com.programmersbox.otakuworld

import android.os.Build
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.ButtonColors
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ButtonElevation
import androidx.compose.material.FabPosition
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import com.google.accompanist.pager.ExperimentalPagerApi
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.helpfulutils.itemRangeOf
import com.programmersbox.models.ApiService
import com.programmersbox.models.ItemModel
import com.programmersbox.models.SwatchInfo
import com.programmersbox.uiviews.utils.*
import com.skydoves.landscapist.glide.GlideImage
import com.skydoves.landscapist.palette.BitmapPalette
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.properties.Delegates
import kotlin.random.Random
import com.programmersbox.anime_sources.Sources as ASources
import com.programmersbox.manga_sources.Sources as MSources
import com.programmersbox.novel_sources.Sources as NSources

class MainActivity : AppCompatActivity() {

    private val disposable = CompositeDisposable()
    private val sourceList = mutableStateListOf<ItemModel>()
    private val favorites = mutableStateListOf<DbModel>()

    @ExperimentalPagerApi
    @ExperimentalMaterial3Api
    @ExperimentalAnimationApi
    @ExperimentalFoundationApi
    @ExperimentalMaterialApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /*Single.merge(
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
            .addTo(disposable)*/

        val strings = itemRangeOf("Hello", "World", "How", "Are", "You?")

        setContent {

            val darkTheme = isSystemInDarkTheme()
            val colorScheme = when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && darkTheme -> dynamicDarkColorScheme(LocalContext.current)
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !darkTheme -> dynamicLightColorScheme(LocalContext.current)
                darkTheme -> darkColorScheme()
                else -> lightColorScheme()
            }

            LaunchedEffect(Unit) { currentScheme = colorScheme }

            val scope = rememberCoroutineScope()

            androidx.compose.material3.MaterialTheme(colorScheme = currentScheme) {

                /*var list by remember { mutableStateOf(listOf("A", "B", "C")) }
                LazyColumn {
                    item {
                        androidx.compose.material3.Button(onClick = { list = list.shuffled() }) {
                            androidx.compose.material3.Text("Shuffle")
                        }
                    }
                    items(list, key = { it }) {
                        Text(
                            "Item $it",
                            Modifier.animateItemPlacement(),
                            color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground
                        )
                    }
                }*/

                val scrollBehavior = remember { TopAppBarDefaults.pinnedScrollBehavior() }
                val scaffoldState = rememberBottomSheetScaffoldState()

                BackHandler(scaffoldState.bottomSheetState.isExpanded) { scope.launch { scaffoldState.bottomSheetState.collapse() } }

                val showSettings: () -> Unit = {
                    TestDialogFragment().showNow(supportFragmentManager, null)
                    scope.launch { scaffoldState.bottomSheetState.collapse() }
                }

                BottomSheetScaffold(
                    scaffoldState = scaffoldState,
                    sheetContent = {
                        Column {
                            CompositionLocalProvider(
                                androidx.compose.material3.LocalContentColor provides
                                        androidx.compose.material3.contentColorFor(androidx.compose.material3.MaterialTheme.colorScheme.surface)
                                            .animate().value
                            ) {
                                Divider(color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f).animate().value)

                                PreferenceSetting(settingTitle = "") {
                                    androidx.compose.material3.IconButton(onClick = showSettings) {
                                        androidx.compose.material3.Icon(Icons.Default.Settings, null)
                                    }
                                }

                                var themeSetting by remember {
                                    mutableStateOf(
                                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && darkTheme ||
                                                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !darkTheme ||
                                                darkTheme
                                    )
                                }

                                val context = LocalContext.current

                                SwitchSetting(
                                    summaryValue = "Current Theme: ${if (themeSetting) "Dark" else "Light"}",
                                    settingIcon = { androidx.compose.material3.Icon(Icons.Default.SettingsBrightness, null) },
                                    settingTitle = "Theme",
                                    value = themeSetting,
                                    updateValue = {
                                        themeSetting = it
                                        currentScheme = when {
                                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && it -> dynamicDarkColorScheme(context)
                                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !it -> dynamicLightColorScheme(context)
                                            it -> darkColorScheme()
                                            else -> lightColorScheme()
                                        }
                                    }
                                )

                                PreferenceSetting(
                                    settingTitle = "System Settings",
                                    settingIcon = { androidx.compose.material3.Icon(Icons.Default.Settings, null) },
                                    onClick = showSettings
                                ) { androidx.compose.material3.Icon(Icons.Default.ChevronRight, null) }
                            }
                        }
                    },
                    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                    topBar = {
                        Column {
                            CenterAlignedTopAppBar(
                                title = { androidx.compose.material3.Text("Fun Playground") },
                                scrollBehavior = scrollBehavior
                            )
                            Divider(color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f).animate().value)
                        }
                    },
                    backgroundColor = androidx.compose.material3.MaterialTheme.colorScheme.background.animate().value,
                    contentColor = androidx.compose.material3.contentColorFor(androidx.compose.material3.MaterialTheme.colorScheme.background)
                        .animate().value,
                    sheetBackgroundColor = androidx.compose.material3.MaterialTheme.colorScheme.surface.animate().value,
                    sheetContentColor = androidx.compose.material3.contentColorFor(androidx.compose.material3.MaterialTheme.colorScheme.surface)
                        .animate().value,
                    sheetPeekHeight = 0.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(it),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {

                        Text("Outlined Button")
                        androidx.compose.material3.OutlinedButton(
                            onClick = {
                                scope.launch {
                                    if (scaffoldState.bottomSheetState.isCollapsed) scaffoldState.bottomSheetState.expand()
                                    else scaffoldState.bottomSheetState.collapse()
                                }
                            },
                            modifier = Modifier.coloredShadow(androidx.compose.material3.MaterialTheme.colorScheme.primary.animate().value),
                            border = BorderStroke(
                                1.dp,
                                androidx.compose.material3.MaterialTheme.colorScheme.primary.animate().value
                            )
                        ) { androidx.compose.material3.Text("Open Settings") }

                        Text("Button with Colored Shadow")
                        androidx.compose.material3.Button(
                            onClick = {
                                scope.launch {
                                    if (scaffoldState.bottomSheetState.isCollapsed) scaffoldState.bottomSheetState.expand()
                                    else scaffoldState.bottomSheetState.collapse()
                                }
                            },
                            modifier = Modifier.coloredShadow(androidx.compose.material3.MaterialTheme.colorScheme.primary.animate().value),
                        ) { androidx.compose.material3.Text("Open Settings") }

                        Text("FilledTonalButton")
                        androidx.compose.material3.FilledTonalButton(
                            onClick = {
                                scope.launch {
                                    if (scaffoldState.bottomSheetState.isCollapsed) scaffoldState.bottomSheetState.expand()
                                    else scaffoldState.bottomSheetState.collapse()
                                }
                            },
                            border = BorderStroke(
                                1.dp,
                                androidx.compose.material3.MaterialTheme.colorScheme.primary.animate().value
                            )
                        ) { androidx.compose.material3.Text("Open Settings") }

                        Text("FilledTonalButton with Colored Shadow")
                        androidx.compose.material3.FilledTonalButton(
                            onClick = {
                                scope.launch {
                                    if (scaffoldState.bottomSheetState.isCollapsed) scaffoldState.bottomSheetState.expand()
                                    else scaffoldState.bottomSheetState.collapse()
                                }
                            },
                            modifier = Modifier.coloredShadow(androidx.compose.material3.MaterialTheme.colorScheme.primary.animate().value),
                            border = BorderStroke(
                                1.dp,
                                androidx.compose.material3.MaterialTheme.colorScheme.primary.animate().value
                            )
                        ) { androidx.compose.material3.Text("Open Settings") }
                    }
                }

                /*val scrollBehavior = remember { TopAppBarDefaults.enterAlwaysScrollBehavior() }

                androidx.compose.material3.Scaffold(
                    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = { androidx.compose.material3.Text("Fun") },
                            scrollBehavior = scrollBehavior
                        )
                    }
                ) { p ->
                    LazyColumn(contentPadding = p) {

                        item {

                            PreferenceSetting(
                                settingTitle = "System Settings",
                                onClick = { TestDialogFragment().showNow(supportFragmentManager, null) }
                            ) { androidx.compose.material3.Icon(Icons.Default.ChevronRight, null) }

                        }

                        item {
                            var themeSetting by remember {
                                mutableStateOf(
                                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && darkTheme ||
                                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !darkTheme ||
                                            darkTheme
                                )
                            }

                            val context = LocalContext.current

                            SwitchSetting(
                                summaryValue = "Current Theme: ${if (themeSetting) "Dark" else "Light"}",
                                settingTitle = "Theme",
                                value = themeSetting,
                                updateValue = {
                                    themeSetting = it
                                    currentScheme = when {
                                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && it -> dynamicDarkColorScheme(context)
                                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !it -> dynamicLightColorScheme(context)
                                        it -> darkColorScheme()
                                        else -> lightColorScheme()
                                    }
                                }
                            )

                        }
                    }
                }*/

                /*var showBanner by remember { mutableStateOf(false) }

                BannerBox(
                    showBanner = showBanner,
                    banner = {
                        Card {
                            ListItem(text = { Text("Hello World") })
                        }
                    },
                    content = {
                        Column {
                            Text("Hello!")
                            TopAppBar(title = { Text("World!") })
                            BottomAppBar { Text("Hello World!") }
                            Button(onClick = { showBanner = !showBanner }) {
                                Text("Show/Hide Banner", style = MaterialTheme.typography.button)
                            }
                            Text(
                                "Show/Hide Banner here too!",
                                modifier = Modifier
                                    .combineClickableWithIndication(
                                        onLongPress = { showBanner = it == ComponentState.Pressed }
                                    )
                            )
                        }
                    }
                )*/

                /*var showInfo by remember { mutableStateOf(false) }

                val scrollBehavior = remember {
                    TopAppBarDefaults.enterAlwaysScrollBehavior { !showInfo }
                }

                val currentOffset = animateFloatAsState(targetValue = if (showInfo) 0f else scrollBehavior.offsetLimit)

                if (showInfo) scrollBehavior.offset = currentOffset.value else scrollBehavior.offset = currentOffset.value

                androidx.compose.material3.Scaffold(
                    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                    topBar = {
                        CenterAlignedTopAppBar(
                            actions = {
                                androidx.compose.material3.Text(
                                    "1/17",
                                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
                                )
                            },
                            title = {
                                AnimatedContent(
                                    targetState = System.currentTimeMillis(),
                                    transitionSpec = {
                                        (slideInVertically { height -> height } + fadeIn() with
                                                slideOutVertically { height -> -height } + fadeOut())
                                            .using(SizeTransform(clip = false))
                                    }
                                ) { targetTime ->
                                    androidx.compose.material3.Text(
                                        DateFormat.format("HH:mm a", targetTime).toString(),
                                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
                                    )
                                }
                            },
                            navigationIcon = {
                                Row {
                                    androidx.compose.material3.Icon(
                                        Icons.Default.BatteryFull,
                                        contentDescription = null,
                                        tint = animateColorAsState(Color.White).value
                                    )
                                    AnimatedContent(
                                        targetState = 100,
                                        transitionSpec = {
                                            if (targetState > initialState) {
                                                slideInVertically { height -> height } + fadeIn() with
                                                        slideOutVertically { height -> -height } + fadeOut()
                                            } else {
                                                slideInVertically { height -> -height } + fadeIn() with
                                                        slideOutVertically { height -> height } + fadeOut()
                                            }
                                                .using(SizeTransform(clip = false))
                                        }
                                    ) { targetBattery ->
                                        androidx.compose.material3.Text(
                                            "$targetBattery%",
                                            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            },
                            scrollBehavior = scrollBehavior
                        )
                        *//*SmallTopAppBar(
                            title = { androidx.compose.material3.Text("Large TopAppBar") },
                            navigationIcon = {
                                androidx.compose.material3.IconButton(onClick = {  }) {
                                    androidx.compose.material3.Icon(
                                        imageVector = Icons.Filled.Menu,
                                        contentDescription = "Localized description"
                                    )
                                }
                            },
                            actions = {
                                androidx.compose.material3.IconButton(onClick = {  }) {
                                    androidx.compose.material3.Icon(
                                        imageVector = Icons.Filled.Favorite,
                                        contentDescription = "Localized description"
                                    )
                                }
                            },
                            scrollBehavior = scrollBehavior
                        )*//*
                    },
                    floatingActionButton = {
                        androidx.compose.material3.FloatingActionButton(
                            onClick = { },
                            modifier = Modifier
                        ) { Icon(Icons.Default.VerticalAlignTop, null) }
                    },
                    floatingActionButtonPosition = androidx.compose.material3.FabPosition.End,
                    bottomBar = {
                        SmallTopAppBar(
                            title = { androidx.compose.material3.Text("Large TopAppBar") },
                            navigationIcon = {
                                androidx.compose.material3.IconButton(onClick = { }) {
                                    androidx.compose.material3.Icon(
                                        imageVector = Icons.Filled.Menu,
                                        contentDescription = "Localized description"
                                    )
                                }
                            },
                            actions = {
                                androidx.compose.material3.IconButton(onClick = { }) {
                                    androidx.compose.material3.Icon(
                                        imageVector = Icons.Filled.Favorite,
                                        contentDescription = "Localized description"
                                    )
                                }
                            },
                            scrollBehavior = scrollBehavior
                        )
                    }
                ) { innerPadding ->
                    LazyColumn(
                        contentPadding = innerPadding,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val list = (0..75).map { it.toString() }
                        items(count = list.size) {
                            androidx.compose.material3.Text(
                                text = list[it],
                                style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .clickable { showInfo = !showInfo }
                            )
                        }
                    }
                }*/

                /*var stringer by remember { mutableStateOf(strings.item) }

                Column {
                    AnimatedContent(
                        targetState = stringer,
                        transitionSpec = {
                            // Compare the incoming number with the previous number.
                            *//*if (targetState > initialState) {
                                // If the target number is larger, it slides up and fades in
                                // while the initial (smaller) number slides up and fades out.
                                slideInVertically { height -> height } + fadeIn() with
                                        slideOutVertically { height -> -height } + fadeOut()
                            } else {
                                // If the target number is smaller, it slides down and fades in
                                // while the initial number slides down and fades out.
                                slideInVertically { height -> -height } + fadeIn() with
                                        slideOutVertically { height -> height } + fadeOut()
                            }*//*
                            (slideInVertically { height -> height } + fadeIn() with
                                    slideOutVertically { height -> -height } + fadeOut())
                                .using(
                                    // Disable clipping since the faded slide-in/out should
                                    // be displayed out of bounds.
                                    SizeTransform(clip = false)
                                )
                        }
                    ) { targetString -> Text(targetString) }
                    Button(
                        onClick = {
                            strings.next()
                            stringer = strings()
                        }
                    ) { Text("Next String") }
                }*/

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
                //CustomNestedScrollExample()
                //ScaffoldNestedScrollExample()

                /*Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {

                    val scope = rememberCoroutineScope()
                    var swipeState by remember { mutableStateOf(SwipeButtonState.INITIAL) }

                    Row {
                        Text("Here")

                        SwipeButton(
                            onSwiped = {
                                swipeState = SwipeButtonState.SWIPED
                                scope.launch {
                                    delay(2000)
                                    swipeState = SwipeButtonState.COLLAPSED
                                }
                            },
                            swipeButtonState = swipeState,
                            modifier = Modifier
                                .padding(16.dp)
                                .height(60.dp),
                            iconPadding = PaddingValues(4.dp),
                            shape = CircleShape,
                            loadingIndicator = {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colors.onPrimary,
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }
                        ) { Text("Delete") }
                    }*/
                /*val options = listOf("Option 1", "Option 2", "Option 3", "Option 4", "Option 5")
                var expanded by remember { mutableStateOf(false) }
                var selectedOptionText by remember { mutableStateOf("") }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    TextField(
                        value = selectedOptionText,
                        onValueChange = { selectedOptionText = it },
                        label = { Text("Label") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        colors = ExposedDropdownMenuDefaults.textFieldColors()
                    )
                    // filter options based on text field value
                    val filteringOptions = options.filter { it.contains(selectedOptionText, ignoreCase = true) }
                    if (filteringOptions.isNotEmpty()) {
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            filteringOptions.forEach { selectionOption ->
                                DropdownMenuItem(
                                    onClick = {
                                        selectedOptionText = selectionOption
                                        expanded = false
                                    }
                                ) { Text(text = selectionOption) }
                            }
                        }
                    }
                }
            }*/
            }
        }
    }

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

@Composable
fun ScaffoldNestedScrollExample() {
    val scope = rememberCoroutineScope()
    val state = rememberLazyListState()

    var showInfo by remember { mutableStateOf(false) }

    val animationSpec = spring<Int>(Spring.DampingRatioMediumBouncy)

    val topBar = remember {
        CoordinatorModel1(56.dp) { it, model ->
            val animateTopBar by animateIntAsState(if (showInfo) 0 else (it.roundToInt()), animationSpec)
            TopAppBar(
                modifier = Modifier
                    .height(56.dp - with(LocalDensity.current) { -animateTopBar.toDp() })
                    .alpha(1f - (-animateTopBar / model.heightPx))
                    .coordinatorOffset(y = animateTopBar),
                title = { Text("toolbar offset is $it") }
            )
        }
    }

    val bottomBar = remember {
        CoordinatorModel1(56.dp) { it, model ->
            val animateBottomBar by animateIntAsState(if (showInfo) 0 else (it.roundToInt()), animationSpec)
            BottomAppBar(
                modifier = Modifier
                    .height(56.dp - with(LocalDensity.current) { -animateBottomBar.toDp() })
                    .alpha(1f - (-animateBottomBar / model.heightPx))
                    .coordinatorOffset(y = -animateBottomBar)
            ) { Text("bottom bar offset is $it") }
        }
    }

    val scrollToTop = remember {
        CoordinatorModel1(56.dp) { it, model ->
            val animateFab by animateIntAsState(if (showInfo) 0 else (-it.roundToInt()), animationSpec)
            FloatingActionButton(
                onClick = { scope.launch { state.animateScrollToItem(0) } },
                modifier = Modifier
                    .alpha(1f - (animateFab / model.heightPx))
                    .coordinatorOffset(animateFab)
            ) { Icon(Icons.Default.VerticalAlignTop, null, modifier = Modifier.rotate(animateFab.toFloat())) }
        }
    }

    val scrollToBottom = remember {
        CoordinatorModel1(56.dp) { it, model ->
            val animateFab by animateIntAsState(if (showInfo) 0 else (it.roundToInt()), animationSpec)
            FloatingActionButton(
                onClick = { scope.launch { state.animateScrollToItem(100) } },
                modifier = Modifier
                    .padding(start = 12.dp)
                    .alpha(1f - (-animateFab / model.heightPx))
                    .coordinatorOffset(animateFab)
            ) { Icon(Icons.Default.VerticalAlignBottom, null, modifier = Modifier.rotate(-animateFab.toFloat())) }
        }
    }

    topBar.Setup()
    bottomBar.Setup()
    scrollToTop.Setup()
    scrollToBottom.Setup()

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y

                topBar.let {
                    val topBarOffset = it.offsetHeightPx.value + delta
                    it.offsetHeightPx.value = topBarOffset.coerceIn(-it.heightPx, 0f)
                }

                bottomBar.let {
                    val bottomBarOffset = it.offsetHeightPx.value + delta
                    it.offsetHeightPx.value = bottomBarOffset.coerceIn(-it.heightPx, 0f)
                }

                scrollToTop.let {
                    val offset = it.offsetHeightPx.value + delta
                    it.offsetHeightPx.value = offset.coerceIn(-it.heightPx, 0f)
                }

                scrollToBottom.let {
                    val offset = it.offsetHeightPx.value + delta
                    it.offsetHeightPx.value = offset.coerceIn(-it.heightPx, 0f)
                }

                return Offset.Zero
            }
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(nestedScrollConnection),
        topBar = { topBar.Content() },
        bottomBar = { bottomBar.Content() },
        floatingActionButton = { scrollToTop.Content() },
        floatingActionButtonPosition = FabPosition.End,
        isFloatingActionButtonDocked = true
    ) { p ->
        LazyColumn(
            state = state,
            contentPadding = p
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

        scrollToBottom.Content()
    }
}

class CoordinatorModel1(
    val height: Dp,
    val show: Boolean = true,
    val content: @Composable (Float, CoordinatorModel1) -> Unit
) {
    var heightPx by Delegates.notNull<Float>()
    val offsetHeightPx = mutableStateOf(0f)

    @Composable
    internal fun Setup() {
        heightPx = with(LocalDensity.current) { height.roundToPx().toFloat() }
    }

    @Composable
    fun Content() = content(offsetHeightPx.value, this)
}

@Composable
fun TestData() = Column { repeat(10) { Text("Hello $it") } }

@ExperimentalAnimationApi
@ExperimentalMaterialApi
@Composable
fun SwipeButton(
    onSwiped: () -> Unit,
    modifier: Modifier = Modifier,
    swipeButtonState: SwipeButtonState,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    elevation: ButtonElevation? = ButtonDefaults.elevation(),
    shape: Shape = MaterialTheme.shapes.small,
    border: BorderStroke? = null,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    icon: ImageVector = Icons.Default.ArrowForward,
    rotateIcon: Boolean = true,
    iconPadding: PaddingValues = PaddingValues(2.dp),
    loadingIndicator: @Composable BoxScope.() -> Unit = { HorizontalDottedProgressBar() },
    content: @Composable RowScope.() -> Unit
) {
    val contentColor by colors.contentColor(enabled)
    val dragOffset = remember { mutableStateOf(0f) }

    val collapsed = swipeButtonState == SwipeButtonState.COLLAPSED
    val swiped = swipeButtonState == SwipeButtonState.SWIPED

    Surface(
        modifier = modifier.animateContentSize(),
        shape = shape,
        color = colors.backgroundColor(enabled).value,
        contentColor = contentColor.copy(alpha = 1f),
        border = border,
        elevation = elevation?.elevation(enabled, interactionSource)?.value ?: 0.dp,
        onClick = {},
        enabled = enabled,
        role = Role.Button,
        interactionSource = interactionSource,
        indication = rememberRipple()
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.CenterStart
        ) {
            // Content
            val maxWidth = this.constraints.maxWidth.toFloat()

            when {
                collapsed -> {
                    val animatedProgress = remember { Animatable(initialValue = 0f) }
                    LaunchedEffect(Unit) {
                        animatedProgress.animateTo(
                            targetValue = 1f,
                            animationSpec = tween(600)
                        )
                    }
                    IconButton(
                        onClick = { },
                        modifier = Modifier
                            .scale(animatedProgress.value)
                            .padding(iconPadding)
                            .clip(CircleShape)
                            .background(MaterialTheme.colors.onPrimary)
                            .align(Alignment.Center)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Done,
                            contentDescription = "Done",
                            tint = MaterialTheme.colors.primary
                        )
                    }
                }
                swiped -> {
                    loadingIndicator()
                }
                else -> {
                    dragOffset.value = 0f // when button goes to inital state
                    CompositionLocalProvider(LocalContentAlpha provides contentColor.alpha) {
                        ProvideTextStyle(
                            value = MaterialTheme.typography.button
                        ) {
                            Row(
                                Modifier
                                    .fillMaxSize()
                                    .padding(contentPadding),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                                content = content
                            )
                        }
                    }
                }
            }
            // Swipe Component
            AnimatedVisibility(visible = !swiped) {
                IconButton(onClick = { }, enabled = enabled, modifier = Modifier
                    .padding(iconPadding)
                    .align(Alignment.CenterStart)
                    .offset { IntOffset(dragOffset.value.roundToInt(), 0) }
                    .draggable(
                        enabled = enabled,
                        orientation = Orientation.Horizontal,
                        state = rememberDraggableState { delta ->
                            val newValue = dragOffset.value + delta
                            dragOffset.value = newValue.coerceIn(0f, maxWidth)
                        },
                        onDragStopped = {
                            if (dragOffset.value > maxWidth * 2 / 3) {
                                dragOffset.value = maxWidth
                                onSwiped.invoke()
                            } else {
                                dragOffset.value = 0f
                            }
                        }
                    )
                    .background(MaterialTheme.colors.onPrimary, shape = CircleShape)
                ) {
                    Icon(
                        imageVector = icon,
                        modifier = Modifier.graphicsLayer {
                            if (rotateIcon) {
                                rotationZ += dragOffset.value / 5
                            }
                        },
                        contentDescription = "Arrow",
                        tint = colors.backgroundColor(enabled).value,
                    )
                }
            }
        }
    }
}

enum class SwipeButtonState {
    INITIAL, SWIPED, COLLAPSED
}

@Composable
fun HorizontalDottedProgressBar() {
    val color = MaterialTheme.colors.onPrimary
    val transition = rememberInfiniteTransition()
    val state by transition.animateFloat(
        initialValue = 0f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 700,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Reverse
        )
    )

    DrawCanvas(state = state, color = color)
}


@Composable
fun DrawCanvas(
    state: Float,
    color: Color,
) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp),
    ) {

        val radius = (4.dp).value
        val padding = (6.dp).value

        for (i in 1..5) {
            if (i - 1 == state.toInt()) {
                drawCircle(
                    radius = radius * 2,
                    brush = SolidColor(color),
                    center = Offset(
                        x = this.center.x + radius * 2 * (i - 3) + padding * (i - 3),
                        y = this.center.y
                    )
                )
            } else {
                drawCircle(
                    radius = radius,
                    brush = SolidColor(color),
                    center = Offset(
                        x = this.center.x + radius * 2 * (i - 3) + padding * (i - 3),
                        y = this.center.y
                    )
                )
            }
        }
    }
}

@ExperimentalMaterialApi
@ExperimentalFoundationApi
@Composable
fun BannerBox(
    showBanner: Boolean = false,
    banner: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        content()
        AnimatedVisibility(
            visible = showBanner,
            enter = slideInVertically { -it },
            exit = slideOutVertically { -it },
            modifier = Modifier.align(Alignment.TopCenter)
        ) { banner() }
    }
}