package com.programmersbox.kmpuiviews.presentation.dailyoverview.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.programmersbox.kmpuiviews.domain.updatecheck.UpdateUnit
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.pluralStringResource
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.do_stat_read_chapters
import otakuworld.kmpuiviews.generated.resources.do_stat_read_episodes
import otakuworld.kmpuiviews.generated.resources.do_stat_titles
import otakuworld.kmpuiviews.generated.resources.do_stat_updates

/** Read count, titles and new updates. Digits roll up from 0 on first show. */
@Composable
fun StatTiles(
    readCount: Int,
    titleCount: Int,
    updateCount: Int,
    unit: UpdateUnit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        StatTile(
            value = readCount,
            label = pluralStringResource(
                when (unit) {
                    UpdateUnit.Chapters -> Res.plurals.do_stat_read_chapters
                    UpdateUnit.Episodes -> Res.plurals.do_stat_read_episodes
                },
                readCount
            ),
            container = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.weight(1f)
        )
        StatTile(
            value = titleCount,
            label = pluralStringResource(Res.plurals.do_stat_titles, titleCount),
            container = MaterialTheme.colorScheme.tertiaryContainer,
            modifier = Modifier.weight(1f)
        )
        StatTile(
            value = updateCount,
            label = pluralStringResource(Res.plurals.do_stat_updates, updateCount),
            container = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatTile(
    value: Int,
    label: String,
    container: Color,
    modifier: Modifier = Modifier,
) {
    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.contentColorFor(container)) {
        Column(
            modifier = modifier
                .clip(RoundedCornerShape(18.dp))
                .background(container)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            RollingNumber(value)
            Text(
                label,
                fontSize = 11.sp,
                color = LocalContentColor.current.copy(alpha = 0.8f),
            )
        }
    }
}

private val DigitStyle = TextStyle(
    fontSize = 26.sp,
    lineHeight = 30.sp,
    fontWeight = FontWeight.ExtraBold,
    fontFeatureSettings = "tnum",
)

private const val RollDelayMs = 420L
private const val RollStaggerMs = 90L

@Composable
private fun RollingNumber(value: Int) {
    Row(
        modifier = Modifier
            .height(30.dp)
            .clipToBounds()
    ) {
        value.toString().forEachIndexed { index, digit ->
            // Starts at 0 and rolls to the real digit, one column at a time.
            var shown by remember { mutableStateOf('0') }
            LaunchedEffect(digit) {
                delay(RollDelayMs + index * RollStaggerMs)
                shown = digit
            }
            AnimatedContent(
                targetState = shown,
                transitionSpec = {
                    val motion = spring<IntOffset>(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow,
                    )
                    val direction = if (targetState > initialState) 1 else -1
                    (slideInVertically(motion) { it * direction } + fadeIn())
                        .togetherWith(slideOutVertically(motion) { -it * direction } + fadeOut())
                },
                label = "digit",
            ) { Text(it.toString(), style = DigitStyle) }
        }
    }
}
