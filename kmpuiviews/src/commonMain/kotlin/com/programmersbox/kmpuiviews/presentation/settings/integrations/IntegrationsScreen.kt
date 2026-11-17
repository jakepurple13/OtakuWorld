package com.programmersbox.kmpuiviews.presentation.settings.integrations

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.programmersbox.kmpuiviews.domain.aitext.AiSettings
import com.programmersbox.kmpuiviews.domain.aitext.AiToggle
import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.kmpuiviews.hasTranslationSettingsPage
import com.programmersbox.kmpuiviews.presentation.components.settings.CategoryGroupListItem
import com.programmersbox.kmpuiviews.presentation.settings.SettingsScaffold
import com.programmersbox.kmpuiviews.presentation.settings.translationmodels.showTranslationScreen
import com.programmersbox.kmpuiviews.utils.ComposeSettingsDsl
import com.programmersbox.kmpuiviews.utils.LocalNavActions
import com.programmersbox.supabaseintegration.ui.SupabaseIcon
import com.programmersbox.supabaseintegration.ui.SupabaseRoutes
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.ai_settings_summary_off
import otakuworld.kmpuiviews.generated.resources.ai_settings_summary_on
import otakuworld.kmpuiviews.generated.resources.ai_settings_title
import otakuworld.kmpuiviews.generated.resources.viewTranslationModels

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun IntegrationsScreen(
    composeSettingsDsl: ComposeSettingsDsl = koinInject(),
    aiSettings: AiSettings = koinInject(),
) {
    val navActions = LocalNavActions.current
    val aiEnabled by aiSettings.enabled.collectAsStateWithLifecycle(true)
    val aiDisabled by aiSettings.disabled.collectAsStateWithLifecycle(emptySet())
    var showTranslation by showTranslationScreen()

    SettingsScaffold(
        title = "Integrations",
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CategoryGroupListItem {
            segmentedListItem(
                content = { Text("Supabase") },
                leadingContent = { SupabaseIcon() },
                onClick = { navActions.navigate(SupabaseRoutes) },
            )
            segmentedListItem(
                content = { Text(stringResource(Res.string.viewTranslationModels)) },
                leadingContent = { Icon(Icons.Default.Language, null) },
                onClick = { if (hasTranslationSettingsPage) navActions.translationSettings() else showTranslation = true },
            )
            segmentedListItem(
                content = { Text(stringResource(Res.string.ai_settings_title)) },
                leadingContent = { Icon(Icons.Default.AutoAwesome, null) },
                supportingContent = {
                    Text(
                        if (aiEnabled) {
                            stringResource(
                                Res.string.ai_settings_summary_on,
                                AiToggle.entries.size - aiDisabled.size,
                                AiToggle.entries.size,
                            )
                        } else {
                            stringResource(Res.string.ai_settings_summary_off)
                        }
                    )
                },
                onClick = { navActions.navigate(Screen.Settings.Ai) },
            )
            apply(composeSettingsDsl.integrationsSettings)
        }
    }
}
