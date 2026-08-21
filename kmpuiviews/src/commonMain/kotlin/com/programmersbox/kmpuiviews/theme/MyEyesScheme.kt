package com.programmersbox.kmpuiviews.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// The absolute worst colors imaginable
private val NeonGreen = Color(0xFF00FF00)
private val HotPink = Color(0xFFFF00FF)
private val MuddyBrown = Color(0xFF4E342E)
private val Cyan = Color(0xFF00FFFF)
private val EyeBleedYellow = Color(0xFFFFFF00)
private val BrightRed = Color(0xFFFF0000)
private val DarkRed = Color(0xFF8B0000)
private val SolidBlue = Color(0xFF0000FF)
private val NavyBlue = Color(0xFF000080)

val UglyLightColorScheme = lightColorScheme(
    primary = NeonGreen,
    onPrimary = HotPink,         // Vibrating colors when placed together
    secondary = MuddyBrown,
    onSecondary = Cyan,          // Muddy and neon clash
    tertiary = EyeBleedYellow,
    onTertiary = SolidBlue,
    background = BrightRed,
    onBackground = DarkRed,      // Impossible to read text
    surface = SolidBlue,
    onSurface = NavyBlue,        // Impossible to read text, part 2
    error = NeonGreen,           // Errors are green now. Chaos.
    onError = EyeBleedYellow
)

val UglyDarkColorScheme = darkColorScheme(
    primary = HotPink,
    onPrimary = NeonGreen,
    secondary = Cyan,
    onSecondary = MuddyBrown,
    tertiary = SolidBlue,
    onTertiary = EyeBleedYellow,
    background = EyeBleedYellow, // A literal flashbang in dark mode
    onBackground = SolidBlue,
    surface = MuddyBrown,
    onSurface = BrightRed,       // Terrible contrast on cards
    error = Cyan,
    onError = HotPink
)