package com.programmersbox.showcase.samples

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.programmersbox.showcase.annotations.ShowcaseComponent

@ShowcaseComponent(
    name = "Motion Scheme",
    description = "Samples of different motion schemes",
    group = "MotionScheme"
)
@Composable
fun MotionSchemeSamples() {
    MotionSchemeSample(
        MotionScheme.expressive(),
        MotionScheme.standard()
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MotionSchemeSample(
    motionScheme: MotionScheme,
    motionScheme2: MotionScheme,
) {
    var side by remember { mutableStateOf(false) }
    val value by remember {
        derivedStateOf {
            if (side) 1f else 0f
        }
    }
    Column {
        MotionSchemeItem(
            type = "Fast Effects",
            value = value,
            finiteAnimationSpec = motionScheme.fastEffectsSpec(),
            finiteAnimationSpec2 = motionScheme2.fastEffectsSpec()
        )
        MotionSchemeItem(
            type = "Fast Spatial",
            value = value,
            finiteAnimationSpec = motionScheme.fastSpatialSpec(),
            finiteAnimationSpec2 = motionScheme2.fastSpatialSpec()
        )
        MotionSchemeItem(
            type = "Default Effects",
            value = value,
            finiteAnimationSpec = motionScheme.defaultEffectsSpec(),
            finiteAnimationSpec2 = motionScheme2.defaultEffectsSpec()
        )
        MotionSchemeItem(
            type = "Default Spatial",
            value = value,
            finiteAnimationSpec = motionScheme.defaultSpatialSpec(),
            finiteAnimationSpec2 = motionScheme2.defaultSpatialSpec()
        )
        MotionSchemeItem(
            type = "Slow Effects",
            value = value,
            finiteAnimationSpec = motionScheme.slowEffectsSpec(),
            finiteAnimationSpec2 = motionScheme2.slowEffectsSpec()
        )
        MotionSchemeItem(
            type = "Slow Spatial",
            value = value,
            finiteAnimationSpec = motionScheme.slowSpatialSpec(),
            finiteAnimationSpec2 = motionScheme2.slowSpatialSpec()
        )

        ListItem(
            content = { Text("Start Animation") },
            checked = side,
            onCheckedChange = { side = it }
        )
    }
}

@Composable
fun MotionSchemeItem(
    type: String,
    value: Float,
    finiteAnimationSpec: FiniteAnimationSpec<Float>,
    finiteAnimationSpec2: FiniteAnimationSpec<Float>,
) {
    val animatedValue by animateFloatAsState(
        targetValue = value,
        animationSpec = finiteAnimationSpec,
        label = ""
    )

    val animatedValue2 by animateFloatAsState(
        targetValue = value,
        animationSpec = finiteAnimationSpec2,
        label = ""
    )

    ListItem(
        headlineContent = { Text(type) },
        supportingContent = {
            Column {
                Text("Expressive")
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    LinearProgressIndicator(
                        progress = { animatedValue }
                    )
                    Text(animatedValue.toString())
                }

                Text("Standard")
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    LinearProgressIndicator(
                        progress = { animatedValue2 }
                    )
                    Text(animatedValue2.toString())
                }
            }
        }
    )
}