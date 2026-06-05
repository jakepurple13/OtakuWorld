package com.programmersbox.koogintegration

import androidx.compose.ui.unit.dp
import com.programmersbox.koogintegration.provider.AgentProvider
import com.programmersbox.koogintegration.provider.OtakuAgentProvider
import com.programmersbox.koogintegration.provider.otakutools.RecommendationTools
import com.programmersbox.koogintegration.screens.chatscreen.ChatViewModel
import com.programmersbox.koogintegration.screens.settings.KoogSettingsViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.new
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

fun buildKoogModule() = module {
    singleOf(::AgentMaker)
    single<AgentProvider>(named("otaku_agent")) { new(::OtakuAgentProvider) }
    viewModel { ChatViewModel(get<AgentProvider>(named(it[0]))) }
    viewModelOf(::KoogSettingsViewModel)
    factoryOf(::RecommendationTools)
    factoryOf(::MathTools)
}

object AppDimension {
    // Base spacing units
    val spacingExtraSmall = 4.dp
    val spacingSmall = 8.dp
    val spacingMedium = 16.dp
    val spacingLarge = 24.dp
    val spacingExtraLarge = 32.dp
    val spacingXXLarge = 40.dp
    val spacingXXXLarge = 48.dp
    val spacingXXXXLarge = 56.dp

    // Specific spacing for common use cases
    val spacingScreenHorizontalPadding = spacingMedium
    val spacingScreenVerticalPadding = spacingMedium
    val spacingBetweenItems = spacingSmall
    val spacingBetweenSections = spacingLarge
    val spacingBetweenGroups = spacingMedium
    val spacingContentPadding = spacingMedium
    val spacingButtonPadding = spacingSmall

    // Elevation values
    val elevationNone = 0.dp
    val elevationExtraSmall = 1.dp
    val elevationSmall = 2.dp
    val elevationMedium = 4.dp
    val elevationLarge = 8.dp
    val elevationExtraLarge = 16.dp

    // Border radius
    val radiusSmall = 4.dp
    val radiusMedium = 8.dp
    val radiusLarge = 12.dp
    val radiusExtraLarge = 16.dp
    val radiusRound = 24.dp

    // Icon button sizes
    val iconButtonSizeSmall = 32.dp
    val iconButtonSizeMedium = 40.dp
    val iconButtonSizeLarge = 48.dp
    val iconButtonSizeExtraLarge = 56.dp

    // Chat message layout
    val messageTitleColumnWidth = 64.dp
}
