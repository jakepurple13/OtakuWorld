package com.programmersbox.uiviews

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.navigation.fragment.findNavController
import com.programmersbox.uiviews.utils.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.android.inject
import androidx.compose.material3.MaterialTheme as M3MaterialTheme

class DebugFragment : BaseBottomSheetDialogFragment() {

    private val genericInfo: GenericInfo by inject()

    @ExperimentalMaterial3Api
    @ExperimentalFoundationApi
    @ExperimentalMaterialApi
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner))
        setContent { M3MaterialTheme(currentColorScheme) { DebugView() } }
    }

    @ExperimentalMaterial3Api
    @Composable
    private fun DebugView() {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val scrollBehavior = remember { TopAppBarDefaults.enterAlwaysScrollBehavior() }

        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                MediumTopAppBar(
                    title = { Text("Debug Menu") },
                    navigationIcon = {
                        IconButton(onClick = { findNavController().popBackStack() }) { Icon(Icons.Default.ArrowBack, null) }
                    },
                    scrollBehavior = scrollBehavior
                )
            }
        ) { p ->
            val moreSettings = remember { genericInfo.debugMenuItem(context) }
            LazyColumn(contentPadding = p) {

                item {
                    var batteryPercent by remember {
                        mutableStateOf(runBlocking { context.dataStore.data.first()[BATTERY_PERCENT] ?: 20 }.toFloat())
                    }
                    SliderSetting(
                        sliderValue = batteryPercent,
                        settingIcon = Icons.Default.BatteryAlert,
                        settingTitle = R.string.battery_alert_percentage,
                        settingSummary = R.string.battery_default,
                        range = 1f..100f,
                        updateValue = {
                            batteryPercent = it
                            scope.launch { context.updatePref(BATTERY_PERCENT, it.toInt()) }
                        }
                    )
                    Divider(color = M3MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
                }

                itemsIndexed(moreSettings) { index, build ->
                    build()
                    if (index < moreSettings.size - 1) Divider(color = M3MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
                }
            }
        }
    }
}