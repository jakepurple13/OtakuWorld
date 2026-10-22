package com.programmersbox.novel.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.programmersbox.kmpuiviews.presentation.tips.TapBarsDemo
import com.programmersbox.kmpuiviews.presentation.tips.TipGesture
import com.programmersbox.kmpuiviews.presentation.tips.TipsScope

val novelTips: TipsScope.() -> Unit = {
    tip(
        title = "Tap the text",
        description = "Shows or hides the reader bars.",
        gesture = TipGesture.Tap,
        demo = { onDone -> TapBarsDemo(onDone = onDone, prompt = "Tap the text") { SampleNovelPage() } },
    )
    tip(
        title = "Pull down in the reader",
        description = "Reloads the chapter.",
        gesture = TipGesture.Drag,
    )
}

/** Placeholder lines of text for the reader tip demo. */
@Composable
private fun BoxScope.SampleNovelPage() {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .matchParentSize()
            .padding(horizontal = 12.dp, vertical = 36.dp)
    ) {
        listOf(1f, 0.92f, 0.97f, 0.6f, 1f, 0.88f, 0.95f, 0.4f).forEach { width ->
            Column(
                Modifier
                    .fillMaxWidth(width)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.outlineVariant)
            ) {}
        }
    }
}
