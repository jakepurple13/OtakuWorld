package com.programmersbox.kmpuiviews.presentation.onboarding.steps

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.programmersbox.datastore.GridChoice
import com.programmersbox.datastore.MiddleNavigationAction
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.datastore.rememberFloatingNavigation
import com.programmersbox.kmpuiviews.presentation.components.item
import com.programmersbox.kmpuiviews.presentation.components.settings.CategoryGroup
import com.programmersbox.kmpuiviews.presentation.components.settings.SwitchSetting
import com.programmersbox.kmpuiviews.presentation.onboarding.HeroShape
import com.programmersbox.kmpuiviews.presentation.onboarding.OnboardingScope
import com.programmersbox.kmpuiviews.presentation.onboarding.OnboardingStepSpec
import com.programmersbox.kmpuiviews.presentation.onboarding.OnboardingTags
import com.programmersbox.kmpuiviews.presentation.onboarding.frame.StepLayout
import com.programmersbox.kmpuiviews.presentation.onboarding.motion.onboardingReducedMotion

/** "Multiple" stays in Settings; the onboarding picker offers the four single destinations. */
internal val middleOptions = listOf(
    MiddleNavigationAction.Notifications,
    MiddleNavigationAction.Lists,
    MiddleNavigationAction.Favorites,
    MiddleNavigationAction.Search,
)

private val gridOptions = listOf(
    GridChoice.FullAdaptive to "Full adaptive",
    GridChoice.Adaptive to "Adaptive",
    GridChoice.Fixed to "Fixed",
)

private fun GridChoice.columns() = when (this) {
    GridChoice.FullAdaptive -> 4
    GridChoice.Adaptive -> 3
    else -> 2
}

/** Step 3: grid type, floating navigation and middle button, with a mini live preview. */
fun OnboardingScope.layoutStep(handling: NewSettingsHandling, appName: String = "OtakuWorld") {
    val spec = OnboardingStepSpec(
        id = "layout",
        title = "Arrange your shelf",
        body = "How covers fill the screen and where navigation sits.",
        icon = Icons.Default.GridView,
        shape = HeroShape.Diamond,
        summaryLabel = "Layout",
        summary = {
            val grid by handling.rememberGridChoice()
            val floating by rememberFloatingNavigation()
            val gridLabel = when (grid) {
                GridChoice.FullAdaptive -> "Full adaptive"
                GridChoice.Adaptive -> "Adaptive"
                else -> "Fixed"
            }
            "$gridLabel grid · ${if (floating) "floating" else "docked"} nav"
        },
    )
    step(spec) { LayoutContent(spec, handling, appName) }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LayoutContent(spec: OnboardingStepSpec, handling: NewSettingsHandling, appName: String) {
    var grid by handling.rememberGridChoice()
    var floating by rememberFloatingNavigation()
    var middle by handling.rememberMiddleNavigationAction()

    Box(Modifier.fillMaxSize().testTag(OnboardingTags.step(spec.id))) {
        StepLayout(spec, preview = { MiniPreview(appName, grid, floating, middle) }) {
            SectionLabel("Grid")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                gridOptions.forEach { (option, label) ->
                    GridOption(option, label, option == grid, Modifier.weight(1f)) { grid = option }
                }
            }

            CategoryGroup {
                item {
                    SwitchSetting(
                        settingTitle = { Text("Floating navigation") },
                        summaryValue = { Text("A pill that floats above content") },
                        settingIcon = { Icon(Icons.Default.Navigation, null, modifier = Modifier.fillMaxSize()) },
                        value = floating,
                        updateValue = { floating = it },
                    )
                }
            }

            SectionLabel("Middle button")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                middleOptions.forEach { option ->
                    FilterChip(
                        selected = middle == option,
                        onClick = { middle = option },
                        label = { Text(option.name) },
                    )
                }
            }
        }
    }
}

@Composable
internal fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp),
    )
}

@Composable
private fun GridOption(
    option: GridChoice,
    label: String,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val outline = MaterialTheme.colorScheme.outlineVariant
    val primary = MaterialTheme.colorScheme.primary
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) primary else outline),
        modifier = modifier.clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(10.dp),
        ) {
            val cols = option.columns()
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                repeat(2) {
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        repeat(cols) {
                            Box(
                                Modifier
                                    .size(width = 36.dp / cols * 2, height = 14.dp)
                                    .background(primary.copy(alpha = 0.55f), RoundedCornerShape(3.dp))
                            )
                        }
                    }
                }
            }
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

private val CoverHues = listOf(0.62f, 0.9f, 0.3f, 0.08f, 0.5f, 0.75f, 0.15f, 0.42f)

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun MiniPreview(appName: String, grid: GridChoice, floating: Boolean, middle: MiddleNavigationAction) {
    val reduced = onboardingReducedMotion()
    val cols = grid.columns()
    val sidePad by animateDpAsState(if (floating) 22.dp else 0.dp)
    val bottomPad by animateDpAsState(if (floating) 8.dp else 0.dp)
    val corner by animateDpAsState(if (floating) 19.dp else 0.dp)
    val side = if (reduced) (if (floating) 22.dp else 0.dp) else sidePad
    val bottom = if (reduced) (if (floating) 8.dp else 0.dp) else bottomPad
    val radius = if (reduced) (if (floating) 19.dp else 0.dp) else corner

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth().height(176.dp).padding(top = 4.dp).clip(RoundedCornerShape(16.dp)),
    ) {
        Box {
            androidx.compose.foundation.layout.Column {
                Text(
                    appName,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                )
                LazyVerticalGrid(
                    columns = GridCells.Fixed(cols),
                    userScrollEnabled = false,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("onb_mini_grid")
                        .semantics { stateDescription = "cols=$cols" },
                ) {
                    items(CoverHues.indices.toList(), key = { it }) { i ->
                        Box(
                            Modifier
                                .animateItem()
                                .height(28.dp)
                                .background(Color.hsv(CoverHues[i] * 360f, 0.45f, 0.85f), RoundedCornerShape(6.dp))
                        )
                    }
                }
            }
            Surface(
                shape = RoundedCornerShape(radius),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shadowElevation = if (floating) 6.dp else 0.dp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = side, end = side, bottom = bottom)
                    .fillMaxWidth()
                    .height(38.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    Icon(Icons.Default.Home, null, Modifier.size(18.dp))
                    AnimatedContent(
                        targetState = middle,
                        transitionSpec = {
                            if (reduced) ContentTransform(fadeIn(), fadeOut()) else
                                (fadeIn() + scaleIn(initialScale = 0.4f)) togetherWith (fadeOut() + scaleOut())
                        },
                        label = "middleIcon",
                    ) { action ->
                        val spin by transition.animateFloat(label = "spin") {
                            if (it == EnterExitState.Visible || reduced) 0f else -90f
                        }
                        val vector = action.item?.icon?.invoke(true) ?: Icons.Default.GridView
                        Icon(vector, null, Modifier.size(18.dp).graphicsLayer { rotationZ = spin })
                    }
                    Icon(Icons.Default.Settings, null, Modifier.size(18.dp))
                }
            }
        }
    }
}
