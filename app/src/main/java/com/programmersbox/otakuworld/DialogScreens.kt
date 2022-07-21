package com.programmersbox.otakuworld

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.BottomSheetScaffold
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.rememberBottomSheetScaffoldState
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ExperimentalMotionApi
import androidx.constraintlayout.compose.MotionLayout
import androidx.constraintlayout.compose.MotionLayoutDebugFlags
import androidx.constraintlayout.compose.MotionScene
import com.google.accompanist.insets.ProvideWindowInsets
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import com.programmersbox.uiviews.utils.*
import com.programmersbox.uiviews.utils.components.FullDynamicThemePrimaryColorsFromImage
import com.programmersbox.uiviews.utils.components.rememberDynamicColorState
import kotlinx.coroutines.channels.ticker
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlin.random.nextInt

class TestDialogFragment : BaseBottomSheetDialogFragment() {

    @OptIn(
        ExperimentalMaterial3Api::class,
        ExperimentalMaterialApi::class,
        ExperimentalComposeUiApi::class,
        ExperimentalPagerApi::class,
        ExperimentalFoundationApi::class,
        ExperimentalMotionApi::class
    )
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner))
        setContent { ProvideWindowInsets { MaterialTheme(currentScheme) { TestView { dismiss() } } } }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
    }

    /*override fun setupDialog(dialog: Dialog, style: Int) {
        dialog.window?.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
    }*/
}

enum class SettingLocation {
    CHECK, SWITCH, PAGER, OPTIMISTIC, MOTIONLAYOUT, THEME
}

@ExperimentalMotionApi
@ExperimentalPagerApi
@ExperimentalComposeUiApi
@ExperimentalMaterial3Api
@ExperimentalFoundationApi
@ExperimentalMaterialApi
@Composable
fun TestView(closeClick: () -> Unit) {
    val topBarState = rememberTopAppBarScrollState()
    val scrollBehavior = remember { TopAppBarDefaults.pinnedScrollBehavior(topBarState) }
    val state = rememberBottomSheetScaffoldState()
    val scope = rememberCoroutineScope()
    var location: SettingLocation? by remember { mutableStateOf(null) }

    LaunchedEffect(state) {
        snapshotFlow { state.bottomSheetState.isCollapsed }
            .distinctUntilChanged()
            .filter { it }
            .collect { location = null }
    }

    BackHandler(state.bottomSheetState.isExpanded) { scope.launch { state.bottomSheetState.collapse() } }

    BottomSheetScaffold(
        scaffoldState = state,
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = { Text("Fun") },
                    actions = { IconButton(onClick = { closeClick() }) { Icon(imageVector = Icons.Default.Close, contentDescription = null) } },
                    scrollBehavior = scrollBehavior
                )
                androidx.compose.material3.Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
            }
        },
        sheetPeekHeight = 0.dp,
        sheetContent = {
            CompositionLocalProvider(
                androidx.compose.material3.LocalContentColor provides contentColorFor(MaterialTheme.colorScheme.surface)
            ) {
                when (location) {
                    SettingLocation.CHECK -> CheckView { scope.launch { state.bottomSheetState.collapse() } }
                    SettingLocation.SWITCH -> SwitchView { scope.launch { state.bottomSheetState.collapse() } }
                    SettingLocation.PAGER -> PagerView { scope.launch { state.bottomSheetState.collapse() } }
                    SettingLocation.OPTIMISTIC -> OptimisticView { scope.launch { state.bottomSheetState.collapse() } }
                    SettingLocation.MOTIONLAYOUT -> MotionLayoutView { scope.launch { state.bottomSheetState.collapse() } }
                    SettingLocation.THEME -> ThemeingView { scope.launch { state.bottomSheetState.collapse() } }
                    else -> {}
                }
            }
        },
        backgroundColor = MaterialTheme.colorScheme.background,
        contentColor = contentColorFor(MaterialTheme.colorScheme.background),
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
    ) { p ->
        CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides contentColorFor(MaterialTheme.colorScheme.background)
        ) {
            LazyColumn(contentPadding = p) {
                item {
                    PreferenceSetting(
                        settingTitle = { Text("Switch Settings") },
                        settingIcon = { Icon(Icons.Default.SwapHoriz, null) },
                        modifier = Modifier.clickable {
                            location = SettingLocation.SWITCH
                            scope.launch { state.bottomSheetState.expand() }
                        }
                    ) { Icon(Icons.Default.ChevronRight, null) }
                }

                item {
                    PreferenceSetting(
                        settingTitle = { Text("Check Settings") },
                        settingIcon = { Icon(Icons.Default.Check, null) },
                        modifier = Modifier.clickable {
                            location = SettingLocation.CHECK
                            scope.launch { state.bottomSheetState.expand() }
                        }
                    ) { Icon(Icons.Default.ChevronRight, null) }
                }

                item {
                    PreferenceSetting(
                        settingTitle = { Text("Pager Settings") },
                        settingIcon = { Icon(Icons.Default.ViewArray, null) },
                        modifier = Modifier.clickable {
                            location = SettingLocation.PAGER
                            scope.launch { state.bottomSheetState.expand() }
                        }
                    ) { Icon(Icons.Default.ChevronRight, null) }
                }

                item {
                    PreferenceSetting(
                        settingTitle = { Text("Optimistic Settings") },
                        settingIcon = { Icon(Icons.Default.Update, null) },
                        modifier = Modifier.clickable {
                            location = SettingLocation.OPTIMISTIC
                            scope.launch { state.bottomSheetState.expand() }
                        }
                    ) { Icon(Icons.Default.ChevronRight, null) }
                }

                item {
                    PreferenceSetting(
                        settingTitle = { Text("MotionLayout Settings") },
                        settingIcon = { Icon(Icons.Default.MotionPhotosAuto, null) },
                        modifier = Modifier.clickable {
                            location = SettingLocation.MOTIONLAYOUT
                            scope.launch { state.bottomSheetState.expand() }
                        }
                    ) { Icon(Icons.Default.ChevronRight, null) }
                }

                item {
                    PreferenceSetting(
                        settingTitle = { Text("Theme Settings") },
                        settingIcon = { Icon(Icons.Default.SettingsBrightness, null) },
                        modifier = Modifier.clickable {
                            location = SettingLocation.THEME
                            scope.launch { state.bottomSheetState.expand() }
                        }
                    ) { Icon(Icons.Default.ChevronRight, null) }

                }
            }
        }
    }
}

@ExperimentalComposeUiApi
@ExperimentalMaterial3Api
@ExperimentalFoundationApi
@ExperimentalMaterialApi
@Composable
fun SwitchView(closeClick: () -> Unit) {
    val topBarState = rememberTopAppBarScrollState()
    val scrollBehavior = remember { TopAppBarDefaults.pinnedScrollBehavior(topBarState) }
    Scaffold(
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = { Text("Switch") },
                    actions = {
                        IconButton(onClick = { closeClick() }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = null)
                        }
                    },
                    scrollBehavior = scrollBehavior
                )
                androidx.compose.material3.Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
            }
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
    ) { p ->
        LazyColumn(contentPadding = p) {
            items(20) {
                val (value, updateValue) = remember { mutableStateOf(false) }

                SwitchSetting(
                    settingTitle = { Text("Change the Thing") },
                    value = value,
                    updateValue = updateValue
                )
            }
        }
    }
}

@ExperimentalComposeUiApi
@ExperimentalMaterial3Api
@ExperimentalFoundationApi
@ExperimentalMaterialApi
@Composable
fun CheckView(closeClick: () -> Unit) {
    val topBarState = rememberTopAppBarScrollState()
    val scrollBehavior = remember { TopAppBarDefaults.pinnedScrollBehavior(topBarState) }
    Scaffold(
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = { Text("Check") },
                    actions = {
                        IconButton(onClick = { closeClick() }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = null)
                        }
                    },
                    scrollBehavior = scrollBehavior
                )
                androidx.compose.material3.Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
            }
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
    ) { p ->
        LazyColumn(contentPadding = p) {
            items(20) {
                val (value, updateValue) = remember { mutableStateOf(false) }

                CheckBoxSetting(
                    settingTitle = { Text("Change the Thing") },
                    value = value,
                    updateValue = updateValue
                )
            }
        }
    }
}

@ExperimentalPagerApi
@ExperimentalComposeUiApi
@ExperimentalMaterial3Api
@ExperimentalFoundationApi
@ExperimentalMaterialApi
@Composable
fun PagerView(closeClick: () -> Unit) {
    val topBarState = rememberTopAppBarScrollState()
    val scrollBehavior = remember { TopAppBarDefaults.pinnedScrollBehavior(topBarState) }
    val scope = rememberCoroutineScope()
    val state = rememberPagerState()
    val currentPage by remember { derivedStateOf { state.currentPage } }
    val pageCount = 5
    //TODO: Look at this for a number picker setting
    // https://github.com/ChargeMap/Compose-NumberPicker/blob/master/lib/src/main/kotlin/com/chargemap/compose/numberpicker/NumberPicker.kt
    Scaffold(
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = { Text("Pager") },
                    actions = { IconButton(onClick = { closeClick() }) { Icon(imageVector = Icons.Default.Close, contentDescription = null) } },
                    scrollBehavior = scrollBehavior
                )
                androidx.compose.material3.Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
            }
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
    ) { p ->
        Column(modifier = Modifier.padding(p)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row {
                    Icon(Icons.Default.ChevronLeft, null)
                    Text("Day/Night")
                }

                Text("${currentPage + 1}/$pageCount")

                IconButton(
                    onClick = { scope.launch { state.scrollToPage(if (currentPage + 1 >= pageCount) 0 else currentPage + 1) } }
                ) { Icon(Icons.Default.ChevronRight, null) }
            }
            HorizontalPager(
                state = state,
                count = pageCount,
                verticalAlignment = Alignment.Top
            ) { page ->

                var popUpOrDialog by remember { mutableStateOf(false) }

                LazyColumn(modifier = Modifier.fillMaxHeight()) {
                    when (page) {
                        0 -> {
                            items(2) {
                                val (value, updateValue) = remember { mutableStateOf(false) }

                                CheckBoxSetting(
                                    settingTitle = { Text("Change the Thing") },
                                    value = value,
                                    updateValue = updateValue
                                )
                            }
                        }
                        1 -> {
                            items(4) {
                                val (value, updateValue) = remember { mutableStateOf(false) }

                                SwitchSetting(
                                    settingTitle = { Text("Switch the Thing") },
                                    value = value,
                                    updateValue = updateValue
                                )
                            }
                        }
                        2 -> {
                            item {
                                SwitchSetting(
                                    settingTitle = { Text("Dropdown or Dialog") },
                                    summaryValue = { Text(if (popUpOrDialog) "Dropdown" else "Dialog") },
                                    value = popUpOrDialog,
                                    updateValue = { popUpOrDialog = it }
                                )
                            }

                            item {
                                var timeout by remember { mutableStateOf(false) }
                                SwitchSetting(
                                    settingTitle = { Text("Timeout") },
                                    value = timeout,
                                    updateValue = { timeout = it }
                                )
                            }
                            item {
                                var dropDownSetting by remember { mutableStateOf(SettingLocation.CHECK) }
                                var showPopup by remember { mutableStateOf(false) }

                                if (showPopup && !popUpOrDialog) {
                                    var currentSelection by remember { mutableStateOf(dropDownSetting) }

                                    AlertDialog(
                                        shape = RoundedCornerShape(5.dp),
                                        onDismissRequest = { showPopup = false },
                                        title = { Text("Choose a Favorite") },
                                        text = {
                                            LazyColumn {
                                                items(SettingLocation.values()) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clickable(
                                                                indication = rememberRipple(),
                                                                interactionSource = remember { MutableInteractionSource() }
                                                            ) { currentSelection = it }
                                                            .border(0.dp, Color.Transparent, RoundedCornerShape(20.dp))
                                                    ) {
                                                        androidx.compose.material3.RadioButton(
                                                            selected = it == currentSelection,
                                                            onClick = { currentSelection = it },
                                                            modifier = Modifier.padding(8.dp)
                                                        )
                                                        Text(
                                                            it.toString(),
                                                            style = MaterialTheme.typography.bodyLarge
                                                        )
                                                    }
                                                }
                                            }
                                        },
                                        confirmButton = {
                                            TextButton(
                                                onClick = {
                                                    showPopup = false
                                                    dropDownSetting = currentSelection
                                                }
                                            ) { Text("OK") }
                                        },
                                        dismissButton = { TextButton(onClick = { showPopup = false }) { Text("Cancel") } }
                                    )
                                }

                                val optionsList = remember { mutableStateOf(SettingLocation.values()) }

                                PreferenceSetting(
                                    settingTitle = { Text("Favorite Setting") },
                                    modifier = Modifier.clickable { showPopup = true },
                                    endIcon = {
                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier
                                                .border(1.dp, MaterialTheme.colorScheme.outline)
                                                .padding(4.dp)
                                        ) {
                                            Text(dropDownSetting.toString())
                                            Icon(
                                                if (popUpOrDialog) Icons.Default.ArrowDropDown else Icons.Default.ArrowRight,
                                                null
                                            )
                                        }

                                        androidx.compose.material3.DropdownMenu(
                                            expanded = showPopup && popUpOrDialog,
                                            onDismissRequest = { showPopup = false },
                                            modifier = Modifier
                                                .background(MaterialTheme.colorScheme.surface)
                                                .border(1.dp, MaterialTheme.colorScheme.outline)
                                        ) {
                                            optionsList.value.forEachIndexed { index, settingLocation ->
                                                androidx.compose.material3.DropdownMenuItem(
                                                    onClick = {
                                                        dropDownSetting = settingLocation
                                                        showPopup = false
                                                    },
                                                    text = { androidx.compose.material3.Text(settingLocation.toString()) }
                                                )
                                                if (index < optionsList.value.size - 1)
                                                    androidx.compose.material3.Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
                                            }
                                        }
                                    }
                                )
                            }
                            item {
                                var text by remember { mutableStateOf("") }
                                TextFieldSetting(value = text, onValueChange = { text = it })
                            }
                        }
                        3 -> {
                            items(5) {
                                PreferenceSetting(settingTitle = { Text("A Thing") })
                            }
                        }
                        4 -> {
                            items(20) {
                                PreferenceSetting(settingTitle = { Text("A Thing") })
                            }

                            items(3) {
                                PreferenceSetting(
                                    settingTitle = { Text("Text!") },
                                    endIcon = { Text("More Text!") }
                                )
                            }

                            items(3) {

                                var showInfo by remember { mutableStateOf(false) }

                                LaunchedEffect(key1 = showInfo) {
                                    delay(10000)
                                    showInfo = false
                                }

                                if (showInfo) {
                                    AlertDialog(
                                        onDismissRequest = { showInfo = false },
                                        title = { Text("Title") },
                                        confirmButton = { TextButton(onClick = { showInfo = false }) { Text("OK") } }
                                    )
                                }

                                PreferenceSetting(
                                    settingTitle = { Text("Text $it!") },
                                    endIcon = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {

                                            Icon(
                                                imageVector = Icons.Default.Info,
                                                contentDescription = null,
                                                tint = Color.Cyan,
                                                modifier = Modifier
                                                    .clickable { showInfo = true }
                                                    .minimumTouchTargetSizeCustom()
                                            )

                                            Text("More Text!")
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
fun Modifier.minimumTouchTargetSizeCustom(): Modifier = composed {
    if (androidx.compose.material3.LocalMinimumTouchTargetEnforcement.current) {
        // TODO: consider using a hardcoded value of 48.dp instead to avoid inconsistent UI if the
        // LocalViewConfiguration changes across devices / during runtime.
        val size = LocalViewConfiguration.current.minimumTouchTargetSize
        MinimumTouchTargetModifierCustom(size)
    } else {
        Modifier
    }
}

class MinimumTouchTargetModifierCustom(val size: DpSize) : LayoutModifier {
    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {

        val placeable = measurable.measure(constraints)

        // Be at least as big as the minimum dimension in both dimensions
        val width = maxOf(placeable.width, size.width.roundToPx())
        val height = maxOf(placeable.height, size.height.roundToPx())

        return layout(width, height) {
            val centerX = ((width - placeable.width) / 2f).roundToInt()
            val centerY = ((height - placeable.height) / 2f).roundToInt()
            placeable.place(centerX, centerY)
        }
    }

    override fun equals(other: Any?): Boolean {
        val otherModifier = other as? MinimumTouchTargetModifierCustom ?: return false
        return size == otherModifier.size
    }

    override fun hashCode(): Int {
        return size.hashCode()
    }
}

@ExperimentalMaterialApi
@ExperimentalComposeUiApi
@Composable
fun TextFieldSetting(value: String, onValueChange: (String) -> Unit) {
    val keyboardController = LocalSoftwareKeyboardController.current
    /*PreferenceSetting(
        settingTitle = { Text("Text Field") },
        endIcon = {
            TextField(
                value = value,
                onValueChange = onValueChange,
                trailingIcon = { IconButton(onClick = { onValueChange("") }) { Icon(Icons.Default.Cancel, null) } },
                colors = TextFieldDefaults.textFieldColors(),
                modifier = Modifier.onFocusChanged { if (!it.isFocused) keyboardController?.hide() }
            )
        }
    )*/
}

@ExperimentalComposeUiApi
@ExperimentalMaterial3Api
@ExperimentalFoundationApi
@ExperimentalMaterialApi
@Composable
fun OptimisticView(closeClick: () -> Unit) {
    val topBarState = rememberTopAppBarScrollState()
    val scrollBehavior = remember { TopAppBarDefaults.pinnedScrollBehavior(topBarState) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = { Text("Optimistic Views") },
                    actions = { IconButton(onClick = { closeClick() }) { Icon(imageVector = Icons.Default.Close, contentDescription = null) } },
                    scrollBehavior = scrollBehavior
                )
                androidx.compose.material3.Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
            }
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
    ) { p ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(p),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            item {
                var count = remember { 0 }

                //THIS WORKS! This allows both optimistic updates and real time updates.
                //To get this working for real, it would be in a view model variable that takes in a flow
                val ticker = remember {
                    ticker(5000).receiveAsFlow()
                        .map { Random.nextInt(0..10) }
                        .shareIn(scope, SharingStarted.Eagerly, 1)
                }

                val viewModel = remember {
                    FlowStateViewModel(ticker, 0)
                }

                val viewModelCount by viewModel.collectAsState(initial = 0)

                androidx.compose.material3.Button(
                    onClick = { scope.launch { viewModel.emit(++count) } }
                ) { Text("FlowState: $viewModelCount") }

                val viewModel2 = remember {
                    FlowSharedViewModel(ticker)
                }

                val viewModelCount2 by viewModel2.collectAsState(initial = 0)

                androidx.compose.material3.Button(
                    onClick = { scope.launch { viewModel2.emit(++count) } }
                ) { Text("FlowShared: $viewModelCount2") }

                val count2 = remember { MutableStateFlow(0) }

                val timer by merge(ticker, count2).collectAsState(initial = 0)

                androidx.compose.material3.Button(
                    onClick = { scope.launch { count2.emit(++count) } }
                ) { Text("Hi: $timer") }

                androidx.compose.material3.Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
            }

            item {

                val lower = remember { FlowSharedViewModel(MutableSharedFlow<Float>()) }
                val lowerFlow by lower.collectAsState(50f)

                val upper = remember { FlowSharedViewModel(MutableSharedFlow<Float>()) }
                val upperFlow by upper.collectAsState(75f)

                val difference = 10f

                SliderSetting(
                    sliderValue = lowerFlow,
                    settingTitle = { Text("Lower") },
                    range = 0f..75f,
                    updateValue = {
                        scope.launch {
                            lower.emit(it)
                            if (upperFlow - difference < it) {
                                upper.emit(it + difference)
                            }
                        }
                    },
                    onValueChangedFinished = {
                        //TODO: This is where things would be sent
                    }
                )

                SliderSetting(
                    sliderValue = upperFlow,
                    settingTitle = { Text("Upper") },
                    settingSummary = { Text("Must be $difference above Lower") },
                    format = { "${it.toInt()}%" },
                    range = 50f..100f,
                    updateValue = {
                        scope.launch {
                            upper.emit(it)
                            if (it < lowerFlow + difference) {
                                lower.emit(it - difference)
                            }
                        }
                    },
                    onValueChangedFinished = {
                        //TODO: This is where things would be sent
                    }
                )

            }
        }
    }
}

@ExperimentalMaterial3Api
@ExperimentalMotionApi
@Composable
fun MotionLayoutView(closeClick: () -> Unit) {
    val topBarState = rememberTopAppBarScrollState()
    val scrollBehavior = remember { TopAppBarDefaults.pinnedScrollBehavior(topBarState) }
    Scaffold(
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = { Text("MotionLayout Views") },
                    actions = { IconButton(onClick = { closeClick() }) { Icon(imageVector = Icons.Default.Close, contentDescription = null) } },
                    scrollBehavior = scrollBehavior
                )
                androidx.compose.material3.Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
            }
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
    ) { p ->
        var animateToEnd by remember { mutableStateOf(false) }

        val progress by animateFloatAsState(
            targetValue = if (animateToEnd) 1f else 0f,
            animationSpec = tween(1000)
        )
        Column(modifier = Modifier.padding(p)) {
            MotionLayout(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
                    .background(Color.White),
                motionScene = MotionScene(
                    """{
                    ConstraintSets: {   // all ConstraintSets
                      start: {          // constraint set id = "start"
                        a: {            // Constraint for widget id='a'
                          width: 40,
                          height: 40,
                          start: ['parent', 'start', 16],
                          bottom: ['parent', 'bottom', 16]
                        }
                      },
                      end: {         // constraint set id = "start"
                        a: {
                          width: 40,
                          height: 40,
                          //rotationZ: 390,
                          end: ['parent', 'end', 16],
                          top: ['parent', 'top', 16]
                        }
                      }
                    },
                    Transitions: {            // All Transitions in here 
                      default: {              // transition named 'default'
                        from: 'start',        // go from Transition "start"
                        to: 'end',            // go to Transition "end"
                        pathMotionArc: 'startHorizontal',  // move in arc 
                        KeyFrames: {          // All keyframes go here
                          KeyAttributes: [    // keyAttributes key frames go here
                            {
                              target: ['a'],              // This keyAttributes group target id "a"
                              frames: [25,50,75],         // 3 points on progress 25% , 50% and 75%
                              rotationX: [0, 45, 60],     // the rotationX angles are a spline from 0,0,45,60,0
                              rotationY: [60, 45, 0]     // the rotationX angles are a spline from 0,60,45,0,0
                            }
                          ]
                        }
                      }
                    }
                }"""
                ),
                debug = EnumSet.of(MotionLayoutDebugFlags.SHOW_ALL),
                progress = progress
            ) {
                Box(
                    modifier = Modifier
                        .layoutId("a")
                        .background(Color.Red)
                )
            }

            androidx.compose.material3.Button(onClick = { animateToEnd = !animateToEnd }) {
                androidx.compose.material3.Text(text = "Run")
            }
        }
    }
}

@ExperimentalMaterial3Api
@ExperimentalFoundationApi
@Composable
fun ThemeingView(closeClick: () -> Unit) {
    val topBarState = rememberTopAppBarScrollState()
    val scrollBehavior = remember { TopAppBarDefaults.pinnedScrollBehavior(topBarState) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = { Text("Theme Views") },
                    actions = { IconButton(onClick = { closeClick() }) { Icon(imageVector = Icons.Default.Close, contentDescription = null) } },
                    scrollBehavior = scrollBehavior
                )
                androidx.compose.material3.Divider()
            }
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
    ) { p ->

        val current = currentColorScheme

        val dominantColor = rememberDynamicColorState()
        val url: String? = remember {
            "https://upload.wikimedia.org/wikipedia/en/c/c3/OnePunchMan_manga_cover.png"
        }
        LaunchedEffect(url) {
            if (url != null) {
                dominantColor.updateColorsFromImageUrl(url)
            } else {
                dominantColor.reset()
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(p),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            stickyHeader {
                Row {
                    Text("Option", modifier = Modifier.weight(1f))
                    Text("M3", modifier = Modifier.weight(1f))
                    Text("Dynamic", modifier = Modifier.weight(1f))
                }
            }

            fun LazyListScope.row(type: String, choice: ColorScheme.() -> Color) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(type, modifier = Modifier.weight(1f))

                        MaterialTheme(current) {
                            Box(
                                modifier = Modifier
                                    .height(15.dp)
                                    .weight(1f)
                                    .background(MaterialTheme.colorScheme.choice())
                            )
                        }

                        FullDynamicThemePrimaryColorsFromImage(dominantColor) {
                            Box(
                                modifier = Modifier
                                    .height(15.dp)
                                    .weight(1f)
                                    .background(MaterialTheme.colorScheme.choice())
                            )
                        }
                    }
                }
            }

            row("Primary") { primary }
            row("onPrimary") { onPrimary }
            row("surface") { surface }
            row("onSurface") { onSurface }
            row("Background") { background }
            row("onBackground") { onBackground }

        }
    }
}