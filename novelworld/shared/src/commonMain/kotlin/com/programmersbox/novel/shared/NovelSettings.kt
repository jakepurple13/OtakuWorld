package com.programmersbox.novel.shared

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.datastore.preferences.core.intPreferencesKey
import com.programmersbox.datastore.DataStoreHandler
import com.programmersbox.datastore.asState
import com.programmersbox.kmpuiviews.presentation.components.settings.SliderSetting
import kotlin.math.roundToInt

/** Body text size in sp for the novel reader. */
val novelTextSize = DataStoreHandler(
    key = intPreferencesKey("novel_text_size"),
    defaultValue = 16,
)

/** Reader text size range in sp, shared by onboarding and settings. */
internal val NovelTextSizeRange = 12..24

/** Settings row for [novelTextSize]; the setting is written once, when the drag ends. */
@Composable
internal fun NovelTextSizeSetting(textSize: DataStoreHandler<Int> = novelTextSize) {
    var saved by textSize.asState()
    var sliderValue by remember(saved) { mutableFloatStateOf(saved.coerceIn(NovelTextSizeRange).toFloat()) }
    SliderSetting(
        sliderValue = sliderValue,
        updateValue = { sliderValue = it.roundToInt().toFloat() },
        range = NovelTextSizeRange.first.toFloat()..NovelTextSizeRange.last.toFloat(),
        steps = NovelTextSizeRange.last - NovelTextSizeRange.first - 1,
        settingTitle = { Text("Text size") },
        settingIcon = { Icon(Icons.Default.FormatSize, null) },
        format = { "${it.roundToInt()}sp" },
        onValueChangedFinished = { saved = sliderValue.roundToInt() },
    )
}
