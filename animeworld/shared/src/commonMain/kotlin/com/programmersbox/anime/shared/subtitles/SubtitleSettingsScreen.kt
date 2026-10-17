package com.programmersbox.anime.shared.subtitles

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import com.programmersbox.kmpuiviews.presentation.components.BackButton
import com.programmersbox.kmpuiviews.presentation.settings.SettingsScaffold
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel

@Serializable
data object SubtitleSettingsRoute : NavKey

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubtitleSettingsScreen(viewModel: SubtitleSettingsViewModel = koinViewModel()) {
    val style by viewModel.style.collectAsStateWithLifecycle()
    var showLanguages by remember { mutableStateOf(false) }

    SettingsScaffold(
        title = "Subtitles",
        topBar = {
            TopAppBar(
                title = { Text("Subtitles") },
                navigationIcon = { BackButton() },
                actions = { TextButton(onClick = viewModel::reset) { Text("Reset") } },
                scrollBehavior = it,
            )
        },
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            SubtitlePreview(style)
            SubtitleStyleControls(
                style = style,
                onChange = { new -> viewModel.update { new } },
                compact = false,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SectionHeader("Behavior")
        }

        BehaviorSwitch(
            title = "Use styles from subtitle file",
            summary = "Keeps .ass fonts, colors and positions. Size and colors above apply when off.",
            checked = style.useEmbeddedStyles,
            onCheckedChange = { on -> viewModel.update { it.copy(useEmbeddedStyles = on) } },
        )
        BehaviorSwitch(
            title = "Turn on automatically",
            summary = "Picks a track in your preferred language",
            checked = style.autoEnable,
            onCheckedChange = { on -> viewModel.update { it.copy(autoEnable = on) } },
        )
        ListItem(
            headlineContent = { Text("Preferred language") },
            supportingContent = { Text(preferredLanguagesSummary(style.preferredLanguages)) },
            trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) },
            modifier = Modifier.clickable { showLanguages = true }
        )
        BehaviorSwitch(
            title = "Skip signs and songs tracks",
            summary = "Prefer full dialogue when choosing a track",
            checked = style.skipSignsAndSongs,
            onCheckedChange = { on -> viewModel.update { it.copy(skipSignsAndSongs = on) } },
        )
        Spacer(Modifier.height(24.dp))
    }

    if (showLanguages) {
        PreferredLanguagesDialog(
            selected = style.preferredLanguages,
            onConfirm = { codes ->
                viewModel.update { it.copy(preferredLanguages = codes) }
                showLanguages = false
            },
            onDismiss = { showLanguages = false },
        )
    }
}

@Composable
private fun BehaviorSwitch(
    title: String,
    summary: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(summary) },
        trailingContent = { Switch(checked = checked, onCheckedChange = null) },
        modifier = Modifier.toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
    )
}
