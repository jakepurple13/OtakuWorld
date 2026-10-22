package com.programmersbox.animeworld

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.PersonalVideo
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.datastore.asState
import com.programmersbox.kmpuiviews.presentation.components.settings.CategoryGroup
import com.programmersbox.kmpuiviews.presentation.components.settings.PreferenceSetting
import com.programmersbox.kmpuiviews.presentation.components.settings.SliderSetting
import com.programmersbox.kmpuiviews.presentation.components.settings.SwitchSetting
import com.programmersbox.kmpuiviews.presentation.tips.TipGesture
import com.programmersbox.kmpuiviews.presentation.tips.TipsScope
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import kotlin.math.roundToInt

/** AnimeWorld's onboarding page: the player settings people usually want to set first. */
@Composable
fun PlayerOnboarding(
    animeDataStoreHandling: AnimeDataStoreHandling,
    onSubtitlesClick: () -> Unit,
    settingsHandling: NewSettingsHandling = koinInject(),
) {
    val scope = rememberCoroutineScope()

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        ListItem(
            headlineContent = { Text("Set up the player") },
            supportingContent = { Text("There are more player settings in Settings.") },
        )

        CategoryGroup {
            item {
                val player by animeDataStoreHandling.useNewPlayer.asState()
                SwitchSetting(
                    settingTitle = { Text(stringResource(R.string.use_new_player)) },
                    summaryValue = { Text(stringResource(R.string.use_new_player_description)) },
                    settingIcon = { Icon(Icons.Default.PersonalVideo, null, modifier = Modifier.fillMaxSize()) },
                    value = player,
                    updateValue = { scope.launch { animeDataStoreHandling.useNewPlayer.set(it) } }
                )
            }

            item {
                PreferenceSetting(
                    settingTitle = { Text(stringResource(R.string.subtitles)) },
                    summaryValue = { Text("Size, color and outline") },
                    settingIcon = { Icon(Icons.Default.Subtitles, null, modifier = Modifier.fillMaxSize()) },
                    onClick = onSubtitlesClick,
                )
            }

            item {
                val batteryPercent = settingsHandling.batteryPercent
                val saved by batteryPercent.rememberPreference()
                var sliderValue by remember(saved) { mutableFloatStateOf(saved.toFloat()) }

                SliderSetting(
                    sliderValue = sliderValue,
                    settingTitle = { Text("Battery alert") },
                    settingSummary = { Text("Warn me when the battery drops below ${sliderValue.roundToInt()}%") },
                    settingIcon = { Icon(Icons.Default.BatteryAlert, null) },
                    range = 1f..100f,
                    updateValue = { sliderValue = it },
                    onValueChangedFinished = { scope.launch { batteryPercent.set(sliderValue.toInt()) } }
                )
            }
        }
    }
}

val animeTips: TipsScope.() -> Unit = {
    tip(
        title = "Drag up or down in the player",
        description = "The left side changes brightness. The right side changes volume.",
        gesture = TipGesture.Drag,
        demo = { PlayerDragDemo(it) },
    )
    tip(
        title = "Drag sideways in the player",
        description = "Seeks forward or back.",
        gesture = TipGesture.Swipe,
    )
    tip(
        title = "Double-tap the player",
        description = "Plays or pauses.",
        gesture = TipGesture.Tap,
    )
    tip(
        title = "Swipe a downloaded video",
        description = "Swipe right to play it. Swipe left to delete it.",
        gesture = TipGesture.Swipe,
    )
}

/** A fake video frame: drag the left half for brightness, the right half for volume. */
@Composable
private fun PlayerDragDemo(onDone: () -> Unit) {
    var brightness by remember { mutableFloatStateOf(0.7f) }
    var volume by remember { mutableFloatStateOf(0.5f) }
    var activeSide by remember { mutableStateOf<Boolean?>(null) }
    var dragged by remember { mutableFloatStateOf(0f) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        modifier = Modifier.fillMaxSize()
    ) {
        Text("Drag up or down on either side", style = MaterialTheme.typography.titleMedium)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 10f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF0D0F16))
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragStart = { offset ->
                            activeSide = offset.x < size.width / 2
                            dragged = 0f
                        },
                        onDragEnd = { activeSide = null },
                        onDragCancel = { activeSide = null },
                    ) { _, dragAmount ->
                        val delta = -dragAmount / size.height
                        if (activeSide == true) {
                            brightness = (brightness + delta).coerceIn(0f, 1f)
                        } else {
                            volume = (volume + delta).coerceIn(0f, 1f)
                        }
                        dragged += kotlin.math.abs(delta)
                        if (dragged > 0.1f) onDone()
                    }
                }
        ) {
            // Darkens the frame as brightness drops.
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = (1f - brightness) * 0.85f))
            )

            Row(Modifier.fillMaxSize()) {
                PlayerHalf(Icons.Default.BrightnessMedium, "Brightness", Modifier.weight(1f))
                PlayerHalf(Icons.AutoMirrored.Filled.VolumeUp, "Volume", Modifier.weight(1f))
            }

            activeSide?.let { left ->
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.Black.copy(alpha = 0.6f),
                    contentColor = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(8.dp)
                ) {
                    val value = if (left) brightness else volume
                    Text(
                        "${if (left) "Brightness" else "Volume"} ${(value * 100).roundToInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayerHalf(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier.fillMaxHeight()
    ) {
        Icon(icon, null, tint = Color.White.copy(alpha = 0.6f))
        Text(
            label,
            color = Color.White.copy(alpha = 0.6f),
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center
        )
    }
}
