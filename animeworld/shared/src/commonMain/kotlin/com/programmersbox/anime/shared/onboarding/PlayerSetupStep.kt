package com.programmersbox.anime.shared.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.programmersbox.anime.shared.subtitles.CaptionCue
import com.programmersbox.anime.shared.subtitles.ColorSwatches
import com.programmersbox.anime.shared.subtitles.EdgeSegments
import com.programmersbox.anime.shared.subtitles.SubtitleFont
import com.programmersbox.anime.shared.subtitles.SubtitleSettingsRepository
import com.programmersbox.anime.shared.subtitles.SubtitleStyle
import com.programmersbox.anime.shared.subtitles.subtitleLanguages
import com.programmersbox.anime.shared.subtitles.toCaptionSpec
import com.programmersbox.anime.shared.videoplayer.PlayerPreviewDefaults
import com.programmersbox.anime.shared.videoplayer.PlayerStyle
import com.programmersbox.anime.shared.videoplayer.PlayerStylePreview
import com.programmersbox.anime.shared.videoplayer.videoPlayerBlur
import com.programmersbox.anime.shared.videoplayer.videoPlayerStyle
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.datastore.asState
import com.programmersbox.kmpuiviews.platform
import com.programmersbox.kmpuiviews.presentation.components.settings.CategoryGroup
import com.programmersbox.kmpuiviews.presentation.components.settings.SwitchSetting
import com.programmersbox.kmpuiviews.presentation.onboarding.HeroShape
import com.programmersbox.kmpuiviews.presentation.onboarding.OnboardingScope
import com.programmersbox.kmpuiviews.presentation.onboarding.OnboardingStepSpec
import com.programmersbox.kmpuiviews.presentation.onboarding.OnboardingTags
import com.programmersbox.kmpuiviews.presentation.onboarding.frame.StepLayout
import com.programmersbox.kmpuiviews.presentation.onboarding.motion.onboardingReducedMotion
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import kotlin.math.roundToInt

internal const val PlayerSubtitleSampleTag = "onb_player_subtitle_sample"
internal const val PlayerSizeTag = "onb_player_size"
internal const val PlayerBatteryTag = "onb_player_battery"

private const val SampleCue = "Hello, Dr. Arinsu."

/** The battery alert only exists on Android; desktop has no battery readout in the player. */
private val hasBatteryAlert: Boolean get() = platform() == "Android"

/**
 * Step 5 for AnimeWorld: player look plus the subtitle and battery settings people reach for first.
 * Dependencies left null come from Koin when the step is shown.
 */
fun OnboardingScope.playerSetupStep(
    repo: SubtitleSettingsRepository? = null,
    settingsHandling: NewSettingsHandling? = null,
) {
    val spec = OnboardingStepSpec(
        id = "app",
        title = "Set up the player",
        body = "Pick the player look. More options live in Settings › Player.",
        icon = Icons.Default.PlayArrow,
        shape = HeroShape.Clover,
        summaryLabel = "Player",
        summary = {
            val style by videoPlayerStyle.asState()
            "${style.label} player"
        },
    )
    step(spec) { PlayerSetupContent(spec, repo, settingsHandling) }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun PlayerSetupContent(
    spec: OnboardingStepSpec,
    givenRepo: SubtitleSettingsRepository?,
    givenSettings: NewSettingsHandling?,
) {
    val repo = givenRepo ?: koinInject<SubtitleSettingsRepository>()
    val settingsHandling = givenSettings ?: koinInject<NewSettingsHandling>()
    val scope = rememberCoroutineScope()
    var playerStyle by videoPlayerStyle.asState()
    var blur by videoPlayerBlur.asState()
    val style by repo.style.collectAsState(SubtitleStyle())
    fun change(transform: (SubtitleStyle) -> SubtitleStyle) {
        scope.launch { repo.update(transform) }
    }

    val batteryState = settingsHandling.batteryPercent.rememberPreference()
    val savedBattery by batteryState
    var batteryDraft by remember { mutableStateOf<Int?>(null) }
    val reduced = onboardingReducedMotion()

    Box(Modifier.fillMaxSize().testTag(OnboardingTags.step(spec.id))) {
        StepLayout(
            spec,
            preview = {
                PlayerPreview(playerStyle = playerStyle, style = style, animate = !reduced)
            },
        ) {
            SectionLabel("Player style")
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                PlayerStyle.entries.forEachIndexed { index, option ->
                    SegmentedButton(
                        selected = playerStyle == option,
                        onClick = { playerStyle = option },
                        shape = SegmentedButtonDefaults.itemShape(index, PlayerStyle.entries.size),
                        label = { Text(option.label) },
                    )
                }
            }

            CategoryGroup {
                item {
                    SwitchSetting(
                        value = blur,
                        updateValue = { blur = it },
                        settingTitle = { Text("Blur player controls") },
                        summaryValue = { Text("Frosted bars over the video") },
                        settingIcon = { Icon(Icons.Default.BlurOn, null, modifier = Modifier.fillMaxSize()) },
                    )
                }
            }

            SectionLabel("Subtitles")
            CategoryGroup {
                item {
                    SwitchSetting(
                        value = style.autoEnable,
                        updateValue = { on -> change { it.copy(autoEnable = on) } },
                        settingTitle = { Text("Turn on automatically") },
                        settingIcon = { Icon(Icons.Default.ClosedCaption, null, modifier = Modifier.fillMaxSize()) },
                    )
                }
            }

            SectionLabel("Preferred language")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                subtitleLanguages.forEach { (code, name) ->
                    FilterChip(
                        selected = code in style.preferredLanguages,
                        onClick = {
                            change {
                                it.copy(
                                    preferredLanguages = if (code in it.preferredLanguages) {
                                        it.preferredLanguages - code
                                    } else {
                                        it.preferredLanguages + code
                                    }
                                )
                            }
                        },
                        label = { Text(name) },
                    )
                }
            }

            StepValueSlider(
                saved = style.sizePercent,
                range = 50..200,
                step = 10,
                tag = PlayerSizeTag,
                onCommit = { v -> change { it.copy(sizePercent = v) } },
            ) { "Size · $it%" }

            SectionLabel("Font")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                SubtitleFont.entries.forEach { font ->
                    FilterChip(
                        selected = style.font == font,
                        onClick = { change { it.copy(font = font) } },
                        label = { Text(font.name) },
                    )
                }
            }

            ColorSwatches(selected = style.textColor, onSelect = { c -> change { it.copy(textColor = c) } })
            EdgeSegments(selected = style.edge, onSelect = { e -> change { it.copy(edge = e) } })

            StepValueSlider(
                saved = style.backgroundPercent,
                range = 0..100,
                step = 5,
                onCommit = { v -> change { it.copy(backgroundPercent = v) } },
            ) { "Background · $it%" }

            StepValueSlider(
                saved = style.raisePercent,
                range = 0..30,
                step = 1,
                onCommit = { v -> change { it.copy(raisePercent = v) } },
            ) { "Raise from bottom · $it" }

            CategoryGroup {
                item {
                    SwitchSetting(
                        value = style.useEmbeddedStyles,
                        updateValue = { on -> change { it.copy(useEmbeddedStyles = on) } },
                        settingTitle = { Text("Use styles from subtitle file") },
                        summaryValue = { Text("Keeps the fansub look when there is one") },
                        settingIcon = { Icon(Icons.Default.Style, null, modifier = Modifier.fillMaxSize()) },
                    )
                }
                item {
                    SwitchSetting(
                        value = style.skipSignsAndSongs,
                        updateValue = { on -> change { it.copy(skipSignsAndSongs = on) } },
                        settingTitle = { Text("Skip signs and songs tracks") },
                        summaryValue = { Text("Picks the dialogue track first") },
                        settingIcon = { Icon(Icons.Default.MusicOff, null, modifier = Modifier.fillMaxSize()) },
                    )
                }
            }

            if (hasBatteryAlert) {
                SectionLabel("Battery")
                val shown = batteryDraft ?: savedBattery.coerceIn(5, 50)
                CategoryGroup {
                    item {
                        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            ListItem(
                                headlineContent = { Text("Battery alert") },
                                supportingContent = { Text("Warn me below $shown%") },
                                leadingContent = { Icon(Icons.Default.BatteryAlert, null) },
                            )
                            StepSlider(
                                value = shown,
                                range = 5..50,
                                step = 5,
                                tag = PlayerBatteryTag,
                                onDraft = { batteryDraft = it },
                                onCommit = {
                                    scope.launch { settingsHandling.batteryPercent.set(it) }
                                    batteryDraft = null
                                },
                            )
                        }
                    }
                }
            }

            SectionLabel("Gestures")
            CategoryGroup {
                item {
                    ListItem(
                        headlineContent = { Text("Brightness and volume") },
                        supportingContent = { Text("Drag up or down on the left or right side") },
                        leadingContent = { Icon(Icons.Default.BrightnessMedium, null) },
                    )
                }
                item {
                    ListItem(
                        headlineContent = { Text("Play and pause") },
                        supportingContent = { Text("Double-tap anywhere") },
                        leadingContent = { Icon(Icons.Default.TouchApp, null) },
                    )
                }
            }
        }
    }
}

/** The real player chrome in the chosen style, with a subtitle line drawn the way the player draws it. */
@Composable
private fun PlayerPreview(playerStyle: PlayerStyle, style: SubtitleStyle, animate: Boolean) {
    val extras = if (hasBatteryAlert) PlayerPreviewDefaults.Android else PlayerPreviewDefaults.Desktop
    BoxWithConstraints {
        PlayerStylePreview(
            style = playerStyle,
            extras = extras,
            animate = animate,
            toggleOnClick = false,
            modifier = Modifier.fillMaxWidth(),
        )
        if (style.autoEnable) {
            val spec = style.toCaptionSpec()
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp + maxWidth * (360f / extras.designWidth.value) * spec.bottomPaddingFraction)
                    .testTag(PlayerSubtitleSampleTag)
            ) {
                CaptionCue(SampleCue, spec, baseFontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp),
    )
}

/** A labelled slider that follows the finger and writes once, on release. */
@Composable
private fun StepValueSlider(
    saved: Int,
    range: IntRange,
    step: Int,
    onCommit: (Int) -> Unit,
    tag: String? = null,
    format: (Int) -> String,
) {
    var draft by remember { mutableStateOf<Int?>(null) }
    SectionLabel(format(draft ?: saved))
    StepSlider(
        value = draft ?: saved,
        range = range,
        step = step,
        tag = tag,
        onDraft = { draft = it },
        onCommit = {
            onCommit(it)
            draft = null
        },
    )
}

@Composable
private fun StepSlider(
    value: Int,
    range: IntRange,
    step: Int,
    tag: String?,
    onDraft: (Int) -> Unit,
    onCommit: (Int) -> Unit,
) {
    // Last value reached during a drag; committed once on release.
    var dragged by remember { mutableStateOf<Int?>(null) }
    Slider(
        value = value.toFloat(),
        onValueChange = { new ->
            val snapped = (((new - range.first) / step).roundToInt() * step + range.first).coerceIn(range)
            dragged = snapped
            onDraft(snapped)
        },
        onValueChangeFinished = {
            dragged?.let(onCommit)
            dragged = null
        },
        valueRange = range.first.toFloat()..range.last.toFloat(),
        steps = (range.last - range.first) / step - 1,
        modifier = if (tag != null) Modifier.testTag(tag) else Modifier,
    )
}
