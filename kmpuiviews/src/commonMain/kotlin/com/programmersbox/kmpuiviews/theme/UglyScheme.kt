package com.programmersbox.kmpuiviews.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color


// The Extended Arsenal of Eye Pain
private val RadioactiveGreen = Color(0xFF00FF00)
private val PeptoPink = Color(0xFFFF69B4)
private val ScreamingOrange = Color(0xFFFF4500)
private val Windows95Teal = Color(0xFF008080)
private val BruisePurple = Color(0xFF3A004C)
private val HighlighterYellow = Color(0xFFE8FF00)
private val AshenGray = Color(0xFFB0B0B0)
private val Rust = Color(0xFF8B4513)
private val ToxicSludge = Color(0xFF4C5803)
private val DepressingBeige = Color(0xFFE3DCC8)

val FullyUglyLightColorScheme = lightColorScheme(
    // Primary colors (Buttons, active states)
    primary = RadioactiveGreen,
    onPrimary = PeptoPink,
    primaryContainer = ScreamingOrange,
    onPrimaryContainer = Windows95Teal,

    // Secondary colors (FABs, selection controls)
    secondary = BruisePurple,
    onSecondary = ToxicSludge,
    secondaryContainer = Rust,
    onSecondaryContainer = BruisePurple, // Zero contrast container text

    // Tertiary colors (Contrasting accents, badges)
    tertiary = HighlighterYellow,
    onTertiary = AshenGray,
    tertiaryContainer = Windows95Teal,
    onTertiaryContainer = RadioactiveGreen,

    // Error colors (Complete mental model breakage)
    error = DepressingBeige,          // Errors look like boring disabled states
    onError = AshenGray,
    errorContainer = ToxicSludge,     // Error backgrounds look like swamp water
    onErrorContainer = PeptoPink,

    // Background and Surface (The foundation of your app)
    background = ScreamingOrange,
    onBackground = Rust,              // Barely visible on orange
    surface = PeptoPink,              // All cards are now pink
    onSurface = HighlighterYellow,    // Unreadable yellow text on pink cards
    surfaceVariant = ToxicSludge,     // Segmented buttons/bottom sheets are sludge
    onSurfaceVariant = BruisePurple,

    // Borders and Dividers
    outline = HighlighterYellow,      // Text field borders will burn your eyes
    outlineVariant = ScreamingOrange, // Dividers that blend into the background

    // Inverse colors (Snackbars)
    inverseSurface = RadioactiveGreen,
    inverseOnSurface = PeptoPink,
    inversePrimary = BruisePurple,

    // Scrim (The background dim when a drawer/dialog opens)
    scrim = HighlighterYellow,        // Instead of dimming, dialogs flashbang the user

    // Surface Tint (Elevation overlay)
    surfaceTint = Rust
)

val FullyUglyDarkColorScheme = darkColorScheme(
    primary = Windows95Teal,
    onPrimary = ScreamingOrange,
    primaryContainer = ToxicSludge,
    onPrimaryContainer = HighlighterYellow,

    secondary = PeptoPink,
    onSecondary = RadioactiveGreen,
    secondaryContainer = AshenGray,
    onSecondaryContainer = DepressingBeige,

    tertiary = Rust,
    onTertiary = BruisePurple,
    tertiaryContainer = ScreamingOrange,
    onTertiaryContainer = ToxicSludge,

    error = RadioactiveGreen,         // Errors are 'success' green in dark mode
    onError = BruisePurple,
    errorContainer = HighlighterYellow,
    onErrorContainer = AshenGray,

    // The "Dark" mode background is blindingly bright
    background = HighlighterYellow,
    onBackground = AshenGray,
    surface = ScreamingOrange,
    onSurface = RadioactiveGreen,
    surfaceVariant = PeptoPink,
    onSurfaceVariant = Rust,

    outline = ToxicSludge,
    outlineVariant = Windows95Teal,

    inverseSurface = DepressingBeige,
    inverseOnSurface = ToxicSludge,
    inversePrimary = ScreamingOrange,

    scrim = PeptoPink, // Dialogs bathe the screen in pink
    surfaceTint = HighlighterYellow
)