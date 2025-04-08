package com.programmersbox.uiviews.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.view.WindowManager
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.ActivityResult
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.BottomAppBarScrollBehavior
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.programmersbox.uiviews.GridChoice
import com.programmersbox.uiviews.R
import com.programmersbox.uiviews.presentation.settings.SourceChooserScreen
import com.programmersbox.uiviews.presentation.settings.TranslationScreen
import com.programmersbox.uiviews.repository.ChangingSettingsRepository
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import kotlin.properties.Delegates

@OptIn(ExperimentalMaterial3Api::class)
val LocalBottomAppBarScrollBehavior = staticCompositionLocalOf<BottomAppBarScrollBehavior> { error("") }

fun Int.toComposeColor() = Color(this)

@Composable
fun <T : Any> rememberMutableStateListOf(vararg elements: T): SnapshotStateList<T> = rememberSaveable(
    saver = listSaver(
        save = { it.toList() },
        restore = { it.toMutableStateList() }
    )
) { elements.toList().toMutableStateList() }

class CoordinatorModel(
    val height: Dp,
    val show: Boolean = true,
    val content: @Composable BoxScope.(Float, CoordinatorModel) -> Unit,
) {
    var heightPx by Delegates.notNull<Float>()
    val offsetHeightPx = mutableFloatStateOf(0f)

    @Composable
    internal fun Setup() {
        heightPx = with(LocalDensity.current) { height.roundToPx().toFloat() }
    }

    @Composable
    fun Content(scope: BoxScope) = scope.content(offsetHeightPx.floatValue, this)
}

fun Modifier.coordinatorOffset(x: Int = 0, y: Int = 0) = offset { IntOffset(x = x, y = y) }

@Composable
fun Coordinator(
    topBar: CoordinatorModel? = null,
    bottomBar: CoordinatorModel? = null,
    vararg otherCoords: CoordinatorModel,
    content: @Composable BoxScope.() -> Unit,
) = Coordinator(topBar, bottomBar, otherCoords.toList(), content)

@Composable
fun Coordinator(
    topBar: CoordinatorModel? = null,
    bottomBar: CoordinatorModel? = null,
    otherCoords: List<CoordinatorModel>,
    content: @Composable BoxScope.() -> Unit,
) {
    topBar?.Setup()
    bottomBar?.Setup()
    otherCoords.fastForEach { it.Setup() }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y

                topBar?.let {
                    val topBarOffset = it.offsetHeightPx.floatValue + delta
                    it.offsetHeightPx.floatValue = topBarOffset.coerceIn(-it.heightPx, 0f)
                }

                bottomBar?.let {
                    val bottomBarOffset = it.offsetHeightPx.floatValue + delta
                    it.offsetHeightPx.floatValue = bottomBarOffset.coerceIn(-it.heightPx, 0f)
                }

                otherCoords.fastForEach { c ->
                    c.let {
                        val offset = it.offsetHeightPx.floatValue + delta
                        it.offsetHeightPx.floatValue = offset.coerceIn(-it.heightPx, 0f)
                    }
                }

                return Offset.Zero
            }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection)
    ) {
        content()
        otherCoords.filter(CoordinatorModel::show).fastForEach { it.Content(this) }
        topBar?.let { if (it.show) it.Content(this) }
        bottomBar?.let { if (it.show) it.Content(this) }
    }
}

val currentColorScheme: ColorScheme
    @Composable
    get() {
        val darkTheme = AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES ||
                (isSystemInDarkTheme() && AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && darkTheme -> dynamicDarkColorScheme(LocalContext.current)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !darkTheme -> dynamicLightColorScheme(LocalContext.current)
            darkTheme -> darkColorScheme(
                primary = Color(0xff90CAF9),
                secondary = Color(0xff90CAF9)
            )

            else -> lightColorScheme(
                primary = Color(0xff2196F3),
                secondary = Color(0xff90CAF9)
            )
        }
    }

fun Color.contrastAgainst(background: Color): Float {
    val fg = if (alpha < 1f) compositeOver(background) else this

    val fgLuminance = fg.luminance() + 0.05f
    val bgLuminance = background.luminance() + 0.05f

    return kotlin.math.max(fgLuminance, bgLuminance) / kotlin.math.min(fgLuminance, bgLuminance)
}

val LocalWindowSizeClass = staticCompositionLocalOf<WindowSizeClass> {
    error("No WindowSizeClass available")
}

val gridColumns: Int
    @Composable get() = when (LocalWindowSizeClass.current.widthSizeClass) {
        WindowWidthSizeClass.Compact -> 3
        WindowWidthSizeClass.Medium -> 5
        else -> 6
    }

@Composable
fun adaptiveGridCell(): GridCells {
    val gridChoice by LocalSettingsHandling.current.rememberGridChoice()
    val width = ComposableUtils.IMAGE_WIDTH
    return when (gridChoice) {
        GridChoice.FullAdaptive -> remember { CustomAdaptive(width) }
        GridChoice.Adaptive -> remember { GridCells.Adaptive(width) }
        GridChoice.Fixed -> GridCells.Fixed(gridColumns)
        else -> GridCells.Fixed(gridColumns)
    }
}

class CustomAdaptive(private val minSize: Dp) : GridCells {
    init {
        require(minSize > 0.dp)
    }

    override fun Density.calculateCrossAxisCellSizes(
        availableSize: Int,
        spacing: Int,
    ): List<Int> {
        val count = maxOf((availableSize + spacing) / (minSize.roundToPx() + spacing), 1) + 1
        return calculateCellsCrossAxisSizeImpl(availableSize, count, spacing)
    }

    override fun hashCode(): Int {
        return minSize.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return other is CustomAdaptive && minSize == other.minSize
    }
}

private fun calculateCellsCrossAxisSizeImpl(
    gridSize: Int,
    slotCount: Int,
    spacing: Int,
): List<Int> {
    val gridSizeWithoutSpacing = gridSize - spacing * (slotCount - 1)
    val slotSize = gridSizeWithoutSpacing / slotCount
    val remainingPixels = gridSizeWithoutSpacing % slotCount
    return List(slotCount) {
        slotSize + if (it < remainingPixels) 1 else 0
    }
}

/**
 * Registers a broadcast receiver and unregisters at the end of the composable lifecycle
 *
 * @param defaultValue the default value that this starts as
 * @param intentFilter the filter for intents
 * @see IntentFilter
 * @param tick the callback from the broadcast receiver
 */
@Composable
fun <T : Any> broadcastReceiver(defaultValue: T, intentFilter: IntentFilter, tick: (context: Context, intent: Intent) -> T): State<T> {
    val item: MutableState<T> = remember { mutableStateOf(defaultValue) }
    val context = LocalContext.current

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                item.value = tick(context, intent)
            }
        }
        context.registerReceiver(receiver, intentFilter)
        onDispose { context.unregisterReceiver(receiver) }
    }
    return item
}

/**
 * Registers a broadcast receiver and unregisters at the end of the composable lifecycle
 *
 * @param defaultValue the default value that this starts as
 * @param intentFilter the filter for intents.
 * @see IntentFilter
 * @param tick the callback from the broadcast receiver
 */
@Composable
fun <T : Any> broadcastReceiverNullable(defaultValue: T?, intentFilter: IntentFilter, tick: (context: Context, intent: Intent) -> T?): State<T?> {
    val item: MutableState<T?> = remember { mutableStateOf(defaultValue) }
    val context = LocalContext.current

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                item.value = tick(context, intent)
            }
        }
        context.registerReceiver(receiver, intentFilter)
        onDispose { context.unregisterReceiver(receiver) }
    }
    return item
}

@Composable
fun BackButton() {
    val navController = LocalNavController.current
    IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsetSmallTopAppBar(
    modifier: Modifier = Modifier,
    insetPadding: WindowInsets = TopAppBarDefaults.windowInsets,
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(),
    scrollBehavior: TopAppBarScrollBehavior? = null,
    title: @Composable () -> Unit = {},
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = title,
        modifier = modifier,
        navigationIcon = navigationIcon,
        actions = actions,
        windowInsets = insetPadding,
        colors = colors,
        scrollBehavior = scrollBehavior
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsetCenterAlignedTopAppBar(
    modifier: Modifier = Modifier,
    insetPadding: WindowInsets = WindowInsets.statusBars,
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(),
    scrollBehavior: TopAppBarScrollBehavior? = null,
    title: @Composable () -> Unit = {},
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    CenterAlignedTopAppBar(
        modifier = modifier,
        windowInsets = insetPadding,
        title = title,
        navigationIcon = navigationIcon,
        actions = actions,
        scrollBehavior = scrollBehavior,
        colors = colors
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsetMediumTopAppBar(
    modifier: Modifier = Modifier,
    insetPadding: WindowInsets = WindowInsets.statusBars,
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(),
    scrollBehavior: TopAppBarScrollBehavior? = null,
    title: @Composable () -> Unit = {},
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    MediumTopAppBar(
        modifier = modifier,
        windowInsets = insetPadding,
        title = title,
        navigationIcon = navigationIcon,
        actions = actions,
        scrollBehavior = scrollBehavior,
        colors = colors
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun InsetLargeTopAppBar(
    modifier: Modifier = Modifier,
    insetPadding: WindowInsets = WindowInsets.statusBars,
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(),
    scrollBehavior: TopAppBarScrollBehavior? = null,
    title: @Composable () -> Unit = {},
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    LargeFlexibleTopAppBar(
        modifier = modifier,
        windowInsets = insetPadding,
        title = title,
        navigationIcon = navigationIcon,
        actions = actions,
        scrollBehavior = scrollBehavior,
        colors = colors
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LoadingDialog(
    showLoadingDialog: Boolean,
    onDismissRequest: () -> Unit,
) {
    if (showLoadingDialog) {
        Dialog(
            onDismissRequest = onDismissRequest,
            DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(100.dp)
                    .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(28.0.dp))
            ) {
                Column {
                    ContainedLoadingIndicator(
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Text(text = stringResource(id = R.string.loading), Modifier.align(Alignment.CenterHorizontally))
                }
            }
        }

    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun loadingDialog(): MutableState<Boolean> {
    val showLoadingDialog = remember { mutableStateOf(false) }

    if (showLoadingDialog.value) {
        Dialog(
            onDismissRequest = { showLoadingDialog.value = false },
            DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(100.dp)
                    .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(28.0.dp))
            ) {
                Column {
                    ContainedLoadingIndicator(
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Text(text = stringResource(id = R.string.loading), Modifier.align(Alignment.CenterHorizontally))
                }
            }
        }
    }

    return showLoadingDialog
}

val Emerald = Color(0xFF2ecc71)
val Sunflower = Color(0xFFf1c40f)
val Alizarin = Color(0xFFe74c3c)

@Composable
fun Color.animate(label: String = "") = animateColorAsState(this, label = label)

@Composable
fun Color?.animate(default: Color, label: String = "") =
    animateColorAsState(this ?: default, label = label)

@Composable
fun LazyListState.isScrollingUp(): Boolean {
    var previousIndex by remember(this) { mutableIntStateOf(firstVisibleItemIndex) }
    var previousScrollOffset by remember(this) { mutableIntStateOf(firstVisibleItemScrollOffset) }
    return remember(this) {
        derivedStateOf {
            if (previousIndex != firstVisibleItemIndex) {
                previousIndex > firstVisibleItemIndex
            } else {
                previousScrollOffset >= firstVisibleItemScrollOffset
            }.also {
                previousIndex = firstVisibleItemIndex
                previousScrollOffset = firstVisibleItemScrollOffset
            }
        }
    }.value
}

fun ManagedActivityResultLauncher<Intent, ActivityResult>.launchCatching(
    intent: Intent,
    options: ActivityOptionsCompat? = null,
) = runCatching {
    launch(
        input = intent,
        options = options
    )
}

@Composable
fun HideSystemBarsWhileOnScreen() {
    val changingSettingsRepository: ChangingSettingsRepository = koinInject()

    LifecycleResumeEffect(Unit) {
        changingSettingsRepository.showNavBar.tryEmit(false)
        onPauseOrDispose { changingSettingsRepository.showNavBar.tryEmit(true) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun showSourceChooser(): MutableState<Boolean> {
    val showSourceChooser = remember { mutableStateOf(false) }
    val state = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    if (showSourceChooser.value) {
        ModalBottomSheet(
            onDismissRequest = { showSourceChooser.value = false },
            sheetState = state,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            SourceChooserScreen(
                onChosen = {
                    scope.launch { state.hide() }
                        .invokeOnCompletion { showSourceChooser.value = false }
                }
            )
        }
    }

    return showSourceChooser
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun showTranslationScreen(): MutableState<Boolean> {
    val showTranslationScreen = remember { mutableStateOf(false) }

    if (showTranslationScreen.value) {
        ModalBottomSheet(
            onDismissRequest = { showTranslationScreen.value = false },
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            TranslationScreen()
        }
    }

    return showTranslationScreen
}

@Composable
fun BoxWithConstraintsScope.bounds(paddingValues: PaddingValues): Array<Rect> {
    val topBarBounds = with(LocalDensity.current) {
        Rect(
            Offset(0f, 0f),
            Offset(maxWidth.toPx(), paddingValues.calculateTopPadding().toPx())
        )
    }
    val bottomBarBounds = with(LocalDensity.current) {
        val bottomPaddingPx = paddingValues.calculateBottomPadding().toPx()
        Rect(
            Offset(0f, maxHeight.toPx() - bottomPaddingPx),
            Offset(maxWidth.toPx(), maxHeight.toPx())
        )
    }
    return arrayOf(topBarBounds, bottomBarBounds)
}

@Composable
fun BoxWithConstraintsScope.topBounds(paddingValues: PaddingValues): Rect {
    return with(LocalDensity.current) {
        Rect(
            Offset(0f, 0f),
            Offset(maxWidth.toPx(), paddingValues.calculateTopPadding().toPx())
        )
    }
}

val LocalNavHostPadding = staticCompositionLocalOf<PaddingValues> { error("") }

@Composable
fun hapticInteractionSource(
    hapticFeedbackType: HapticFeedbackType = HapticFeedbackType.LongPress,
    enabled: Boolean = true,
): MutableInteractionSource {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    LaunchedEffect(isPressed) {
        if (isPressed && enabled) {
            haptic.performHapticFeedback(hapticFeedbackType)
        }
    }

    return interactionSource
}

@Composable
fun KeepScreenOn() {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val window = context.findActivity().window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}

/*
// Need `implementation("com.mutualmobile:composesensors:1.1.2")` to make this work
@Composable
fun HingeDetection(
    locationAlignment: Alignment = Alignment.TopCenter,
    timeoutDurationMillis: Long = 2000
) {
    val hingeState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        rememberHingeAngleSensorState()
    } else {
        null
    }

    hingeState?.let { hinge ->
        val alphaHinge = remember { Animatable(0f) }
        var firstCount by remember { mutableIntStateOf(0) }

        LaunchedEffect(hinge.angle) {
            alphaHinge.animateTo(1f)
            delay(timeoutDurationMillis)
            alphaHinge.animateTo(0f)
        }

        Box(
            contentAlignment = locationAlignment,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = alphaHinge.value }
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(WindowInsets.systemBars.asPaddingValues())
                    .clip(MaterialTheme.shapes.medium)
                    .background(Color.Black.copy(alpha = .75f))
                    .padding(16.dp)
            ) {
                Icon(
                    Icons.Default.DevicesFold,
                    null,
                    tint = Color.White,
                    modifier = Modifier.weight(1f, false)
                )

                Text(
                    "${hinge.angle}°",
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )

                Canvas(
                    Modifier
                        .size(24.dp)
                        .weight(1f, false)
                ) {
                    drawArc(
                        Color.White,
                        270f,
                        hinge.angle,
                        true
                    )
                    drawCircle(
                        color = Color.White,
                        style = Stroke()
                    )
                }
            }
        }
    }
}
*/
