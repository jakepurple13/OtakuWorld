package com.programmersbox.kmpuiviews.presentation.onboarding.steps

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.dp
import com.materialkolor.material3.rememberDynamicColorScheme
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.datastore.SystemThemeMode
import com.programmersbox.datastore.ThemeColor
import com.programmersbox.kmpuiviews.presentation.components.seedColor
import com.programmersbox.kmpuiviews.presentation.components.settings.CategoryGroup
import com.programmersbox.kmpuiviews.presentation.components.settings.SwitchSetting
import com.programmersbox.kmpuiviews.presentation.onboarding.HeroShape
import com.programmersbox.kmpuiviews.presentation.onboarding.OnboardingScope
import com.programmersbox.kmpuiviews.presentation.onboarding.OnboardingStepSpec
import com.programmersbox.kmpuiviews.presentation.onboarding.frame.StepLayout
import com.programmersbox.kmpuiviews.presentation.onboarding.motion.ColorSweepHost
import com.programmersbox.kmpuiviews.presentation.onboarding.motion.LocalColorSweep
import com.programmersbox.kmpuiviews.presentation.onboarding.motion.onboardingReducedMotion
import com.programmersbox.kmpuiviews.presentation.settings.general.ColorBlindTypeSettings
import com.programmersbox.kmpuiviews.presentation.settings.general.ExpressivenessSetting
import com.programmersbox.kmpuiviews.theme.UglyDarkColorScheme
import com.programmersbox.kmpuiviews.theme.UglyLightColorScheme

private val MainSwatches = listOf(
    ThemeColor.Dynamic,
    ThemeColor.Blue,
    ThemeColor.Red,
    ThemeColor.Green,
    ThemeColor.Yellow,
    ThemeColor.Cyan,
    ThemeColor.Magenta,
)
private val ExtraSwatches = listOf(ThemeColor.Ugly, ThemeColor.MyEyes)

private val SwatchSize = 44.dp

/** Step 2: pick mode and theme color; every change applies app-wide immediately with a color sweep. */
fun OnboardingScope.lookStep(handling: NewSettingsHandling) {
    val spec = OnboardingStepSpec(
        id = "look",
        title = "Make it yours",
        body = "Pick a theme. Every change shows up right away.",
        icon = Icons.Default.Palette,
        shape = HeroShape.Cookie,
        summaryLabel = "Look",
        summary = {
            val mode by handling.rememberSystemThemeMode()
            val color by handling.rememberThemeColor()
            val amoled by handling.rememberIsAmoledMode()
            val modeLabel = when {
                isDark(mode) && amoled -> "AMOLED"
                mode == SystemThemeMode.Day -> "Light"
                mode == SystemThemeMode.Night -> "Dark"
                else -> "System"
            }
            "${color.name} · $modeLabel"
        },
    )
    step(spec) {
        // Uses a host a parent provides (Task 10 may lift it to the scaffold); otherwise wraps the step itself.
        val outerSweep = LocalColorSweep.current
        if (outerSweep != null) {
            LookContent(spec, handling, outerSweep)
        } else {
            var hostCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
            ColorSweepHost { sweepFrom ->
                Box(Modifier.fillMaxSize().onGloballyPositioned { hostCoords = it }) {
                    LookContent(spec, handling) { root ->
                        sweepFrom(root - (hostCoords?.positionInRoot() ?: Offset.Zero))
                    }
                }
            }
        }
    }
}

@Composable
private fun isDark(mode: SystemThemeMode): Boolean = when (mode) {
    SystemThemeMode.Night -> true
    SystemThemeMode.Day -> false
    else -> isSystemInDarkTheme()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LookContent(
    spec: OnboardingStepSpec,
    handling: NewSettingsHandling,
    sweepFromRoot: (Offset) -> Unit,
) {
    var mode by handling.rememberSystemThemeMode()
    var themeColor by handling.rememberThemeColor()
    var isAmoled by handling.rememberIsAmoledMode()
    var showMore by remember { mutableStateOf(false) }
    val dark = isDark(mode)
    val reduced = onboardingReducedMotion()

    val modes = listOf(
        SystemThemeMode.FollowSystem to "System",
        SystemThemeMode.Day to "Light",
        SystemThemeMode.Night to "Dark",
    )

    StepLayout(spec, preview = { ThemePreview(Modifier.padding(bottom = 4.dp)) }) {
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            modes.forEachIndexed { index, (value, label) ->
                SegmentedButton(
                    selected = mode == value,
                    onClick = { mode = value },
                    shape = SegmentedButtonDefaults.itemShape(index, modes.size),
                    label = { Text(label) },
                )
            }
        }

        SwatchRow(MainSwatches, themeColor, dark, isAmoled) { color, root ->
            sweepFromRoot(root)
            themeColor = color
        }

        AnimatedVisibility(
            visible = dark,
            enter = if (reduced) EnterTransition.None else expandVertically(),
            exit = if (reduced) ExitTransition.None else shrinkVertically(),
        ) {
            CategoryGroup {
                item {
                    SwitchSetting(
                        settingTitle = { Text("AMOLED black") },
                        summaryValue = { Text("Pure black backgrounds") },
                        settingIcon = { Icon(Icons.Default.DarkMode, null, modifier = Modifier.fillMaxSize()) },
                        value = isAmoled,
                        updateValue = { isAmoled = it },
                    )
                }
            }
        }

        ListItem(
            headlineContent = { Text("More look options") },
            supportingContent = { Text("Expressiveness, color-blind mode, cover-tinted details") },
            leadingContent = { Icon(Icons.Default.MoreHoriz, null) },
            modifier = Modifier.clip(RoundedCornerShape(16.dp)).clickable { showMore = true },
        )
    }

    if (showMore) {
        ModalBottomSheet(
            onDismissRequest = { showMore = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp),
            ) {
                CategoryGroup {
                    item { ExpressivenessSetting(handling) }
                    item { ColorBlindTypeSettings(handling) }
                    item {
                        var usePalette by handling.rememberUsePalette()
                        SwitchSetting(
                            settingTitle = { Text("Color details from cover") },
                            summaryValue = { Text("Tint the details screen with colors from the cover art") },
                            settingIcon = { Icon(Icons.Default.Palette, null, modifier = Modifier.fillMaxSize()) },
                            value = usePalette,
                            updateValue = { usePalette = it },
                        )
                    }
                }
                SwatchRow(ExtraSwatches, themeColor, dark, isAmoled) { color, root ->
                    sweepFromRoot(root)
                    themeColor = color
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SwatchRow(
    colors: List<ThemeColor>,
    selected: ThemeColor,
    dark: Boolean,
    isAmoled: Boolean,
    onPick: (ThemeColor, Offset) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        colors.forEach { color ->
            val scheme = when (color) {
                ThemeColor.Dynamic -> MaterialTheme.colorScheme
                ThemeColor.Ugly, ThemeColor.MyEyes -> if (dark) UglyDarkColorScheme else UglyLightColorScheme
                else -> rememberDynamicColorScheme(color.seedColor, isDark = dark, isAmoled = isAmoled && dark)
            }
            Swatch(color, scheme, color == selected, onPick)
        }
    }
}

@Composable
private fun Swatch(
    color: ThemeColor,
    scheme: ColorScheme,
    selected: Boolean,
    onPick: (ThemeColor, Offset) -> Unit,
) {
    var center by remember { mutableStateOf(Offset.Zero) }
    // Circle at rest, rounded square (11 dp) when selected.
    val corner by animateDpAsState(if (selected) 11.dp else SwatchSize / 2)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable { onPick(color, center) },
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(SwatchSize)
                .onGloballyPositioned {
                    center = it.positionInRoot() + Offset(it.size.width / 2f, it.size.height / 2f)
                }
                .clip(RoundedCornerShape(corner))
                .background(scheme.primary),
        ) {
            Box(Modifier.align(Alignment.BottomEnd).size(SwatchSize / 2).background(scheme.tertiary, CircleShape))
            SwatchCheck(selected, scheme.onPrimary)
        }
        Text(color.name, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
    }
}

// Separate composable: inside the swatch Box the Column receiver is blocked by the layout DSL marker.
@Composable
private fun SwatchCheck(visible: Boolean, tint: androidx.compose.ui.graphics.Color) {
    AnimatedVisibility(visible, enter = scaleIn(), exit = scaleOut()) {
        Icon(Icons.Default.Check, null, tint = tint)
    }
}

/** A tiny details row so theme changes have something to show on. */
@Composable
private fun ThemePreview(modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(width = 48.dp, height = 68.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Filled.MenuBook, null, tint = MaterialTheme.colorScheme.onTertiary)
                }
            }
            Column {
                Text(
                    "Preview title",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    "Chapter 12 · 3 min ago",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}
