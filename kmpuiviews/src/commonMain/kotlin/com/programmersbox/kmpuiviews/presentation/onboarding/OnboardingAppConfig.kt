package com.programmersbox.kmpuiviews.presentation.onboarding

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.vector.ImageVector

data class GenreCard(val name: String, val icon: ImageVector, val hue: Float)

data class OnboardingAppConfig(
    val genres: List<GenreCard>,
    val startLabel: String,
    val updateNoun: String,
    val appStepTab: String,
) {
    companion object {
        val Default = OnboardingAppConfig(
            genres = listOf(
                GenreCard("Action", Icons.Default.FlashOn, 20f),
                GenreCard("Romance", Icons.Default.Favorite, 340f),
                GenreCard("Mystery", Icons.Default.Search, 260f),
                GenreCard("Comedy", Icons.Default.EmojiEmotions, 50f),
                GenreCard("Fantasy", Icons.Default.AutoStories, 160f),
            ),
            startLabel = "Start reading",
            updateNoun = "chapter",
            appStepTab = "Reader",
        )
    }
}
