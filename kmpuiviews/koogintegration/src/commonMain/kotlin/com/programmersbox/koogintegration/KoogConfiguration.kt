package com.programmersbox.koogintegration

import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.stringPreferencesKey
import com.programmersbox.datastore.DataStoreHandler
import com.programmersbox.koogintegration.integrator.BookmarksIntegrator
import com.programmersbox.koogintegration.integrator.FavoritesIntegrator
import com.programmersbox.koogintegration.integrator.HeatMapIntegrator
import com.programmersbox.koogintegration.integrator.KoogIntegrator
import com.programmersbox.koogintegration.integrator.ListIntegrator
import com.programmersbox.koogintegration.provider.AgentProvider
import com.programmersbox.koogintegration.provider.OtakuAgentProvider
import com.programmersbox.koogintegration.provider.otakutools.LocalExplainTools
import com.programmersbox.koogintegration.provider.otakutools.RecommendationTools
import com.programmersbox.koogintegration.screens.chatscreen.ChatScreen
import com.programmersbox.koogintegration.screens.chatscreen.ChatViewModel
import com.programmersbox.koogintegration.screens.chatscreen.KoogNavigation
import com.programmersbox.koogintegration.screens.settings.KoogSettingsScreen
import com.programmersbox.koogintegration.screens.settings.KoogSettingsViewModel
import com.programmersbox.sharedcomponents.Navigator
import com.programmersbox.sharedcomponents.components.HideNavBarWhileOnScreen
import com.programmersbox.sharedtools.SearchRegistryItem
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.new
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

fun buildKoogModule() = module {
    singleOf(::AgentMaker)
    single<AgentProvider>(named("otaku_agent")) { new(::OtakuAgentProvider) }
    viewModel { ChatViewModel(get<AgentProvider>(named(it[0])), get(), get()) }
    viewModelOf(::KoogSettingsViewModel)
    factoryOf(::RecommendationTools)
    factoryOf(::MathTools)
    single(named("favorites")) { FavoritesIntegrator(get()) } bind KoogIntegrator::class
    single(named("heatMap")) { HeatMapIntegrator(get()) } bind KoogIntegrator::class
    single(named("bookmarks")) { BookmarksIntegrator(get()) } bind KoogIntegrator::class
    single(named("list")) { ListIntegrator(get()) } bind KoogIntegrator::class
    factory {
        LocalExplainTools(
            favoritesAnalyzer = get(qualifier = named("favorites")),
            heatmapAnalyzer = get(qualifier = named("heatMap")),
            bookmarksAnalyzer = get(qualifier = named("bookmarks")),
            listAnalyzer = get(qualifier = named("list"))
        )
    }

    singleOf(::KoogSearchItems) bind SearchRegistryItem::class

    single<KoogNavigation> {
        val navigationActions = get<Navigator>()
        KoogNavigation(
            onBack = { navigationActions.onBack() },
            onKoogSettingsClick = { navigationActions.navigateTo(KoogSettings) },
            onSearchClick = { navigationActions.toGlobalSearch(it) },
            onListClick = { navigationActions.toCustomList() }
        )
    }

    single {
        val koogApiKey = DataStoreHandler(
            key = stringPreferencesKey("koogApiKey"),
            defaultValue = ""
        )

        val koogCompany = DataStoreHandler(
            key = stringPreferencesKey("koogCompany"),
            defaultValue = ""
        )

        val koogModel = DataStoreHandler(
            key = stringPreferencesKey("koogModel"),
            defaultValue = ""
        )

        KoogDataStore(
            getApiKey = { koogApiKey.get() },
            getModelCompany = { koogCompany.get() },
            getModelName = { koogModel.get() },
            storeApiKey = { koogApiKey.set(it) },
            storeModelCompany = { koogCompany.set(it) },
            storeModelName = { koogModel.set(it) },
            apiKeyFlow = koogApiKey.asFlow(),
            modelCompanyFlow = koogCompany.asFlow(),
            modelNameFlow = koogModel.asFlow()
        )
    }

    navigation<KoogSettings> {
        val koogNavigation: KoogNavigation = koinInject()
        KoogSettingsScreen(
            onBack = { koogNavigation.onBack() }
        )
    }
    navigation<Koog> {
        val koogNavigation: KoogNavigation = koinInject()

        HideNavBarWhileOnScreen()
        ChatScreen(
            viewModel = koinViewModel { parametersOf("otaku_agent") },
            koogNavigation = koogNavigation
        )
    }
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
