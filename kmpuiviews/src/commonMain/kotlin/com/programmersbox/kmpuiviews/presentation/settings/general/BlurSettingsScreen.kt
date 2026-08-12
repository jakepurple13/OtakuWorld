package com.programmersbox.kmpuiviews.presentation.settings.general

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.BlurCircular
import androidx.compose.material.icons.filled.BlurLinear
import androidx.compose.material.icons.filled.BlurOff
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Deblur
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.programmersbox.datastore.BlurKind
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.kmpuiviews.painterLogo
import com.programmersbox.kmpuiviews.presentation.components.BackButton
import com.programmersbox.kmpuiviews.presentation.components.DiagonalWipeIcon
import com.programmersbox.kmpuiviews.presentation.components.DiagonalWipeIconDefaults
import com.programmersbox.kmpuiviews.presentation.components.WipeDirection
import com.programmersbox.kmpuiviews.presentation.components.blurkind.HazeOptionsInfo
import com.programmersbox.kmpuiviews.presentation.components.blurkind.hazeGlassOptions
import com.programmersbox.kmpuiviews.presentation.components.blurkind.rememberBlurKindState
import com.programmersbox.kmpuiviews.presentation.components.blurkind.setBlurKind
import com.programmersbox.kmpuiviews.presentation.components.blurkind.setBlurKindSource
import dev.chrisbanes.haze.HazeProgressive
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.cancel
import otakuworld.kmpuiviews.generated.resources.confirm
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlurSettingsScreen() {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("Blur Settings") },
                navigationIcon = { BackButton() },
                scrollBehavior = scrollBehavior
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
    ) { padding ->
        BlurSettings(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun BlurSettings(
    modifier: Modifier = Modifier,
    dataStore: NewSettingsHandling = koinInject(),
) {
    val colors =
        ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer)

    var showBlur by dataStore.rememberShowBlur()

    Column(
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
        modifier = modifier.verticalScroll(rememberScrollState())
    ) {
        SegmentedListItem(
            leadingContent = {
                DiagonalWipeIcon(
                    isWiped = showBlur,
                    wipedIcon = Icons.Default.BlurOn,
                    baseIcon = Icons.Default.BlurOff,
                    motion = DiagonalWipeIconDefaults.expressive(
                        WipeDirection.BottomRightToTopLeft
                    ),
                    modifier = Modifier.size(24.dp)
                )
            },
            content = { Text("Show Blur") },
            trailingContent = {
                Switch(
                    checked = showBlur,
                    onCheckedChange = null
                )
            },
            checked = showBlur,
            onCheckedChange = { showBlur = it },
            colors = colors,
            shapes = ListItemDefaults.segmentedShapes(
                0,
                if (showBlur) 3 else 1
            )
        )

        AnimatedVisibility(showBlur) {
            var blurKind by dataStore.rememberBlurKind()
            var showBlurKindDialog by remember { mutableStateOf(false) }

            if (showBlurKindDialog) {
                AlertDialog(
                    onDismissRequest = { showBlurKindDialog = false },
                    title = { Text("Choose Blur Kind") },
                    text = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            BlurKind.entries.forEach {
                                ListItem(
                                    content = { Text(it.name) },
                                    onClick = { blurKind = it },
                                    trailingContent = {
                                        RadioButton(
                                            selected = it == blurKind,
                                            onClick = null
                                        )
                                    },
                                    selected = it == blurKind,
                                    shapes = ListItemDefaults.shapes(
                                        shape = MaterialTheme.shapes.large,
                                    )
                                )
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = { showBlurKindDialog = false }
                        ) { Text(stringResource(Res.string.confirm)) }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showBlurKindDialog = false }
                        ) { Text(stringResource(Res.string.cancel)) }
                    }
                )
            }

            val appIcon = painterLogo()

            Column(
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)
            ) {
                SegmentedListItem(
                    leadingContent = { Icon(Icons.Default.Deblur, null) },
                    content = { Text("Blur Kind") },
                    supportingContent = { Text("Choose the kind of blur to use.\nCurrently selected ${blurKind.name}") },
                    trailingContent = { Icon(Icons.Default.ArrowDropDown, null) },
                    colors = colors,
                    onClick = { showBlurKindDialog = true },
                    shapes = ListItemDefaults.segmentedShapes(1, 4)
                )

                val blurKindState = rememberBlurKindState()

                AnimatedContent(
                    targetState = blurKind,
                    transitionSpec = {
                        slideInVertically() togetherWith slideOutVertically()
                    }
                ) { target ->
                    Column(
                        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)
                    ) {
                        when (target) {
                            BlurKind.Haze -> {
                                HazeOptions(
                                    dataStore = dataStore,
                                    colors = colors,
                                )
                            }

                            BlurKind.HazeGlass -> {
                                HazeGlassOptions(
                                    dataStore = dataStore,
                                    colors = colors,
                                )
                            }

                            BlurKind.LiquidGlass -> {
                                LiquidGlassOptions(
                                    dataStore = dataStore,
                                    colors = colors,
                                )
                            }
                        }

                        Box {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.setBlurKindSource(blurKindState)
                            ) {
                                repeat(6) {
                                    Image(
                                        painter = appIcon,
                                        contentDescription = null,
                                        contentScale = ContentScale.FillBounds,
                                        modifier = Modifier.size(80.dp)
                                    )
                                }
                            }

                            BottomAppBar(
                                containerColor = Color.Transparent,
                                modifier = Modifier.setBlurKind(blurKindState) {
                                    progressive(
                                        if (blurKindState.hazeState.useProgressive)
                                            HazeProgressive.verticalGradient(
                                                startIntensity = 0f,
                                                endIntensity = 1f,
                                                preferPerformance = true
                                            )
                                        else
                                            null
                                    )
                                }
                            ) {
                                Text(
                                    text = "Blur Kind",
                                    modifier = Modifier.align(Alignment.CenterVertically)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ColumnScope.HazeOptions(
    dataStore: NewSettingsHandling,
    colors: ListItemColors,
) {
    var showBlurOptions by remember { mutableStateOf(false) }
    var blurType by dataStore.rememberBlurType()
    var useProgressive by dataStore.rememberUseProgressive()

    SegmentedListItem(
        leadingContent = {
            DiagonalWipeIcon(
                isWiped = useProgressive,
                wipedIcon = Icons.Default.BlurCircular,
                baseIcon = Icons.Default.BlurLinear,
                motion = DiagonalWipeIconDefaults.expressive(),
                modifier = Modifier.size(24.dp)
            )
        },
        content = { Text("Use Progressive Blur") },
        supportingContent = { Text("") },
        trailingContent = {
            Switch(
                checked = useProgressive,
                onCheckedChange = null
            )
        },
        checked = useProgressive,
        onCheckedChange = { useProgressive = it },
        colors = colors,
        shapes = ListItemDefaults.segmentedShapes(2, 4)
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ColumnScope.HazeGlassOptions(
    dataStore: NewSettingsHandling,
    colors: ListItemColors,
) {
    val hazeGlass = remember { hazeGlassOptions }
    val hazeGlassState by hazeGlass
        .asFlow()
        .collectAsStateWithLifecycle(HazeOptionsInfo())
    var showBlurOptions by remember { mutableStateOf(false) }
    var blurType by dataStore.rememberBlurType()
    var useProgressive by dataStore.rememberUseProgressive()
    val scope = rememberCoroutineScope()

    HazeSettingsList(
        options = hazeGlassState,
        onOptionsChange = { scope.launch { hazeGlass.set(it) } }
    )

    /*SegmentedListItem(
        leadingContent = {
            DiagonalWipeIcon(
                isWiped = useProgressive,
                wipedIcon = Icons.Default.BlurCircular,
                baseIcon = Icons.Default.BlurLinear,
                motion = DiagonalWipeIconDefaults.expressive(),
                modifier = Modifier.size(24.dp)
            )
        },
        content = { Text("Use Progressive Blur") },
        supportingContent = { Text("") },
        trailingContent = {
            Switch(
                checked = useProgressive,
                onCheckedChange = null
            )
        },
        checked = useProgressive,
        onCheckedChange = { useProgressive = it },
        colors = colors,
        shapes = ListItemDefaults.segmentedShapes(2, 4)
    )*/


}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ColumnScope.LiquidGlassOptions(
    dataStore: NewSettingsHandling,
    colors: ListItemColors,
) {
    var blurAmount by dataStore.rememberLiquidGlassBlurAmount()
    ListItem(
        headlineContent = { Text("Blur Amount") },
        overlineContent = { Text("(Default is 1)") },
        trailingContent = { Text("${blurAmount.roundToInt()}") },
        leadingContent = { Icon(Icons.Default.BlurCircular, null) },
        supportingContent = {
            Slider(
                value = blurAmount,
                onValueChange = { blurAmount = it },
                valueRange = 0f..10f,
                steps = 11
            )
        },
        colors = colors,
    )

    var refractionHeight by dataStore.rememberLiquidGlassRefractionHeight()
    ListItem(
        headlineContent = { Text("Refraction Height") },
        overlineContent = { Text("(Default is 12)") },
        trailingContent = { Text("${refractionHeight.roundToInt()}") },
        leadingContent = { Icon(Icons.Default.BlurCircular, null) },
        supportingContent = {
            Slider(
                value = refractionHeight,
                onValueChange = { refractionHeight = it },
                valueRange = 1f..100f,
                steps = 100
            )
        },
        colors = colors,
    )

    var refractionAmount by dataStore.rememberLiquidGlassRefractionAmount()
    ListItem(
        headlineContent = { Text("Refraction Amount") },
        overlineContent = { Text("(Default is 32)") },
        trailingContent = { Text("${refractionAmount.roundToInt()}") },
        leadingContent = { Icon(Icons.Default.BlurCircular, null) },
        supportingContent = {
            Slider(
                value = refractionAmount,
                onValueChange = { refractionAmount = it },
                valueRange = 1f..100f,
                steps = 100
            )
        },
        colors = colors,
    )

    var depthEffect by dataStore.rememberLiquidGlassDepthEffect()
    ListItem(
        content = { Text("Depth Effect") },
        overlineContent = { Text("(Default is true)") },
        checked = depthEffect,
        onCheckedChange = { depthEffect = it },
        trailingContent = { Switch(checked = depthEffect, onCheckedChange = null) },
        colors = colors,
    )

    var chromaticAberration by dataStore.rememberLiquidGlassChromaticAberration()
    ListItem(
        content = { Text("Chromatic Aberration (Default true)") },
        overlineContent = { Text("(Default is true)") },
        checked = chromaticAberration,
        onCheckedChange = { chromaticAberration = it },
        trailingContent = { Switch(checked = chromaticAberration, onCheckedChange = null) },
        colors = colors,
    )

    Button(
        onClick = {
            blurAmount = 1f
            refractionHeight = 12f
            refractionAmount = 32f
            depthEffect = true
            chromaticAberration = true
        },
        modifier = Modifier
            .fillMaxWidth(.5f)
            .align(Alignment.CenterHorizontally)
    ) { Text("Reset") }
}

@Composable
fun HazeSettingsList(
    options: HazeOptionsInfo,
    onOptionsChange: (HazeOptionsInfo) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = "Haze Configuration",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Refraction Settings
        FloatSetting(
            label = "Refraction Strength",
            value = options.refractionStrength,
            valueRange = 0f..1f,
            onValueChange = { onOptionsChange(options.copy(refractionStrength = it)) }
        )
        FloatSetting(
            label = "Refraction Height Fraction",
            value = options.refractionHeightFraction,
            valueRange = 0f..1f,
            onValueChange = { onOptionsChange(options.copy(refractionHeightFraction = it)) }
        )
        IntSetting(
            label = "Refraction Displacement",
            value = options.refractionDisplacement,
            valueRange = 0f..50f,
            onValueChange = { onOptionsChange(options.copy(refractionDisplacement = it)) }
        )

        // Depth & Blur
        FloatSetting(
            label = "Depth",
            value = options.depth,
            valueRange = 0f..1f,
            onValueChange = { onOptionsChange(options.copy(depth = it)) }
        )
        IntSetting(
            label = "Blur Radius",
            value = options.blurRadius,
            valueRange = 0f..100f,
            onValueChange = { onOptionsChange(options.copy(blurRadius = it)) }
        )
        IntSetting(
            label = "Edge Softness",
            value = options.edgeSoftness,
            valueRange = 0f..50f,
            onValueChange = { onOptionsChange(options.copy(edgeSoftness = it)) }
        )

        // Lighting & Specular
        FloatSetting(
            label = "Specular Intensity",
            value = options.specularIntensity,
            valueRange = 0f..2f,
            onValueChange = { onOptionsChange(options.copy(specularIntensity = it)) }
        )
        FloatSetting(
            label = "Ambient Response",
            value = options.ambientResponse,
            valueRange = 0f..1f,
            onValueChange = { onOptionsChange(options.copy(ambientResponse = it)) }
        )
        FloatSetting(
            label = "Specular Exponent",
            value = options.specularExponent,
            valueRange = 0.1f..10f,
            onValueChange = { onOptionsChange(options.copy(specularExponent = it)) }
        )
        FloatSetting(
            label = "Fresnel Exponent",
            value = options.fresnelExponent,
            valueRange = 0.1f..10f,
            onValueChange = { onOptionsChange(options.copy(fresnelExponent = it)) }
        )

        // Chroma
        FloatSetting(
            label = "Chromatic Aberration Strength",
            value = options.chromaticAberrationStrength,
            valueRange = 0f..1f,
            onValueChange = { onOptionsChange(options.copy(chromaticAberrationStrength = it)) }
        )
        FloatSetting(
            label = "Chroma Multiplier",
            value = options.chromaMultiplier,
            valueRange = 0f..2f,
            onValueChange = { onOptionsChange(options.copy(chromaMultiplier = it)) }
        )
    }
}

// --- Reusable Slider Components ---

@Composable
fun FloatSetting(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange
        )
    }
}

@Composable
fun IntSetting(
    label: String,
    value: Int,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            Text(text = value.toString(), style = MaterialTheme.typography.bodyMedium)
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.roundToInt()) },
            valueRange = valueRange
        )
    }
}