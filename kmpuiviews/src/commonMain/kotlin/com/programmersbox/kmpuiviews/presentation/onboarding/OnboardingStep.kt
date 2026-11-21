package com.programmersbox.kmpuiviews.presentation.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

/** Rotation/scale of the hero shape for a step; [Hidden] collapses it entirely. */
enum class HeroShape(val rotation: Float, val scale: Float) {
    Hidden(0f, 0f),
    Cookie(-14f, 1f),
    Diamond(45f, 1f),
    Arch(0f, 1f),
    Clover(22f, 1f),
    Bun(0f, 1f),
    Sunny(-30f, 1f),
    Flower(16f, 1f),
    Done(360f, 1.15f),
}

data class OnboardingStepSpec(
    val id: String,
    val title: String,
    val body: String,
    val icon: ImageVector,
    val shape: HeroShape,
    val summaryLabel: String? = null,
    val summary: (@Composable () -> String)? = null,
)

data class OnboardingStepEntry(
    val spec: OnboardingStepSpec,
    val content: @Composable () -> Unit,
)

object OnboardingTags {
    const val Next = "onb_next"
    const val Back = "onb_back"
    const val Skip = "onb_skip"
    const val SkipConfirm = "onb_skip_confirm"
    const val SkipCancel = "onb_skip_cancel"
    const val Progress = "onb_progress"
    const val Hero = "onb_hero"
    const val TwoPane = "onb_two_pane"
    const val SinglePane = "onb_single_pane"
    const val UpdatesOptions = "onb_updates_options"
    fun step(id: String) = "onb_step_$id"
}
