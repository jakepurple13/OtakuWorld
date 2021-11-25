package com.programmersbox.otakuworld

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import com.google.accompanist.insets.ProvideWindowInsets
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import com.programmersbox.uiviews.utils.*
import kotlinx.coroutines.channels.ticker
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.random.Random
import kotlin.random.nextInt

class TestDialogFragment : BaseBottomSheetDialogFragment() {

    @ExperimentalPagerApi
    @ExperimentalComposeUiApi
    @ExperimentalMaterial3Api
    @ExperimentalFoundationApi
    @ExperimentalMaterialApi
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
    CHECK, SWITCH, PAGER, OPTIMISTIC
}

@ExperimentalPagerApi
@ExperimentalComposeUiApi
@ExperimentalMaterial3Api
@ExperimentalFoundationApi
@ExperimentalMaterialApi
@Composable
fun TestView(closeClick: () -> Unit) {
    val scrollBehavior = remember { TopAppBarDefaults.pinnedScrollBehavior() }
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
                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
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
                        settingTitle = "Switch Settings",
                        settingIcon = { Icon(Icons.Default.SwapHoriz, null) },
                        onClick = {
                            location = SettingLocation.SWITCH
                            scope.launch { state.bottomSheetState.expand() }
                        }
                    ) { Icon(Icons.Default.ChevronRight, null) }
                }

                item {
                    PreferenceSetting(
                        settingTitle = "Check Settings",
                        settingIcon = { Icon(Icons.Default.Check, null) },
                        onClick = {
                            location = SettingLocation.CHECK
                            scope.launch { state.bottomSheetState.expand() }
                        }
                    ) { Icon(Icons.Default.ChevronRight, null) }
                }

                item {
                    PreferenceSetting(
                        settingTitle = "Pager Settings",
                        settingIcon = { Icon(Icons.Default.ViewArray, null) },
                        onClick = {
                            location = SettingLocation.PAGER
                            scope.launch { state.bottomSheetState.expand() }
                        }
                    ) { Icon(Icons.Default.ChevronRight, null) }
                }

                item {
                    PreferenceSetting(
                        settingTitle = "Optimistic Settings",
                        settingIcon = { Icon(Icons.Default.Update, null) },
                        onClick = {
                            location = SettingLocation.OPTIMISTIC
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
    val scrollBehavior = remember { TopAppBarDefaults.pinnedScrollBehavior() }

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
                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
            }
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
    ) { p ->
        LazyColumn(contentPadding = p) {
            items(20) {
                val (value, updateValue) = remember { mutableStateOf(false) }

                SwitchSetting(
                    settingTitle = "Change the Thing",
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
    val scrollBehavior = remember { TopAppBarDefaults.pinnedScrollBehavior() }

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
                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
            }
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
    ) { p ->
        LazyColumn(contentPadding = p) {
            items(20) {
                val (value, updateValue) = remember { mutableStateOf(false) }

                CheckBoxSetting(
                    settingTitle = "Change the Thing",
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
    val scrollBehavior = remember { TopAppBarDefaults.pinnedScrollBehavior() }
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
                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
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
                                    settingTitle = "Change the Thing",
                                    value = value,
                                    updateValue = updateValue
                                )
                            }
                        }
                        1 -> {
                            items(4) {
                                val (value, updateValue) = remember { mutableStateOf(false) }

                                SwitchSetting(
                                    settingTitle = "Switch the Thing",
                                    value = value,
                                    updateValue = updateValue
                                )
                            }
                        }
                        2 -> {
                            item {
                                SwitchSetting(
                                    settingTitle = "Dropdown or Dialog",
                                    summaryValue = if (popUpOrDialog) "Dropdown" else "Dialog",
                                    value = popUpOrDialog,
                                    updateValue = { popUpOrDialog = it }
                                )
                            }

                            item {
                                var timeout by remember { mutableStateOf(false) }
                                SwitchSetting(
                                    settingTitle = "Timeout",
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
                                                        RadioButton(
                                                            selected = it == currentSelection,
                                                            onClick = { currentSelection = it },
                                                            modifier = Modifier.padding(8.dp),
                                                            colors = defaultRadioButtonColors()
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
                                    settingTitle = "Favorite Setting",
                                    onClick = { showPopup = true },
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

                                        DropdownMenu(
                                            expanded = showPopup && popUpOrDialog,
                                            onDismissRequest = { showPopup = false },
                                            modifier = Modifier
                                                .background(MaterialTheme.colorScheme.surface)
                                                .border(1.dp, MaterialTheme.colorScheme.outline)
                                        ) {
                                            optionsList.value.forEachIndexed { index, settingLocation ->
                                                DropdownMenuItem(
                                                    onClick = {
                                                        dropDownSetting = settingLocation
                                                        showPopup = false
                                                    },
                                                ) { Text(settingLocation.toString()) }
                                                if (index < optionsList.value.size - 1)
                                                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
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
                                PreferenceSetting(settingTitle = "A Thing")
                            }
                        }
                        4 -> {
                            items(20) {
                                PreferenceSetting(settingTitle = "A Thing2")
                            }
                        }
                    }
                }
            }
        }
    }
}

@ExperimentalMaterialApi
@ExperimentalComposeUiApi
@Composable
fun TextFieldSetting(value: String, onValueChange: (String) -> Unit) {
    val keyboardController = LocalSoftwareKeyboardController.current
    PreferenceSetting(
        settingTitle = "Text Field",
        endIcon = {
            TextField(
                value = value,
                onValueChange = onValueChange,
                trailingIcon = { IconButton(onClick = { onValueChange("") }) { Icon(Icons.Default.Cancel, null) } },
                colors = TextFieldDefaults.textFieldColors(),
                modifier = Modifier.onFocusChanged { if (!it.isFocused) keyboardController?.hide() }
            )
        }
    )
}

@ExperimentalComposeUiApi
@ExperimentalMaterial3Api
@ExperimentalFoundationApi
@ExperimentalMaterialApi
@Composable
fun OptimisticView(closeClick: () -> Unit) {
    val scrollBehavior = remember { TopAppBarDefaults.pinnedScrollBehavior() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = { Text("Optimistic Views") },
                    actions = { IconButton(onClick = { closeClick() }) { Icon(imageVector = Icons.Default.Close, contentDescription = null) } },
                    scrollBehavior = scrollBehavior
                )
                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
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

                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
            }

            item {

                val lower = remember { FlowSharedViewModel(MutableSharedFlow<Float>()) }
                val lowerFlow by lower.collectAsState(50f)

                val upper = remember { FlowSharedViewModel(MutableSharedFlow<Float>()) }
                val upperFlow by upper.collectAsState(75f)

                val difference = 10f

                SliderSetting(
                    sliderValue = lowerFlow,
                    settingTitle = "Lower",
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
                    settingTitle = "Upper",
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