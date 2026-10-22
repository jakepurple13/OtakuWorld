package com.programmersbox.kmpuiviews.presentation.onboarding.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.programmersbox.kmpuiviews.presentation.components.settings.CategoryGroup
import com.programmersbox.kmpuiviews.presentation.components.settings.PreferenceSetting
import com.programmersbox.kmpuiviews.presentation.components.settings.categorySetting
import com.programmersbox.kmpuiviews.presentation.settings.languageorder.PreferredLanguagesSheet
import com.programmersbox.kmpuiviews.presentation.settings.languageorder.languageDisplayName
import com.programmersbox.kmpuiviews.presentation.settings.languageorder.rememberPreferredLanguages
import com.programmersbox.kmpuiviews.utils.AppConfig
import org.koin.compose.koinInject

@Composable
internal fun SourcesContent(
    appConfig: AppConfig = koinInject(),
) {
    var showLanguages by remember { mutableStateOf(false) }
    val languages = rememberPreferredLanguages()

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        ListItem(
            headlineContent = { Text("Where titles come from") },
            supportingContent = {
                Text("${appConfig.appName} has no built-in sources. Everything you browse comes from external extensions that you install.")
            },
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(Icons.Default.Extension, null)
                Text("Add them later from Settings › Sources & Extensions.")
            }
        }

        CategoryGroup {
            categorySetting { Text("Preferred languages") }

            item {
                PreferenceSetting(
                    settingTitle = { Text("Show these first") },
                    summaryValue = { Text(languages.joinToString { languageDisplayName(it) }) },
                    settingIcon = { Icon(Icons.Default.Translate, null, modifier = Modifier.fillMaxSize()) },
                    onClick = { showLanguages = true },
                )
            }
        }
    }

    if (showLanguages) {
        PreferredLanguagesSheet(onDismiss = { showLanguages = false })
    }
}
