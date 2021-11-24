package com.programmersbox.otakuworld

import android.app.Dialog
import android.os.Bundle
import android.view.*
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
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import com.google.accompanist.insets.ProvideWindowInsets
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import com.programmersbox.uiviews.utils.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

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

    override fun setupDialog(dialog: Dialog, style: Int) {
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
    }
}

enum class SettingLocation {
    CHECK, SWITCH, PAGER
}

@ExperimentalPagerApi
@ExperimentalComposeUiApi
@ExperimentalMaterial3Api
@ExperimentalFoundationApi
@ExperimentalMaterialApi
@Composable
fun TestView(closeClick: () -> Unit) {
    val scrollBehavior = remember { TopAppBarDefaults.enterAlwaysScrollBehavior() }
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
            when (location) {
                SettingLocation.CHECK -> CheckView { scope.launch { state.bottomSheetState.collapse() } }
                SettingLocation.SWITCH -> SwitchView { scope.launch { state.bottomSheetState.collapse() } }
                SettingLocation.PAGER -> PagerView { scope.launch { state.bottomSheetState.collapse() } }
                else -> {}
            }
        },
        backgroundColor = MaterialTheme.colorScheme.background,
        contentColor = contentColorFor(MaterialTheme.colorScheme.background)
    ) { p ->
        CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides contentColorFor(MaterialTheme.colorScheme.background)
        ) {
            LazyColumn(contentPadding = p) {
                item {
                    PreferenceSetting(
                        settingTitle = "Switch Settings",
                        onClick = {
                            location = SettingLocation.SWITCH
                            scope.launch { state.bottomSheetState.expand() }
                        }
                    ) { androidx.compose.material3.Icon(Icons.Default.ChevronRight, null) }
                }

                item {
                    PreferenceSetting(
                        settingTitle = "Check Settings",
                        onClick = {
                            location = SettingLocation.CHECK
                            scope.launch { state.bottomSheetState.expand() }
                        }
                    ) { androidx.compose.material3.Icon(Icons.Default.ChevronRight, null) }
                }

                item {
                    PreferenceSetting(
                        settingTitle = "Pager Settings",
                        onClick = {
                            location = SettingLocation.PAGER
                            scope.launch { state.bottomSheetState.expand() }
                        }
                    ) { androidx.compose.material3.Icon(Icons.Default.ChevronRight, null) }
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
    val scrollBehavior = remember { TopAppBarDefaults.enterAlwaysScrollBehavior() }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Fun") },
                actions = {
                    IconButton(onClick = { closeClick() }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = null)
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
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
    val scrollBehavior = remember { TopAppBarDefaults.enterAlwaysScrollBehavior() }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Fun") },
                actions = {
                    IconButton(onClick = { closeClick() }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = null)
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
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
    val scrollBehavior = remember { TopAppBarDefaults.enterAlwaysScrollBehavior() }
    val scope = rememberCoroutineScope()
    val state = rememberPagerState()
    val currentPage by remember { derivedStateOf { state.currentPage } }
    val pageCount = 4

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Fun") },
                actions = {
                    IconButton(onClick = { closeClick() }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = null)
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
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