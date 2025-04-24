package com.programmersbox.datastore

import com.kmpalette.palette.graphics.Palette

enum class PaletteSwatchType(val swatch: (Palette) -> Palette.Swatch?) {
    Vibrant(Palette::vibrantSwatch),
    Muted(Palette::mutedSwatch),
    Dominant(Palette::dominantSwatch),
    LightVibrant(Palette::lightVibrantSwatch),
    DarkVibrant(Palette::darkVibrantSwatch),
    LightMuted(Palette::lightMutedSwatch),
    DarkMuted(Palette::darkMutedSwatch),
}