package com.programmersbox.kmpuiviews.presentation.settings.languageorder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.programmersbox.datastore.NewSettingsHandling
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/** Languages offered before any extension is installed, using the codes extensions report. */
private val CommonLanguages = listOf(
    "en", "ja", "ko", "zh", "zh-Hant", "es", "es-419", "pt-BR", "fr", "de",
    "it", "ru", "id", "vi", "th", "ar", "tr", "pl",
)

/** The default source language order, resolved the same way the source chooser resolves it. */
@Composable
fun rememberPreferredLanguages(settings: NewSettingsHandling = koinInject()): List<String> {
    val deviceLanguage = Locale.current.language
    val languages by remember(settings, deviceLanguage) {
        settings.sourceLanguageOrder.map { SourceLanguageOrdering.resolveDefaults(it, deviceLanguage) }
    }.collectAsStateWithLifecycle(listOf(deviceLanguage))
    return languages
}

/**
 * Edits the default source language order without needing any extensions installed, unlike
 * [LanguageOrderSheet], which only offers languages that installed extensions provide.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreferredLanguagesSheet(
    onDismiss: () -> Unit,
    settings: NewSettingsHandling = koinInject(),
) {
    val languages = rememberPreferredLanguages(settings)
    val scope = rememberCoroutineScope()
    fun save(order: List<String>) {
        scope.launch { settings.setSourceLanguageOrder(order) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("Preferred languages", style = MaterialTheme.typography.titleLarge)
            Text(
                "Sources that offer several languages list these first, in this order.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            LanguageList(
                languages = languages,
                onReorder = ::save,
                // Keep at least one; an empty list means "device language".
                onRemove = if (languages.size > 1) {
                    { lang -> save(languages - lang) }
                } else null,
            )

            val unranked = CommonLanguages.filter { it !in languages }
            if (unranked.isNotEmpty()) {
                SectionLabel("Add a language")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    unranked.forEach { lang ->
                        AssistChip(
                            onClick = { save(languages + lang) },
                            label = { Text(languageDisplayName(lang)) },
                            leadingIcon = { Icon(Icons.Default.Add, null) },
                        )
                    }
                }
            }
        }
    }
}
