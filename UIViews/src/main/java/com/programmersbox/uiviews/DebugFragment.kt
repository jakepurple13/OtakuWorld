package com.programmersbox.uiviews

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallTopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
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
        val scrollBehavior = remember { TopAppBarDefaults.pinnedScrollBehavior() }

        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                SmallTopAppBar(
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
                    Divider()
                }

                itemsIndexed(moreSettings) { index, build ->
                    build()
                    if (index < moreSettings.size - 1) Divider()
                }
            }
        }
    }

    @Composable
    private fun SliderSetting(
        modifier: Modifier = Modifier,
        sliderValue: Float,
        settingIcon: ImageVector,
        @StringRes settingTitle: Int,
        @StringRes settingSummary: Int,
        range: ClosedFloatingPointRange<Float>,
        steps: Int = 0,
        updateValue: (Float) -> Unit
    ) {
        ConstraintLayout(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth()
                .then(modifier)
        ) {
            val (
                icon,
                title,
                summary,
                slider,
                value
            ) = createRefs()

            Icon(
                settingIcon,
                null,
                modifier = Modifier
                    .constrainAs(icon) {
                        start.linkTo(parent.start)
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                    }
                    .padding(8.dp)
            )

            Text(
                stringResource(settingTitle),
                style = M3MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Start,
                modifier = Modifier.constrainAs(title) {
                    top.linkTo(parent.top)
                    end.linkTo(parent.end)
                    start.linkTo(icon.end, margin = 10.dp)
                    width = Dimension.fillToConstraints
                }
            )

            Text(
                stringResource(settingSummary),
                style = M3MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Start,
                modifier = Modifier.constrainAs(summary) {
                    top.linkTo(title.bottom)
                    end.linkTo(parent.end)
                    start.linkTo(icon.end, margin = 10.dp)
                    width = Dimension.fillToConstraints
                }
            )

            Slider(
                value = sliderValue,
                onValueChange = updateValue,
                valueRange = range,
                steps = steps,
                colors = SliderDefaults.colors(
                    thumbColor = M3MaterialTheme.colorScheme.primary,
                    disabledThumbColor = M3MaterialTheme.colorScheme.onSurface.copy(alpha = ContentAlpha.disabled)
                        .compositeOver(M3MaterialTheme.colorScheme.surface),
                    activeTrackColor = M3MaterialTheme.colorScheme.primary,
                    disabledActiveTrackColor = M3MaterialTheme.colorScheme.onSurface.copy(alpha = SliderDefaults.DisabledActiveTrackAlpha),
                    activeTickColor = contentColorFor(M3MaterialTheme.colorScheme.primary)
                        .copy(alpha = SliderDefaults.TickAlpha)
                ),
                modifier = Modifier.constrainAs(slider) {
                    top.linkTo(summary.bottom)
                    end.linkTo(value.start)
                    start.linkTo(icon.end)
                    width = Dimension.fillToConstraints
                }
            )

            Text(
                sliderValue.toInt().toString(),
                style = M3MaterialTheme.typography.titleMedium,
                modifier = Modifier.constrainAs(value) {
                    end.linkTo(parent.end)
                    start.linkTo(slider.end)
                    centerVerticallyTo(slider)
                }
            )

        }
    }
}