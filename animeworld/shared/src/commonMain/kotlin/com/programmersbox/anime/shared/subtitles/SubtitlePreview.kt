package com.programmersbox.anime.shared.subtitles

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val PreviewHeight = 200.dp

// The scene stands in for video content, so it keeps fixed colors instead of theme roles.
private val SceneSky = Color(0xFF2B3A4A)
private val SceneGround = Color(0xFF3E5A3A)
private val SceneSun = Color(0xFFE8C66A)
private val SceneHill = Color(0xFF4C6B46)

// Highlight of the raised edge; part of the caption itself, so not a theme role.
private val RaisedHighlight = Color(0xFF9A9A9A)

private val sampleCues = listOf("Hello, Dr. Arinsu.", "You're looking as beautiful as ever.")

/** Two sample cues over a stand-in video frame, drawn from the same [CaptionSpec] the player uses. */
@Composable
fun SubtitlePreview(style: SubtitleStyle, modifier: Modifier = Modifier) {
    val spec = style.toCaptionSpec()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(PreviewHeight)
            .clip(RoundedCornerShape(16.dp))
            .background(SceneSky)
    ) {
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .height(70.dp)
                .background(SceneGround)
        )
        Box(
            Modifier
                .offset(x = 230.dp, y = 36.dp)
                .size(56.dp)
                .background(SceneSun, CircleShape)
        )
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .offset(x = 60.dp, y = (-60).dp)
                .size(width = 90.dp, height = 70.dp)
                .background(SceneHill, RoundedCornerShape(topStart = 45.dp, topEnd = 45.dp))
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 12.dp)
                .padding(bottom = 8.dp + PreviewHeight * spec.bottomPaddingFraction)
        ) {
            sampleCues.forEach { CaptionCue(it, spec) }
        }
        Text(
            "PREVIEW",
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.5.sp,
            modifier = Modifier
                .padding(start = 12.dp, top = 10.dp)
                .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun CaptionCue(text: String, spec: CaptionSpec) {
    val base = spec.textStyle()
    Box(
        modifier = Modifier
            .background(Color(spec.backgroundArgb.toInt()), RoundedCornerShape(3.dp))
            .padding(horizontal = 6.dp)
    ) {
        when (spec.edge) {
            // Stroke is centered on the glyph outline, so half of it shows outside the fill.
            SubtitleEdge.Outline -> {
                val strokeWidth = with(LocalDensity.current) { 3.dp.toPx() } * spec.textScale
                Text(text, style = base.copy(color = Color.Black, drawStyle = Stroke(width = strokeWidth)))
            }
            // Media3 raised edge: a light copy up-left; the base text adds the dark edge down-right.
            SubtitleEdge.Raised -> Text(
                text,
                style = base.copy(color = RaisedHighlight, shadow = null),
                modifier = Modifier.offset(x = (-1).dp, y = (-1).dp)
            )
            else -> Unit
        }
        Text(text, style = base)
    }
}

private fun CaptionSpec.textStyle(): TextStyle = TextStyle(
    color = Color(foregroundArgb.toInt()),
    fontSize = 15.sp * textScale,
    lineHeight = 15.sp * textScale * 1.3f,
    fontFamily = when (font) {
        SubtitleFont.Default, SubtitleFont.Bold -> FontFamily.Default
        SubtitleFont.Serif -> FontFamily.Serif
        SubtitleFont.Mono -> FontFamily.Monospace
    },
    fontWeight = if (font == SubtitleFont.Bold) FontWeight.Bold else FontWeight.Normal,
    textAlign = TextAlign.Center,
    shadow = when (edge) {
        SubtitleEdge.None, SubtitleEdge.Outline -> null
        SubtitleEdge.Shadow -> Shadow(Color.Black.copy(alpha = 0.95f), Offset(2f, 2f), blurRadius = 4f)
        SubtitleEdge.Raised -> Shadow(Color.Black, Offset(1.5f, 1.5f), blurRadius = 0f)
    },
)
