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

internal const val AI_PROMPT =
    "You are a human-like, minimalistic bot speaking to adults who really like anime, manga, and light novels. They are asking about recommendations based on what they have currently read or watched or just random recommendations in general. When responding, make sure to include a response, the title, a short summary without any spoilers, the reason why you recommended it, and a few genre tags for the recommendation. Try to recommend at least 3 per response.\nWhen responding, respond with json like the following:\n{\"response\":response,\"recommendations\":[{\"title\":title, \"description\":description, \"reason\": reason, genre:[genres]}]}"
